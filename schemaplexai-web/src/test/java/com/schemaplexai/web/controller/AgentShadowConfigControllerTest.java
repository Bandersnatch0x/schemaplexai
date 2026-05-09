package com.schemaplexai.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.schemaplexai.agent.config.service.AgentShadowConfigService;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.model.entity.agent.SfAgentShadowConfig;
import com.schemaplexai.web.controller.agent.AgentShadowConfigController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentShadowConfigControllerTest {

    @Mock
    private AgentShadowConfigService agentShadowConfigService;

    @InjectMocks
    private AgentShadowConfigController controller;

    @Test
    void pageList() {
        IPage<SfAgentShadowConfig> page = new Page<>();
        when(agentShadowConfigService.pageList(any())).thenReturn(page);
        Result<IPage<SfAgentShadowConfig>> result = controller.pageList(1, 20);
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void getById_found() {
        SfAgentShadowConfig config = new SfAgentShadowConfig();
        when(agentShadowConfigService.getById(1L)).thenReturn(config);
        Result<SfAgentShadowConfig> result = controller.getById(1L);
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void getByAgentId_found() {
        SfAgentShadowConfig config = new SfAgentShadowConfig();
        when(agentShadowConfigService.getByAgentId(1L)).thenReturn(config);
        Result<SfAgentShadowConfig> result = controller.getByAgentId(1L);
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void create_success() {
        SfAgentShadowConfig config = new SfAgentShadowConfig();
        when(agentShadowConfigService.save(config)).thenReturn(true);
        Result<Boolean> result = controller.create(config);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void update_success() {
        SfAgentShadowConfig config = new SfAgentShadowConfig();
        when(agentShadowConfigService.updateById(config)).thenReturn(true);
        Result<Boolean> result = controller.update(1L, config);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void toggleEnabled() {
        Result<Void> result = controller.toggleEnabled(1L, true);
        assertThat(result.getCode()).isEqualTo(200);
        verify(agentShadowConfigService).toggleEnabled(1L, true);
    }

    @Test
    void delete_success() {
        when(agentShadowConfigService.removeById(1L)).thenReturn(true);
        Result<Boolean> result = controller.delete(1L);
        assertThat(result.getData()).isTrue();
    }
}
