package com.schemaplexai.web.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.web.service.approval.ApprovalWorkflowPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * M6.4: Approval Web Controller.
 * Provides HTTP API for approval workflow: list, approve, reject, escalate.
 */
@RestController
@RequestMapping("/web/approvals")
@Tag(name = "Approval Management", description = "Approval workflow API")
@RequiredArgsConstructor
public class ApprovalWebController extends BaseController {

    private final ApprovalWorkflowPort approvalWorkflowPort;

    @Operation(summary = "List pending approvals")
    @GetMapping
    public Result<List<Map<String, Object>>> listPendingApprovals(
            @RequestHeader("X-Tenant-Id") String tenantId) {
        return success(approvalWorkflowPort.listPendingApprovals(tenantId));
    }

    @Operation(summary = "Approve ticket")
    @PostMapping("/{ticketId}/approve")
    public Result<Void> approve(@PathVariable String ticketId,
                                @RequestParam String approverId,
                                @RequestParam String reason) {
        approvalWorkflowPort.approve(ticketId, approverId, reason);
        return success();
    }

    @Operation(summary = "Reject ticket")
    @PostMapping("/{ticketId}/reject")
    public Result<Void> reject(@PathVariable String ticketId,
                               @RequestParam String approverId,
                               @RequestParam String reason) {
        approvalWorkflowPort.reject(ticketId, approverId, reason);
        return success();
    }

    @Operation(summary = "Escalate ticket")
    @PostMapping("/{ticketId}/escalate")
    public Result<Void> escalate(@PathVariable String ticketId,
                                 @RequestParam String escalatorId) {
        approvalWorkflowPort.escalate(ticketId, escalatorId);
        return success();
    }
}
