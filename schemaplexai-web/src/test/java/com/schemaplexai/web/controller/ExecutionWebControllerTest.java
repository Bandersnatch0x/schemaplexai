package com.schemaplexai.web.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.web.service.execution.EngineExecutionQueryPort;
import com.schemaplexai.web.service.execution.ExecutionLifecyclePort;
import com.schemaplexai.web.service.execution.ExecutionStatusPort;
import com.schemaplexai.web.vo.ExecutionStatusVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("M6.4: Execution Web Controller Tests")
class ExecutionWebControllerTest {

    @Mock
    private ExecutionLifecyclePort lifecyclePort;

    @Mock
    private ExecutionStatusPort statusPort;

    @Mock
    private EngineExecutionQueryPort queryPort;

    @InjectMocks
    private ExecutionWebController controller;

    @Test
    @DisplayName("GET /web/executions/{id} delegates to status query port and returns typed VO")
    void getExecutionStatus_delegatesToStatusPort() {
        ExecutionStatusVO status = new ExecutionStatusVO();
        status.setExecutionId(1L);
        status.setState("PAUSED");
        when(statusPort.getExecutionStatus(1L)).thenReturn(status);

        Result<ExecutionStatusVO> result = controller.getExecutionStatus(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getExecutionId()).isEqualTo(1L);
        assertThat(result.getData().getState()).isEqualTo("PAUSED");
        verify(statusPort).getExecutionStatus(1L);
    }

    @Test
    @DisplayName("POST /web/executions/{id}/pause delegates to lifecycle service")
    void pauseExecution_delegatesToLifecycleService() {
        Result<Void> result = controller.pauseExecution(1L);

        assertThat(result.getCode()).isEqualTo(200);
        verify(lifecyclePort).pauseExecution(1L);
    }

    @Test
    @DisplayName("POST /web/executions/{id}/resume delegates to lifecycle service")
    void resumeExecution_delegatesToLifecycleService() {
        Result<Void> result = controller.resumeExecution(1L);

        assertThat(result.getCode()).isEqualTo(200);
        verify(lifecyclePort).resumeExecution(1L);
    }

    @Test
    @DisplayName("POST /web/executions/{id}/cancel delegates to lifecycle service")
    void cancelExecution_delegatesToLifecycleService() {
        Result<Void> result = controller.cancelExecution(1L);

        assertThat(result.getCode()).isEqualTo(200);
        verify(lifecyclePort).cancelExecution(1L);
    }

    @Test
    @DisplayName("GET /web/executions delegates to query port")
    void listExecutions_delegatesToQueryPort() {
        long page = 1;
        long size = 20;

        Result<?> result = controller.listExecutions(page, size, null, null);

        assertThat(result.getCode()).isEqualTo(200);
        verify(queryPort).listExecutions(page, size, null, null);
    }
}
