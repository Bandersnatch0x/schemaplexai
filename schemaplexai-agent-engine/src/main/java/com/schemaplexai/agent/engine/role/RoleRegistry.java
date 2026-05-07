package com.schemaplexai.agent.engine.role;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.schemaplexai.agent.engine.entity.SfAgentRole;
import com.schemaplexai.agent.engine.mapper.SfAgentRoleMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RoleRegistry {

    private final SfAgentRoleMapper roleMapper;
    private final Cache<String, RoleOverlay> cache;

    public RoleRegistry(SfAgentRoleMapper roleMapper) {
        this.roleMapper = roleMapper;
        this.cache = Caffeine.newBuilder()
                .maximumSize(200)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .build();
    }

    /**
     * Resolve a role name to a RoleOverlay.
     * Cache key: tenantId:roleName
     */
    public RoleOverlay resolve(String roleName, String tenantId) {
        String cacheKey = tenantId + ":" + roleName;
        return Optional.ofNullable(cache.getIfPresent(cacheKey))
                .orElseGet(() -> loadAndCache(roleName, tenantId, cacheKey));
    }

    private RoleOverlay loadAndCache(String roleName, String tenantId, String cacheKey) {
        SfAgentRole entity = roleMapper.selectOne(
                new LambdaQueryWrapper<SfAgentRole>()
                        .eq(SfAgentRole::getName, roleName)
                        .eq(SfAgentRole::getTenantId, tenantId)
                        .eq(SfAgentRole::getStatus, 1)
        );
        if (entity == null) {
            log.debug("Role not found: {}", roleName);
            return null;
        }
        RoleOverlay overlay = new RoleOverlay(entity.getName(), entity.getDescription(), entity.getOverlay());
        cache.put(cacheKey, overlay);
        return overlay;
    }

    public void invalidate(String roleName, String tenantId) {
        cache.invalidate(tenantId + ":" + roleName);
    }
}
