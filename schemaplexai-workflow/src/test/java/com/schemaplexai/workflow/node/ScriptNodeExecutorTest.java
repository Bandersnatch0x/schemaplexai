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
    void execute_returnsPlaceholderResult() {
        NodeExecutionResult result = executor.execute(Map.of("script", "print('hello')"), "tenant-1");
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).containsEntry("result", "script executed");
    }
}
