package com.schemaplexai.ops.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.ops.service.CostService;
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
public class CostController {

    private final CostService costService;

    @GetMapping
    public Result<Map<String, BigDecimal>> queryCost(@RequestParam String tenantId) {
        return Result.success(costService.queryCostByTenant(tenantId));
    }
}
