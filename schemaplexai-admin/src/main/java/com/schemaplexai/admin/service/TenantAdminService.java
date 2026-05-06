package com.schemaplexai.admin.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.admin.dto.TenantAdminDTO;
import com.schemaplexai.admin.dto.TenantConfigUpdateDTO;
import com.schemaplexai.admin.entity.SfAuditLog;
import com.schemaplexai.admin.mapper.SfAuditLogMapper;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.system.entity.SfTenant;
import com.schemaplexai.system.entity.SfUser;
import com.schemaplexai.system.mapper.SfTenantMapper;
import com.schemaplexai.system.mapper.SfUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantAdminService extends ServiceImpl<SfTenantMapper, SfTenant> {

    private final SfUserMapper userMapper;
    private final SfAuditLogMapper auditLogMapper;

    public TenantAdminDTO getTenantAdminDetail(Long tenantId) {
        SfTenant tenant = getById(tenantId);
        if (tenant == null) {
            throw new BaseException(ResultCode.TENANT_NOT_FOUND);
        }

        TenantAdminDTO dto = new TenantAdminDTO();
        dto.setId(tenant.getId());
        dto.setName(tenant.getName());
        dto.setCode(tenant.getCode());
        dto.setStatus(tenant.getStatus());
        dto.setConfigJson(tenant.getConfigJson());
        dto.setCreatedAt(tenant.getCreatedAt());
        dto.setUpdatedAt(tenant.getUpdatedAt());

        dto.setUserCount(countUsersByTenant(tenant.getCode()));
        dto.setAuditLogCount(countAuditLogsByTenant(tenant.getCode()));
        dto.setActiveUserCountLast7Days(countActiveUsersSince(tenant.getCode(), LocalDateTime.now().minusDays(7)));

        return dto;
    }

    public List<TenantAdminDTO> listAllTenantDetails() {
        List<SfTenant> tenants = list();
        return tenants.stream()
                .map(t -> {
                    TenantAdminDTO dto = new TenantAdminDTO();
                    dto.setId(t.getId());
                    dto.setName(t.getName());
                    dto.setCode(t.getCode());
                    dto.setStatus(t.getStatus());
                    dto.setConfigJson(t.getConfigJson());
                    dto.setCreatedAt(t.getCreatedAt());
                    dto.setUpdatedAt(t.getUpdatedAt());
                    dto.setUserCount(countUsersByTenant(t.getCode()));
                    dto.setAuditLogCount(countAuditLogsByTenant(t.getCode()));
                    dto.setActiveUserCountLast7Days(countActiveUsersSince(t.getCode(), LocalDateTime.now().minusDays(7)));
                    return dto;
                })
                .toList();
    }

    public void disableTenant(Long tenantId) {
        SfTenant tenant = getById(tenantId);
        if (tenant == null) {
            throw new BaseException(ResultCode.TENANT_NOT_FOUND);
        }
        tenant.setStatus(0);
        updateById(tenant);
        log.info("Tenant disabled: id={}, code={}", tenantId, tenant.getCode());
    }

    public void enableTenant(Long tenantId) {
        SfTenant tenant = getById(tenantId);
        if (tenant == null) {
            throw new BaseException(ResultCode.TENANT_NOT_FOUND);
        }
        tenant.setStatus(1);
        updateById(tenant);
        log.info("Tenant enabled: id={}, code={}", tenantId, tenant.getCode());
    }

    @Transactional
    public void updateTenantConfig(Long tenantId, TenantConfigUpdateDTO dto) {
        SfTenant tenant = getById(tenantId);
        if (tenant == null) {
            throw new BaseException(ResultCode.TENANT_NOT_FOUND);
        }
        tenant.setConfigJson(dto.getConfigJson());
        updateById(tenant);
        log.info("Tenant config updated: id={}, code={}", tenantId, tenant.getCode());
    }

    private Long countUsersByTenant(String tenantCode) {
        return userMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SfUser>()
                        .eq(SfUser::getTenantId, tenantCode)
        );
    }

    private Long countAuditLogsByTenant(String tenantCode) {
        return auditLogMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SfAuditLog>()
                        .eq(SfAuditLog::getTenantId, tenantCode)
        );
    }

    private Long countActiveUsersSince(String tenantCode, LocalDateTime since) {
        return auditLogMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SfAuditLog>()
                        .eq(SfAuditLog::getTenantId, tenantCode)
                        .ge(SfAuditLog::getExecutedAt, since)
                        .groupBy(SfAuditLog::getUserId)
        );
    }
}
