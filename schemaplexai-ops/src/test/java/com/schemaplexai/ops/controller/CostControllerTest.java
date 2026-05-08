package com.schemaplexai.ops.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.ops.service.CostService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CostControllerTest {

    @Mock
    private CostService costService;

    @InjectMocks
    private CostController costController;

    @Test
    void queryCost_returnsMap() {
        Map<String, BigDecimal> costMap = Map.of(
                "totalCost", BigDecimal.valueOf(100.50),
                "todayCost", BigDecimal.valueOf(10.25),
                "monthCost", BigDecimal.valueOf(75.00)
        );
        when(costService.queryCostByTenant("tenant1")).thenReturn(costMap);

        Result<Map<String, BigDecimal>> result = costController.queryCost("tenant1");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).hasSize(3);
        assertThat(result.getData().get("totalCost")).isEqualByComparingTo(BigDecimal.valueOf(100.50));
    }

    @Test
    void queryCost_returnsEmptyMap() {
        when(costService.queryCostByTenant("tenant2")).thenReturn(Map.of());

        Result<Map<String, BigDecimal>> result = costController.queryCost("tenant2");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEmpty();
    }
}
