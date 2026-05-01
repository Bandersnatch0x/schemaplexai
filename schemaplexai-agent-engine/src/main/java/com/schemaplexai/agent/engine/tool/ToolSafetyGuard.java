package com.schemaplexai.agent.engine.tool;

import org.springframework.stereotype.Component;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class ToolSafetyGuard {

    private static final Set<String> IRREVERSIBLE_TOOLS = Set.of(
        "volumeDelete", "databaseDrop", "destroy", "purge", "hardDelete", "permanentDelete"
    );

    private static final Pattern IRREVERSIBLE_PATTERN = Pattern.compile(
        "(?i)(DROP\\s+TABLE|DROP\\s+DATABASE|DELETE\\s+FROM|TRUNCATE|RM\\s+-RF)"
    );

    public SafetyCheckResult check(String toolName, String arguments) {
        return check(toolName, arguments, null);
    }

    public SafetyCheckResult check(String toolName, String arguments, String expectedEnvironment) {
        // Check 1: Irreversible tool names
        if (IRREVERSIBLE_TOOLS.contains(toolName)) {
            return SafetyCheckResult.reject(
                ToolErrorCategory.IRREVERSIBLE_OPERATION,
                "Operation blocked by safety policy."
            );
        }

        // Check 2: Irreversible commands in arguments
        if (arguments != null && IRREVERSIBLE_PATTERN.matcher(arguments).find()) {
            return SafetyCheckResult.reject(
                ToolErrorCategory.IRREVERSIBLE_OPERATION,
                "Operation blocked by safety policy."
            );
        }

        // Check 3: Environment/credential mismatch
        if (expectedEnvironment != null && arguments != null) {
            if (arguments.contains("prod") && !expectedEnvironment.contains("prod")) {
                return SafetyCheckResult.reject(
                    ToolErrorCategory.ENVIRONMENT_MISMATCH,
                    "Operation blocked by safety policy."
                );
            }
        }

        return SafetyCheckResult.permit();
    }

    public record SafetyCheckResult(
        boolean allowed,
        boolean blocked,
        ToolErrorCategory errorCategory,
        String reason
    ) {
        public static SafetyCheckResult permit() {
            return new SafetyCheckResult(true, false, null, null);
        }

        public static SafetyCheckResult reject(ToolErrorCategory category, String reason) {
            return new SafetyCheckResult(false, true, category, reason);
        }
    }
}
