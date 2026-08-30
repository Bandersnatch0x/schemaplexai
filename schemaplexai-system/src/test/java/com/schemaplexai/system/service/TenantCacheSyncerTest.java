package com.schemaplexai.system.service;

import com.schemaplexai.system.entity.SfTenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the tenant cache write channel consumed by the gateway's
 * tenant validation (issue 913).
 */
@ExtendWith(MockitoExtension.class)
class TenantCacheSyncerTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private TenantCacheSyncer syncer;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        syncer = new TenantCacheSyncer(stringRedisTemplate);
    }

    private SfTenant tenant(String code, String status) {
        SfTenant tenant = new SfTenant();
        tenant.setId(1L);
        tenant.setCode(code);
        tenant.setStatus(status);
        return tenant;
    }

    @Test
    void sync_activeTenant_writesActiveToStatusKey() {
        syncer.sync(tenant("acme", "ACTIVE"));

        verify(valueOperations).set("sf:global:cache:tenant:acme", "ACTIVE");
    }

    @Test
    void sync_disabledTenant_writesDisabledToStatusKey() {
        syncer.sync(tenant("acme", "DISABLED"));

        verify(valueOperations).set("sf:global:cache:tenant:acme", "DISABLED");
    }

    @Test
    void sync_nullStatus_isTreatedAsActive() {
        syncer.sync(tenant("acme", null));

        verify(valueOperations).set("sf:global:cache:tenant:acme", "ACTIVE");
    }

    @Test
    void sync_tenantWithoutCode_skipsRedis() {
        syncer.sync(tenant(null, "ACTIVE"));

        verifyNoInteractions(stringRedisTemplate);
    }

    @Test
    void sync_nullTenant_skipsRedis() {
        syncer.sync(null);

        verifyNoInteractions(stringRedisTemplate);
    }

    @Test
    void sync_redisFailure_isSwallowed() {
        // Write side is best-effort: a Redis outage must not break tenant
        // mutations (the startup backfill heals the channel).
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RuntimeException("Redis down"))
                .when(valueOperations).set(anyString(), anyString());

        assertThatCode(() -> syncer.sync(tenant("acme", "ACTIVE")))
                .doesNotThrowAnyException();
    }

    @Test
    void evict_deletesStatusKey() {
        syncer.evict("acme");

        verify(stringRedisTemplate).delete("sf:global:cache:tenant:acme");
    }

    @Test
    void evict_blankCode_isIgnored() {
        syncer.evict(" ");

        verifyNoInteractions(stringRedisTemplate);
    }

    @Test
    void statusOf_mapsStatusConvention() {
        assertThat(TenantCacheSyncer.statusOf(tenant("t", "ACTIVE"))).isEqualTo("ACTIVE");
        assertThat(TenantCacheSyncer.statusOf(tenant("t", null))).isEqualTo("ACTIVE");
        assertThat(TenantCacheSyncer.statusOf(tenant("t", "DISABLED"))).isEqualTo("DISABLED");
    }
}
