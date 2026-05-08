package com.schemaplexai.agent.config.controller;

import com.schemaplexai.agent.config.entity.SfAgent;
import com.schemaplexai.agent.config.entity.SfAgentConfig;
import com.schemaplexai.agent.config.entity.SfAgentToolBinding;
import com.schemaplexai.agent.config.service.AgentConfigService;
import com.schemaplexai.agent.config.service.ShadowConfigService;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.model.entity.agent.SfAgentShadowConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentConfigControllerTest {

    @Mock
    private AgentConfigService agentConfigService;

    @Mock
    private ShadowConfigService shadowConfigService;

    @InjectMocks
    private AgentConfigController agentConfigController;

    // ========== Agent CRUD ==========

    @Test
    void listAgents_returnsList() {
        SfAgent agent = new SfAgent();
        agent.setId(1L);
        agent.setName("Test Agent");
        when(agentConfigService.listAgents()).thenReturn(List.of(agent));

        Result<List<SfAgent>> result = agentConfigController.listAgents();

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getName()).isEqualTo("Test Agent");
    }

    @Test
    void getAgent_returnsAgent() {
        SfAgent agent = new SfAgent();
        agent.setId(1L);
        agent.setName("Test Agent");
        when(agentConfigService.getAgent(1L)).thenReturn(agent);

        Result<SfAgent> result = agentConfigController.getAgent(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getName()).isEqualTo("Test Agent");
    }

    @Test
    void createAgent_returnsSuccess() {
        SfAgent agent = new SfAgent();
        agent.setName("New Agent");
        doNothing().when(agentConfigService).createAgent(agent);

        Result<Void> result = agentConfigController.createAgent(agent);

        assertThat(result.getCode()).isEqualTo(200);
        verify(agentConfigService).createAgent(agent);
    }

    @Test
    void updateAgent_returnsSuccess() {
        SfAgent agent = new SfAgent();
        agent.setName("Updated Agent");
        doNothing().when(agentConfigService).updateAgent(agent);

        Result<Void> result = agentConfigController.updateAgent(1L, agent);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(agent.getId()).isEqualTo(1L);
        verify(agentConfigService).updateAgent(agent);
    }

    @Test
    void deleteAgent_returnsSuccess() {
        doNothing().when(agentConfigService).deleteAgent(1L);

        Result<Void> result = agentConfigController.deleteAgent(1L);

        assertThat(result.getCode()).isEqualTo(200);
        verify(agentConfigService).deleteAgent(1L);
    }

    // ========== Agent Config ==========

    @Test
    void getAgentConfig_returnsConfig() {
        SfAgentConfig config = new SfAgentConfig();
        config.setAgentId(1L);
        config.setSystemPrompt("You are a helpful assistant");
        when(agentConfigService.getAgentConfig(1L)).thenReturn(config);

        Result<SfAgentConfig> result = agentConfigController.getAgentConfig(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getSystemPrompt()).isEqualTo("You are a helpful assistant");
    }

    @Test
    void saveAgentConfig_insert_returnsSuccess() {
        SfAgentConfig config = new SfAgentConfig();
        config.setSystemPrompt("New prompt");
        doNothing().when(agentConfigService).saveAgentConfig(config);

        Result<Void> result = agentConfigController.saveAgentConfig(1L, config);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(config.getAgentId()).isEqualTo(1L);
        verify(agentConfigService).saveAgentConfig(config);
    }

    // ========== Tool Bindings ==========

    @Test
    void listToolBindings_returnsList() {
        SfAgentToolBinding binding = new SfAgentToolBinding();
        binding.setAgentId(1L);
        binding.setToolName("search");
        when(agentConfigService.listToolBindings(1L)).thenReturn(List.of(binding));

        Result<List<SfAgentToolBinding>> result = agentConfigController.listToolBindings(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getToolName()).isEqualTo("search");
    }

    @Test
    void saveToolBindings_returnsSuccess() {
        SfAgentToolBinding binding = new SfAgentToolBinding();
        binding.setToolName("search");
        List<SfAgentToolBinding> bindings = List.of(binding);
        doNothing().when(agentConfigService).saveToolBindings(1L, bindings);

        Result<Void> result = agentConfigController.saveToolBindings(1L, bindings);

        assertThat(result.getCode()).isEqualTo(200);
        verify(agentConfigService).saveToolBindings(1L, bindings);
    }

    @Test
    void saveToolBindings_withNullBindings_returnsSuccess() {
        doNothing().when(agentConfigService).saveToolBindings(1L, null);

        Result<Void> result = agentConfigController.saveToolBindings(1L, null);

        assertThat(result.getCode()).isEqualTo(200);
        verify(agentConfigService).saveToolBindings(1L, null);
    }

    // ========== Shadow Config ==========

    @Test
    void listShadowConfigs_returnsList() {
        SfAgentShadowConfig config = new SfAgentShadowConfig();
        config.setAgentId(1L);
        config.setEnabled(true);
        when(shadowConfigService.listShadowConfigs()).thenReturn(List.of(config));

        Result<List<SfAgentShadowConfig>> result = agentConfigController.listShadowConfigs();

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getEnabled()).isTrue();
    }

    @Test
    void getShadowConfigByAgentId_returnsConfig() {
        SfAgentShadowConfig config = new SfAgentShadowConfig();
        config.setAgentId(1L);
        config.setFeedbackActionsJson("[{\"action\":\"approve\"}]");
        when(shadowConfigService.getByAgentId(1L)).thenReturn(config);

        Result<SfAgentShadowConfig> result = agentConfigController.getShadowConfigByAgentId(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getFeedbackActionsJson()).isEqualTo("[{\"action\":\"approve\"}]");
    }

    @Test
    void createShadowConfig_returnsSuccess() {
        SfAgentShadowConfig config = new SfAgentShadowConfig();
        config.setAgentId(1L);
        doNothing().when(shadowConfigService).createShadowConfig(config);

        Result<Void> result = agentConfigController.createShadowConfig(config);

        assertThat(result.getCode()).isEqualTo(200);
        verify(shadowConfigService).createShadowConfig(config);
    }

    @Test
    void updateShadowConfig_returnsSuccess() {
        SfAgentShadowConfig config = new SfAgentShadowConfig();
        config.setAgentId(1L);
        doNothing().when(shadowConfigService).updateShadowConfig(config);

        Result<Void> result = agentConfigController.updateShadowConfig(1L, config);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(config.getId()).isEqualTo(1L);
        verify(shadowConfigService).updateShadowConfig(config);
    }

    @Test
    void deleteShadowConfig_returnsSuccess() {
        doNothing().when(shadowConfigService).deleteShadowConfig(1L);

        Result<Void> result = agentConfigController.deleteShadowConfig(1L);

        assertThat(result.getCode()).isEqualTo(200);
        verify(shadowConfigService).deleteShadowConfig(1L);
    }
}
