package com.schemaplexai.ops.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.ops.entity.SfBudget;
import com.schemaplexai.ops.service.BudgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/ops/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @PostMapping
    public Result<Long> create(@RequestBody SfBudget budget) {
        budgetService.save(budget);
        return Result.success(budget.getId());
    }

    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfBudget budget) {
        budget.setId(id);
        return Result.success(budgetService.updateById(budget));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(budgetService.removeById(id));
    }

    @GetMapping("/{id}")
    public Result<SfBudget> get(@PathVariable Long id) {
        SfBudget budget = budgetService.getById(id);
        if (budget == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(budget);
    }

    @GetMapping
    public Result<List<SfBudget>> list() {
        return Result.success(budgetService.list());
    }

    @PostMapping("/allocate")
    public Result<SfBudget> allocateBudget(@RequestBody SfBudget budget) {
        return Result.success(budgetService.allocateBudget(budget));
    }

    @GetMapping("/{id}/check-limit")
    public Result<Boolean> checkBudgetLimit(@PathVariable Long id) {
        return Result.success(budgetService.checkBudgetLimit(id));
    }

    @GetMapping("/{id}/usage")
    public Result<BigDecimal> getBudgetUsage(@PathVariable Long id) {
        return Result.success(budgetService.getBudgetUsage(id));
    }

    @GetMapping("/by-tenant")
    public Result<List<SfBudget>> listBudgetsByTenant(@RequestParam String tenantId) {
        return Result.success(budgetService.listBudgetsByTenant(tenantId));
    }

    @PostMapping("/{id}/update-allocation")
    public Result<SfBudget> updateBudgetAllocation(@PathVariable Long id, @RequestParam BigDecimal newLimitAmount) {
        return Result.success(budgetService.updateBudgetAllocation(id, newLimitAmount));
    }
}
