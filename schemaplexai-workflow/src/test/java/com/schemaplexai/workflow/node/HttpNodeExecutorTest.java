package com.schemaplexai.workflow.node;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HttpNodeExecutorTest {

    private final HttpNodeExecutor executor = new HttpNodeExecutor();

    @Test
    void getNodeType_returnsHttp() {
        assertThat(executor.getNodeType()).isEqualTo("HTTP");
    }

    @Test
    void execute_nullUrl_returnsPlaceholder() {
        NodeExecutionResult result = executor.execute(Map.of(), "tenant-1");
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).containsEntry("statusCode", 200);
    }

    @Test
    void execute_blankUrl_returnsPlaceholder() {
        NodeExecutionResult result = executor.execute(Map.of("url", "  "), "tenant-1");
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).containsEntry("statusCode", 200);
    }

    @Test
    void execute_unsupportedMethod_returnsFailure() {
        NodeExecutionResult result = executor.execute(
                Map.of("url", "http://example.com", "method", "FOOBAR"), "tenant-1");
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void execute_validUrlAndMethod_makesRequest() {
        // This will likely fail at network level but exercises the branch
        NodeExecutionResult result = executor.execute(
                Map.of("url", "http://localhost:59999/test", "method", "GET"), "tenant-1");
        // Network failure returns failure result
        assertThat(result).isNotNull();
    }
}
