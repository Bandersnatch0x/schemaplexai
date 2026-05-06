package com.schemaplexai.agent.engine.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.schemaplexai.dao.mapper.TenantEnvironmentConfigMapper;
import com.schemaplexai.model.entity.config.TenantEnvironmentConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("SecurityPolicyLoader")
class SecurityPolicyLoaderTest {

    private TenantEnvironmentConfigMapper mockMapper;
    private SecurityPolicyLoader loader;

    @BeforeEach
    void setUp() {
        mockMapper = mock(TenantEnvironmentConfigMapper.class);
        loader = new SecurityPolicyLoader(mockMapper);
    }

    @Nested
    @DisplayName("load")
    class LoadTests {

        @Test
        @DisplayName("should return default config for null tenantId")
        void shouldReturnDefaultForNull() {
            TenantEnvironmentConfig config = loader.load(null);

            assertNotNull(config);
            assertEquals("unknown", config.getEnvironment());
            assertEquals("HIGH", config.getSecurityLevel());
            assertFalse(config.getAllowHttpCalls());
            assertFalse(config.getAllowFileRead());
            assertFalse(config.getAllowIrreversibleOps());
            assertEquals(1, config.getMaxConcurrentToolCalls());
            verifyNoInteractions(mockMapper);
        }

        @Test
        @DisplayName("should return default config for blank tenantId")
        void shouldReturnDefaultForBlank() {
            TenantEnvironmentConfig config = loader.load("  ");

            assertNotNull(config);
            assertEquals("HIGH", config.getSecurityLevel());
            verifyNoInteractions(mockMapper);
        }

        @Test
        @DisplayName("should load config from DB on cache miss")
        void shouldLoadFromDbOnCacheMiss() {
            TenantEnvironmentConfig dbConfig = new TenantEnvironmentConfig();
            dbConfig.setTenantId("tenant-1");
            dbConfig.setEnvironment("prod");
            dbConfig.setSecurityLevel("LOW");
            dbConfig.setAllowHttpCalls(true);
            dbConfig.setAllowFileRead(true);
            dbConfig.setAllowIrreversibleOps(false);
            dbConfig.setMaxConcurrentToolCalls(5);

            when(mockMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(dbConfig);

            TenantEnvironmentConfig config = loader.load("tenant-1");

            assertNotNull(config);
            assertEquals("prod", config.getEnvironment());
            assertEquals("LOW", config.getSecurityLevel());
            assertTrue(config.getAllowHttpCalls());
            verify(mockMapper, times(1)).selectOne(any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("should return cached config on second call (no DB hit)")
        void shouldReturnCachedOnSecondCall() {
            TenantEnvironmentConfig dbConfig = new TenantEnvironmentConfig();
            dbConfig.setTenantId("tenant-2");
            dbConfig.setEnvironment("staging");
            dbConfig.setSecurityLevel("MEDIUM");

            when(mockMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(dbConfig);

            // First call - hits DB
            TenantEnvironmentConfig first = loader.load("tenant-2");
            // Second call - should use cache
            TenantEnvironmentConfig second = loader.load("tenant-2");

            assertSame(first, second);
            verify(mockMapper, times(1)).selectOne(any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("should return default config when DB returns null")
        void shouldReturnDefaultWhenDbReturnsNull() {
            when(mockMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            TenantEnvironmentConfig config = loader.load("unknown-tenant");

            assertNotNull(config);
            assertEquals("unknown", config.getEnvironment());
            assertEquals("HIGH", config.getSecurityLevel());
            assertFalse(config.getAllowHttpCalls());
            assertFalse(config.getAllowFileRead());
        }

        @Test
        @DisplayName("should return default config when DB throws exception")
        void shouldReturnDefaultWhenDbThrows() {
            when(mockMapper.selectOne(any(LambdaQueryWrapper.class)))
                    .thenThrow(new RuntimeException("DB connection failed"));

            TenantEnvironmentConfig config = loader.load("tenant-error");

            assertNotNull(config);
            assertEquals("unknown", config.getEnvironment());
            assertEquals("HIGH", config.getSecurityLevel());
        }

        @Test
        @DisplayName("should use deny-by-default policy for unknown tenants")
        void shouldUseDenyByDefault() {
            when(mockMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            TenantEnvironmentConfig config = loader.load("new-tenant");

            assertThat(config.getSecurityLevel()).isEqualTo("HIGH");
            assertThat(config.getAllowHttpCalls()).isFalse();
            assertThat(config.getAllowFileRead()).isFalse();
            assertThat(config.getAllowIrreversibleOps()).isFalse();
            assertThat(config.getMaxConcurrentToolCalls()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("refresh")
    class RefreshTests {

        @Test
        @DisplayName("should invalidate cache for tenant")
        void shouldInvalidateCache() {
            // Load to populate cache
            TenantEnvironmentConfig dbConfig = new TenantEnvironmentConfig();
            dbConfig.setTenantId("tenant-3");
            dbConfig.setEnvironment("prod");
            dbConfig.setSecurityLevel("LOW");

            when(mockMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(dbConfig);

            loader.load("tenant-3");
            loader.load("tenant-3"); // cached
            verify(mockMapper, times(1)).selectOne(any());

            // Refresh - invalidates cache
            loader.refresh("tenant-3");

            // Next load should hit DB again
            loader.load("tenant-3");
            verify(mockMapper, times(2)).selectOne(any());
        }

        @Test
        @DisplayName("should handle null tenantId gracefully")
        void shouldHandleNullRefresh() {
            assertDoesNotThrow(() -> loader.refresh(null));
        }
    }
}
