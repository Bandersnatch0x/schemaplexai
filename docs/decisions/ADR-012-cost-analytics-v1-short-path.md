---
topic: cost-analytics-v1-short-path
stage: decision
version: v1.0
status: 已批准
supersedes: ""
---

# ADR-012: Cost Analytics v1 Short-Path

> **日期**: 2026-05-08
> **决策人**: 架构评审委员会
> **状态**: 已批准

---

## 背景

SchemaPlexAI v1 需要向租户管理员提供实时成本看板（今日消耗、本月消耗、预算使用率）。当前状态：

- **ClickHouse 集群尚未就绪**：运维团队预计 3-4 周后完成部署和调优
- **PostgreSQL 已运行**：`sf_budget` 表已存储每个 Agent 执行的 `used_amount`（Token 消耗折算金额）
- **时间压力**：v1 上线日期锁定，成本看板为上线 blocker

需要决策：v1 成本分析使用何种数据后端。

## 决策

v1 成本分析采用 **PostgreSQL `sf_budget.used_amount` 聚合** 作为短期路径（short-path），ClickHouse 分析能力 deferred 至 v1.1。

### 实现细节

1. **数据源**
   - 表：`sf_budget`（PostgreSQL）
   - 字段：`tenant_id`, `agent_id`, `used_amount` (DECIMAL), `created_at`
   - 聚合维度：tenant 级别 todayCost / monthCost / budgetUsagePercent

2. **聚合策略**
   - 今日消耗：`SUM(used_amount) WHERE created_at >= CURRENT_DATE`
   - 本月消耗：`SUM(used_amount) WHERE created_at >= DATE_TRUNC('month', CURRENT_DATE)`
   - 预算使用率：`monthCost / budget_limit * 100`
   - 缓存：Redis 缓存聚合结果，TTL = 5 分钟（降低 PG 查询压力）

3. **精度声明**
   - `todayCost` 和 `monthCost` 为近似值，精确到分（两位小数）
   - 不包含：
     - 跨服务网络调用成本
     - 存储成本（MinIO / Milvus）
     - 基础设施摊销成本
   - 上述成本将在 v1.1 ClickHouse 统一计费模型中补齐

4. **v1.1 迁移路径**
   - ClickHouse 就绪后，成本数据从 PG 同步至 ClickHouse
   - 看板查询切换至 ClickHouse，保留 PG 作为源数据备份
   - 统一计费模型包含 Token + 网络 + 存储 + 基础设施全成本

## 理由

- **时间约束**：PG 聚合可在 2 天内完成开发，满足 v1 上线时间
- **数据已就绪**：`sf_budget` 表已有数据，无需额外数据管道
- **风险可控**：近似值在管理看板场景可接受，财务结算不依赖此数据
- **ClickHouse 不可替代**：v1.1 需要多维度 OLAP 分析（按 Agent / 按模型 / 按时段），ClickHouse 仍是长期方案

## 影响

- **对现有代码的影响**
  - `schemaplexai-ops` 新增 CostAnalyticsService，基于 MyBatis-Plus 查询 `sf_budget`
  - 新增 Redis 缓存层（`ops:cost:{tenantId}`）
  - 成本看板 API 返回字段增加 `isApproximation: true` 标识

- **对数据的影响**
  - PG `sf_budget` 表查询量增加，需确保 `created_at` + `tenant_id` 联合索引存在
  - 大租户月数据量预估 < 100 万条，PG 聚合性能可接受（< 200ms）

- **对用户的影响**
  - 成本看板显示 "估算值" 提示，避免用户误认为精确账单
  - 精确账单功能在 v1.1 提供

## 替代方案

| 方案 | 优点 | 缺点 | 结果 |
|------|------|------|------|
| 等待 ClickHouse 就绪 | 数据精确，OLAP 能力强 | v1 上线延期 3-4 周 | 拒绝 |
| 自建临时 OLAP（Druid/Pinot） | 功能完整 | 引入新组件，运维负担大 | 拒绝 |
| **PG 聚合 short-path（本方案）** | 快速交付，风险可控 | 数据为近似值，维度有限 | **采纳** |
| 完全不做成本看板 | 无开发成本 | v1 上线 blocker | 拒绝 |

## 相关文档

- `docs/designs/cost-analytics-architecture.md`
- `docs/specs/v1.1-clickhouse-migration-spec.md`
- `wiki/data-model.md#sf_budget`
