package com.schemaplexai.workflow.node;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NodeExecutionResultTest {

    @Test
    void success_withOutput_returnsResultWithSuccessTrue() {
        Map<String, Object> output = Map.of("statusCode", 200, "body", "OK");

        NodeExecutionResult result = NodeExecutionResult.success(output);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).isNull();
        assertThat(result.getOutput()).isEqualTo(output);
    }

    @Test
    void success_noArgs_returnsResultWithEmptyOutput() {
        NodeExecutionResult result = NodeExecutionResult.success();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).isNull();
        assertThat(result.getOutput()).isEmpty();
    }

    @Test
    void failure_returnsResultWithSuccessFalse_andMessage() {
        NodeExecutionResult result = NodeExecutionResult.failure("Connection refused");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Connection refused");
        assertThat(result.getOutput()).isEmpty();
    }

    @Test
    void allArgsConstructor_worksCorrectly() {
        Map<String, Object> output = Map.of("key", "value");
        NodeExecutionResult result = new NodeExecutionResult(true, "completed", output);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).isEqualTo("completed");
        assertThat(result.getOutput()).isEqualTo(output);
    }

    @Test
    void noArgsConstructor_createsDefaultObject() {
        NodeExecutionResult result = new NodeExecutionResult();

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isNull();
        assertThat(result.getOutput()).isNull();
    }
}
