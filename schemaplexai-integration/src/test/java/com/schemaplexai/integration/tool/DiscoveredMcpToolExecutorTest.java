package com.schemaplexai.integration.tool;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.integration.service.McpServerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DiscoveredMcpToolExecutor} (issue 930): the executor
 * published by discovery under {@code mcp:<serverId>:<toolName>} must route
 * execution through the server configuration lookup (id → endpoint row), and
 * must turn degraded "Error:" answers into structured failures.
 */
@ExtendWith(MockitoExtension.class)
class DiscoveredMcpToolExecutorTest {

    @Mock
    private McpServerService mcpServerService;

    @Test
    void getToolName_returnsQualifiedName() {
        DiscoveredMcpToolExecutor executor =
                new DiscoveredMcpToolExecutor(1L, "read_file", mcpServerService);

        assertThat(executor.getToolName()).isEqualTo("mcp:1:read_file");
        assertThat(executor.getServerId()).isEqualTo(1L);
        assertThat(executor.getRemoteToolName()).isEqualTo("read_file");
    }

    @Test
    void execute_delegatesToServerServiceWithDiscoveredToolName() {
        DiscoveredMcpToolExecutor executor =
                new DiscoveredMcpToolExecutor(7L, "calculator", mcpServerService);
        when(mcpServerService.invokeTool(eq(7L), eq("calculator"), anyMap()))
                .thenReturn("{\"content\":[{\"type\":\"text\",\"text\":\"42\"}]}");

        String result = executor.execute(Map.of("expression", "6*7"));

        assertThat(result).contains("42");
        verify(mcpServerService).invokeTool(7L, "calculator", Map.of("expression", "6*7"));
    }

    @Test
    void execute_nullParameters_passesEmptyArguments() {
        DiscoveredMcpToolExecutor executor =
                new DiscoveredMcpToolExecutor(7L, "status", mcpServerService);
        when(mcpServerService.invokeTool(eq(7L), eq("status"), eq(Map.of())))
                .thenReturn("ok");

        assertThat(executor.execute(null)).isEqualTo("ok");
    }

    @Test
    void execute_degradedErrorString_throwsStructuredFailure() {
        DiscoveredMcpToolExecutor executor =
                new DiscoveredMcpToolExecutor(9L, "flaky", mcpServerService);
        when(mcpServerService.invokeTool(eq(9L), eq("flaky"), anyMap()))
                .thenReturn("Error: MCP endpoint http://dead:9000 is unreachable");

        assertThatThrownBy(() -> executor.execute(Map.of()))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> {
                    BaseException be = (BaseException) ex;
                    assertThat(be.getCode()).isEqualTo(ResultCode.TOOL_EXECUTION_FAILED.getCode());
                    assertThat(be.getMessage()).contains("unreachable");
                });
    }

    @Test
    void constructor_rejectsNulls() {
        assertThatThrownBy(() -> new DiscoveredMcpToolExecutor(null, "t", mcpServerService))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new DiscoveredMcpToolExecutor(1L, null, mcpServerService))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new DiscoveredMcpToolExecutor(1L, "t", null))
                .isInstanceOf(NullPointerException.class);
    }
}
