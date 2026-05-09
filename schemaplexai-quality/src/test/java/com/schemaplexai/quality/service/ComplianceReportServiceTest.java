package com.schemaplexai.quality.service;

import com.schemaplexai.quality.entity.SfAuditEvent;
import com.schemaplexai.quality.mapper.AuditEventMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("M5.1: Compliance Report Service Tests")
class ComplianceReportServiceTest {

    @Mock
    private AuditEventMapper auditEventMapper;

    @InjectMocks
    private ComplianceReportService complianceReportService;

    @Test
    @DisplayName("Builds execution report with event count")
    void buildsExecutionReport() {
        SfAuditEvent e1 = new SfAuditEvent();
        e1.setExecutionId(1L);
        e1.setEventType("TOOL_CALLED");
        when(auditEventMapper.selectList(any())).thenReturn(List.of(e1));

        Map<String, Object> report = complianceReportService.buildExecutionReport(1L);

        assertThat(report).containsEntry("executionId", 1L);
        assertThat(report).containsEntry("eventCount", 1);
        assertThat(report).containsKey("events");
    }

    @Test
    @DisplayName("Counts corrupted events in report")
    void countsCorruptedEvents() {
        SfAuditEvent good = new SfAuditEvent();
        good.setCorrupted(false);
        SfAuditEvent bad = new SfAuditEvent();
        bad.setCorrupted(true);
        when(auditEventMapper.selectList(any())).thenReturn(List.of(good, bad));

        Map<String, Object> report = complianceReportService.buildExecutionReport(1L);

        assertThat(report).containsEntry("corruptedCount", 1L);
    }

    @Test
    @DisplayName("Dashboard returns 24h metrics")
    void dashboardReturnsMetrics() {
        when(auditEventMapper.selectRecentEvents(any())).thenReturn(Collections.emptyList());

        Map<String, Object> dashboard = complianceReportService.buildDashboard();

        assertThat(dashboard).containsEntry("last24hEventCount", 0);
        assertThat(dashboard).containsKey("generatedAt");
    }

    @Test
    @DisplayName("Query audit events with filters")
    void queryWithFilters() {
        when(auditEventMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<SfAuditEvent> result = complianceReportService.queryAuditEvents(
                "TOOL_CALLED", "EXECUTION", 1L,
                LocalDateTime.now().minusDays(1), LocalDateTime.now());

        assertThat(result).isEmpty();
    }
}
