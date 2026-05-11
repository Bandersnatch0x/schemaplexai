-- ============================================================
-- M12: Approval Ticket table for FAST + BPMN approval workflows
-- ============================================================

CREATE TABLE IF NOT EXISTS sf_approval_ticket (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT NOT NULL,
    execution_id        BIGINT,
    agent_id            BIGINT,
    stage               VARCHAR(32) NOT NULL DEFAULT 'CREATED',
    approval_mode       VARCHAR(16) NOT NULL,
    risk_level          VARCHAR(32),
    risk_score          INT DEFAULT 0,
    assignee_id         BIGINT,
    approver_pool       TEXT,
    escalation_count    INT NOT NULL DEFAULT 0,
    previous_assignee_id BIGINT,
    assignment_round    INT NOT NULL DEFAULT 0,
    decision            VARCHAR(16),
    decision_reason     TEXT,
    decided_at          TIMESTAMP,
    sla_deadline        TIMESTAMP,
    requested_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    stage_entered_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    bpmn_process_instance_id VARCHAR(128),
    bpmn_task_id        VARCHAR(128),
    metadata_json       TEXT,
    version             INT NOT NULL DEFAULT 0,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          BIGINT,
    updated_by          BIGINT,
    deleted             INT NOT NULL DEFAULT 0
);

-- Indexes for fast inbox queries and SLA checks
CREATE INDEX IF NOT EXISTS idx_approval_ticket_assignee
    ON sf_approval_ticket(assignee_id) WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_approval_ticket_stage
    ON sf_approval_ticket(stage) WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_approval_ticket_sla
    ON sf_approval_ticket(sla_deadline) WHERE stage NOT IN ('APPROVED', 'REJECTED', 'EXPIRED', 'COMPLETED') AND deleted = 0;

CREATE INDEX IF NOT EXISTS idx_approval_ticket_tenant_stage
    ON sf_approval_ticket(tenant_id, stage) WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_approval_ticket_execution
    ON sf_approval_ticket(execution_id) WHERE deleted = 0;

-- Unique constraint: only one active ticket per execution
CREATE UNIQUE INDEX IF NOT EXISTS idx_approval_ticket_execution_active
    ON sf_approval_ticket(execution_id) WHERE stage NOT IN ('APPROVED', 'REJECTED', 'EXPIRED', 'COMPLETED') AND deleted = 0;
