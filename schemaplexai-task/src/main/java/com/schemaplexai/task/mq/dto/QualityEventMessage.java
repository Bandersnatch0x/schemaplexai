package com.schemaplexai.task.mq.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class QualityEventMessage {

    private String eventType;

    private String projectId;

    private String commitSha;

    private String ruleId;

    private String severity;

    private JsonNode details;

    private String tenantId;
}
