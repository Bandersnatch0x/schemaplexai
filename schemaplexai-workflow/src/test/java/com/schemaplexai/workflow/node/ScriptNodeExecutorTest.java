package com.schemaplexai.workflow.node;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ScriptNodeExecutorTest {

    private final ScriptNodeExecutor executor = new ScriptNodeExecutor();

    @Test
    void getNodeType_returnsScript() {
        assertThat(executor.getNodeType()).isEqualTo("SCRIPT");
    }

    @Test
    void execute_missingScript_returnsFailure() {
        NodeExecutionResult result = executor.execute(Map.of("language", "groovy"), "tenant-1");
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("script");
    }

    @Test
    void execute_missingLanguage_returnsFailure() {
        NodeExecutionResult result = executor.execute(Map.of("script", "return 1"), "tenant-1");
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("language");
    }

    @Test
    void execute_unsupportedLanguage_returnsFailure() {
        NodeExecutionResult result = executor.execute(
                Map.of("script", "return 1", "language", "python"), "tenant-1");
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("Unsupported script language");
    }

    @Test
    void execute_groovy_returnsComputedResult() {
        // The node input map is exposed to the script as `input`.
        NodeExecutionResult result = executor.execute(
                Map.of("script", "input.a + input.b", "language", "groovy", "a", 1, "b", 2),
                "tenant-1");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput().get("language")).isEqualTo("groovy");
        assertThat(result.getOutput().get("result")).isEqualTo(3);
    }

    @Test
    void execute_groovy_seesTenantId() {
        NodeExecutionResult result = executor.execute(
                Map.of("script", "tenantId", "language", "groovy"), "tenant-42");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput().get("result")).isEqualTo("tenant-42");
    }

    @Test
    void execute_javascript_returnsComputedResult() {
        NodeExecutionResult result = executor.execute(
                Map.of("script", "input.a * input.b", "language", "javascript", "a", 3, "b", 4),
                "tenant-1");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput().get("language")).isEqualTo("javascript");
        assertThat(((Number) result.getOutput().get("result")).intValue()).isEqualTo(12);
    }

    @Test
    void execute_scriptThrows_returnsFailure() {
        NodeExecutionResult result = executor.execute(
                Map.of("script", "throw new IllegalStateException('boom')", "language", "groovy"),
                "tenant-1");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.isTimeout()).isFalse();
        assertThat(result.getMessage()).contains("boom");
    }

    @Test
    void execute_infiniteLoop_timesOut() {
        NodeExecutionResult result = executor.execute(
                Map.of("script", "while (true) { }", "language", "groovy", "timeoutSeconds", 1),
                "tenant-1");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.isTimeout()).isTrue();
        assertThat(result.getMessage()).contains("timed out");
    }

    @Test
    void execute_invalidTimeoutSeconds_fallsBackToDefault() {
        NodeExecutionResult result = executor.execute(
                Map.of("script", "1 + 1", "language", "groovy", "timeoutSeconds", -5),
                "tenant-1");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput().get("result")).isEqualTo(2);
    }
}
