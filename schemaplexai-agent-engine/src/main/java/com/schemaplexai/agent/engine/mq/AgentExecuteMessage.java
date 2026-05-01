package com.schemaplexai.agent.engine.mq;

import lombok.Data;

@Data
public class AgentExecuteMessage {
    private Long agentId;
    private String tenantId;
    private String prompt;
}
