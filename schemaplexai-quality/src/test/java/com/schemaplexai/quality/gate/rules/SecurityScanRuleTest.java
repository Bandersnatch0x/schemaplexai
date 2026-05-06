package com.schemaplexai.quality.gate.rules;

import com.schemaplexai.quality.gate.QualityCheckResult;
import com.schemaplexai.quality.gate.QualityContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityScanRuleTest {

    private final SecurityScanRule rule = new SecurityScanRule();

    @Test
    void getRuleName_returnsSECURITY_SCAN() {
        assertThat(rule.getRuleName()).isEqualTo("SECURITY_SCAN");
    }

    @Test
    void check_passesWhenNoSensitivePatterns() {
        QualityContext context = new QualityContext();
        context.setExecutionId(1L);
        context.setMetadata(Map.of("output", "This is a safe response with no secrets."));

        QualityCheckResult result = rule.check(context);

        assertThat(result.isPassed()).isTrue();
    }

    @Test
    void check_passesWhenOutputIsNull() {
        QualityContext context = new QualityContext();
        context.setExecutionId(1L);
        context.setMetadata(null);

        QualityCheckResult result = rule.check(context);

        assertThat(result.isPassed()).isTrue();
    }

    @Test
    void check_passesWhenMetadataIsNull() {
        QualityContext context = new QualityContext();
        context.setExecutionId(1L);
        context.setMetadata(Map.of());

        QualityCheckResult result = rule.check(context);

        assertThat(result.isPassed()).isTrue();
    }

    @Test
    void check_detectsPasswordInOutput() {
        QualityContext context = new QualityContext();
        context.setExecutionId(2L);
        context.setMetadata(Map.of("output", "Here is your password=admin123 for the system."));

        QualityCheckResult result = rule.check(context);

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getSeverity()).isEqualTo("CRITICAL");
        assertThat(result.getMessage()).contains("secret leak");
    }

    @Test
    void check_detectsSecretInOutput() {
        QualityContext context = new QualityContext();
        context.setExecutionId(3L);
        context.setMetadata(Map.of("output", "The API secret=sk-abc123xyz should not be exposed."));

        QualityCheckResult result = rule.check(context);

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getSeverity()).isEqualTo("CRITICAL");
    }

    @Test
    void check_emptyOutput_passes() {
        QualityContext context = new QualityContext();
        context.setExecutionId(5L);
        context.setMetadata(Map.of("output", ""));

        QualityCheckResult result = rule.check(context);

        assertThat(result.isPassed()).isTrue();
    }

    @Test
    void check_outputWithSimilarButSafeWords_passes() {
        QualityContext context = new QualityContext();
        context.setExecutionId(6L);
        context.setMetadata(Map.of("output", "The password reset feature is available."));

        QualityCheckResult result = rule.check(context);

        assertThat(result.isPassed()).isTrue();
    }
}
