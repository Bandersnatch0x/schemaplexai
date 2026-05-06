package com.schemaplexai.quality.gate.rules;

import com.schemaplexai.quality.gate.QualityCheckResult;
import com.schemaplexai.quality.gate.QualityContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SpecComplianceRuleTest {

    private final SpecComplianceRule rule = new SpecComplianceRule();

    @Test
    void getRuleName_returnsSPEC_COMPLIANCE() {
        assertThat(rule.getRuleName()).isEqualTo("SPEC_COMPLIANCE");
    }

    @Test
    void check_withSpecId_passes() {
        QualityContext context = new QualityContext();
        context.setExecutionId(1L);
        context.setMetadata(Map.of("specId", 100L));

        QualityCheckResult result = rule.check(context);

        assertThat(result.isPassed()).isTrue();
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
