package com.schemaplexai.agent.config.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.schemaplexai.model.entity.config.TenantEnvironmentConfig;

public interface TenantEnvironmentConfigService extends IService<TenantEnvironmentConfig> {

    TenantEnvironmentConfig getByTenantId(String tenantId);

    void refreshCache(String tenantId);

    IPage<TenantEnvironmentConfig> pageList(IPage<TenantEnvironmentConfig> page);
}
