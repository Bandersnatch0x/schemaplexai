---
topic: agent-state-machine
stage: decision
version: v1.0
status: 已批准
supersedes: ""
---

# ADR-009: Agent 执行引擎状态机化

> **日期**: 2026-04-29
> **决策人**: 架构评审委员会
> **状态**: 已批准

---

## 背景

原 `runAgenticLoop` 为一个大方法，状态转换隐含在 if-else 中：
- 难以单元测试（需要构造完整的执行上下文）
- 新增状态（如 PAUSED）需要修改多处条件判断
- 状态转换历史不可追溯，调试困难
- 不支持状态快照持久化（无法中断/恢复）

## 决策

重构为**显式状态机**：`AgentStateMachine` + `AgentExecutionState` + `AgentStateHandler`。

## 理由

1. **状态转换清晰可追溯**：每个状态的出边在代码中显式定义
2. **每个状态独立测试**：可为 `ThinkingStateHandler`、`ToolCallingStateHandler` 等编写独立单元测试
3. **易于扩展新状态**：新增状态只需实现 `AgentStateHandler` 接口并注册到状态机
4. **支持持久化状态快照**：暂停时可序列化当前状态，恢复时重建上下文

## 影响

- **正面**：可测试性、可维护性、可扩展性显著提升
- **负面**：Phase 2 开发周期增加 ~1 周；需重新设计执行记录表结构
- **缓解**：状态机框架先写单元测试（100% 覆盖状态转换矩阵），再集成到引擎

## 替代方案

| 方案 | 评估 | 结论 |
|------|------|------|
| 保持原设计 | 增加状态枚举但不改变控制流，if-else 继续膨胀 | 拒绝 |
| 使用 Spring Statemachine | 功能强大但引入额外依赖，且过于复杂 | 拒绝 |
| **自研轻量状态机（本方案）** | Map<State, Handler> 模式，轻量、可控、与业务紧密结合 | **采纳** |

## 相关文档

- `docs/designs/2026-04-29-v1.1-system-architecture.md`
- `docs/specs/2026-04-30-v1.0-agent-execution-engine.md`
