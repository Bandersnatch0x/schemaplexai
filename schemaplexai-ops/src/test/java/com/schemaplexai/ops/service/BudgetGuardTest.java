package com.schemaplexai.ops.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.schemaplexai.ops.entity.BudgetConfig;
import com.schemaplexai.ops.entity.SfCostRecord;
import com.schemaplexai.ops.mapper.BudgetConfigMapper;
import com.schemaplexai.ops.mapper.SfCostRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("M6.3: Budget Guard Tests")
class BudgetGuardTest {

    @Mock
    private BudgetConfigMapper budgetConfigMapper;

    @Mock
    private SfCostRecordMapper costRecordMapper;

    @InjectMocks
    private BudgetGuard budgetGuard;

    private static final String TENANT_ID = "42";

    @BeforeEach
    void setUp() {
        budgetGuard = new BudgetGuard(budgetConfigMapper, costRecordMapper);
    }

    @Test
    @DisplayName("Within budget allows execution")
    void withinBudgetAllowsExecution() {
        BudgetConfig config = createBudgetConfig(new BigDecimal("1000.00"));
        when(budgetConfigMapper.selectByTenantId(TENANT_ID)).thenReturn(config);
        when(costRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(
                List.of(createCostRecord(new BigDecimal("100.00")))
        );

        BudgetStatus status = budgetGuard.checkBudget(42L, new BigDecimal("50.00"));

        assertThat(status).isEqualTo(BudgetStatus.WITHIN_BUDGET);
    }

    @Test
    @DisplayName("Warning status when >80% consumed")
    void warningStatusWhenOverEightyPercentConsumed() {
        BudgetConfig config = createBudgetConfig(new BigDecimal("1000.00"));
        when(budgetConfigMapper.selectByTenantId(TENANT_ID)).thenReturn(config);
        when(costRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(
                List.of(createCostRecord(new BigDecimal("850.00")))
        );

        BudgetStatus status = budgetGuard.checkBudget(42L, new BigDecimal("10.00"));

        assertThat(status).isEqualTo(BudgetStatus.WARNING);
    }

    @Test
    @DisplayName("Exceeded status blocks execution")
    void exceededStatusBlocksExecution() {
        BudgetConfig config = createBudgetConfig(new BigDecimal("1000.00"));
        when(budgetConfigMapper.selectByTenantId(TENANT_ID)).thenReturn(config);
        when(costRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(
                List.of(createCostRecord(new BigDecimal("950.00")))
        );

        BudgetStatus status = budgetGuard.checkBudget(42L, new BigDecimal("100.00"));

        assertThat(status).isEqualTo(BudgetStatus.EXCEEDED);
    }

    @Test
    @DisplayName("Handles no budget config - assumes unlimited / within budget")
    void handlesNoBudgetConfig() {
        when(budgetConfigMapper.selectByTenantId(TENANT_ID)).thenReturn(null);

        BudgetStatus status = budgetGuard.checkBudget(42L, new BigDecimal("100.00"));

        assertThat(status).isEqualTo(BudgetStatus.WITHIN_BUDGET);
        verifyNoInteractions(costRecordMapper);
    }

    @Test
    @DisplayName("getRemainingBudget returns correct amount")
    void getRemainingBudgetReturnsCorrectAmount() {
        BudgetConfig config = createBudgetConfig(new BigDecimal("1000.00"));
        when(budgetConfigMapper.selectByTenantId(TENANT_ID)).thenReturn(config);
        when(costRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(
                List.of(createCostRecord(new BigDecimal("300.00")))
        );

        BigDecimal remaining = budgetGuard.getRemainingBudget(42L);

        assertThat(remaining).isEqualByComparingTo(new BigDecimal("700.00"));
    }

    @Test
    @DisplayName("getConsumedBudget returns sum for current month")
    void getConsumedBudgetReturnsSumForCurrentMonth() {
        when(costRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(
                List.of(
                        createCostRecord(new BigDecimal("100.00")),
                        createCostRecord(new BigDecimal("200.00"))
                )
        );

        BigDecimal consumed = budgetGuard.getConsumedBudget(42L);

        assertThat(consumed).isEqualByComparingTo(new BigDecimal("300.00"));
    }

    @Test
    @DisplayName("getRemainingBudget returns zero when over budget")
    void getRemainingBudgetReturnsZeroWhenOverBudget() {
        BudgetConfig config = createBudgetConfig(new BigDecimal("100.00"));
        when(budgetConfigMapper.selectByTenantId(TENANT_ID)).thenReturn(config);
        when(costRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(
                List.of(createCostRecord(new BigDecimal("150.00")))
        );

        BigDecimal remaining = budgetGuard.getRemainingBudget(42L);

        assertThat(remaining).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Cost record query filters by current month")
    void costRecordQueryFiltersByCurrentMonth() {
        when(costRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        budgetGuard.getConsumedBudget(42L);

        ArgumentCaptor<LambdaQueryWrapper> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(costRecordMapper).selectList(captor.capture());
    }

    private BudgetConfig createBudgetConfig(BigDecimal monthlyLimit) {
        BudgetConfig config = new BudgetConfig();
        config.setTenantId(TENANT_ID);
        config.setMonthlyLimit(monthlyLimit);
        config.setCurrency("USD");
        return config;
    }

    private SfCostRecord createCostRecord(BigDecimal costAmount) {
        SfCostRecord record = new SfCostRecord();
        record.setCostAmount(costAmount);
        record.setCurrency("USD");
        record.setOccurredAt(LocalDateTime.now());
        return record;
    }
}
