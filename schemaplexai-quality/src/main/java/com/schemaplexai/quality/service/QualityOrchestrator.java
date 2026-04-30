package com.schemaplexai.quality.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.quality.entity.SfQualityGate;
import com.schemaplexai.quality.entity.SfQualityIssue;
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
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class QualityOrchestrator {

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
        List<SfQualityGate> gates = gateMapper.selectList(null);
        List<QualityCheckResult> results = new ArrayList<>();
        boolean allPassed = true;

        for (SfQualityGate gate : gates) {
            List<String> ruleNames = parseRulesJson(gate.getRulesJson());
            for (String ruleName : ruleNames) {
                QualityRule rule = rules.get(ruleName);
                if (rule == null) {
                    log.warn("No rule implementation for: {}", ruleName);
                    continue;
                }

                QualityCheckResult result = rule.check(context);
                results.add(result);

                if (!result.isPassed()) {
                    allPassed = false;
                    SfQualityIssue issue = new SfQualityIssue();
                    issue.setExecutionId(executionId);
                    issue.setIssueType(ruleName);
                    issue.setSeverity(result.getSeverity());
                    issue.setDescription(result.getMessage());
                    issue.setStatus(0); // open
                    issueMapper.insert(issue);
                }
            }
        }

        QualityReport report = new QualityReport(executionId, allPassed, results);
        log.info("Quality evaluation for execution {}: passed={}", executionId, allPassed);
        return report;
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
            log.warn("Failed to parse rules JSON: {}", rulesJson, e);
            return List.of();
        }
    }
}
