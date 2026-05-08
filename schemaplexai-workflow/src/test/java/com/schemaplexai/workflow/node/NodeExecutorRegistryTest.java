package com.schemaplexai.workflow.node;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class NodeExecutorRegistryTest {

    @Autowired
    private List<NodeExecutor> executors;

    @Test
    void allSevenExecutorsAreRegistered() {
        Map<String, NodeExecutor> byType = executors.stream()
                .collect(Collectors.toMap(NodeExecutor::getNodeType, Function.identity()));

        assertThat(byType).containsKeys("HTTP", "SCRIPT", "START", "END", "AI_MODEL", "TOOL_CALL", "CONDITION", "CONCURRENT", "JOIN");
        assertThat(byType).hasSize(9);
    }

    @Test
    void startNodeExecutor_returnsCorrectType() {
        NodeExecutor executor = findByType("START");
        assertThat(executor.getNodeType()).isEqualTo("START");
    }

    @Test
    void startNodeExecutor_validInput_returnsSuccessWithStartedAt() {
        NodeExecutor executor = findByType("START");
        NodeExecutionResult result = executor.execute(Map.of("workflowInstanceId", "wf-123"), "tenant-1");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).containsEntry("workflowInstanceId", "wf-123");
        assertThat(result.getOutput()).containsKey("startedAt");
    }

    @Test
    void startNodeExecutor_missingWorkflowInstanceId_returnsFailure() {
        NodeExecutor executor = findByType("START");
        NodeExecutionResult result = executor.execute(Map.of(), "tenant-1");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("workflowInstanceId");
    }

    @Test
    void endNodeExecutor_returnsCorrectType() {
        NodeExecutor executor = findByType("END");
        assertThat(executor.getNodeType()).isEqualTo("END");
    }

    @Test
    void endNodeExecutor_withStatus_returnsSuccess() {
        NodeExecutor executor = findByType("END");
        NodeExecutionResult result = executor.execute(
                Map.of("result", "done", "status", "SUCCESS"), "tenant-1");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).containsEntry("result", "done");
        assertThat(result.getOutput()).containsEntry("status", "SUCCESS");
        assertThat(result.getOutput()).containsKey("endedAt");
    }

    @Test
    void endNodeExecutor_withoutStatus_defaultsToCompleted() {
        NodeExecutor executor = findByType("END");
        NodeExecutionResult result = executor.execute(Map.of(), "tenant-1");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).containsEntry("status", "COMPLETED");
    }

    @Test
    void aiModelNodeExecutor_returnsCorrectType() {
        NodeExecutor executor = findByType("AI_MODEL");
        assertThat(executor.getNodeType()).isEqualTo("AI_MODEL");
    }

    @Test
    void aiModelNodeExecutor_validInput_returnsSimulatedResponse() {
        NodeExecutor executor = findByType("AI_MODEL");
        NodeExecutionResult result = executor.execute(
                Map.of("prompt", "Hello world, this is a test prompt", "modelId", "gpt-4"), "tenant-1");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).containsKey("generatedText");
        assertThat(result.getOutput()).containsEntry("modelUsed", "gpt-4");
    }

    @Test
    void aiModelNodeExecutor_withoutModelId_usesDefault() {
        NodeExecutor executor = findByType("AI_MODEL");
        NodeExecutionResult result = executor.execute(Map.of("prompt", "Hello"), "tenant-1");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).containsEntry("modelUsed", "default");
    }

    @Test
    void aiModelNodeExecutor_nullPrompt_returnsFailure() {
        NodeExecutor executor = findByType("AI_MODEL");
        NodeExecutionResult result = executor.execute(Map.of(), "tenant-1");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("prompt");
    }

    @Test
    void toolCallNodeExecutor_returnsCorrectType() {
        NodeExecutor executor = findByType("TOOL_CALL");
        assertThat(executor.getNodeType()).isEqualTo("TOOL_CALL");
    }

    @Test
    void toolCallNodeExecutor_validInput_returnsSimulatedResult() {
        NodeExecutor executor = findByType("TOOL_CALL");
        NodeExecutionResult result = executor.execute(
                Map.of("toolName", "sendEmail", "toolParameters", Map.of("to", "user@example.com")),
                "tenant-1");

        assertThat(result.isSuccess()).isTrue();
        @SuppressWarnings("unchecked")
        Map<String, Object> toolResult = (Map<String, Object>) result.getOutput().get("toolResult");
        assertThat(toolResult).containsEntry("tool", "sendEmail");
        assertThat(toolResult).containsEntry("status", "executed");
    }

    @Test
    void toolCallNodeExecutor_nullToolName_returnsFailure() {
        NodeExecutor executor = findByType("TOOL_CALL");
        NodeExecutionResult result = executor.execute(Map.of(), "tenant-1");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("toolName");
    }

    @Test
    void conditionNodeExecutor_returnsCorrectType() {
        NodeExecutor executor = findByType("CONDITION");
        assertThat(executor.getNodeType()).isEqualTo("CONDITION");
    }

    @Test
    void conditionNodeExecutor_equalityMatch_returnsTrueBranch() {
        NodeExecutor executor = findByType("CONDITION");
        NodeExecutionResult result = executor.execute(
                Map.of("expression", "status == 'active'",
                        "variables", Map.of("status", "active")),
                "tenant-1");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).containsEntry("branch", "true");
    }

    @Test
    void conditionNodeExecutor_equalityMismatch_returnsFalseBranch() {
        NodeExecutor executor = findByType("CONDITION");
        NodeExecutionResult result = executor.execute(
                Map.of("expression", "status == 'active'",
                        "variables", Map.of("status", "inactive")),
                "tenant-1");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).containsEntry("branch", "false");
    }

    @Test
    void conditionNodeExecutor_numericGreaterThan_returnsTrueBranch() {
        NodeExecutor executor = findByType("CONDITION");
        NodeExecutionResult result = executor.execute(
                Map.of("expression", "score > 50",
                        "variables", Map.of("score", 75)),
                "tenant-1");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).containsEntry("branch", "true");
    }

    @Test
    void conditionNodeExecutor_nullExpression_returnsDefaultBranch() {
        NodeExecutor executor = findByType("CONDITION");
        NodeExecutionResult result = executor.execute(Map.of(), "tenant-1");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).containsEntry("branch", "default");
    }

    @Test
    void httpNodeExecutor_returnsCorrectType() {
        NodeExecutor executor = findByType("HTTP");
        assertThat(executor.getNodeType()).isEqualTo("HTTP");
    }

    @Test
    void scriptNodeExecutor_returnsCorrectType() {
        NodeExecutor executor = findByType("SCRIPT");
        assertThat(executor.getNodeType()).isEqualTo("SCRIPT");
    }

    private NodeExecutor findByType(String type) {
        return executors.stream()
                .filter(e -> e.getNodeType().equals(type))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Executor not found: " + type));
    }
}
