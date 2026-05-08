package com.schemaplexai.quality.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.quality.entity.SfAuditEvent;
import com.schemaplexai.quality.service.AuditEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/quality/audit-events")
@RequiredArgsConstructor
@Tag(name = "审计事件管理")
public class AuditEventController {

    private final AuditEventService auditEventService;

    @Operation(summary = "创建审计事件")
    @PostMapping
    public Result<Long> create(@RequestBody SfAuditEvent auditEvent) {
        auditEventService.save(auditEvent);
        return Result.success(auditEvent.getId());
    }

    @Operation(summary = "更新审计事件")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfAuditEvent auditEvent) {
        auditEvent.setId(id);
        return Result.success(auditEventService.updateById(auditEvent));
    }

    @Operation(summary = "删除审计事件")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(auditEventService.removeById(id));
    }

    @Operation(summary = "获取审计事件详情")
    @GetMapping("/{id}")
    public Result<SfAuditEvent> get(@PathVariable Long id) {
        SfAuditEvent event = auditEventService.getById(id);
        if (event == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(event);
    }

    @Operation(summary = "获取审计事件列表")
    @GetMapping
    public Result<List<SfAuditEvent>> list() {
        return Result.success(auditEventService.list());
    }
}
