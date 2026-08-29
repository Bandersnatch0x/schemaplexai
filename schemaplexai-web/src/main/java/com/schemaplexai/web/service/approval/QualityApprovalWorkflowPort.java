package com.schemaplexai.web.service.approval;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.quality.entity.ApprovalTicket;
import com.schemaplexai.quality.service.ApprovalTicketService;
import com.schemaplexai.web.mapper.ApprovalMapper;
import com.schemaplexai.web.vo.ApprovalVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QualityApprovalWorkflowPort implements ApprovalWorkflowPort {

    private final ObjectProvider<ApprovalTicketService> approvalTicketServiceProvider;
    private final ApprovalMapper approvalMapper;

    @Override
    public List<ApprovalVO> listPendingApprovals(String tenantId) {
        Long parsedTenantId = parseTenantId(tenantId);
        return approvalTicketService().listPendingByTenant(parsedTenantId).stream()
                .map(approvalMapper::toApprovalVO)
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
        ApprovalTicketService service = approvalTicketServiceProvider.getIfAvailable();
        if (service == null) {
            throw new BaseException(ResultCode.ERROR,
                    "Approval workflow service is not available in web runtime");
        }
        return service;
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
}
