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
        if (budget.getAlertThreshold() == null) {
            budget.setAlertThreshold(new BigDecimal("0.8"));
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
}
