package com.schemaplexai.agent.engine.tool;

/**
 * 验证结果，统一表示验证是否通过及失败原因。
 */
public record ValidationResult(boolean isValid, String errorMessage) {

    public static ValidationResult valid() {
        return new ValidationResult(true, null);
    }

    public static ValidationResult invalid(String errorMessage) {
        return new ValidationResult(false, errorMessage);
    }
}
