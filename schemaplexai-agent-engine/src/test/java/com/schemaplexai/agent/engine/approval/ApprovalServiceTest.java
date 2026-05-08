package com.schemaplexai.agent.engine.approval;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.mapper.SfAgentExecutionMapper;
import com.schemaplexai.agent.engine.state.AgentExecutionState;
import com.schemaplexai.agent.engine.state.AgentStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApprovalServiceTest {

    @Mock
    private AgentStateMachine stateMachine;

    @Mock
    private SfAgentExecutionMapper executionMapper;

    @Mock
    private AuditTrail auditTrail;

    @InjectMocks
    private ApprovalService approvalService;

    private ApprovalRequest sampleRequest;
    private SfAgentExecution sampleExecution;
    private static final String EXEC_ID = "100";

    @BeforeEach
    void setUp() {
        sampleRequest = new ApprovalRequest(
                EXEC_ID,
                "agent-1",
                "tenant-1",
                "Deploy to production",
                "HIGH",
                Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-01T11:00:00Z")
        );

        sampleExecution = new SfAgentExecution();
        sampleExecution.setId(100L);
        sampleExecution.setAgentId(1L);
        sampleExecution.setState(AgentExecutionState.PAUSED.name());
    }

    // ---- requestApproval ----

    @Test
    void requestApprovalShouldStoreRequestAndRecordAudit() {
        approvalService.requestApproval(sampleRequest);

        assertThat(approvalService.hasPendingRequest(EXEC_ID)).isTrue();
        Optional<ApprovalRequest> stored = approvalService.getPendingRequest(EXEC_ID);
        assertThat(stored).isPresent();
        assertThat(stored.get().actionDescription()).isEqualTo("Deploy to production");
        assertThat(stored.get().riskLevel()).isEqualTo("HIGH");

        verify(auditTrail).record(any(AuditEntry.class));
    }

    @Test
    void requestApprovalShouldThrowOnDuplicateRequest() {
        approvalService.requestApproval(sampleRequest);

        assertThatThrownBy(() -> approvalService.requestApproval(sampleRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already pending");
    }

    // ---- decide APPROVE ----

    @Test
    void decideApproveShouldTransitionToThinking() {
        approvalService.requestApproval(sampleRequest);
        when(executionMapper.selectById(100L)).thenReturn(sampleExecution);

        ApprovalDecision decision = new ApprovalDecision(
                EXEC_ID, "approver-1", ApprovalAction.APPROVE, "Looks safe", Instant.now()
        );
        approvalService.decide(EXEC_ID, decision);

        ArgumentCaptor<AgentExecutionState> stateCaptor = ArgumentCaptor.forClass(AgentExecutionState.class);
        verify(stateMachine).transition(stateCaptor.capture(), eq(sampleExecution));
        assertThat(stateCaptor.getValue()).isEqualTo(AgentExecutionState.THINKING);

        assertThat(approvalService.hasPendingRequest(EXEC_ID)).isFalse();
    }

    @Test
    void decideApproveShouldRecordAuditTrail() {
        approvalService.requestApproval(sampleRequest);
        when(executionMapper.selectById(100L)).thenReturn(sampleExecution);

        ApprovalDecision decision = new ApprovalDecision(
                EXEC_ID, "approver-1", ApprovalAction.APPROVE, "approved", Instant.now()
        );
        approvalService.decide(EXEC_ID, decision);

        // Two audit calls: one for request, one for decision
        verify(auditTrail, times(2)).record(any(AuditEntry.class));
    }

    // ---- decide REJECT ----

    @Test
    void decideRejectShouldTransitionToFailed() {
        approvalService.requestApproval(sampleRequest);
        when(executionMapper.selectById(100L)).thenReturn(sampleExecution);

        ApprovalDecision decision = new ApprovalDecision(
                EXEC_ID, "approver-2", ApprovalAction.REJECT, "Too risky", Instant.now()
        );
        approvalService.decide(EXEC_ID, decision);

        ArgumentCaptor<AgentExecutionState> stateCaptor = ArgumentCaptor.forClass(AgentExecutionState.class);
        verify(stateMachine).transition(stateCaptor.capture(), eq(sampleExecution));
        assertThat(stateCaptor.getValue()).isEqualTo(AgentExecutionState.FAILED);
    }

    @Test
    void decideRejectShouldSetRejectionReasonInMetadata() {
        approvalService.requestApproval(sampleRequest);
        when(executionMapper.selectById(100L)).thenReturn(sampleExecution);

        ApprovalDecision decision = new ApprovalDecision(
                EXEC_ID, "approver-2", ApprovalAction.REJECT, "Too risky", Instant.now()
        );
        approvalService.decide(EXEC_ID, decision);

        assertThat(sampleExecution.getMetadata("rejectionReason")).isEqualTo("Too risky");
    }

    // ---- decide DEFER ----

    @Test
    void decideDeferShouldKeepRequestPendingWithExtendedDeadline() {
        approvalService.requestApproval(sampleRequest);
        when(executionMapper.selectById(100L)).thenReturn(sampleExecution);

        ApprovalDecision decision = new ApprovalDecision(
                EXEC_ID, "approver-3", ApprovalAction.DEFER, "Need more info", Instant.now()
        );
        approvalService.decide(EXEC_ID, decision);

        // Request should still be pending
        assertThat(approvalService.hasPendingRequest(EXEC_ID)).isTrue();

        // Deadline should be extended by 30 minutes
        Optional<ApprovalRequest> deferred = approvalService.getPendingRequest(EXEC_ID);
        assertThat(deferred).isPresent();
        Instant originalDeadline = sampleRequest.deadline();
        assertThat(deferred.get().deadline()).isEqualTo(originalDeadline.plus(java.time.Duration.ofMinutes(30)));

        // No state transition should occur
        verify(stateMachine, never()).transition(any(), any());
    }

    @Test
    void decideDeferShouldRecordAuditTrail() {
        approvalService.requestApproval(sampleRequest);
        when(executionMapper.selectById(100L)).thenReturn(sampleExecution);

        ApprovalDecision decision = new ApprovalDecision(
                EXEC_ID, "approver-3", ApprovalAction.DEFER, "waiting", Instant.now()
        );
        approvalService.decide(EXEC_ID, decision);

        // Two audit calls: one for request, one for defer
        verify(auditTrail, times(2)).record(any(AuditEntry.class));
    }

    // ---- error cases ----

    @Test
    void decideShouldThrowWhenNoPendingRequest() {
        ApprovalDecision decision = new ApprovalDecision(
                "nonexistent", "approver-1", ApprovalAction.APPROVE, "ok", Instant.now()
        );

        assertThatThrownBy(() -> approvalService.decide("nonexistent", decision))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No pending approval request");
    }

    @Test
    void decideShouldThrowWhenExecutionNotFound() {
        approvalService.requestApproval(sampleRequest);
        when(executionMapper.selectById(100L)).thenReturn(null);

        ApprovalDecision decision = new ApprovalDecision(
                EXEC_ID, "approver-1", ApprovalAction.APPROVE, "ok", Instant.now()
        );

        assertThatThrownBy(() -> approvalService.decide(EXEC_ID, decision))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Execution not found");
    }

    // ---- deadline enforcement ----

    @Test
    void expireStaleRequestsShouldRemoveExpiredRequests() {
        ApprovalRequest expiredRequest = new ApprovalRequest(
                "200", "agent-2", "tenant-1", "Old action", "LOW",
                Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-01T10:30:00Z")
        );
        approvalService.requestApproval(expiredRequest);

        int expired = approvalService.expireStaleRequests(Instant.parse("2026-01-01T11:00:00Z"));

        assertThat(expired).isEqualTo(1);
        assertThat(approvalService.hasPendingRequest("200")).isFalse();
        verify(auditTrail, times(2)).record(any(AuditEntry.class)); // request + expire
    }

    @Test
    void expireStaleRequestsShouldNotRemoveNonExpiredRequests() {
        ApprovalRequest activeRequest = new ApprovalRequest(
                "300", "agent-3", "tenant-1", "Active action", "MEDIUM",
                Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-01T12:00:00Z")
        );
        approvalService.requestApproval(activeRequest);

        int expired = approvalService.expireStaleRequests(Instant.parse("2026-01-01T11:00:00Z"));

        assertThat(expired).isEqualTo(0);
        assertThat(approvalService.hasPendingRequest("300")).isTrue();
    }

    @Test
    void expireStaleRequestsShouldReturnZeroWhenNothingToExpire() {
        int expired = approvalService.expireStaleRequests(Instant.now());
        assertThat(expired).isEqualTo(0);
    }
}
