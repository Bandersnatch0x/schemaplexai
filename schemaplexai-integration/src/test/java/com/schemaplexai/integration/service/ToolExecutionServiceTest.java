package com.schemaplexai.integration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.integration.tool.ToolExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ToolExecutionServiceTest {

    @Mock
    private ToolExecutor localExecutor;

    @InjectMocks
    private ToolExecutionService toolExecutionService;

    @BeforeEach
    void setUp() {
        when(localExecutor.getToolName()).thenReturn("local");
        ReflectionTestUtils.setField(toolExecutionService, "executors", List.of(localExecutor));
        ReflectionTestUtils.setField(toolExecutionService, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.invokeMethod(toolExecutionService, "init");
    }

    @Test
    void executeTool_success() {
        when(localExecutor.execute(Map.of("action", "echo", "message", "hi"))).thenReturn("hi");

        String result = toolExecutionService.executeTool("local", "{\"action\":\"echo\",\"message\":\"hi\"}");
        assertThat(result).isEqualTo("hi");
    }

    @Test
    void executeTool_toolNotFound_throwsIntegrationNotFound() {
        assertThatThrownBy(() -> toolExecutionService.executeTool("unknown", "{}"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.INTEGRATION_NOT_FOUND.getCode());
    }

    @Test
    void executeTool_nullParametersJson_usesEmptyMap() {
        when(localExecutor.execute(Map.of())).thenReturn("empty");

        String result = toolExecutionService.executeTool("local", null);
        assertThat(result).isEqualTo("empty");
    }

    @Test
    void executeTool_blankParametersJson_usesEmptyMap() {
        when(localExecutor.execute(Map.of())).thenReturn("empty");

        String result = toolExecutionService.executeTool("local", "   ");
        assertThat(result).isEqualTo("empty");
    }

    @Test
    void executeTool_invalidJson_usesRawString() {
        when(localExecutor.execute(Map.of("raw", "not-json"))).thenReturn("raw-result");

        String result = toolExecutionService.executeTool("local", "not-json");
        assertThat(result).isEqualTo("raw-result");
    }

    @Test
    void executeTool_executorThrowsBaseException_propagates() {
        when(localExecutor.execute(Map.of("action", "echo", "message", "hi")))
                .thenThrow(new BaseException(ResultCode.TOOL_EXECUTION_FAILED, "fail"));

        assertThatThrownBy(() -> toolExecutionService.executeTool("local", "{\"action\":\"echo\",\"message\":\"hi\"}"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.TOOL_EXECUTION_FAILED.getCode());
    }

    @Test
    void executeTool_executorThrowsRuntimeException_wrapsInBaseException() {
        when(localExecutor.execute(Map.of("action", "echo", "message", "hi")))
                .thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> toolExecutionService.executeTool("local", "{\"action\":\"echo\",\"message\":\"hi\"}"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.TOOL_EXECUTION_FAILED.getCode());
    }
}
