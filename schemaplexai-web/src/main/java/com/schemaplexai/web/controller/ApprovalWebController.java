package com.schemaplexai.web.controller;

import com.schemaplexai.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * M6.4: Approval Web Controller.
 * Provides HTTP API for approval workflow: list, approve, reject, escalate.
 */
@RestController
@RequestMapping("/web/approvals")
@Tag(name = "审批管理", description = "审批工作流 API")
public class ApprovalWebController extends BaseController {

    @Operation(summary = "获取待审批列表")
    @GetMapping
    public Result<List<Map<String, Object>>> listPendingApprovals() {
        List<Map<String, Object>> approvals = new ArrayList<>();
        Map<String, Object> approval = new HashMap<>();
        approval.put("ticketId", "TICKET-001");
        approval.put("type", "EXECUTION_APPROVAL");
        approval.put("requester", "user@example.com");
        approval.put("createdAt", System.currentTimeMillis());
        approvals.add(approval);
        return success(approvals);
    }

    @Operation(summary = "审批通过")
    @PostMapping("/{ticketId}/approve")
    public Result<Void> approve(@PathVariable String ticketId,
                                @RequestParam String reason) {
        // TODO: delegate to approval service
        return success();
    }

    @Operation(summary = "审批拒绝")
    @PostMapping("/{ticketId}/reject")
    public Result<Void> reject(@PathVariable String ticketId,
                               @RequestParam String reason) {
        // TODO: delegate to approval service
        return success();
    }

    @Operation(summary = "审批升级")
    @PostMapping("/{ticketId}/escalate")
    public Result<Void> escalate(@PathVariable String ticketId) {
        // TODO: delegate to approval service
        return success();
    }
}
