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
    void execute_failsExplicitlyUntilRuntimeIsImplemented() {
        NodeExecutionResult result = executor.execute(Map.of("script", "print('hello')"), "tenant-1");
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("SCRIPT node execution is not implemented");
        assertThat(result.getOutput()).isEmpty();
    }
}
