---
title: Langfuse 架构调研
status: researching
project: langfuse
source: https://github.com/langfuse/langfuse
creation_date: 2026-04-30
---

# Langfuse 架构调研

> 开源 LLM 工程平台：可观测性、指标、评估、Prompt 管理、Playground、数据集。

## 一句话描述

基于 SDK/OpenTelemetry 的 LLM 可观测性平台，以 Trace/Span/Generation 的层级数据模型为核心，深度追踪多步 Agent 工作流。

## 核心架构（v3）

| 组件 | 技术 | 职责 |
|------|------|------|
| Langfuse Web | Node.js/Next.js | UI 和 API 端点 |
| Langfuse Worker | Node.js | 异步事件处理和后台作业 |
| PostgreSQL | 关系型数据库 | 事务数据（用户、项目、配置） |
| ClickHouse | OLAP | 可观测性数据（Trace、Span、Score、Event） |
| Redis/Valkey | KV | 缓存和队列 |
| S3/Blob Storage | 对象存储 | 原始事件和多模态数据 |

## 关键设计决策

### 1. 层级数据模型

```
Session → Trace → Observation (Span / Generation / Tool)
```

- **Trace**: 独立工作单元（一次聊天、一次 Agent 运行、一次流水线执行）
- **Span**: 中间步骤（检索、编排、工具执行）
- **Generation**: 专门的 LLM 调用事件，携带模型参数、Token 用量、成本
- **Score**: 附加到 Trace 或 Observation 的质量评估分数

### 2. SDK 插桩（非代理模式）

- 安装 Python/JS SDK，使用 `@observe()` 装饰器或原生回调
- 捕获输入、输出、Token、延迟、元数据、父子层级关系
- 异步发送到后端
- **与代理模式对比**：需要代码改动，但提供最深层级的多步 Agent 工作流可见性

### 3. OpenTelemetry 原生（v3）

- 基于官方 OpenTelemetry 客户端构建
- 原生支持 OTLP 摄取
- 可与现有 OTel 基础设施并行输出到多个后端（Jaeger、Datadog 等）
- 支持零代码变更的网关层集成（如 AgentGateway）

### 4. Prompt 管理

- 版本控制：每次保存的 Prompt 获得版本号
- 标签：`production`、`staging`、`latest`
- 运行时关联：Generation 记录 `promptName` 和 `promptVersion`
- 支持通过 SDK/CLI 在运行时获取 Prompt，无需重新部署

### 5. 评估体系

- LLM-as-a-Judge：内置 UI 评估器
- 用户反馈收集
- 手动标注
- 自定义评估流水线（通过 API）
- Dataset & Benchmark：从生产 Trace 中策划测试集

## 可借鉴设计

1. **Trace/Span/Generation 数据模型**：我们的 Agent 执行引擎缺少可观测性数据模型，可引入类似层级结构来追踪 Agent 运行
2. **ClickHouse + PostgreSQL 分离**：当前我们用 PostgreSQL 做所有事情；Langfuse 的 OLTP/OLAP 分离设计值得借鉴
3. **Prompt 版本管理**：当前无 Prompt 管理模块，Langfuse 的 Prompt 版本+标签+运行时获取模式可直接参考
4. **LLM-as-a-Judge 评估**：我们的 `quality` 服务可引入基于 LLM 的自动化评估
5. **Dataset 从生产 Trace 构建**：质量数据集可从实际 Agent 运行中自动收集

## 参考链接

- [GitHub - langfuse/langfuse](https://github.com/langfuse/langfuse)
- [Langfuse Docs](https://langfuse.com/docs)
- [Langfuse Architecture](https://langfuse.com/docs/deployment/self-host)
