package com.schemaplexai.ops.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.ops.entity.SfBudget;
import com.schemaplexai.ops.mapper.BudgetMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Transactional(rollbackFor = Exception.class)
@Service
@RequiredArgsConstructor
public class BudgetServiceImpl extends ServiceImpl<BudgetMapper, SfBudget> implements BudgetService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    @Override
    public SfBudget allocateBudget(SfBudget budget) {
        if (budget.getBudgetType() == null || budget.getBudgetType().isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Budget type is required");
        }
        if (budget.getLimitAmount() == null || budget.getLimitAmount().compareTo(ZERO) <= 0) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Budget limit amount must be greater than zero");
        }
        if (budget.getUsedAmount() == null) {
            budget.setUsedAmount(ZERO);
        }
        // Spec §3.3: alert_threshold is a decimal fraction (0.8 = 80%).
        if (budget.getAlertThreshold() == null) {
            budget.setAlertThreshold(new BigDecimal("0.8"));
        } else if (budget.getAlertThreshold().signum() < 0) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Alert threshold must not be negative");
        } else if (budget.getAlertThreshold().compareTo(BigDecimal.ONE) > 0) {
            // Accept legacy percent input (e.g. 80) and normalize it to the decimal unit
            BigDecimal normalized = budget.getAlertThreshold()
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            if (normalized.compareTo(BigDecimal.ONE) > 0) {
                throw new BaseException(ResultCode.PARAM_ERROR,
                        "Alert threshold must be a fraction in [0,1] (e.g. 0.8 = 80%)");
            }
            budget.setAlertThreshold(normalized);
        }
        baseMapper.insert(budget);
        log.info("Allocated budget: id={}, type={}, limit={}", budget.getId(), budget.getBudgetType(), budget.getLimitAmount());
        return budget;
    }

    @Override
    public boolean checkBudgetLimit(Long budgetId) {
        SfBudget budget = baseMapper.selectById(budgetId);
        if (budget == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Budget not found: " + budgetId);
        }
        if (budget.getLimitAmount() == null || budget.getUsedAmount() == null) {
            return false;
        }
        boolean exceeded = budget.getUsedAmount().compareTo(budget.getLimitAmount()) > 0;
        if (exceeded) {
            log.warn("Budget limit exceeded: id={}, used={}, limit={}", budgetId, budget.getUsedAmount(), budget.getLimitAmount());
        }
        return exceeded;
    }

    @Override
    public BigDecimal getBudgetUsage(Long budgetId) {
        SfBudget budget = baseMapper.selectById(budgetId);
        if (budget == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Budget not found: " + budgetId);
        }
        if (budget.getLimitAmount() == null || budget.getLimitAmount().compareTo(ZERO) == 0) {
            return ZERO;
        }
        BigDecimal usage = budget.getUsedAmount() != null
                ? budget.getUsedAmount().divide(budget.getLimitAmount(), 4, RoundingMode.HALF_UP)
                : ZERO;
        log.info("Budget usage: id={}, usage={}", budgetId, usage);
        return usage;
    }

    @Override
    public List<SfBudget> listBudgetsByTenant(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Tenant ID is required");
        }
        return baseMapper.selectList(
                new LambdaQueryWrapper<SfBudget>()
                        .eq(SfBudget::getTenantId, tenantId)
                        .orderByDesc(SfBudget::getCreatedAt));
    }

    @Override
    public SfBudget updateBudgetAllocation(Long budgetId, BigDecimal newLimitAmount) {
        if (newLimitAmount == null || newLimitAmount.compareTo(ZERO) <= 0) {
            throw new BaseException(ResultCode.PARAM_ERROR, "New limit amount must be greater than zero");
        }
        SfBudget budget = baseMapper.selectById(budgetId);
        if (budget == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Budget not found: " + budgetId);
        }
        BigDecimal oldLimit = budget.getLimitAmount();
        budget.setLimitAmount(newLimitAmount);
        baseMapper.updateById(budget);
        log.info("Updated budget allocation: id={}, oldLimit={}, newLimit={}", budgetId, oldLimit, newLimitAmount);
        return budget;
    }

    @Override
    public void addUsedAmount(String tenantId, BigDecimal usedAmount) {
        if (tenantId == null || tenantId.isBlank() || usedAmount == null || usedAmount.compareTo(ZERO) <= 0) {
            return;
        }
        List<SfBudget> budgets = listBudgetsByTenant(tenantId);
        if (budgets.isEmpty()) {
            log.warn("No budget found for tenant {}, skipping cost sync", tenantId);
            return;
        }
        SfBudget budget = budgets.get(0);
        BigDecimal newUsed = budget.getUsedAmount() != null
                ? budget.getUsedAmount().add(usedAmount)
                : usedAmount;
        budget.setUsedAmount(newUsed);
        baseMapper.updateById(budget);
        log.info("Updated budget used amount: tenantId={}, added={}, newUsed={}", tenantId, usedAmount, newUsed);
    }
}
