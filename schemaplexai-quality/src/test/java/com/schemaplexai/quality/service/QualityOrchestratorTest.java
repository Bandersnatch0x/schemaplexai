package com.schemaplexai.quality.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.quality.entity.SfQualityGate;
import com.schemaplexai.quality.entity.SfQualityIssue;
import com.schemaplexai.quality.gate.GateDisposition;
import com.schemaplexai.quality.gate.QualityCheckResult;
import com.schemaplexai.quality.gate.QualityContext;
import com.schemaplexai.quality.gate.QualityReport;
import com.schemaplexai.quality.gate.QualityRule;
import com.schemaplexai.quality.gate.rules.SecurityScanRule;
import com.schemaplexai.quality.gate.rules.SpecComplianceRule;
import com.schemaplexai.quality.mapper.QualityGateMapper;
import com.schemaplexai.quality.mapper.QualityIssueMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    private QualityOrchestrator orchestrator;

    @BeforeAll
    static void initTableInfo() {
        // Enables LambdaQueryWrapper column resolution in pure unit tests
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), SfQualityGate.class);
    }

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

    /**
     * Ticket 924 / REQ-15: an empty gate table must not pass vacuously. The
     * built-in default policy (spec §4.2) applies; with no rule implementations
     * registered its three rules fail closed and the verdict is BLOCK.
     */
    @Test
    void evaluate_noGates_appliesBuiltInDefaultPolicyFailClosed() {
        when(gateMapper.selectList(any())).thenReturn(List.of());

        QualityContext context = new QualityContext(1L, null, Map.of());
        QualityReport report = orchestrator.evaluate(1L, context);

        assertThat(report.isAllPassed()).isFalse();
        assertThat(report.getDisposition()).isEqualTo(GateDisposition.BLOCK);
        assertThat(report.getResults()).hasSize(QualityOrchestrator.DEFAULT_POLICY.size());
        assertThat(report.getResults())
                .allSatisfy(result -> {
                    assertThat(result.isPassed()).isFalse();
                    assertThat(result.getDisposition()).isEqualTo(GateDisposition.BLOCK);
                });
        assertThat(report.getResults()).extracting(QualityCheckResult::getRuleName)
                .containsExactlyInAnyOrderElementsOf(QualityOrchestrator.DEFAULT_POLICY.keySet());
        // every fail-closed default-policy failure is recorded as an issue
        verify(issueMapper, times(QualityOrchestrator.DEFAULT_POLICY.size())).insert(any());
    }

    /**
     * Ticket 924 / REQ-15 with the real default rules registered: empty table
     * evaluates SECURITY_SCAN (BLOCK policy) and SPEC_COMPLIANCE (WARN policy)
     * against the context; OUTPUT_FORMAT has no implementation yet (phased,
     * REQ-12) and fails closed to BLOCK.
     */
    @Test
    void evaluate_noGatesWithRealRules_appliesDefaultPolicyDispositions() {
        orchestrator = new QualityOrchestrator(List.of(new SecurityScanRule(), new SpecComplianceRule()),
                gateMapper, issueMapper, objectMapper);
        orchestrator.init();
        when(gateMapper.selectList(any())).thenReturn(List.of());

        QualityReport report = orchestrator.evaluate(11L, new QualityContext(11L, null, Map.of()));

        assertThat(report.isAllPassed()).isFalse();
        assertThat(report.getDisposition()).isEqualTo(GateDisposition.BLOCK);
        Map<String, GateDisposition> byRule = new HashMap<>();
        report.getResults().forEach(r -> byRule.put(r.getRuleName(), r.getDisposition()));
        assertThat(byRule).containsEntry("SECURITY_SCAN", GateDisposition.BLOCK);
        assertThat(byRule).containsEntry("SPEC_COMPLIANCE", GateDisposition.WARN);
        assertThat(byRule).containsEntry("OUTPUT_FORMAT", GateDisposition.BLOCK);
    }

    /**
     * REQ-15 fail-closed content detection still applies on the default policy:
     * a secret pattern in the output escalates to a BLOCK verdict.
     */
    @Test
    void evaluate_noGatesWithSecretInOutput_blocks() {
        orchestrator = new QualityOrchestrator(List.of(new SecurityScanRule(), new SpecComplianceRule()),
                gateMapper, issueMapper, objectMapper);
        orchestrator.init();
        when(gateMapper.selectList(any())).thenReturn(List.of());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("output", "config: password=hunter2");
        QualityReport report = orchestrator.evaluate(12L, new QualityContext(12L, null, metadata));

        assertThat(report.isAllPassed()).isFalse();
        assertThat(report.getDisposition()).isEqualTo(GateDisposition.BLOCK);
        assertThat(report.getResults()).extracting(QualityCheckResult::getRuleName)
                .contains("SECURITY_SCAN");
        QualityCheckResult security = report.getResults().stream()
                .filter(r -> "SECURITY_SCAN".equals(r.getRuleName())).findFirst().orElseThrow();
        assertThat(security.getSeverity()).isEqualTo("CRITICAL");
        assertThat(severityOfIssue("SECURITY_SCAN")).isEqualTo("CRITICAL");
    }

    @Test
    void evaluate_passingRule_returnsAllPassedWithPassDisposition() throws JsonProcessingException {
        SfQualityGate gate = new SfQualityGate();
        gate.setName("Test Gate");
        gate.setRulesJson("[\"PASS_RULE\"]");
        when(gateMapper.selectList(any())).thenReturn(List.of(gate));
        when(objectMapper.readValue(eq("[\"PASS_RULE\"]"), any(TypeReference.class)))
                .thenReturn(List.of("PASS_RULE"));

        QualityContext context = new QualityContext(2L, null, Map.of());
        QualityReport report = orchestrator.evaluate(2L, context);

        assertThat(report.isAllPassed()).isTrue();
        assertThat(report.getDisposition()).isEqualTo(GateDisposition.PASS);
        assertThat(report.getResults()).hasSize(1);
        assertThat(report.getResults().get(0).isPassed()).isTrue();
        assertThat(report.getResults().get(0).getRuleName()).isEqualTo("PASS_RULE");
        verify(issueMapper, never()).insert(any());
    }

    @Test
    void evaluate_failingRule_recordsIssueWithBlockDisposition() throws JsonProcessingException {
        SfQualityGate gate = new SfQualityGate();
        gate.setName("Security Gate");
        gate.setRulesJson("[\"FAIL_RULE\"]");
        when(gateMapper.selectList(any())).thenReturn(List.of(gate));
        when(objectMapper.readValue(eq("[\"FAIL_RULE\"]"), any(TypeReference.class)))
                .thenReturn(List.of("FAIL_RULE"));

        QualityContext context = new QualityContext(3L, null, Map.of());
        QualityReport report = orchestrator.evaluate(3L, context);

        assertThat(report.isAllPassed()).isFalse();
        // FAIL_RULE has no policy entry -> fail-closed BLOCK
        assertThat(report.getDisposition()).isEqualTo(GateDisposition.BLOCK);
        assertThat(report.getResults()).hasSize(1);
        assertThat(report.getResults().get(0).isPassed()).isFalse();
        assertThat(report.getResults().get(0).getDisposition()).isEqualTo(GateDisposition.BLOCK);

        ArgumentCaptor<SfQualityIssue> issueCaptor = ArgumentCaptor.forClass(SfQualityIssue.class);
        verify(issueMapper).insert(issueCaptor.capture());
        SfQualityIssue issue = issueCaptor.getValue();
        assertThat(issue.getExecutionId()).isEqualTo(3L);
        assertThat(issue.getIssueType()).isEqualTo("FAIL_RULE");
        assertThat(issue.getSeverity()).isEqualTo("CRITICAL");
        assertThat(issue.getStatus()).isEqualTo("OPEN");
    }

    /**
     * Ticket 924 / REQ-02: the gate policy maps SPEC_COMPLIANCE failures to
     * WARN (alert, continue) even when the rule reports HIGH severity.
     */
    @Test
    void evaluate_specComplianceFailure_mapsToWarnDisposition() throws JsonProcessingException {
        orchestrator = new QualityOrchestrator(List.of(new SpecComplianceRule()),
                gateMapper, issueMapper, objectMapper);
        orchestrator.init();

        SfQualityGate gate = new SfQualityGate();
        gate.setName("Spec Gate");
        gate.setRulesJson("[\"SPEC_COMPLIANCE\"]");
        when(gateMapper.selectList(any())).thenReturn(List.of(gate));
        when(objectMapper.readValue(eq("[\"SPEC_COMPLIANCE\"]"), any(TypeReference.class)))
                .thenReturn(List.of("SPEC_COMPLIANCE"));

        // specId present but compliance evidence negative -> HIGH failure
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("specId", 5L);
        metadata.put("specCompliancePassed", Boolean.FALSE);
        QualityReport report = orchestrator.evaluate(13L, new QualityContext(13L, 5L, metadata));

        assertThat(report.isAllPassed()).isFalse();
        assertThat(report.getDisposition()).isEqualTo(GateDisposition.WARN);
        assertThat(report.getResults().get(0).getSeverity()).isEqualTo("HIGH");
        assertThat(report.getResults().get(0).getDisposition()).isEqualTo(GateDisposition.WARN);
    }

    @Test
    void evaluate_loadsOnlyActiveGates() {
        when(gateMapper.selectList(any())).thenReturn(List.of());

        QualityContext context = new QualityContext(9L, null, Map.of());
        orchestrator.evaluate(9L, context);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<SfQualityGate>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(gateMapper).selectList(captor.capture());
        LambdaQueryWrapper<SfQualityGate> wrapper = captor.getValue();
        assertThat(wrapper.getSqlSegment()).contains("status");
        assertThat(wrapper.getParamNameValuePairs().values())
                .containsExactly("ACTIVE");
    }

    @Test
    void evaluate_unknownRule_returnsFailedReportWithBlockDisposition() throws JsonProcessingException {
        SfQualityGate gate = new SfQualityGate();
        gate.setName("Test Gate");
        gate.setRulesJson("[\"UNKNOWN_RULE\"]");
        when(gateMapper.selectList(any())).thenReturn(List.of(gate));
        when(objectMapper.readValue(eq("[\"UNKNOWN_RULE\"]"), any(TypeReference.class)))
                .thenReturn(List.of("UNKNOWN_RULE"));

        QualityContext context = new QualityContext(4L, null, Map.of());
        QualityReport report = orchestrator.evaluate(4L, context);

        assertThat(report.isAllPassed()).isFalse();
        assertThat(report.getDisposition()).isEqualTo(GateDisposition.BLOCK);
        assertThat(report.getResults()).hasSize(1);
        assertThat(report.getResults().get(0).isPassed()).isFalse();
        assertThat(report.getResults().get(0).getSeverity()).isEqualTo("CRITICAL");
        assertThat(report.getResults().get(0).getMessage()).contains("UNKNOWN_RULE");
        assertThat(report.getResults().get(0).getRuleName()).isEqualTo("UNKNOWN_RULE");
        // fail-closed unknown rules are recorded as issues too
        verify(issueMapper).insert(any());
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
        assertThat(report.getDisposition()).isEqualTo(GateDisposition.PASS);
        assertThat(report.getResults()).isEmpty();
        verify(objectMapper, never()).readValue(anyString(), any(TypeReference.class));
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
    void evaluate_parseError_returnsFailedReportWithBlockDisposition() throws JsonProcessingException {
        SfQualityGate gate = new SfQualityGate();
        gate.setName("Bad Gate");
        gate.setRulesJson("invalid-json");
        when(gateMapper.selectList(any())).thenReturn(List.of(gate));
        when(objectMapper.readValue(eq("invalid-json"), any(TypeReference.class)))
                .thenThrow(new RuntimeException("Parse error"));

        QualityContext context = new QualityContext(7L, null, Map.of());
        QualityReport report = orchestrator.evaluate(7L, context);

        assertThat(report.isAllPassed()).isFalse();
        assertThat(report.getDisposition()).isEqualTo(GateDisposition.BLOCK);
        assertThat(report.getResults()).hasSize(1);
        assertThat(report.getResults().get(0).isPassed()).isFalse();
        assertThat(report.getResults().get(0).getSeverity()).isEqualTo("CRITICAL");
        assertThat(report.getResults().get(0).getMessage()).contains("Bad Gate");
    }

    @Test
    void checkQualityGate_delegatesToEvaluate() {
        when(gateMapper.selectList(any())).thenReturn(List.of());

        // Empty table applies the built-in default policy, whose rules have no
        // implementations registered here -> fail-closed -> gate does not pass.
        boolean result = orchestrator.checkQualityGate(10L, "test-gate");

        assertThat(result).isFalse();
    }

    @Test
    void runQualityPipeline_delegatesToEvaluate() {
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
        when(objectMapper.readValue(eq("[\"PASS_RULE\"]"), any(TypeReference.class)))
                .thenReturn(List.of("PASS_RULE"));

        QualityContext context = new QualityContext(8L, null, Map.of());
        QualityReport report = orchestrator.evaluate(8L, context);

        assertThat(report.isAllPassed()).isTrue();
        assertThat(report.getDisposition()).isEqualTo(GateDisposition.PASS);
        verify(issueMapper, never()).insert(any());
        // FAIL_RULE exists in ruleList but is NOT in the gate's rulesJson, so it should NOT be called
    }

    private String severityOfIssue(String issueType) {
        ArgumentCaptor<SfQualityIssue> captor = ArgumentCaptor.forClass(SfQualityIssue.class);
        verify(issueMapper, org.mockito.Mockito.atLeastOnce()).insert(captor.capture());
        return captor.getAllValues().stream()
                .filter(issue -> issueType.equals(issue.getIssueType()))
                .map(SfQualityIssue::getSeverity)
                .findFirst()
                .orElseThrow();
    }
}
