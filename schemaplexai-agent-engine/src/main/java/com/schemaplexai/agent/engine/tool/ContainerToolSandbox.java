package com.schemaplexai.agent.engine.tool;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * 基于容器的工具执行沙箱，提供白名单验证和参数检查。
 */
@Component
public class ContainerToolSandbox implements ToolSandbox {

    private static final int MAX_PARAMETERS = 20;
    private static final int MAX_PARAMETER_VALUE_LENGTH = 10000;

    private final ToolWhitelist whitelist;

    public ContainerToolSandbox() {
        this.whitelist = new ToolWhitelist(Set.of());
    }

    public ContainerToolSandbox(ToolWhitelist whitelist) {
        this.whitelist = whitelist;
    }

    @Override
    public ToolResult execute(ToolCall toolCall, SandboxConfig config) throws ToolExecutionException {
        // 验证工具调用
        ValidationResult validation = validate(toolCall);
        if (!validation.isValid()) {
            throw new ToolExecutionException(
                ToolErrorCategory.INVALID_ARGUMENT,
                "Tool call validation failed: " + validation.errorMessage()
            );
        }

        // 验证工具是否在白名单中
        if (!whitelist.isAllowed(toolCall.toolName())) {
            throw new ToolExecutionException(
                ToolErrorCategory.PERMISSION_DENIED,
                "Tool '" + toolCall.toolName() + "' is not in the allowed list"
            );
        }

        try {
            return executeInContainer(toolCall, config);
        } catch (Exception e) {
            throw new ToolExecutionException(
                ToolErrorCategory.INTERNAL_ERROR,
                "Tool execution failed: " + e.getMessage(),
                e
            );
        }
    }

    @Override
    public ValidationResult validate(ToolCall toolCall) {
        if (toolCall.toolName() == null || toolCall.toolName().isBlank()) {
            return ValidationResult.invalid("Tool name is required");
        }

        if (toolCall.parameters() != null && toolCall.parameters().size() > MAX_PARAMETERS) {
            return ValidationResult.invalid("Too many parameters (max " + MAX_PARAMETERS + ")");
        }

        if (toolCall.parameters() != null) {
            for (Map.Entry<String, Object> param : toolCall.parameters().entrySet()) {
                if (param.getValue() != null
                        && param.getValue().toString().length() > MAX_PARAMETER_VALUE_LENGTH) {
                    return ValidationResult.invalid(
                            "Parameter '" + param.getKey() + "' exceeds max length");
                }
            }
        }

        return ValidationResult.valid();
    }

    private ToolResult executeInContainer(ToolCall toolCall, SandboxConfig config) {
        // Container execution framework placeholder.
        // Future implementation:
        // 1. Create temporary container
        // 2. Limit resources (CPU / memory / network)
        // 3. Execute tool call
        // 4. Collect output
        // 5. Clean up container
        return ToolResult.success("Tool '" + toolCall.toolName() + "' executed in sandbox");
    }
}
