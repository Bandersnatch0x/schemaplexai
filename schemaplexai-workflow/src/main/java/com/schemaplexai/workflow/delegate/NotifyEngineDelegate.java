package com.schemaplexai.workflow.delegate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.model.event.ApprovalDecisionEvent;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * BPMN service task delegate that publishes an ApprovalDecisionEvent to MQ.
 *
 * <p>Called at the end of the approval workflow (both approve and reject paths).
 * The Engine's ApprovalDecisionConsumer picks up the event and transitions state.
 *
 * <p>Reads decision from process variable "approvalDecision" (set by human task or timer).
 * Falls back to "APPROVE" if not set (auto-approve path).
 */
@Slf4j
@Component
public class NotifyEngineDelegate implements JavaDelegate {

    private static final String EXCHANGE = "approval";
    private static final String ROUTING_KEY_DECISIONS = "approval.decisions";

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public NotifyEngineDelegate(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String ticketId = (String) execution.getVariable("ticketId");
        String executionIdStr = execution.getVariable("executionId") != null
                ? execution.getVariable("executionId").toString() : null;
        String approverId = (String) execution.getVariable("approverId");
        Integer decisionVersion = execution.getVariable("decisionVersion") != null
                ? Integer.parseInt(execution.getVariable("decisionVersion").toString()) : 0;
        Integer expectedExecutionVersion = execution.getVariable("expectedExecutionVersion") != null
                ? Integer.parseInt(execution.getVariable("expectedExecutionVersion").toString()) : 0;

        // Decision from human task form, or default to APPROVE for auto-approve paths
        String decisionValue = (String) execution.getVariable("approvalDecision");
        if (decisionValue == null || decisionValue.isBlank()) {
            decisionValue = "APPROVE";
        }

        String reasonValue = (String) execution.getVariable("rejectionReason");
        if (reasonValue == null || reasonValue.isBlank()) {
            reasonValue = (String) execution.getVariable("escalationReason");
        }

        ApprovalDecisionEvent event = new ApprovalDecisionEvent(
                UUID.fromString(ticketId),
                executionIdStr != null ? Long.parseLong(executionIdStr) : null,
                decisionValue,
                approverId,
                reasonValue,
                Instant.now(),
                decisionVersion,
                expectedExecutionVersion
        );

        try {
            String json = objectMapper.writeValueAsString(event);
            rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY_DECISIONS, json);
            log.info("[ApprovalWF] Published {} decision for ticket={}, execution={}, approver={}",
                    decisionValue, ticketId, executionIdStr, approverId);
        } catch (Exception e) {
            log.error("[ApprovalWF] Failed to publish approval decision for ticket={}", ticketId, e);
            throw new RuntimeException("Failed to notify engine of approval decision", e);
        }
    }
}
