package com.schemaplexai.ops.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.ops.service.CostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/ops/costs")
@RequiredArgsConstructor
@Tag(name = "成本查询", description = "租户成本统计与查询接口")
public class CostController {

    private final CostService costService;

    @GetMapping
    @Operation(summary = "查询租户成本")
    public Result<Map<String, BigDecimal>> queryCost(@RequestParam String tenantId) {
        return Result.success(costService.queryCostByTenant(tenantId));
    }
}
