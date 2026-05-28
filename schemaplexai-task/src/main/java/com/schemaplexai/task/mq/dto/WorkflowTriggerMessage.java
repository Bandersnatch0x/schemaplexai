package com.schemaplexai.task.mq.dto;

import lombok.Data;

import java.util.Map;

@Data
public class WorkflowTriggerMessage {

    private String workflowDefinitionKey;

    private String businessKey;

    private Map<String, Object> variables;

    private String tenantId;

    private String triggerSource;

    private String idempotencyKey;
}
