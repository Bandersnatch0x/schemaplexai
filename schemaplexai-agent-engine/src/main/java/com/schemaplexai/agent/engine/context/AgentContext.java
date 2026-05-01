package com.schemaplexai.agent.engine.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent execution context carrying tenant, project, and conversation metadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentContext {

    private String tenantId;
    private String projectId;
    private String conversationId;
    private Long agentId;
    private String userId;
}
