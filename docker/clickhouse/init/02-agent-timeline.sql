-- ============================================================
-- SchemaPlexAI Agent Timeline — ClickHouse Schema Initialization
-- Database: schemaplexai_costs (co-located with cost analytics)
-- ============================================================

CREATE DATABASE IF NOT EXISTS schemaplexai_costs;

-- ============================================================
-- agent_timeline_event
-- Stores every agent execution event for historical replay,
-- auditing, and debugging. Written async via TimelineClickHouseService.
-- ============================================================
CREATE TABLE IF NOT EXISTS schemaplexai_costs.agent_timeline_event
(
    execution_id    UInt64           COMMENT 'FK to sf_agent_execution.id',
    event_type      LowCardinality(String) COMMENT 'Event category: thought, tool_call, tool_result, plan, output, error, etc.',
    content         String           COMMENT 'Human-readable event content',
    metadata_json   String           COMMENT 'Full event payload as JSON (tool names, parameters, trace_id, etc.)',
    tenant_id       UInt64           COMMENT 'Multi-tenant isolation',
    created_at      DateTime64(3)    COMMENT 'Event creation timestamp (UTC)',

    -- Indexes
    INDEX idx_event_type event_type TYPE bloom_filter() GRANULARITY 3,
    INDEX idx_tenant tenant_id TYPE bloom_filter() GRANULARITY 3
)
ENGINE = MergeTree()
PARTITION BY toYYYYMMDD(created_at)
ORDER BY (tenant_id, execution_id, created_at)
TTL created_at + INTERVAL 90 DAY
SETTINGS index_granularity = 8192
COMMENT 'Agent execution timeline events — SSE events persisted for audit and replay';
