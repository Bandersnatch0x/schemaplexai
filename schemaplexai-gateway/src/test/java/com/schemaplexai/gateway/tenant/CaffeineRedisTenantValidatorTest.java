package com.schemaplexai.gateway.tenant;

import com.schemaplexai.common.redis.TenantRedisKeyResolver;
import com.schemaplexai.gateway.config.TenantValidationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the Caffeine L1 + Redis L2 tenant status validator (issue 913).
 */
class CaffeineRedisTenantValidatorTest {

    private CaffeineRedisTenantValidator validator;
    private ReactiveStringRedisTemplate redisTemplate;
    private ReactiveValueOperations<String, String> valueOps;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(ReactiveStringRedisTemplate.class);
        valueOps = mock(ReactiveValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        validator = new CaffeineRedisTenantValidator(redisTemplate, new TenantValidationProperties());
    }

    @Test
    void validate_activeTenant_returnsActive() {
        when(valueOps.get(eq(TenantRedisKeyResolver.tenantStatus("tenant-1"))))
                .thenReturn(Mono.just(TenantRedisKeyResolver.TENANT_STATUS_ACTIVE));

        StepVerifier.create(validator.validate("tenant-1"))
                .expectNext(TenantStatus.ACTIVE)
                .verifyComplete();
    }

    @Test
    void validate_disabledTenant_returnsDisabled() {
        when(valueOps.get(eq(TenantRedisKeyResolver.tenantStatus("tenant-2"))))
                .thenReturn(Mono.just(TenantRedisKeyResolver.TENANT_STATUS_DISABLED));

        StepVerifier.create(validator.validate("tenant-2"))
                .expectNext(TenantStatus.DISABLED)
                .verifyComplete();
    }

    @Test
    void validate_missingKey_returnsNotFound() {
        // A tenant the cache channel knows nothing about is forged/stale.
        when(valueOps.get(anyString())).thenReturn(Mono.empty());

        StepVerifier.create(validator.validate("forged-tenant"))
                .expectNext(TenantStatus.NOT_FOUND)
                .verifyComplete();
    }

    @Test
    void validate_unexpectedValue_returnsNotFound() {
        when(valueOps.get(anyString())).thenReturn(Mono.just("SOMETHING_ELSE"));

        StepVerifier.create(validator.validate("tenant-x"))
                .expectNext(TenantStatus.NOT_FOUND)
                .verifyComplete();
    }

    @Test
    void validate_secondLookup_servedFromLocalCache() {
        when(valueOps.get(anyString()))
                .thenReturn(Mono.just(TenantRedisKeyResolver.TENANT_STATUS_ACTIVE));

        StepVerifier.create(validator.validate("tenant-cached"))
                .expectNext(TenantStatus.ACTIVE)
                .verifyComplete();
        StepVerifier.create(validator.validate("tenant-cached"))
                .expectNext(TenantStatus.ACTIVE)
                .verifyComplete();

        // Only one Redis round-trip; the second lookup hits the Caffeine L1.
        verify(valueOps, times(1)).get(anyString());
    }

    @Test
    void validate_notFoundResult_isCachedToo() {
        // Negative caching shields Redis from floods with forged tenant ids.
        when(valueOps.get(anyString())).thenReturn(Mono.empty());

        StepVerifier.create(validator.validate("forged"))
                .expectNext(TenantStatus.NOT_FOUND)
                .verifyComplete();
        StepVerifier.create(validator.validate("forged"))
                .expectNext(TenantStatus.NOT_FOUND)
                .verifyComplete();

        verify(valueOps, times(1)).get(anyString());
    }

    @Test
    void validate_redisError_propagatesForFailClosedHandling() {
        when(valueOps.get(anyString())).thenReturn(Mono.error(new RuntimeException("Redis down")));

        StepVerifier.create(validator.validate("tenant-1"))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void validate_usesTenantStatusKeyConvention() {
        when(valueOps.get(anyString())).thenReturn(Mono.empty());

        StepVerifier.create(validator.validate("tenant-42"))
                .expectNext(TenantStatus.NOT_FOUND)
                .verifyComplete();

        verify(valueOps).get(eq("sf:global:cache:tenant:tenant-42"));
    }
}
