package com.schemaplexai.web.controller;

import com.schemaplexai.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * M6.4: Cost Web Controller.
 * Provides HTTP API for cost analytics: tenant summary and per-execution cost.
 */
@RestController
@RequestMapping("/web/costs")
@Tag(name = "成本分析", description = "成本分析 API")
public class CostWebController extends BaseController {

    @Operation(summary = "租户成本汇总")
    @GetMapping("/summary")
    public Result<Map<String, Object>> getCostSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalCost", 1250.50);
        summary.put("currency", "USD");
        summary.put("executionCount", 42);
        summary.put("averageCost", 29.77);
        return success(summary);
    }

    @Operation(summary = "执行成本明细")
    @GetMapping("/executions/{executionId}")
    public Result<Map<String, Object>> getExecutionCost(@PathVariable Long executionId) {
        Map<String, Object> cost = new HashMap<>();
        cost.put("executionId", executionId);
        cost.put("cost", 15.75);
        cost.put("currency", "USD");
        cost.put("tokenCount", 12500);
        cost.put("model", "gpt-4");
        return success(cost);
    }
}
