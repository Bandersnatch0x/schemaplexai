---
topic: cost-analytics
stage: spec
version: v1.0
status: 草稿
supersedes: ""
---

# 成本分析技术规格

> **主题**: `cost-analytics`
> **阶段**: `spec`
> **版本**: v1.0
> **状态**: 草稿
> **日期**: 2026-04-30
> **范围**: `schemaplexai-ops` 服务中的成本分析模块

---

## 1. 概述

成本分析模块负责：

- **Token 成本采集**: 记录每次 LLM 调用的输入/输出 Token 数及成本
- **成本同步**: PostgreSQL → ClickHouse 增量同步
- **成本报表**: 多维度成本分析（租户/Agent/模型/时间）
- **预算告警**: 预算超支预警

## 2. 数据流

```
Agent 执行
    │
    ▼
TokenUsageRecorder (AOP / 拦截器)
    │
    ▼
PostgreSQL (sf_token_usage)
    │
    │ 增量同步 (定时任务 / MQ)
    ▼
ClickHouse (cost_records)
    │
    ▼
Cost Analytics Queries
    │
    ▼
Dashboard / API
```

## 3. 核心组件

### 3.1 Token 使用记录

**sf_token_usage** (PostgreSQL):

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| tenant_id | BIGINT | 租户隔离 |
| agent_id | BIGINT | Agent ID |
| execution_id | BIGINT | 执行 ID |
| model | VARCHAR | 模型名称 |
| provider | VARCHAR | 供应商 |
| input_tokens | BIGINT | 输入 Token 数 |
| output_tokens | BIGINT | 输出 Token 数 |
| cost | DECIMAL(18,6) | 成本（USD） |
| created_at | TIMESTAMP | 创建时间 |

**成本计算公式**:

```java
cost = (inputTokens * inputPricePer1K / 1000) + (outputTokens * outputPricePer1K / 1000)
```

**模型价格配置** (sf_ai_model 表扩展):

| 字段 | 说明 |
|------|------|
| input_price_per_1k | 每 1K 输入 Token 价格 |
| output_price_per_1k | 每 1K 输出 Token 价格 |
| currency | 货币单位（默认 USD） |

### 3.2 ClickHouse 同步

**同步策略**: 增量游标同步

```java
@Service
public class ClickHouseCostSyncService {
    // 1. 读取游标 (sf_sync_cursor)
    // 2. 查询 PG: SELECT * FROM sf_token_usage WHERE created_at > cursor
    // 3. 批量插入 ClickHouse
    // 4. 更新游标
    // 5. 记录批次日志 (sf_sync_batch_log)
}
```

**ClickHouse 表结构**:

```sql
CREATE TABLE cost_records (
    id UInt64,
    tenant_id UInt64,
    agent_id UInt64,
    execution_id UInt64,
    model String,
    provider String,
    input_tokens UInt64,
    output_tokens UInt64,
    cost Decimal(18, 6),
    created_at DateTime
) ENGINE = MergeTree()
ORDER BY (tenant_id, created_at)
PARTITION BY toYYYYMM(created_at);
```

**物化视图** (日聚合):

```sql
CREATE MATERIALIZED VIEW mv_daily_cost
ENGINE = SummingMergeTree()
ORDER BY (tenant_id, agent_id, model, date)
AS SELECT
    tenant_id,
    agent_id,
    model,
    toDate(created_at) as date,
    sum(input_tokens) as total_input_tokens,
    sum(output_tokens) as total_output_tokens,
    sum(cost) as total_cost,
    count() as call_count
FROM cost_records
GROUP BY tenant_id, agent_id, model, date;
```

### 3.3 预算告警

**sf_budget**:

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| tenant_id | BIGINT | 租户隔离 |
| type | VARCHAR | MONTHLY / AGENT / MODEL |
| target_id | BIGINT | 目标 ID（Agent ID 或 Model ID） |
| limit_amount | DECIMAL(18,6) | 预算上限 |
| alert_threshold | DECIMAL(3,2) | 告警阈值（如 0.8 = 80%） |

**告警检查** (定时任务):

```java
@Scheduled(cron = "0 0 * * * ?") // 每小时
public void checkBudgetAlerts() {
    // 1. 查询各租户当前周期成本
    // 2. 对比预算上限
    // 3. 超过阈值 → 发送告警通知
}
```

## 4. API 接口

### 4.1 成本查询

```http
GET /ops/costs/summary?tenantId={id}&startDate=2026-04-01&endDate=2026-04-30

Response:
{
  "code": 200,
  "data": {
    "totalCost": 125.50,
    "totalInputTokens": 5000000,
    "totalOutputTokens": 2000000,
    "callCount": 1500,
    "byAgent": [...],
    "byModel": [...],
    "byDay": [...]
  }
}
```

### 4.2 预算管理

```http
POST   /ops/budgets              # 创建预算
GET    /ops/budgets              # 列表
PUT    /ops/budgets/{id}         # 更新
DELETE /ops/budgets/{id}         # 删除
GET    /ops/budgets/alerts       # 告警记录
```

## 5. 定时任务

| 任务 | 频率 | 说明 |
|------|------|------|
| CostSyncJob | 每 5 分钟 | PG → ClickHouse 增量同步 |
| CostStatisticsJob | 每日 01:00 | 日成本聚合 |
| BudgetAlertJob | 每小时 | 预算告警检查 |

## 6. 非功能需求

| 指标 | 目标 |
|------|------|
| 同步延迟 | < 5 分钟 |
| 报表查询延迟 | P99 < 2s |
| 成本计算精度 | 6 位小数 |
| 数据保留 | ClickHouse 保留 2 年，PG 保留 90 天 |

## 7. 相关文档

- `docs/plans/project-plan.md`（Phase 8）
- `docs/plans/unified-dev-plan.md`（Tasks 42-43）
- `docs/decisions/ADR-004-database-middleware-selection.md`
