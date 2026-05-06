-- ============================================================
-- SchemaPlexAI Cost Analytics — ClickHouse Schema Initialization
-- Database: schemaplexai_costs
-- ============================================================

CREATE DATABASE IF NOT EXISTS schemaplexai_costs;

-- ============================================================
-- 1. sf_cost_record
--    Main cost tracking table — stores every LLM API call with
--    token usage and cost breakdown.
-- ============================================================
CREATE TABLE IF NOT EXISTS schemaplexai_costs.sf_cost_record
(
    -- Identity & Routing
    tenant_id               UInt64 COMMENT 'Multi-tenant shard key',
    record_id               UUID   COMMENT 'Unique record identifier (UUID v7 preferred)',

    -- Service & Model Info
    service_name            LowCardinality(String) COMMENT 'Service that initiated the call (e.g. agent-engine, workflow)',
    model_name              LowCardinality(String) COMMENT 'LLM model name (e.g. gpt-4o, claude-3-5-sonnet)',
    provider                LowCardinality(String) COMMENT 'Model provider (e.g. openai, anthropic, azure)',
    request_type            LowCardinality(String) COMMENT 'Request category (chat, embedding, tool_call, image)',

    -- Token Metrics
    input_tokens            UInt64 COMMENT 'Number of input/prompt tokens consumed',
    output_tokens           UInt64 COMMENT 'Number of output/completion tokens consumed',
    total_tokens            UInt64 COMMENT 'Total tokens (input + output)',

    -- Cost Metrics
    cost_amount             Decimal128(12) COMMENT 'Cost amount in the specified currency',
    currency                LowCardinality(String) COMMENT 'ISO 4217 currency code (e.g. USD, CNY)',

    -- Timestamps & Context
    created_at              DateTime64(3) COMMENT 'Record creation timestamp (UTC)',
    execution_id            Nullable(UInt64) COMMENT 'FK to sf_agent_execution.id',
    agent_id                Nullable(UInt64) COMMENT 'FK to sf_agent.id',
    workflow_instance_id    Nullable(UInt64) COMMENT 'FK to workflow instance if applicable',

    -- Index
    INDEX idx_model model_name TYPE bloom_filter() GRANULARITY 3,
    INDEX idx_provider provider TYPE bloom_filter() GRANULARITY 3
)
ENGINE = MergeTree()
PARTITION BY toYYYYMMDD(created_at)
ORDER BY (tenant_id, service_name, created_at)
TTL created_at + INTERVAL 1 YEAR
SETTINGS index_granularity = 8192
COMMENT 'Main cost tracking table — stores every LLM API call with token usage and cost breakdown';

-- ============================================================
-- 2. sf_model_usage_hourly
--    Hourly aggregated usage per model — populated by a
--    materialized view that aggregates sf_cost_record.
-- ============================================================
CREATE TABLE IF NOT EXISTS schemaplexai_costs.sf_model_usage_hourly
(
    tenant_id               UInt64 COMMENT 'Multi-tenant shard key',
    model_name              LowCardinality(String) COMMENT 'LLM model name',
    provider                LowCardinality(String) COMMENT 'Model provider',
    hour_bucket             DateTime COMMENT 'Hour bucket (truncated to hour)',

    -- Aggregated Metrics (SummingMergeTree will sum these)
    total_requests          UInt64 COMMENT 'Total number of requests in the hour',
    total_input_tokens      UInt64 COMMENT 'Sum of input tokens',
    total_output_tokens     UInt64 COMMENT 'Sum of output tokens',
    total_tokens            UInt64 COMMENT 'Sum of total tokens',
    avg_latency_ms          UInt64 COMMENT 'Average latency in milliseconds (stored as sum for SummingMergeTree)',
    total_cost              Decimal128(12) COMMENT 'Sum of cost in the hour'
)
ENGINE = SummingMergeTree()
PARTITION BY toYYYYMMDD(hour_bucket)
ORDER BY (tenant_id, model_name, provider, hour_bucket)
TTL hour_bucket + INTERVAL 1 YEAR
SETTINGS index_granularity = 8192
COMMENT 'Hourly aggregated usage per model — populated by materialized view from sf_cost_record';

-- Materialized View: sf_cost_record -> sf_model_usage_hourly
CREATE MATERIALIZED VIEW IF NOT EXISTS schemaplexai_costs.mv_model_usage_hourly
TO schemaplexai_costs.sf_model_usage_hourly
AS
SELECT
    tenant_id,
    model_name,
    provider,
    toStartOfHour(created_at) AS hour_bucket,
    count()                   AS total_requests,
    sum(input_tokens)         AS total_input_tokens,
    sum(output_tokens)        AS total_output_tokens,
    sum(total_tokens)         AS total_tokens,
    0                         AS avg_latency_ms,  -- placeholder; will be populated by application
    sum(cost_amount)          AS total_cost
FROM schemaplexai_costs.sf_cost_record
GROUP BY
    tenant_id,
    model_name,
    provider,
    hour_bucket;

-- ============================================================
-- 3. sf_token_consumption_daily
--    Daily token consumption aggregated per tenant.
-- ============================================================
CREATE TABLE IF NOT EXISTS schemaplexai_costs.sf_token_consumption_daily
(
    tenant_id               UInt64 COMMENT 'Multi-tenant shard key',
    date                    Date COMMENT 'Calendar date',

    -- Aggregated Metrics (SummingMergeTree will sum these)
    total_input_tokens      UInt64 COMMENT 'Sum of input tokens for the day',
    total_output_tokens     UInt64 COMMENT 'Sum of output tokens for the day',
    total_tokens            UInt64 COMMENT 'Sum of total tokens for the day',
    total_cost              Decimal128(12) COMMENT 'Sum of cost for the day',
    unique_models_count     UInt16 COMMENT 'Count of unique models used (stored as sum for SummingMergeTree)'
)
ENGINE = SummingMergeTree()
PARTITION BY toYYYYMM(date)
ORDER BY (tenant_id, date)
TTL date + INTERVAL 2 YEAR
SETTINGS index_granularity = 8192
COMMENT 'Daily token consumption aggregated per tenant';

-- Materialized View: sf_cost_record -> sf_token_consumption_daily
CREATE MATERIALIZED VIEW IF NOT EXISTS schemaplexai_costs.mv_token_consumption_daily
TO schemaplexai_costs.sf_token_consumption_daily
AS
SELECT
    tenant_id,
    toDate(created_at) AS date,
    sum(input_tokens)  AS total_input_tokens,
    sum(output_tokens) AS total_output_tokens,
    sum(total_tokens)  AS total_tokens,
    sum(cost_amount)   AS total_cost,
    0                  AS unique_models_count  -- placeholder; application may backfill
FROM schemaplexai_costs.sf_cost_record
GROUP BY
    tenant_id,
    date;

-- ============================================================
-- 4. sf_agent_execution_cost
--    Per-agent execution cost tracking — stores aggregated
--    cost and token metrics for a single agent execution.
-- ============================================================
CREATE TABLE IF NOT EXISTS schemaplexai_costs.sf_agent_execution_cost
(
    tenant_id               UInt64 COMMENT 'Multi-tenant shard key',
    agent_id                UInt64 COMMENT 'FK to sf_agent.id',
    execution_id            UInt64 COMMENT 'FK to sf_agent_execution.id',
    workflow_instance_id    Nullable(UInt64) COMMENT 'FK to workflow instance if applicable',

    -- Aggregated Cost Metrics
    total_cost              Decimal128(12) COMMENT 'Total cost for this execution',
    total_tokens            UInt64 COMMENT 'Total tokens consumed during execution',
    tool_call_count         UInt32 COMMENT 'Number of tool calls made',
    llm_call_count          UInt32 COMMENT 'Number of LLM API calls made',
    duration_ms             UInt64 COMMENT 'Execution duration in milliseconds',

    -- Timestamp
    created_at              DateTime64(3) COMMENT 'Record creation timestamp (UTC)',

    -- Index
    INDEX idx_agent agent_id TYPE bloom_filter() GRANULARITY 3
)
ENGINE = MergeTree()
PARTITION BY toYYYYMMDD(created_at)
ORDER BY (tenant_id, agent_id, created_at)
TTL created_at + INTERVAL 1 YEAR
SETTINGS index_granularity = 8192
COMMENT 'Per-agent execution cost tracking — aggregated cost and token metrics for a single agent execution';
