package com.schemaplexai.integration.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.integration.entity.SfApiGatewayConfig;
import com.schemaplexai.integration.entity.SfIntegration;
import com.schemaplexai.integration.entity.SfMcpServer;
import com.schemaplexai.integration.entity.SfSkill;
import com.schemaplexai.integration.service.ApiGatewayService;
import com.schemaplexai.integration.service.IntegrationService;
import com.schemaplexai.integration.service.McpServerService;
import com.schemaplexai.integration.service.SkillService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IntegrationControllerTest {

    @Mock private ApiGatewayService apiGatewayService;
    @InjectMocks private ApiGatewayController apiGatewayController;

    @Mock private IntegrationService integrationService;
    @InjectMocks private IntegrationController integrationController;

    @Mock private McpServerService mcpServerService;
    @InjectMocks private McpServerController mcpServerController;

    @Mock private SkillService skillService;
    @InjectMocks private SkillController skillController;

    // ApiGatewayController
    @Test
    void apiGateway_create() {
        SfApiGatewayConfig config = new SfApiGatewayConfig(); config.setId(1L);
        Result<Long> result = apiGatewayController.create(config);
        assertThat(result.getData()).isEqualTo(1L);
    }

    @Test
    void apiGateway_update() {
        SfApiGatewayConfig config = new SfApiGatewayConfig();
        when(apiGatewayService.updateById(config)).thenReturn(true);
        Result<Boolean> result = apiGatewayController.update(1L, config);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void apiGateway_delete() {
        when(apiGatewayService.removeById(1L)).thenReturn(true);
        Result<Boolean> result = apiGatewayController.delete(1L);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void apiGateway_get_found() {
        SfApiGatewayConfig config = new SfApiGatewayConfig(); config.setId(1L);
        when(apiGatewayService.getById(1L)).thenReturn(config);
        Result<SfApiGatewayConfig> result = apiGatewayController.get(1L);
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void apiGateway_get_notFound() {
        when(apiGatewayService.getById(1L)).thenReturn(null);
        Result<SfApiGatewayConfig> result = apiGatewayController.get(1L);
        assertThat(result.getCode()).isEqualTo(404);
    }

    @Test
    void apiGateway_list() {
        when(apiGatewayService.list()).thenReturn(Collections.emptyList());
        Result<?> result = apiGatewayController.list();
        assertThat(result.getCode()).isEqualTo(200);
    }

    // IntegrationController
    @Test
    void integration_create() {
        SfIntegration integration = new SfIntegration(); integration.setId(1L);
        Result<Long> result = integrationController.create(integration);
        assertThat(result.getData()).isEqualTo(1L);
    }

    @Test
    void integration_update() {
        SfIntegration integration = new SfIntegration();
        when(integrationService.updateById(integration)).thenReturn(true);
        Result<Boolean> result = integrationController.update(1L, integration);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void integration_delete() {
        when(integrationService.removeById(1L)).thenReturn(true);
        Result<Boolean> result = integrationController.delete(1L);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void integration_get_found() {
        SfIntegration integration = new SfIntegration(); integration.setId(1L);
        when(integrationService.getById(1L)).thenReturn(integration);
        Result<SfIntegration> result = integrationController.get(1L);
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void integration_get_notFound() {
        when(integrationService.getById(1L)).thenReturn(null);
        Result<SfIntegration> result = integrationController.get(1L);
        assertThat(result.getCode()).isEqualTo(404);
    }

    @Test
    void integration_list() {
        when(integrationService.list()).thenReturn(Collections.emptyList());
        Result<?> result = integrationController.list();
        assertThat(result.getCode()).isEqualTo(200);
    }

    // McpServerController
    @Test
    void mcpServer_create() {
        SfMcpServer server = new SfMcpServer(); server.setId(1L);
        Result<Long> result = mcpServerController.create(server);
        assertThat(result.getData()).isEqualTo(1L);
    }

    @Test
    void mcpServer_update() {
        SfMcpServer server = new SfMcpServer();
        when(mcpServerService.updateById(server)).thenReturn(true);
        Result<Boolean> result = mcpServerController.update(1L, server);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void mcpServer_delete() {
        when(mcpServerService.removeById(1L)).thenReturn(true);
        Result<Boolean> result = mcpServerController.delete(1L);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void mcpServer_get_found() {
        SfMcpServer server = new SfMcpServer(); server.setId(1L);
        when(mcpServerService.getById(1L)).thenReturn(server);
        Result<SfMcpServer> result = mcpServerController.get(1L);
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void mcpServer_get_notFound() {
        when(mcpServerService.getById(1L)).thenReturn(null);
        Result<SfMcpServer> result = mcpServerController.get(1L);
        assertThat(result.getCode()).isEqualTo(404);
    }

    @Test
    void mcpServer_list() {
        when(mcpServerService.list()).thenReturn(Collections.emptyList());
        Result<?> result = mcpServerController.list();
        assertThat(result.getCode()).isEqualTo(200);
    }

    // SkillController
    @Test
    void skill_create() {
        SfSkill skill = new SfSkill(); skill.setId(1L);
        Result<Long> result = skillController.create(skill);
        assertThat(result.getData()).isEqualTo(1L);
    }

    @Test
    void skill_update() {
        SfSkill skill = new SfSkill();
        when(skillService.updateById(skill)).thenReturn(true);
        Result<Boolean> result = skillController.update(1L, skill);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void skill_delete() {
        when(skillService.removeById(1L)).thenReturn(true);
        Result<Boolean> result = skillController.delete(1L);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void skill_get_found() {
        SfSkill skill = new SfSkill(); skill.setId(1L);
        when(skillService.getById(1L)).thenReturn(skill);
        Result<SfSkill> result = skillController.get(1L);
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void skill_get_notFound() {
        when(skillService.getById(1L)).thenReturn(null);
        Result<SfSkill> result = skillController.get(1L);
        assertThat(result.getCode()).isEqualTo(404);
    }

    @Test
    void skill_list() {
        when(skillService.list()).thenReturn(Collections.emptyList());
        Result<?> result = skillController.list();
        assertThat(result.getCode()).isEqualTo(200);
    }
}
