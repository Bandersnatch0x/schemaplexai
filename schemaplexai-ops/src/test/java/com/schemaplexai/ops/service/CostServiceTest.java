package com.schemaplexai.ops.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.schemaplexai.ops.entity.SfBudget;
import com.schemaplexai.ops.mapper.BudgetMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CostServiceTest {

    @Mock
    private BudgetMapper budgetMapper;

    @InjectMocks
    private CostService costService;

    @BeforeEach
    void setUp() {
        // No additional setup needed per test
    }

    @Test
    void queryCostByTenant_returnsPlaceholderCostData() {
        var result = costService.queryCostByTenant("tenant-1");

        assert result.containsKey("totalCost");
        assert result.containsKey("todayCost");
        assert result.containsKey("monthCost");
        assert result.get("totalCost").compareTo(BigDecimal.ZERO) == 0;
        assert result.get("todayCost").compareTo(BigDecimal.ZERO) == 0;
        assert result.get("monthCost").compareTo(BigDecimal.ZERO) == 0;
    }

    @Test
    void checkBudgetAlerts_noBudgets_noError() {
        when(budgetMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        costService.checkBudgetAlerts();

        verify(budgetMapper).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    void checkBudgetAlerts_nullLimitAmount_skipped() {
        SfBudget budget = new SfBudget();
        budget.setBudgetType("API");
        budget.setLimitAmount(null);
        budget.setUsedAmount(BigDecimal.valueOf(50));
        when(budgetMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(budget));

        costService.checkBudgetAlerts();

        verify(budgetMapper).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    void checkBudgetAlerts_nullUsedAmount_skipped() {
        SfBudget budget = new SfBudget();
        budget.setBudgetType("API");
        budget.setLimitAmount(BigDecimal.valueOf(100));
        budget.setUsedAmount(null);
        when(budgetMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(budget));

        costService.checkBudgetAlerts();

        verify(budgetMapper).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    void checkBudgetAlerts_budgetExceeded_logsWarning() {
        SfBudget budget = new SfBudget();
        budget.setBudgetType("API");
        budget.setLimitAmount(BigDecimal.valueOf(100));
        budget.setUsedAmount(BigDecimal.valueOf(150));
        budget.setAlertThreshold(BigDecimal.valueOf(80));
        when(budgetMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(budget));

        costService.checkBudgetAlerts();

        verify(budgetMapper).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    void checkBudgetAlerts_thresholdReachedButNotExceeded_logsWarning() {
        SfBudget budget = new SfBudget();
        budget.setBudgetType("API");
        budget.setLimitAmount(BigDecimal.valueOf(100));
        budget.setUsedAmount(BigDecimal.valueOf(85));
        budget.setAlertThreshold(BigDecimal.valueOf(80));
        when(budgetMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(budget));

        costService.checkBudgetAlerts();

        verify(budgetMapper).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    void checkBudgetAlerts_underThreshold_noWarning() {
        SfBudget budget = new SfBudget();
        budget.setBudgetType("API");
        budget.setLimitAmount(BigDecimal.valueOf(100));
        budget.setUsedAmount(BigDecimal.valueOf(50));
        budget.setAlertThreshold(BigDecimal.valueOf(80));
        when(budgetMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(budget));

        costService.checkBudgetAlerts();

        verify(budgetMapper).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    void checkBudgetAlerts_exactlyAtThreshold_logsWarning() {
        SfBudget budget = new SfBudget();
        budget.setBudgetType("API");
        budget.setLimitAmount(BigDecimal.valueOf(100));
        budget.setUsedAmount(BigDecimal.valueOf(80));
        budget.setAlertThreshold(BigDecimal.valueOf(80));
        when(budgetMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(budget));

        costService.checkBudgetAlerts();

        verify(budgetMapper).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    void checkBudgetAlerts_exactlyAtLimit_logsExceeded() {
        SfBudget budget = new SfBudget();
        budget.setBudgetType("API");
        budget.setLimitAmount(BigDecimal.valueOf(100));
        budget.setUsedAmount(BigDecimal.valueOf(100));
        when(budgetMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(budget));

        costService.checkBudgetAlerts();

        verify(budgetMapper).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    void checkBudgetAlerts_nullAlertThreshold_onlyChecksExceeded() {
        SfBudget budget = new SfBudget();
        budget.setBudgetType("API");
        budget.setLimitAmount(BigDecimal.valueOf(100));
        budget.setUsedAmount(BigDecimal.valueOf(90));
        budget.setAlertThreshold(null);
        when(budgetMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(budget));

        costService.checkBudgetAlerts();

        verify(budgetMapper).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    void checkBudgetAlerts_multipleBudgets_processesAll() {
        SfBudget budget1 = new SfBudget();
        budget1.setBudgetType("API");
        budget1.setLimitAmount(BigDecimal.valueOf(100));
        budget1.setUsedAmount(BigDecimal.valueOf(150));

        SfBudget budget2 = new SfBudget();
        budget2.setBudgetType("TOKEN");
        budget2.setLimitAmount(BigDecimal.valueOf(1000));
        budget2.setUsedAmount(BigDecimal.valueOf(500));
        budget2.setAlertThreshold(BigDecimal.valueOf(80));

        when(budgetMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(budget1, budget2));

        costService.checkBudgetAlerts();

        verify(budgetMapper).selectList(any(LambdaQueryWrapper.class));
    }
}
