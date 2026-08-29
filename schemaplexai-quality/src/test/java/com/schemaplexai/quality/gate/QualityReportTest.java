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
        assertThat(report.getDisposition()).isEqualTo(GateDisposition.PASS);
    }

    @Test
    void noArgsConstructor_createsDefaultObject() {
        QualityReport report = new QualityReport();

        assertThat(report.getExecutionId()).isNull();
        assertThat(report.isAllPassed()).isFalse();
        assertThat(report.getResults()).isNull();
        assertThat(report.getDisposition()).isNull();
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
        assertThat(report.getDisposition()).isEqualTo(GateDisposition.PASS);
    }

    /**
     * Ticket 924 / REQ-02: the report disposition is the structured verdict —
     * the most severe disposition across all results.
     */
    @Test
    void dispositionIsMostSevereAcrossResults() {
        List<QualityCheckResult> results = List.of(
                QualityCheckResult.pass(),
                QualityCheckResult.fail("MEDIUM", "warn-level issue", GateDisposition.WARN),
                QualityCheckResult.fail("CRITICAL", "block-level issue", GateDisposition.BLOCK)
        );
        QualityReport report = new QualityReport(4L, false, results);

        assertThat(report.getDisposition()).isEqualTo(GateDisposition.BLOCK);
    }

    @Test
    void failDispositionEscalatesToBlockForCriticalSeverity() {
        // legacy 3-arg constructor: CRITICAL failure derives BLOCK disposition
        List<QualityCheckResult> results = List.of(QualityCheckResult.fail("CRITICAL", "leak"));
        QualityReport report = new QualityReport(5L, false, results);

        assertThat(report.getDisposition()).isEqualTo(GateDisposition.BLOCK);
    }

    @Test
    void explicitConstructorKeepsGivenDisposition() {
        QualityReport report = new QualityReport(6L, false,
                List.of(QualityCheckResult.fail("LOW", "minor", GateDisposition.FAIL)),
                GateDisposition.FAIL);

        assertThat(report.getDisposition()).isEqualTo(GateDisposition.FAIL);
    }

    @Test
    void nullResultsDerivePassDisposition() {
        QualityReport report = new QualityReport(7L, true, null);

        assertThat(report.getDisposition()).isEqualTo(GateDisposition.PASS);
    }
}
