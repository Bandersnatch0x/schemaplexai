package com.schemaplexai.agent.config.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.schemaplexai.agent.config.entity.SfAgent;
import com.schemaplexai.dao.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SfAgentMapper extends BaseMapperX<SfAgent> {

    /**
     * 按名称和租户查找未删除的 agent。
     *
     * @param name     agent 名称（不可为 null）
     * @param tenantId 租户 ID（为 null 时直接返回 null）
     * @return 匹配的 SfAgent，找不到时返回 null
     */
    default SfAgent findByNameAndTenant(String name, String tenantId) {
        if (tenantId == null || name == null) {
            return null;
        }
        return selectOne(
                new LambdaQueryWrapper<SfAgent>()
                        .eq(SfAgent::getName, name)
                        .eq(SfAgent::getTenantId, tenantId)
        );
    }
}
