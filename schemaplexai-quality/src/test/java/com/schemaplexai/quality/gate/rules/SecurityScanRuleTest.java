package com.schemaplexai.quality.gate.rules;

import com.schemaplexai.quality.gate.QualityCheckResult;
import com.schemaplexai.quality.gate.QualityContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityScanRuleTest {

    private final SecurityScanRule rule = new SecurityScanRule();

    @Test
    void getRuleName_returnsSECURITY_SCAN() {
        assertThat(rule.getRuleName()).isEqualTo("SECURITY_SCAN");
    }

    @Test
    void check_passesWhenCompletedScanHasNoFindings() {
        QualityContext context = new QualityContext();
        context.setExecutionId(1L);
        context.setMetadata(Map.of(
                "securityScanCompleted", true,
                "securityScanFindings", List.of(),
                "output", "This is a safe response with no secrets."
        ));

        QualityCheckResult result = rule.check(context);

        assertThat(result.isPassed()).isTrue();
    }

    @Test
    void check_failsWhenMetadataIsNull() {
        QualityContext context = new QualityContext();
        context.setExecutionId(1L);
        context.setMetadata(null);

        QualityCheckResult result = rule.check(context);

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getMessage()).contains("security scan evidence");
    }

    @Test
    void check_withoutSecurityScanEvidence_failsInsteadOfPlaceholderPass() {
        QualityContext context = new QualityContext();
        context.setExecutionId(1L);
        context.setMetadata(Map.of("output", "This is a safe response with no secrets."));

        QualityCheckResult result = rule.check(context);

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getMessage()).contains("security scan evidence");
    }

    @Test
    void check_failsWhenMetadataHasNoSecurityScanEvidence() {
        QualityContext context = new QualityContext();
        context.setExecutionId(1L);
        context.setMetadata(Map.of());

        QualityCheckResult result = rule.check(context);

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getMessage()).contains("security scan evidence");
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
    void check_failsWhenCompletedScanReportsFindings() {
        QualityContext context = new QualityContext();
        context.setExecutionId(5L);
        context.setMetadata(Map.of(
                "securityScanCompleted", true,
                "securityScanFindings", List.of("CVE-2026-0001")
        ));

        QualityCheckResult result = rule.check(context);

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getSeverity()).isEqualTo("CRITICAL");
        assertThat(result.getMessage()).contains("findings");
    }

    @Test
    void check_withExplicitFailedScanEvidence_reportsScanFailure() {
        QualityContext context = new QualityContext();
        context.setExecutionId(7L);
        context.setMetadata(Map.of("securityScanPassed", false));

        QualityCheckResult result = rule.check(context);

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getSeverity()).isEqualTo("HIGH");
        assertThat(result.getMessage()).contains("failed");
    }

    @Test
    void check_outputWithSimilarButSafeWords_passes() {
        QualityContext context = new QualityContext();
        context.setExecutionId(6L);
        context.setMetadata(Map.of(
                "securityScanPassed", "true",
                "output", "The password reset feature is available."
        ));

        QualityCheckResult result = rule.check(context);

        assertThat(result.isPassed()).isTrue();
    }
}
