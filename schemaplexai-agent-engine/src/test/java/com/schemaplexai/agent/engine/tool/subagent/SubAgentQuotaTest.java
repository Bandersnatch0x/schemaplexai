package com.schemaplexai.agent.engine.tool.subagent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubAgentQuotaTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private SubAgentQuotaService quotaService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void shouldAllowSubAgentUnderParentQuota() {
        when(valueOps.increment("sf:subagent:count:100")).thenReturn(1L);

        assertDoesNotThrow(() -> quotaService.checkAndIncrement(100L));

        verify(valueOps).increment("sf:subagent:count:100");
        verify(redisTemplate).expire(eq("sf:subagent:count:100"), any());
    }

    @Test
    void shouldRejectWhenParentQuotaExceeded() {
        when(valueOps.increment("sf:subagent:count:200")).thenReturn(17L);
        when(valueOps.decrement("sf:subagent:count:200")).thenReturn(16L);

        SubAgentQuotaExceededException ex = assertThrows(
            SubAgentQuotaExceededException.class,
            () -> quotaService.checkAndIncrement(200L)
        );

        assertTrue(ex.getMessage().contains("Parent 200 exceeded sub-agent quota (16)"));
        verify(valueOps).decrement("sf:subagent:count:200");
    }

    @Test
    void shouldRejectWhenTenantQuotaExceeded() {
        when(valueOps.increment("sf:subagent:count:300")).thenReturn(1L);
        when(valueOps.increment("sf:subagent:tenant:tenantA")).thenReturn(65L);
        when(valueOps.decrement("sf:subagent:tenant:tenantA")).thenReturn(64L);
        when(valueOps.decrement("sf:subagent:count:300")).thenReturn(0L);

        SubAgentQuotaExceededException ex = assertThrows(
            SubAgentQuotaExceededException.class,
            () -> quotaService.checkAndIncrementForTenant("tenantA", 300L)
        );

        assertTrue(ex.getMessage().contains("Tenant tenantA exceeded global sub-agent quota (64)"));
        verify(valueOps).decrement("sf:subagent:tenant:tenantA");
        verify(valueOps).decrement("sf:subagent:count:300");
    }

    @Test
    void shouldDecrementCorrectly() {
        when(valueOps.decrement("sf:subagent:count:400")).thenReturn(0L);

        quotaService.decrement(400L);

        verify(valueOps).decrement("sf:subagent:count:400");
    }
}
