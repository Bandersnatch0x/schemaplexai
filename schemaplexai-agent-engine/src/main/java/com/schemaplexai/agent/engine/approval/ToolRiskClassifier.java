package com.schemaplexai.agent.engine.approval;

import com.schemaplexai.agent.engine.tool.ToolCall;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Classifies tool calls by risk level to determine whether approval is required.
 *
 * <p>Risk levels:
 * <ul>
 *   <li>{@code LOW} — read-only, safe operations (no approval needed)</li>
 *   <li>{@code MEDIUM} — reversible write operations (approval recommended)</li>
 *   <li>{@code HIGH} — destructive or irreversible operations (approval required)</li>
 *   <li>{@code CRITICAL} — system-level operations with broad impact (always require approval)</li>
 * </ul>
 */
@Slf4j
@Component
public class ToolRiskClassifier {

    /**
     * Tools classified as HIGH risk (destructive / irreversible).
     */
    private static final Set<String> HIGH_RISK_TOOLS = Set.of(
            "volumeDelete",
            "fileDelete",
            "dropDatabase",
            "dropTable",
            "truncateTable",
            "executeShell",
            "deployProduction",
            "deleteInfrastructure",
            "revokeAccess",
            "deleteUser"
    );

    /**
     * Tools classified as CRITICAL risk (system-level, broad impact).
     */
    private static final Set<String> CRITICAL_RISK_TOOLS = Set.of(
            "systemShutdown",
            "networkReconfigure",
            "securityPolicyOverride",
            "bulkDataDelete",
            "adminResetAll"
    );

    /**
     * Tenant-specific risk overrides. Key: "tenantId:toolName", Value: risk level.
     */
    private final Map<String, String> tenantOverrides = new ConcurrentHashMap<>();

    /**
     * Classifies the risk level of a tool call.
     *
     * @param toolCall the tool call to classify
     * @param tenantId the tenant context (for tenant-specific overrides)
     * @return the risk level
     */
    public String classify(ToolCall toolCall, String tenantId) {
        String toolName = toolCall.toolName();

        // Check tenant-specific overrides first
        String overrideKey = tenantId + ":" + toolName;
        String override = tenantOverrides.get(overrideKey);
        if (override != null) {
            log.debug("Using tenant override risk={} for tool={} tenant={}", override, toolName, tenantId);
            return override;
        }

        // Check built-in classifications
        if (CRITICAL_RISK_TOOLS.contains(toolName)) {
            return "CRITICAL";
        }
        if (HIGH_RISK_TOOLS.contains(toolName)) {
            return "HIGH";
        }

        return "LOW";
    }

    /**
     * Checks whether the tool call requires human approval based on the current approval mode.
     *
     * @param toolCall     the tool call to check
     * @param tenantId     the tenant context
     * @param approvalMode the current approval mode
     * @return {@code true} if approval is required
     */
    public boolean requiresApproval(ToolCall toolCall, String tenantId, ApprovalMode approvalMode) {
        if (approvalMode == ApprovalMode.AUTO) {
            return false;
        }

        String riskLevel = classify(toolCall, tenantId);
        return switch (riskLevel) {
            case "CRITICAL" -> true;
            case "HIGH" -> approvalMode == ApprovalMode.MANUAL;
            default -> false;
        };
    }

    /**
     * Registers a tenant-specific risk override for a tool.
     *
     * @param tenantId  the tenant
     * @param toolName  the tool name
     * @param riskLevel the risk level to assign
     */
    public void setTenantOverride(String tenantId, String toolName, String riskLevel) {
        tenantOverrides.put(tenantId + ":" + toolName, riskLevel);
    }
}
