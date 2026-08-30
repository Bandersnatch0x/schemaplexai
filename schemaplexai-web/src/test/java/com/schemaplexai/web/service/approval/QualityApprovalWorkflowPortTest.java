package com.schemaplexai.web.service.approval;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.quality.entity.ApprovalTicket;
import com.schemaplexai.quality.service.ApprovalTicketService;
import com.schemaplexai.web.mapper.ApprovalMapper;
import com.schemaplexai.web.vo.ApprovalVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Approval workflow web port")
class QualityApprovalWorkflowPortTest {

    @Mock
    private ObjectProvider<ApprovalTicketService> approvalTicketServiceProvider;

    @Mock
    private ApprovalTicketService approvalTicketService;

    @Mock
    private ApprovalMapper approvalMapper;

    @InjectMocks
    private QualityApprovalWorkflowPort port;

    @Test
    @DisplayName("list pending approvals delegates to quality service and maps via ApprovalMapper")
    void listPendingApprovals_delegatesAndMapsViaMapper() {
        UUID ticketId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-05-20T10:15:30Z");
        Instant updatedAt = Instant.parse("2026-05-20T10:16:30Z");
        ApprovalTicket ticket = new ApprovalTicket();
        ticket.setTicketId(ticketId);
        ticket.setExecutionId(101L);
        ticket.setTenantId(10L);
        ticket.setAgentId(20L);
        ticket.setStage("PENDING_FAST");
        ticket.setHandler("FAST");
        ticket.setRiskLevel("HIGH");
        ticket.setActionDescription("Deploy production workflow");
        ticket.setTriggeringSeq(3);
        ticket.setCreatedAt(createdAt);
        ticket.setUpdatedAt(updatedAt);

        ApprovalVO expected = new ApprovalVO();
        expected.setTicketId(ticketId.toString());
        expected.setExecutionId(101L);
        expected.setAgentId(20L);
        expected.setStage("PENDING_FAST");
        expected.setHandler("FAST");
        expected.setRiskLevel("HIGH");
        expected.setActionDescription("Deploy production workflow");
        expected.setTriggeringSeq(3);
        expected.setCreatedAt(createdAt);
        expected.setUpdatedAt(updatedAt);

        when(approvalTicketServiceProvider.getIfAvailable()).thenReturn(approvalTicketService);
        when(approvalTicketService.listPendingByTenant(10L)).thenReturn(List.of(ticket));
        when(approvalMapper.toApprovalVO(ticket)).thenReturn(expected);

        List<ApprovalVO> approvals = port.listPendingApprovals("10");

        assertThat(approvals).hasSize(1);
        ApprovalVO vo = approvals.getFirst();
        assertThat(vo.getTicketId()).isEqualTo(ticketId.toString());
        assertThat(vo.getExecutionId()).isEqualTo(101L);
        assertThat(vo.getAgentId()).isEqualTo(20L);
        assertThat(vo.getStage()).isEqualTo("PENDING_FAST");
        assertThat(vo.getRiskLevel()).isEqualTo("HIGH");
        assertThat(vo.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("approve delegates to quality service with parsed ticket UUID")
    void approve_delegatesWithParsedTicketId() {
        UUID ticketId = UUID.randomUUID();
        when(approvalTicketServiceProvider.getIfAvailable()).thenReturn(approvalTicketService);

        port.approve(ticketId.toString(), "APPROVER:user-1", "Approved by manager");

        verify(approvalTicketService).approve(ticketId, "APPROVER:user-1", "Approved by manager");
    }

    @Test
    @DisplayName("reject delegates to quality service with parsed ticket UUID")
    void reject_delegatesWithParsedTicketId() {
        UUID ticketId = UUID.randomUUID();
        when(approvalTicketServiceProvider.getIfAvailable()).thenReturn(approvalTicketService);

        port.reject(ticketId.toString(), "APPROVER:user-1", "Insufficient budget");

        verify(approvalTicketService).reject(ticketId, "APPROVER:user-1", "Insufficient budget");
    }

    @Test
    @DisplayName("escalate delegates to quality service with parsed ticket UUID")
    void escalate_delegatesWithParsedTicketId() {
        UUID ticketId = UUID.randomUUID();
        when(approvalTicketServiceProvider.getIfAvailable()).thenReturn(approvalTicketService);

        port.escalate(ticketId.toString(), "APPROVER:user-1");

        verify(approvalTicketService).escalate(ticketId, "APPROVER:user-1");
    }

    @Test
    @DisplayName("missing quality service fails explicitly")
    void missingQualityService_throwsExplicitException() {
        when(approvalTicketServiceProvider.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> port.approve(UUID.randomUUID().toString(), "APPROVER:user-1", "ok"))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("Approval workflow service is not available");
    }

    @Test
    @DisplayName("invalid tenant id fails with parameter error")
    void invalidTenantId_throwsParamError() {
        assertThatThrownBy(() -> port.listPendingApprovals("tenant-10"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(400);
    }

    @Test
    @DisplayName("invalid ticket id fails with parameter error")
    void invalidTicketId_throwsParamError() {
        assertThatThrownBy(() -> port.approve("ticket-1", "APPROVER:user-1", "ok"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(400);
    }
}
