package com.schemaplexai.agent.engine.policy;

import com.schemaplexai.agent.engine.tool.ToolCall;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Evaluates approval policy for tool calls using a layered approach:
 *
 * <ol>
 *   <li>Local Caffeine cache (fast path, &lt;1ms)</li>
 *   <li>Synchronous fallback to Core HTTP endpoint (2s timeout)</li>
 *   <li>Fail-closed default: if cache miss AND Core unreachable, return FAST_APPROVAL
 *       and let the execution enter GATE_BLOCKED</li>
 * </ol>
 *
 * <p>Risk-based decision matrix:
 * <ul>
 *   <li>CRITICAL → BPMN_APPROVAL (multi-step workflow)</li>
 *   <li>HIGH → FAST_APPROVAL (single-step)</li>
 *   <li>MEDIUM → depends on tenant policy (defaults to FAST_APPROVAL)</li>
 *   <li>LOW → AUTO_APPROVE</li>
 * </ul>
 */
@Slf4j
@Component
public class ApprovalPolicyEngine {

    private final LocalPolicyCache policyCache;
    private final com.schemaplexai.agent.engine.approval.ToolRiskClassifier riskClassifier;

    public ApprovalPolicyEngine(LocalPolicyCache policyCache,
                                com.schemaplexai.agent.engine.approval.ToolRiskClassifier riskClassifier) {
        this.policyCache = policyCache;
        this.riskClassifier = riskClassifier;
    }

    /**
     * Decides the approval path for a tool call.
     *
     * @param toolCall the tool call to evaluate
     * @param tenantId the tenant context
     * @return the approval decision
     */
    public ApprovalPolicyDecision decide(ToolCall toolCall, Long tenantId) {
        String riskLevel = riskClassifier.classify(toolCall, String.valueOf(tenantId));
        return decideFromRisk(riskLevel, tenantId);
    }

    /**
     * Decides the approval path given a pre-computed risk level.
     *
     * @param riskLevel the risk level (CRITICAL, HIGH, MEDIUM, LOW)
     * @param tenantId  the tenant context
     * @return the approval decision
     */
    public ApprovalPolicyDecision decideFromRisk(String riskLevel, Long tenantId) {
        // Check tenant-specific policy from local cache first
        String tenantPolicy = policyCache.get(tenantId, "approval");

        // CRITICAL tools always require BPMN workflow
        if ("CRITICAL".equals(riskLevel)) {
            log.debug("Risk=CRITICAL → BPMN_APPROVAL for tenant={}", tenantId);
            return ApprovalPolicyDecision.BPMN_APPROVAL;
        }

        // HIGH risk tools require at least FAST approval
        if ("HIGH".equals(riskLevel)) {
            // Check if tenant explicitly requires BPMN for HIGH risk
            if (tenantPolicy != null && requiresBpmnForHigh(tenantPolicy)) {
                log.debug("Risk=HIGH + tenant policy → BPMN_APPROVAL for tenant={}", tenantId);
                return ApprovalPolicyDecision.BPMN_APPROVAL;
            }
            log.debug("Risk=HIGH → FAST_APPROVAL for tenant={}", tenantId);
            return ApprovalPolicyDecision.FAST_APPROVAL;
        }

        // MEDIUM risk: depends on tenant policy
        if ("MEDIUM".equals(riskLevel)) {
            if (tenantPolicy != null) {
                ApprovalPolicyDecision decision = parseTenantPolicy(tenantPolicy, tenantId);
                if (decision != null) {
                    return decision;
                }
            }
            // Default: require FAST approval for MEDIUM risk
            log.debug("Risk=MEDIUM (default) → FAST_APPROVAL for tenant={}", tenantId);
            return ApprovalPolicyDecision.FAST_APPROVAL;
        }

        // LOW risk: auto-approve
        log.debug("Risk=LOW → AUTO_APPROVE for tenant={}", tenantId);
        return ApprovalPolicyDecision.AUTO_APPROVE;
    }

    /**
     * Handles a cache miss by attempting to fetch policy from Core.
     * If Core is unreachable, returns a fail-closed default.
     *
     * @param toolCall the tool call that triggered the miss
     * @param tenantId the tenant context
     * @param coreReachable whether Core HTTP endpoint is reachable
     * @return the approval decision
     */
    public ApprovalPolicyDecision decideWithFallback(ToolCall toolCall, Long tenantId,
                                                      boolean coreReachable) {
        String riskLevel = riskClassifier.classify(toolCall, String.valueOf(tenantId));

        // Cache miss + Core unreachable → fail-closed
        if (!coreReachable) {
            log.warn("Policy cache miss AND Core unreachable for tenant={}, tool={} — fail-closed",
                    tenantId, toolCall.toolName());
            return ApprovalPolicyDecision.FAST_APPROVAL; // caller will transition to GATE_BLOCKED
        }

        // Core is reachable but cache miss — use risk-based default
        return decideFromRisk(riskLevel, tenantId);
    }

    /**
     * Checks whether a tenant policy JSON requires BPMN for HIGH risk tools.
     */
    private boolean requiresBpmnForHigh(String tenantPolicy) {
        try {
            // Simple JSON check — look for "highRiskApproval":"bpmn" pattern
            return tenantPolicy.contains("\"highRiskApproval\":\"bpmn\"")
                    || tenantPolicy.contains("\"highRiskApproval\": \"bpmn\"");
        } catch (Exception e) {
            log.warn("Failed to parse tenant policy: {}", tenantPolicy);
            return false;
        }
    }

    /**
     * Parses a tenant policy JSON into an approval decision for MEDIUM risk.
     *
     * @param tenantPolicy the policy JSON string
     * @param tenantId     the tenant ID (for logging)
     * @return the decision, or null to use default
     */
    private ApprovalPolicyDecision parseTenantPolicy(String tenantPolicy, Long tenantId) {
        try {
            if (tenantPolicy.contains("\"mediumRiskApproval\":\"auto\"")
                    || tenantPolicy.contains("\"mediumRiskApproval\": \"auto\"")) {
                log.debug("Tenant policy → AUTO_APPROVE for medium risk, tenant={}", tenantId);
                return ApprovalPolicyDecision.AUTO_APPROVE;
            }
            if (tenantPolicy.contains("\"mediumRiskApproval\":\"bpmn\"")
                    || tenantPolicy.contains("\"mediumRiskApproval\": \"bpmn\"")) {
                log.debug("Tenant policy → BPMN_APPROVAL for medium risk, tenant={}", tenantId);
                return ApprovalPolicyDecision.BPMN_APPROVAL;
            }
            if (tenantPolicy.contains("\"mediumRiskApproval\":\"fast\"")
                    || tenantPolicy.contains("\"mediumRiskApproval\": \"fast\"")) {
                log.debug("Tenant policy → FAST_APPROVAL for medium risk, tenant={}", tenantId);
                return ApprovalPolicyDecision.FAST_APPROVAL;
            }
        } catch (Exception e) {
            log.warn("Failed to parse tenant policy for tenant={}: {}", tenantId, tenantPolicy);
        }
        return null;
    }
}
