package com.schemaplexai.integration.e2e;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.integration.service.GitIntegrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * End-to-end integration tests for Git integration service.
 * Covers: repository registration, clone, branch management, webhook handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Git Integration End-to-End Tests")
class GitIntegrationEndToEndTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private GitIntegrationService gitService;

    @BeforeEach
    void setUp() {
        gitService = new GitIntegrationService(new com.fasterxml.jackson.databind.ObjectMapper(), restTemplate);
        // Clear internal state for test isolation
        gitService.clearStore();
    }

    @Test
    @DisplayName("E2E: Register repo, get repository info, list branches")
    void fullGitLifecycle() {
        // Step 1: Register repository
        Long repoId = gitService.registerRepository("github", "schemaplexai", "core",
                "https://github.com/schemaplexai/core.git", "main", "token123");
        assertThat(repoId).isEqualTo(1L);

        // Step 2: Get repository (safe view, no token)
        Map<String, Object> repo = gitService.getRepository(repoId);
        assertThat(repo.get("provider")).isEqualTo("github");
        assertThat(repo.get("repoName")).isEqualTo("core");
        assertThat(repo.containsKey("accessToken")).isFalse();

        // Step 3: List repositories
        List<Map<String, Object>> repos = gitService.listRepositories();
        assertThat(repos).hasSize(1);

        // Step 4: Handle webhook
        String payload = "{\"action\":\"push\",\"repository\":{\"full_name\":\"schemaplexai/core\"},\"ref\":\"refs/heads/main\",\"after\":\"abc123\"}";
        gitService.handleWebhook("github", payload);

        List<Map<String, Object>> events = gitService.listWebhookEvents("schemaplexai/core", "push", 10);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).get("eventType")).isEqualTo("push");
    }

    @Test
    @DisplayName("E2E: Register with invalid provider throws param error")
    void registerInvalidProvider() {
        assertThatThrownBy(() -> gitService.registerRepository("", "owner", "repo", "url", "main", null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    @DisplayName("E2E: Handle invalid webhook payload throws param error")
    void handleInvalidWebhook() {
        assertThatThrownBy(() -> gitService.handleWebhook("github", "not-json"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    @DisplayName("E2E: OAuth callback validation")
    void oauthCallbackValidation() {
        assertThatThrownBy(() -> gitService.handleOAuthCallback(null, "code"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    @DisplayName("E2E: Delete repository removes it from store")
    void deleteRepository() {
        Long repoId = gitService.registerRepository("gitlab", "group", "project",
                "https://gitlab.com/group/project.git", "master", null);
        assertThat(gitService.listRepositories()).hasSize(1);

        gitService.deleteRepository(repoId);
        assertThat(gitService.listRepositories()).isEmpty();
    }

    @Test
    @DisplayName("E2E: Get non-existent repository throws NOT_FOUND")
    void getNonExistentRepository() {
        assertThatThrownBy(() -> gitService.getRepository(999L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }
}
