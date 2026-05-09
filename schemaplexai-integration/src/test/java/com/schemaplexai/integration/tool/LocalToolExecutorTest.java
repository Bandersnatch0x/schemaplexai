package com.schemaplexai.integration.tool;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LocalToolExecutorTest {

    private final LocalToolExecutor executor = new LocalToolExecutor();

    @Test
    void getToolName_returnsLocal() {
        assertThat(executor.getToolName()).isEqualTo("local");
    }

    @Test
    void execute_echo_returnsMessage() {
        Map<String, Object> params = Map.of("action", "echo", "message", "hello world");
        String result = executor.execute(params);
        assertThat(result).isEqualTo("hello world");
    }

    @Test
    void execute_math_add() {
        Map<String, Object> params = Map.of("action", "math", "a", 3, "b", 5, "operator", "add");
        String result = executor.execute(params);
        assertThat(result).isEqualTo("8.0");
    }

    @Test
    void execute_math_subtract() {
        Map<String, Object> params = Map.of("action", "math", "a", 10, "b", 4, "operator", "subtract");
        String result = executor.execute(params);
        assertThat(result).isEqualTo("6.0");
    }

    @Test
    void execute_math_multiply() {
        Map<String, Object> params = Map.of("action", "math", "a", 3, "b", 4, "operator", "multiply");
        String result = executor.execute(params);
        assertThat(result).isEqualTo("12.0");
    }

    @Test
    void execute_math_divide() {
        Map<String, Object> params = Map.of("action", "math", "a", 10, "b", 2, "operator", "divide");
        String result = executor.execute(params);
        assertThat(result).isEqualTo("5.0");
    }

    @Test
    void execute_math_divideByZero_returnsError() {
        Map<String, Object> params = Map.of("action", "math", "a", 10, "b", 0, "operator", "divide");
        String result = executor.execute(params);
        assertThat(result).isEqualTo("error: division by zero");
    }

    @Test
    void execute_math_unknownOperator_returnsError() {
        Map<String, Object> params = Map.of("action", "math", "a", 10, "b", 2, "operator", "power");
        String result = executor.execute(params);
        assertThat(result).isEqualTo("error: unknown operator");
    }

    @Test
    void execute_unknownAction_returnsError() {
        Map<String, Object> params = Map.of("action", "unknown");
        String result = executor.execute(params);
        assertThat(result).isEqualTo("error: unknown action");
    }
}
