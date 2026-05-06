package com.schemaplexai.admin.controller;

import com.schemaplexai.admin.dto.AuditLogQuery;
import com.schemaplexai.admin.entity.SfAuditLog;
import com.schemaplexai.admin.service.AuditLogService;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.model.dto.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "审计日志管理")
@RestController
@RequestMapping("/admin/audit-logs")
@RequiredArgsConstructor
public class AuditLogController extends BaseAdminController {

    private final AuditLogService auditLogService;

    @Operation(summary = "分页查询审计日志")
    @GetMapping
    public Result<PageResult<SfAuditLog>> page(AuditLogQuery query) {
        return success(auditLogService.queryAuditLogs(query));
    }

    @Operation(summary = "获取审计日志详情")
    @GetMapping("/{id}")
    public Result<SfAuditLog> getById(@PathVariable Long id) {
        SfAuditLog logEntry = auditLogService.getById(id);
        if (logEntry == null) {
            return error("audit log not found");
        }
        return success(logEntry);
    }

    @Operation(summary = "按租户查询最近日志")
    @GetMapping("/recent")
    public Result<List<SfAuditLog>> recentByTenant(
            @RequestParam String tenantId,
            @RequestParam(required = false, defaultValue = "50") Integer limit) {
        return success(auditLogService.getRecentLogsByTenant(tenantId, limit));
    }

    @Operation(summary = "记录审计日志")
    @PostMapping
    public Result<Long> create(@RequestBody SfAuditLog auditLog) {
        auditLogService.recordAuditLog(auditLog);
        return success(auditLog.getId());
    }

    @Operation(summary = "最近失败的操作日志")
    @GetMapping("/failed")
    public Result<List<SfAuditLog>> recentFailed(
            @RequestParam(required = false, defaultValue = "20") Integer limit) {
        return success(auditLogService.getRecentFailedLogs(limit));
    }
}
