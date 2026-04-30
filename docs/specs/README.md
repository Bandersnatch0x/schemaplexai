# Specs / 技术规格说明书

技术规格（Spec）是设计与实现的契约。任何超过 50 行代码的变更或新模块开发，必须先有 Spec。

## 范围

- 接口契约（API Spec）
- 数据模型规格
- 算法/策略规格
- 集成协议规格

## 命名规范

```
YYYY-MM-DD[-vX.Y]-<topic>.md
```

`topic` 为短横线分隔的英文小写主题词，与对应 `designs/`、`plans/`、`ui/` 文档保持一致。

## 当前文档

| 文档 | 版本 | 状态 | 说明 |
|------|------|------|------|
| [`2026-04-30-v1.0-agent-execution-engine.md`](2026-04-30-v1.0-agent-execution-engine.md) | v1.0 | 已批准 | Agent 执行引擎技术规格（状态机、Token 预算、SSE、准入控制） |
| [`2026-04-30-v1.0-api-gateway.md`](2026-04-30-v1.0-api-gateway.md) | v1.0 | 已批准 | API Gateway 技术规格（路由、JWT、限流、租户解析） |
| [`2026-04-30-v1.0-cost-analytics.md`](2026-04-30-v1.0-cost-analytics.md) | v1.0 | 已批准 | 成本分析技术规格（Token 采集、ClickHouse 同步、预算告警） |
| [`2026-04-30-v1.0-rag-pipeline.md`](2026-04-30-v1.0-rag-pipeline.md) | v1.0 | 已批准 | RAG Pipeline 技术规格（文档上传、分块、Embedding、Milvus 同步） |
| [`2026-04-30-v1.0-workflow-engine.md`](2026-04-30-v1.0-workflow-engine.md) | v1.0 | 已批准 | 工作流引擎技术规格（BPMN、AI 节点、Flowable 桥接） |
| [`2026-04-30-v1.0-quality-gate.md`](2026-04-30-v1.0-quality-gate.md) | v1.0 | 已批准 | 质量门禁技术规格（Spec 合规、安全扫描、Grounding 验证） |
| [`2026-04-30-v1.0-integration-layer.md`](2026-04-30-v1.0-integration-layer.md) | v1.0 | 已批准 | 集成层技术规格（Git/Jenkins/MCP/Skill/API Gateway） |
| [`2026-04-30-v1.0-spec-management.md`](2026-04-30-v1.0-spec-management.md) | v1.0 | 已批准 | Spec 管理技术规格（版本控制、评审工作流、Steering 文档） |
| [`2026-04-30-v1.0-open-source-agent-architecture-research.md`](2026-04-30-v1.0-open-source-agent-architecture-research.md) | v1.0 | 已批准 | 开源 AI Agent 架构调研与设计借鉴 |
