package com.schemaplexai.web.service.approval;

import com.schemaplexai.web.vo.ApprovalVO;

import java.util.List;

public interface ApprovalWorkflowPort {

    List<ApprovalVO> listPendingApprovals(String tenantId);

    void approve(String ticketId, String approverId, String reason);

    void reject(String ticketId, String approverId, String reason);

    void escalate(String ticketId, String escalatorId);
}
