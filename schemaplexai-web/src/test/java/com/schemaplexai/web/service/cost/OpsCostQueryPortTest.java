package com.schemaplexai.web.service.cost;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.ops.service.CostService;
import com.schemaplexai.web.mapper.CostMapper;
import com.schemaplexai.web.vo.CostSummaryVO;
import com.schemaplexai.web.vo.ExecutionCostVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Cost query web port")
class OpsCostQueryPortTest {

    @Mock
    private ObjectProvider<CostService> costServiceProvider;

    @Mock
    private CostService costService;

    @Mock
    private CostMapper costMapper;

    @InjectMocks
    private OpsCostQueryPort port;

    @Test
    @DisplayName("summary delegates to ops cost service and maps via CostMapper")
    void getCostSummary_delegatesToOpsCostService() {
        Map<String, BigDecimal> raw = new LinkedHashMap<>();
        raw.put("totalCost", BigDecimal.valueOf(42.25));
        raw.put("todayCost", BigDecimal.valueOf(5.50));
        raw.put("monthCost", BigDecimal.valueOf(42.25));

        CostSummaryVO expected = new CostSummaryVO();
        expected.setTotalCost(BigDecimal.valueOf(42.25));
        expected.setTodayCost(BigDecimal.valueOf(5.50));
        expected.setMonthCost(BigDecimal.valueOf(42.25));
        expected.setCurrency("USD");

        when(costServiceProvider.getIfAvailable()).thenReturn(costService);
        when(costService.queryCostByTenant("tenant-1")).thenReturn(raw);
        when(costMapper.toCostSummaryVO(org.mockito.ArgumentMatchers.anyMap())).thenReturn(expected);

        CostSummaryVO summary = port.getCostSummary("tenant-1");

        assertThat(summary.getTotalCost()).isEqualByComparingTo(BigDecimal.valueOf(42.25));
        assertThat(summary.getTodayCost()).isEqualByComparingTo(BigDecimal.valueOf(5.50));
        assertThat(summary.getMonthCost()).isEqualByComparingTo(BigDecimal.valueOf(42.25));
        assertThat(summary.getCurrency()).isEqualTo("USD");
        verify(costService).queryCostByTenant("tenant-1");
    }

    @Test
    @DisplayName("execution cost delegates to ops cost service and maps via CostMapper")
    void getExecutionCost_delegatesToOpsCostService() {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("executionId", 42L);
        raw.put("totalCost", BigDecimal.valueOf(15.75));
        raw.put("currency", "USD");
        raw.put("inputTokens", 100L);
        raw.put("outputTokens", 50L);
        raw.put("totalTokens", 150L);
        raw.put("recordCount", 3);

        ExecutionCostVO expected = new ExecutionCostVO();
        expected.setExecutionId(42L);
        expected.setTotalCost(BigDecimal.valueOf(15.75));
        expected.setCurrency("USD");
        expected.setInputTokens(100L);
        expected.setOutputTokens(50L);
        expected.setTotalTokens(150L);
        expected.setRecordCount(3);

        when(costServiceProvider.getIfAvailable()).thenReturn(costService);
        when(costService.queryCostByExecution("tenant-1", 42L)).thenReturn(raw);
        when(costMapper.toExecutionCostVO(raw)).thenReturn(expected);

        ExecutionCostVO result = port.getExecutionCost("tenant-1", 42L);

        assertThat(result.getExecutionId()).isEqualTo(42L);
        assertThat(result.getTotalCost()).isEqualByComparingTo(BigDecimal.valueOf(15.75));
        assertThat(result.getInputTokens()).isEqualTo(100L);
        verify(costService).queryCostByExecution("tenant-1", 42L);
    }

    @Test
    @DisplayName("missing ops cost service fails explicitly")
    void missingOpsCostService_throwsExplicitException() {
        when(costServiceProvider.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> port.getCostSummary("tenant-1"))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("Cost query service is not available");
    }

    @Test
    @DisplayName("blank tenant id fails with parameter error")
    void blankTenantId_throwsParamError() {
        assertThatThrownBy(() -> port.getCostSummary(" "))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(400);
    }

    @Test
    @DisplayName("invalid execution id fails with parameter error")
    void invalidExecutionId_throwsParamError() {
        assertThatThrownBy(() -> port.getExecutionCost("tenant-1", 0L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(400);
    }
}
