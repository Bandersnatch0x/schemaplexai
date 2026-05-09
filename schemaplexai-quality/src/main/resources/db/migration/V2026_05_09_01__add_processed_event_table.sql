-- M7.8: Add sf_processed_event table (quality module)
-- Note: This table also exists in agent-engine migration V2026_05_09__execution_control_plane_tables.sql
-- as part of the inbox deduplication pattern. This migration ensures the table is present
-- when the quality module owns its own schema lifecycle.

CREATE TABLE IF NOT EXISTS sf_processed_event (
    event_id        UUID NOT NULL,
    consumer_name   VARCHAR(64) NOT NULL,
    processed_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (event_id, consumer_name)
);
