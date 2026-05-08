package com.schemaplexai.workflow.node;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ConcurrentNodeExecutorTest {

    @Autowired
    private ConcurrentNodeExecutor executor;

    @Test
    void returnsCorrectNodeType() {
        assertThat(executor.getNodeType()).isEqualTo("CONCURRENT");
    }

    @Test
    void singleSubTask_returnsSuccess() {
        Map<String, Object> input = Map.of(
                "subTasks", List.of(
                        Map.of("name", "task1", "prompt", "Generate unit tests for the UserService class")
                )
        );

        NodeExecutionResult result = executor.execute(input, "tenant-1");

        assertThat(result.isSuccess()).isTrue();
        @SuppressWarnings("unchecked")
        Map<String, Object> results = (Map<String, Object>) result.getOutput().get("results");
        assertThat(results).containsKey("task1");
        assertThat(result.getOutput()).containsEntry("executedCount", 1);
        assertThat(result.getOutput()).containsEntry("successCount", 1);
        assertThat(result.getOutput()).containsEntry("failedCount", 0);
        assertThat(result.getOutput()).containsEntry("allSucceeded", true);
    }

    @Test
    void multipleSubTasks_allExecuteConcurrently() {
        Map<String, Object> input = Map.of(
                "subTasks", List.of(
                        Map.of("name", "unitTest", "prompt", "Write unit tests"),
                        Map.of("name", "integrationTest", "prompt", "Write integration tests"),
                        Map.of("name", "docs", "prompt", "Write documentation")
                )
        );

        NodeExecutionResult result = executor.execute(input, "tenant-1");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).containsEntry("executedCount", 3);
        assertThat(result.getOutput()).containsEntry("successCount", 3);
        assertThat(result.getOutput()).containsEntry("allSucceeded", true);
    }

    @Test
    void emptySubTasks_returnsFailure() {
        Map<String, Object> input = Map.of("subTasks", List.of());

        NodeExecutionResult result = executor.execute(input, "tenant-1");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("subTasks");
    }

    @Test
    void missingSubTasks_returnsFailure() {
        NodeExecutionResult result = executor.execute(Map.of(), "tenant-1");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("subTasks");
    }

    @Test
    void subTaskWithoutPrompt_returnsFailureForThatTask() {
        Map<String, Object> input = Map.of(
                "subTasks", List.of(
                        Map.of("name", "badTask", "prompt", ""),
                        Map.of("name", "goodTask", "prompt", "Write tests")
                )
        );

        NodeExecutionResult result = executor.execute(input, "tenant-1");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).containsEntry("successCount", 1);
        assertThat(result.getOutput()).containsEntry("failedCount", 1);
        assertThat(result.getOutput()).containsEntry("allSucceeded", false);
    }

    @Test
    void customTimeout_isAccepted() {
        Map<String, Object> input = Map.of(
                "subTasks", List.of(
                        Map.of("name", "task1", "prompt", "Quick test")
                ),
                "timeoutSeconds", 10
        );

        NodeExecutionResult result = executor.execute(input, "tenant-1");

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void subTaskName_defaultsToUnnamed() {
        Map<String, Object> input = Map.of(
                "subTasks", List.of(
                        Map.of("prompt", "Test unnamed task")
                )
        );

        NodeExecutionResult result = executor.execute(input, "tenant-1");

        assertThat(result.isSuccess()).isTrue();
        @SuppressWarnings("unchecked")
        Map<String, Object> results = (Map<String, Object>) result.getOutput().get("results");
        assertThat(results).containsKey("unnamed");
    }
}
