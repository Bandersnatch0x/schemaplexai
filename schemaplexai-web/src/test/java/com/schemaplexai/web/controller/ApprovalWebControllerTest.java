package com.schemaplexai.web.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.web.service.approval.ApprovalWorkflowPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("M6.4: Approval Web Controller Tests")
class ApprovalWebControllerTest {

    @Mock
    private ApprovalWorkflowPort approvalWorkflowPort;

    @InjectMocks
    private ApprovalWebController controller;

    @Test
    @DisplayName("GET /web/approvals delegates to approval workflow port")
    void listPendingApprovals_delegatesToApprovalWorkflowPort() {
        List<Map<String, Object>> approvals = List.of(Map.of("ticketId", "ticket-1"));
        when(approvalWorkflowPort.listPendingApprovals("10")).thenReturn(approvals);

        Result<List<Map<String, Object>>> result = controller.listPendingApprovals("10");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(approvals);
        verify(approvalWorkflowPort).listPendingApprovals("10");
    }

    @Test
    @DisplayName("POST /web/approvals/{ticketId}/approve delegates to approval workflow port")
    void approve_delegatesToApprovalWorkflowPort() {
        String ticketId = UUID.randomUUID().toString();

        Result<Void> result = controller.approve(ticketId, "APPROVER:user-1", "Approved by manager");

        assertThat(result.getCode()).isEqualTo(200);
        verify(approvalWorkflowPort).approve(ticketId, "APPROVER:user-1", "Approved by manager");
    }

    @Test
    @DisplayName("POST /web/approvals/{ticketId}/reject delegates to approval workflow port")
    void reject_delegatesToApprovalWorkflowPort() {
        String ticketId = UUID.randomUUID().toString();

        Result<Void> result = controller.reject(ticketId, "APPROVER:user-1", "Insufficient budget");

        assertThat(result.getCode()).isEqualTo(200);
        verify(approvalWorkflowPort).reject(ticketId, "APPROVER:user-1", "Insufficient budget");
    }

    @Test
    @DisplayName("POST /web/approvals/{ticketId}/escalate delegates to approval workflow port")
    void escalate_delegatesToApprovalWorkflowPort() {
        String ticketId = UUID.randomUUID().toString();

        Result<Void> result = controller.escalate(ticketId, "APPROVER:user-1");

        assertThat(result.getCode()).isEqualTo(200);
        verify(approvalWorkflowPort).escalate(ticketId, "APPROVER:user-1");
    }
}
