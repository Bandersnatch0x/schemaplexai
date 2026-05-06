package com.schemaplexai.agent.config.manifest;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.schemaplexai.agent.config.entity.SfAgent;
import com.schemaplexai.agent.config.entity.SfAgentConfig;
import com.schemaplexai.agent.config.entity.SfAgentToolBinding;
import com.schemaplexai.agent.config.mapper.SfAgentConfigMapper;
import com.schemaplexai.agent.config.mapper.SfAgentMapper;
import com.schemaplexai.agent.config.service.AgentConfigService;
import com.schemaplexai.common.manifest.AgentsManifest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentsManifestLoaderTest {

    @Mock
    private SfAgentMapper agentMapper;

    @Mock
    private SfAgentConfigMapper agentConfigMapper;

    @Mock
    private AgentConfigService agentConfigService;

    @InjectMocks
    private AgentsManifestLoader loader;

    private AgentsManifest fullManifest;

    @BeforeEach
    void setup() {
        fullManifest = new AgentsManifest(
                "code-reviewer",
                "Reviews code",
                "claude-sonnet-4-6",
                "review",
                8L,
                4L,
                32000L,
                4000L,
                0.3,
                "single",
                List.of(
                        new AgentsManifest.ToolBinding("file_read", "builtin", null),
                        new AgentsManifest.ToolBinding("grep", "builtin", "{\"k\":\"v\"}")
                ),
                "You are a senior code reviewer."
        );
    }

    @Test
    void shouldCreateNewAgentWhenNameNotFound() {
        when(agentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        // emulate ID assignment after insert
        doAnswer(inv -> {
            SfAgent created = inv.getArgument(0, SfAgent.class);
            created.setId(42L);
            return null;
        }).when(agentConfigService).createAgent(any(SfAgent.class));

        Long resultId = loader.loadFromManifest(fullManifest, "tenant-x");

        assertThat(resultId).isEqualTo(42L);
        ArgumentCaptor<SfAgent> agentCap = ArgumentCaptor.forClass(SfAgent.class);
        verify(agentConfigService).createAgent(agentCap.capture());
        SfAgent saved = agentCap.getValue();
        assertThat(saved.getName()).isEqualTo("code-reviewer");
        assertThat(saved.getType()).isEqualTo("review");
        assertThat(saved.getDescription()).isEqualTo("Reviews code");
        verify(agentConfigService, never()).updateAgent(any());
    }

    @Test
    void shouldUpdateExistingAgentWhenNameFound() {
        SfAgent existing = new SfAgent();
        existing.setId(7L);
        existing.setName("code-reviewer");
        existing.setType("oldtype");
        when(agentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        Long resultId = loader.loadFromManifest(fullManifest, "tenant-x");

        assertThat(resultId).isEqualTo(7L);
        ArgumentCaptor<SfAgent> agentCap = ArgumentCaptor.forClass(SfAgent.class);
        verify(agentConfigService).updateAgent(agentCap.capture());
        SfAgent updated = agentCap.getValue();
        assertThat(updated.getId()).isEqualTo(7L);
        assertThat(updated.getType()).isEqualTo("review");
        assertThat(updated.getDescription()).isEqualTo("Reviews code");
        verify(agentConfigService, never()).createAgent(any());
    }

    @Test
    void shouldDefaultTypeToGeneralWhenAbsent() {
        AgentsManifest noType = new AgentsManifest(
                "minimal", null, null, null, null, null, null, null, null, null,
                List.of(), "Body"
        );
        when(agentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        doAnswer(inv -> {
            inv.getArgument(0, SfAgent.class).setId(1L);
            return null;
        }).when(agentConfigService).createAgent(any());

        loader.loadFromManifest(noType, "tenant-x");

        ArgumentCaptor<SfAgent> cap = ArgumentCaptor.forClass(SfAgent.class);
        verify(agentConfigService).createAgent(cap.capture());
        assertThat(cap.getValue().getType()).isEqualTo("general");
    }

    @Test
    void shouldSaveAgentConfigFromManifest() {
        when(agentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        doAnswer(inv -> {
            inv.getArgument(0, SfAgent.class).setId(99L);
            return null;
        }).when(agentConfigService).createAgent(any());
        when(agentConfigMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        loader.loadFromManifest(fullManifest, "tenant-x");

        ArgumentCaptor<SfAgentConfig> cap = ArgumentCaptor.forClass(SfAgentConfig.class);
        verify(agentConfigService).saveAgentConfig(cap.capture());
        SfAgentConfig cfg = cap.getValue();
        assertThat(cfg.getAgentId()).isEqualTo(99L);
        assertThat(cfg.getModelId()).isEqualTo("claude-sonnet-4-6");
        assertThat(cfg.getMaxRounds()).isEqualTo(8L);
        assertThat(cfg.getMaxTools()).isEqualTo(4L);
        assertThat(cfg.getMaxInputTokens()).isEqualTo(32000L);
        assertThat(cfg.getMaxOutputTokens()).isEqualTo(4000L);
        assertThat(cfg.getTemperature()).isEqualTo(0.3);
        assertThat(cfg.getExecutionMode()).isEqualTo("single");
        assertThat(cfg.getSystemPrompt()).isEqualTo("You are a senior code reviewer.");
    }

    @Test
    void shouldUpdateExistingConfigInsteadOfInserting() {
        SfAgent existing = new SfAgent();
        existing.setId(5L);
        when(agentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);
        SfAgentConfig prev = new SfAgentConfig();
        prev.setId(50L);
        prev.setAgentId(5L);
        when(agentConfigMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(prev);

        loader.loadFromManifest(fullManifest, "tenant-x");

        ArgumentCaptor<SfAgentConfig> cap = ArgumentCaptor.forClass(SfAgentConfig.class);
        verify(agentConfigService).saveAgentConfig(cap.capture());
        assertThat(cap.getValue().getId()).isEqualTo(50L);
    }

    @Test
    void shouldReplaceToolBindings() {
        when(agentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        doAnswer(inv -> {
            inv.getArgument(0, SfAgent.class).setId(11L);
            return null;
        }).when(agentConfigService).createAgent(any());

        loader.loadFromManifest(fullManifest, "tenant-x");

        ArgumentCaptor<List<SfAgentToolBinding>> cap = ArgumentCaptor.forClass(List.class);
        verify(agentConfigService).saveToolBindings(eq(11L), cap.capture());
        List<SfAgentToolBinding> bindings = cap.getValue();
        assertThat(bindings).hasSize(2);
        assertThat(bindings.get(0).getToolName()).isEqualTo("file_read");
        assertThat(bindings.get(0).getToolType()).isEqualTo("builtin");
        assertThat(bindings.get(0).getConfigJson()).isNull();
        assertThat(bindings.get(1).getToolName()).isEqualTo("grep");
        assertThat(bindings.get(1).getConfigJson()).contains("\"k\"");
    }

    @Test
    void shouldHandleManifestWithNoTools() {
        AgentsManifest empty = new AgentsManifest(
                "no-tools", null, null, null, null, null, null, null, null, null,
                List.of(), "Body"
        );
        when(agentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        doAnswer(inv -> {
            inv.getArgument(0, SfAgent.class).setId(1L);
            return null;
        }).when(agentConfigService).createAgent(any());

        loader.loadFromManifest(empty, "tenant-x");

        ArgumentCaptor<List<SfAgentToolBinding>> cap = ArgumentCaptor.forClass(List.class);
        verify(agentConfigService).saveToolBindings(anyLong(), cap.capture());
        assertThat(cap.getValue()).isEmpty();
    }

    @Test
    void shouldRejectNullManifest() {
        assertThatThrownBy(() -> loader.loadFromManifest(null, "tenant-x"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectBlankTenant() {
        assertThatThrownBy(() -> loader.loadFromManifest(fullManifest, "  "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> loader.loadFromManifest(fullManifest, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
