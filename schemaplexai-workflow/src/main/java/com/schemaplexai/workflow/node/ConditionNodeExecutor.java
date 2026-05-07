package com.schemaplexai.workflow.node;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class ConditionNodeExecutor implements NodeExecutor {

    @Override
    public String getNodeType() {
        return "CONDITION";
    }

    @Override
    @SuppressWarnings("unchecked")
    public NodeExecutionResult execute(Map<String, Object> input, String tenantId) {
        String expression = (String) input.get("expression");
        Object variablesObj = input.get("variables");
        Map<String, Object> variables = (variablesObj instanceof Map)
                ? (Map<String, Object>) variablesObj
                : Map.of();

        String branch = evaluateExpression(expression, variables);

        Map<String, Object> output = new HashMap<>();
        output.put("branch", branch);
        output.put("expression", expression);

        log.info("Condition node evaluated: expression='{}', branch={}, tenantId={}",
                expression, branch, tenantId);
        return NodeExecutionResult.success(output);
    }

    private String evaluateExpression(String expression, Map<String, Object> variables) {
        if (expression == null || expression.isBlank()) {
            return "default";
        }

        String trimmed = expression.trim();

        String[] operators = {">=", "<=", "!=", "==", ">", "<"};
        for (String op : operators) {
            int idx = trimmed.indexOf(op);
            if (idx >= 0) {
                String left = trimmed.substring(0, idx).trim();
                String right = trimmed.substring(idx + op.length()).trim();
                return evaluateComparison(left, right, op, variables) ? "true" : "false";
            }
        }

        return "default";
    }

    private boolean evaluateComparison(String leftKey, String rightKey, String operator,
                                       Map<String, Object> variables) {
        Object leftValue = resolveValue(leftKey, variables);
        Object rightValue = resolveValue(rightKey, variables);

        if (leftValue == null || rightValue == null) {
            return false;
        }

        if (isNumeric(leftValue) && isNumeric(rightValue)) {
            double leftNum = toDouble(leftValue);
            double rightNum = toDouble(rightValue);
            return switch (operator) {
                case "==" -> Double.compare(leftNum, rightNum) == 0;
                case "!=" -> Double.compare(leftNum, rightNum) != 0;
                case ">" -> leftNum > rightNum;
                case "<" -> leftNum < rightNum;
                case ">=" -> leftNum >= rightNum;
                case "<=" -> leftNum <= rightNum;
                default -> false;
            };
        }

        String leftStr = leftValue.toString();
        String rightStr = rightValue.toString();
        return switch (operator) {
            case "==" -> leftStr.equals(rightStr);
            case "!=" -> !leftStr.equals(rightStr);
            default -> false;
        };
    }

    private Object resolveValue(String key, Map<String, Object> variables) {
        if (key.startsWith("'") && key.endsWith("'")) {
            return key.substring(1, key.length() - 1);
        }
        if (key.startsWith("\"") && key.endsWith("\"")) {
            return key.substring(1, key.length() - 1);
        }
        Object value = variables.get(key);
        if (value != null) {
            return value;
        }
        // Try parsing as a numeric literal
        try {
            return Double.parseDouble(key);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isNumeric(Object value) {
        if (value instanceof Number) {
            return true;
        }
        try {
            Double.parseDouble(value.toString());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return Double.parseDouble(value.toString());
    }
}
