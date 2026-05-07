package com.schemaplexai.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.schemaplexai.agent.config.service.AgentShadowConfigService;
import com.schemaplexai.agent.config.service.TenantEnvironmentConfigService;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.model.entity.agent.SfAgentShadowConfig;
import com.schemaplexai.model.entity.config.TenantEnvironmentConfig;
import com.schemaplexai.web.controller.agent.AgentShadowConfigController;
import com.schemaplexai.web.controller.config.TenantEnvironmentConfigController;
import com.schemaplexai.web.controller.notification.NotificationController;
import com.schemaplexai.web.service.notification.NotificationService;
import com.schemaplexai.web.sse.AgentSseEmitter;
import com.schemaplexai.web.vo.notification.NotificationVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebControllerTest {

    @Mock
    private AgentShadowConfigService agentShadowConfigService;
    @InjectMocks
    private AgentShadowConfigController agentShadowConfigController;

    @Mock
    private TenantEnvironmentConfigService tenantEnvironmentConfigService;
    @InjectMocks
    private TenantEnvironmentConfigController tenantEnvironmentConfigController;

    @Mock
    private NotificationService notificationService;
    @InjectMocks
    private NotificationController notificationController;

    @Mock
    private AgentSseEmitter agentSseEmitter;
    @InjectMocks
    private SseController sseController;

    // AgentShadowConfigController tests
    @Test
    void agentShadow_pageList() {
        IPage<SfAgentShadowConfig> page = new Page<>();
        when(agentShadowConfigService.pageList(any())).thenReturn(page);
        Result<IPage<SfAgentShadowConfig>> result = agentShadowConfigController.pageList(1, 20);
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void agentShadow_getById() {
        SfAgentShadowConfig config = new SfAgentShadowConfig();
        when(agentShadowConfigService.getById(1L)).thenReturn(config);
        Result<SfAgentShadowConfig> result = agentShadowConfigController.getById(1L);
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void agentShadow_getByAgentId() {
        SfAgentShadowConfig config = new SfAgentShadowConfig();
        when(agentShadowConfigService.getByAgentId(1L)).thenReturn(config);
        Result<SfAgentShadowConfig> result = agentShadowConfigController.getByAgentId(1L);
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void agentShadow_create() {
        SfAgentShadowConfig config = new SfAgentShadowConfig();
        when(agentShadowConfigService.save(config)).thenReturn(true);
        Result<Boolean> result = agentShadowConfigController.create(config);
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void agentShadow_update() {
        SfAgentShadowConfig config = new SfAgentShadowConfig();
        when(agentShadowConfigService.updateById(config)).thenReturn(true);
        Result<Boolean> result = agentShadowConfigController.update(1L, config);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void agentShadow_toggleEnabled() {
        Result<Void> result = agentShadowConfigController.toggleEnabled(1L, true);
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void agentShadow_delete() {
        when(agentShadowConfigService.removeById(1L)).thenReturn(true);
        Result<Boolean> result = agentShadowConfigController.delete(1L);
        assertThat(result.getData()).isTrue();
    }

    // TenantEnvironmentConfigController tests
    @Test
    void tenantEnv_pageList() {
        IPage<TenantEnvironmentConfig> page = new Page<>();
        when(tenantEnvironmentConfigService.pageList(any())).thenReturn(page);
        Result<IPage<TenantEnvironmentConfig>> result = tenantEnvironmentConfigController.pageList(1, 20);
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void tenantEnv_getById() {
        TenantEnvironmentConfig config = new TenantEnvironmentConfig();
        when(tenantEnvironmentConfigService.getById(1L)).thenReturn(config);
        Result<TenantEnvironmentConfig> result = tenantEnvironmentConfigController.getById(1L);
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void tenantEnv_getByTenantId() {
        TenantEnvironmentConfig config = new TenantEnvironmentConfig();
        when(tenantEnvironmentConfigService.getByTenantId("t1")).thenReturn(config);
        Result<TenantEnvironmentConfig> result = tenantEnvironmentConfigController.getByTenantId("t1");
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void tenantEnv_create() {
        TenantEnvironmentConfig config = new TenantEnvironmentConfig();
        when(tenantEnvironmentConfigService.save(config)).thenReturn(true);
        Result<Boolean> result = tenantEnvironmentConfigController.create(config);
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void tenantEnv_update() {
        TenantEnvironmentConfig config = new TenantEnvironmentConfig();
        when(tenantEnvironmentConfigService.updateById(config)).thenReturn(true);
        Result<Boolean> result = tenantEnvironmentConfigController.update(1L, config);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void tenantEnv_refreshCache() {
        TenantEnvironmentConfig config = new TenantEnvironmentConfig();
        config.setTenantId("t1");
        when(tenantEnvironmentConfigService.getById(1L)).thenReturn(config);
        Result<Void> result = tenantEnvironmentConfigController.refreshCache(1L);
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void tenantEnv_refreshCache_nullConfig() {
        when(tenantEnvironmentConfigService.getById(1L)).thenReturn(null);
        Result<Void> result = tenantEnvironmentConfigController.refreshCache(1L);
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void tenantEnv_delete() {
        when(tenantEnvironmentConfigService.removeById(1L)).thenReturn(true);
        Result<Boolean> result = tenantEnvironmentConfigController.delete(1L);
        assertThat(result.getData()).isTrue();
    }

    // NotificationController tests
    @Test
    void notification_page() {
        IPage<NotificationVO> page = new Page<>();
        when(notificationService.pageQuery(100L, 1, 20, null)).thenReturn(page);
        Result<IPage<NotificationVO>> result = notificationController.page("100", 1, 20, null);
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void notification_markAsRead() {
        when(notificationService.markAsRead(1L, 100L)).thenReturn(true);
        Result<Boolean> result = notificationController.markAsRead("100", 1L);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void notification_markAllAsRead() {
        when(notificationService.markAllAsRead(100L)).thenReturn(5);
        Result<Integer> result = notificationController.markAllAsRead("100");
        assertThat(result.getData()).isEqualTo(5);
    }

    // SseController tests
    @Test
    void sse_subscribe() {
        SseEmitter emitter = new SseEmitter();
        when(agentSseEmitter.createEmitter("client1", "token")).thenReturn(emitter);
        SseEmitter result = sseController.subscribe("client1", "token");
        assertThat(result).isNotNull();
    }

    @Test
    void sse_sendEvent() {
        Result<Void> result = sseController.sendEvent("client1", "event", "data");
        assertThat(result.getCode()).isEqualTo(200);
    }
}
