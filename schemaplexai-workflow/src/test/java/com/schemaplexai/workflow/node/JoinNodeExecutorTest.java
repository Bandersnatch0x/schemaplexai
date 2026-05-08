package com.schemaplexai.workflow.node;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class JoinNodeExecutorTest {

    @Autowired
    private JoinNodeExecutor executor;

    @Test
    void returnsCorrectNodeType() {
        assertThat(executor.getNodeType()).isEqualTo("JOIN");
    }

    @Test
    void concatStrategy_mergesAllOutputs() {
        Map<String, Object> sourceResults = Map.of(
                "branch1", Map.of("success", true, "output", "Unit tests generated"),
                "branch2", Map.of("success", true, "output", "Integration tests generated")
        );

        NodeExecutionResult result = executor.execute(
                Map.of("sourceResults", sourceResults, "mergeStrategy", "CONCAT"),
                "tenant-1");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).containsEntry("strategy", "CONCAT");
        assertThat(result.getOutput()).containsEntry("sourceCount", 2);
        assertThat(result.getOutput()).containsEntry("successCount", 2);
        String merged = (String) result.getOutput().get("mergedContent");
        assertThat(merged).contains("[branch1]");
        assertThat(merged).contains("[branch2]");
        assertThat(merged).contains("Unit tests generated");
        assertThat(merged).contains("Integration tests generated");
    }

    @Test
    void firstSuccess_strategy_picksFirstValidResult() {
        Map<String, Object> sourceResults = Map.of(
                "slow", Map.of("success", true, "output", "Slow result"),
                "fast", Map.of("success", true, "output", "Fast result")
        );

        NodeExecutionResult result = executor.execute(
                Map.of("sourceResults", sourceResults, "mergeStrategy", "FIRST_SUCCESS"),
                "tenant-1");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).containsEntry("strategy", "FIRST_SUCCESS");
        assertThat(result.getOutput()).containsEntry("successCount", 1);
        String merged = (String) result.getOutput().get("mergedContent");
        assertThat(merged).isNotNull();
        assertThat(merged).isNotEmpty();
    }

    @Test
    void firstSuccess_noValidResult_returnsEmptyContent() {
        Map<String, Object> sourceResults = Map.of(
                "branch1", Map.of("success", false, "error", "failed")
        );

        NodeExecutionResult result = executor.execute(
                Map.of("sourceResults", sourceResults, "mergeStrategy", "FIRST_SUCCESS"),
                "tenant-1");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).containsEntry("successCount", 0);
        assertThat(result.getOutput()).containsEntry("mergedContent", "");
    }

    @Test
    void aggregateStrategy_returnsStructuredResults() {
        Map<String, Object> sourceResults = Map.of(
                "unitTest", Map.of("success", true, "output", "5 tests written"),
                "review", Map.of("success", true, "output", "Code looks good")
        );

        NodeExecutionResult result = executor.execute(
                Map.of("sourceResults", sourceResults, "mergeStrategy", "AGGREGATE"),
                "tenant-1");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).containsEntry("strategy", "AGGREGATE");
        assertThat(result.getOutput()).containsEntry("sourceCount", 2);
        assertThat(result.getOutput()).containsEntry("successCount", 2);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sources = (List<Map<String, Object>>) result.getOutput().get("sources");
        assertThat(sources).hasSize(2);
    }

    @Test
    void aggregateStrategy_partialFailure() {
        Map<String, Object> sourceResults = Map.of(
                "success", Map.of("success", true, "output", "OK"),
                "failed", Map.of("success", false, "error", "Timeout")
        );

        NodeExecutionResult result = executor.execute(
                Map.of("sourceResults", sourceResults, "mergeStrategy", "AGGREGATE"),
                "tenant-1");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).containsEntry("successCount", 1);
        assertThat(result.getOutput()).containsEntry("sourceCount", 2);
    }

    @Test
    void missingSourceResults_returnsFailure() {
        NodeExecutionResult result = executor.execute(Map.of(), "tenant-1");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("sourceResults");
    }

    @Test
    void emptySourceResults_returnsFailure() {
        NodeExecutionResult result = executor.execute(
                Map.of("sourceResults", Map.of()), "tenant-1");

        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void listFormatSourceResults_accepted() {
        NodeExecutionResult result = executor.execute(
                Map.of(
                        "sourceResults", List.of(
                                Map.of("success", true, "output", "First"),
                                Map.of("success", true, "output", "Second")
                        ),
                        "mergeStrategy", "CONCAT"
                ),
                "tenant-1");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).containsEntry("sourceCount", 2);
        assertThat(result.getOutput()).containsEntry("successCount", 2);
    }

    @Test
    void defaultStrategy_isAggregate() {
        Map<String, Object> sourceResults = Map.of(
                "a", Map.of("success", true, "output", "data")
        );

        NodeExecutionResult result = executor.execute(
                Map.of("sourceResults", sourceResults), "tenant-1");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).containsEntry("strategy", "AGGREGATE");
    }

    @Test
    void unknownStrategy_fallsBackToAggregate() {
        Map<String, Object> sourceResults = Map.of(
                "a", Map.of("success", true, "output", "data")
        );

        NodeExecutionResult result = executor.execute(
                Map.of("sourceResults", sourceResults, "mergeStrategy", "UNKNOWN"),
                "tenant-1");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).containsEntry("strategy", "AGGREGATE");
    }

    @Test
    void stringSourceValue_isExtractedDirectly() {
        Map<String, Object> sourceResults = Map.of(
                "plain", "Plain text output"
        );

        NodeExecutionResult result = executor.execute(
                Map.of("sourceResults", sourceResults, "mergeStrategy", "CONCAT"),
                "tenant-1");

        assertThat(result.isSuccess()).isTrue();
        String merged = (String) result.getOutput().get("mergedContent");
        assertThat(merged).contains("Plain text output");
    }
}
