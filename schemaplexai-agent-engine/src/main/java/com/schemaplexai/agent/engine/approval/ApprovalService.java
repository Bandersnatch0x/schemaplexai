package com.schemaplexai.agent.engine.approval;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.mapper.SfAgentExecutionMapper;
import com.schemaplexai.agent.engine.state.AgentExecutionState;
import com.schemaplexai.agent.engine.state.AgentStateMachine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the HITL (Human-in-the-Loop) approval lifecycle for agent executions.
 * <p>
 * When an execution requires human approval (e.g. a high-risk action), this service:
 * <ol>
 *     <li>Stores the approval request in memory</li>
 *     <li>Records an audit event</li>
 *     <li>Waits for a human decision</li>
 *     <li>Transitions the execution based on the decision</li>
 * </ol>
 * <p>
 * v1 implementation — requests are stored in-memory only (no database persistence).
 */
@Slf4j
@Service
public class ApprovalService {

    /** Default extension added to the deadline when a request is deferred. */
    private static final Duration DEFER_EXTENSION = Duration.ofMinutes(30);

    private final Map<String, ApprovalRequest> pendingRequests = new ConcurrentHashMap<>();
    private final AgentStateMachine stateMachine;
    private final SfAgentExecutionMapper executionMapper;
    private final AuditTrail auditTrail;

    public ApprovalService(AgentStateMachine stateMachine,
                           SfAgentExecutionMapper executionMapper,
                           AuditTrail auditTrail) {
        this.stateMachine = stateMachine;
        this.executionMapper = executionMapper;
        this.auditTrail = auditTrail;
    }

    /**
     * Submits an approval request. The associated execution must already be in PAUSED state.
     *
     * @param request the approval request to store
     * @throws IllegalArgumentException if a pending request already exists for this execution
     */
    public void requestApproval(ApprovalRequest request) {
        if (pendingRequests.containsKey(request.executionId())) {
            throw new IllegalArgumentException(
                    "Approval request already pending for execution " + request.executionId());
        }

        pendingRequests.put(request.executionId(), request);
        log.info("Approval requested for execution {}, agent {}, risk={}",
                request.executionId(), request.agentId(), request.riskLevel());

        auditTrail.record(new AuditEntry(
                request.executionId(),
                request.agentId(),
                "APPROVAL_REQUESTED",
                String.format("Action: %s, Risk: %s, Deadline: %s",
                        request.actionDescription(), request.riskLevel(), request.deadline()),
                request.requestedAt()
        ));
    }

    /**
     * Records a human decision on a pending approval request and triggers the
     * appropriate state transition.
     *
     * @param executionId the execution the decision applies to
     * @param decision    the approver's decision
     * @throws IllegalArgumentException if no pending request exists for the execution
     */
    public void decide(String executionId, ApprovalDecision decision) {
        ApprovalRequest request = pendingRequests.remove(executionId);
        if (request == null) {
            throw new IllegalArgumentException(
                    "No pending approval request for execution " + executionId);
        }

        log.info("Approval decision for execution {}: {} by approver {}, reason={}",
                executionId, decision.action(), decision.approverId(), decision.reason());

        SfAgentExecution execution = executionMapper.selectById(parseLong(executionId));
        if (execution == null) {
            log.error("Execution {} not found when processing approval decision", executionId);
            throw new IllegalArgumentException("Execution not found: " + executionId);
        }

        switch (decision.action()) {
            case APPROVE -> handleApprove(execution, decision);
            case REJECT -> handleReject(execution, decision);
            case DEFER -> handleDefer(executionId, request, decision);
        }
    }

    /**
     * Checks whether the given execution has a pending approval request.
     */
    public boolean hasPendingRequest(String executionId) {
        return pendingRequests.containsKey(executionId);
    }

    /**
     * Returns the pending request for the given execution, if any.
     */
    public Optional<ApprovalRequest> getPendingRequest(String executionId) {
        return Optional.ofNullable(pendingRequests.get(executionId));
    }

    /**
     * Removes expired approval requests and records them as expired in the audit trail.
     * Intended to be called periodically (e.g. via a scheduled task).
     */
    public int expireStaleRequests(Instant now) {
        int expired = 0;
        var iterator = pendingRequests.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            ApprovalRequest request = entry.getValue();
            if (request.deadline() != null && request.deadline().isBefore(now)) {
                iterator.remove();
                auditTrail.record(new AuditEntry(
                        request.executionId(),
                        "SYSTEM",
                        "APPROVAL_EXPIRED",
                        "Deadline passed without decision",
                        now
                ));
                log.warn("Approval request expired for execution {}", request.executionId());
                expired++;
            }
        }
        return expired;
    }

    // ---- internal helpers ----

    private void handleApprove(SfAgentExecution execution, ApprovalDecision decision) {
        auditTrail.record(new AuditEntry(
                decision.executionId(),
                decision.approverId(),
                "APPROVED",
                decision.reason(),
                decision.decidedAt()
        ));
        stateMachine.transition(AgentExecutionState.THINKING, execution);
    }

    private void handleReject(SfAgentExecution execution, ApprovalDecision decision) {
        auditTrail.record(new AuditEntry(
                decision.executionId(),
                decision.approverId(),
                "REJECTED",
                decision.reason(),
                decision.decidedAt()
        ));
        execution.setMetadata("rejectionReason", decision.reason());
        stateMachine.transition(AgentExecutionState.FAILED, execution);
    }

    private void handleDefer(String executionId, ApprovalRequest original, ApprovalDecision decision) {
        Instant newDeadline = original.deadline() != null
                ? original.deadline().plus(DEFER_EXTENSION)
                : Instant.now().plus(DEFER_EXTENSION);

        ApprovalRequest deferred = new ApprovalRequest(
                original.executionId(),
                original.agentId(),
                original.tenantId(),
                original.actionDescription(),
                original.riskLevel(),
                original.requestedAt(),
                newDeadline
        );
        pendingRequests.put(executionId, deferred);

        auditTrail.record(new AuditEntry(
                decision.executionId(),
                decision.approverId(),
                "DEFERRED",
                String.format("Reason: %s, New deadline: %s", decision.reason(), newDeadline),
                decision.decidedAt()
        ));
        // Execution stays PAUSED — no state transition
        log.info("Approval deferred for execution {}, new deadline: {}", executionId, newDeadline);
    }

    private static Long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid execution ID: " + value, e);
        }
    }
}
