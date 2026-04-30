---
topic: workflow-engine
stage: spec
version: v1.0
status: 草稿
supersedes: ""
---

# 工作流引擎技术规格

> **主题**: `workflow-engine`
> **阶段**: `spec`
> **版本**: v1.0
> **状态**: 草稿
> **日期**: 2026-04-30
> **范围**: `schemaplexai-workflow` 服务

---

## 1. 概述

工作流引擎支持两种编排模式：

1. **BPMN 工作流**: 基于 Flowable 引擎的标准业务流程（审批、会签、定时触发）
2. **AI 节点工作流**: 自定义节点类型（Agent、HTTP、Script、人工审批），支持动态编排

本规格基于已有框架代码（`WorkflowNodeEngine`、`FlowableDelegateAdapter`）补充缺失节点执行器。

## 2. 架构视图

```
┌─────────────────────────────────────────────────────────┐
│              WorkflowTemplateController                  │
│                 WorkflowInstanceController               │
└─────────────────────┬───────────────────────────────────┘
                      │
    ┌─────────────────┼─────────────────┐
    │                 │                 │
    ▼                 ▼                 ▼
┌────────────┐ ┌──────────────┐ ┌──────────────┐
│ Flowable   │ │ WorkflowNode │ │ Workflow     │
│ Engine     │ │   Engine     │ │ Instance     │
│ (BPMN)     │ │ (AI Nodes)   │ │ Service      │
└─────┬──────┘ └──────┬───────┘ └──────────────┘
      │               │
      │    ┌──────────┼──────────┬──────────┐
      │    │          │          │          │
      │    ▼          ▼          ▼          ▼
      │ ┌──────┐ ┌────────┐ ┌────────┐ ┌──────────┐
      │ │Agent │ │  HTTP  │ │ Script │ │ Human    │
      │ │ Node │ │  Node  │ │  Node  │ │ Approval │
      │ └──────┘ └────────┘ └────────┘ └──────────┘
      │
      ▼
┌─────────────────────────────────────────┐
│     FlowableDelegateAdapter             │
│  (桥接 Flowable 到 WorkflowNodeEngine)   │
└─────────────────────────────────────────┘
```

## 3. 节点类型规格

### 3.1 节点接口

```java
public interface NodeExecutor {
    String getNodeType();
    NodeExecutionResult execute(NodeExecutionContext context);
}
```

### 3.2 Agent 节点

**类型**: `AGENT`

**职责**: 调用 Agent 执行引擎完成 AI 任务

**配置参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| agentId | Long | 是 | 要调用的 Agent ID |
| prompt | String | 是 | 输入提示词（支持变量替换） |
| timeoutSeconds | Integer | 否 | 超时时间（默认 300） |
| waitForCompletion | Boolean | 否 | 是否等待完成（默认 true） |

**执行流程**:

1. 变量替换：`prompt` 中的 `${input.xxx}` 替换为上游节点输出
2. 调用 `agentEngine.createExecution(agentId, prompt, tenantId)`
3. 如果 `waitForCompletion=true`，轮询状态至终端状态
4. 返回执行结果（response / tokenUsage / status）

**状态转换**:

```
PENDING → RUNNING → COMPLETED
              ↓
           FAILED / TIMEOUT
```

### 3.3 HTTP 节点

**类型**: `HTTP`

**职责**: 发送 HTTP 请求调用外部服务

**配置参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| url | String | 是 | 请求 URL（支持变量） |
| method | String | 是 | GET / POST / PUT / DELETE |
| headers | Map | 否 | 请求头 |
| body | String | 否 | 请求体（支持变量） |
| timeoutSeconds | Integer | 否 | 默认 30 |

**执行流程**:

1. 使用 WebClient 发送请求（非阻塞）
2. 超时处理：超时后标记为 FAILED
3. 响应解析：将 JSON 响应转为 Map 供下游使用

### 3.4 Script 节点

**类型**: `SCRIPT`

**职责**: 执行脚本代码

**配置参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| language | String | 是 | `groovy` / `javascript` |
| script | String | 是 | 脚本代码 |
| timeoutSeconds | Integer | 否 | 默认 60 |

**安全约束**:

- 使用沙箱执行（Groovy Sandbox / Nashorn with security manager）
- 禁止访问文件系统、网络、反射
- 仅允许访问白名单类

### 3.5 人工审批节点

**类型**: `HUMAN_APPROVAL`

**职责**: 暂停工作流等待人工审批

**配置参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| approverRole | String | 是 | 审批角色 |
| timeoutHours | Integer | 否 | 默认 24 |

**执行流程**:

1. 创建审批任务（写入 `sf_approval` 表）
2. 工作流状态设为 `WAITING_APPROVAL`
3. 审批完成后，通过回调继续工作流
4. 超时未审批，自动拒绝

## 4. 工作流实例生命周期

```
DRAFT → PUBLISHED → RUNNING → COMPLETED
                          ↓
                       FAILED / CANCELLED
```

| 状态 | 说明 |
|------|------|
| `DRAFT` | 模板编辑中，不可执行 |
| `PUBLISHED` | 模板已发布，可创建实例 |
| `RUNNING` | 实例执行中 |
| `WAITING_APPROVAL` | 等待人工审批 |
| `COMPLETED` | 正常完成 |
| `FAILED` | 执行失败 |
| `CANCELLED` | 用户取消 |

## 5. 数据模型

### 5.1 sf_workflow_template

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| tenant_id | BIGINT | 租户隔离 |
| name | VARCHAR | 模板名称 |
| definition | JSONB | 节点定义数组 |
| status | VARCHAR | DRAFT / PUBLISHED |
| version | INT | 版本号 |

### 5.2 sf_workflow_instance

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| template_id | BIGINT | 关联模板 |
| tenant_id | BIGINT | 租户隔离 |
| status | VARCHAR | 实例状态 |
| input_data | JSONB | 输入参数 |
| output_data | JSONB | 输出结果 |
| start_time | TIMESTAMP | 开始时间 |
| end_time | TIMESTAMP | 结束时间 |

### 5.3 sf_workflow_node_execution

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| instance_id | BIGINT | 关联实例 |
| node_id | VARCHAR | 节点 ID（模板内唯一） |
| node_type | VARCHAR | AGENT / HTTP / SCRIPT / HUMAN_APPROVAL |
| status | VARCHAR | 执行状态 |
| input_data | JSONB | 节点输入 |
| output_data | JSONB | 节点输出 |
| error_message | TEXT | 错误信息 |

## 6. API 接口

### 6.1 模板管理

```http
POST   /workflow/templates              # 创建模板
GET    /workflow/templates              # 列表
GET    /workflow/templates/{id}         # 详情
PUT    /workflow/templates/{id}         # 更新
POST   /workflow/templates/{id}/publish # 发布
DELETE /workflow/templates/{id}         # 删除
```

### 6.2 实例执行

```http
POST   /workflow/instances              # 创建并触发实例
GET    /workflow/instances              # 列表
GET    /workflow/instances/{id}         # 详情
POST   /workflow/instances/{id}/cancel  # 取消
POST   /workflow/instances/{id}/approve # 人工审批通过
POST   /workflow/instances/{id}/reject  # 人工审批拒绝
```

## 7. 与 Flowable 的边界

| 场景 | 使用 Flowable | 使用 WorkflowNodeEngine |
|------|--------------|------------------------|
| 人工审批流程 | 是 | 否 |
| 会签/或签 | 是 | 否 |
| 定时触发 | 是 | 否 |
| Agent 执行 | 否 | 是 |
| HTTP 调用 | 否 | 是 |
| 脚本执行 | 否 | 是 |
| 复杂条件分支 | 是 | 否 |

**桥接方式**: `FlowableDelegateAdapter` 将 Flowable ServiceTask 代理到 `WorkflowNodeEngine`

## 8. 非功能需求

| 指标 | 目标 |
|------|------|
| 工作流触发延迟 | P99 < 500ms |
| 节点执行超时 | 可配置，默认 30s（HTTP）/ 300s（Agent） |
| 并发实例数 | 单服务 100+ |
| 失败重试 | 支持 3 次重试，指数退避 |

## 9. 相关文档

- `docs/plans/project-plan.md`（Phase 6）
- `docs/plans/unified-dev-plan.md`（Task 38）
- `docs/specs/agent-execution-engine.md`
