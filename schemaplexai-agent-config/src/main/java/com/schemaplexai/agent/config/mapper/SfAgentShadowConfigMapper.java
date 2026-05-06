package com.schemaplexai.agent.config.mapper;

import com.schemaplexai.dao.mapper.BaseMapperX;
import com.schemaplexai.model.entity.agent.SfAgentShadowConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SfAgentShadowConfigMapper extends BaseMapperX<SfAgentShadowConfig> {

    SfAgentShadowConfig selectByAgentId(@Param("agentId") Long agentId, @Param("tenantId") Long tenantId);
}
