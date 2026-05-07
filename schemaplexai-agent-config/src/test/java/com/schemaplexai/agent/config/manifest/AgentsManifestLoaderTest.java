package com.schemaplexai.agent.config.manifest;

import com.schemaplexai.agent.config.entity.SfAgent;
import com.schemaplexai.agent.config.entity.SfAgentConfig;
import com.schemaplexai.agent.config.entity.SfAgentToolBinding;
import com.schemaplexai.agent.config.mapper.SfAgentConfigMapper;
import com.schemaplexai.agent.config.mapper.SfAgentMapper;
import com.schemaplexai.agent.config.mapper.SfAgentToolBindingMapper;
import com.schemaplexai.common.manifest.AgentsManifest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentsManifestLoaderTest {

    @Mock
    private SfAgentMapper agentMapper;

    @Mock
    private SfAgentConfigMapper agentConfigMapper;

    @Mock
    private SfAgentToolBindingMapper toolBindingMapper;

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
        when(agentMapper.findByNameAndTenant("code-reviewer", "tenant-x")).thenReturn(null);
        doAnswer(inv -> {
            SfAgent created = inv.getArgument(0, SfAgent.class);
            created.setId(42L);
            return null;
        }).when(agentMapper).insert(any(SfAgent.class));

        Long resultId = loader.loadFromManifest(fullManifest, "tenant-x");

        assertThat(resultId).isEqualTo(42L);
        ArgumentCaptor<SfAgent> agentCap = ArgumentCaptor.forClass(SfAgent.class);
        verify(agentMapper).insert(agentCap.capture());
        SfAgent saved = agentCap.getValue();
        assertThat(saved.getName()).isEqualTo("code-reviewer");
        assertThat(saved.getType()).isEqualTo("review");
        assertThat(saved.getDescription()).isEqualTo("Reviews code");
        verify(agentMapper, never()).updateById(any());
    }

    @Test
    void shouldUpdateExistingAgentWhenNameFound() {
        SfAgent existing = new SfAgent();
        existing.setId(7L);
        existing.setName("code-reviewer");
        existing.setType("oldtype");
        when(agentMapper.findByNameAndTenant("code-reviewer", "tenant-x")).thenReturn(existing);

        Long resultId = loader.loadFromManifest(fullManifest, "tenant-x");

        assertThat(resultId).isEqualTo(7L);
        ArgumentCaptor<SfAgent> agentCap = ArgumentCaptor.forClass(SfAgent.class);
        verify(agentMapper).updateById(agentCap.capture());
        SfAgent updated = agentCap.getValue();
        assertThat(updated.getId()).isEqualTo(7L);
        assertThat(updated.getType()).isEqualTo("review");
        assertThat(updated.getDescription()).isEqualTo("Reviews code");
        verify(agentMapper, never()).insert(any());
    }

    @Test
    void shouldDefaultTypeToGeneralWhenAbsent() {
        AgentsManifest noType = new AgentsManifest(
                "minimal", null, null, null, null, null, null, null, null, null,
                List.of(), "Body"
        );
        when(agentMapper.findByNameAndTenant("minimal", "tenant-x")).thenReturn(null);
        doAnswer(inv -> {
            inv.getArgument(0, SfAgent.class).setId(1L);
            return null;
        }).when(agentMapper).insert(any(SfAgent.class));

        loader.loadFromManifest(noType, "tenant-x");

        ArgumentCaptor<SfAgent> cap = ArgumentCaptor.forClass(SfAgent.class);
        verify(agentMapper).insert(cap.capture());
        assertThat(cap.getValue().getType()).isEqualTo("general");
    }

    @Test
    void shouldSaveAgentConfigFromManifest() {
        when(agentMapper.findByNameAndTenant("code-reviewer", "tenant-x")).thenReturn(null);
        doAnswer(inv -> {
            inv.getArgument(0, SfAgent.class).setId(99L);
            return null;
        }).when(agentMapper).insert(any(SfAgent.class));
        when(agentConfigMapper.selectOne(any())).thenReturn(null);

        loader.loadFromManifest(fullManifest, "tenant-x");

        ArgumentCaptor<SfAgentConfig> cap = ArgumentCaptor.forClass(SfAgentConfig.class);
        verify(agentConfigMapper).insert(cap.capture());
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
        when(agentMapper.findByNameAndTenant("code-reviewer", "tenant-x")).thenReturn(existing);
        SfAgentConfig prev = new SfAgentConfig();
        prev.setId(50L);
        prev.setAgentId(5L);
        when(agentConfigMapper.selectOne(any())).thenReturn(prev);

        loader.loadFromManifest(fullManifest, "tenant-x");

        ArgumentCaptor<SfAgentConfig> cap = ArgumentCaptor.forClass(SfAgentConfig.class);
        verify(agentConfigMapper).updateById(cap.capture());
        assertThat(cap.getValue().getId()).isEqualTo(50L);
    }

    @Test
    void shouldReplaceToolBindings() {
        when(agentMapper.findByNameAndTenant("code-reviewer", "tenant-x")).thenReturn(null);
        doAnswer(inv -> {
            inv.getArgument(0, SfAgent.class).setId(11L);
            return null;
        }).when(agentMapper).insert(any(SfAgent.class));

        loader.loadFromManifest(fullManifest, "tenant-x");

        verify(toolBindingMapper).delete(any());
        ArgumentCaptor<SfAgentToolBinding> cap = ArgumentCaptor.forClass(SfAgentToolBinding.class);
        verify(toolBindingMapper, times(2)).insert(cap.capture());
        List<SfAgentToolBinding> bindings = cap.getAllValues();
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
        when(agentMapper.findByNameAndTenant("no-tools", "tenant-x")).thenReturn(null);
        doAnswer(inv -> {
            inv.getArgument(0, SfAgent.class).setId(1L);
            return null;
        }).when(agentMapper).insert(any(SfAgent.class));

        loader.loadFromManifest(empty, "tenant-x");

        verify(toolBindingMapper).delete(any());
        verify(toolBindingMapper, never()).insert(any());
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

    // --- load(Path, String) tests ---

    @Test
    void shouldReturnEmptyReportForNullRepoRoot() {
        LoadReport report = loader.load(null, "tenant-x");
        assertThat(report.results()).isEmpty();
        assertThat(report.okCount()).isEqualTo(0);
        assertThat(report.failedCount()).isEqualTo(0);
    }

    @Test
    void shouldReturnEmptyReportForNonExistentDir() {
        LoadReport report = loader.load(Path.of("/does/not/exist"), "tenant-x");
        assertThat(report.results()).isEmpty();
    }

    @Test
    void shouldDiscoverAndLoadRootAgentsMd(@TempDir Path repoRoot) throws Exception {
        Files.writeString(repoRoot.resolve("AGENTS.md"), """
                ---
                name: root-agent
                ---
                System prompt.
                """);
        when(agentMapper.findByNameAndTenant("root-agent", "tenant-x")).thenReturn(null);
        doAnswer(inv -> {
            inv.getArgument(0, SfAgent.class).setId(100L);
            return null;
        }).when(agentMapper).insert(any(SfAgent.class));

        LoadReport report = loader.load(repoRoot, "tenant-x");

        assertThat(report.okCount()).isEqualTo(1);
        assertThat(report.failedCount()).isEqualTo(0);
        assertThat(report.results().get(0).name()).isEqualTo("root-agent");
        assertThat(report.results().get(0).agentId()).isEqualTo(100L);
    }

    @Test
    void shouldDiscoverDotAgentsDir(@TempDir Path repoRoot) throws Exception {
        Path dotAgents = Files.createDirectory(repoRoot.resolve(".agents"));
        Files.writeString(dotAgents.resolve("reviewer.md"), """
                ---
                name: reviewer
                ---
                Prompt.
                """);
        when(agentMapper.findByNameAndTenant("reviewer", "tenant-x")).thenReturn(null);
        doAnswer(inv -> {
            inv.getArgument(0, SfAgent.class).setId(200L);
            return null;
        }).when(agentMapper).insert(any(SfAgent.class));

        LoadReport report = loader.load(repoRoot, "tenant-x");

        assertThat(report.okCount()).isEqualTo(1);
        assertThat(report.results().get(0).name()).isEqualTo("reviewer");
    }

    @Test
    void shouldDiscoverAgentsGlob(@TempDir Path repoRoot) throws Exception {
        Path agents = Files.createDirectories(repoRoot.resolve("agents"));
        Path sub = Files.createDirectory(agents.resolve("sub"));
        Files.writeString(agents.resolve("a.md"), """
                ---
                name: a
                ---
                Prompt A.
                """);
        Files.writeString(sub.resolve("b.md"), """
                ---
                name: b
                ---
                Prompt B.
                """);
        when(agentMapper.findByNameAndTenant(any(), eq("tenant-x"))).thenReturn(null);
        doAnswer(inv -> {
            inv.getArgument(0, SfAgent.class).setId(300L);
            return null;
        }).when(agentMapper).insert(any(SfAgent.class));

        LoadReport report = loader.load(repoRoot, "tenant-x");

        assertThat(report.okCount()).isEqualTo(2);
        List<String> names = report.results().stream()
                .map(LoadResult::name)
                .toList();
        assertThat(names).containsExactlyInAnyOrder("a", "b");
    }

    @Test
    void shouldNotFailWhenOneFileIsBroken(@TempDir Path repoRoot) throws Exception {
        Files.writeString(repoRoot.resolve("AGENTS.md"), """
                ---
                name: good
                ---
                OK.
                """);
        Path dotAgents = Files.createDirectory(repoRoot.resolve(".agents"));
        Files.writeString(dotAgents.resolve("bad.md"), "no frontmatter at all");
        when(agentMapper.findByNameAndTenant("good", "tenant-x")).thenReturn(null);
        doAnswer(inv -> {
            inv.getArgument(0, SfAgent.class).setId(1L);
            return null;
        }).when(agentMapper).insert(any(SfAgent.class));

        LoadReport report = loader.load(repoRoot, "tenant-x");

        assertThat(report.okCount()).isEqualTo(1);
        assertThat(report.failedCount()).isEqualTo(1);
    }

    @Test
    void shouldRejectNullTenantForLoad() {
        assertThatThrownBy(() -> loader.load(Path.of("."), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectBlankTenantForLoad() {
        assertThatThrownBy(() -> loader.load(Path.of("."), "   "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
