package com.schemaplexai.workflow.node;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConditionNodeExecutorTest {

    private final ConditionNodeExecutor executor = new ConditionNodeExecutor();

    @Test
    void execute_booleanLiteralComparison_returnsTrueBranch() {
        NodeExecutionResult result = executor.execute(Map.of(
                "expression", "approved == true",
                "variables", Map.of("approved", true)), "tenant-1");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).containsEntry("branch", "true");
    }
}
