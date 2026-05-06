package com.schemaplexai.task.mq.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * MQ payload for agent execution dispatch requests.
 */
@Data
public class AgentExecuteMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long agentId;
    private String tenantId;
    private String prompt;
    private String conversationId;
    private String idempotencyKey;
}
