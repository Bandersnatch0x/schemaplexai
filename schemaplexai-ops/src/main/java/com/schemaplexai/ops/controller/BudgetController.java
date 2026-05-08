package com.schemaplexai.ops.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.ops.entity.SfBudget;
import com.schemaplexai.ops.service.BudgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/ops/budgets")
@RequiredArgsConstructor
@Tag(name = "预算管理", description = "预算分配、限额检查与使用统计接口")
public class BudgetController {

    private final BudgetService budgetService;

    @PostMapping
    @Operation(summary = "创建预算")
    public Result<Long> create(@RequestBody SfBudget budget) {
        budgetService.save(budget);
        return Result.success(budget.getId());
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新预算")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfBudget budget) {
        budget.setId(id);
        return Result.success(budgetService.updateById(budget));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除预算")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(budgetService.removeById(id));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取预算")
    public Result<SfBudget> get(@PathVariable Long id) {
        SfBudget budget = budgetService.getById(id);
        if (budget == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(budget);
    }

    @GetMapping
    @Operation(summary = "列出所有预算")
    public Result<List<SfBudget>> list() {
        return Result.success(budgetService.list());
    }

    @PostMapping("/allocate")
    @Operation(summary = "分配预算")
    public Result<SfBudget> allocateBudget(@RequestBody SfBudget budget) {
        return Result.success(budgetService.allocateBudget(budget));
    }

    @GetMapping("/{id}/check-limit")
    @Operation(summary = "检查预算限额")
    public Result<Boolean> checkBudgetLimit(@PathVariable Long id) {
        return Result.success(budgetService.checkBudgetLimit(id));
    }

    @GetMapping("/{id}/usage")
    @Operation(summary = "获取预算使用量")
    public Result<BigDecimal> getBudgetUsage(@PathVariable Long id) {
        return Result.success(budgetService.getBudgetUsage(id));
    }

    @GetMapping("/by-tenant")
    @Operation(summary = "根据租户列出预算")
    public Result<List<SfBudget>> listBudgetsByTenant(@RequestParam String tenantId) {
        return Result.success(budgetService.listBudgetsByTenant(tenantId));
    }

    @PostMapping("/{id}/update-allocation")
    @Operation(summary = "更新预算分配")
    public Result<SfBudget> updateBudgetAllocation(@PathVariable Long id, @RequestParam BigDecimal newLimitAmount) {
        return Result.success(budgetService.updateBudgetAllocation(id, newLimitAmount));
    }
}
