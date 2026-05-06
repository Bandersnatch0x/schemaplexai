package com.schemaplexai.agent.config.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.schemaplexai.agent.engine.config.SecurityPolicyLoader;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.dao.mapper.TenantEnvironmentConfigMapper;
import com.schemaplexai.model.entity.config.TenantEnvironmentConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantEnvironmentConfigServiceImplTest {

    @Mock
    private TenantEnvironmentConfigMapper tenantEnvironmentConfigMapper;

    @Mock
    private SecurityPolicyLoader securityPolicyLoader;

    @InjectMocks
    private TenantEnvironmentConfigServiceImpl tenantEnvironmentConfigService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tenantEnvironmentConfigService, "baseMapper", tenantEnvironmentConfigMapper);
    }

    @Test
    void shouldGetByTenantId() {
        TenantEnvironmentConfig config = new TenantEnvironmentConfig();
        config.setTenantId("tenant-1");
        config.setEnvironment("prod");
        config.setSecurityLevel("HIGH");
        when(tenantEnvironmentConfigMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(config);

        TenantEnvironmentConfig result = tenantEnvironmentConfigService.getByTenantId("tenant-1");

        assertThat(result).isNotNull();
        assertThat(result.getTenantId()).isEqualTo("tenant-1");
        assertThat(result.getEnvironment()).isEqualTo("prod");
        assertThat(result.getSecurityLevel()).isEqualTo("HIGH");
    }

    @Test
    void shouldReturnNullWhenNotFound() {
        when(tenantEnvironmentConfigMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        TenantEnvironmentConfig result = tenantEnvironmentConfigService.getByTenantId("non-existent");

        assertThat(result).isNull();
    }

    @Test
    void shouldCreateConfig() {
        TenantEnvironmentConfig config = new TenantEnvironmentConfig();
        config.setTenantId("tenant-1");
        config.setEnvironment("dev");
        config.setSecurityLevel("LOW");
        when(tenantEnvironmentConfigMapper.insert(any(TenantEnvironmentConfig.class))).thenReturn(1);

        boolean result = tenantEnvironmentConfigService.save(config);

        assertThat(result).isTrue();
        verify(tenantEnvironmentConfigMapper).insert(config);
    }

    @Test
    void shouldRejectCreateWhenTenantIdMissing() {
        TenantEnvironmentConfig config = new TenantEnvironmentConfig();
        config.setEnvironment("dev");

        assertThatThrownBy(() -> tenantEnvironmentConfigService.save(config))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void shouldUpdateConfig() {
        TenantEnvironmentConfig existing = new TenantEnvironmentConfig();
        existing.setId(1L);
        existing.setTenantId("tenant-1");
        existing.setEnvironment("dev");

        TenantEnvironmentConfig update = new TenantEnvironmentConfig();
        update.setId(1L);
        update.setTenantId("tenant-1");
        update.setEnvironment("prod");

        when(tenantEnvironmentConfigMapper.selectById(1L)).thenReturn(existing);
        when(tenantEnvironmentConfigMapper.updateById(any(TenantEnvironmentConfig.class))).thenReturn(1);

        boolean result = tenantEnvironmentConfigService.updateById(update);

        assertThat(result).isTrue();
        verify(tenantEnvironmentConfigMapper).updateById(update);
    }

    @Test
    void shouldThrowNotFoundWhenUpdateWithInvalidId() {
        TenantEnvironmentConfig update = new TenantEnvironmentConfig();
        update.setId(999L);
        when(tenantEnvironmentConfigMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> tenantEnvironmentConfigService.updateById(update))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void shouldRefreshCache() {
        doNothing().when(securityPolicyLoader).refresh("tenant-1");

        tenantEnvironmentConfigService.refreshCache("tenant-1");

        verify(securityPolicyLoader).refresh("tenant-1");
    }
}
