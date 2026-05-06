package com.schemaplexai.ops.service;

import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.ops.entity.SfBudget;
import com.schemaplexai.ops.mapper.BudgetMapper;
import com.schemaplexai.ops.service.BudgetServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceImplTest {

    @Mock
    private BudgetMapper budgetMapper;

    @InjectMocks
    private BudgetServiceImpl budgetService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(budgetService, "baseMapper", budgetMapper);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    // ------------------------------------------------------------------
    // allocateBudget
    // ------------------------------------------------------------------

    @Test
    void allocateBudget_nullType_throwsParamError() {
        SfBudget budget = new SfBudget();
        budget.setLimitAmount(BigDecimal.valueOf(100));

        assertThatThrownBy(() -> budgetService.allocateBudget(budget))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void allocateBudget_blankType_throwsParamError() {
        SfBudget budget = new SfBudget();
        budget.setBudgetType("   ");
        budget.setLimitAmount(BigDecimal.valueOf(100));

        assertThatThrownBy(() -> budgetService.allocateBudget(budget))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void allocateBudget_nullLimit_throwsParamError() {
        SfBudget budget = new SfBudget();
        budget.setBudgetType("API");

        assertThatThrownBy(() -> budgetService.allocateBudget(budget))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void allocateBudget_zeroLimit_throwsParamError() {
        SfBudget budget = new SfBudget();
        budget.setBudgetType("API");
        budget.setLimitAmount(BigDecimal.ZERO);

        assertThatThrownBy(() -> budgetService.allocateBudget(budget))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void allocateBudget_negativeLimit_throwsParamError() {
        SfBudget budget = new SfBudget();
        budget.setBudgetType("API");
        budget.setLimitAmount(BigDecimal.valueOf(-10));

        assertThatThrownBy(() -> budgetService.allocateBudget(budget))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void allocateBudget_success_withDefaults() {
        SfBudget budget = new SfBudget();
        budget.setBudgetType("API");
        budget.setLimitAmount(BigDecimal.valueOf(100));

        SfBudget result = budgetService.allocateBudget(budget);

        assertThat(result.getUsedAmount()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.getAlertThreshold()).isEqualTo(new BigDecimal("0.8"));
        verify(budgetMapper).insert(budget);
    }

    @Test
    void allocateBudget_preservesProvidedValues() {
        SfBudget budget = new SfBudget();
        budget.setBudgetType("API");
        budget.setLimitAmount(BigDecimal.valueOf(100));
        budget.setUsedAmount(BigDecimal.valueOf(10));
        budget.setAlertThreshold(new BigDecimal("0.9"));

        SfBudget result = budgetService.allocateBudget(budget);

        assertThat(result.getUsedAmount()).isEqualTo(BigDecimal.valueOf(10));
        assertThat(result.getAlertThreshold()).isEqualTo(new BigDecimal("0.9"));
    }

    // ------------------------------------------------------------------
    // checkBudgetLimit
    // ------------------------------------------------------------------

    @Test
    void checkBudgetLimit_notFound_throwsNotFound() {
        when(budgetMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> budgetService.checkBudgetLimit(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void checkBudgetLimit_nullLimitAmount_returnsFalse() {
        SfBudget budget = new SfBudget();
        budget.setId(1L);
        budget.setLimitAmount(null);
        budget.setUsedAmount(BigDecimal.valueOf(50));
        when(budgetMapper.selectById(1L)).thenReturn(budget);

        boolean result = budgetService.checkBudgetLimit(1L);

        assertThat(result).isFalse();
    }

    @Test
    void checkBudgetLimit_nullUsedAmount_returnsFalse() {
        SfBudget budget = new SfBudget();
        budget.setId(1L);
        budget.setLimitAmount(BigDecimal.valueOf(100));
        budget.setUsedAmount(null);
        when(budgetMapper.selectById(1L)).thenReturn(budget);

        boolean result = budgetService.checkBudgetLimit(1L);

        assertThat(result).isFalse();
    }

    @Test
    void checkBudgetLimit_underLimit_returnsFalse() {
        SfBudget budget = new SfBudget();
        budget.setId(1L);
        budget.setLimitAmount(BigDecimal.valueOf(100));
        budget.setUsedAmount(BigDecimal.valueOf(50));
        when(budgetMapper.selectById(1L)).thenReturn(budget);

        boolean result = budgetService.checkBudgetLimit(1L);

        assertThat(result).isFalse();
    }

    @Test
    void checkBudgetLimit_exactlyAtLimit_returnsFalse() {
        SfBudget budget = new SfBudget();
        budget.setId(1L);
        budget.setLimitAmount(BigDecimal.valueOf(100));
        budget.setUsedAmount(BigDecimal.valueOf(100));
        when(budgetMapper.selectById(1L)).thenReturn(budget);

        boolean result = budgetService.checkBudgetLimit(1L);

        assertThat(result).isFalse();
    }

    @Test
    void checkBudgetLimit_overLimit_returnsTrue() {
        SfBudget budget = new SfBudget();
        budget.setId(1L);
        budget.setLimitAmount(BigDecimal.valueOf(100));
        budget.setUsedAmount(BigDecimal.valueOf(101));
        when(budgetMapper.selectById(1L)).thenReturn(budget);

        boolean result = budgetService.checkBudgetLimit(1L);

        assertThat(result).isTrue();
    }

    // ------------------------------------------------------------------
    // getBudgetUsage
    // ------------------------------------------------------------------

    @Test
    void getBudgetUsage_notFound_throwsNotFound() {
        when(budgetMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> budgetService.getBudgetUsage(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void getBudgetUsage_nullLimit_returnsZero() {
        SfBudget budget = new SfBudget();
        budget.setId(1L);
        budget.setLimitAmount(null);
        when(budgetMapper.selectById(1L)).thenReturn(budget);

        BigDecimal result = budgetService.getBudgetUsage(1L);

        assertThat(result).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void getBudgetUsage_zeroLimit_returnsZero() {
        SfBudget budget = new SfBudget();
        budget.setId(1L);
        budget.setLimitAmount(BigDecimal.ZERO);
        when(budgetMapper.selectById(1L)).thenReturn(budget);

        BigDecimal result = budgetService.getBudgetUsage(1L);

        assertThat(result).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void getBudgetUsage_nullUsed_returnsZero() {
        SfBudget budget = new SfBudget();
        budget.setId(1L);
        budget.setLimitAmount(BigDecimal.valueOf(100));
        budget.setUsedAmount(null);
        when(budgetMapper.selectById(1L)).thenReturn(budget);

        BigDecimal result = budgetService.getBudgetUsage(1L);

        assertThat(result).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void getBudgetUsage_halfUsed_returnsPointFive() {
        SfBudget budget = new SfBudget();
        budget.setId(1L);
        budget.setLimitAmount(BigDecimal.valueOf(100));
        budget.setUsedAmount(BigDecimal.valueOf(50));
        when(budgetMapper.selectById(1L)).thenReturn(budget);

        BigDecimal result = budgetService.getBudgetUsage(1L);

        assertThat(result).isEqualTo(new BigDecimal("0.5000"));
    }

    @Test
    void getBudgetUsage_fullUsed_returnsOne() {
        SfBudget budget = new SfBudget();
        budget.setId(1L);
        budget.setLimitAmount(BigDecimal.valueOf(100));
        budget.setUsedAmount(BigDecimal.valueOf(100));
        when(budgetMapper.selectById(1L)).thenReturn(budget);

        BigDecimal result = budgetService.getBudgetUsage(1L);

        assertThat(result).isEqualTo(new BigDecimal("1.0000"));
    }

    // ------------------------------------------------------------------
    // listBudgetsByTenant
    // ------------------------------------------------------------------

    @Test
    void listBudgetsByTenant_nullTenantId_throwsParamError() {
        assertThatThrownBy(() -> budgetService.listBudgetsByTenant(null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void listBudgetsByTenant_blankTenantId_throwsParamError() {
        assertThatThrownBy(() -> budgetService.listBudgetsByTenant("   "))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void listBudgetsByTenant_returnsBudgets() {
        SfBudget b1 = new SfBudget();
        b1.setBudgetType("API");
        when(budgetMapper.selectList(any())).thenReturn(List.of(b1));

        List<SfBudget> result = budgetService.listBudgetsByTenant("tenant-1");

        assertThat(result).hasSize(1);
    }

    @Test
    void listBudgetsByTenant_noBudgets_returnsEmpty() {
        when(budgetMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<SfBudget> result = budgetService.listBudgetsByTenant("tenant-1");

        assertThat(result).isEmpty();
    }

    // ------------------------------------------------------------------
    // updateBudgetAllocation
    // ------------------------------------------------------------------

    @Test
    void updateBudgetAllocation_notFound_throwsNotFound() {
        when(budgetMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> budgetService.updateBudgetAllocation(1L, BigDecimal.valueOf(200)))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void updateBudgetAllocation_nullNewLimit_throwsParamError() {
        assertThatThrownBy(() -> budgetService.updateBudgetAllocation(1L, null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void updateBudgetAllocation_zeroNewLimit_throwsParamError() {
        assertThatThrownBy(() -> budgetService.updateBudgetAllocation(1L, BigDecimal.ZERO))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void updateBudgetAllocation_negativeNewLimit_throwsParamError() {
        assertThatThrownBy(() -> budgetService.updateBudgetAllocation(1L, BigDecimal.valueOf(-10)))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void updateBudgetAllocation_success_updatesLimit() {
        SfBudget budget = new SfBudget();
        budget.setId(1L);
        budget.setLimitAmount(BigDecimal.valueOf(100));
        when(budgetMapper.selectById(1L)).thenReturn(budget);

        SfBudget result = budgetService.updateBudgetAllocation(1L, BigDecimal.valueOf(200));

        assertThat(result.getLimitAmount()).isEqualTo(BigDecimal.valueOf(200));
        verify(budgetMapper).updateById(budget);
    }
}
