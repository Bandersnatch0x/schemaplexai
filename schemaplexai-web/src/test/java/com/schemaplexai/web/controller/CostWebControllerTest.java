package com.schemaplexai.web.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.web.service.cost.CostQueryPort;
import com.schemaplexai.web.vo.CostSummaryVO;
import com.schemaplexai.web.vo.ExecutionCostVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

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
    @DisplayName("GET /web/costs/summary delegates to cost query port and returns typed VO")
    void getCostSummary_delegatesToCostQueryPort() {
        CostSummaryVO summary = new CostSummaryVO();
        summary.setTotalCost(BigDecimal.valueOf(42.25));
        summary.setCurrency("USD");
        when(costQueryPort.getCostSummary("tenant-1")).thenReturn(summary);

        Result<CostSummaryVO> result = controller.getCostSummary("tenant-1");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getTotalCost()).isEqualByComparingTo(BigDecimal.valueOf(42.25));
        assertThat(result.getData().getCurrency()).isEqualTo("USD");
        verify(costQueryPort).getCostSummary("tenant-1");
    }

    @Test
    @DisplayName("GET /web/costs/executions/{executionId} delegates to cost query port and returns typed VO")
    void getExecutionCost_delegatesToCostQueryPort() {
        ExecutionCostVO executionCost = new ExecutionCostVO();
        executionCost.setExecutionId(42L);
        executionCost.setTotalCost(BigDecimal.valueOf(15.75));
        executionCost.setCurrency("USD");
        when(costQueryPort.getExecutionCost("tenant-1", 42L)).thenReturn(executionCost);

        Result<ExecutionCostVO> result = controller.getExecutionCost("tenant-1", 42L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getExecutionId()).isEqualTo(42L);
        assertThat(result.getData().getTotalCost()).isEqualByComparingTo(BigDecimal.valueOf(15.75));
        verify(costQueryPort).getExecutionCost("tenant-1", 42L);
    }
}
