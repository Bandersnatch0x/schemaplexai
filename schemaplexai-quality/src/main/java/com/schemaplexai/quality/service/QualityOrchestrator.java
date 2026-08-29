package com.schemaplexai.quality.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.quality.entity.SfQualityGate;
import com.schemaplexai.quality.entity.SfQualityIssue;
import com.schemaplexai.quality.gate.GateDisposition;
import com.schemaplexai.quality.gate.QualityCheckResult;
import com.schemaplexai.quality.gate.QualityContext;
import com.schemaplexai.quality.gate.QualityReport;
import com.schemaplexai.quality.gate.QualityRule;
import com.schemaplexai.quality.mapper.QualityGateMapper;
import com.schemaplexai.quality.mapper.QualityIssueMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Orchestrates quality gate evaluation.
 *
 * <p>Evaluation semantics (ticket 924, spec docs/specs/2026-04-30-v1.0-quality-gate.md):
 * <ul>
 *   <li>Only gates with status {@code ACTIVE} participate (REQ-02/903).</li>
 *   <li>An empty gate table no longer passes vacuously: the built-in default
 *       policy of spec §4.2 (SECURITY_SCAN/BLOCK, SPEC_COMPLIANCE/WARN,
 *       OUTPUT_FORMAT/WARN) is applied (REQ-15). The built-in policy was
 *       chosen over SQL seeding so existing deployments with an empty table
 *       are covered too — the docker init scripts only run on a fresh
 *       database volume.</li>
 *   <li>Every failed result carries a disposition (PASS/WARN/BLOCK/FAIL)
 *       resolved from the rule's policy entry, escalated by the CRITICAL
 *       severity floor. The returned {@link QualityReport#getDisposition()}
 *       is the structured verdict the caller acts on (REQ-02): PASS continue,
 *       WARN record alert and continue, BLOCK pause for manual confirmation,
 *       FAIL terminate.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QualityOrchestrator {

    /** sf_quality_gate.status value that marks gates eligible for evaluation. */
    private static final String GATE_STATUS_ACTIVE = "ACTIVE";
    /** sf_quality_issue.status value written for newly recorded issues. */
    private static final String ISSUE_STATUS_OPEN = "OPEN";
    /** Name reported for the built-in policy when no ACTIVE gate is configured. */
    static final String DEFAULT_POLICY_GATE_NAME = "DEFAULT_POLICY";

    /**
     * Built-in default policy (spec §4.2), applied when no ACTIVE gate is
     * configured. Dispositions are the action the caller must take, not issue
     * severities. Fail-closed: a rule without an implementation yields a
     * CRITICAL/BLOCK result rather than being skipped.
     */
    static final Map<String, GateDisposition> DEFAULT_POLICY;

    static {
        Map<String, GateDisposition> policy = new LinkedHashMap<>();
        policy.put("SECURITY_SCAN", GateDisposition.BLOCK);
        policy.put("SPEC_COMPLIANCE", GateDisposition.WARN);
        policy.put("OUTPUT_FORMAT", GateDisposition.WARN);
        DEFAULT_POLICY = Map.copyOf(policy);
    }

    /** Fail-closed disposition for rules absent from the policy map. */
    private static final GateDisposition UNKNOWN_RULE_DISPOSITION = GateDisposition.BLOCK;

    private final List<QualityRule> ruleList;
    private final QualityGateMapper gateMapper;
    private final QualityIssueMapper issueMapper;
    private final ObjectMapper objectMapper;

    private Map<String, QualityRule> rules;

    @jakarta.annotation.PostConstruct
    public void init() {
        this.rules = ruleList.stream()
                .collect(Collectors.toMap(QualityRule::getRuleName, Function.identity()));
    }

    @Transactional(rollbackFor = Exception.class)
    public QualityReport evaluate(Long executionId, QualityContext context) {
        List<SfQualityGate> gates = gateMapper.selectList(
                new LambdaQueryWrapper<SfQualityGate>()
                        .eq(SfQualityGate::getStatus, GATE_STATUS_ACTIVE));
        List<QualityCheckResult> results = new ArrayList<>();
        boolean allPassed = true;
        GateDisposition overall = GateDisposition.PASS;

        if (gates.isEmpty()) {
            // REQ-15: an empty gate table must not pass vacuously. Apply the
            // built-in default policy (spec §4.2) instead.
            log.warn("No ACTIVE quality gate configured for execution {}; applying built-in default policy {}",
                    executionId, DEFAULT_POLICY.keySet());
            for (Map.Entry<String, GateDisposition> entry : DEFAULT_POLICY.entrySet()) {
                QualityCheckResult result = runRule(executionId, context, entry.getKey(),
                        DEFAULT_POLICY_GATE_NAME, results);
                allPassed &= result.isPassed();
                overall = GateDisposition.mostSevere(overall, result.getDisposition());
            }
            QualityReport report = new QualityReport(executionId, allPassed, results, overall);
            log.info("Quality evaluation (default policy) for execution {}: passed={}, disposition={}",
                    executionId, allPassed, overall);
            return report;
        }

        for (SfQualityGate gate : gates) {
            List<String> ruleNames;
            try {
                ruleNames = parseRulesJson(gate.getRulesJson());
            } catch (Exception e) {
                log.warn("Failed to parse rules JSON for gate {}: {}", gate.getName(), gate.getRulesJson(), e);
                // Fail-closed: misconfigured gate blocks, it never silently passes.
                QualityCheckResult result = QualityCheckResult.fail("CRITICAL",
                        "Failed to parse rules JSON for gate: " + gate.getName(), GateDisposition.BLOCK);
                results.add(result);
                allPassed = false;
                overall = GateDisposition.mostSevere(overall, result.getDisposition());
                continue;
            }

            for (String ruleName : ruleNames) {
                QualityCheckResult result = runRule(executionId, context, ruleName, gate.getName(), results);
                allPassed &= result.isPassed();
                overall = GateDisposition.mostSevere(overall, result.getDisposition());
            }
        }

        QualityReport report = new QualityReport(executionId, allPassed, results, overall);
        log.info("Quality evaluation for execution {}: passed={}, disposition={}",
                executionId, allPassed, overall);
        return report;
    }

    /**
     * Runs one rule by name, applies the policy disposition to the result and
     * records an issue for failures. Never throws for rule-level problems —
     * unknown rules fail closed with a BLOCK disposition.
     */
    private QualityCheckResult runRule(Long executionId, QualityContext context, String ruleName,
                                       String gateName, List<QualityCheckResult> results) {
        QualityRule rule = rules.get(ruleName);
        if (rule == null) {
            log.warn("No rule implementation for: {}", ruleName);
            QualityCheckResult missing = QualityCheckResult.fail("CRITICAL",
                    "No rule implementation for: " + ruleName, UNKNOWN_RULE_DISPOSITION);
            missing.setRuleName(ruleName);
            results.add(missing);
            recordIssue(executionId, ruleName, missing);
            return missing;
        }

        QualityCheckResult result = rule.check(context);
        result.setRuleName(ruleName);
        if (!result.isPassed()) {
            result.setDisposition(resolveDisposition(ruleName, result));
            recordIssue(executionId, ruleName, result);
        } else if (result.getDisposition() == null) {
            result.setDisposition(GateDisposition.PASS);
        }
        results.add(result);
        return result;
    }

    /**
     * Resolves the disposition for a failed result: the policy disposition of
     * the rule (fail-closed BLOCK for unknown rules), escalated by the
     * CRITICAL severity floor. A rule that already produced a more severe
     * explicit disposition keeps it.
     */
    private GateDisposition resolveDisposition(String ruleName, QualityCheckResult result) {
        GateDisposition policy = DEFAULT_POLICY.getOrDefault(ruleName, UNKNOWN_RULE_DISPOSITION);
        GateDisposition floor = GateDisposition.severityFloor(result.getSeverity());
        return GateDisposition.mostSevere(policy,
                GateDisposition.mostSevere(floor, result.getDisposition()));
    }

    private void recordIssue(Long executionId, String ruleName, QualityCheckResult result) {
        SfQualityIssue issue = new SfQualityIssue();
        issue.setExecutionId(executionId);
        issue.setIssueType(ruleName);
        issue.setSeverity(result.getSeverity());
        issue.setDescription(result.getMessage());
        issue.setStatus(ISSUE_STATUS_OPEN);
        issueMapper.insert(issue);
    }

    public boolean checkQualityGate(Long executionId, String gateName) {
        log.info("Check quality gate for execution: {}, gate: {}", executionId, gateName);

        QualityContext context = new QualityContext(executionId, null, Map.of());
        QualityReport report = evaluate(executionId, context);
        return report.isAllPassed();
    }

    public void runQualityPipeline(Long executionId) {
        log.info("Run quality pipeline for execution: {}", executionId);
        QualityContext context = new QualityContext(executionId, null, Map.of());
        evaluate(executionId, context);
    }

    private List<String> parseRulesJson(String rulesJson) {
        if (rulesJson == null || rulesJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(rulesJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse rules JSON", e);
        }
    }
}
