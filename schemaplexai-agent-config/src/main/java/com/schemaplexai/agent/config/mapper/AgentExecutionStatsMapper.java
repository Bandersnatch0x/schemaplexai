package com.schemaplexai.agent.config.mapper;

import com.schemaplexai.agent.config.entity.SfAgentExecution;
import com.schemaplexai.dao.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Mapper;

/**
 * Read-only access to {@code sf_agent_execution} for Cockpit statistics.
 * Tenant filtering is applied automatically by the MyBatis-Plus
 * {@code TenantLineInnerInterceptor} registered in {@code MyBatisPlusConfig}.
 */
@Mapper
public interface AgentExecutionStatsMapper extends BaseMapperX<SfAgentExecution> {
}
