package com.schemaplexai.quality.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.model.event.ApprovalRequestEvent;
import com.schemaplexai.quality.entity.ApprovalTicket;
import com.schemaplexai.quality.mapper.ApprovalTicketMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApprovalTicketService")
class ApprovalTicketServiceTest {

    @Mock
    private ApprovalTicketMapper approvalTicketMapper;

    @Mock
    private ApprovalEscalationService escalationService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private ApprovalTicketService approvalTicketService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        org.springframework.test.util.ReflectionTestUtils.setField(approvalTicketService, "objectMapper", objectMapper);
    }

    // ------------------------------------------------------------------
    // createTicket
    // ------------------------------------------------------------------

    @Test
    @DisplayName("should create ticket with PENDING_FAST stage for FAST request type")
    void createTicket_fastRequest_setsPendingFast() {
        ApprovalRequestEvent event = fastEvent();

        approvalTicketService.createTicket(event);

        ArgumentCaptor<ApprovalTicket> captor = ArgumentCaptor.forClass(ApprovalTicket.class);
        verify(approvalTicketMapper).insert(captor.capture());
        ApprovalTicket ticket = captor.getValue();
        assertThat(ticket.getStage()).isEqualTo("PENDING_FAST");
        assertThat(ticket.getExecutionId()).isEqualTo(1L);
        assertThat(ticket.getTenantId()).isEqualTo(10L);
        assertThat(ticket.getDecisionVersion()).isEqualTo(0);
        assertThat(ticket.getTicketId()).isNotNull();
    }

    @Test
    @DisplayName("should create ticket with PENDING_BPMN stage for BPMN request type")
    void createTicket_bpmnRequest_setsPendingBpmn() {
        ApprovalRequestEvent event = new ApprovalRequestEvent(
                UUID.randomUUID(), 1L, 10L, 2L, 3, "BPMN", "HIGH", "Deploy", 5, Instant.now());

        approvalTicketService.createTicket(event);

        ArgumentCaptor<ApprovalTicket> captor = ArgumentCaptor.forClass(ApprovalTicket.class);
        verify(approvalTicketMapper).insert(captor.capture());
        assertThat(captor.getValue().getStage()).isEqualTo("PENDING_BPMN");
    }

    @Test
    @DisplayName("should throw when event is null")
    void createTicket_nullEvent_throwsParamError() {
        assertThatThrownBy(() -> approvalTicketService.createTicket(null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    @DisplayName("should throw when approvalRequestId is null")
    void createTicket_nullApprovalRequestId_throwsParamError() {
        ApprovalRequestEvent event = new ApprovalRequestEvent(
                null, 1L, 10L, 2L, 3, "FAST", "HIGH", "Deploy", 5, Instant.now());

        assertThatThrownBy(() -> approvalTicketService.createTicket(event))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    // ------------------------------------------------------------------
    // approve
    // ------------------------------------------------------------------

    @Test
    @DisplayName("should approve pending ticket and publish decision event")
    void approve_pendingTicket_updatesToApprovedAndPublishesEvent() {
        ApprovalTicket ticket = pendingFastTicket();
        when(approvalTicketMapper.selectOne(any())).thenReturn(ticket);

        ApprovalTicket result = approvalTicketService.approve(ticket.getTicketId(), "APPROVER:user-1", "Looks good");

        assertThat(result.getStage()).isEqualTo("APPROVED");
        assertThat(result.getDecisionVersion()).isEqualTo(1);
        assertThat(result.getDecidedAt()).isNotNull();
        verify(approvalTicketMapper).updateById(result);
        verify(rabbitTemplate).convertAndSend(eq("approval"), eq("approval.decisions"), any(String.class));
    }

    @Test
    @DisplayName("should allow TENANT_ADMIN to approve any ticket in tenant")
    void approve_tenantAdmin_allowsApproval() {
        ApprovalTicket ticket = pendingFastTicket();
        when(approvalTicketMapper.selectOne(any())).thenReturn(ticket);

        ApprovalTicket result = approvalTicketService.approve(ticket.getTicketId(), "TENANT_ADMIN:admin-1", "Approved");

        assertThat(result.getStage()).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("should throw when ticket not found")
    void approve_notFound_throwsNotFound() {
        when(approvalTicketMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> approvalTicketService.approve(UUID.randomUUID(), "APPROVER:user-1", "ok"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("should throw when ticket is already approved")
    void approve_alreadyApproved_throwsParamError() {
        ApprovalTicket ticket = pendingFastTicket();
        ticket.setStage("APPROVED");
        when(approvalTicketMapper.selectOne(any())).thenReturn(ticket);

        assertThatThrownBy(() -> approvalTicketService.approve(ticket.getTicketId(), "APPROVER:user-1", "ok"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    @DisplayName("should throw when approverId is blank")
    void approve_blankApprover_throwsParamError() {
        ApprovalTicket ticket = pendingFastTicket();
        when(approvalTicketMapper.selectOne(any())).thenReturn(ticket);

        assertThatThrownBy(() -> approvalTicketService.approve(ticket.getTicketId(), "", "ok"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    // ------------------------------------------------------------------
    // reject
    // ------------------------------------------------------------------

    @Test
    @DisplayName("should reject pending ticket and publish decision event")
    void reject_pendingTicket_updatesToRejectedAndPublishesEvent() {
        ApprovalTicket ticket = pendingFastTicket();
        when(approvalTicketMapper.selectOne(any())).thenReturn(ticket);

        ApprovalTicket result = approvalTicketService.reject(ticket.getTicketId(), "APPROVER:user-1", "Too risky");

        assertThat(result.getStage()).isEqualTo("REJECTED");
        assertThat(result.getDecisionVersion()).isEqualTo(1);
        verify(approvalTicketMapper).updateById(result);
        verify(rabbitTemplate).convertAndSend(eq("approval"), eq("approval.decisions"), any(String.class));
    }

    @Test
    @DisplayName("should throw when rejecting already rejected ticket")
    void reject_alreadyRejected_throwsParamError() {
        ApprovalTicket ticket = pendingFastTicket();
        ticket.setStage("REJECTED");
        when(approvalTicketMapper.selectOne(any())).thenReturn(ticket);

        assertThatThrownBy(() -> approvalTicketService.reject(ticket.getTicketId(), "APPROVER:user-1", "nope"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    // ------------------------------------------------------------------
    // escalate
    // ------------------------------------------------------------------

    @Test
    @DisplayName("should escalate FAST ticket to BPMN via escalation service")
    void escalate_fastTicket_delegatesToEscalationService() {
        ApprovalTicket ticket = pendingFastTicket();
        when(approvalTicketMapper.selectOne(any())).thenReturn(ticket);
        when(escalationService.escalateFastToBpmn(any(), any())).thenReturn(true);

        ApprovalTicket escalated = pendingBpmnTicket();
        escalated.setTicketId(ticket.getTicketId());
        when(approvalTicketMapper.selectOne(any())).thenReturn(ticket).thenReturn(escalated);

        ApprovalTicket result = approvalTicketService.escalate(ticket.getTicketId(), "APPROVER:user-1");

        verify(escalationService).escalateFastToBpmn(eq(ticket.getTicketId()), any());
        assertThat(result.getStage()).isEqualTo("PENDING_BPMN");
    }

    @Test
    @DisplayName("should escalate BPMN ticket within workflow")
    void escalate_bpmnTicket_signalsWorkflow() {
        ApprovalTicket ticket = pendingBpmnTicket();
        when(approvalTicketMapper.selectOne(any())).thenReturn(ticket);

        ApprovalTicket result = approvalTicketService.escalate(ticket.getTicketId(), "APPROVER:user-1");

        verify(escalationService).escalateWithinBpmn(eq(ticket.getTicketId()), eq("APPROVER:user-1"), any());
        verify(approvalTicketMapper).updateById(ticket);
    }

    @Test
    @DisplayName("should throw when FAST escalation fails")
    void escalate_fastEscalationFails_throwsError() {
        ApprovalTicket ticket = pendingFastTicket();
        when(approvalTicketMapper.selectOne(any())).thenReturn(ticket);
        when(escalationService.escalateFastToBpmn(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> approvalTicketService.escalate(ticket.getTicketId(), "APPROVER:user-1"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.ERROR.getCode());
    }

    @Test
    @DisplayName("should throw when escalating non-pending ticket")
    void escalate_nonPending_throwsParamError() {
        ApprovalTicket ticket = pendingFastTicket();
        ticket.setStage("APPROVED");
        when(approvalTicketMapper.selectOne(any())).thenReturn(ticket);

        assertThatThrownBy(() -> approvalTicketService.escalate(ticket.getTicketId(), "APPROVER:user-1"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    // ------------------------------------------------------------------
    // getTicket
    // ------------------------------------------------------------------

    @Test
    @DisplayName("should return ticket by UUID")
    void getTicket_found_returnsTicket() {
        ApprovalTicket ticket = pendingFastTicket();
        when(approvalTicketMapper.selectOne(any())).thenReturn(ticket);

        ApprovalTicket result = approvalTicketService.getTicket(ticket.getTicketId());

        assertThat(result).isEqualTo(ticket);
    }

    @Test
    @DisplayName("should return null when ticket not found")
    void getTicket_notFound_returnsNull() {
        when(approvalTicketMapper.selectOne(any())).thenReturn(null);

        ApprovalTicket result = approvalTicketService.getTicket(UUID.randomUUID());

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should return null when ticketId is null")
    void getTicket_nullId_returnsNull() {
        ApprovalTicket result = approvalTicketService.getTicket(null);

        assertThat(result).isNull();
    }

    // ------------------------------------------------------------------
    // listPendingByTenant
    // ------------------------------------------------------------------

    @Test
    @DisplayName("should return pending tickets for tenant")
    void listPendingByTenant_returnsPendingTickets() {
        ApprovalTicket t1 = pendingFastTicket();
        ApprovalTicket t2 = pendingBpmnTicket();
        when(approvalTicketMapper.selectList(any())).thenReturn(List.of(t1, t2));

        List<ApprovalTicket> result = approvalTicketService.listPendingByTenant(10L);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("should return empty list when tenantId is null")
    void listPendingByTenant_nullTenant_returnsEmpty() {
        List<ApprovalTicket> result = approvalTicketService.listPendingByTenant(null);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should return empty list when no pending tickets")
    void listPendingByTenant_noPending_returnsEmpty() {
        when(approvalTicketMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<ApprovalTicket> result = approvalTicketService.listPendingByTenant(10L);

        assertThat(result).isEmpty();
    }

    // ------------------------------------------------------------------
    // RBAC validation
    // ------------------------------------------------------------------

    @Test
    @DisplayName("should allow APPROVER for tenant-owned tickets")
    void rbac_approverOwnTenant_allowed() {
        ApprovalTicket ticket = pendingFastTicket();
        ticket.setTenantId(10L);
        when(approvalTicketMapper.selectOne(any())).thenReturn(ticket);

        ApprovalTicket result = approvalTicketService.approve(ticket.getTicketId(), "APPROVER:user-1", "ok");

        assertThat(result.getStage()).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("should deny APPROVER when ticket has no tenant")
    void rbac_approverNoTenant_forbidden() {
        ApprovalTicket ticket = pendingFastTicket();
        ticket.setTenantId(null);
        when(approvalTicketMapper.selectOne(any())).thenReturn(ticket);

        assertThatThrownBy(() -> approvalTicketService.approve(ticket.getTicketId(), "APPROVER:user-1", "ok"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.FORBIDDEN.getCode());
    }

    @Test
    @DisplayName("should allow raw userId without role prefix when ticket has tenant")
    void rbac_noRolePrefixWithTenant_allowed() {
        ApprovalTicket ticket = pendingFastTicket();
        ticket.setTenantId(10L);
        when(approvalTicketMapper.selectOne(any())).thenReturn(ticket);

        ApprovalTicket result = approvalTicketService.approve(ticket.getTicketId(), "user-1", "ok");

        assertThat(result.getStage()).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("should deny raw userId without role prefix when ticket has no tenant")
    void rbac_noRolePrefixNoTenant_forbidden() {
        ApprovalTicket ticket = pendingFastTicket();
        ticket.setTenantId(null);
        when(approvalTicketMapper.selectOne(any())).thenReturn(ticket);

        assertThatThrownBy(() -> approvalTicketService.approve(ticket.getTicketId(), "user-1", "ok"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.FORBIDDEN.getCode());
    }

    @Test
    @DisplayName("should deny approver with unsupported role prefix")
    void rbac_unknownRolePrefix_forbidden() {
        ApprovalTicket ticket = pendingFastTicket();
        ticket.setTenantId(10L);
        when(approvalTicketMapper.selectOne(any())).thenReturn(ticket);

        assertThatThrownBy(() -> approvalTicketService.approve(ticket.getTicketId(), "AUDITOR:user-1", "ok"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.FORBIDDEN.getCode());

        verify(approvalTicketMapper, never()).updateById(any());
        verifyNoInteractions(rabbitTemplate);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private ApprovalRequestEvent fastEvent() {
        return new ApprovalRequestEvent(
                UUID.randomUUID(), 1L, 10L, 2L, 3, "FAST", "HIGH", "Delete production DB", 5, Instant.now());
    }

    private ApprovalTicket pendingFastTicket() {
        ApprovalTicket ticket = new ApprovalTicket();
        ticket.setId(1L);
        ticket.setTicketId(UUID.randomUUID());
        ticket.setExecutionId(1L);
        ticket.setTenantId(10L);
        ticket.setAgentId(2L);
        ticket.setStage("PENDING_FAST");
        ticket.setHandler("FAST");
        ticket.setRiskLevel("HIGH");
        ticket.setDecisionVersion(0);
        ticket.setExpectedExecutionVersion(5);
        ticket.setCreatedAt(Instant.now());
        ticket.setUpdatedAt(Instant.now());
        return ticket;
    }

    private ApprovalTicket pendingBpmnTicket() {
        ApprovalTicket ticket = new ApprovalTicket();
        ticket.setId(2L);
        ticket.setTicketId(UUID.randomUUID());
        ticket.setExecutionId(2L);
        ticket.setTenantId(10L);
        ticket.setAgentId(3L);
        ticket.setStage("PENDING_BPMN");
        ticket.setHandler("BPMN");
        ticket.setRiskLevel("MEDIUM");
        ticket.setDecisionVersion(0);
        ticket.setCreatedAt(Instant.now());
        ticket.setUpdatedAt(Instant.now());
        return ticket;
    }
}
