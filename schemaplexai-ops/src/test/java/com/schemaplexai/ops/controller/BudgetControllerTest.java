package com.schemaplexai.ops.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.ops.entity.SfBudget;
import com.schemaplexai.ops.service.BudgetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetControllerTest {

    @Mock
    private BudgetService budgetService;

    @InjectMocks
    private BudgetController budgetController;

    private SfBudget budget;

    @BeforeEach
    void setUp() {
        budget = new SfBudget();
        budget.setId(1L);
        budget.setBudgetType("monthly");
        budget.setLimitAmount(BigDecimal.valueOf(1000));
        budget.setUsedAmount(BigDecimal.valueOf(250));
        budget.setAlertThreshold(new BigDecimal("0.8"));
    }

    @Test
    void create_returnsId() {
        when(budgetService.save(any())).thenReturn(true);

        Result<Long> result = budgetController.create(budget);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(1L);
    }

    @Test
    void update_returnsBoolean() {
        when(budgetService.updateById(any())).thenReturn(true);

        Result<Boolean> result = budgetController.update(1L, budget);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void delete_returnsBoolean() {
        when(budgetService.removeById(1L)).thenReturn(true);

        Result<Boolean> result = budgetController.delete(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void get_found() {
        when(budgetService.getById(1L)).thenReturn(budget);

        Result<SfBudget> result = budgetController.get(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getBudgetType()).isEqualTo("monthly");
    }

    @Test
    void get_notFound() {
        when(budgetService.getById(1L)).thenReturn(null);

        Result<SfBudget> result = budgetController.get(1L);

        assertThat(result.getCode()).isEqualTo(404);
    }

    @Test
    void list_returnsBudgets() {
        when(budgetService.list()).thenReturn(List.of(budget));

        Result<List<SfBudget>> result = budgetController.list();

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).hasSize(1);
    }

    @Test
    void allocateBudget_returnsBudget() {
        when(budgetService.allocateBudget(any())).thenReturn(budget);

        Result<SfBudget> result = budgetController.allocateBudget(budget);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getLimitAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000));
    }

    @Test
    void checkBudgetLimit_returnsTrue() {
        when(budgetService.checkBudgetLimit(1L)).thenReturn(true);

        Result<Boolean> result = budgetController.checkBudgetLimit(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void checkBudgetLimit_returnsFalse() {
        when(budgetService.checkBudgetLimit(1L)).thenReturn(false);

        Result<Boolean> result = budgetController.checkBudgetLimit(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isFalse();
    }

    @Test
    void getBudgetUsage_returnsDecimal() {
        when(budgetService.getBudgetUsage(1L)).thenReturn(new BigDecimal("0.25"));

        Result<BigDecimal> result = budgetController.getBudgetUsage(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualByComparingTo(new BigDecimal("0.25"));
    }

    @Test
    void listBudgetsByTenant_returnsBudgets() {
        when(budgetService.listBudgetsByTenant("tenant1")).thenReturn(List.of(budget));

        Result<List<SfBudget>> result = budgetController.listBudgetsByTenant("tenant1");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).hasSize(1);
    }

    @Test
    void updateBudgetAllocation_returnsBudget() {
        BigDecimal newLimit = BigDecimal.valueOf(2000);
        when(budgetService.updateBudgetAllocation(1L, newLimit)).thenReturn(budget);

        Result<SfBudget> result = budgetController.updateBudgetAllocation(1L, newLimit);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getId()).isEqualTo(1L);
    }
}
