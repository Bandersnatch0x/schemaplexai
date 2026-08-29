package com.schemaplexai.workflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.constants.CommonConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Publishes workflow-node quality check requests (trigger point 3 of ticket 924).
 *
 * <p>Same wire contract as the agent-engine QualityCheckEventPublisher:
 * {@code sf.exchange} / routing key {@code sf.quality}, consumed by the
 * quality module. The node execution id rides in the {@code executionId}
 * field (the consumer requires it); the workflow instance id is carried as
 * an extra field. The verdict channel is engine-scoped today, so workflow
 * verdicts are recorded by the quality module without pausing the flow —
 * disposition handling for workflows is a documented phase-2 item.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowQualityCheckPublisher {

    public static final String EVENT_TYPE_CHECK_REQUEST = "QUALITY_CHECK_REQUEST";
    public static final String TRIGGER_POINT_WORKFLOW_NODE = "WORKFLOW_NODE";

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Best-effort publish: an MQ outage must never fail node execution.
     *
     * @return true if the request was handed to MQ, false on failure
     */
    public boolean publishPostNodeCheck(Long nodeExecutionId, Long workflowInstanceId,
                                        String nodeId, String tenantId, String output) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("eventType", EVENT_TYPE_CHECK_REQUEST);
            payload.put("eventId", UUID.randomUUID().toString());
            payload.put("executionId", nodeExecutionId);
            payload.put("workflowInstanceId", workflowInstanceId);
            payload.put("agentId", null);
            payload.put("tenantId", tenantId);
            payload.put("triggerPoint", TRIGGER_POINT_WORKFLOW_NODE);
            payload.put("output", "[" + nodeId + "] " + output);
            // Node output is not cleared by any guardrail pass — the quality
            // rules must inspect it themselves.
            payload.put("securityScanCompleted", Boolean.FALSE);
            payload.put("securityScanPassed", Boolean.FALSE);
            payload.put("timestamp", System.currentTimeMillis());

            String message = objectMapper.writeValueAsString(payload);
            rabbitTemplate.convertAndSend(CommonConstants.EXCHANGE_SCHEMAPLEXAI, CommonConstants.RK_QUALITY, message);
            log.info("Published quality check request for workflow node execution {} (instance={})",
                    nodeExecutionId, workflowInstanceId);
            return true;
        } catch (Exception e) {
            log.error("Failed to publish workflow node quality check for node execution {} — "
                    + "gate skipped: {}", nodeExecutionId, e.getMessage());
            return false;
        }
    }
}
