package com.schemaplexai.agent.config.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.agent.config.service.TenantEnvironmentConfigService;
import com.schemaplexai.agent.engine.config.SecurityPolicyLoader;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.dao.mapper.TenantEnvironmentConfigMapper;
import com.schemaplexai.model.entity.config.TenantEnvironmentConfig;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantEnvironmentConfigServiceImpl
        extends ServiceImpl<TenantEnvironmentConfigMapper, TenantEnvironmentConfig>
        implements TenantEnvironmentConfigService {

    private final SecurityPolicyLoader securityPolicyLoader;

    public TenantEnvironmentConfigServiceImpl(SecurityPolicyLoader securityPolicyLoader) {
        this.securityPolicyLoader = securityPolicyLoader;
    }

    @Override
    public TenantEnvironmentConfig getByTenantId(String tenantId) {
        return baseMapper.selectOne(
                new LambdaQueryWrapper<TenantEnvironmentConfig>()
                        .eq(TenantEnvironmentConfig::getTenantId, tenantId)
        );
    }

    @Override
    public void refreshCache(String tenantId) {
        securityPolicyLoader.refresh(tenantId);
    }

    @Override
    public IPage<TenantEnvironmentConfig> pageList(IPage<TenantEnvironmentConfig> page) {
        LambdaQueryWrapper<TenantEnvironmentConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(TenantEnvironmentConfig::getCreatedAt);
        return baseMapper.selectPage(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(TenantEnvironmentConfig entity) {
        if (entity.getTenantId() == null || entity.getTenantId().isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "tenantId is required");
        }
        return super.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(TenantEnvironmentConfig entity) {
        if (entity.getId() == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "id is required for update");
        }
        TenantEnvironmentConfig existing = getById(entity.getId());
        if (existing == null) {
            throw new BaseException(ResultCode.NOT_FOUND);
        }
        return super.updateById(entity);
    }
}
