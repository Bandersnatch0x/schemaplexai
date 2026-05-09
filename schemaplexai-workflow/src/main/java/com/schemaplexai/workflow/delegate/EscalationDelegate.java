package com.schemaplexai.workflow.delegate;

import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Handles approval escalation: increments escalation level and logs the event.
 *
 * <p>Called when:
 * <ul>
 *   <li>Primary approver clicks "Escalate"</li>
 *   <li>SLA timer fires on primary approval task</li>
 * </ul>
 *
 * <p>Escalation levels:
 * <ul>
 *   <li>0 → 1: Escalate to senior-approvers group</li>
 *   <li>1 → 2: Final level — SLA timeout leads to auto-rejection</li>
 * </ul>
 */
@Slf4j
@Component
public class EscalationDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        int currentLevel = execution.getVariable("escalationLevel") != null
                ? Integer.parseInt(execution.getVariable("escalationLevel").toString()) : 0;
        int newLevel = currentLevel + 1;

        execution.setVariable("escalationLevel", newLevel);

        String ticketId = (String) execution.getVariable("ticketId");
        String executionId = execution.getVariable("executionId") != null
                ? execution.getVariable("executionId").toString() : null;
        String riskLevel = (String) execution.getVariable("riskLevel");

        // Update SLA for escalated level: 24h for level 1
        if (newLevel == 1) {
            execution.setVariable("slaTimeoutHours", 24);
        }

        log.info("[ApprovalWF] Escalated ticket={} execution={} from level {} to {} (risk={})",
                ticketId, executionId, currentLevel, newLevel, riskLevel);

        // Store escalation audit trail
        String escalationNote = String.format("Escalated from level %d to %d at %s",
                currentLevel, newLevel, java.time.Instant.now());
        execution.setVariable("escalationHistory", escalationNote);
    }
}
