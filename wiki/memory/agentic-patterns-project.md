---
name: agentic-patterns-project
description: Agentic Design Patterns pre-research and Layer 1 implementation plan status
type: project
---

Agentic Design Patterns 圆桌辩论和实施规划已完成，待执行。

**What's done:**
- 21 个设计模式预研总结 → `wiki/agentic-design-patterns.md`
- 架构差距分析（14+ gaps）→ `wiki/architecture-gap-analysis.md`
- 四方专家圆桌辩论 → `docs/superpowers/specs/2026-05-01-agentic-patterns-roundtable-report.md`
- Layer 1 实施计划（5 tasks, 6 weeks）→ `docs/superpowers/plans/2026-05-02-layer1-agentic-patterns.md`

**Layer 1 分层（辩论后裁决）:**
- Exception Handling, Evaluation, Resource Optimization, Tool Use, Memory, Reflection, Reasoning, Guardrails(规则层), RAG, Routing

**3 个 Critical 安全风险（P0，必须先修复）:**
1. Tool 执行无 Sandbox → `ToolCallingStateHandler` 需容器隔离
2. ContextInjector 无输入验证 → 需黑名单+长度+语义检测
3. SSE 无额外认证 → 需一次性 token/JWT scope 验证

**实施计划 5 个任务:**
1. W1: Critical 安全风险修复 (ToolSandbox, InputValidator, SseTokenValidator)
2. W1-2: Tool Use 完整实现 (ToolRegistry, ReActPromptTemplate, FinalAnswerExtractor)
3. W3-4: Reasoning + Exception Handling (ReasoningStrategy, RecoveryStrategy, TokenBudget)
4. W5: Memory + RAG 数据隔离 (MemoryStrategy, MilvusIsolationService)
5. W6: Reflection + Evaluation + Guardrails (Evaluator, GuardrailsEngine, ReflectingStateHandler)

**关键依赖:** Tool Use → 被 Reflection/Planning/Multi-Agent 依赖；RAG → 被 Memory/Learning 依赖

**Why:** 圆桌辩论确认 Reasoning 是基础设施不是创新层，不修复则 Tool Use 无法工作。Multi-Agent 是最大差异化但需覆盖率>70%前提。

**How to apply:** 下次会话直接读取实施计划开始执行，或调用 writing-plans skill 细化。
