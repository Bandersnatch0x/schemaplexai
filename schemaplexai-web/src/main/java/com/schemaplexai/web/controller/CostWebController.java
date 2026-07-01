package com.schemaplexai.web.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.web.service.cost.CostQueryPort;
import com.schemaplexai.web.vo.CostSummaryVO;
import com.schemaplexai.web.vo.ExecutionCostVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * M6.4: Cost Web Controller.
 * Provides HTTP API for cost analytics: tenant summary and per-execution cost.
 */
@RestController
@RequestMapping("/web/costs")
@Tag(name = "Cost Analytics", description = "Cost analytics API")
@RequiredArgsConstructor
public class CostWebController extends BaseController {

    private final CostQueryPort costQueryPort;

    @Operation(summary = "Tenant cost summary")
    @GetMapping("/summary")
    public Result<CostSummaryVO> getCostSummary(
            @RequestHeader("X-Tenant-Id") String tenantId) {
        return success(costQueryPort.getCostSummary(tenantId));
    }

    @Operation(summary = "Execution cost detail")
    @GetMapping("/executions/{executionId}")
    public Result<ExecutionCostVO> getExecutionCost(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable Long executionId) {
        return success(costQueryPort.getExecutionCost(tenantId, executionId));
    }
}
