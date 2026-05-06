package com.schemaplexai.quality.gate;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QualityCheckResultTest {

    @Test
    void pass_returnsResultWithPassedTrue() {
        QualityCheckResult result = QualityCheckResult.pass();

        assertThat(result.isPassed()).isTrue();
        assertThat(result.getSeverity()).isNull();
        assertThat(result.getMessage()).isNull();
    }

    @Test
    void fail_returnsResultWithPassedFalse_andSeverityAndMessage() {
        QualityCheckResult result = QualityCheckResult.fail("CRITICAL", "Secret leak detected");

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getSeverity()).isEqualTo("CRITICAL");
        assertThat(result.getMessage()).isEqualTo("Secret leak detected");
    }

    @Test
    void fail_withHighSeverity() {
        QualityCheckResult result = QualityCheckResult.fail("HIGH", "Unsafe code pattern");

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getSeverity()).isEqualTo("HIGH");
    }

    @Test
    void fail_withLowSeverity() {
        QualityCheckResult result = QualityCheckResult.fail("LOW", "Minor style issue");

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getSeverity()).isEqualTo("LOW");
    }

    @Test
    void allArgsConstructor_worksCorrectly() {
        QualityCheckResult result = new QualityCheckResult(false, "MEDIUM", "Some issue");

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getSeverity()).isEqualTo("MEDIUM");
        assertThat(result.getMessage()).isEqualTo("Some issue");
    }

    @Test
    void noArgsConstructor_createsDefaultObject() {
        QualityCheckResult result = new QualityCheckResult();

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getSeverity()).isNull();
        assertThat(result.getMessage()).isNull();
    }
}
