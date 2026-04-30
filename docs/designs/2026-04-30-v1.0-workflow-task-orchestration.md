---
topic: workflow-task-orchestration
stage: design
version: v1.0
status: 草稿
supersedes: ""
---

# SchemaPlexAI — Workflow 双轨任务编排设计

> **主题**：`workflow-task-orchestration`
> **阶段**：`design`
> **版本**：v1.0
> **状态**：草稿
> **日期**：2026-04-30
> **关联文档**：`docs/designs/agent-runtime-task-board.md` (v1.0), `docs/designs/system-architecture.md` (v1.1)

---

## 变更历史

| 版本 | 日期 | 变更内容 | 作者 |
|------|------|---------|------|
| v1.0 | 2026-04-30 | 初始创建 | — |

---

## 一、背景

SchemaPlexAI 的 `schemaplexai-workflow` 模块当前已具备基础工作流引擎：

- `SfWorkflowTemplate`：定义流程模板，以 JSON 数组描述节点配置
- `SfWorkflowInstance`：流程实例，记录运行状态
- `SfWorkflowNodeExecution`：节点执行记录
- `WorkflowNodeEngine`：策略模式执行器注册表，支持 `HTTP`、`SCRIPT` 节点类型
- `Flowable`：已引入但仅通过 `FlowableDelegateAdapter` 做桥接，未深度使用

### 1.1 现有缺口

| 缺口 | 现状 | 影响 |
|------|------|------|
| 人工任务节点 | 无 `HUMAN_TASK` 节点执行器 | 流程无法暂停等待人工审批/输入 |
| 任务收件箱 | 无任务分配与认领机制 | 审批、审核类需求无法落地 |
| Agent 任务状态机 | MQ consumer 全为 stub | Agent 执行无状态跟踪、无重试、无调度 |
| 任务面板 UI | WorkflowCenter 仅为模板管理页 | 用户无法查看/操作自己的任务 |

### 1.2 参考方案

| 方案 | 核心特点 | 可借鉴点 |
|------|---------|---------|
| **Symphony (OpenAI)** | 后台自动编排，状态机 `Unclaimed→Claimed→Running→RetryQueued→Released`，轮询-分派-落地 | Agent 任务状态机、指数退避重试、工作空间隔离 |
| **MultiCa** | Agent 作为团队成员出现在任务面板，看板式交互，WebSocket 实时推送 | 人+Agent 统一面板、任务卡片设计、实时活动流 |

**决策**：在现有 Workflow 模块上扩展双轨编排，人工管线借鉴 MultiCa 面板交互理念，自动管线借鉴 Symphony 后台状态机。

---

## 二、目标

1. **人工任务管线**：支持流程执行到某节点时暂停，创建可认领/提交/转派的人工任务
2. **自动任务管线**：支持 Agent 任务的独立状态机、MQ 分派、指数退避重试
3. **任务面板**：前端看板 + 收件箱，支持人+Agent 任务统一浏览
4. **双轨交汇**：一个 Workflow 可混合编排人工节点与自动节点

---

## 三、方案

### 3.1 总体架构

```
                        Workflow Instance
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
   Node N: HTTP        Node N+1: HUMAN_TASK   Node N+2: AGENT_TASK
        │                     │                     │
        ▼                     ▼                     ▼
   HttpNodeExecutor    HumanTaskNodeExecutor   发送 MQ
        │                     │                     │
        │                创建 SfHumanTask         sf.agent.execute.queue
        │                     │                     │
        │                用户认领/提交              ▼
        │                     │              AgentTaskDispatcher
        │                     │                     │
        │                回调恢复流程          依赖/锁/Agent 检查
        │                     │                     │
        │                     │                     ▼
        │                     │               SfAgentTask 状态机
        │                     │                     │
        └─────────────────────┴─────────────────────┘
                              │
                              ▼
                    WorkflowNodeEngine
                    (顺序执行，失败即停)
```

### 3.2 数据模型

#### 3.2.1 `sf_human_task` — 人工任务

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK | 主键 |
| tenant_id | BIGINT | NOT NULL | 租户ID（自动注入） |
| workflow_instance_id | BIGINT | FK | 所属流程实例 |
| node_execution_id | BIGINT | FK | 所属节点执行记录 |
| task_type | VARCHAR(32) | NOT NULL | APPROVAL / REVIEW / INPUT / DELEGATION |
| title | VARCHAR(256) | NOT NULL | 任务标题 |
| description | TEXT | | 任务描述 |
| assignee_id | BIGINT | | 被指派人（空=待认领） |
| assignee_role_id | BIGINT | | 指派角色（成员可认领） |
| form_schema_json | TEXT | | 动态表单 JSON Schema |
| result_json | TEXT | | 用户提交结果 |
| status | VARCHAR(32) | NOT NULL DEFAULT 'PENDING' | PENDING / CLAIMED / COMPLETED / TRANSFERRED / TIMEOUT |
| priority | INT | DEFAULT 3 | 1-5，5=最高 |
| due_time | TIMESTAMP | | 截止时间 |
| claimed_at | TIMESTAMP | | 认领时间 |
| completed_at | TIMESTAMP | | 完成时间 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | |
| updated_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | |
| created_by | BIGINT | | |
| updated_by | BIGINT | | |
| deleted | INT | DEFAULT 0 | 逻辑删除 |

**索引**：
- `idx_human_task_assignee` (assignee_id, status)
- `idx_human_task_instance` (workflow_instance_id)
- `idx_human_task_tenant_status` (tenant_id, status, priority, due_time)

#### 3.2.2 `sf_agent_task` — 自动任务

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK | 主键 |
| tenant_id | BIGINT | NOT NULL | 租户ID |
| workflow_instance_id | BIGINT | FK | 所属流程实例（可为空=独立任务） |
| node_execution_id | BIGINT | FK | 所属节点执行记录 |
| task_name | VARCHAR(256) | NOT NULL | 任务名称 |
| agent_type | VARCHAR(32) | | PLANNER / CODER / TESTER / REVIEWER |
| status | VARCHAR(32) | NOT NULL DEFAULT 'UNCLAIMED' | UNCLAIMED / CLAIMED / RUNNING / RETRY_QUEUED / COMPLETED / FAILED |
| input_json | TEXT | | 输入参数 |
| output_json | TEXT | | 输出结果 |
| retry_count | INT | DEFAULT 0 | 已重试次数 |
| max_retry | INT | DEFAULT 3 | 最大重试次数 |
| resource_keys | VARCHAR(512) | | 资源锁key列表（逗号分隔） |
| claimed_by | VARCHAR(128) | | 认领的 worker ID |
| started_at | TIMESTAMP | | |
| completed_at | TIMESTAMP | | |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | |
| updated_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | |
| deleted | INT | DEFAULT 0 | 逻辑删除 |

**索引**：
- `idx_agent_task_status` (status, tenant_id)
- `idx_agent_task_instance` (workflow_instance_id)

### 3.3 状态机

#### 人工任务

```
                    ┌─transfer()─────────────────────┐
                    │                                 ▼
┌──────┐  claim()  ┌────────┐  submit()  ┌──────────┐
│PENDING│─────────►│CLAIMED │───────────►│COMPLETED │
└──────┘           └────────┘            └──────────┘
   │
   │ timeout()
   ▼
┌────────┐
│TIMEOUT │
└────────┘
```

#### 自动任务

```
┌─────────┐ dispatch() ┌─────────┐ start() ┌─────────┐
│UNCLAIMED│───────────►│ CLAIMED │────────►│ RUNNING │
└─────────┘            └─────────┘         └────┬────┘
                                                │
                    ┌───────────────────────────┼──success──►┌──────────┐
                    │                           │            │COMPLETED │
                    │                           │            └──────────┘
                    │                        failure
                    │                           │
                    │                           ▼
                    │                    ┌─────────────┐
                    │                    │RETRY_QUEUED │
                    │                    └──────┬──────┘
                    │                           │ retry()
                    │                           │ (指数退避)
                    │                           ▼
                    │                    ┌─────────┐
                    └────────────────────│ RUNNING │
                                         └────┬────┘
                                              │
                                              │ max retry exceeded
                                              ▼
                                        ┌──────────┐
                                        │  FAILED  │
                                        └──────────┘
```

**退避策略**：`delay = min(10000 * 2^(retry_count), max_backoff_ms)`，默认 `max_backoff_ms = 300000`（5分钟）

### 3.4 后端设计

#### 3.4.1 实体与数据访问层

在 `schemaplexai-workflow` 模块新增：

```
entity/
  SfHumanTask.java          — 人工任务实体
  SfAgentTask.java          — 自动任务实体
mapper/
  SfHumanTaskMapper.java    — extends BaseMapperX<SfHumanTask>
  SfAgentTaskMapper.java    — extends BaseMapperX<SfAgentTask>
```

#### 3.4.2 Service 层

```
service/
  HumanTaskService.java          — 接口
  AgentTaskService.java          — 接口
  impl/
    HumanTaskServiceImpl.java    — 实现
    AgentTaskServiceImpl.java    — 实现
```

**HumanTaskService 核心方法**：

```java
public interface HumanTaskService {
    SfHumanTask createFromNode(Long instanceId, Long nodeExecutionId, Map<String, Object> config);
    SfHumanTask claim(Long taskId, Long userId);
    SfHumanTask submit(Long taskId, Long userId, String resultJson);
    SfHumanTask transfer(Long taskId, Long fromUserId, Long toUserId);
    Page<SfHumanTask> pageMyTasks(Long userId, String status, PageParam pageParam);
    List<SfHumanTask> listTeamTasks(String status);
}
```

**AgentTaskService 核心方法**：

```java
public interface AgentTaskService {
    SfAgentTask createFromNode(Long instanceId, Long nodeExecutionId, Map<String, Object> config);
    SfAgentTask claim(Long taskId, String workerId);
    SfAgentTask start(Long taskId);
    SfAgentTask complete(Long taskId, String outputJson);
    SfAgentTask fail(Long taskId, String errorMsg);
    SfAgentTask retry(Long taskId);
    boolean shouldRetry(SfAgentTask task);
    long getRetryDelayMs(SfAgentTask task);
}
```

#### 3.4.3 Controller 层

```
controller/
  TaskController.java  — 统一任务 API
```

**API 列表**：

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/workflow/tasks/my` | 我的任务列表（人工） |
| GET | `/workflow/tasks/team` | 团队任务（看板数据源） |
| GET | `/workflow/tasks/{id}` | 任务详情 |
| POST | `/workflow/tasks/{id}/claim` | 认领 |
| POST | `/workflow/tasks/{id}/submit` | 提交 |
| POST | `/workflow/tasks/{id}/transfer` | 转派 |
| GET | `/workflow/agent-tasks` | 自动任务列表 |
| GET | `/workflow/agent-tasks/{id}` | 自动任务详情 |
| POST | `/workflow/agent-tasks/{id}/retry` | 手动重试 |

#### 3.4.4 节点执行器

新增 `HumanTaskNodeExecutor`：

```java
@Component
public class HumanTaskNodeExecutor implements NodeExecutor {

    private final HumanTaskService humanTaskService;

    @Override
    public String getNodeType() {
        return "HUMAN_TASK";
    }

    @Override
    public NodeExecutionResult execute(Map<String, Object> input, String tenantId) {
        Long instanceId = (Long) input.get("instanceId");
        Long nodeExecutionId = (Long) input.get("nodeExecutionId");

        SfHumanTask task = humanTaskService.createFromNode(instanceId, nodeExecutionId, input);

        // 返回 paused 状态，WorkflowInstanceService 检测到后暂停后续节点执行
        return NodeExecutionResult.paused("Waiting for human task: " + task.getId());
    }
}
```

**WorkflowInstanceServiceImpl.trigger() 修改**：

执行每个节点后检查结果：
- 如果 `result.isPaused()` → 实例状态设为 `SUSPENDED`，记录当前节点，退出
- 人工任务提交后，调用 `workflowInstanceService.resume(instanceId)` 从下一节点继续

#### 3.4.5 Agent 任务调度器

改造 `schemaplexai-task` 模块的 `AgentExecuteDispatcher`：

```java
@RabbitListener(queues = "sf.agent.execute.queue")
public void onAgentExecuteMessage(Message message) {
    AgentTaskMessage msg = parse(message);
    SfAgentTask task = agentTaskService.getById(msg.getTaskId());

    // 1. 依赖检查
    if (!dependencyService.allCompleted(task.getDependsOn())) {
        requeue(task, 30_000);
        return;
    }

    // 2. 资源锁检查
    if (!resourceLock.tryAcquire(task.getResourceKeys(), 60_000)) {
        requeue(task, 10_000);
        return;
    }

    // 3. 查找空闲 Agent slot
    AgentSlot slot = agentPool.findAvailable(task.getRequiredCapabilities());
    if (slot == null) {
        requeue(task, 5_000);
        return;
    }

    // 4. 状态流转：UNCLAIMED → CLAIMED → RUNNING
    agentTaskService.claim(task.getId(), slot.getWorkerId());
    agentTaskService.start(task.getId());

    // 5. 异步执行
    slot.executeAsync(task, this::onTaskComplete, this::onTaskFail);
}
```

### 3.5 前端设计

#### 3.5.1 页面结构

```
schemaplexai-ui/src/
  api/
    task.ts                    # 任务管理 API
  pages/
    TaskPanel/
      index.tsx                # 入口页面
      TaskKanban.tsx           # 看板视图
      TaskInbox.tsx            # 收件箱列表
      TaskDetail.tsx           # 详情抽屉
      task.type.ts             # TypeScript 类型
  router/
    index.tsx                  # 新增 /tasks 路由
```

#### 3.5.2 类型定义

```typescript
// task.type.ts
export type HumanTaskStatus = 'PENDING' | 'CLAIMED' | 'COMPLETED' | 'TRANSFERRED' | 'TIMEOUT'
export type AgentTaskStatus = 'UNCLAIMED' | 'CLAIMED' | 'RUNNING' | 'RETRY_QUEUED' | 'COMPLETED' | 'FAILED'
export type TaskType = 'APPROVAL' | 'REVIEW' | 'INPUT' | 'DELEGATION'

export interface HumanTask {
  id: string
  title: string
  description?: string
  taskType: TaskType
  status: HumanTaskStatus
  priority: number
  assigneeId?: string
  assigneeName?: string
  dueTime?: string
  claimedAt?: string
  completedAt?: string
  formSchema?: Record<string, unknown>
  result?: Record<string, unknown>
}

export interface AgentTask {
  id: string
  taskName: string
  agentType: string
  status: AgentTaskStatus
  retryCount: number
  maxRetry: number
  claimedBy?: string
  startedAt?: string
  completedAt?: string
}
```

#### 3.5.3 看板设计

三列布局：
- **待办** — `PENDING` 状态任务，显示认领按钮
- **进行中** — `CLAIMED` 状态任务，显示处理人
- **已完成** — `COMPLETED` / `TIMEOUT` 状态

卡片内容：标题、优先级标签（颜色区分）、截止时间倒计时、指派人头像。

操作：
- 待办卡片：点击"认领" → 状态变为进行中
- 进行中卡片：点击"打开" → 弹出详情抽屉（表单 + 提交按钮）
- 拖拽：不支持跨列拖拽（状态由后端操作驱动）

---

## 四、验收标准

| 编号 | 验收项 | 验证方式 |
|------|--------|---------|
| AC-1 | Workflow 模板可配置 `HUMAN_TASK` 节点 | 创建模板，node_config_json 包含 HUMAN_TASK 节点，触发后生成 sf_human_task 记录 |
| AC-2 | 人工任务可被用户认领、提交、转派 | 调用 claim/submit/transfer API，数据库状态正确流转 |
| AC-3 | 前端任务面板可展示三列看板 | 浏览器访问 /tasks，看到待办/进行中/已完成三列 |
| AC-4 | Agent 任务状态机完整流转 | MQ 消息触发后，sf_agent_task 记录经历 UNCLAIMED→CLAIMED→RUNNING→COMPLETED |
| AC-5 | Agent 失败自动重试（指数退避） | 模拟 Agent 失败，retry_count 递增，重试间隔符合公式 |
| AC-6 | 混合 Workflow 正确执行 | 一个模板包含 HTTP + HUMAN_TASK + AGENT_TASK，HTTP 自动执行，HUMAN_TASK 暂停，人工提交后 AGENT_TASK 继续 |
| AC-7 | 单元测试覆盖率 ≥ 80% | `mvn test` 报告覆盖 HumanTaskServiceImpl 和 AgentTaskServiceImpl |

---

## 五、依赖与风险

### 5.1 依赖项

| 依赖项 | 状态 | 说明 |
|--------|------|------|
| `schemaplexai-workflow` 现有模块 | 已存在 | 需在现有表和代码上扩展，不破坏已有 HTTP/SCRIPT 节点 |
| `schemaplexai-task` MQ 基础设施 | 已存在 | RabbitMQ consumer stub 需改造为真实逻辑 |
| `schemaplexai-agent-engine` | 已存在 | AgentPool / AgentSlot 接口需定义或 mock |
| 前端路由系统 | 已存在 | React Router 已配置，需新增 /tasks 路由 |

### 5.2 风险

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| Flowable 冲突 | 中 | 本设计不依赖 Flowable BPMN，仅使用自研节点引擎，避免冲突 |
| 资源死锁 | 高 | Agent 调度器实现锁超时（60s TTL），超时自动释放 |
| 并发认领竞争 | 中 | claim 操作使用数据库乐观锁（version 字段或 CAS update） |
| 前端 Ant Design 版本 | 低 | 使用现有 Ant Design 5 组件（Card, Table, Tag, Modal, Form） |

---

## 六、相关文档

- `docs/designs/agent-runtime-task-board.md` — Agent Runtime 与 Task Board 总体设计
- `docs/designs/system-architecture.md` — 系统架构
- `wiki/entities/workflow.md` — Workflow 实体文档
- `wiki/services/workflow-node-engine.md` — 节点引擎文档
