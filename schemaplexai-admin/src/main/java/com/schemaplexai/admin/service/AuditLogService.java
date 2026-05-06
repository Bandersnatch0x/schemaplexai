package com.schemaplexai.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.admin.dto.AuditLogQuery;
import com.schemaplexai.admin.entity.SfAuditLog;
import com.schemaplexai.admin.mapper.SfAuditLogMapper;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.model.dto.PageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class AuditLogService extends ServiceImpl<SfAuditLogMapper, SfAuditLog> {

    public PageResult<SfAuditLog> queryAuditLogs(AuditLogQuery query) {
        Page<SfAuditLog> page = lambdaQuery()
                .eq(query.getTenantId() != null, SfAuditLog::getTenantId, query.getTenantId())
                .eq(query.getUserId() != null, SfAuditLog::getUserId, query.getUserId())
                .eq(query.getAction() != null && !query.getAction().isBlank(), SfAuditLog::getAction, query.getAction())
                .eq(query.getResourceType() != null && !query.getResourceType().isBlank(), SfAuditLog::getResourceType, query.getResourceType())
                .eq(query.getStatus() != null, SfAuditLog::getStatus, query.getStatus())
                .ge(query.getStartTime() != null, SfAuditLog::getExecutedAt, query.getStartTime())
                .le(query.getEndTime() != null, SfAuditLog::getExecutedAt, query.getEndTime())
                .orderByDesc(SfAuditLog::getExecutedAt)
                .page(new Page<>(query.getCurrent(), query.getSize()));

        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public List<SfAuditLog> getRecentLogsByTenant(String tenantId, Integer limit) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "tenantId is required");
        }
        int safeLimit = (limit != null && limit > 0 && limit <= 1000) ? limit : 50;
        return baseMapper.selectRecentByTenantId(tenantId, safeLimit);
    }

    public void recordAuditLog(SfAuditLog auditLog) {
        if (auditLog.getExecutedAt() == null) {
            auditLog.setExecutedAt(LocalDateTime.now());
        }
        if (auditLog.getStatus() == null) {
            auditLog.setStatus(1);
        }
        save(auditLog);
        log.debug("Audit log recorded: action={}, userId={}, resourceType={}",
                auditLog.getAction(), auditLog.getUserId(), auditLog.getResourceType());
    }

    public long countFailedActionsSince(LocalDateTime since) {
        if (since == null) {
            since = LocalDateTime.now().minusDays(1);
        }
        return lambdaQuery()
                .eq(SfAuditLog::getStatus, 0)
                .ge(SfAuditLog::getExecutedAt, since)
                .count();
    }

    public List<SfAuditLog> getRecentFailedLogs(Integer limit) {
        int safeLimit = (limit != null && limit > 0 && limit <= 1000) ? limit : 20;
        return lambdaQuery()
                .eq(SfAuditLog::getStatus, 0)
                .orderByDesc(SfAuditLog::getExecutedAt)
                .last("LIMIT " + safeLimit)
                .list();
    }
}
