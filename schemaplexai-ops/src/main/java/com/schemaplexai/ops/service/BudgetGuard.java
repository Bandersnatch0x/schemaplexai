package com.schemaplexai.ops.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.schemaplexai.ops.entity.BudgetConfig;
import com.schemaplexai.ops.entity.SfCostRecord;
import com.schemaplexai.ops.mapper.BudgetConfigMapper;
import com.schemaplexai.ops.mapper.SfCostRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class BudgetGuard {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal WARNING_THRESHOLD = new BigDecimal("0.8");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final BudgetConfigMapper budgetConfigMapper;
    private final SfCostRecordMapper costRecordMapper;

    /**
     * Checks if tenant has budget remaining for the projected cost.
     *
     * @param tenantId      the tenant ID
     * @param projectedCost the cost of the operation about to execute
     * @return BudgetStatus indicating whether execution should proceed
     */
    public BudgetStatus checkBudget(Long tenantId, BigDecimal projectedCost) {
        BudgetConfig config = budgetConfigMapper.selectByTenantId(String.valueOf(tenantId));
        if (config == null || config.getMonthlyLimit() == null) {
            log.debug("No budget config for tenant={}, assuming unlimited", tenantId);
            return BudgetStatus.WITHIN_BUDGET;
        }

        BigDecimal consumed = getConsumedBudget(tenantId);
        BigDecimal projectedTotal = consumed.add(projectedCost != null ? projectedCost : ZERO);

        if (projectedTotal.compareTo(config.getMonthlyLimit()) > 0) {
            log.warn("Budget exceeded for tenant={}: consumed={}, projected={}, limit={}",
                    tenantId, consumed, projectedTotal, config.getMonthlyLimit());
            return BudgetStatus.EXCEEDED;
        }

        BigDecimal usageRatio = config.getMonthlyLimit().compareTo(ZERO) > 0
                ? consumed.divide(config.getMonthlyLimit(), 4, RoundingMode.HALF_UP)
                : ZERO;

        if (usageRatio.compareTo(WARNING_THRESHOLD) >= 0) {
            log.warn("Budget warning for tenant={}: consumed={}, limit={}, ratio={}%",
                    tenantId, consumed, config.getMonthlyLimit(),
                    usageRatio.multiply(ONE_HUNDRED).setScale(2, RoundingMode.HALF_UP));
            return BudgetStatus.WARNING;
        }

        return BudgetStatus.WITHIN_BUDGET;
    }

    /**
     * Returns the remaining budget amount for the tenant in the current billing period.
     *
     * @param tenantId the tenant ID
     * @return remaining budget, or zero if over budget
     */
    public BigDecimal getRemainingBudget(Long tenantId) {
        BudgetConfig config = budgetConfigMapper.selectByTenantId(String.valueOf(tenantId));
        if (config == null || config.getMonthlyLimit() == null) {
            return null;
        }

        BigDecimal consumed = getConsumedBudget(tenantId);
        BigDecimal remaining = config.getMonthlyLimit().subtract(consumed);
        return remaining.max(ZERO);
    }

    /**
     * Returns the consumed budget amount for the current month.
     *
     * @param tenantId the tenant ID
     * @return total consumed amount in current billing period
     */
    public BigDecimal getConsumedBudget(Long tenantId) {
        String tenantIdStr = String.valueOf(tenantId);
        LocalDateTime startOfMonth = LocalDateTime.now()
                .withDayOfMonth(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        LocalDateTime endOfMonth = startOfMonth.plusMonths(1);

        List<SfCostRecord> records = costRecordMapper.selectList(
                new LambdaQueryWrapper<SfCostRecord>()
                        .eq(SfCostRecord::getTenantId, tenantIdStr)
                        .ge(SfCostRecord::getOccurredAt, startOfMonth)
                        .lt(SfCostRecord::getOccurredAt, endOfMonth)
        );

        return records.stream()
                .map(SfCostRecord::getCostAmount)
                .filter(Objects::nonNull)
                .reduce(ZERO, BigDecimal::add);
    }
}
