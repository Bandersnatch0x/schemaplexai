package com.schemaplexai.web.service.cost;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.ops.service.CostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
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

    @InjectMocks
    private OpsCostQueryPort port;

    @Test
    @DisplayName("summary delegates to ops cost service and adds currency")
    void getCostSummary_delegatesToOpsCostService() {
        when(costServiceProvider.getIfAvailable()).thenReturn(costService);
        when(costService.queryCostByTenant("tenant-1")).thenReturn(Map.of(
                "totalCost", BigDecimal.valueOf(42.25),
                "todayCost", BigDecimal.valueOf(5.50),
                "monthCost", BigDecimal.valueOf(42.25)));

        Map<String, Object> summary = port.getCostSummary("tenant-1");

        assertThat(summary)
                .containsEntry("totalCost", BigDecimal.valueOf(42.25))
                .containsEntry("todayCost", BigDecimal.valueOf(5.50))
                .containsEntry("monthCost", BigDecimal.valueOf(42.25))
                .containsEntry("currency", "USD");
        verify(costService).queryCostByTenant("tenant-1");
    }

    @Test
    @DisplayName("execution cost delegates to ops cost service")
    void getExecutionCost_delegatesToOpsCostService() {
        Map<String, Object> executionCost = Map.of(
                "executionId", 42L,
                "totalCost", BigDecimal.valueOf(15.75),
                "currency", "USD");
        when(costServiceProvider.getIfAvailable()).thenReturn(costService);
        when(costService.queryCostByExecution("tenant-1", 42L)).thenReturn(executionCost);

        Map<String, Object> result = port.getExecutionCost("tenant-1", 42L);

        assertThat(result).isEqualTo(executionCost);
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
