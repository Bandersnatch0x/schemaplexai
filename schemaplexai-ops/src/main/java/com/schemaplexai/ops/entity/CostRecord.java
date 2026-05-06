package com.schemaplexai.ops.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Maps to the ClickHouse table {@code schemaplexai_costs.sf_cost_record}.
 * <p>
 * This is a plain POJO (not a JPA entity) because ClickHouse is accessed
 * via JDBC directly — there is no JPA provider for ClickHouse in this project.
 * Used by {@link com.schemaplexai.ops.service.ClickHouseCostSyncService}
 * and {@link com.schemaplexai.ops.service.CostService} for cost analytics.
 */
@Data
public class CostRecord {

    /** Multi-tenant shard key. */
    private Long tenantId;

    /** Unique record identifier (UUID v7 preferred). */
    private String recordId;

    /** Service that initiated the call (e.g. agent-engine, workflow). */
    private String serviceName;

    /** LLM model name (e.g. gpt-4o, claude-3-5-sonnet). */
    private String modelName;

    /** Model provider (e.g. openai, anthropic, azure). */
    private String provider;

    /** Request category (chat, embedding, tool_call, image). */
    private String requestType;

    /** Number of input/prompt tokens consumed. */
    private Long inputTokens;

    /** Number of output/completion tokens consumed. */
    private Long outputTokens;

    /** Total tokens (input + output). */
    private Long totalTokens;

    /** Cost amount in the specified currency. */
    private BigDecimal costAmount;

    /** ISO 4217 currency code (e.g. USD, CNY). */
    private String currency;

    /** Record creation timestamp (UTC). */
    private LocalDateTime createdAt;

    /** FK to sf_agent_execution.id. */
    private Long executionId;

    /** FK to sf_agent.id. */
    private Long agentId;

    /** FK to workflow instance if applicable. */
    private Long workflowInstanceId;
}
