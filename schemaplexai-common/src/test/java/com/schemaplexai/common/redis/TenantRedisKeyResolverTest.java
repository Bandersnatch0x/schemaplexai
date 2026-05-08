package com.schemaplexai.common.redis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TenantRedisKeyResolverTest {

    @Test
    void tenantKey_shouldReturnCorrectFormat() {
        String key = TenantRedisKeyResolver.tenantKey("tenant-1", "cache", "detail");
        assertEquals("sf:tenant-1:cache:detail", key);
    }

    @Test
    void tenantKeyWithSub_shouldReturnCorrectFormat() {
        String key = TenantRedisKeyResolver.tenantKey("tenant-1", "cache", "sub", "detail");
        assertEquals("sf:tenant-1:cache:sub:detail", key);
    }

    @Test
    void globalKey_shouldReturnCorrectFormat() {
        String key = TenantRedisKeyResolver.globalKey("token", "session");
        assertEquals("sf:global:token:session", key);
    }

    @Test
    void globalKeyWithSub_shouldReturnCorrectFormat() {
        String key = TenantRedisKeyResolver.globalKey("token", "sub", "detail");
        assertEquals("sf:global:token:sub:detail", key);
    }

    @Test
    void chatMemory_shouldReturnCorrectFormat() {
        String key = TenantRedisKeyResolver.chatMemory("tenant-1", "conv-1");
        assertEquals("sf:tenant-1:cache:chat:conv-1", key);
    }

    @Test
    void rateLimit_shouldReturnCorrectFormat() {
        String key = TenantRedisKeyResolver.rateLimit("tenant-1", "api", "window-1");
        assertEquals("sf:tenant-1:ratelimit:api:window-1", key);
    }

    @Test
    void rateLimitGlobal_shouldReturnCorrectFormat() {
        String key = TenantRedisKeyResolver.rateLimitGlobal("ip", "127.0.0.1", "window-1");
        assertEquals("sf:global:ratelimit:ip:127.0.0.1:window-1", key);
    }

    @Test
    void admissionRate_shouldReturnCorrectFormat() {
        String key = TenantRedisKeyResolver.admissionRate("tenant-1", "agent-1");
        assertEquals("sf:tenant-1:admission:rate:agent-1", key);
    }

    @Test
    void admissionConcurrency_shouldReturnCorrectFormat() {
        String key = TenantRedisKeyResolver.admissionConcurrency("tenant-1", "agent-1");
        assertEquals("sf:tenant-1:admission:concurrency:agent-1", key);
    }

    @Test
    void admissionCost_shouldReturnCorrectFormat() {
        String key = TenantRedisKeyResolver.admissionCost("tenant-1");
        assertEquals("sf:tenant-1:admission:cost", key);
    }

    @Test
    void tokenSession_shouldReturnCorrectFormat() {
        String key = TenantRedisKeyResolver.tokenSession("user-1");
        assertEquals("sf:global:token:session:user-1", key);
    }

    @Test
    void tokenBlacklist_shouldReturnCorrectFormat() {
        String key = TenantRedisKeyResolver.tokenBlacklist("jti-123");
        assertEquals("sf:global:token:blacklist:jti-123", key);
    }

    @Test
    void idempotency_shouldReturnCorrectFormat() {
        String key = TenantRedisKeyResolver.idempotency("group", "msg-1");
        assertEquals("sf:global:idempotency:group:msg-1", key);
    }

    @Test
    void modelCooldown_shouldReturnCorrectFormat() {
        String key = TenantRedisKeyResolver.modelCooldown("openai");
        assertEquals("sf:global:model:cooldown:openai", key);
    }

    @Test
    void healthCheck_shouldReturnCorrectFormat() {
        String key = TenantRedisKeyResolver.healthCheck();
        assertEquals("sf:global:health:check", key);
    }

    @Test
    void tenantKey_shouldRejectNullTenantId() {
        assertThrows(IllegalArgumentException.class, () ->
            TenantRedisKeyResolver.tenantKey(null, "cache", "detail")
        );
    }

    @Test
    void tenantKey_shouldRejectBlankTenantId() {
        assertThrows(IllegalArgumentException.class, () ->
            TenantRedisKeyResolver.tenantKey("   ", "cache", "detail")
        );
    }

    @Test
    void tenantKey_shouldRejectNullCategory() {
        assertThrows(IllegalArgumentException.class, () ->
            TenantRedisKeyResolver.tenantKey("tenant-1", null, "detail")
        );
    }

    @Test
    void globalKey_shouldRejectNullCategory() {
        assertThrows(IllegalArgumentException.class, () ->
            TenantRedisKeyResolver.globalKey(null, "detail")
        );
    }

    @Test
    void subagentParentCount_shouldReturnCorrectFormat() {
        String key = TenantRedisKeyResolver.subagentParentCount("tenant-1", "exec-1");
        assertEquals("sf:tenant-1:subagent:count:exec-1", key);
    }

    @Test
    void subagentTenantCount_shouldReturnCorrectFormat() {
        String key = TenantRedisKeyResolver.subagentTenantCount("tenant-1");
        assertEquals("sf:tenant-1:subagent:tenant", key);
    }

    @Test
    void executionPaused_shouldReturnCorrectFormat() {
        String key = TenantRedisKeyResolver.executionPaused("tenant-1", "exec-1");
        assertEquals("sf:tenant-1:execution:paused:exec-1", key);
    }

    @Test
    void conversationFiles_shouldReturnCorrectFormat() {
        String key = TenantRedisKeyResolver.conversationFiles("tenant-1", "conv-1");
        assertEquals("sf:tenant-1:cache:files:conv-1", key);
    }

    @Test
    void chatMemoryBackfillLock_shouldReturnCorrectFormat() {
        String key = TenantRedisKeyResolver.chatMemoryBackfillLock("tenant-1", "conv-1");
        assertEquals("sf:tenant-1:cache:chat:conv-1:backfill_lock", key);
    }

    @Test
    void idempotencyAgentExecute_shouldReturnCorrectFormat() {
        String key = TenantRedisKeyResolver.idempotencyAgentExecute("key-1");
        assertEquals("sf:global:idempotency:agent:execute:key-1", key);
    }

    @Test
    void idempotencyCostSync_shouldReturnCorrectFormat() {
        String key = TenantRedisKeyResolver.idempotencyCostSync("key-1");
        assertEquals("sf:global:idempotency:cost:sync:key-1", key);
    }
}
