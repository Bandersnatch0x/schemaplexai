package com.schemaplexai.ops.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.schemaplexai.ops.entity.SfBudget;

import java.math.BigDecimal;
import java.util.List;

public interface BudgetService extends IService<SfBudget> {

    /**
     * Allocate a new budget.
     *
     * @param budget the budget to allocate
     * @return the allocated budget
     */
    SfBudget allocateBudget(SfBudget budget);

    /**
     * Check if a budget has exceeded its limit.
     *
     * @param budgetId the budget ID
     * @return true if the budget limit is exceeded
     */
    boolean checkBudgetLimit(Long budgetId);

    /**
     * Get budget usage details.
     *
     * @param budgetId the budget ID
     * @return budget usage as a decimal (0.0 to 1.0+)
     */
    BigDecimal getBudgetUsage(Long budgetId);

    /**
     * List budgets by tenant.
     *
     * @param tenantId the tenant ID
     * @return list of budgets
     */
    List<SfBudget> listBudgetsByTenant(String tenantId);

    /**
     * Update budget allocation amount.
     *
     * @param budgetId      the budget ID
     * @param newLimitAmount the new limit amount
     * @return the updated budget
     */
    SfBudget updateBudgetAllocation(Long budgetId, BigDecimal newLimitAmount);
}
