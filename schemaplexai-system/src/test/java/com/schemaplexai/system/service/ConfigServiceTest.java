package com.schemaplexai.system.service;

import com.schemaplexai.system.entity.SfConfig;
import com.schemaplexai.system.mapper.SfConfigMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfigServiceTest {

    @Mock
    private SfConfigMapper configMapper;

    private ConfigService configService;

    @BeforeEach
    void setUp() {
        configService = new ConfigService();
        ReflectionTestUtils.setField(configService, "baseMapper", configMapper);
    }

    @Test
    void getConfigValue_returnsValueWhenFound() {
        SfConfig config = new SfConfig();
        config.setConfigKey("api.key");
        config.setConfigValue("secret123");
        config.setTenantId("tenant-1");
        when(configMapper.selectByKeyAndTenantId("api.key", "tenant-1")).thenReturn(config);

        String result = configService.getConfigValue("api.key", "tenant-1");

        assertThat(result).isEqualTo("secret123");
    }

    @Test
    void getConfigValue_returnsNullWhenNotFound() {
        when(configMapper.selectByKeyAndTenantId("missing.key", "tenant-1")).thenReturn(null);

        String result = configService.getConfigValue("missing.key", "tenant-1");

        assertThat(result).isNull();
    }

    @Test
    void getConfigValue_returnsNullWhenConfigValueIsNull() {
        SfConfig config = new SfConfig();
        config.setConfigKey("empty.key");
        config.setConfigValue(null);
        when(configMapper.selectByKeyAndTenantId("empty.key", "tenant-1")).thenReturn(config);

        String result = configService.getConfigValue("empty.key", "tenant-1");

        assertThat(result).isNull();
    }

    @Test
    void getById_returnsConfig() {
        SfConfig config = new SfConfig();
        config.setId(1L);
        config.setConfigKey("test.key");
        when(configMapper.selectById(1L)).thenReturn(config);

        SfConfig result = configService.getById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getConfigKey()).isEqualTo("test.key");
    }

    @Test
    void save_success_returnsTrue() {
        SfConfig config = new SfConfig();
        config.setConfigKey("new.key");
        config.setConfigValue("new.value");
        when(configMapper.insert(any(SfConfig.class))).thenReturn(1);

        boolean result = configService.save(config);

        assertThat(result).isTrue();
        verify(configMapper).insert(config);
    }

    @Test
    void updateById_success_returnsTrue() {
        SfConfig config = new SfConfig();
        config.setId(1L);
        config.setConfigValue("updated.value");
        when(configMapper.updateById(any(SfConfig.class))).thenReturn(1);

        boolean result = configService.updateById(config);

        assertThat(result).isTrue();
        verify(configMapper).updateById(config);
    }

}
