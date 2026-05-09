package com.schemaplexai.workflow.delegate;

import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

/**
 * Execution listener that sets approvalDecision=REJECT for SLA timeout auto-rejection.
 * Placed on the notifyAutoReject service task's start event.
 */
@Slf4j
@Component
public class SlaAutoRejectListener implements ExecutionListener {

    @Override
    public void notify(DelegateExecution execution) {
        execution.setVariable("approvalDecision", "REJECT");
        execution.setVariable("rejectionReason", "SLA timeout: no response within escalation window");

        String ticketId = (String) execution.getVariable("ticketId");
        int level = execution.getVariable("escalationLevel") != null
                ? Integer.parseInt(execution.getVariable("escalationLevel").toString()) : 0;
        log.info("[ApprovalWF] SLA auto-reject at level {} for ticket={}", level, ticketId);
    }
}
