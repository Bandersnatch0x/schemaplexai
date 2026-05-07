package com.schemaplexai.agent.config.service;

import com.schemaplexai.agent.config.manifest.AgentsManifestLoader;
import com.schemaplexai.agent.config.manifest.LoadReport;
import com.schemaplexai.agent.config.manifest.LoadResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentConfigServiceManifestTest {

    @Mock
    private AgentsManifestLoader manifestLoader;

    @InjectMocks
    private AgentConfigService agentConfigService;

    @Test
    void shouldDelegateLoadFromManifestToLoader() {
        Path repoRoot = Path.of("/repo");
        LoadReport expected = new LoadReport(List.of(
                LoadResult.ok(Path.of("/repo/AGENTS.md"), 42L, "reviewer")
        ));
        when(manifestLoader.load(repoRoot, "tenant-x")).thenReturn(expected);

        LoadReport result = agentConfigService.loadFromManifest(repoRoot, "tenant-x");

        assertThat(result).isEqualTo(expected);
        verify(manifestLoader).load(repoRoot, "tenant-x");
    }

    @Test
    void shouldPassEmptyDirectoryToLoader() {
        Path repoRoot = Path.of("/empty");
        when(manifestLoader.load(repoRoot, "tenant-y")).thenReturn(new LoadReport(List.of()));

        LoadReport result = agentConfigService.loadFromManifest(repoRoot, "tenant-y");

        assertThat(result.okCount()).isEqualTo(0);
        assertThat(result.failedCount()).isEqualTo(0);
    }
}
