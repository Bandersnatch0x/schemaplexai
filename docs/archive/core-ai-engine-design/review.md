---
change_id: core-ai-engine-design
reviewer: code-reviewer
review_date: 2026-05-01
status: conditional-pass
---

# Code Review: Core AI Engine Design (Phase 2)

## 评审结论: CONDITIONAL PASS (附带条件通过)

无 CRITICAL 问题，发现 2 个 HIGH + 3 个 MEDIUM + 1 个 LOW 问题。HIGH 问题必须在 Phase 2 实施前修复，其余可在实施中消化。

---

## HIGH Issues

### H-1: Spec 状态机与实际代码状态枚举不一致

**位置**: `spec.md` §5 状态机

**问题**: Spec 定义 7 状态（QUEUED, THINKING, TOOL_CALLING, COMPLETED, FAILED, CANCELLED, PAUSED），但实际代码 `AgentExecutionState.java` 有 11 个状态：
- INITIALIZING, READY, THINKING, TOOL_CALLING, OBSERVATION, PAUSED, GATE_BLOCKED, RETRYING, COMPLETED, FAILED, CANCELLED

Spec 遗漏了 INITIALIZING, READY, OBSERVATION, GATE_BLOCKED, RETRYING 五个已存在状态。这会导致 Phase 2 实施时状态转换逻辑与现有代码冲突。

**建议**: 
1. 将 Spec 状态机更新为 11 状态完整版
2. 或明确说明 Phase 2 将精简状态为 7 状态（需评估对现有代码的影响）
3. 推荐方案：保留 11 状态，在 Spec 中标注哪些是 Phase 2 重点（THINKING/TOOL_CALLING/COMPLETED/FAILED）哪些是已有但 Phase 2 不改动

### H-2: Spec 数据模型字段名与代码实体不一致

**位置**: `spec.md` §4.1 数据模型

**问题**:
| Spec 字段 | 代码实体字段 | 状态 |
|-----------|-------------|------|
| `state` | `status` | 不一致 |
| `token_budget_json` (结构化 JSON) | `tokenBudgetJson` (逗号分隔字符串) | 不一致 |
| `snapshot_json` | `context_data` + `state` + `memory_summary` | 不一致 |

**影响**: Phase 2 实施时若按 Spec 设计会导致数据库 schema 变更或代码兼容性问题。

**建议**: 
1. 同步 Spec 数据模型到实际代码实体定义
2. 或明确标注哪些字段需要迁移（如 `token_budget_json` 从逗号分隔改为 JSON 是 Phase 2 的任务之一）

---

## MEDIUM Issues

### M-1: resume 流转目标不一致

**位置**: `design.md` §4.2 暂停/恢复流序列图

**问题**: Design 序列图中 `resume` 后流转到 THINKING，但 `AgentExecutionLifecycleService.resumeExecution()` 实际代码转的是 READY。Spec 状态机图也写 PAUSED → THINKING。

**建议**: 统一为 PAUSED → READY（与现有代码一致），并在文档中说明 READY 后由 orchestrator 下一轮循环进入 THINKING。

### M-2: C4 Component 图遗漏现有 Handler

**位置**: `design.md` §2.2 C4 Container

**问题**: State Handlers 子图只包含 Thinking/Tool/Complete，遗漏了代码中已存在的：
- `InitializingStateHandler`, `ReadyStateHandler`, `PausedStateHandler`
- `GateBlockedStateHandler`, `RetryingStateHandler`, `ObservationStateHandler`, `FailedStateHandler`

**建议**: 完整列出所有 handler，或标注哪些是 Phase 2 新增/重点。

### M-3: GATE_BLOCKED 异常处理不完整

**位置**: `spec.md` §7 异常场景

**问题**: Spec 说 GATE_BLOCKED 返回 429，但 `AgentRuntimeOrchestrator.run()` 中 GATE_BLOCKED 后直接 `return`，没有返回任何 Result 给调用方。异步执行模式下调用方只拿到 executionId，无法感知准入拒绝。

**建议**: 明确异步模式下准入拒绝的反馈机制（如通过 SSE 推送 ERROR 事件，或同步返回时包含 admission 状态）。

---

## LOW Issues

### L-1: QUEUED 状态在代码中不存在

**位置**: `spec.md` §3.1, §5

**问题**: Spec 引入 QUEUED 状态，但代码中没有此状态。实际流程中 `startExecution()` 直接设置 INITIALIZING 并同步调用 orchestrator。

**建议**: 若 Phase 2 引入 @Async，QUEUED 可作为异步队列等待状态；否则应移除，使用现有的 INITIALIZING/READY。

---

## 质量门禁检查表

| 门禁项 | 状态 | 备注 |
|--------|------|------|
| 引用已批准规格，不重复定义 | PASS | 正确引用 `2026-04-30-v1.0-agent-execution-engine.md` |
| 基于实际代码 stubs，不凭空设计 | CONDITIONAL | 状态机和数据模型与代码存在偏差（H-1, H-2） |
| 包含具体实现差距和优先级 | PASS | §6 组件待实现清单完整，8 项含优先级 |
| Spec 中状态机完整（7状态+转换矩阵） | FAIL | 实际代码有 11 状态，Spec 遗漏 5 个 |
| Design 中 C4 三级图完整 | PASS | Context/Container/Component 三级齐全 |
| 序列图（正常流+暂停恢复流）正确 | CONDITIONAL | 正常流正确；暂停恢复流 resume 目标需修正（M-1） |
| 技术决策表有依据 | PASS | 6 项决策均有明确原因 |
| Token Budget 和 Chat Memory 架构图合理 | PASS | 架构设计合理，与代码骨架方向一致 |

---

## 归档建议

1. **同步前必须修复**: H-1（状态机同步）、H-2（数据模型同步）
2. **建议同步修复**: M-1（resume 流转）、M-2（C4 图完整）
3. **可在实施中消化**: M-3（GATE_BLOCKED 反馈）、L-1（QUEUED 状态）

修复后文件可同步到 `docs/specs/` 和 `docs/designs/`。

---

## 修复记录 (2026-05-01)

### H-1 修复
- `spec.md` §1.1: 更新当前代码状态，列出所有已有 Handler 和缺失项
- `spec.md` §4.1: 数据模型同步到实际代码实体（`SfAgentExecution`、`SfAgentExecutionSnapshot`、`sf_chat_message` 分区表）
- `spec.md` §4.2: 状态枚举值更新为 11 状态完整链
- `spec.md` §5.1: 新增 11 状态定义表（含 Handler 映射）
- `spec.md` §5.2: 更新状态转换矩阵为 21 条转换规则
- `spec.md` §5: Mermaid 状态图更新为 11 状态完整版
- `spec.md` §3.1: QUEUED → INITIALIZING
- `spec.md` §6: 组件状态更新为更精确的描述（骨架/不存在）
- `design.md` §1: 7 状态 → 11 状态
- `design.md` §2.2: C4 Container 图补全 9 个 State Handler

### H-2 修复
- `spec.md` §4.1: 数据模型字段完全对照 `SfAgentExecution.java`、`SfAgentExecutionSnapshot.java`、`02-init-schema-agent.sql`
- 标注 `token_budget_json` 当前为逗号分隔字符串，Phase 2 计划改为结构化 JSON
- 标注实体字段较精简，user_input 等字段实际存储在 `sf_chat_message` 表

### M-1 修复
- `design.md` §4.2: resume 流转目标从 THINKING 修正为 READY
- 添加注释说明 READY handler 自动流转到 THINKING

### M-2 修复
- `design.md` §2.2: C4 Container 图补全所有 9 个 State Handler
- `design.md` §4.1: 正常执行流序列图补全 Observation 状态
- 修复 ObservabilityRecorder 命名冲突（Obs → ObsRec）

### 修复后自评审
- [x] 状态机 11 状态与 `AgentExecutionState.java` 完全一致
- [x] 数据模型字段与 `SfAgentExecution.java` / `SfAgentExecutionSnapshot.java` / SQL schema 一致
- [x] resume 流转与 `AgentExecutionLifecycleService.resumeExecution()` 代码一致（PAUSED → READY）
- [x] C4 Container 图包含所有现有 Handler
- [x] 无 CRITICAL 或 HIGH 问题
