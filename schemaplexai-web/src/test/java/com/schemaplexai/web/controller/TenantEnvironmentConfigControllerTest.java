package com.schemaplexai.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.schemaplexai.agent.config.service.TenantEnvironmentConfigService;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.model.entity.config.TenantEnvironmentConfig;
import com.schemaplexai.web.controller.config.TenantEnvironmentConfigController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantEnvironmentConfigControllerTest {

    @Mock
    private TenantEnvironmentConfigService tenantEnvironmentConfigService;

    @InjectMocks
    private TenantEnvironmentConfigController controller;

    @Test
    void pageList() {
        IPage<TenantEnvironmentConfig> page = new Page<>();
        when(tenantEnvironmentConfigService.pageList(any())).thenReturn(page);
        Result<IPage<TenantEnvironmentConfig>> result = controller.pageList(1, 20);
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void getById_found() {
        TenantEnvironmentConfig config = new TenantEnvironmentConfig();
        when(tenantEnvironmentConfigService.getById(1L)).thenReturn(config);
        Result<TenantEnvironmentConfig> result = controller.getById(1L);
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void getByTenantId_found() {
        TenantEnvironmentConfig config = new TenantEnvironmentConfig();
        when(tenantEnvironmentConfigService.getByTenantId("t1")).thenReturn(config);
        Result<TenantEnvironmentConfig> result = controller.getByTenantId("t1");
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void create_success() {
        TenantEnvironmentConfig config = new TenantEnvironmentConfig();
        when(tenantEnvironmentConfigService.save(config)).thenReturn(true);
        Result<Boolean> result = controller.create(config);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void update_success() {
        TenantEnvironmentConfig config = new TenantEnvironmentConfig();
        when(tenantEnvironmentConfigService.updateById(config)).thenReturn(true);
        Result<Boolean> result = controller.update(1L, config);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void refreshCache_withTenantId() {
        TenantEnvironmentConfig config = new TenantEnvironmentConfig();
        config.setTenantId("t1");
        when(tenantEnvironmentConfigService.getById(1L)).thenReturn(config);
        Result<Void> result = controller.refreshCache(1L);
        assertThat(result.getCode()).isEqualTo(200);
        verify(tenantEnvironmentConfigService).refreshCache("t1");
    }

    @Test
    void refreshCache_nullConfig() {
        when(tenantEnvironmentConfigService.getById(1L)).thenReturn(null);
        Result<Void> result = controller.refreshCache(1L);
        assertThat(result.getCode()).isEqualTo(200);
        verifyNoMoreInteractions(tenantEnvironmentConfigService);
    }

    @Test
    void delete_success() {
        when(tenantEnvironmentConfigService.removeById(1L)).thenReturn(true);
        Result<Boolean> result = controller.delete(1L);
        assertThat(result.getData()).isTrue();
    }
}
