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
@DisplayName("M6.4: Execution Web Controller Tests")
class ExecutionWebControllerTest {

    @InjectMocks
    private ExecutionWebController controller;

    @Test
    @DisplayName("GET /web/executions/{id} returns execution status")
    void getExecutionStatus_returnsSuccessResult() {
        Result<Map<String, Object>> result = controller.getExecutionStatus(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().get("executionId")).isEqualTo(1L);
        assertThat(result.getData().get("status")).isEqualTo("RUNNING");
    }

    @Test
    @DisplayName("POST /web/executions/{id}/pause returns success")
    void pauseExecution_returnsSuccessResult() {
        Result<Void> result = controller.pauseExecution(1L);

        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("POST /web/executions/{id}/resume returns success")
    void resumeExecution_returnsSuccessResult() {
        Result<Void> result = controller.resumeExecution(1L);

        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("POST /web/executions/{id}/cancel returns success")
    void cancelExecution_returnsSuccessResult() {
        Result<Void> result = controller.cancelExecution(1L);

        assertThat(result.getCode()).isEqualTo(200);
    }
}
