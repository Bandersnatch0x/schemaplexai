package com.schemaplexai.workflow.delegate;

import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Initializes the approval process context.
 * Sets default values for escalationLevel and slaTimeoutHours.
 */
@Slf4j
@Component
public class InitApprovalDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        String ticketId = (String) execution.getVariable("ticketId");
        Long executionId = execution.getVariable("executionId") != null
                ? Long.valueOf(execution.getVariable("executionId").toString()) : null;
        String riskLevel = (String) execution.getVariable("riskLevel");

        if (ticketId == null || executionId == null) {
            throw new IllegalArgumentException("ticketId and executionId are required process variables");
        }

        // Set defaults
        if (execution.getVariable("escalationLevel") == null) {
            execution.setVariable("escalationLevel", 0);
        }
        if (execution.getVariable("slaTimeoutHours") == null) {
            execution.setVariable("slaTimeoutHours", "CRITICAL".equals(riskLevel) ? 2 : 4);
        }

        log.info("[ApprovalWF] Initialized: ticketId={}, executionId={}, riskLevel={}, slaHours={}",
                ticketId, executionId, riskLevel, execution.getVariable("slaTimeoutHours"));
    }
}
