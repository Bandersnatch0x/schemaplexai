package com.schemaplexai.workflow.delegate;

import com.schemaplexai.common.context.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Initializes an AI agent execution workflow instance.
 * Sets up process variables, validates inputs, and prepares execution context.
 */
@Slf4j
@Component
public class AiAgentInitDelegate implements JavaDelegate, ExecutionListener {

    @Override
    public void execute(DelegateExecution execution) {
        String processInstanceId = execution.getProcessInstanceId();

        // Ensure tenantId is present
        String tenantId = (String) execution.getVariable("tenantId");
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = TenantContextHolder.getTenantId();
            if (tenantId == null || tenantId.isBlank()) {
                tenantId = "default";
            }
            execution.setVariable("tenantId", tenantId);
        }
        TenantContextHolder.setTenantId(tenantId);

        // Validate required variables
        String agentId = (String) execution.getVariable("agentId");
        if (agentId == null || agentId.isBlank()) {
            throw new IllegalArgumentException("agentId is required to start AI agent execution workflow");
        }

        String taskDescription = (String) execution.getVariable("taskDescription");
        if (taskDescription == null || taskDescription.isBlank()) {
            throw new IllegalArgumentException("taskDescription is required to start AI agent execution workflow");
        }

        // Generate execution tracking ID
        execution.setVariable("executionTrackingId", UUID.randomUUID().toString());

        // Set default trust score if not provided
        Object trustScoreObj = execution.getVariable("trustScore");
        double trustScore;
        if (trustScoreObj == null) {
            trustScore = 0.5;
            execution.setVariable("trustScore", trustScore);
        } else if (trustScoreObj instanceof Number number) {
            trustScore = number.doubleValue();
        } else {
            try {
                trustScore = Double.parseDouble(trustScoreObj.toString());
            } catch (NumberFormatException e) {
                trustScore = 0.5;
                execution.setVariable("trustScore", trustScore);
            }
        }

        // Set default human approval requirement
        Object requireApprovalObj = execution.getVariable("requireHumanApproval");
        if (requireApprovalObj == null) {
            // Auto-require approval for low trust scores
            boolean requireApproval = trustScore < 0.8;
            execution.setVariable("requireHumanApproval", requireApproval);
        }

        // Set default userId if not present
        if (execution.getVariable("userId") == null) {
            execution.setVariable("userId", "ai-supervisor");
        }

        // Initialize agent result container
        Map<String, Object> agentResult = new HashMap<>();
        agentResult.put("status", "PENDING");
        execution.setVariable("agentResult", agentResult);

        // Set timestamps
        execution.setVariable("startedAt", Instant.now().toString());
        execution.setVariable("retryCount", 0);

        log.info("[AiAgentInit] process={}, tenantId={}, agentId={}, trustScore={}, requireApproval={}",
                processInstanceId, tenantId, agentId, trustScore,
                execution.getVariable("requireHumanApproval"));
    }

    @Override
    public void notify(DelegateExecution execution) {
        execute(execution);
    }
}
