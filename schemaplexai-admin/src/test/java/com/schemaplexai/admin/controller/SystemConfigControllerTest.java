package com.schemaplexai.admin.controller;

import com.schemaplexai.admin.dto.SystemConfigDTO;
import com.schemaplexai.admin.dto.SystemConfigQuery;
import com.schemaplexai.admin.service.SystemConfigService;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.model.dto.PageResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemConfigControllerTest {

    @Mock
    private SystemConfigService systemConfigService;

    @InjectMocks
    private SystemConfigController systemConfigController;

    @Test
    void page_returnsConfigs() {
        SystemConfigQuery query = new SystemConfigQuery();
        PageResult<SystemConfigDTO> pageResult = PageResult.of(Collections.emptyList(), 0L, 1L, 10L);
        when(systemConfigService.queryConfigs(query)).thenReturn(pageResult);

        Result<PageResult<SystemConfigDTO>> result = systemConfigController.page(query);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(pageResult);
    }

    @Test
    void getById_returnsConfig() {
        SystemConfigDTO dto = new SystemConfigDTO();
        dto.setId(1L);
        when(systemConfigService.getConfigDetail(1L)).thenReturn(dto);

        Result<SystemConfigDTO> result = systemConfigController.getById(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(dto);
    }

    @Test
    void getByKey_returnsConfig() {
        SystemConfigDTO dto = new SystemConfigDTO();
        dto.setId(1L);
        when(systemConfigService.getConfigByKey("key1", "tenant1")).thenReturn(dto);

        Result<SystemConfigDTO> result = systemConfigController.getByKey("key1", "tenant1");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(dto);
    }

    @Test
    void create_returnsConfig() {
        SystemConfigDTO dto = new SystemConfigDTO();
        dto.setId(1L);
        when(systemConfigService.createConfig(dto)).thenReturn(dto);

        Result<SystemConfigDTO> result = systemConfigController.create(dto);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(dto);
    }

    @Test
    void update_returnsConfig() {
        SystemConfigDTO dto = new SystemConfigDTO();
        dto.setId(1L);
        when(systemConfigService.updateConfig(1L, dto)).thenReturn(dto);

        Result<SystemConfigDTO> result = systemConfigController.update(1L, dto);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(dto);
    }

    @Test
    void delete_returnsSuccess() {
        Result<Void> result = systemConfigController.delete(1L);

        assertThat(result.getCode()).isEqualTo(200);
        verify(systemConfigService).deleteConfig(1L);
    }

    @Test
    void setMaintenanceMode_returnsSuccess() {
        Result<Void> result = systemConfigController.setMaintenanceMode(Map.of("enabled", true, "tenantId", "tenant1"));

        assertThat(result.getCode()).isEqualTo(200);
        verify(systemConfigService).setMaintenanceMode(true, "tenant1");
    }

    @Test
    void setFeatureFlag_returnsSuccess() {
        Result<Void> result = systemConfigController.setFeatureFlag(Map.of("featureKey", "agent", "enabled", true, "tenantId", "tenant1"));

        assertThat(result.getCode()).isEqualTo(200);
        verify(systemConfigService).setFeatureFlag("agent", true, "tenant1");
    }

    @Test
    void isMaintenanceMode_returnsStatus() {
        when(systemConfigService.isMaintenanceMode("tenant1")).thenReturn(true);

        Result<Map<String, Boolean>> result = systemConfigController.isMaintenanceMode("tenant1");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().get("enabled")).isTrue();
    }

    @Test
    void isFeatureEnabled_returnsStatus() {
        when(systemConfigService.isFeatureEnabled("agent", "tenant1")).thenReturn(true);

        Result<Map<String, Boolean>> result = systemConfigController.isFeatureEnabled("agent", "tenant1");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().get("enabled")).isTrue();
    }
}
