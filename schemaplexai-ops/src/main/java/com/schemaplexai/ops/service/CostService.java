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
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class CostService {

    private final BudgetMapper budgetMapper;

    public Map<String, BigDecimal> queryCostByTenant(String tenantId) {
        log.info("Query cost for tenant: {}", tenantId);

        // PG short-path v1: aggregate from sf_budget.used_amount as real-time cost proxy
        // TODO(v2): replace with ClickHouse sf_cost_record for per-transaction accuracy
        LambdaQueryWrapper<SfBudget> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SfBudget::getTenantId, tenantId);
        List<SfBudget> budgets = budgetMapper.selectList(wrapper);

        BigDecimal totalCost = budgets.stream()
                .map(SfBudget::getUsedAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> result = new HashMap<>();
        result.put("totalCost", totalCost);
        // v1 short-path: todayCost and monthCost mirror totalCost until
        // time-series cost records are available in PostgreSQL
        result.put("todayCost", totalCost);
        result.put("monthCost", totalCost);
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
