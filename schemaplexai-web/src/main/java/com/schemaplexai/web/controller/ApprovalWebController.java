package com.schemaplexai.web.controller;

import com.schemaplexai.common.controller.BaseController;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.web.dto.ApprovalRequest;
import com.schemaplexai.web.dto.EscalationRequest;
import com.schemaplexai.web.service.approval.ApprovalWorkflowPort;
import com.schemaplexai.web.vo.ApprovalVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * M6.4: Approval Web Controller.
 * Provides HTTP API for approval workflow: list, approve, reject, escalate.
 */
@RestController
@RequestMapping("/web/approvals")
@Tag(name = "Approval Management", description = "Approval workflow API")
@RequiredArgsConstructor
@Validated
public class ApprovalWebController extends BaseController {

    private final ApprovalWorkflowPort approvalWorkflowPort;

    @Operation(summary = "List pending approvals")
    @GetMapping
    public Result<List<ApprovalVO>> listPendingApprovals(
            @RequestHeader("X-Tenant-Id") String tenantId) {
        return success(approvalWorkflowPort.listPendingApprovals(tenantId));
    }

    @Operation(summary = "Approve ticket")
    @PostMapping("/{ticketId}/approve")
    public Result<Void> approve(@PathVariable String ticketId,
                                @Valid @RequestBody ApprovalRequest request) {
        approvalWorkflowPort.approve(ticketId, request.getApproverId(), request.getReason());
        return success();
    }

    @Operation(summary = "Reject ticket")
    @PostMapping("/{ticketId}/reject")
    public Result<Void> reject(@PathVariable String ticketId,
                               @Valid @RequestBody ApprovalRequest request) {
        approvalWorkflowPort.reject(ticketId, request.getApproverId(), request.getReason());
        return success();
    }

    @Operation(summary = "Escalate ticket")
    @PostMapping("/{ticketId}/escalate")
    public Result<Void> escalate(@PathVariable String ticketId,
                                 @Valid @RequestBody EscalationRequest request) {
        approvalWorkflowPort.escalate(ticketId, request.getEscalatorId());
        return success();
    }
}
