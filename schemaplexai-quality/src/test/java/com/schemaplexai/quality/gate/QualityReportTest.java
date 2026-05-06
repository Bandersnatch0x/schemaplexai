package com.schemaplexai.quality.gate;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QualityReportTest {

    @Test
    void allArgsConstructor_worksCorrectly() {
        List<QualityCheckResult> results = List.of(QualityCheckResult.pass());
        QualityReport report = new QualityReport(1L, true, results);

        assertThat(report.getExecutionId()).isEqualTo(1L);
        assertThat(report.isAllPassed()).isTrue();
        assertThat(report.getResults()).hasSize(1);
    }

    @Test
    void noArgsConstructor_createsDefaultObject() {
        QualityReport report = new QualityReport();

        assertThat(report.getExecutionId()).isNull();
        assertThat(report.isAllPassed()).isFalse();
        assertThat(report.getResults()).isNull();
    }

    @Test
    void withFailedResults_allPassedIsFalse() {
        List<QualityCheckResult> results = List.of(
                QualityCheckResult.pass(),
                QualityCheckResult.fail("CRITICAL", "Secret leak")
        );
        QualityReport report = new QualityReport(2L, false, results);

        assertThat(report.isAllPassed()).isFalse();
        assertThat(report.getResults()).hasSize(2);
    }

    @Test
    void withAllPassingResults_allPassedIsTrue() {
        List<QualityCheckResult> results = List.of(
                QualityCheckResult.pass(),
                QualityCheckResult.pass()
        );
        QualityReport report = new QualityReport(3L, true, results);

        assertThat(report.isAllPassed()).isTrue();
        assertThat(report.getResults()).hasSize(2);
    }
}
