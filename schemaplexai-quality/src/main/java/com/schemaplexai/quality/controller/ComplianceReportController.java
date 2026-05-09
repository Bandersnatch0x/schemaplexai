package com.schemaplexai.quality.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.quality.entity.SfAuditEvent;
import com.schemaplexai.quality.service.AuditEventService;
import com.schemaplexai.quality.service.ComplianceReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * M5.1 Compliance Reporting API.
 * Provides audit trail and compliance reports for executions and tenants.
 */
@RestController
@RequestMapping("/quality/compliance")
@RequiredArgsConstructor
@Tag(name = "合规报告", description = "审计追踪与合规报告")
public class ComplianceReportController {

    private final ComplianceReportService complianceReportService;
    private final AuditEventService auditEventService;

    @Operation(summary = "执行合规报告")
    @GetMapping("/executions/{executionId}")
    public Result<Map<String, Object>> executionReport(@PathVariable Long executionId) {
        return Result.success(complianceReportService.buildExecutionReport(executionId));
    }

    @Operation(summary = "租户审计事件列表")
    @GetMapping("/audit-events")
    public Result<List<SfAuditEvent>> auditEvents(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) Long resourceId,
            @RequestParam(required = false) LocalDateTime start,
            @RequestParam(required = false) LocalDateTime end) {
        return Result.success(complianceReportService.queryAuditEvents(eventType, resourceType, resourceId, start, end));
    }

    @Operation(summary = "租户合规仪表盘")
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        return Result.success(complianceReportService.buildDashboard());
    }
}
