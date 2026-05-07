package com.schemaplexai.agent.engine.role;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.schemaplexai.agent.engine.entity.SfAgentRole;
import com.schemaplexai.agent.engine.mapper.SfAgentRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class CachingRoleRegistry implements RoleRegistry {

    private final SfAgentRoleMapper roleMapper;

    private final Cache<String, RoleOverlay> cache = Caffeine.newBuilder()
            .maximumSize(128)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    @Override
    public RoleOverlay resolve(String roleName, String tenantId) {
        String key = tenantId + ":" + roleName;
        return cache.get(key, k -> loadFromDb(roleName, tenantId));
    }

    private RoleOverlay loadFromDb(String roleName, String tenantId) {
        SfAgentRole entity = roleMapper.selectOne(
                new LambdaQueryWrapper<SfAgentRole>()
                        .eq(SfAgentRole::getName, roleName)
                        .eq(SfAgentRole::getTenantId, tenantId)
                        .eq(SfAgentRole::getStatus, 1)
                        .last("LIMIT 1"));
        if (entity == null) {
            return null;
        }
        return new RoleOverlay(entity.getId(), entity.getName(), entity.getDescription(),
                entity.getOverlay(), entity.getStatus());
    }
}
