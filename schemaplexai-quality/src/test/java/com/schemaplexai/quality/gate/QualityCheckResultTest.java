package com.schemaplexai.quality.gate;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QualityCheckResultTest {

    @Test
    void pass_returnsResultWithPassedTrueAndPassDisposition() {
        QualityCheckResult result = QualityCheckResult.pass();

        assertThat(result.isPassed()).isTrue();
        assertThat(result.getSeverity()).isNull();
        assertThat(result.getMessage()).isNull();
        assertThat(result.getDisposition()).isEqualTo(GateDisposition.PASS);
    }

    @Test
    void fail_returnsResultWithPassedFalse_andSeverityAndMessage() {
        QualityCheckResult result = QualityCheckResult.fail("CRITICAL", "Secret leak detected");

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getSeverity()).isEqualTo("CRITICAL");
        assertThat(result.getMessage()).isEqualTo("Secret leak detected");
    }

    @Test
    void fail_withCriticalSeverity_escalatesDispositionToBlock() {
        QualityCheckResult result = QualityCheckResult.fail("CRITICAL", "Secret leak detected");

        assertThat(result.getDisposition()).isEqualTo(GateDisposition.BLOCK);
    }

    @Test
    void fail_withHighSeverity_defaultsToWarnDisposition() {
        QualityCheckResult result = QualityCheckResult.fail("HIGH", "Unsafe code pattern");

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getSeverity()).isEqualTo("HIGH");
        assertThat(result.getDisposition()).isEqualTo(GateDisposition.WARN);
    }

    @Test
    void fail_withLowSeverity_defaultsToWarnDisposition() {
        QualityCheckResult result = QualityCheckResult.fail("LOW", "Minor style issue");

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getSeverity()).isEqualTo("LOW");
        assertThat(result.getDisposition()).isEqualTo(GateDisposition.WARN);
    }

    @Test
    void fail_withExplicitDisposition_keepsIt() {
        QualityCheckResult result = QualityCheckResult.fail("HIGH", "violations", GateDisposition.FAIL);

        assertThat(result.getDisposition()).isEqualTo(GateDisposition.FAIL);
    }

    @Test
    void allArgsConstructor_worksCorrectly() {
        QualityCheckResult result = new QualityCheckResult(false, "MEDIUM", "Some issue");

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getSeverity()).isEqualTo("MEDIUM");
        assertThat(result.getMessage()).isEqualTo("Some issue");
        assertThat(result.getDisposition()).isEqualTo(GateDisposition.WARN);
    }

    @Test
    void passedTrueLegacyConstructor_derivesPassDisposition() {
        QualityCheckResult result = new QualityCheckResult(true, null, null);

        assertThat(result.getDisposition()).isEqualTo(GateDisposition.PASS);
    }

    @Test
    void noArgsConstructor_createsDefaultObject() {
        QualityCheckResult result = new QualityCheckResult();

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getSeverity()).isNull();
        assertThat(result.getMessage()).isNull();
        assertThat(result.getDisposition()).isNull();
        assertThat(result.getRuleName()).isNull();
    }
}
