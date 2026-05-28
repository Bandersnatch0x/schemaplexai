package com.schemaplexai.web.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.web.service.cost.CostQueryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("M6.4: Cost Web Controller Tests")
class CostWebControllerTest {

    @Mock
    private CostQueryPort costQueryPort;

    @InjectMocks
    private CostWebController controller;

    @Test
    @DisplayName("GET /web/costs/summary delegates to cost query port")
    void getCostSummary_delegatesToCostQueryPort() {
        Map<String, Object> summary = Map.of(
                "totalCost", BigDecimal.valueOf(42.25),
                "currency", "USD");
        when(costQueryPort.getCostSummary("tenant-1")).thenReturn(summary);

        Result<Map<String, Object>> result = controller.getCostSummary("tenant-1");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(summary);
        verify(costQueryPort).getCostSummary("tenant-1");
    }

    @Test
    @DisplayName("GET /web/costs/executions/{executionId} delegates to cost query port")
    void getExecutionCost_delegatesToCostQueryPort() {
        Map<String, Object> executionCost = Map.of(
                "executionId", 42L,
                "totalCost", BigDecimal.valueOf(15.75),
                "currency", "USD");
        when(costQueryPort.getExecutionCost("tenant-1", 42L)).thenReturn(executionCost);

        Result<Map<String, Object>> result = controller.getExecutionCost("tenant-1", 42L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(executionCost);
        verify(costQueryPort).getExecutionCost("tenant-1", 42L);
    }
}
