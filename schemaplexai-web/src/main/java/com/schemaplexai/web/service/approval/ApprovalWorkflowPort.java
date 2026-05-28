package com.schemaplexai.web.service.approval;

import java.util.List;
import java.util.Map;

public interface ApprovalWorkflowPort {

    List<Map<String, Object>> listPendingApprovals(String tenantId);

    void approve(String ticketId, String approverId, String reason);

    void reject(String ticketId, String approverId, String reason);

    void escalate(String ticketId, String escalatorId);
}
