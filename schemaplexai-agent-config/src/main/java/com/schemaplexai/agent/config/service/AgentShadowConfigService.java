package com.schemaplexai.agent.config.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.schemaplexai.model.entity.agent.SfAgentShadowConfig;

public interface AgentShadowConfigService extends IService<SfAgentShadowConfig> {

    SfAgentShadowConfig getByAgentId(Long agentId);

    void toggleEnabled(Long id, boolean enabled);

    IPage<SfAgentShadowConfig> pageList(IPage<SfAgentShadowConfig> page);
}
