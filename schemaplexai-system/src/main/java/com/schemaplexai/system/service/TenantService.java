package com.schemaplexai.system.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.system.entity.SfTenant;
import com.schemaplexai.system.mapper.SfTenantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TenantService extends ServiceImpl<SfTenantMapper, SfTenant> {

    /**
     * Write-through publisher for the gateway tenant cache channel (issue 913).
     * Mutations go through the create/update/delete methods below so the shared
     * cache the gateway validates against never drifts from the database.
     */
    private final TenantCacheSyncer tenantCacheSyncer;

    public SfTenant getByCode(String code) {
        return lambdaQuery().eq(SfTenant::getCode, code).one();
    }

    public SfTenant getValidTenant(Long id) {
        SfTenant tenant = getById(id);
        if (tenant == null) {
            throw new BaseException(ResultCode.TENANT_NOT_FOUND);
        }
        if (com.schemaplexai.common.redis.TenantRedisKeyResolver.TENANT_STATUS_DISABLED.equals(tenant.getStatus())) {
            throw new BaseException(ResultCode.TENANT_DISABLED);
        }
        return tenant;
    }

    /** Create a tenant and publish it to the gateway tenant cache channel. */
    public boolean createTenant(SfTenant tenant) {
        boolean created = save(tenant);
        if (created) {
            tenantCacheSyncer.sync(tenant);
        }
        return created;
    }

    /**
     * Update a tenant and republish its status. The entity is re-read after the
     * update so partial updates (e.g. only configJson) still publish the true
     * current status.
     */
    public boolean updateTenant(SfTenant tenant) {
        boolean updated = updateById(tenant);
        if (updated) {
            SfTenant current = getById(tenant.getId());
            if (current != null) {
                tenantCacheSyncer.sync(current);
            }
        }
        return updated;
    }

    /** Delete a tenant and withdraw it from the gateway tenant cache channel. */
    public boolean deleteTenant(Long id) {
        SfTenant existing = getById(id);
        boolean deleted = removeById(id);
        if (deleted && existing != null) {
            tenantCacheSyncer.evict(existing);
        }
        return deleted;
    }
}
