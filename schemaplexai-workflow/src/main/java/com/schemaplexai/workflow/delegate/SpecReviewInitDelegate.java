package com.schemaplexai.workflow.delegate;

import com.schemaplexai.common.context.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Initializes a spec review workflow instance.
 * Sets up process variables, assigns a review tracking ID, and logs the start.
 */
@Slf4j
@Component
public class SpecReviewInitDelegate implements JavaDelegate, ExecutionListener {

    @Override
    public void execute(DelegateExecution execution) {
        String processInstanceId = execution.getProcessInstanceId();
        String businessKey = execution.getProcessInstanceBusinessKey();

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

        // Generate review tracking ID if not present
        if (execution.getVariable("reviewTrackingId") == null) {
            execution.setVariable("reviewTrackingId", UUID.randomUUID().toString());
        }

        // Set default risk level if not provided
        String riskLevel = (String) execution.getVariable("riskLevel");
        if (riskLevel == null || riskLevel.isBlank()) {
            riskLevel = "MEDIUM";
            execution.setVariable("riskLevel", riskLevel);
        }

        // Set default reviewer if not provided
        if (execution.getVariable("reviewerId") == null) {
            execution.setVariable("reviewerId", "reviewer-group");
        }

        // Set submission timestamp
        execution.setVariable("submittedAt", Instant.now().toString());

        // Initialize approval decision
        execution.setVariable("approvalDecision", "PENDING");

        log.info("[SpecReviewInit] process={}, tenantId={}, specId={}, riskLevel={}, trackingId={}",
                processInstanceId, tenantId,
                execution.getVariable("specId"),
                riskLevel,
                execution.getVariable("reviewTrackingId"));
    }

    @Override
    public void notify(DelegateExecution execution) {
        // ExecutionListener entry point — reuse same logic
        execute(execution);
    }
}
