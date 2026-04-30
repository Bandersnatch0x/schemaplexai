---
topic: agent-runtime-task-board
stage: design
version: v1.0
status: 草稿
supersedes: ""
---

# SchemaPlexAI — Agent Runtime 平台与 Task Board 设计

> **主题**：`agent-runtime-task-board`
> **阶段**：`design`
> **版本**：v1.0
> **状态**：草稿
> **日期**：2026-04-30
> **关联文档**：`docs/designs/system-architecture.md` (v1.1)

---

## 变更历史

| 版本 | 日期 | 变更内容 | 作者 |
|------|------|---------|------|
| v1.0 | 2026-04-30 | 初始创建 | — |

---

## 一、背景与核心决策

### 1.1 原方案问题

前期曾调研将 **Cursor SDK** (`@cursor/sdk`) 作为外部 Runtime 接入 SchemaPlexAI。经验证，该方案存在不可接受的第三方锁定：

| 约束项 | 验证结论 |
|--------|---------|
| API Key | `CURSOR_API_KEY` 强制要求，本地/云端运行时共用同一 Key |
| Self-Hosted Workers | 仅执行节点在自有基础设施，任务调度仍通过 Cursor 控制平面 |
| 平台依赖 | 即使设置 `usePrivateWorker: true`，任务分派仍走 Cursor API |
| 企业门槛 | Service Account API Key reportedly 需 Enterprise Plan |

**结论**：Cursor SDK 是 Cursor 平台的延伸，不是独立运行时。若底线是"不受第三方约束"，此路不可行。

### 1.2 方向修正

从"接入第三方 Agent Runtime（Cursor SDK）"转向"**建设自有 Agent Runtime 基础设施**"：

- **沙箱层**：开源隔离执行环境（OpenSandbox），自主可控
- **Agent 智能层**：保留并升级 SchemaPlexAI 自有 `agent-engine` 状态机
- **编排层**：新增 Task Board（看板），支持人工/AI 自动任务分配
- **工具执行层**：从"空实现"升级为沙箱内真实执行（文件、浏览器、终端、代码）

---

## 二、开源沙箱方案对比

针对 `sandbox + computer use + observability + stateful + 无第三方约束` 五维需求，评估主流开源方案：

| 方案 | 许可证 | 沙箱隔离 | Computer Use | 有状态 | Java SDK | 第三方依赖 | 评估 |
|------|--------|---------|-------------|--------|----------|-----------|------|
| **OpenSandbox** (Alibaba) | Apache 2.0 | Docker/gVisor/Kata/Firecracker | VNC + Chrome/Playwright | Session/Snapshot/Fork | 原生 Maven | 无 | **首推** |
| **E2B** | Apache 2.0 | Firecracker microVM | 无桌面 | 最近 Beta | 无（Python/JS） | 无 | 代码执行强，缺 Computer Use |
| **Daytona** | AGPL-3.0 | Docker | Linux/Win/macOS 桌面 | 持久卷 | 无（Go/JS） | 无 | 能力全，AGPL 企业法务风险 |
| **OpenHands** | MIT | Docker (默认) | 依赖底层 Runtime | 依赖底层 | 无（Python） | 无 | 是 Agent 层，非沙箱层 |

### 2.1 选型：OpenSandbox

- **原生 Java SDK**：Maven Central 直接引入，与 Spring Boot 零摩擦
- **有状态为原生设计**：Session 持久化、Snapshot/Restore、Fork（COW）
- **Computer Use 内置**：VNC 桌面 + Chrome/Playwright 浏览器自动化
- **隔离级别可选**：开发用 Docker，生产切 gVisor/Kata/Firecracker
- **可观察性**：SSE 流式输出 stdout/stderr，进程生命周期管理
- **Apache 2.0**：企业可自由修改、闭源部署

---

## 三、总体架构

```
┌─────────────────────────────────────────────────────────────┐
│                    SchemaPlexAI Platform                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │  Task Board  │  │  Workflow    │  │  Spec / Quality  │  │
│  │  任务编排     │  │  (Flowable)  │  │  规约/质量门      │  │
│  └──────┬───────┘  └──────┬───────┘  └────────┬─────────┘  │
│         │                 │                    │           │
│         └─────────────────┼────────────────────┘           │
│                           ▼                                │
│              ┌────────────────────────┐                   │
│              │   Agent Runtime Plane  │   ← 新增核心层     │
│              │  （运行时控制平面）       │                   │
│              │                        │                   │
│              │  ┌──────────────────┐  │                   │
│              │  │ Runtime Registry │  │ 运行时注册中心      │
│              │  │ - 沙箱实例池管理   │  │ (Warm Pools)      │
│              │  │ - 健康检查/心跳    │  │                   │
│              │  │ - 资源配额调度     │  │                   │
│              │  └──────────────────┘  │                   │
│              │  ┌──────────────────┐  │                   │
│              │  │ Task Dispatcher  │  │ 任务分派器         │
│              │  │ (人工/自动/混合)  │  │                   │
│              │  └──────────────────┘  │                   │
│              └───────────┬────────────┘                   │
│                          │                                 │
│         ┌────────────────┼────────────────┐               │
│         ▼                ▼                ▼               │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐       │
│  │ 内部状态机   │  │ OpenSandbox │  │ 外部CLI适配 │       │
│  │ Runtime     │  │ Runtime     │  │ (可选扩展)   │       │
│  │ (Default)   │  │ (沙箱执行)   │  │             │       │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘       │
│         │                │                 │              │
│         └────────────────┼─────────────────┘              │
│                          ▼                                │
│              ┌────────────────────────┐                   │
│              │   OpenSandbox Cluster  │                   │
│              │   (K8s / Docker)       │                   │
│              │                        │                   │
│              │  ┌──────────────────┐  │                   │
│              │  │ 沙箱实例 A        │  │  ┌─────────────┐ │
│              │  │ - 文件系统        │  │  │ Browser     │ │
│              │  │ - 终端/Shell      │──┼─▶│ (Playwright)│ │
│              │  │ - 代码执行        │  │  └─────────────┘ │
│              │  │ - VNC 桌面        │  │                   │
│              │  └──────────────────┘  │                   │
│              │  ┌──────────────────┐  │                   │
│              │  │ 沙箱实例 B        │  │                   │
│              │  │ (隔离环境)        │  │                   │
│              │  └──────────────────┘  │                   │
│              └────────────────────────┘                   │
└─────────────────────────────────────────────────────────────┘
```

---

## 四、核心组件设计

### 4.1 Runtime 注册中心 (`RuntimeRegistry`)

```java
public interface RuntimeRegistry {
    // 运行时注册/注销
    void register(RuntimeRegistration registration);
    void unregister(String runtimeId);

    // 运行时发现
    List<RuntimeInstance> findHealthyRuntimes();
    List<RuntimeInstance> findByCapability(String capabilityTag);
    RuntimeInstance findLeastLoaded(String capabilityTag);

    // 健康检查
    void heartbeat(String runtimeId, RuntimeHealth health);
    List<RuntimeInstance> findExpired(Duration threshold);
}

public class RuntimeInstance {
    private String runtimeId;
    private RuntimeType type;          // INTERNAL / OPEN_SANDBOX / EXTERNAL_CLI
    private List<String> capabilities; // ["python", "browser", "git", "java"]
    private RuntimeHealth health;
    private int currentLoad;           // 当前执行任务数
    private int maxConcurrency;        // 最大并发数
}
```

### 4.2 Task Board 数据模型

```java
public class SfTask {
    private Long id;
    private Long tenantId;
    private String title;
    private String description;
    private List<String> skillTags;      // ["frontend", "security-review"]
    private TaskPriority priority;       // P0 / P1 / P2 / P3
    private TaskStatus status;           // BACKLOG / QUEUED / IN_PROGRESS / AWAITING_REVIEW / REVISING / DONE / BLOCKED
    private Long assignedToRuntimeId;    // 分配的运行时
    private Long assignedToAgentId;      // 分配的 Agent
    private TaskAssignmentType assignmentType; // MANUAL / AUTO / MIXED
    private Long specId;                 // 关联 Spec
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}

public enum TaskStatus {
    BACKLOG,          // 待办
    QUEUED,           // 已排队等待执行
    IN_PROGRESS,      // 执行中
    AWAITING_REVIEW,  // 等待人工审核（强制关卡）
    REVISING,         // Agent 根据反馈修正中
    DONE,             // 完成
    BLOCKED           // 阻塞（等待人工解阻）
}
```

### 4.3 自动分配引擎 (`AutoAssignmentEngine`)

```java
public interface AssignmentStrategy {
    RuntimeInstance assign(SfTask task, List<RuntimeInstance> candidates);
}

// 策略实现
public class CapabilityMatchingStrategy implements AssignmentStrategy {
    // 任务 skillTags 与 Runtime capabilities 交集最大化
}

public class LeastLoadedStrategy implements AssignmentStrategy {
    // 选择当前负载最低的 Runtime
}

public class RoundRobinStrategy implements AssignmentStrategy {
    // 轮询
}

public class HistoricalSuccessStrategy implements AssignmentStrategy {
    // 选择历史上处理同类任务成功率最高的 Runtime
}
```

### 4.4 沙箱执行桥接 (`OpenSandboxRuntimeAdapter`)

```java
@Component
public class OpenSandboxRuntimeAdapter implements RuntimeAdapter {

    @Autowired
    private OpenSandboxClient sandboxClient;

    @Override
    public String getRuntimeType() {
        return "OPEN_SANDBOX";
    }

    @Override
    public ExecutionResult execute(ExecutionRequest request) {
        // 1. 获取或创建沙箱实例
        SandboxSession session = sandboxClient.createSession(
            SessionConfig.builder()
                .image("schemaplexai/agent-runtime:latest")
                .cpuLimit(2)
                .memoryLimitMB(4096)
                .enableVnc(true)
                .enableBrowser(true)
                .build()
        );

        // 2. 在沙箱中执行 Agent 工具调用
        ExecutionResult result = session.execute(request.getCommand(), request.getParameters());

        // 3. 流式上报事件
        result.getEvents().forEach(event -> eventPublisher.publish(event));

        return result;
    }

    @Override
    public List<String> getCapabilities() {
        return List.of("bash", "python", "node", "browser", "git", "file");
    }
}
```

---

## 五、Task Board 交互设计

### 5.1 看板列定义

采用 **Iterative Kanban（迭代式看板）** 模式：

| 列 | 说明 | 谁可操作 |
|----|------|---------|
| **BACKLOG** | 待办任务池 | 人工创建 / Agent 自动创建 |
| **QUEUED** | 已排队，等待 Runtime 资源 | 系统自动 |
| **IN_PROGRESS** | Agent 正在执行 | 系统自动 |
| **AWAITING_REVIEW** | 执行完成，等待人工审核 | 人工操作（Approve / Request Changes） |
| **REVISING** | Agent 根据反馈修正 | 系统自动 |
| **BLOCKED** | 遇到阻塞（依赖 / 权限 / 异常） | 人工解阻 |
| **DONE** | 最终完成 | 系统自动 |

### 5.2 人工关卡（Human-in-the-Loop）

- **AWAITING_REVIEW 为强制关卡**：Agent 执行完成后必须进入此列，不可自动跳到 DONE
- **Reviewer 操作**：
  - `Approve` → 任务进入 DONE
  - `Request Changes` → 任务进入 REVISING，附带反馈评论
  - `Escalate` → 任务转给人类工程师，Agent 释放
- **与 Quality 模块集成**：进入 AWAITING_REVIEW 时自动触发 spec-drift 检测、安全扫描

### 5.3 Blocker 处理

```java
public class BlockerEscalationService {

    public void reportBlocker(Long taskId, Blocker blocker) {
        // 1. 任务移入 BLOCKED 列
        taskService.updateStatus(taskId, TaskStatus.BLOCKED);

        // 2. 创建通知
        notificationService.notify(
            blocker.getEscalateToUserIds(),
            new TaskBlockedNotification(taskId, blocker.getReason())
        );

        // 3. 释放 Runtime 资源
        runtimeRegistry.release(taskId);
    }

    public void resolveBlocker(Long taskId, String resolution, Long resolvedBy) {
        // 人工解阻后，任务回到 QUEUED 或 IN_PROGRESS
        taskService.updateStatus(taskId, TaskStatus.QUEUED);
        taskService.appendComment(taskId, "Blocker resolved by " + resolvedBy + ": " + resolution);
    }
}
```

---

## 六、与现有模块的集成

| 现有模块 | 升级点 |
|----------|--------|
| `agent-engine` | `ToolCallingStateHandler` 接入 `OpenSandboxRuntimeAdapter`，工具调用在真实沙箱中执行 |
| `agent-config` | `SfAgentToolBinding` 新增 `sandboxRequired` 字段，标记哪些工具必须在沙箱中执行 |
| `integration` | `ToolExecutionService` 新增 `SandboxToolExecutor`，统一管理沙箱工具执行 |
| `workflow` | 新增 `SandboxNodeExecutor`，BPMN 节点可直接在沙箱中执行脚本/命令 |
| `quality` | AWAITING_REVIEW 阶段自动触发 spec-drift 检测和安全扫描 |
| `spec` | Task 可关联 Spec 文档，Agent 执行前自动拉取 Spec 上下文 |

---

## 七、数据模型扩展

### 7.1 新增表

```sql
-- 运行时注册表
CREATE TABLE sf_runtime (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    runtime_id      VARCHAR(64) NOT NULL UNIQUE,
    runtime_type    VARCHAR(32) NOT NULL,  -- INTERNAL / OPEN_SANDBOX / EXTERNAL_CLI
    name            VARCHAR(128) NOT NULL,
    endpoint        VARCHAR(256),           -- 运行时访问地址
    capabilities    JSONB,                  -- ["python", "browser", "git"]
    status          VARCHAR(32) NOT NULL,   -- ONLINE / OFFLINE / DEGRADED
    max_concurrency INT NOT NULL DEFAULT 5,
    current_load    INT NOT NULL DEFAULT 0,
    last_heartbeat  TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 运行时能力标签
CREATE TABLE sf_runtime_capability (
    id          BIGSERIAL PRIMARY KEY,
    runtime_id  BIGINT NOT NULL REFERENCES sf_runtime(id),
    capability  VARCHAR(64) NOT NULL,
    UNIQUE (runtime_id, capability)
);

-- 任务表
CREATE TABLE sf_task (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    title           VARCHAR(256) NOT NULL,
    description     TEXT,
    skill_tags      JSONB,                  -- ["frontend", "security-review"]
    priority        VARCHAR(16) NOT NULL,   -- P0 / P1 / P2 / P3
    status          VARCHAR(32) NOT NULL,   -- BACKLOG / QUEUED / IN_PROGRESS / AWAITING_REVIEW / REVISING / DONE / BLOCKED
    assigned_runtime_id BIGINT REFERENCES sf_runtime(id),
    assigned_agent_id   BIGINT REFERENCES sf_agent(id),
    assignment_type VARCHAR(32),            -- MANUAL / AUTO / MIXED
    spec_id         BIGINT REFERENCES sf_spec(id),
    blocker_reason  TEXT,
    blocker_reported_at TIMESTAMP,
    created_by      BIGINT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    started_at      TIMESTAMP,
    completed_at    TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 任务评论/反馈
CREATE TABLE sf_task_comment (
    id          BIGSERIAL PRIMARY KEY,
    task_id     BIGINT NOT NULL REFERENCES sf_task(id),
    author_type VARCHAR(16) NOT NULL,   -- HUMAN / AGENT
    author_id   BIGINT,
    content     TEXT NOT NULL,
    comment_type VARCHAR(32),           -- GENERAL / REVIEW_FEEDBACK / BLOCKER_RESOLUTION
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 任务分配历史
CREATE TABLE sf_task_assignment_log (
    id              BIGSERIAL PRIMARY KEY,
    task_id         BIGINT NOT NULL REFERENCES sf_task(id),
    from_runtime_id BIGINT,
    to_runtime_id   BIGINT,
    from_status     VARCHAR(32),
    to_status       VARCHAR(32),
    assigned_by     VARCHAR(32),        -- MANUAL / AUTO / SYSTEM
    reason          TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 运行时心跳日志
CREATE TABLE sf_runtime_heartbeat (
    id          BIGSERIAL PRIMARY KEY,
    runtime_id  BIGINT NOT NULL REFERENCES sf_runtime(id),
    status      VARCHAR(32) NOT NULL,
    load        INT,
    metrics     JSONB,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
```

---

## 八、产品演进路线

### Phase 1：沙箱化执行（MVP，4-6 周）
- 引入 OpenSandbox Java SDK，本地 Docker 模式跑通
- 升级 `ToolCallingStateHandler`，让 Agent 的工具调用在沙箱中真实执行（文件读写、Shell 命令、Python 代码）
- `agent-engine` 作为唯一 Default Runtime，沙箱作为其执行后端

### Phase 2：Runtime 平台化（2-3 个月）
- 建设 `RuntimeRegistry`：沙箱实例池、预热池、心跳健康检查
- 建设 Task Board：BACKLOG/IN_PROGRESS/DONE + 人工分配
- 多租户隔离：每个 tenant 的沙箱网络/文件系统隔离

### Phase 3：Computer Use + 自动分配（3-4 个月）
- 启用 OpenSandbox 的 VNC + Playwright 能力，Agent 可操作浏览器
- 自动分配引擎：Task 技能标签 ↔ Runtime 能力标签匹配
- Iterative Kanban：引入 AWAITING_REVIEW / REVISING / BLOCKED 列

### Phase 4：生态扩展（长期）
- 外部 CLI Runtime 适配器（Claude Code、Codex 等作为可选运行时）
- 多 Runtime 调度：根据任务类型自动选择"内部状态机"或"外部 CLI"
- Snapshot/Fork：Agent 执行到关键节点可快照，支持并行探索多方案

---

## 九、风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| OpenSandbox 生态较新 | 文档/社区支持不足 | 预留 2 周预研缓冲；准备 E2B 作为 fallback（仅代码执行场景） |
| 沙箱冷启动延迟 | Agent 响应变慢 | Warm Pools 预热；区分"交互式"（低延迟）与"批量式"（可容忍延迟）任务 |
| VNC/浏览器资源消耗高 | K8s 集群压力大 | 浏览器任务单独调度到 GPU/高内存节点；非浏览器任务用轻量 Docker |
| 有状态沙箱数据丢失 | 长任务中断后无法恢复 | Snapshot 定期保存到 MinIO/S3；失败时从最近 Snapshot 恢复 |
| AGPL 替代品（Daytona）被误用 | 法务合规风险 | 明确技术选型规范：OpenSandbox（Apache 2.0）为唯一官方沙箱 |

---

## 十、关键设计决策（ADR）

### ADR-011：放弃 Cursor SDK，自建 Agent Runtime 基础设施

| 项目 | 内容 |
|------|------|
| **背景** | 原计划接入 Cursor SDK 作为外部 Runtime，经验证发现存在强制平台依赖 |
| **决策** | 放弃 Cursor SDK，采用 OpenSandbox 作为沙箱底座，自有 agent-engine 作为智能核心 |
| **理由** | ① 自主可控，不受第三方 API Key 和平台约束 ② OpenSandbox 原生 Java SDK 与现有技术栈匹配 ③ 沙箱层与智能层解耦，未来可替换 LLM 或沙箱实现 |
| **影响** | Phase 2 需增加 ~2 周用于 OpenSandbox 集成预研；删除所有 Cursor SDK 相关设计 |
| **替代方案** | 接受 Cursor 平台依赖（否决）；使用 E2B（缺 Computer Use）；使用 Daytona（AGPL 风险） |

### ADR-012：引入 Task Board 作为核心编排层

| 项目 | 内容 |
|------|------|
| **背景** | 现有系统缺少任务可视化编排和人工/AI 协作界面 |
| **决策** | 新增 Task Board 模块，作为与 Runtime 注册中心平级的核心编排层 |
| **理由** | ① 人机协作需要可视化看板 ② 自动分配需要任务-Runtime 匹配引擎 ③ Iterative Kanban 模式适合 AI Agent 迭代执行 |
| **影响** | 新增 `sf_task` 等 5 张表；前端新增 Task Board 页面；与 Workflow 模块需明确边界 |
| **替代方案** | 复用 Flowable BPMN 渲染为看板（否决：BPMN 过于 rigid，不适合 Agent 异步事件） |

---

*本文档基于 Cursor SDK 调研、开源沙箱对比、Multica/ai-agent-board 等开源项目参考编写。所有技术选型以"自主可控、渐进交付、兼容现有资产"为原则。*
