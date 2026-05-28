package com.schemaplexai.web.service.approval;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.quality.entity.ApprovalTicket;
import com.schemaplexai.quality.service.ApprovalTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QualityApprovalWorkflowPort implements ApprovalWorkflowPort {

    private final ObjectProvider<ApprovalTicketService> approvalTicketServiceProvider;

    @Override
    public List<Map<String, Object>> listPendingApprovals(String tenantId) {
        Long parsedTenantId = parseTenantId(tenantId);
        return approvalTicketService().listPendingByTenant(parsedTenantId).stream()
                .map(this::toApprovalMap)
                .toList();
    }

    @Override
    public void approve(String ticketId, String approverId, String reason) {
        UUID parsedTicketId = parseTicketId(ticketId);
        approvalTicketService().approve(parsedTicketId, approverId, reason);
    }

    @Override
    public void reject(String ticketId, String approverId, String reason) {
        UUID parsedTicketId = parseTicketId(ticketId);
        approvalTicketService().reject(parsedTicketId, approverId, reason);
    }

    @Override
    public void escalate(String ticketId, String escalatorId) {
        UUID parsedTicketId = parseTicketId(ticketId);
        approvalTicketService().escalate(parsedTicketId, escalatorId);
    }

    private ApprovalTicketService approvalTicketService() {
        ApprovalTicketService approvalTicketService = approvalTicketServiceProvider.getIfAvailable();
        if (approvalTicketService == null) {
            throw new BaseException(ResultCode.ERROR,
                    "Approval workflow service is not available in web runtime");
        }
        return approvalTicketService;
    }

    private Long parseTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "tenantId must not be blank");
        }
        try {
            return Long.valueOf(tenantId);
        } catch (NumberFormatException ex) {
            throw new BaseException(ResultCode.PARAM_ERROR, "tenantId must be a number", ex);
        }
    }

    private UUID parseTicketId(String ticketId) {
        if (ticketId == null || ticketId.isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "ticketId must not be blank");
        }
        try {
            return UUID.fromString(ticketId);
        } catch (IllegalArgumentException ex) {
            throw new BaseException(ResultCode.PARAM_ERROR, "ticketId must be a valid UUID", ex);
        }
    }

    private Map<String, Object> toApprovalMap(ApprovalTicket ticket) {
        Map<String, Object> approval = new LinkedHashMap<>();
        approval.put("ticketId", ticket.getTicketId() != null ? ticket.getTicketId().toString() : null);
        approval.put("executionId", ticket.getExecutionId());
        approval.put("tenantId", ticket.getTenantId());
        approval.put("agentId", ticket.getAgentId());
        approval.put("stage", ticket.getStage());
        approval.put("handler", ticket.getHandler());
        approval.put("riskLevel", ticket.getRiskLevel());
        approval.put("actionDescription", ticket.getActionDescription());
        approval.put("triggeringSeq", ticket.getTriggeringSeq());
        approval.put("createdAt", ticket.getCreatedAt());
        approval.put("updatedAt", ticket.getUpdatedAt());
        return approval;
    }
}
