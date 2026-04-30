---
topic: database-middleware-selection
stage: decision
version: v1.0
status: 已批准
supersedes: ""
---

# ADR-004: 数据库与中间件选型

> **日期**: 2026-04-12
> **决策人**: 架构评审委员会
> **状态**: 已批准

---

## 背景

SchemaPlexAI 作为企业级 AI 协作平台，需要支撑：

- **OLTP**：租户/用户/Agent/Spec 等业务数据（高并发、强一致）
- **OLAP**：Token 成本、Agent 指标分析（大时间跨度聚合）
- **向量检索**：知识文档 Embedding 检索（高维向量相似度搜索）
- **缓存/状态**：会话状态、Token 预算、限流计数（低延迟）
- **异步消息**：Agent 执行事件、Milvus 同步（可靠投递）
- **对象存储**：文档、制品、备份文件（大文件、高吞吐）

## 决策

| 用途 | 选型 | 版本 | 理由 |
|------|------|------|------|
| **OLTP 数据库** | PostgreSQL | 16 | ACID 强一致，JSONB 支持灵活 Schema，MyBatis-Plus 支持成熟 |
| **OLAP 数据库** | ClickHouse | 24 | 列式存储，秒级聚合查询，成本分析场景天然适配 |
| **向量数据库** | Milvus | 2.3.5 | 分布式向量检索，支持元数据过滤，与 PG 互补 |
| **缓存/状态** | Redis | 7 | 会话、限流、Chat Memory 热数据，支持 Stream |
| **消息队列** | RabbitMQ | 3.12 | 可靠消息投递，死信队列，Spring AMQP 原生支持 |
| **对象存储** | MinIO | latest | S3 兼容 API，可私有化部署，支持 Lifecycle |
| **搜索/日志** | Elasticsearch | 8.x | 日志聚合（ELK），全文检索备用 |

### 数据分层架构

```
┌─────────────────────────────────────────────┐
│              Application Layer               │
└──────────┬─────────────────┬────────────────┘
           │                 │
    ┌──────▼──────┐   ┌──────▼──────┐
    │    Redis    │   │  RabbitMQ   │
    │  (Hot Data) │   │   (Events)  │
    └──────┬──────┘   └─────────────┘
           │
    ┌──────▼──────────────────────────────┐
    │           PostgreSQL 16             │
    │  (OLTP: tenants, agents, specs...)  │
    │  ┌──────────────────────────────┐   │
    │  │     MyBatis-Plus (Tenant)    │   │
    │  └──────────────────────────────┘   │
    └──────────────┬──────────────────────┘
                   │
    ┌──────────────▼──────────────┐
    │         Milvus 2.3.5        │
    │   (Vectors: knowledge_doc)  │
    └─────────────────────────────┘

    ┌─────────────────────────────┐
    │      ClickHouse 24          │
    │   (OLAP: cost, metrics)     │
    │   ← 增量游标同步            │
    └─────────────────────────────┘

    ┌─────────────────────────────┐
    │         MinIO               │
    │   (Objects: docs, artifacts)│
    └─────────────────────────────┘
```

## 替代方案

| 组件 | 备选方案 | 评估 | 结论 |
|------|----------|------|------|
| OLTP | MySQL 8 | 团队更熟悉，但 JSON/数组支持弱于 PG | 拒绝 |
| OLAP | Apache Doris | 实时数仓优秀，但团队无经验 | 待定（未来评估） |
| 向量 | pgvector | 省去同步复杂度，但性能不如 Milvus | 拒绝（Milvus 胜出） |
| 缓存 | KeyDB | Redis 多线程分支，性能更高 | 拒绝（Redis 生态更成熟） |
| 消息 | Apache Kafka | 吞吐更高，但运维复杂 | 拒绝（RabbitMQ 足够） |
| 对象存储 | Ceph | 功能更全面，但部署复杂 | 拒绝（MinIO 轻量） |

## 影响

- **兼容性**：所有服务统一使用 PostgreSQL，已修复 MySQL 驱动残留（见 P0-001）
- **Milvus-PG 一致性**：需实现 PG→Milvus 同步消费者 + 每日对账任务（见 project-plan Phase 3）
- **ClickHouse 同步**：增量游标持久化 + 批次校验（见 project-plan Phase 8）
- **运维**：Docker Compose 一键启动全部基础设施（见 `docker/docker-compose.yml`）

## 相关文档

- `docs/plans/project-plan.md` v1.1（Phase 0、Phase 3、Phase 8）
- `docs/designs/system-architecture.md` v1.1
