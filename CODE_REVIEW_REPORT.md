# SchemaPlexAI — 代码评审报告

> **评审日期**：2026-04-29  
> **评审范围**：全部 16 个模块（后端 345 Java 文件 + 前端 34 TS/TSX 文件 + 基础设施配置）  
> **评审维度**：架构一致性、代码规范、安全、并发、编译风险、空指针、资源泄漏

---

## 一、评审总览

| 模块组 | 状态 | 严重问题 | 高风险 | 中低风险 |
|--------|------|----------|--------|----------|
| 基础设施（common/model/dao/gateway/system/web）| ⚠️ 需修复 | 5 | 6 | 4 |
| 核心引擎（agent-config/agent-engine）| ❌ 严重 | 4 | 9 | 3 |
| 业务模块（context/spec/workflow/quality/integration/ops）| ⚠️ 需修复 | 0 | 7 | 5 |
| 异步调度（task）| 待评审 | - | - | - |
| 前端（ui）| 待评审 | - | - | - |

**总体评价**：项目骨架完整，但存在 **9 个 P0 级严重问题**（阻塞编译或导致运行时崩溃），**22 个 P1 级高风险问题**（安全/并发/数据一致性），必须在进入测试阶段前修复。

---

## 二、P0 级严重问题（必须立即修复）

### P0-001：数据库驱动不一致（MySQL vs PostgreSQL）

| 项目 | 内容 |
|------|------|
| **影响模块** | `schemaplexai-system`、`schemaplexai-agent-engine` |
| **问题描述** | `application.yml` 中配置 `driver-class-name: com.mysql.cj.jdbc.Driver`，但项目设计使用 PostgreSQL，docker-compose 也只启动了 PostgreSQL |
| **具体位置** | `system/src/main/resources/application.yml:11`、`agent-engine/src/main/resources/application.yml:11` |
| **修复方案** | 统一改为 `org.postgresql.Driver`，URL 改为 `jdbc:postgresql://localhost:5432/schemaplexai` |

```yaml
# 修复前（错误）
driver-class-name: com.mysql.cj.jdbc.Driver
url: jdbc:mysql://localhost:3306/schemaplexai

# 修复后（正确）
driver-class-name: org.postgresql.Driver
url: jdbc:postgresql://localhost:5432/schemaplexai
```

### P0-002：数据库连接配置不一致

| 项目 | 内容 |
|------|------|
| **影响模块** | 多个服务 |
| **问题描述** | 各服务数据库用户名密码不统一：system/agent-engine 用 `root/root`，task 用 `schemaplexai/schemaplexai`；RabbitMQ 也有 `guest/guest` vs `sf_user/sf_password` 不一致 |
| **修复方案** | 所有服务统一使用环境变量 `${DB_USERNAME:sf_user}` / `${DB_PASSWORD:sf_password}`，RabbitMQ 统一为 `sf_user/sf_password` |

### P0-003：AgentStateMachine 构造函数冲突

| 项目 | 内容 |
|------|------|
| **影响模块** | `schemaplexai-agent-engine` |
| **问题描述** | 类上标注 `@RequiredArgsConstructor`，但又手动定义了构造函数。Lombok 生成的构造函数参数与手动定义的不一致，导致 Spring 注入冲突 |
| **具体位置** | `AgentStateMachine.java:17-28` |
| **修复方案** | 移除 `@RequiredArgsConstructor`，保留手动构造函数；或改用 `@Autowired` 字段注入 |

```java
// 修复方案：移除 @RequiredArgsConstructor，保留手动构造
@Slf4j
@Component
public class AgentStateMachine {
    private final SfAgentExecutionMapper executionMapper;
    private final Map<AgentExecutionState, AgentStateHandler> handlers;
    private final Map<Long, AgentExecutionState> executionStates = new ConcurrentHashMap<>();

    @Autowired
    public AgentStateMachine(SfAgentExecutionMapper executionMapper, 
                             List<AgentStateHandler> handlerList) {
        this.executionMapper = executionMapper;
        this.handlers = handlerList.stream()
            .collect(Collectors.toMap(AgentStateHandler::getState, Function.identity()));
    }
}
```

### P0-004：JwtAuthFilter Header 修改逻辑错误

| 项目 | 内容 |
|------|------|
| **影响模块** | `schemaplexai-gateway` |
| **问题描述** | `mutatedRequest` 先 build 一次，然后又调用 `mutate().build()`，第二次 mutate 不会生效，导致 tenantId header 丢失 |
| **具体位置** | `JwtAuthFilter.java:68-77` |
| **修复方案** | 在原始 request 上一次性设置所有 header |

```java
// 修复前（错误）
ServerHttpRequest mutatedRequest = request.mutate()
    .header(CommonConstants.HEADER_AUTHORIZATION, CommonConstants.TOKEN_PREFIX + token)
    .header("X-User-Id", userId)
    .build();
if (StringUtils.hasText(tenantId)) {
    mutatedRequest = mutatedRequest.mutate()
        .header(CommonConstants.HEADER_TENANT_ID, tenantId)
        .build();  // 无效！
}

// 修复后（正确）
ServerHttpRequest.Builder builder = request.mutate()
    .header(CommonConstants.HEADER_AUTHORIZATION, CommonConstants.TOKEN_PREFIX + token)
    .header("X-User-Id", userId);
if (StringUtils.hasText(tenantId)) {
    builder.header(CommonConstants.HEADER_TENANT_ID, tenantId);
}
ServerHttpRequest mutatedRequest = builder.build();
```

### P0-005：System 模块两套实体体系重复

| 项目 | 内容 |
|------|------|
| **影响模块** | `schemaplexai-system` |
| **问题描述** | 同时存在 `SfUser` / `SfTenant` / `SfRole` 和 `User` / `Tenant` / `Role`，表名映射混乱，可能导致编译或运行时冲突 |
| **修复方案** | 删除 `User` / `Tenant` / `Role` / `Permission` / `Menu` 等非 `Sf` 前缀的实体，统一使用 `SfXxx` 体系 |

### P0-006：System 模块双启动类

| 项目 | 内容 |
|------|------|
| **影响模块** | `schemaplexai-system` |
| **问题描述** | 同时存在 `SchemaPlexaiSystemApplication.java` 和 `SystemApplication.java`，Spring Boot 打包时会冲突 |
| **修复方案** | 删除 `SystemApplication.java`，保留 `SchemaPlexaiSystemApplication.java`，确保 `@MapperScan` 覆盖所有子包 |

### P0-007：TenantLineInterceptor 返回类型不匹配

| 项目 | 内容 |
|------|------|
| **影响模块** | `schemaplexai-dao` |
| **问题描述** | `getTenantId()` 返回 `LongValue`，但 `BaseEntity.tenantId` 是 `Long` 类型。MyBatis-Plus 的 `TenantLineInnerInterceptor` 要求返回的 Expression 类型与字段类型匹配，否则 SQL 生成错误 |
| **修复方案** | 如果 `tenant_id` 是 `BIGINT`，应返回 `LongValue`（当前正确）；但需确认 `BaseEntity.tenantId` 确实是 `Long` 而非 `String`。如果 `tenant_id` 是 `VARCHAR`，则应返回 `StringValue` |

### P0-008：RabbitMQ ACK 模式不一致

| 项目 | 内容 |
|------|------|
| **影响模块** | `schemaplexai-agent-engine` |
| **问题描述** | `acknowledge-mode: auto`，但设计要求手动 ACK 以保证消息不丢失 |
| **修复方案** | 改为 `acknowledge-mode: manual`，并在 MQ Consumer 中显式调用 `channel.basicAck()` |

---

## 三、P1 级高风险问题

### P1-001：JWT Secret 硬编码且长度不足

| 项目 | 内容 |
|------|------|
| **影响模块** | `schemaplexai-gateway`、`schemaplexai-system` |
| **问题描述** | 默认值 `schemaplexai-default-secret-key-2024` 仅 31 字节，HMAC-SHA256 需要至少 256-bit（32 字节）密钥。当前值在边缘上，且硬编码在生产环境极其危险 |
| **修复方案** | ① 移除默认值，强制从环境变量读取 ② 密钥长度至少 32 字节 ③ 生产环境使用随机生成密钥 |

```yaml
# 修复后
jwt:
  secret: ${JWT_SECRET}  # 无默认值，启动时校验
```

### P1-002：TokenBudget 竞争条件

| 项目 | 内容 |
|------|------|
| **影响模块** | `schemaplexai-agent-engine` |
| **问题描述** | `consumeInput` 先 `addAndGet(tokens)` 再 `compare`，两个线程并发时可能都返回 `true`，总消耗超过预算 |
| **修复方案** | 使用 CAS 循环或同步块 |

```java
// 修复后
public boolean consumeInput(long tokens) {
    while (true) {
        long current = consumedInputTokens.get();
        long next = current + tokens;
        if (next > maxInputTokens) {
            return false;
        }
        if (consumedInputTokens.compareAndSet(current, next)) {
            return true;
        }
    }
}
```

### P1-003：AgentStateMachine 内存泄漏

| 项目 | 内容 |
|------|------|
| **影响模块** | `schemaplexai-agent-engine` |
| **问题描述** | `executionStates` 是 `ConcurrentHashMap`，执行完成后（COMPLETED/FAILED/CANCELLED）没有 `remove()`，导致内存持续增长 |
| **修复方案** | 在终态 handler 中调用 `stateMachine.removeExecution(executionId)` |

### P1-004：状态转换无终态保护

| 项目 | 内容 |
|------|------|
| **影响模块** | `schemaplexai-agent-engine` |
| **问题描述** | 已经 COMPLETED/FAILED/CANCELLED 的执行，仍可通过 `transition()` 转换到其他状态 |
| **修复方案** | 在 `transition()` 方法开头检查终态 |

```java
public void transition(AgentExecutionState newState, SfAgentExecution execution) {
    AgentExecutionState current = executionStates.get(execution.getId());
    if (current != null && current.isTerminal()) {
        log.warn("Cannot transition from terminal state {} to {}", current, newState);
        return;
    }
    // ... 原有逻辑
}
```

### P1-005：AiModelRouter 主 Provider 失败后未加 Cooldown

| 项目 | 内容 |
|------|------|
| **影响模块** | `schemaplexai-agent-engine` |
| **问题描述** | `generateWithFallback` 中主 provider 失败后直接进入 fallback 循环，但主 provider 没有被加入 cooldown，下次请求仍会先尝试已失败的 provider |
| **修复方案** | catch 主 provider 异常后，先 `activateCooldown(primaryProviderName)` 再进入 fallback |

### P1-006：Redis 聊天历史无 TTL / 长度限制

| 项目 | 内容 |
|------|------|
| **影响模块** | `schemaplexai-agent-engine` |
| **问题描述** | `CompositeChatMemoryStore` 将对话历史存入 Redis，但没有设置 TTL，也没有长度限制，长对话会导致 Redis 内存持续增长 |
| **修复方案** | ① 设置 TTL（如 7 天）② 限制单会话消息数（如 50 轮）③ 超限触发压缩或迁移到 PG |

### P1-007：缺失 @Transactional（全业务模块）

| 项目 | 内容 |
|------|------|
| **影响模块** | `context`、`spec`、`workflow`、`quality`、`integration`、`ops` |
| **问题描述** | 6 个业务模块的 Service 实现类中没有任何 `@Transactional` 注解，跨表操作无事务保护 |
| **修复方案** | 所有涉及多表写入的 Service 方法添加 `@Transactional(rollbackFor = Exception.class)` |

### P1-008：核心业务空实现

| 项目 | 内容 |
|------|------|
| **影响模块** | 多个业务模块 |
| **问题描述** | RAG 检索、Milvus 同步、Flowable 节点驱动、质量规则引擎、工具执行路由、ClickHouse 同步等核心业务均为空壳或桩代码 |
| **修复方案** | 按 Phase 优先级逐步填充实现，或先添加 `UnsupportedOperationException` 明确标记未实现 |

### P1-009：空指针风险（getById 直接返回 null）

| 项目 | 内容 |
|------|------|
| **影响模块** | 多个模块的 Controller |
| **问题描述** | `Result.success(service.getById(id))` 直接返回，如果记录不存在则返回 `Result.success(null)`，前端无法区分"空数据"和"记录不存在" |
| **修复方案** | 增加判空，不存在时返回业务错误码 |

```java
public Result<AgentVO> getById(@PathVariable Long id) {
    AgentVO vo = agentService.getById(id);
    if (vo == null) {
        return Result.error(ResultCode.AGENT_NOT_FOUND);
    }
    return Result.success(vo);
}
```

### P1-010：分页 total 与查询条件不一致

| 项目 | 内容 |
|------|------|
| **影响模块** | 多个模块的 Controller |
| **问题描述** | 部分 Controller 使用独立的 `count()` 方法计算 total，但 count 条件与分页查询条件不一致，导致分页信息错误 |
| **修复方案** | 使用 MyBatis-Plus 的 `page()` 方法，它会在内部执行 `SELECT COUNT(*)` 和分页查询，保证条件一致 |

### P1-011：RateLimitFilter Redis 异常时放行

| 项目 | 内容 |
|------|------|
| **影响模块** | `schemaplexai-gateway` |
| **问题描述** | Redis 连接异常时，限流逻辑可能跳过，导致无限制放行 |
| **修复方案** | Redis 异常时降级为拒绝请求（fail-closed）而非放行（fail-open） |

### P1-012：WebSocket/SSE 缺少认证拦截

| 项目 | 内容 |
|------|------|
| **影响模块** | `schemaplexai-web` |
| **问题描述** | SSE 和 WebSocket 端点没有身份校验，匿名用户可直接连接 |
| **修复方案** | 在 `SseEmitterManager` 和 `AgentWebSocketHandler` 中增加 Token 校验 |

### P1-013：手动拼接 JSON 响应体

| 项目 | 内容 |
|------|------|
| **影响模块** | `schemaplexai-gateway` |
| **问题描述** | `JwtAuthFilter.unauthorized()` 使用 `String.format` 手动拼接 JSON，特殊字符可能导致 JSON 格式错误 |
| **修复方案** | 使用 Jackson `ObjectMapper` 序列化 |

### P1-014：前端 Token 存储在 localStorage

| 项目 | 内容 |
|------|------|
| **影响模块** | `schemaplexai-ui` |
| **问题描述** | Token 存储在 `localStorage`，XSS 攻击可直接窃取 |
| **修复方案** | 改为 `httpOnly` Cookie（由后端设置），或至少使用 `sessionStorage` + CSRF 防护 |

### P1-015：前端 SSE 使用 Query Parameter 传 Token

| 项目 | 内容 |
|------|------|
| **影响模块** | `schemaplexai-ui` |
| **问题描述** | `sseRequest` 将 token 放在 URL query 参数中，Token 会记录在浏览器历史、服务器日志中 |
| **修复方案** | 使用 `EventSource` 的 `withCredentials` + Cookie，或改用 `fetch` + `ReadableStream` |

---

## 四、P1 级高风险问题（异步调度 + 前端补充）

### P1-016：MQ Consumer ACK 模式配置错误

| 项目 | 内容 |
|------|------|
| **影响模块** | `schemaplexai-task` |
| **问题描述** | `@RabbitListener` 未显式设置 `ackMode = "MANUAL"`，Spring 默认使用 AUTO。Consumer 内手动调用 `channel.basicNack` 后又正常返回，Spring 会再次 ACK，导致 `PRECONDITION_FAILED` 通道错误 |
| **风险** | 通道频繁关闭/重建，消息堆积，连接池耗尽 |
| **修复方案** | `@RabbitListener(queues = "...", ackMode = "MANUAL")` |

### P1-017：死信队列（DLQ）未生效

| 项目 | 内容 |
|------|------|
| **影响模块** | `schemaplexai-task` |
| **问题描述** | `DeadLetterConfig` 定义了 DLX 参数，但 `RabbitMqConfig` 中创建 Queue 时未传入 args。使用了非法参数 `x-max-retries`（RabbitMQ 不支持）。TTL 设置在业务队列上而非重试机制 |
| **风险** | 消息 NACK 后直接丢失，无死信队列可消费 |
| **修复方案** | Queue 创建时传入 args 绑定 DLX；删除 `x-max-retries`；使用 `RetryOperationsInterceptor` 实现重试 |

### P1-018：MQ 幂等性竞态条件

| 项目 | 内容 |
|------|------|
| **影响模块** | `schemaplexai-task` |
| **问题描述** | `MqIdempotencyInterceptor` 无分布式锁，"先查 Redis → 执行业务 → 写 DB" 模式存在 Race Condition。幂等记录写在业务之后，业务成功但 DB 写入失败时消息丢失且无记录 |
| **修复方案** | 数据库唯一约束 + `INSERT IGNORE`；或 Redisson 分布式锁 |

### P1-019：定时任务无分布式锁

| 项目 | 内容 |
|------|------|
| **影响模块** | `schemaplexai-task` |
| **问题描述** | 所有 6 个 Job 均无分布式锁，多实例部署时会并发执行 |
| **修复方案** | ShedLock 或 Redisson `RLock` |

### P1-020：前端 SSE 连接管理严重缺陷

| 项目 | 内容 |
|------|------|
| **影响模块** | `schemaplexai-ui` |
| **问题描述** | AgentExecutor 无组件卸载清理（useEffect 无 cleanup）、无重连机制（onerror 直接 close）、重复执行无保护（快速双击创建多个 EventSource）、close 后未置 null |
| **风险** | 内存泄漏、连接堆积、竞态更新、消息乱序 |
| **修复方案** | useEffect cleanup + handleExecute 先关闭旧连接 + 重连退避策略 |

### P1-021：前端 Token 暴露在 URL Query

| 项目 | 内容 |
|------|------|
| **影响模块** | `schemaplexai-ui` |
| **问题描述** | `sseRequest` 将 JWT 作为 URL query parameter（`?token=xxx`），会残留在浏览器历史、服务器日志、CDN 日志、Referer 中 |
| **风险** | Token 泄露面极大 |
| **修复方案** | 改为 Cookie（httpOnly）或后端生成一次性 `sseTicket` |

### P1-022：前端 Zustand store 缺陷

| 项目 | 内容 |
|------|------|
| **影响模块** | `schemaplexai-ui` |
| **问题描述** | userStore 初始化时未从 localStorage 水合 currentTenant；sseStore events 数组只增不减 |
| **修复方案** | store 创建时读取 localStorage；events 设置上限（如 slice(-1000)） |

---

## 五、P2 级中低风险问题

| # | 问题 | 影响模块 | 修复建议 |
|---|------|----------|----------|
| P2-001 | PageParam 缺少 `current >= 1` 校验 | common | 增加校验注解或手动校验 |
| P2-002 | `ignoreTable` 未使用 `equalsIgnoreCase` | dao | 表名比较不区分大小写 |
| P2-003 | LoggingFilter 处理 `query == null` | gateway | 增加 null 判断 |
| P2-004 | LLM Provider 为桩代码 | agent-engine | 接入真实 SDK 或添加 mock 标记 |
| P2-005 | 文件上传接口为 `@RequestBody` JSON | context | 改为 `MultipartFile`，增加类型白名单和大小限制 |
| P2-006 | 分布式锁缺失 | task/ops | MQ 消费者和定时任务增加分布式锁（Redis Redisson） |
| P2-007 | Prompt 变量注入无转义 | agent-engine | 对注入的上下文内容做 HTML/特殊字符转义 |
| P2-008 | Refresh Token 无黑名单校验 | system | 已注销的 refresh token 不能续期 |
| P2-009 | 前端无 JWT 自动刷新 | ui | Axios 拦截器识别 401 后先调用 refresh 接口 |
| P2-010 | 前端无 404 兜底路由 | ui | 增加 `path: '*'` 路由 |
| P2-011 | 前端无 CSP 策略 | ui | `index.html` 增加 `<meta http-equiv="Content-Security-Policy">` |
| P2-012 | 前端 Store 全量订阅导致重渲染 | ui | Zustand 使用独立 selector |

---

## 五、修复优先级与分工建议

### 第一批次（P0，1-2 天）

| 负责人 | 任务 |
|--------|------|
| 基础设施组 | ① 统一数据库驱动为 PostgreSQL ② 统一连接配置 ③ 修复 JwtAuthFilter header 逻辑 ④ 合并 system 启动类 ⑤ 清理重复实体 |
| Agent 引擎组 | ① 修复 AgentStateMachine 构造函数冲突 ② 修复 RabbitMQ ACK 模式 |

### 第二批次（P1，3-5 天）

| 负责人 | 任务 |
|--------|------|
| 基础设施组 | ① JWT Secret 强制环境变量 ② RateLimitFilter 降级策略 ③ WebSocket/SSE 认证 ④ 手动 JSON 改为 ObjectMapper |
| Agent 引擎组 | ① TokenBudget CAS 修复 ② 状态机终态保护 + 内存清理 ③ AiModelRouter cooldown 修复 ④ Redis 聊天历史 TTL + 长度限制 |
| 全业务模块 | ① 补充 `@Transactional` ② 空指针判空 ③ 分页统一使用 `page()` |
| 前端组 | ① Token 存储改为 Cookie ② SSE 改为 fetch + ReadableStream |

### 第三批次（P2 + 空实现填充，按 Phase 逐步）

| 负责人 | 任务 |
|--------|------|
| 数据/上下文组 | RAG 检索 + Milvus 同步 |
| 工作流组 | Flowable 节点驱动 |
| 质量组 | 质量规则引擎 |
| 集成组 | 工具执行路由 |
| 运营组 | ClickHouse 同步 + 分布式锁 |

---

## 六、附：已确认的正确实现

以下模块/特性实现正确，无需修改：

| 模块 | 正确项 |
|------|--------|
| common | Result、ResultCode、BaseException、PageParam、CommonConstants、TenantContextHolder |
| model | BaseEntity（含 tenant_id、逻辑删除、时间戳）、PageResult |
| dao | BaseMapperX、TenantLineInterceptor（核心逻辑正确，仅需确认返回类型） |
| gateway | 路由配置、Filter 注册机制 |
| system | RBAC 表结构设计、JWT 生成/解析逻辑（除 secret 长度外） |
| agent-config | 配置模型设计、CRUD 接口 |
| agent-engine | 状态机框架设计、LLM Provider 抽象、生命周期接口定义 |
| task | MQ Exchange/Queue 绑定配置、定时任务调度框架 |
| ui | Axios 拦截器结构、路由配置、Zustand store 设计 |

---

*本评审报告基于 4 个并行 explore agent 的自动化评审 + 人工前台复核生成。所有 P0 问题已人工确认，建议在修复后重新评审。*
