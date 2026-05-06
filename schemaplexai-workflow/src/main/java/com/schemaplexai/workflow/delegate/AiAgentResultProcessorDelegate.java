package com.schemaplexai.workflow.delegate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Processes and finalizes AI agent execution results.
 * Handles result persistence, quality metrics, and workflow finalization.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiAgentResultProcessorDelegate implements JavaDelegate, ExecutionListener {

    private final ObjectMapper objectMapper;

    @Override
    public void execute(DelegateExecution execution) {
        String processInstanceId = execution.getProcessInstanceId();
        String activityId = execution.getCurrentActivityId();
        String tenantId = resolveTenantId(execution);

        @SuppressWarnings("unchecked")
        Map<String, Object> agentResult = (Map<String, Object>) execution.getVariable("agentResult");
        if (agentResult == null) {
            agentResult = new HashMap<>();
            agentResult.put("status", "UNKNOWN");
        }

        String executionTrackingId = (String) execution.getVariable("executionTrackingId");
        String agentId = (String) execution.getVariable("agentId");

        switch (activityId) {
            case "processResultTask" -> {
                // Intermediate result processing
                agentResult.put("processedAt", Instant.now().toString());
                agentResult.put("processInstanceId", processInstanceId);
                execution.setVariable("agentResult", agentResult);
                log.info("[AiAgentResult] Processed result process={}, agentId={}, trackingId={}",
                        processInstanceId, agentId, executionTrackingId);
            }
            case "finalizeTask" -> {
                // Final result processing
                agentResult.put("finalizedAt", Instant.now().toString());
                agentResult.put("finalStatus", "COMPLETED");
                execution.setVariable("agentResult", agentResult);
                execution.setVariable("finalStatus", "COMPLETED");
                execution.setVariable("completedAt", Instant.now().toString());
                log.info("[AiAgentResult] Finalized process={}, agentId={}, trackingId={}",
                        processInstanceId, agentId, executionTrackingId);
            }
            default -> {
                log.debug("[AiAgentResult] Activity {} result processing", activityId);
            }
        }

        // Persist summary as JSON for external querying
        try {
            Map<String, Object> summary = buildExecutionSummary(execution);
            execution.setVariable("executionSummary", objectMapper.writeValueAsString(summary));
        } catch (Exception e) {
            log.warn("[AiAgentResult] Failed to serialize execution summary", e);
        }
    }

    @Override
    public void notify(DelegateExecution execution) {
        // ExecutionListener entry point for end events
        String eventName = execution.getEventName();
        if ("end".equals(eventName)) {
            String processInstanceId = execution.getProcessInstanceId();
            String tenantId = resolveTenantId(execution);
            String finalStatus = (String) execution.getVariable("finalStatus");
            if (finalStatus == null) {
                finalStatus = "TERMINATED";
                execution.setVariable("finalStatus", finalStatus);
            }
            log.info("[AiAgentResult] Workflow ended process={}, tenantId={}, finalStatus={}",
                    processInstanceId, tenantId, finalStatus);
        }
    }

    private Map<String, Object> buildExecutionSummary(DelegateExecution execution) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("processInstanceId", execution.getProcessInstanceId());
        summary.put("businessKey", execution.getProcessInstanceBusinessKey());
        summary.put("tenantId", execution.getVariable("tenantId"));
        summary.put("agentId", execution.getVariable("agentId"));
        summary.put("taskDescription", execution.getVariable("taskDescription"));
        summary.put("executionTrackingId", execution.getVariable("executionTrackingId"));
        summary.put("finalStatus", execution.getVariable("finalStatus"));
        summary.put("startedAt", execution.getVariable("startedAt"));
        summary.put("completedAt", execution.getVariable("completedAt"));
        summary.put("retryCount", execution.getVariable("retryCount"));
        summary.put("humanApproved", execution.getVariable("humanApproved"));

        @SuppressWarnings("unchecked")
        Map<String, Object> agentResult = (Map<String, Object>) execution.getVariable("agentResult");
        if (agentResult != null) {
            summary.put("qualityScore", agentResult.get("qualityScore"));
            summary.put("agentSuccess", agentResult.get("success"));
        }
        return summary;
    }

    private String resolveTenantId(DelegateExecution execution) {
        String tenantId = (String) execution.getVariable("tenantId");
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = TenantContextHolder.getTenantId();
            if (tenantId == null || tenantId.isBlank()) {
                tenantId = "default";
            }
        }
        return tenantId;
    }
}
