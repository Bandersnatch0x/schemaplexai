package com.schemaplexai.ops.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.schemaplexai.ops.entity.SfBudget;
import com.schemaplexai.ops.mapper.BudgetMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CostService {

    private final BudgetMapper budgetMapper;

    public Map<String, BigDecimal> queryCostByTenant(String tenantId) {
        log.info("Query cost for tenant: {}", tenantId);

        Map<String, BigDecimal> result = new HashMap<>();
        // Phase 1: Return placeholder cost data
        // Phase 2: Query ClickHouse for actual cost analytics
        result.put("totalCost", BigDecimal.valueOf(0));
        result.put("todayCost", BigDecimal.valueOf(0));
        result.put("monthCost", BigDecimal.valueOf(0));
        return result;
    }

    public void checkBudgetAlerts() {
        List<SfBudget> budgets = budgetMapper.selectList(null);
        for (SfBudget budget : budgets) {
            if (budget.getLimitAmount() == null || budget.getUsedAmount() == null) {
                continue;
            }

            BigDecimal ratio = budget.getUsedAmount()
                    .divide(budget.getLimitAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            if (ratio.compareTo(BigDecimal.valueOf(100)) >= 0) {
                log.warn("Budget exceeded: type={}, used={}/{} ({}%)",
                        budget.getBudgetType(), budget.getUsedAmount(),
                        budget.getLimitAmount(), ratio);
            } else if (budget.getAlertThreshold() != null &&
                    ratio.compareTo(budget.getAlertThreshold()) >= 0) {
                log.warn("Budget alert threshold reached: type={}, used={}/{} ({}%)",
                        budget.getBudgetType(), budget.getUsedAmount(),
                        budget.getLimitAmount(), ratio);
            }
        }
    }
}
