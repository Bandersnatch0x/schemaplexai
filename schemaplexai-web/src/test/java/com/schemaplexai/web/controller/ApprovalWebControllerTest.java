package com.schemaplexai.web.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.web.dto.ApprovalRequest;
import com.schemaplexai.web.dto.EscalationRequest;
import com.schemaplexai.web.service.approval.ApprovalWorkflowPort;
import com.schemaplexai.web.vo.ApprovalVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
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
    @DisplayName("GET /web/approvals delegates to approval workflow port and returns typed VO list")
    void listPendingApprovals_delegatesToApprovalWorkflowPort() {
        ApprovalVO vo = new ApprovalVO();
        vo.setTicketId("ticket-1");
        when(approvalWorkflowPort.listPendingApprovals("10")).thenReturn(List.of(vo));

        Result<List<ApprovalVO>> result = controller.listPendingApprovals("10");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().getFirst().getTicketId()).isEqualTo("ticket-1");
        verify(approvalWorkflowPort).listPendingApprovals("10");
    }

    @Test
    @DisplayName("POST /web/approvals/{ticketId}/approve delegates to approval workflow port with request body")
    void approve_delegatesToApprovalWorkflowPort() {
        String ticketId = UUID.randomUUID().toString();
        ApprovalRequest request = new ApprovalRequest();
        request.setApproverId("APPROVER:user-1");
        request.setReason("Approved by manager");

        Result<Void> result = controller.approve(ticketId, request);

        assertThat(result.getCode()).isEqualTo(200);
        verify(approvalWorkflowPort).approve(ticketId, "APPROVER:user-1", "Approved by manager");
    }

    @Test
    @DisplayName("POST /web/approvals/{ticketId}/reject delegates to approval workflow port with request body")
    void reject_delegatesToApprovalWorkflowPort() {
        String ticketId = UUID.randomUUID().toString();
        ApprovalRequest request = new ApprovalRequest();
        request.setApproverId("APPROVER:user-1");
        request.setReason("Insufficient budget");

        Result<Void> result = controller.reject(ticketId, request);

        assertThat(result.getCode()).isEqualTo(200);
        verify(approvalWorkflowPort).reject(ticketId, "APPROVER:user-1", "Insufficient budget");
    }

    @Test
    @DisplayName("POST /web/approvals/{ticketId}/escalate delegates to approval workflow port with request body")
    void escalate_delegatesToApprovalWorkflowPort() {
        String ticketId = UUID.randomUUID().toString();
        EscalationRequest request = new EscalationRequest();
        request.setEscalatorId("APPROVER:user-1");

        Result<Void> result = controller.escalate(ticketId, request);

        assertThat(result.getCode()).isEqualTo(200);
        verify(approvalWorkflowPort).escalate(ticketId, "APPROVER:user-1");
    }
}
