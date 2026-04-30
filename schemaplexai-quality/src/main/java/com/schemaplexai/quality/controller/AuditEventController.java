package com.schemaplexai.quality.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.quality.entity.SfAuditEvent;
import com.schemaplexai.quality.service.AuditEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/quality/audit-events")
@RequiredArgsConstructor
public class AuditEventController {

    private final AuditEventService auditEventService;

    @PostMapping
    public Result<Long> create(@RequestBody SfAuditEvent auditEvent) {
        auditEventService.save(auditEvent);
        return Result.success(auditEvent.getId());
    }

    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfAuditEvent auditEvent) {
        auditEvent.setId(id);
        return Result.success(auditEventService.updateById(auditEvent));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(auditEventService.removeById(id));
    }

    @GetMapping("/{id}")
    public Result<SfAuditEvent> get(@PathVariable Long id) {
        SfAuditEvent event = auditEventService.getById(id);
        if (event == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(event);
    }

    @GetMapping
    public Result<List<SfAuditEvent>> list() {
        return Result.success(auditEventService.list());
    }
}
