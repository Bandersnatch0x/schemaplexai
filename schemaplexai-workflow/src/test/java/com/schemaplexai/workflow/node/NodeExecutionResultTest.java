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

    @Test
    void failure_isNotRetryableAndNotTimeout() {
        NodeExecutionResult result = NodeExecutionResult.failure("bad config");

        assertThat(result.isRetryable()).isFalse();
        assertThat(result.isTimeout()).isFalse();
    }

    @Test
    void retryableFailure_marksRetryable() {
        NodeExecutionResult result = NodeExecutionResult.retryableFailure("connection reset");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.isRetryable()).isTrue();
        assertThat(result.isTimeout()).isFalse();
        assertThat(result.getOutput()).isEmpty();
    }

    @Test
    void timeout_marksTimeoutAndNotRetryable() {
        NodeExecutionResult result = NodeExecutionResult.timeout("exceeded 300s");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.isTimeout()).isTrue();
        assertThat(result.isRetryable()).isFalse();
        assertThat(result.getMessage()).isEqualTo("exceeded 300s");
        assertThat(result.getOutput()).isEmpty();
    }
}
