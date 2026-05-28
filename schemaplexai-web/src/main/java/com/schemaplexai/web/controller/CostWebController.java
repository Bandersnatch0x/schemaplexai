package com.schemaplexai.web.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.web.service.cost.CostQueryPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

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
    public Result<Map<String, Object>> getCostSummary(
            @RequestHeader("X-Tenant-Id") String tenantId) {
        return success(costQueryPort.getCostSummary(tenantId));
    }

    @Operation(summary = "Execution cost detail")
    @GetMapping("/executions/{executionId}")
    public Result<Map<String, Object>> getExecutionCost(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable Long executionId) {
        return success(costQueryPort.getExecutionCost(tenantId, executionId));
    }
}
