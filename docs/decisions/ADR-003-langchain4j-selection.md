---
topic: langchain4j-selection
stage: decision
version: v1.0
status: 已批准
supersedes: ""
---

# ADR-003: LangChain4j 作为 LLM 编排框架选型

> **日期**: 2026-04-10
> **决策人**: 架构评审委员会
> **状态**: 已批准

---

## 背景

Agent 执行引擎需要与多种 LLM Provider（OpenAI、Anthropic、Azure、本地模型）交互，需统一封装以下能力：

- 流式响应 (Streaming / SSE)
- 工具调用 (Tool Calling / Function Calling)
- 对话记忆管理 (Chat Memory)
- 提示词模板 (Prompt Templates)
- Token 计数与预算控制

## 决策

采用 **LangChain4j 0.31.0** 作为 LLM 编排基础框架，同时封装自有 `LlmProvider` 抽象层作为降级保护。

### 选型对比

| 框架 | 成熟度 | Java 原生 | Tool Calling | Streaming | Memory | 社区活跃度 | 结论 |
|------|--------|-----------|--------------|-----------|--------|-----------|------|
| **LangChain4j** | 中 | 是 | 支持 | 支持 | 支持 | 高 | **采纳** |
| Spring AI | 低（Milestone） | 是 | 部分 | 部分 | 基础 | 中 | 等待 GA |
| 直接调用各厂商 SDK | 高 | 是 | 需自行封装 | 需自行封装 | 需自行实现 | — | 作为 fallback |
| Python LangChain (JNI/服务化) | 高 | 否 | 支持 | 支持 | 支持 | 极高 | 引入跨语言复杂度 |

### 使用边界

```
┌─────────────────────────────────────────────┐
│          schemaplexai-agent-engine          │
│                                             │
│  ┌─────────────┐    ┌─────────────────────┐ │
│  │   LlmProvider│◄───│ 自有抽象层（防腐层） │ │
│  │  Interface  │    │  TokenBudget / Router│ │
│  └──────┬──────┘    └─────────────────────┘ │
│         │                                   │
│  ┌──────┴──────┐  ┌────────────────────────┐│
│  │ LangChain4j │  │ 直接调用 SDK（fallback）││
│  │  (primary)  │  │  (OpenAI/Anthropic SDK) ││
│  └─────────────┘  └────────────────────────┘│
└─────────────────────────────────────────────┘
```

- **主路径**：LangChain4j 处理 Streaming、Tool Calling、Memory
- **防腐层**：`LlmProvider` 接口隔离 LangChain4j API，未来可无痛替换
- **Fallback**：若 LangChain4j 某能力不满足，直接调用厂商 SDK

## 关键风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| LangChain4j Tool Calling 不成熟 | Phase 2 阻塞 | Phase 0 并行预研，2 周内输出可行性报告 |
| LangChain4j Streaming API 变更 | API 不兼容 | 防腐层隔离，仅影响适配器 |
| LangGraph4j Team Agent 不成熟 | Phase 9 缩减 | 预研决定取舍；如不过则降级为顺序调用 |

## 相关文档

- `docs/plans/project-plan.md` v1.1（Phase 2 任务、关键风险表）
- `docs/specs/agent-execution-engine.md`
