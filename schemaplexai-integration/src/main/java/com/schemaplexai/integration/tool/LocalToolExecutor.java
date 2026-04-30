package com.schemaplexai.integration.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class LocalToolExecutor implements ToolExecutor {

    @Override
    public String getToolName() {
        return "local";
    }

    @Override
    public String execute(Map<String, Object> parameters) {
        String action = (String) parameters.get("action");
        log.info("Executing local tool action: {}", action);

        return switch (action) {
            case "echo" -> (String) parameters.get("message");
            case "math" -> {
                double a = ((Number) parameters.get("a")).doubleValue();
                double b = ((Number) parameters.get("b")).doubleValue();
                String op = (String) parameters.get("operator");
                yield switch (op) {
                    case "add" -> String.valueOf(a + b);
                    case "subtract" -> String.valueOf(a - b);
                    case "multiply" -> String.valueOf(a * b);
                    case "divide" -> b != 0 ? String.valueOf(a / b) : "error: division by zero";
                    default -> "error: unknown operator";
                };
            }
            default -> "error: unknown action";
        };
    }
}
