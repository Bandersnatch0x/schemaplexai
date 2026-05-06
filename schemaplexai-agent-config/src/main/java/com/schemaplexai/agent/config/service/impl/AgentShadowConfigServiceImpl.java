package com.schemaplexai.agent.config.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.agent.config.mapper.SfAgentShadowConfigMapper;
import com.schemaplexai.agent.config.service.AgentShadowConfigService;
import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.model.entity.agent.SfAgentShadowConfig;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgentShadowConfigServiceImpl
        extends ServiceImpl<SfAgentShadowConfigMapper, SfAgentShadowConfig>
        implements AgentShadowConfigService {

    @Override
    public SfAgentShadowConfig getByAgentId(Long agentId) {
        String tenantIdStr = TenantContextHolder.getTenantId();
        Long tenantId = tenantIdStr != null ? Long.valueOf(tenantIdStr) : null;
        return baseMapper.selectByAgentId(agentId, tenantId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleEnabled(Long id, boolean enabled) {
        SfAgentShadowConfig config = getById(id);
        if (config == null) {
            throw new BaseException(ResultCode.NOT_FOUND);
        }
        config.setEnabled(enabled);
        updateById(config);
    }

    @Override
    public IPage<SfAgentShadowConfig> pageList(IPage<SfAgentShadowConfig> page) {
        LambdaQueryWrapper<SfAgentShadowConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(SfAgentShadowConfig::getCreatedAt);
        return baseMapper.selectPage(page, wrapper);
    }
}
