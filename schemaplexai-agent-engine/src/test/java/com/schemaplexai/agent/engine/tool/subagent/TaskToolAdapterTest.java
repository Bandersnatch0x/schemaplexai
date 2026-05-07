package com.schemaplexai.agent.engine.tool.subagent;

import com.schemaplexai.agent.engine.tool.ToolCall;
import com.schemaplexai.agent.engine.tool.ToolExecutionException;
import com.schemaplexai.agent.engine.tool.ToolResult;
import com.schemaplexai.agent.engine.tool.adapter.ExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskToolAdapterTest {

    @Mock
    private SubAgentExecutionService subAgentService;

    @Mock
    private SubAgentQuotaService quotaService;

    private TaskToolAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new TaskToolAdapter(subAgentService, quotaService);
    }

    @Test
    void shouldReturnTaskAsToolName() {
        assertThat(adapter.getToolName()).isEqualTo("task");
    }

    @Test
    void shouldReturnErrorWhenPromptIsMissing() throws Exception {
        ToolCall call = new ToolCall("task", Map.of());
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, "/workspace");

        ToolResult result = adapter.execute(call, ctx);

        assertThat(result.isError()).isTrue();
        assertThat(result.errorMessage()).contains("prompt");
    }

    @Test
    void shouldReturnErrorWhenPromptIsBlank() throws Exception {
        ToolCall call = new ToolCall("task", Map.of("prompt", "  "));
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, "/workspace");

        ToolResult result = adapter.execute(call, ctx);

        assertThat(result.isError()).isTrue();
        assertThat(result.errorMessage()).contains("prompt");
    }

    @Test
    void shouldReturnErrorWhenQuotaExceeded() throws Exception {
        doThrow(new SubAgentQuotaExceededException("quota exceeded"))
            .when(quotaService).checkAndIncrementForTenant("tenant1", 1L);

        ToolCall call = new ToolCall("task", Map.of("prompt", "do something"));
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, "/workspace");

        ToolResult result = adapter.execute(call, ctx);

        assertThat(result.isError()).isTrue();
        assertThat(result.errorMessage()).contains("quota exceeded");
    }

    @Test
    void shouldExecuteSubAgentSuccessfully() throws Exception {
        SubAgentResult subResult = new SubAgentResult("completed output", 42L);
        when(subAgentService.execute(any(SubAgentRequest.class))).thenReturn(subResult);

        ToolCall call = new ToolCall("task", Map.of("prompt", "do something"));
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, "/workspace");

        ToolResult result = adapter.execute(call, ctx);

        assertThat(result.success()).isTrue();
        assertThat(result.output()).isEqualTo("completed output");
    }

    @Test
    void shouldPassInheritedGuardrailsToSubAgent() throws Exception {
        SubAgentResult subResult = new SubAgentResult("done", 99L);
        when(subAgentService.execute(any(SubAgentRequest.class))).thenReturn(subResult);

        Map<String, Object> guardrails = Map.of("maxDepth", 3, "allowedTools", "file_read,task");
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, "/workspace", Map.of(), guardrails);

        ToolCall call = new ToolCall("task", Map.of("prompt", "nested task"));

        adapter.execute(call, ctx);

        verify(subAgentService).execute(argThat(req ->
            req.parentExecutionId().equals(1L) &&
            req.prompt().equals("nested task") &&
            req.inheritedGuardrails().equals(guardrails) &&
            req.maxDepth() == 2
        ));
    }

    @Test
    void shouldPassAgentIdWhenProvided() throws Exception {
        SubAgentResult subResult = new SubAgentResult("done", 100L);
        when(subAgentService.execute(any(SubAgentRequest.class))).thenReturn(subResult);

        ToolCall call = new ToolCall("task", Map.of(
            "prompt", "do something",
            "agentId", 7L
        ));
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, "/workspace");

        adapter.execute(call, ctx);

        verify(subAgentService).execute(argThat(req ->
            req.parentExecutionId().equals(1L) &&
            req.prompt().equals("do something")
        ));
    }

    @Test
    void shouldDecrementQuotaOnSubAgentFailure() throws Exception {
        when(subAgentService.execute(any(SubAgentRequest.class)))
            .thenThrow(new RuntimeException("sub-agent failed"));

        ToolCall call = new ToolCall("task", Map.of("prompt", "do something"));
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, "/workspace");

        ToolResult result = adapter.execute(call, ctx);

        assertThat(result.isError()).isTrue();
        verify(quotaService).decrement(1L);
    }

    @Test
    void shouldUseDefaultRoleWhenNotSpecified() throws Exception {
        SubAgentResult subResult = new SubAgentResult("done", 101L);
        when(subAgentService.execute(any(SubAgentRequest.class))).thenReturn(subResult);

        ToolCall call = new ToolCall("task", Map.of("prompt", "do something"));
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, "/workspace");

        adapter.execute(call, ctx);

        verify(subAgentService).execute(argThat(req ->
            "subagent".equals(req.role())
        ));
    }

    @Test
    void shouldUseCustomRoleWhenSpecified() throws Exception {
        SubAgentResult subResult = new SubAgentResult("done", 102L);
        when(subAgentService.execute(any(SubAgentRequest.class))).thenReturn(subResult);

        ToolCall call = new ToolCall("task", Map.of(
            "prompt", "do something",
            "role", "analyzer"
        ));
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, "/workspace");

        adapter.execute(call, ctx);

        verify(subAgentService).execute(argThat(req ->
            "analyzer".equals(req.role())
        ));
    }

    @Test
    void shouldStopAtMaxDepthZero() throws Exception {
        Map<String, Object> guardrails = Map.of("maxDepth", 0);
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, "/workspace", Map.of(), guardrails);

        ToolCall call = new ToolCall("task", Map.of("prompt", "do something"));

        ToolResult result = adapter.execute(call, ctx);

        assertThat(result.isError()).isTrue();
        assertThat(result.errorMessage()).contains("max depth");
        verifyNoInteractions(subAgentService);
    }
}
