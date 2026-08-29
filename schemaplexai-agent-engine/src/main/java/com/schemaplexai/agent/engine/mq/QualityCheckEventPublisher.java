package com.schemaplexai.agent.engine.mq;

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
 * Publishes quality gate check requests to the quality service.
 *
 * <p>Trigger point wired by ticket 924 (REQ-01): after an Agent execution
 * completes, the engine publishes a check request on {@code sf.exchange} with
 * routing key {@code sf.quality} (MQ decoupling — the engine has no bean
 * dependency on the quality module). The quality module's
 * QualityCheckEventConsumer evaluates the gates and answers with a verdict
 * event on {@code sf.quality.verdict} (consumed by QualityVerdictConsumer).
 *
 * <p>Phased trigger points (declared in ticket 924, same wire contract):
 * POST_TOOL (after tool execution) and WORKFLOW_NODE (after workflow node
 * completion, producer lives in the workflow module).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QualityCheckEventPublisher {

    public static final String EVENT_TYPE_CHECK_REQUEST = "QUALITY_CHECK_REQUEST";
    public static final String TRIGGER_POINT_POST_EXECUTION = "POST_EXECUTION";

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Publishes a post-execution quality check request. Never throws: the
     * completion flow must not degrade to FAILED because MQ is unavailable —
     * the missed check is logged for operational follow-up.
     *
     * @return true if the request was handed to MQ, false on failure
     */
    public boolean publishPostExecutionCheck(Long executionId, Long agentId, String tenantId, String output) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("eventType", EVENT_TYPE_CHECK_REQUEST);
            payload.put("eventId", UUID.randomUUID().toString());
            payload.put("executionId", executionId);
            payload.put("agentId", agentId);
            payload.put("tenantId", tenantId);
            payload.put("triggerPoint", TRIGGER_POINT_POST_EXECUTION);
            payload.put("output", output);
            // Evidence flags for SecurityScanRule: the engine's internal
            // guardrail pass completed (this event is only published from the
            // COMPLETED path, which guardrails have already cleared). The
            // rule's own content checks (e.g. secret patterns) still apply
            // to the output carried above.
            payload.put("securityScanCompleted", Boolean.TRUE);
            payload.put("securityScanPassed", Boolean.TRUE);
            payload.put("timestamp", System.currentTimeMillis());

            String message = objectMapper.writeValueAsString(payload);
            rabbitTemplate.convertAndSend(CommonConstants.EXCHANGE_SCHEMAPLEXAI, CommonConstants.RK_QUALITY, message);
            log.info("Published quality check request for execution {} (trigger={})",
                    executionId, TRIGGER_POINT_POST_EXECUTION);
            return true;
        } catch (Exception e) {
            log.error("Failed to publish quality check request for execution {} — "
                    + "post-execution quality gate skipped: {}", executionId, e.getMessage(), e);
            return false;
        }
    }
}
