package com.schemaplexai.quality.mq.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Quality check request event published by the Agent engine on sf.exchange
 * with routing key {@code sf.quality} (ticket 924).
 *
 * <p>The engine publishes one event per trigger point; {@code triggerPoint}
 * distinguishes them (currently only POST_EXECUTION is wired; POST_TOOL and
 * WORKFLOW_NODE are phased — see ticket 924).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class QualityCheckRequestEvent {

    /** Event type marker, e.g. QUALITY_CHECK_REQUEST. */
    private String eventType;

    /** Producer-generated idempotency key (optional but strongly advised). */
    private String eventId;

    /** Execution the check belongs to (required). */
    private Long executionId;

    private Long agentId;

    /** Tenant id as carried on the execution entity (String vocabulary). */
    private String tenantId;

    /** POST_EXECUTION / POST_TOOL / WORKFLOW_NODE. */
    private String triggerPoint;

    /** Agent output content to scan (nullable). */
    private String output;

    /**
     * Security screening evidence flags consumed by SecurityScanRule: the
     * engine's internal guardrail pass that ran during execution (published
     * only from the COMPLETED path, i.e. after guardrails passed).
     */
    private Boolean securityScanCompleted;
    private Boolean securityScanPassed;

    private Long timestamp;
}
