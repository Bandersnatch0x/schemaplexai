package com.schemaplexai.agent.engine.tool;

import com.schemaplexai.agent.engine.entity.SfAgentExecutionLog;
import com.schemaplexai.agent.engine.mapper.SfAgentExecutionLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ToolExecutionRecorder {

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
}
