package com.schemaplexai.agent.engine.tool.subagent;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class SubAgentQuotaService {

    private final StringRedisTemplate redisTemplate;
    private static final int PARENT_QUOTA = 16;
    private static final int TENANT_QUOTA = 64;
    private static final String PARENT_KEY_PREFIX = "sf:subagent:count:";
    private static final String TENANT_KEY_PREFIX = "sf:subagent:tenant:";
    private static final Duration KEY_TTL = Duration.ofHours(1);

    public SubAgentQuotaService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void checkAndIncrement(Long parentExecutionId) {
        String key = PARENT_KEY_PREFIX + parentExecutionId;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null) {
            redisTemplate.expire(key, KEY_TTL);
            if (count > PARENT_QUOTA) {
                redisTemplate.opsForValue().decrement(key);
                throw new SubAgentQuotaExceededException(
                    "Parent " + parentExecutionId + " exceeded sub-agent quota (" + PARENT_QUOTA + ")");
            }
        }
    }

    public void checkAndIncrementForTenant(String tenantId, Long parentExecutionId) {
        checkAndIncrement(parentExecutionId);
        String tenantKey = TENANT_KEY_PREFIX + tenantId;
        Long tenantCount = redisTemplate.opsForValue().increment(tenantKey);
        if (tenantCount != null) {
            redisTemplate.expire(tenantKey, KEY_TTL);
            if (tenantCount > TENANT_QUOTA) {
                redisTemplate.opsForValue().decrement(tenantKey);
                redisTemplate.opsForValue().decrement(PARENT_KEY_PREFIX + parentExecutionId);
                throw new SubAgentQuotaExceededException(
                    "Tenant " + tenantId + " exceeded global sub-agent quota (" + TENANT_QUOTA + ")");
            }
        }
    }

    public void decrement(Long parentExecutionId) {
        redisTemplate.opsForValue().decrement(PARENT_KEY_PREFIX + parentExecutionId);
    }
}
