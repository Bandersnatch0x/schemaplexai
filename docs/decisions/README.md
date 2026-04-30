# Decisions / 架构决策记录（ADR）

所有影响系统架构、技术选型、模块边界的重要决策必须以 ADR 形式记录。

## ADR 编号规则

```
ADR-NNN-<short-title>.md
```

- `NNN`：三位数字，顺序递增
- 已用编号：001~004

## ADR 模板

见 [`ADR-TEMPLATE.md`](ADR-TEMPLATE.md)。

## 当前 ADR

| 编号 | 文档 | 日期 | 状态 | 说明 |
|------|------|------|------|------|
| ADR-001 | [`ADR-001-service-decomposition.md`](ADR-001-service-decomposition.md) | 2026-04-15 | 已批准 | 微服务拆分与模块边界决策 |
| ADR-002 | [`ADR-002-cursor-sdk-to-opensandbox.md`](ADR-002-cursor-sdk-to-opensandbox.md) | 2026-04-30 | 已批准 | 放弃 Cursor SDK，转向自建 Agent Runtime + OpenSandbox |
| ADR-003 | [`ADR-003-langchain4j-selection.md`](ADR-003-langchain4j-selection.md) | 2026-04-10 | 已批准 | LangChain4j 作为 LLM 编排框架选型 |
| ADR-004 | [`ADR-004-database-middleware-selection.md`](ADR-004-database-middleware-selection.md) | 2026-04-12 | 已批准 | 数据库与中间件选型决策 |
