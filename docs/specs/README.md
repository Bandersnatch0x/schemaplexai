# Specs / 技术规格说明书

技术规格（Spec）是设计与实现的契约。任何超过 50 行代码的变更或新模块开发，必须先有 Spec。

## 范围

- 接口契约（API Spec）
- 数据模型规格
- 算法/策略规格
- 集成协议规格

## 命名规范

```
<topic>.md
```

`topic` 为短横线分隔的英文小写主题词，与对应 `designs/`、`plans/`、`ui/` 文档保持一致。

## 当前文档

| 文档 | 版本 | 状态 | 说明 |
|------|------|------|------|
| [`agent-execution-engine.md`](agent-execution-engine.md) | v1.0 | 已批准 | Agent 执行引擎技术规格（状态机、Token 预算、SSE、准入控制） |
| [`api-gateway.md`](api-gateway.md) | v1.0 | 已批准 | API Gateway 技术规格（路由、JWT、限流、租户解析） |
| [`rag-pipeline.md`](rag-pipeline.md) | v1.0 | 草稿 | RAG Pipeline 技术规格（文档上传、分块、Embedding、Milvus 同步） |
| [`workflow-engine.md`](workflow-engine.md) | v1.0 | 草稿 | 工作流引擎技术规格（BPMN、AI 节点、Flowable 桥接） |
| [`quality-gate.md`](quality-gate.md) | v1.0 | 草稿 | 质量门禁技术规格（Spec 合规、安全扫描、Grounding 验证） |
| [`integration-layer.md`](integration-layer.md) | v1.0 | 草稿 | 集成层技术规格（Git/Jenkins/MCP/Skill/API Gateway） |
| [`cost-analytics.md`](cost-analytics.md) | v1.0 | 草稿 | 成本分析技术规格（Token 采集、ClickHouse 同步、预算告警） |
| [`spec-management.md`](spec-management.md) | v1.0 | 草稿 | Spec 管理技术规格（版本控制、评审工作流、Steering 文档） |
