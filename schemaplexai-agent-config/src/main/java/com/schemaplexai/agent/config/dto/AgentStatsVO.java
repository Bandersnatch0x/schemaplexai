package com.schemaplexai.agent.config.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tenant-scoped agent statistics for the Cockpit page
 * (GET /agent-config/agents/stats, issue 927).
 *
 * <p>Fields map 1:1 to the Cockpit stat cards plus the live status bar:</p>
 * <ul>
 *   <li>{@code activeAgents} — "Active Agents" card + orbit center + status bar</li>
 *   <li>{@code totalExecutions} — "Executions" card</li>
 *   <li>{@code totalTokens} — "Tokens Used" card</li>
 *   <li>{@code pendingApprovals} — "Pending Review" card</li>
 *   <li>{@code totalAgents}/{@code runningExecutions}/{@code todayExecutions} — supplementary counts</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentStatsVO {

    /** Total non-deleted agents of the tenant. */
    private long totalAgents;

    /** Agents with status ACTIVE. */
    private long activeAgents;

    /** Executions currently in a non-terminal state. */
    private long runningExecutions;

    /** Total executions of the tenant. */
    private long totalExecutions;

    /** Executions created today (database-local date). */
    private long todayExecutions;

    /** Sum of token_count over the tenant's chat messages. */
    private long totalTokens;

    /** Executions paused awaiting manual approval. */
    private long pendingApprovals;
}
