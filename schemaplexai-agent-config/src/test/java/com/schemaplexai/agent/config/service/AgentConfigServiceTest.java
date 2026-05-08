package com.schemaplexai.agent.config.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.schemaplexai.agent.config.entity.SfAgent;
import com.schemaplexai.agent.config.entity.SfAgentConfig;
import com.schemaplexai.agent.config.entity.SfAgentToolBinding;
import com.schemaplexai.agent.config.mapper.SfAgentConfigMapper;
import com.schemaplexai.agent.config.mapper.SfAgentMapper;
import com.schemaplexai.agent.config.mapper.SfAgentToolBindingMapper;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentConfigServiceTest {

    @Mock
    private SfAgentMapper agentMapper;

    @Mock
    private SfAgentConfigMapper agentConfigMapper;

    @Mock
    private SfAgentToolBindingMapper toolBindingMapper;

    @InjectMocks
    private AgentConfigService agentConfigService;

    // ========== getAgent ==========

    @Test
    void shouldReturnAgentWhenFound() {
        SfAgent agent = new SfAgent();
        agent.setId(1L);
        agent.setName("Test Agent");
        when(agentMapper.selectById(1L)).thenReturn(agent);

        SfAgent result = agentConfigService.getAgent(1L);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Agent");
    }

    @Test
    void shouldThrowWhenAgentNotFound() {
        when(agentMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> agentConfigService.getAgent(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.AGENT_NOT_FOUND.getCode());
    }

    // ========== listAgents ==========

    @Test
    void shouldListAllAgents() {
        SfAgent agent1 = new SfAgent();
        agent1.setId(1L);
        SfAgent agent2 = new SfAgent();
        agent2.setId(2L);
        when(agentMapper.selectList(null)).thenReturn(List.of(agent1, agent2));

        List<SfAgent> result = agentConfigService.listAgents();

        assertThat(result).hasSize(2);
    }

    // ========== createAgent ==========

    @Test
    void shouldCreateAgent() {
        SfAgent agent = new SfAgent();
        agent.setName("New Agent");
        when(agentMapper.insert(agent)).thenReturn(1);

        agentConfigService.createAgent(agent);

        verify(agentMapper).insert(agent);
    }

    // ========== updateAgent ==========

    @Test
    void shouldUpdateAgent() {
        SfAgent agent = new SfAgent();
        agent.setId(1L);
        agent.setName("Updated Agent");
        when(agentMapper.updateById(agent)).thenReturn(1);

        agentConfigService.updateAgent(agent);

        verify(agentMapper).updateById(agent);
    }

    // ========== deleteAgent ==========

    @Test
    void shouldDeleteAgent() {
        when(agentMapper.deleteById(1L)).thenReturn(1);

        agentConfigService.deleteAgent(1L);

        verify(agentMapper).deleteById(1L);
    }

    // ========== getAgentConfig ==========

    @Test
    void shouldGetAgentConfig() {
        SfAgentConfig config = new SfAgentConfig();
        config.setAgentId(1L);
        config.setSystemPrompt("Test prompt");
        when(agentConfigMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(config);

        SfAgentConfig result = agentConfigService.getAgentConfig(1L);

        assertThat(result).isNotNull();
        assertThat(result.getSystemPrompt()).isEqualTo("Test prompt");
    }

    @Test
    void shouldReturnNullWhenAgentConfigNotFound() {
        when(agentConfigMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        SfAgentConfig result = agentConfigService.getAgentConfig(1L);

        assertThat(result).isNull();
    }

    // ========== saveAgentConfig ==========

    @Test
    void shouldInsertAgentConfigWhenIdIsNull() {
        SfAgentConfig config = new SfAgentConfig();
        config.setAgentId(1L);
        config.setSystemPrompt("New prompt");
        when(agentConfigMapper.insert(config)).thenReturn(1);

        agentConfigService.saveAgentConfig(config);

        verify(agentConfigMapper).insert(config);
        verify(agentConfigMapper, never()).updateById(any());
    }

    @Test
    void shouldUpdateAgentConfigWhenIdExists() {
        SfAgentConfig config = new SfAgentConfig();
        config.setId(1L);
        config.setAgentId(1L);
        config.setSystemPrompt("Updated prompt");
        when(agentConfigMapper.updateById(config)).thenReturn(1);

        agentConfigService.saveAgentConfig(config);

        verify(agentConfigMapper).updateById(config);
        verify(agentConfigMapper, never()).insert(any());
    }

    // ========== listToolBindings ==========

    @Test
    void shouldListToolBindings() {
        SfAgentToolBinding binding = new SfAgentToolBinding();
        binding.setAgentId(1L);
        binding.setToolName("search");
        when(toolBindingMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(binding));

        List<SfAgentToolBinding> result = agentConfigService.listToolBindings(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getToolName()).isEqualTo("search");
    }

    // ========== saveToolBindings ==========

    @Test
    void shouldDeleteAndInsertToolBindings() {
        SfAgentToolBinding binding = new SfAgentToolBinding();
        binding.setToolName("search");
        List<SfAgentToolBinding> bindings = List.of(binding);
        when(toolBindingMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        when(toolBindingMapper.insert(binding)).thenReturn(1);

        agentConfigService.saveToolBindings(1L, bindings);

        verify(toolBindingMapper).delete(any(LambdaQueryWrapper.class));
        verify(toolBindingMapper).insert(binding);
        assertThat(binding.getAgentId()).isEqualTo(1L);
    }

    @Test
    void shouldDeleteOnlyWhenBindingsIsNull() {
        when(toolBindingMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

        agentConfigService.saveToolBindings(1L, null);

        verify(toolBindingMapper).delete(any(LambdaQueryWrapper.class));
        verify(toolBindingMapper, never()).insert(any());
    }

    @Test
    void shouldDeleteOnlyWhenBindingsIsEmpty() {
        when(toolBindingMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

        agentConfigService.saveToolBindings(1L, List.of());

        verify(toolBindingMapper).delete(any(LambdaQueryWrapper.class));
        verify(toolBindingMapper, never()).insert(any());
    }
}
