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
    void singleSubTask_withoutRuntime_returnsFailure() {
        Map<String, Object> input = Map.of(
                "subTasks", List.of(
                        Map.of("name", "task1", "prompt", "Generate unit tests for the UserService class")
                )
        );

        NodeExecutionResult result = executor.execute(input, "tenant-1");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("CONCURRENT node execution is not implemented");
        assertThat(result.getOutput()).isEmpty();
    }

    @Test
    void multipleSubTasks_withoutRuntime_returnsFailure() {
        Map<String, Object> input = Map.of(
                "subTasks", List.of(
                        Map.of("name", "unitTest", "prompt", "Write unit tests"),
                        Map.of("name", "integrationTest", "prompt", "Write integration tests"),
                        Map.of("name", "docs", "prompt", "Write documentation")
                )
        );

        NodeExecutionResult result = executor.execute(input, "tenant-1");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("CONCURRENT node execution is not implemented");
        assertThat(result.getOutput()).isEmpty();
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
    void subTaskWithoutPrompt_returnsFailureBeforeRuntimeDispatch() {
        Map<String, Object> input = Map.of(
                "subTasks", List.of(
                        Map.of("name", "badTask", "prompt", ""),
                        Map.of("name", "goodTask", "prompt", "Write tests")
                )
        );

        NodeExecutionResult result = executor.execute(input, "tenant-1");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("Missing prompt in sub-task");
        assertThat(result.getOutput()).isEmpty();
    }

    @Test
    void customTimeout_withoutRuntimeStillFailsExplicitly() {
        Map<String, Object> input = Map.of(
                "subTasks", List.of(
                        Map.of("name", "task1", "prompt", "Quick test")
                ),
                "timeoutSeconds", 10
        );

        NodeExecutionResult result = executor.execute(input, "tenant-1");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("CONCURRENT node execution is not implemented");
    }

    @Test
    void subTaskNameWithoutRuntime_returnsFailure() {
        Map<String, Object> input = Map.of(
                "subTasks", List.of(
                        Map.of("prompt", "Test unnamed task")
                )
        );

        NodeExecutionResult result = executor.execute(input, "tenant-1");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("CONCURRENT node execution is not implemented");
        assertThat(result.getOutput()).isEmpty();
    }
}
