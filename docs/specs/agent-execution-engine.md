---
topic: agent-execution-engine
stage: spec
version: v1.0
status: 已批准
supersedes: ""
---

# Agent 执行引擎技术规格

> **主题**: `agent-execution-engine`
> **阶段**: `spec`
> **版本**: v1.0
> **状态**: 已批准
> **日期**: 2026-04-30
> **范围**: `schemaplexai-agent-engine` 服务核心执行逻辑

---

## 1. 概述

Agent 执行引擎是 SchemaPlexAI 的核心智能组件，负责驱动 Agent 的完整生命周期：从执行准入、状态机循环、LLM 调用、工具执行到结果交付。

本规格基于 ADR-002（自建 Runtime）和 ADR-003（LangChain4j 选型）制定。

## 2. 架构视图

```
┌─────────────────────────────────────────────────────────────┐
│              AgentRuntimeOrchestrator                        │
│                    (编排入口)                                 │
└──────────────────────┬──────────────────────────────────────┘
                       │
    ┌──────────────────┼──────────────────┐
    │                  │                  │
    ▼                  ▼                  ▼
┌────────────┐  ┌──────────────┐  ┌──────────────┐
│ Execution  │  │ AgentState   │  │ AgentLoop    │
│ Admission  │  │   Machine    │  │  Detection   │
│  Service   │  │              │  │   Service    │
└────────────┘  └──────┬───────┘  └──────────────┘
                       │
        ┌──────────────┼──────────────┐
        ▼              ▼              ▼
┌──────────────┐ ┌──────────┐ ┌──────────────┐
│  Thinking    │ │  Tool    │ │  Completed   │
│   Handler    │ │ Calling  │ │   Handler    │
│              │ │ Handler  │ │              │
└──────┬───────┘ └────┬─────┘ └──────────────┘
       │              │
       ▼              ▼
┌─────────────────────────────────────────────┐
│           LlmProvider (防腐层)               │
│    ┌──────────────┐  ┌──────────────────┐   │
│    │ LangChain4j  │  │  Direct SDK      │   │
│    │  (primary)   │  │  (fallback)      │   │
│    └──────────────┘  └──────────────────┘   │
└─────────────────────────────────────────────┘
```

## 3. 核心组件规格

### 3.1 AgentStateMachine

**职责**: 管理执行状态的生命周期转换

```java
public class AgentStateMachine {
    // 状态存储: ConcurrentHashMap<Long, AgentExecutionState>
    // 关键约束:
    // - Terminal 状态（COMPLETED/FAILED/CANCELLED）不可再转换
    // - 终端状态触发内存清理（remove from map）
}
```

**状态定义**:

| 状态 | 类型 | 说明 |
|------|------|------|
| `IDLE` | 初始 | 执行刚创建 |
| `THINKING` | 中间 | LLM 推理中 |
| `TOOL_CALLING` | 中间 | 调用外部工具 |
| `PAUSED` | 中间 | 人工暂停/等待输入 |
| `COMPLETED` | 终端 | 正常完成 |
| `FAILED` | 终端 | 执行失败 |
| `CANCELLED` | 终端 | 用户取消 |

**状态转换矩阵**:

| 当前状态 | 可转换至 |
|----------|----------|
| IDLE | THINKING, CANCELLED |
| THINKING | TOOL_CALLING, PAUSED, COMPLETED, FAILED |
| TOOL_CALLING | THINKING, PAUSED, FAILED |
| PAUSED | THINKING, CANCELLED |
| COMPLETED | —（终端） |
| FAILED | —（终端） |
| CANCELLED | —（终端） |

### 3.2 ExecutionAdmissionService（准入控制）

**职责**: 四维限流 + Token 预算检查

```java
public class ExecutionAdmissionService {
    // 维度1: 租户级并发限制
    // 维度2: Agent 级并发限制
    // 维度3: 模型级并发限制
    // 维度4: 供应商级并发限制（含冷却期）
}
```

**限流参数**（配置化）:

```yaml
agent:
  admission:
    tenant-max-concurrent: 100
    agent-max-concurrent: 10
    model-max-concurrent: 50
    provider-max-concurrent: 30
    provider-cooldown-seconds: 60
```

### 3.3 TokenBudget

**职责**: 输入/输出 Token 预算管理

```java
public class TokenBudget {
    private final AtomicLong consumedInputTokens;
    private final AtomicLong consumedOutputTokens;
    private final long maxInputTokens;
    private final long maxOutputTokens;

    // CAS 循环保证线程安全
    public boolean consumeInput(long tokens);
    public boolean consumeOutput(long tokens);
}
```

**预算策略**:

1. 预检：调用前预估输入 Token，预算不足直接拒绝
2. 后扣：调用完成后扣除实际输出 Token
3. 超限处理：先压缩记忆 → 再截断上下文 → 最终终止执行

### 3.4 ThinkingStateHandler

**职责**: LLM 调用主逻辑

**输入**: `SfAgentExecution`（含 userInput、modelConfig、conversationId）
**输出**: 状态转换至 `TOOL_CALLING` 或 `COMPLETED`

**处理流程**:

1. 构建四层 Prompt（System → Context → Memory → User）
2. 预估 Token → `tokenBudget.consumeInput()`
3. 调用 `AiModelRouter.generateWithFallback()`
4. 扣除输出 Token → `tokenBudget.consumeOutput()`
5. 存储至 `CompositeChatMemoryStore`
6. 解析响应，判断是否含工具调用

### 3.5 ToolCallingStateHandler

**职责**: 工具执行

**执行策略**:

- **并行读**：多个读工具可并发执行
- **串行写**：写工具必须串行，保证一致性
- **超时**：单工具 30 秒超时
- **错误处理**：单工具失败记录错误，不影响其他工具

**工具注册**:

```java
public interface ToolAdapter {
    String getToolName();
    ToolResult execute(Map<String, Object> params, Long tenantId);
    boolean isReadOnly();
}
```

### 3.6 AgentLoopDetectionService

**职责**: 检测执行循环

**检测策略**:

1. **哈希检测**：连续 N 次响应内容哈希相同
2. **工具序列检测**：连续 N 轮调用相同工具序列
3. **窗口大小**: 5 轮

**处理**: 检测到循环后，强制转换至 `COMPLETED`，记录循环原因

### 3.7 CompositeChatMemoryStore

**职责**: L1（Redis）+ L2（PostgreSQL）双层记忆

| 层级 | 存储 | TTL | 用途 |
|------|------|-----|------|
| L1 | Redis List | 7 天 | 活跃对话快速读取 |
| L2 | PostgreSQL | 永久 | 历史消息持久化 |

**压缩策略**:

- 当消息数 > 50 轮时触发压缩
- 压缩方式：摘要提取（保留最近 10 轮 + 历史摘要）

## 4. API 接口

### 4.1 执行启动

```http
POST /agents/{id}/execute
Content-Type: application/json

{
  "input": "分析这段代码的性能问题",
  "contextId": 123,
  "conversationId": "conv_456"
}

Response: Result<Long>  // 返回 executionId
```

### 4.2 SSE 事件流

```http
GET /agents/execute/stream?executionId={id}
Authorization: Bearer {token}
```

**事件类型**:

| 事件 | 说明 |
|------|------|
| `THINKING_STARTED` | 开始推理 |
| `THINKING_CHUNK` | 流式输出片段 |
| `TOOL_CALLING` | 调用工具 |
| `TOOL_RESULT` | 工具执行结果 |
| `PAUSED` | 执行暂停（等待人工） |
| `COMPLETED` | 执行完成 |
| `FAILED` | 执行失败 |
| `ERROR` | 错误信息 |

### 4.3 执行控制

```http
POST /agents/execution/{id}/pause    // 暂停
POST /agents/execution/{id}/resume   // 恢复
POST /agents/execution/{id}/cancel   // 取消
GET  /agents/execution/{id}          // 查询状态
```

## 5. 数据模型

### 5.1 sf_agent_execution

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| agent_id | BIGINT | 关联 Agent |
| tenant_id | BIGINT | 租户隔离 |
| status | VARCHAR | 执行状态 |
| user_input | TEXT | 用户输入 |
| last_response | TEXT | 最后 LLM 响应 |
| model_config | JSONB | 模型配置 |
| token_used_input | BIGINT | 已用输入 Token |
| token_used_output | BIGINT | 已用输出 Token |
| start_time | TIMESTAMP | 开始时间 |
| end_time | TIMESTAMP | 结束时间 |
| conversation_id | VARCHAR | 对话 ID |

### 5.2 sf_agent_execution_snapshot

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| execution_id | BIGINT | 关联执行 |
| state | VARCHAR | 快照状态 |
| context_data | JSONB | 上下文数据 |
| memory_summary | TEXT | 记忆摘要 |
| created_at | TIMESTAMP | 快照时间 |

## 6. 非功能需求

| 指标 | 目标 | 验证方式 |
|------|------|----------|
| 单执行延迟 | P99 < 30s | 压测 |
| 并发执行数 | 单实例 100+ | 压测 |
| 状态机内存泄漏 | 0 | 终端状态清理验证 |
| Token 预算精度 | 误差 < 5% | 单元测试 |
| 循环检测准确率 | > 95% | 集成测试 |

## 7. 相关文档

- `docs/decisions/ADR-002-cursor-sdk-to-opensandbox.md`
- `docs/decisions/ADR-003-langchain4j-selection.md`
- `docs/designs/agent-runtime-task-board.md`
- `docs/plans/unified-dev-plan.md`（Tasks 30-34）
