package com.schemaplexai.integration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitIntegrationServiceTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private GitIntegrationService gitService;

    @BeforeEach
    void setUp() {
        gitService.clearStore();
        ReflectionTestUtils.setField(gitService, "objectMapper", new ObjectMapper());
    }

    // --- registerRepository ---

    @Test
    void registerRepository_nullProvider_throwsParamError() {
        assertThatThrownBy(() -> gitService.registerRepository(null, "owner", "repo", "url", "main", "token"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void registerRepository_nullCloneUrl_throwsParamError() {
        assertThatThrownBy(() -> gitService.registerRepository("github", "owner", "repo", null, "main", "token"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void registerRepository_success_returnsId() {
        Long id = gitService.registerRepository("github", "owner", "repo", "https://github.com/o/r.git", "main", "token");
        assertThat(id).isEqualTo(1L);
    }

    @Test
    void registerRepository_defaultBranch_defaultsToMain() {
        gitService.registerRepository("github", "owner", "repo", "https://github.com/o/r.git", null, null);
        Map<String, Object> repo = gitService.getRepository(1L);
        assertThat(repo.get("defaultBranch")).isEqualTo("main");
        assertThat(repo.get("accessToken")).isNull();
    }

    // --- getRepository ---

    @Test
    void getRepository_notFound_throwsNotFound() {
        assertThatThrownBy(() -> gitService.getRepository(999L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void getRepository_found_excludesToken() {
        gitService.registerRepository("github", "owner", "repo", "https://github.com/o/r.git", "main", "secret");
        Map<String, Object> repo = gitService.getRepository(1L);
        assertThat(repo).doesNotContainKey("accessToken");
        assertThat(repo.get("provider")).isEqualTo("github");
    }

    // --- listRepositories ---

    @Test
    void listRepositories_empty_returnsEmpty() {
        assertThat(gitService.listRepositories()).isEmpty();
    }

    @Test
    void listRepositories_returnsReposWithoutTokens() {
        gitService.registerRepository("github", "o1", "r1", "url1", "main", "t1");
        gitService.registerRepository("gitlab", "o2", "r2", "url2", "dev", "t2");
        List<Map<String, Object>> repos = gitService.listRepositories();
        assertThat(repos).hasSize(2);
        assertThat(repos.get(0)).doesNotContainKey("accessToken");
    }

    // --- deleteRepository ---

    @Test
    void deleteRepository_notFound_throwsNotFound() {
        assertThatThrownBy(() -> gitService.deleteRepository(999L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void deleteRepository_success() {
        gitService.registerRepository("github", "o", "r", "url", "main", "t");
        gitService.deleteRepository(1L);
        assertThatThrownBy(() -> gitService.getRepository(1L))
                .isInstanceOf(BaseException.class);
    }

    // --- cloneRepository ---

    @Test
    void cloneRepository_notFound_throwsNotFound() {
        assertThatThrownBy(() -> gitService.cloneRepository(999L, null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    // --- pullRepository ---

    @Test
    void pullRepository_notFound_throwsNotFound() {
        assertThatThrownBy(() -> gitService.pullRepository(999L, "/tmp/repo"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void pullRepository_nullPath_throwsParamError() {
        gitService.registerRepository("github", "o", "r", "url", "main", "t");
        assertThatThrownBy(() -> gitService.pullRepository(1L, null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    // --- pushRepository ---

    @Test
    void pushRepository_notFound_throwsNotFound() {
        assertThatThrownBy(() -> gitService.pushRepository(999L, "/tmp/repo", "main"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void pushRepository_nullPath_throwsParamError() {
        gitService.registerRepository("github", "o", "r", "url", "main", "t");
        assertThatThrownBy(() -> gitService.pushRepository(1L, null, "main"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    // --- listBranches ---

    @Test
    void listBranches_nullPath_throwsParamError() {
        assertThatThrownBy(() -> gitService.listBranches(1L, null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    // --- createBranch ---

    @Test
    void createBranch_nullBranchName_throwsParamError() {
        assertThatThrownBy(() -> gitService.createBranch(1L, "/tmp", null, "main"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    // --- deleteBranch ---

    @Test
    void deleteBranch_nullBranchName_throwsParamError() {
        assertThatThrownBy(() -> gitService.deleteBranch(1L, "/tmp", null, false))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    // --- handleOAuthCallback ---

    @Test
    void handleOAuthCallback_nullProvider_throwsParamError() {
        assertThatThrownBy(() -> gitService.handleOAuthCallback(null, "code"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void handleOAuthCallback_nullCode_throwsParamError() {
        assertThatThrownBy(() -> gitService.handleOAuthCallback("github", null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void handleOAuthCallback_success() {
        gitService.handleOAuthCallback("github", "authcode123");
        // no exception
    }

    // --- handleWebhook ---

    @Test
    void handleWebhook_nullProvider_throwsParamError() {
        assertThatThrownBy(() -> gitService.handleWebhook(null, "{}"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void handleWebhook_nullPayload_throwsParamError() {
        assertThatThrownBy(() -> gitService.handleWebhook("github", null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void handleWebhook_invalidPayload_throwsParamError() {
        assertThatThrownBy(() -> gitService.handleWebhook("github", "not-json"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void handleWebhook_missingRepository_throwsParamErrorWithoutStoringEvent() {
        String payload = "{\"action\":\"opened\",\"ref\":\"refs/heads/main\",\"after\":\"abc123\"}";

        assertThatThrownBy(() -> gitService.handleWebhook("github", payload))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());

        assertThat(gitService.listWebhookEvents(null, null, 10)).isEmpty();
    }

    // --- listWebhookEvents ---

    @Test
    void listWebhookEvents_empty_returnsEmpty() {
        assertThat(gitService.listWebhookEvents(null, null, 10)).isEmpty();
    }

    @Test
    void listWebhookEvents_filtersByRepository() {
        gitService.registerRepository("github", "o", "r", "url", "main", "t");
        // Can't easily trigger webhook without mocking ObjectMapper, but we can test filtering on empty store
        assertThat(gitService.listWebhookEvents("nonexistent", null, 10)).isEmpty();
    }

    @Test
    void listWebhookEvents_respectsLimit() {
        assertThat(gitService.listWebhookEvents(null, null, 0)).isEmpty();
    }

    @Test
    void listWebhookEvents_zeroLimitWithStoredEvents_returnsEmpty() {
        gitService.registerRepository("github", "o", "r", "url", "main", "t");
        gitService.handleWebhook("github", "{\"action\":\"opened\",\"repository\":{\"full_name\":\"o/r\"},\"ref\":\"refs/heads/main\",\"after\":\"abc123\"}");

        assertThat(gitService.listWebhookEvents(null, null, 0)).isEmpty();
    }

    // --- fetchRepositoryInfo ---

    @Test
    void fetchRepositoryInfo_notFound_throwsNotFound() {
        assertThatThrownBy(() -> gitService.fetchRepositoryInfo(999L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void fetchRepositoryInfo_unsupportedProvider_throwsParamError() {
        gitService.registerRepository("bitbucket", "o", "r", "url", "main", "t");
        assertThatThrownBy(() -> gitService.fetchRepositoryInfo(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void fetchRepositoryInfo_github_success() {
        gitService.registerRepository("github", "octocat", "hello-world", "url", "main", "token");
        when(restTemplate.exchange(eq("https://api.github.com/repos/octocat/hello-world"), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"name\":\"hello-world\"}"));

        String result = gitService.fetchRepositoryInfo(1L);
        assertThat(result).isEqualTo("{\"name\":\"hello-world\"}");
    }

    @Test
    void fetchRepositoryInfo_gitlab_success() {
        gitService.registerRepository("gitlab", "group", "project", "url", "main", "token");
        when(restTemplate.exchange(eq("https://gitlab.com/api/v4/projects/group%2Fproject"), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"name\":\"project\"}"));

        String result = gitService.fetchRepositoryInfo(1L);
        assertThat(result).isEqualTo("{\"name\":\"project\"}");
    }

    // --- fetchBranchesViaApi ---

    @Test
    void fetchBranchesViaApi_github_success() {
        gitService.registerRepository("github", "octocat", "hello-world", "url", "main", "token");
        when(restTemplate.exchange(eq("https://api.github.com/repos/octocat/hello-world/branches"), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("[]"));

        String result = gitService.fetchBranchesViaApi(1L);
        assertThat(result).isEqualTo("[]");
    }

    @Test
    void fetchBranchesViaApi_gitlab_success() {
        gitService.registerRepository("gitlab", "group", "project", "url", "main", "token");
        when(restTemplate.exchange(eq("https://gitlab.com/api/v4/projects/group%2Fproject/repository/branches"), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("[]"));

        String result = gitService.fetchBranchesViaApi(1L);
        assertThat(result).isEqualTo("[]");
    }

    @Test
    void fetchBranchesViaApi_notFound_throwsNotFound() {
        assertThatThrownBy(() -> gitService.fetchBranchesViaApi(999L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void fetchBranchesViaApi_unsupportedProvider_throwsParamError() {
        gitService.registerRepository("bitbucket", "o", "r", "url", "main", "t");
        assertThatThrownBy(() -> gitService.fetchBranchesViaApi(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void fetchRepositoryInfo_github_withoutToken_success() {
        gitService.registerRepository("github", "octocat", "hello-world", "url", "main", "");
        when(restTemplate.exchange(eq("https://api.github.com/repos/octocat/hello-world"), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{}"));

        String result = gitService.fetchRepositoryInfo(1L);
        assertThat(result).isEqualTo("{}");
    }

    @Test
    void fetchRepositoryInfo_gitlab_withoutToken_success() {
        gitService.registerRepository("gitlab", "group", "project", "url", "main", null);
        when(restTemplate.exchange(eq("https://gitlab.com/api/v4/projects/group%2Fproject"), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{}"));

        String result = gitService.fetchRepositoryInfo(1L);
        assertThat(result).isEqualTo("{}");
    }

    @Test
    void handleWebhook_githubPayload_success() {
        gitService.registerRepository("github", "o", "r", "url", "main", "t");
        String payload = "{\"action\":\"opened\",\"repository\":{\"full_name\":\"o/r\"},\"ref\":\"refs/heads/main\",\"after\":\"abc123\"}";
        gitService.handleWebhook("github", payload);
        assertThat(gitService.listWebhookEvents("o/r", null, 10)).hasSize(1);
    }

    @Test
    void handleWebhook_gitlabPayload_success() {
        gitService.registerRepository("gitlab", "o", "r", "url", "main", "t");
        String payload = "{\"object_kind\":\"push\",\"project\":{\"path_with_namespace\":\"o/r\"},\"ref\":\"refs/heads/dev\",\"after\":\"def456\"}";
        gitService.handleWebhook("gitlab", payload);
        assertThat(gitService.listWebhookEvents("o/r", null, 10)).hasSize(1);
    }

    @Test
    void handleWebhook_unknownProvider_success() {
        gitService.registerRepository("other", "o", "r", "url", "main", "t");
        String payload = "{\"event_type\":\"push\",\"repository\":{\"full_name\":\"o/r\"},\"ref\":\"refs/heads/main\",\"after\":\"ghi789\"}";
        gitService.handleWebhook("other", payload);
        assertThat(gitService.listWebhookEvents("o/r", null, 10)).hasSize(1);
    }

    @Test
    void listWebhookEvents_filtersByEventType() {
        gitService.registerRepository("github", "o", "r", "url", "main", "t");
        gitService.handleWebhook("github", "{\"action\":\"opened\",\"repository\":{\"full_name\":\"o/r\"},\"ref\":\"refs/heads/main\",\"after\":\"abc123\"}");
        assertThat(gitService.listWebhookEvents(null, "opened", 10)).hasSize(1);
        assertThat(gitService.listWebhookEvents(null, "closed", 10)).isEmpty();
    }

    @Test
    void listWebhookEvents_filtersByRepositoryAndEventType() {
        gitService.registerRepository("github", "o", "r", "url", "main", "t");
        gitService.handleWebhook("github", "{\"action\":\"opened\",\"repository\":{\"full_name\":\"o/r\"},\"ref\":\"refs/heads/main\",\"after\":\"abc123\"}");
        assertThat(gitService.listWebhookEvents("o/r", "opened", 10)).hasSize(1);
        assertThat(gitService.listWebhookEvents("other", "opened", 10)).isEmpty();
    }
}
