package com.schemaplexai.web.controller;

import com.schemaplexai.common.result.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("M6.4: Cost Web Controller Tests")
class CostWebControllerTest {

    @InjectMocks
    private CostWebController controller;

    @Test
    @DisplayName("GET /web/costs/summary returns tenant cost summary")
    void getCostSummary_returnsSuccessResult() {
        Result<Map<String, Object>> result = controller.getCostSummary();

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().get("totalCost")).isEqualTo(1250.50);
        assertThat(result.getData().get("currency")).isEqualTo("USD");
    }

    @Test
    @DisplayName("GET /web/costs/executions/{executionId} returns execution cost")
    void getExecutionCost_returnsSuccessResult() {
        Result<Map<String, Object>> result = controller.getExecutionCost(42L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().get("executionId")).isEqualTo(42L);
        assertThat(result.getData().get("cost")).isEqualTo(15.75);
    }
}
