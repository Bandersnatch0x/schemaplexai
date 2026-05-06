package com.schemaplexai.admin.service;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.system.entity.SfConfig;
import com.schemaplexai.system.mapper.SfConfigMapper;
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
class SystemConfigServiceTest {

    @Mock
    private SfConfigMapper configMapper;

    private SystemConfigService systemConfigService;

    @BeforeEach
    void setUp() {
        systemConfigService = spy(new SystemConfigService());
        ReflectionTestUtils.setField(systemConfigService, "baseMapper", configMapper);
    }

    // ------------------------------------------------------------------
    // getConfigDetail
    // ------------------------------------------------------------------

    @Test
    void getConfigDetail_notFound_throwsNotFound() {
        when(configMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> systemConfigService.getConfigDetail(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void getConfigDetail_success_returnsDto() {
        SfConfig config = new SfConfig();
        config.setId(1L);
        config.setConfigKey("app.name");
        config.setConfigValue("SchemaPlexAI");
        when(configMapper.selectById(1L)).thenReturn(config);

        var result = systemConfigService.getConfigDetail(1L);

        assertThat(result.getConfigKey()).isEqualTo("app.name");
        assertThat(result.getConfigValue()).isEqualTo("SchemaPlexAI");
    }

    // ------------------------------------------------------------------
    // getConfigByKey
    // ------------------------------------------------------------------

    @Test
    void getConfigByKey_notFound_throwsNotFound() {
        when(configMapper.selectByKeyAndTenantId("app.name", "tenant-1")).thenReturn(null);

        assertThatThrownBy(() -> systemConfigService.getConfigByKey("app.name", "tenant-1"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void getConfigByKey_success_returnsDto() {
        SfConfig config = new SfConfig();
        config.setConfigKey("app.name");
        config.setConfigValue("SchemaPlexAI");
        when(configMapper.selectByKeyAndTenantId("app.name", "tenant-1")).thenReturn(config);

        var result = systemConfigService.getConfigByKey("app.name", "tenant-1");

        assertThat(result.getConfigValue()).isEqualTo("SchemaPlexAI");
    }

    // ------------------------------------------------------------------
    // createConfig
    // ------------------------------------------------------------------

    @Test
    void createConfig_nullKey_throwsParamError() {
        var dto = new com.schemaplexai.admin.dto.SystemConfigDTO();

        assertThatThrownBy(() -> systemConfigService.createConfig(dto))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void createConfig_invalidKey_throwsParamError() {
        var dto = new com.schemaplexai.admin.dto.SystemConfigDTO();
        dto.setConfigKey("INVALID_KEY");

        assertThatThrownBy(() -> systemConfigService.createConfig(dto))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void createConfig_duplicateKey_throwsParamError() {
        var dto = new com.schemaplexai.admin.dto.SystemConfigDTO();
        dto.setConfigKey("app.name");
        dto.setTenantId("tenant-1");
        SfConfig existing = new SfConfig();
        when(configMapper.selectByKeyAndTenantId("app.name", "tenant-1")).thenReturn(existing);

        assertThatThrownBy(() -> systemConfigService.createConfig(dto))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void createConfig_success_savesConfig() {
        var dto = new com.schemaplexai.admin.dto.SystemConfigDTO();
        dto.setConfigKey("app.name");
        dto.setConfigValue("SchemaPlexAI");
        dto.setTenantId("tenant-1");
        when(configMapper.selectByKeyAndTenantId("app.name", "tenant-1")).thenReturn(null);
        doReturn(true).when(systemConfigService).save(any(SfConfig.class));

        var result = systemConfigService.createConfig(dto);

        assertThat(result.getConfigKey()).isEqualTo("app.name");
        assertThat(result.getConfigValue()).isEqualTo("SchemaPlexAI");
        verify(systemConfigService).save(any(SfConfig.class));
    }

    // ------------------------------------------------------------------
    // updateConfig
    // ------------------------------------------------------------------

    @Test
    void updateConfig_notFound_throwsNotFound() {
        when(configMapper.selectById(1L)).thenReturn(null);

        var dto = new com.schemaplexai.admin.dto.SystemConfigDTO();
        assertThatThrownBy(() -> systemConfigService.updateConfig(1L, dto))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void updateConfig_success_updatesValue() {
        SfConfig config = new SfConfig();
        config.setId(1L);
        config.setConfigKey("app.name");
        config.setConfigValue("Old");
        when(configMapper.selectById(1L)).thenReturn(config);

        var dto = new com.schemaplexai.admin.dto.SystemConfigDTO();
        dto.setConfigValue("New");

        var result = systemConfigService.updateConfig(1L, dto);

        assertThat(result.getConfigValue()).isEqualTo("New");
        verify(configMapper).updateById(config);
    }

    // ------------------------------------------------------------------
    // deleteConfig
    // ------------------------------------------------------------------

    @Test
    void deleteConfig_notFound_throwsNotFound() {
        when(configMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> systemConfigService.deleteConfig(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void deleteConfig_success_deletes() {
        SfConfig config = new SfConfig();
        config.setId(1L);
        when(configMapper.selectById(1L)).thenReturn(config);
        doReturn(true).when(systemConfigService).removeById(1L);

        systemConfigService.deleteConfig(1L);

        verify(systemConfigService).removeById(1L);
    }

    // ------------------------------------------------------------------
    // isMaintenanceMode
    // ------------------------------------------------------------------

    @Test
    void isMaintenanceMode_true_whenConfigIsTrue() {
        SfConfig config = new SfConfig();
        config.setConfigValue("true");
        when(configMapper.selectByKeyAndTenantId("system.maintenance.mode", "tenant-1")).thenReturn(config);

        boolean result = systemConfigService.isMaintenanceMode("tenant-1");

        assertThat(result).isTrue();
    }

    @Test
    void isMaintenanceMode_false_whenConfigIsFalse() {
        SfConfig config = new SfConfig();
        config.setConfigValue("false");
        when(configMapper.selectByKeyAndTenantId("system.maintenance.mode", "tenant-1")).thenReturn(config);

        boolean result = systemConfigService.isMaintenanceMode("tenant-1");

        assertThat(result).isFalse();
    }

    @Test
    void isMaintenanceMode_false_whenConfigMissing() {
        when(configMapper.selectByKeyAndTenantId("system.maintenance.mode", "tenant-1")).thenReturn(null);

        boolean result = systemConfigService.isMaintenanceMode("tenant-1");

        assertThat(result).isFalse();
    }

    // ------------------------------------------------------------------
    // isFeatureEnabled
    // ------------------------------------------------------------------

    @Test
    void isFeatureEnabled_true_whenConfigIsTrue() {
        SfConfig config = new SfConfig();
        config.setConfigValue("true");
        when(configMapper.selectByKeyAndTenantId("system.feature.agent.enabled", "tenant-1")).thenReturn(config);

        boolean result = systemConfigService.isFeatureEnabled("agent", "tenant-1");

        assertThat(result).isTrue();
    }

    @Test
    void isFeatureEnabled_true_whenConfigMissing() {
        when(configMapper.selectByKeyAndTenantId("system.feature.agent.enabled", "tenant-1")).thenReturn(null);

        boolean result = systemConfigService.isFeatureEnabled("agent", "tenant-1");

        assertThat(result).isTrue();
    }

    @Test
    void isFeatureEnabled_false_whenConfigIsFalse() {
        SfConfig config = new SfConfig();
        config.setConfigValue("false");
        when(configMapper.selectByKeyAndTenantId("system.feature.agent.enabled", "tenant-1")).thenReturn(config);

        boolean result = systemConfigService.isFeatureEnabled("agent", "tenant-1");

        assertThat(result).isFalse();
    }
}
