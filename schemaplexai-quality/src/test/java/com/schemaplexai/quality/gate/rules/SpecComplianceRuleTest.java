package com.schemaplexai.quality.gate.rules;

import com.schemaplexai.quality.gate.QualityCheckResult;
import com.schemaplexai.quality.gate.QualityContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SpecComplianceRuleTest {

    private final SpecComplianceRule rule = new SpecComplianceRule();

    @Test
    void getRuleName_returnsSPEC_COMPLIANCE() {
        assertThat(rule.getRuleName()).isEqualTo("SPEC_COMPLIANCE");
    }

    @Test
    void check_withSpecIdButNoComplianceEvidence_failsInsteadOfPlaceholderPass() {
        QualityContext context = new QualityContext();
        context.setExecutionId(1L);
        context.setMetadata(Map.of("specId", 100L));

        QualityCheckResult result = rule.check(context);

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getSeverity()).isEqualTo("MEDIUM");
        assertThat(result.getMessage()).contains("compliance");
    }

    @Test
    void check_withCompletedPassingComplianceEvidence_passes() {
        QualityContext context = new QualityContext();
        context.setExecutionId(5L);
        context.setMetadata(Map.of(
                "specId", 100L,
                "specComplianceChecked", true,
                "specCompliancePassed", true,
                "specComplianceViolations", List.of()
        ));

        QualityCheckResult result = rule.check(context);

        assertThat(result.isPassed()).isTrue();
    }

    @Test
    void check_withComplianceViolationsList_fails() {
        QualityContext context = new QualityContext();
        context.setExecutionId(6L);
        context.setMetadata(Map.of(
                "specId", 100L,
                "specComplianceChecked", true,
                "specCompliancePassed", true,
                "specComplianceViolations", List.of("Missing acceptance criteria")
        ));

        QualityCheckResult result = rule.check(context);

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getSeverity()).isEqualTo("HIGH");
        assertThat(result.getMessage()).contains("violations");
    }

    @Test
    void check_withComplianceViolationsArray_fails() {
        QualityContext context = new QualityContext();
        context.setExecutionId(7L);
        context.setMetadata(Map.of(
                "specId", 100L,
                "specComplianceChecked", true,
                "specCompliancePassed", true,
                "specComplianceViolations", new String[]{"Output missing required field"}
        ));

        QualityCheckResult result = rule.check(context);

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getSeverity()).isEqualTo("HIGH");
    }

    @Test
    void check_withComplianceViolationsString_fails() {
        QualityContext context = new QualityContext();
        context.setExecutionId(8L);
        context.setMetadata(Map.of(
                "specId", 100L,
                "specComplianceChecked", true,
                "specCompliancePassed", true,
                "specComplianceViolations", "Output contradicts required workflow"
        ));

        QualityCheckResult result = rule.check(context);

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getSeverity()).isEqualTo("HIGH");
    }

    @Test
    void check_withCompletedNonCompliantEvidence_fails() {
        QualityContext context = new QualityContext();
        context.setExecutionId(9L);
        context.setMetadata(Map.of(
                "specId", 100L,
                "specComplianceChecked", true,
                "specCompliancePassed", false
        ));

        QualityCheckResult result = rule.check(context);

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getSeverity()).isEqualTo("HIGH");
        assertThat(result.getMessage()).contains("not compliant");
    }

    @Test
    void check_withoutMetadata_failsWithMediumSeverity() {
        QualityContext context = new QualityContext();
        context.setExecutionId(2L);
        context.setMetadata(null);

        QualityCheckResult result = rule.check(context);

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getSeverity()).isEqualTo("MEDIUM");
        assertThat(result.getMessage()).contains("spec");
    }

    @Test
    void check_withMetadataButNoSpecId_failsWithMediumSeverity() {
        QualityContext context = new QualityContext();
        context.setExecutionId(3L);
        context.setMetadata(Map.of("otherKey", "value"));

        QualityCheckResult result = rule.check(context);

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getSeverity()).isEqualTo("MEDIUM");
    }

    @Test
    void check_withEmptyMetadata_failsWithMediumSeverity() {
        QualityContext context = new QualityContext();
        context.setExecutionId(4L);
        context.setMetadata(Map.of());

        QualityCheckResult result = rule.check(context);

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getSeverity()).isEqualTo("MEDIUM");
    }
}
