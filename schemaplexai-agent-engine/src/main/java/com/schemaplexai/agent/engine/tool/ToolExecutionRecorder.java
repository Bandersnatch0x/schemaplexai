package com.schemaplexai.agent.engine.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.schemaplexai.agent.engine.entity.SfAgentExecutionLog;
import com.schemaplexai.agent.engine.mapper.SfAgentExecutionLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class ToolExecutionRecorder {

    private static final int DEFAULT_RECENT_FAILURE_LIMIT = 200;
    private static final int MAX_RECENT_FAILURE_LIMIT = 500;
    private static final Pattern FIELD_PATTERN = Pattern.compile("([a-zA-Z]+)=(\"([^\"]*)\"|[^,]+)");

    private final SfAgentExecutionLogMapper logMapper;

    public void record(Long executionId, ToolExecutionResult result) {
        SfAgentExecutionLog logEntry = new SfAgentExecutionLog();
        logEntry.setExecutionId(executionId);
        logEntry.setState(mapState(result));
        logEntry.setMessage(formatMessage(result));

        try {
            logMapper.insert(logEntry);
        } catch (Exception e) {
            log.error("Failed to persist tool execution log for executionId={}", executionId, e);
            if (result.errorCategory() != null && result.errorCategory().isSecurityRelated()) {
                throw new ToolExecutionAuditException(
                    "Security-related tool execution audit log failed for execution " + executionId, e);
            }
        }
    }

    public List<ToolExecutionResult> listRecentFailures(String tenantId, int limit) {
        if (tenantId == null || tenantId.isBlank()) {
            return Collections.emptyList();
        }

        int safeLimit = normalizeLimit(limit);
        List<SfAgentExecutionLog> logs = logMapper.selectList(
                new LambdaQueryWrapper<SfAgentExecutionLog>()
                        .eq(SfAgentExecutionLog::getTenantId, tenantId)
                        .in(SfAgentExecutionLog::getState, "TOOL_FAILURE", "TOOL_BLOCKED")
                        .orderByDesc(SfAgentExecutionLog::getCreatedAt)
                        .last("LIMIT " + safeLimit));

        if (logs == null || logs.isEmpty()) {
            return Collections.emptyList();
        }

        return logs.stream()
                .map(this::parseFailureLog)
                .flatMap(List::stream)
                .toList();
    }

    private String mapState(ToolExecutionResult result) {
        if (result.blocked()) {
            return "TOOL_BLOCKED";
        }
        if (result.success()) {
            return "TOOL_SUCCESS";
        }
        return "TOOL_FAILURE";
    }

    private String formatMessage(ToolExecutionResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("tool=").append(result.toolName());
        sb.append(", status=");
        if (result.blocked()) {
            sb.append("BLOCKED");
        } else if (result.success()) {
            sb.append("SUCCESS");
        } else {
            sb.append("FAILURE");
        }
        if (result.errorCategory() != null) {
            sb.append(", category=").append(result.errorCategory().name());
        }
        if (result.errorMessage() != null) {
            sb.append(", error=\"").append(result.errorMessage()).append("\"");
        }
        sb.append(", latencyMs=").append(result.latencyMs());
        sb.append(", tokens=").append(result.tokenCount());
        return sb.toString();
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_RECENT_FAILURE_LIMIT;
        }
        return Math.min(limit, MAX_RECENT_FAILURE_LIMIT);
    }

    private List<ToolExecutionResult> parseFailureLog(SfAgentExecutionLog logEntry) {
        if (logEntry == null || logEntry.getMessage() == null || logEntry.getMessage().isBlank()) {
            return Collections.emptyList();
        }

        ParsedToolLog parsed = ParsedToolLog.parse(logEntry.getMessage());
        if (parsed.toolName == null || parsed.category == null) {
            return Collections.emptyList();
        }

        boolean blocked = "TOOL_BLOCKED".equals(logEntry.getState())
                || "BLOCKED".equalsIgnoreCase(parsed.status);
        ToolExecutionResult result = blocked
                ? ToolExecutionResult.blocked(parsed.toolName, parsed.category, parsed.errorMessage)
                : ToolExecutionResult.failure(
                        parsed.toolName,
                        parsed.category,
                        parsed.errorMessage,
                        parsed.latencyMs,
                        parsed.tokenCount);
        return List.of(result);
    }

    private static final class ParsedToolLog {
        private String toolName;
        private String status;
        private ToolErrorCategory category;
        private String errorMessage;
        private long latencyMs;
        private int tokenCount;

        private static ParsedToolLog parse(String message) {
            ParsedToolLog parsed = new ParsedToolLog();
            Matcher matcher = FIELD_PATTERN.matcher(message);
            while (matcher.find()) {
                String key = matcher.group(1);
                String value = matcher.group(3) != null ? matcher.group(3) : matcher.group(2).trim();
                parsed.apply(key, value);
            }
            return parsed;
        }

        private void apply(String key, String value) {
            switch (key) {
                case "tool" -> toolName = value;
                case "status" -> status = value;
                case "category" -> category = parseCategory(value);
                case "error" -> errorMessage = value;
                case "latencyMs" -> latencyMs = parseLong(value);
                case "tokens" -> tokenCount = (int) parseLong(value);
                default -> {
                    // Ignore forward-compatible fields.
                }
            }
        }

        private ToolErrorCategory parseCategory(String value) {
            try {
                return ToolErrorCategory.valueOf(value);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        private long parseLong(String value) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
    }
}
