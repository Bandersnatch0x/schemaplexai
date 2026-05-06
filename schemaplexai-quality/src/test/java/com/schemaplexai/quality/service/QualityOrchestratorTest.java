package com.schemaplexai.quality.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.quality.entity.SfQualityGate;
import com.schemaplexai.quality.entity.SfQualityIssue;
import com.schemaplexai.quality.gate.QualityCheckResult;
import com.schemaplexai.quality.gate.QualityContext;
import com.schemaplexai.quality.gate.QualityReport;
import com.schemaplexai.quality.gate.QualityRule;
import com.schemaplexai.quality.mapper.QualityGateMapper;
import com.schemaplexai.quality.mapper.QualityIssueMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QualityOrchestratorTest {

    @Mock
    private QualityGateMapper gateMapper;

    @Mock
    private QualityIssueMapper issueMapper;

    @Mock
    private ObjectMapper objectMapper;

    private QualityRule passingRule;
    private QualityRule failingRule;

    @InjectMocks
    private QualityOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        passingRule = new QualityRule() {
            @Override
            public String getRuleName() {
                return "PASS_RULE";
            }

            @Override
            public QualityCheckResult check(QualityContext context) {
                return QualityCheckResult.pass();
            }
        };

        failingRule = new QualityRule() {
            @Override
            public String getRuleName() {
                return "FAIL_RULE";
            }

            @Override
            public QualityCheckResult check(QualityContext context) {
                return QualityCheckResult.fail("CRITICAL", "Test failure");
            }
        };

        orchestrator = new QualityOrchestrator(List.of(passingRule, failingRule),
                gateMapper, issueMapper, objectMapper);
        orchestrator.init();
    }

    @Test
    void evaluate_noGates_returnsReportWithNoResults() throws JsonProcessingException {
        when(gateMapper.selectList(any())).thenReturn(List.of());

        QualityContext context = new QualityContext(1L, null, Map.of());
        QualityReport report = orchestrator.evaluate(1L, context);

        assertThat(report.isAllPassed()).isTrue();
        assertThat(report.getResults()).isEmpty();
    }

    @Test
    void evaluate_passingRule_returnsAllPassed() throws JsonProcessingException {
        SfQualityGate gate = new SfQualityGate();
        gate.setName("Test Gate");
        gate.setRulesJson("[\"PASS_RULE\"]");
        when(gateMapper.selectList(any())).thenReturn(List.of(gate));
        when(objectMapper.readValue(eq("[\"PASS_RULE\"]"), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(List.of("PASS_RULE"));

        QualityContext context = new QualityContext(2L, null, Map.of());
        QualityReport report = orchestrator.evaluate(2L, context);

        assertThat(report.isAllPassed()).isTrue();
        assertThat(report.getResults()).hasSize(1);
        assertThat(report.getResults().get(0).isPassed()).isTrue();
        verify(issueMapper, never()).insert(any());
    }

    @Test
    void evaluate_failingRule_recordsIssue() throws JsonProcessingException {
        SfQualityGate gate = new SfQualityGate();
        gate.setName("Security Gate");
        gate.setRulesJson("[\"FAIL_RULE\"]");
        when(gateMapper.selectList(any())).thenReturn(List.of(gate));
        when(objectMapper.readValue(eq("[\"FAIL_RULE\"]"), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(List.of("FAIL_RULE"));

        QualityContext context = new QualityContext(3L, null, Map.of());
        QualityReport report = orchestrator.evaluate(3L, context);

        assertThat(report.isAllPassed()).isFalse();
        assertThat(report.getResults()).hasSize(1);
        assertThat(report.getResults().get(0).isPassed()).isFalse();

        ArgumentCaptor<SfQualityIssue> issueCaptor = ArgumentCaptor.forClass(SfQualityIssue.class);
        verify(issueMapper).insert(issueCaptor.capture());
        SfQualityIssue issue = issueCaptor.getValue();
        assertThat(issue.getExecutionId()).isEqualTo(3L);
        assertThat(issue.getIssueType()).isEqualTo("FAIL_RULE");
        assertThat(issue.getSeverity()).isEqualTo("CRITICAL");
        assertThat(issue.getStatus()).isEqualTo(0);
    }

    @Test
    void evaluate_unknownRule_warnsAndSkips() throws JsonProcessingException {
        SfQualityGate gate = new SfQualityGate();
        gate.setName("Test Gate");
        gate.setRulesJson("[\"UNKNOWN_RULE\"]");
        when(gateMapper.selectList(any())).thenReturn(List.of(gate));
        when(objectMapper.readValue(eq("[\"UNKNOWN_RULE\"]"), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(List.of("UNKNOWN_RULE"));

        QualityContext context = new QualityContext(4L, null, Map.of());
        QualityReport report = orchestrator.evaluate(4L, context);

        assertThat(report.isAllPassed()).isTrue();
        assertThat(report.getResults()).isEmpty();
        verify(issueMapper, never()).insert(any());
    }

    @Test
    void evaluate_nullRulesJson_skipsParsing() throws JsonProcessingException {
        SfQualityGate gate = new SfQualityGate();
        gate.setName("Empty Gate");
        gate.setRulesJson(null);
        when(gateMapper.selectList(any())).thenReturn(List.of(gate));

        QualityContext context = new QualityContext(5L, null, Map.of());
        QualityReport report = orchestrator.evaluate(5L, context);

        assertThat(report.isAllPassed()).isTrue();
        assertThat(report.getResults()).isEmpty();
        verify(objectMapper, never()).readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class));
    }

    @Test
    void evaluate_blankRulesJson_skipsParsing() throws JsonProcessingException {
        SfQualityGate gate = new SfQualityGate();
        gate.setName("Blank Gate");
        gate.setRulesJson("   ");
        when(gateMapper.selectList(any())).thenReturn(List.of(gate));

        QualityContext context = new QualityContext(6L, null, Map.of());
        QualityReport report = orchestrator.evaluate(6L, context);

        assertThat(report.isAllPassed()).isTrue();
        assertThat(report.getResults()).isEmpty();
    }

    @Test
    void evaluate_parseError_handlesGracefully() throws JsonProcessingException {
        SfQualityGate gate = new SfQualityGate();
        gate.setName("Bad Gate");
        gate.setRulesJson("invalid-json");
        when(gateMapper.selectList(any())).thenReturn(List.of(gate));
        when(objectMapper.readValue(eq("invalid-json"), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenThrow(new RuntimeException("Parse error"));

        QualityContext context = new QualityContext(7L, null, Map.of());
        QualityReport report = orchestrator.evaluate(7L, context);

        assertThat(report.isAllPassed()).isTrue();
        assertThat(report.getResults()).isEmpty();
    }

    @Test
    void checkQualityGate_delegatesToEvaluate() throws JsonProcessingException {
        when(gateMapper.selectList(any())).thenReturn(List.of());

        boolean result = orchestrator.checkQualityGate(10L, "test-gate");

        assertThat(result).isTrue();
    }

    @Test
    void runQualityPipeline_delegatesToEvaluate() throws JsonProcessingException {
        when(gateMapper.selectList(any())).thenReturn(List.of());

        orchestrator.runQualityPipeline(20L);

        verify(gateMapper).selectList(any());
    }

    @Test
    void evaluate_mixedRules_allPassingWins() throws JsonProcessingException {
        // Only the passing rule is configured in the gate
        SfQualityGate gate = new SfQualityGate();
        gate.setName("Mixed Gate");
        gate.setRulesJson("[\"PASS_RULE\"]");
        when(gateMapper.selectList(any())).thenReturn(List.of(gate));
        when(objectMapper.readValue(eq("[\"PASS_RULE\"]"), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(List.of("PASS_RULE"));

        QualityContext context = new QualityContext(8L, null, Map.of());
        QualityReport report = orchestrator.evaluate(8L, context);

        assertThat(report.isAllPassed()).isTrue();
        verify(issueMapper, never()).insert(any());
        // FAIL_RULE exists in ruleList but is NOT in the gate's rulesJson, so it should NOT be called
    }
}
