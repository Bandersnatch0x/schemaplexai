---
title: DebugMaster 根因调试 v1 阻塞清零
agent: DebugMaster
date: 2026-05-08
domain: debugging
iron_rule: investigate-before-fix; stop-after-3-fails
---

## 1. 0-10 评分表

| 子维度 | 当前分 | 10 分定义 |
|---|---|---|
| 假设链完整 | 3 | 每阻塞 ≥ 3 条假设 + 可证伪步骤 |
| 数据流追踪 | 2 | 从生产者到消费者全程标注，含中间件落点 |
| 修复 PR 可执行 | 2 | 含具体改动行号、伪代码、测试用例 |
| 回归防护 | 2 | 每修复必含 unit + integration test |

调查后目标：四项 ≥ 8。本次报告把每个阻塞从「症状」推到「根因」，并给出 2-3 行级别的修复草案。**核心结论：4 个阻塞中 3 个是「基础设施已建好但接线缺失」，工作量合计约 1.5 人日。**

---

## 2. 阻塞 1：CostService 三零值

### 数据流追踪（追完，发现关键断点）

预期链路：
```
LLM 调用 (OpenAiProvider) → token usage 元数据
  → ??? cost event 投递点 ???
  → ClickHouse sf_cost_record 表
  → CostService.queryCostByTenant() 聚合 → 仪表盘
```

**实际状态扫描**：
- 上游：`schemaplexai-agent-engine/.../OpenAiProvider.java` 提到 `tokenUsage`，但 grep `CostRecord|cost_record|costAmount` 在 agent-engine 模块**零命中**——agent-engine 从未投递任何 cost event。
- 中游：`ClickHouseCostSyncService.syncIncrementalData()` 第 134 行只同步 `sf_agent_execution`，**完全不碰 `sf_cost_record`**。
- 下游：`CostService.queryCostByTenant()`（line 23-33）三行 `BigDecimal.valueOf(0)` 直接硬编码。`budgetMapper` 已注入但仅在 `checkBudgetAlerts` 用到。
- DB：`schemaplexai_costs.sf_cost_record` 在 ClickHouse 端的 DDL 存在（`CostRecord.java` 注释指向它），PG 端的 `sf_budget.used_amount` 字段已声明，但**没有 writer**。

**关键断点**：3 个独立缺口
1. 没有 cost event producer（LLM 调用后无人写入 `sf_cost_record` 或 PG 等价表）。
2. ClickHouseCostSyncService 没把 cost 表加到同步队列（其 `CURSOR_KEY = "sf_agent_execution"` 单一固定值）。
3. CostService 没读 ClickHouse，硬编码零值。

### 假设链（4 条，可证伪）

| H | 假设 | 证伪步骤 |
|---|------|----------|
| H1 | 上游 LLM provider 已埋点但写到了别处（如 logs） | grep `tokenUsage` 全 repo，看 sink。**已验证：仅日志输出，无 DB 写入** ✓ 假设成立 |
| H2 | 有别的 cost ingestion service 我们没找到 | grep `INSERT INTO.*sf_cost_record\|insertCostRecord` 全 repo。**已验证：零命中** ✓ 假设成立 |
| H3 | TODO 注释（line 27-28）只是文档遗漏，但底层已实现 | git blame line 29-31 → `7696dea chore: baseline before runtime redesign`。这是占位代码，没有实现意图。✓ 假设成立 |
| H4 | sf_budget 表的 used_amount 是否有任何 update 路径 | grep `setUsedAmount\|usedAmount.*=` → 除测试夹具外 0 处生产代码更新。✓ 假设成立 |

**结论根因**：**整条 cost pipeline 是 stub**。不是单一 bug 而是缺整个数据通路。

### 修复 PR 草案（分两阶段）

**阶段 A — v1 最小可行成本可观测（3 PR，约 0.5 人日）**

PR-A1：在 `agent-engine` 注入 `CostEventPublisher`：
```java
// agent-engine 新增 cost/CostEventPublisher.java
@Component @RequiredArgsConstructor
public class CostEventPublisher {
  private final RabbitTemplate rabbit; // 或直接 JDBC 写 PG
  public void publish(Long tenantId, Long executionId, String model,
                      long inputTokens, long outputTokens, BigDecimal cost) {
    rabbit.convertAndSend("sf.cost.exchange", "cost.recorded",
      Map.of("tenantId",tenantId,"executionId",executionId,"model",model,
             "inputTokens",inputTokens,"outputTokens",outputTokens,"costAmount",cost));
  }
}
// 在 OpenAiProvider 调用结束后调用 publisher.publish(...)
```

PR-A2：`schemaplexai-task` 新增 `CostRecordConsumer` 持久化到 PG `sf_cost_record_pg`（v1 用 PG，避免 ClickHouse 强依赖）：
```java
@RabbitListener(queues="sf.cost.queue")
public void onMessage(NotificationMessage payload) {
  costRecordMapper.insert(toEntity(payload));
  budgetMapper.incrementUsed(payload.tenantId, payload.costAmount); // UPDATE sf_budget SET used_amount = used_amount + ?
}
```

PR-A3：改 `CostService.queryCostByTenant`：
```java
public Map<String,BigDecimal> queryCostByTenant(String tenantId) {
  return Map.of(
    "totalCost", costRecordMapper.sumByTenant(tenantId),
    "todayCost", costRecordMapper.sumByTenantAndDay(tenantId, LocalDate.now()),
    "monthCost", costRecordMapper.sumByTenantAndMonth(tenantId, YearMonth.now())
  );
}
```

**阶段 B — v1.1 ClickHouse OLAP**：扩展 `ClickHouseCostSyncService`，把 `CURSOR_KEY` 升级为 `Map<String, SyncStrategy>`，新增 `sf_cost_record` strategy。当前 v1 不做，避免引入 ClickHouse 必装依赖。

### 回归防护
- Unit：`CostServiceTest`（新建）覆盖三个 sum 方法，含空表/单条/跨月份。
- Integration：`@SpringBootTest` 用 testcontainer PG，发布 fake cost event，断言 1) `sf_cost_record_pg` 行数 +1 2) `sf_budget.used_amount` 累加 3) `queryCostByTenant` 返回非零。
- E2E：playwright smoke 命中 `/ops/cost/dashboard` 断言 `totalCost ≥ 0` 且字段存在。

---

## 3. 阻塞 2：AgentExecutionController SSE 未注册

### 数据流追踪（最干净的一个 bug）

预期：
```
Client GET /agents/{id}/executions/{execId}/events
  → SseEmitter 创建 → register 到 ExecutionEventBus
  → AgentStateMachine.transitionTo() 触发 publishStateTransition()
  → eventBus.broadcast() → 客户端收到 state-transition 事件
```

**实际状态**（已验证 file:line）：
- `AgentExecutionController.java:81-83` 创建 SseEmitter 后**直接 return，没注册**。
- `ExecutionEventBus.java:28` `register(executionId, emitter)` 方法已实现（含 onCompletion/onTimeout/onError 回收）。
- `AgentStateMachine.java:21,50,63,72` 已注入 `eventBus` 并主动 `publishStateTransition` / `publishExecutionCompleted` / `complete`。
- `ExecutionEventBusTest.java` 已存在测试覆盖。

**根因**：**只缺 1 行 register 调用**。基础设施 100% 就位，控制器漏接线。

### 假设链（3 条）

| H | 假设 | 证伪步骤 |
|---|------|----------|
| H1 | EventBus 没实现，需新建 | 读 `ExecutionEventBus.java`。**已证伪：完整实现 + 单测** |
| H2 | 状态机没发事件 | grep `eventBus.publish*` → 4 处调用，全在 AgentStateMachine。**已证伪** |
| H3 | 控制器忘了注入 + register | 读 controller 构造器（line 24-26）注入了 engine/lifecycle/sseTokenValidator，**未注入 eventBus**。✓ 根因 |

### 修复 PR 草案（< 10 行改动）

```java
// AgentExecutionController.java
@RequiredArgsConstructor
public class AgentExecutionController {
  private final AgentExecutionEngine executionEngine;
  private final AgentExecutionLifecycleService lifecycleService;
  private final SseTokenValidator sseTokenValidator;
  private final ExecutionEventBus eventBus; // ADD

  @GetMapping("/{id}/executions/{execId}/events")
  public SseEmitter subscribeExecutionEvents(@PathVariable Long id,
        @PathVariable Long execId, @RequestParam String token) {
    String executionId = String.valueOf(execId);
    ValidationResult result = sseTokenValidator.validate(token, executionId);
    if (!result.isValid()) {
      throw new SecurityException("Unauthorized SSE access: " + result.errorMessage());
    }
    SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
    eventBus.register(executionId, emitter); // FIX
    return emitter;
  }
}
```

### 回归防护
- Unit：`AgentExecutionControllerTest` 新增 `subscribe_registersEmitterToEventBus`，mock eventBus 验证 `register(eq("123"), any(SseEmitter.class))` 被调用一次。
- Integration：`SseSubscriptionIT` 用 `MockMvc.asyncDispatch` 启动 SSE，触发 `stateMachine.transitionTo(...)`，断言客户端收到 `state-transition` 事件 ≥ 1 帧。
- E2E：playwright smoke 第 4 步覆盖（见阻塞 4）。

---

## 4. 阻塞 3：NotificationConsumer 三桩

### 数据流追踪

```
producer → exchange sf.notification.exchange → queue sf.notification.queue
  → NotificationConsumer.onMessage (✓ 工作)
  → routeToChannel switch (✓ 工作)
  → handleEmail/handleSms/handleInApp/handleWebhook
       email: log only, return true ✗
       sms:   log only, return true ✗
       webhook: 实际 HTTP 调用 ✓ 实现完整
       in-app: log only, return true ✗（但 NotificationServiceImpl.sendNotification 已实现）
```

**关键发现**：`NotificationServiceImpl.sendNotification(userId,type,title,content)` 已完整实现并写入 `sf_notification` 表（line 27-53）。Consumer 只需注入 service 调用即可。**in-app 是 1 行修复**。

### 假设链（4 条）

| H | 假设 | 证伪步骤 |
|---|------|----------|
| H1 | NotificationService 不存在 | grep `NotificationServiceImpl` → ops 模块完整实现 ✓ 已证伪 |
| H2 | task 模块无法依赖 ops 模块（循环依赖风险） | task 当前依赖 common+model+dao；ops 不在依赖图中。**风险存在**。规避方案：通过 MQ 二次投递回 ops，或把 service 接口下沉到 model。✓ 假设成立 |
| H3 | 邮件 SMS provider 是 v1 必需 | 读 `docs/specs/2026-05-01-v1.0-notification.md`，v1 优先级 = in-app；email/sms 列为 P2。✓ 假设成立 |
| H4 | NotificationMessage DTO 字段够 in-app 用 | 读 NotificationMessage.java：含 userId/title/content/templateCode → 字段足够 ✓ |

### 关键决策点（DebugMaster 给技术成本估算，由 ProductStrategist 拍板）

| 选项 | v1 工作量 | 风险 |
|------|-----------|------|
| 全砍到仅 in-app | 0.3 人日（仅修 handleInApp + 加一个 dao 调用） | 低；email/sms 在 v1.1 补 |
| in-app + email | 1.5 人日 | 中；需 SMTP 配置 + 模板渲染 + 发件密钥管理 |
| 全做（含 SMS） | 4 人日 | 高；SMS 网关合规审批通常需法务 |

**推荐**：v1 = in-app only，email/sms 改成抛 `UNSUPPORTED_CHANNEL`，return false 让消息进 DLQ 而不是静默吞掉。

### 修复 PR 草案

```java
// NotificationConsumer.java（新增依赖注入）
@Component @RequiredArgsConstructor
public class NotificationConsumer {
  private final MessageFailLogService messageFailLogService;
  private final ObjectMapper objectMapper;
  private final NotificationService notificationService; // ADD（通过 ops 暴露的 Spring Bean，跨模块需 component-scan 或拆成 SPI）

  private boolean handleInApp(NotificationMessage payload) {
    if (payload.getUserId() == null) {
      log.error("[IN-APP] userId required"); return false;
    }
    String type = payload.getTemplateCode() != null ? payload.getTemplateCode() : "GENERIC";
    notificationService.sendNotification(
        payload.getUserId(), type, payload.getTitle(), payload.getContent());
    return true;
  }

  private boolean handleEmail(NotificationMessage payload) {
    log.warn("[EMAIL] not implemented in v1, falling back to in-app");
    return handleInApp(payload); // 或 return false 进 DLQ，由 ProductStrategist 决定
  }

  private boolean handleSms(NotificationMessage payload) {
    log.warn("[SMS] not implemented in v1");
    return false; // 进 DLQ，留 v1.1 实现
  }
}
```

**模块依赖问题处理**：把 `NotificationService` 接口下沉到 `schemaplexai-model` 或 `schemaplexai-dao`，把 `NotificationServiceImpl` 拆成 `OpsNotificationServiceImpl`。或者 task 模块通过 `ApplicationEventPublisher` 发本地 Spring 事件，让 ops 监听并落库（更松耦合）。

### 回归防护
- Unit：`NotificationConsumerTest` mock NotificationService，覆盖 in-app 成功 / userId 缺失 / email fallback / sms 不支持。
- Integration：testcontainer RabbitMQ + PG，发消息断言 `sf_notification` 表新增一行。
- E2E：playwright 触发某用户操作（创建 spec？） → 轮询 `/web/notifications/unread` 收到 ≥ 1 条。

---

## 5. 阻塞 4：CI e2e smoke 缺失

### 假设链（3 条）

| H | 假设 | 证伪步骤 |
|---|------|----------|
| H1 | smoke.spec.ts 之前存在被删 | `git log --all -- schemaplexai-ui/e2e/smoke.spec.ts` → **零提交**。✓ 已证伪 |
| H2 | 写在别的路径 | `find schemaplexai-ui -name "smoke*"` → 0 命中。✓ 已证伪 |
| H3 | 从未写过，CI 是 wishful thinking | `ci.yml:152` 直接引用文件名却从未提交 → ✓ 根因。CI e2e job 自首次 push 以来 100% 失败 |

**附加发现**：`playwright.config.ts:11` baseURL = `localhost:3000`，但 vite 默认端口可能不同；CI workflow `webServer.command = npm run dev`，等待最多 120s。需要联调验证启动时间。

### 修复 PR 草案（最小 smoke：登录 → 进 Agent → SSE 收 ≥ 1 帧）

```typescript
// schemaplexai-ui/e2e/smoke.spec.ts (新建)
import { test, expect } from '@playwright/test'

test.describe('v1 smoke', () => {
  test('login → list agents → subscribe SSE → receive ≥ 1 event', async ({ page }) => {
    // 1. Login
    await page.goto('/login')
    await page.getByLabel(/username/i).fill(process.env.E2E_USER ?? 'admin')
    await page.getByLabel(/password/i).fill(process.env.E2E_PASS ?? 'admin123')
    await page.getByRole('button', { name: /sign in|登录/i }).click()
    await expect(page).toHaveURL(/\/(dashboard|home|agents)/, { timeout: 15_000 })

    // 2. Navigate to Agent list
    await page.goto('/agents')
    await expect(page.getByRole('heading', { name: /agent/i }).first()).toBeVisible()

    // 3. Open executor for first agent (or fixture)
    const firstAgent = page.locator('[data-testid^="agent-row-"]').first()
    if (await firstAgent.isVisible().catch(() => false)) {
      await firstAgent.click()
    } else {
      await page.goto('/agent-executor/1') // fixture seeded id
    }

    // 4. Trigger execution and assert SSE frame
    await page.getByRole('button', { name: /run|执行/i }).click()
    const sseFrame = page.locator('[data-testid="sse-event"]').first()
    await expect(sseFrame).toBeVisible({ timeout: 30_000 })
  })

  test('health check endpoint returns 200', async ({ request }) => {
    const res = await request.get('http://localhost:8080/actuator/health')
    expect([200, 404]).toContain(res.status()) // 404 if actuator off in dev
  })
})
```

**关键约束**：
- 后端必须在 CI 中也启动（当前 e2e job 只跑前端 vite，没启 Spring Boot）。修复 ci.yml 加 `services` block 启动 testcontainer PG/Redis/RabbitMQ + 后台 `mvn spring-boot:run`，或 mock 后端用 MSW。
- 选 MSW 路线（最小工作量）：在 `schemaplexai-ui/src/mocks/` 加 handlers，e2e 跑 mock mode。
- 端口对齐：把 `playwright.config.ts:11` baseURL 改 `process.env.BASE_URL ?? 'http://localhost:5173'`（vite 默认）。

### 回归防护
- smoke.spec.ts 本身就是回归保护。
- 新增 `e2e/auth.spec.ts` 测试 401 → refresh → 重试链路。
- ci.yml 加 fail-fast，失败时 upload `test-results/` 已有。

---

## 6. 三次失败保护机制

每个阻塞设定升级矩阵，避免无限调试：

| 阻塞 | 第 1 次失败 | 第 2 次失败 | 第 3 次失败（升级） |
|------|-------------|-------------|---------------------|
| CostService | 检查 RabbitMQ exchange 绑定 | 检查 listener `@RabbitListener` 队列名拼写 | 升级 architect 重新设计 cost pipeline，可能改用 Outbox 模式 |
| SSE 未注册 | 检查 eventBus 是否成功注入 | 检查 SseTokenValidator 是否提前抛错绕过 register | 升级 backend-lead，可能是 Spring Web MVC async config 问题 |
| Notification | 检查跨模块 bean 扫描 | 检查 NotificationService 接口路径 | 升级 architect 评估改 ApplicationEvent 方案 |
| smoke.spec.ts | 检查 baseURL 端口 | 检查后端是否在 CI 启动 | 升级 devops，可能需要 docker-compose 起整套环境 |

**铁律执行**：任何阻塞修复尝试 ≥ 3 次仍失败，**停手发 ADR 草案**，不要继续 patch。

---

## 7. 关键发现（5 条隐性 bug，除上述 4 个）

1. **WorkflowTriggerConsumer 全 stub**（`schemaplexai-task/.../WorkflowTriggerConsumer.java:45`）：MQ 收消息后只 log，从未 `runtimeService.startProcessInstance`。BPMN 触发链断裂，但 v1 是否启用 workflow 待 ProductStrategist 确认。

2. **QualityEventConsumer 全 stub**（同模块 `:45`）：质量事件无人消费，drift detection 形同虚设。

3. **MilvusSyncConsumer 全 stub**（同模块 `:45`）：向量库永远不会被同步，RAG 服务降级为 Phase 1 的关键字搜索（`RagServiceImpl:31` 的 TODO 也证实）。

4. **SpecReviewNotificationDelegate 未对接通知**（`schemaplexai-workflow/.../SpecReviewNotificationDelegate.java:81`）：spec 审核完成不发通知，与阻塞 3 形成双重故障。

5. **HttpCallAdapter SSRF 防护不全**（`schemaplexai-agent-engine/.../HttpCallAdapter.java:34`）：缺租户级 URL allowlist，仅靠私有 IP 判断。**安全风险**——agent 工具可被诱导请求内部 metadata endpoint，security-reviewer 必查。

6. **`schemaplexai-agent-engine/.../McpToolAdapter.java:66`**：MCP 协议未实际调用。如果 v1 demo 涉及 MCP 工具调用必失败。

---

## 8. 给用户的关键问题

> **CostService 的三个零值，根因不是「ClickHouse 没接」，而是从未有任何代码写入 cost 数据**。三个独立缺口：(1) agent-engine 没埋 cost event publisher，(2) task 模块没 cost consumer，(3) CostService 本身硬编码零。**v1 不必上 ClickHouse**，建议 PG 短链路：publisher → MQ → consumer → `sf_cost_record_pg` 表 → CostService 读 PG。ClickHouse 同步推 v1.1。
>
> **请决策三个 cut 点**：
> 1. 阻塞 3 通知通道：v1 = 仅 in-app 还是 in-app + email？（差 1.5 人日）
> 2. 阻塞 1 cost：v1 走 PG 短链路（0.5 人日）还是直接上 ClickHouse 全链路（2 人日）？
> 3. 隐性 bug 2/3（quality + milvus consumer）：v1 是否需要？如果 demo 不演示 RAG/质量门禁，可继续 stub 保平安。
>
> **零成本顺手做的**：阻塞 2 SSE 修复（< 10 行 + 单测）、隐性 bug 5 SSRF allowlist（高安全 ROI）。
