-- M7.8: Add sf_execution_snapshot table (agent-engine module)

CREATE TABLE IF NOT EXISTS sf_execution_snapshot (
    id              BIGSERIAL PRIMARY KEY,
    execution_id    BIGINT NOT NULL,
    state_json      TEXT NOT NULL,
    version         INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_snapshot_execution_id ON sf_execution_snapshot(execution_id);
