package com.schemaplexai.agent.engine.approval;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.state.AgentExecutionState;
import com.schemaplexai.agent.engine.state.AgentStateMachine;
import com.schemaplexai.agent.engine.tool.ToolCall;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Integrates tool execution with the HITL approval lifecycle.
 *
 * <p>When a tool call requires approval (based on {@link ToolRiskClassifier} and
 * the configured {@link ApprovalMode}), this service:
 * <ol>
 *   <li>Creates an {@link ApprovalRequest}</li>
 *   <li>Submits it to {@link ApprovalService}</li>
 *   <li>Transitions the execution to PAUSED state</li>
 * </ol>
 *
 * <p>The tool execution is deferred until an approver decides on the request.
 */
@Slf4j
@Service
public class ToolApprovalService {

    private static final long DEFAULT_DEADLINE_MINUTES = 60;

    private final ApprovalService approvalService;
    private final ToolRiskClassifier riskClassifier;
    private final ApprovalMode approvalMode;
    private final long deadlineMinutes;

    public ToolApprovalService(ApprovalService approvalService,
                               ToolRiskClassifier riskClassifier,
                               @Value("${agent.approval.mode:AUTO}") ApprovalMode approvalMode,
                               @Value("${agent.approval.deadline-minutes:60}") long deadlineMinutes) {
        this.approvalService = approvalService;
        this.riskClassifier = riskClassifier;
        this.approvalMode = approvalMode;
        this.deadlineMinutes = deadlineMinutes;
    }

    /**
     * Checks if a tool call requires approval and, if so, submits an approval request
     * and pauses the execution.
     *
     * @param toolCall  the tool call to check
     * @param execution the current execution
     * @param stateMachine the state machine for pausing
     * @return {@code true} if approval was required and execution was paused,
     *         {@code false} if the tool can execute immediately
     */
    public boolean checkAndRequestApproval(ToolCall toolCall,
                                           SfAgentExecution execution,
                                           AgentStateMachine stateMachine) {
        if (!riskClassifier.requiresApproval(toolCall, execution.getTenantId(), approvalMode)) {
            return false;
        }

        String riskLevel = riskClassifier.classify(toolCall, execution.getTenantId());
        log.info("Tool {} requires approval (risk={}, mode={}) for execution {}",
                toolCall.toolName(), riskLevel, approvalMode, execution.getId());

        // Create approval request
        ApprovalRequest request = new ApprovalRequest(
                String.valueOf(execution.getId()),
                String.valueOf(execution.getAgentId()),
                execution.getTenantId(),
                String.format("Execute tool '%s' with parameters %s",
                        toolCall.toolName(), toolCall.parameters()),
                riskLevel,
                Instant.now(),
                Instant.now().plus(deadlineMinutes, ChronoUnit.MINUTES)
        );

        try {
            approvalService.requestApproval(request);

            // Store the pending tool info in execution metadata for later resumption
            execution.setMetadata("pendingApprovalTool", toolCall.toolName());
            execution.setMetadata("pendingApprovalParams", toolCall.parameters());
            execution.setMetadata("approvalMode", approvalMode.name());

            // Transition to PAUSED state
            stateMachine.transition(AgentExecutionState.PAUSED, execution);
            return true;
        } catch (IllegalArgumentException e) {
            log.warn("Failed to submit approval request for execution {}: {}",
                    execution.getId(), e.getMessage());
            return false;
        }
    }

    /**
     * Returns the current approval mode.
     */
    public ApprovalMode getApprovalMode() {
        return approvalMode;
    }

    /**
     * Checks if the given execution has a pending approval request.
     */
    public boolean hasPendingApproval(String executionId) {
        return approvalService.hasPendingRequest(executionId);
    }
}
