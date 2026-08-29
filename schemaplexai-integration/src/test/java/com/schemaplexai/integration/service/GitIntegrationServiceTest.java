package com.schemaplexai.integration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.integration.config.IntegrationOAuthProperties;
import com.schemaplexai.integration.security.IntegrationCredentialEncryptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitIntegrationServiceTest {

    private static final Long TENANT_ID = 1L;
    private static final Long OTHER_TENANT_ID = 2L;
    private static final String MASTER_SECRET = "test-master-secret-for-integration";

    @Mock
    private RestTemplate restTemplate;

    private IntegrationCredentialEncryptor encryptor;

    private GitIntegrationService gitService;

    @BeforeEach
    void setUp() {
        encryptor = new IntegrationCredentialEncryptor(MASTER_SECRET);
        gitService = new GitIntegrationService(new ObjectMapper(), restTemplate, encryptor, oauthProperties());
        gitService.clearStore();
    }

    static IntegrationOAuthProperties oauthProperties() {
        IntegrationOAuthProperties props = new IntegrationOAuthProperties();
        IntegrationOAuthProperties.Provider github = new IntegrationOAuthProperties.Provider();
        github.setClientId("test-client-id");
        github.setClientSecret("test-client-secret");
        github.setTokenUrl("https://github.com/login/oauth/access_token");
        props.getProviders().put("github", github);
        IntegrationOAuthProperties.Provider gitlab = new IntegrationOAuthProperties.Provider();
        gitlab.setClientId("gitlab-client-id");
        gitlab.setClientSecret("gitlab-client-secret");
        gitlab.setTokenUrl("https://gitlab.com/oauth/token");
        props.getProviders().put("gitlab", gitlab);
        return props;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> rawRepo(Long tenantId, Long repoId) {
        Map<Long, Map<Long, Map<String, Object>>> repoStore =
                (Map<Long, Map<Long, Map<String, Object>>>) ReflectionTestUtils.getField(gitService, "repoStore");
        Map<Long, Map<String, Object>> tenantRepos = repoStore.get(tenantId);
        return tenantRepos != null ? tenantRepos.get(repoId) : null;
    }

    // --- registerRepository ---

    @Test
    void registerRepository_nullTenant_throwsParamError() {
        assertThatThrownBy(() -> gitService.registerRepository(null, "github", "owner", "repo", "url", "main", "token"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void registerRepository_nullProvider_throwsParamError() {
        assertThatThrownBy(() -> gitService.registerRepository(TENANT_ID, null, "owner", "repo", "url", "main", "token"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void registerRepository_nullCloneUrl_throwsParamError() {
        assertThatThrownBy(() -> gitService.registerRepository(TENANT_ID, "github", "owner", "repo", null, "main", "token"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void registerRepository_success_returnsId() {
        Long id = gitService.registerRepository(TENANT_ID, "github", "owner", "repo", "https://github.com/o/r.git", "main", "token");
        assertThat(id).isEqualTo(1L);
    }

    @Test
    void registerRepository_defaultBranch_defaultsToMain() {
        gitService.registerRepository(TENANT_ID, "github", "owner", "repo", "https://github.com/o/r.git", null, null);
        Map<String, Object> repo = gitService.getRepository(TENANT_ID, 1L);
        assertThat(repo.get("defaultBranch")).isEqualTo("main");
        assertThat(repo.get("accessToken")).isNull();
        assertThat(repo.get("accessTokenCipher")).isNull();
    }

    @Test
    void registerRepository_storesEncryptedToken_neverPlaintext() {
        String token = "ghp_super_secret_token_value";
        gitService.registerRepository(TENANT_ID, "github", "owner", "repo", "https://github.com/o/r.git", "main", token);

        Map<String, Object> raw = rawRepo(TENANT_ID, 1L);

        // No plaintext token anywhere in the raw store
        assertThat(raw).doesNotContainKey("accessToken");
        assertThat(raw.values()).noneMatch(v -> v instanceof String s && s.contains(token));

        // Stored value is versioned AES-256-GCM ciphertext that decrypts back to the token
        String cipher = (String) raw.get("accessTokenCipher");
        assertThat(cipher).startsWith(IntegrationCredentialEncryptor.CIPHER_PREFIX);
        assertThat(cipher).doesNotContain(token);
        assertThat(encryptor.decrypt(cipher, TENANT_ID)).isEqualTo(token);
    }

    // --- getRepository ---

    @Test
    void getRepository_notFound_throwsNotFound() {
        assertThatThrownBy(() -> gitService.getRepository(TENANT_ID, 999L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void getRepository_nullTenant_throwsParamError() {
        gitService.registerRepository(TENANT_ID, "github", "o", "r", "url", "main", "t");
        assertThatThrownBy(() -> gitService.getRepository(null, 1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void getRepository_found_excludesToken() {
        gitService.registerRepository(TENANT_ID, "github", "owner", "repo", "https://github.com/o/r.git", "main", "secret");
        Map<String, Object> repo = gitService.getRepository(TENANT_ID, 1L);
        assertThat(repo).doesNotContainKey("accessToken");
        assertThat(repo).doesNotContainKey("accessTokenCipher");
        assertThat(repo.get("provider")).isEqualTo("github");
    }

    // --- listRepositories ---

    @Test
    void listRepositories_empty_returnsEmpty() {
        assertThat(gitService.listRepositories(TENANT_ID)).isEmpty();
    }

    @Test
    void listRepositories_nullTenant_throwsParamError() {
        assertThatThrownBy(() -> gitService.listRepositories(null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void listRepositories_returnsReposWithoutTokens() {
        gitService.registerRepository(TENANT_ID, "github", "o1", "r1", "url1", "main", "t1");
        gitService.registerRepository(TENANT_ID, "gitlab", "o2", "r2", "url2", "dev", "t2");
        List<Map<String, Object>> repos = gitService.listRepositories(TENANT_ID);
        assertThat(repos).hasSize(2);
        assertThat(repos.get(0)).doesNotContainKey("accessToken");
        assertThat(repos.get(0)).doesNotContainKey("accessTokenCipher");
    }

    // --- deleteRepository ---

    @Test
    void deleteRepository_notFound_throwsNotFound() {
        assertThatThrownBy(() -> gitService.deleteRepository(TENANT_ID, 999L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void deleteRepository_success() {
        gitService.registerRepository(TENANT_ID, "github", "o", "r", "url", "main", "t");
        gitService.deleteRepository(TENANT_ID, 1L);
        assertThatThrownBy(() -> gitService.getRepository(TENANT_ID, 1L))
                .isInstanceOf(BaseException.class);
    }

    // --- tenant isolation (issue 917) ---

    @Test
    void crossTenant_listRepositories_doesNotSeeOtherTenantRepos() {
        gitService.registerRepository(TENANT_ID, "github", "o", "r", "url", "main", "t");
        gitService.registerRepository(OTHER_TENANT_ID, "gitlab", "o2", "r2", "url2", "main", "t2");

        assertThat(gitService.listRepositories(TENANT_ID)).hasSize(1);
        assertThat(gitService.listRepositories(OTHER_TENANT_ID)).hasSize(1);
        assertThat(gitService.listRepositories(TENANT_ID).get(0).get("owner")).isEqualTo("o");
        assertThat(gitService.listRepositories(OTHER_TENANT_ID).get(0).get("owner")).isEqualTo("o2");
    }

    @Test
    void crossTenant_getRepository_throwsNotFound() {
        Long repoId = gitService.registerRepository(TENANT_ID, "github", "o", "r", "url", "main", "t");

        assertThatThrownBy(() -> gitService.getRepository(OTHER_TENANT_ID, repoId))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());

        // Owning tenant still sees it
        assertThat(gitService.getRepository(TENANT_ID, repoId)).isNotNull();
    }

    @Test
    void crossTenant_deleteRepository_cannotRemoveOtherTenantRepo() {
        Long repoId = gitService.registerRepository(TENANT_ID, "github", "o", "r", "url", "main", "t");

        assertThatThrownBy(() -> gitService.deleteRepository(OTHER_TENANT_ID, repoId))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());

        // Tenant 1's repo survived the cross-tenant delete attempt
        assertThat(gitService.getRepository(TENANT_ID, repoId)).isNotNull();
    }

    @Test
    void crossTenant_clonePullPushFetch_allNotFound() {
        Long repoId = gitService.registerRepository(TENANT_ID, "github", "o", "r", "https://github.com/o/r.git", "main", "t");

        assertThatThrownBy(() -> gitService.cloneRepository(OTHER_TENANT_ID, repoId, null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
        assertThatThrownBy(() -> gitService.pullRepository(OTHER_TENANT_ID, repoId, "/tmp/repo"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
        assertThatThrownBy(() -> gitService.pushRepository(OTHER_TENANT_ID, repoId, "/tmp/repo", "main"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
        assertThatThrownBy(() -> gitService.fetchRepositoryInfo(OTHER_TENANT_ID, repoId))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
        assertThatThrownBy(() -> gitService.fetchBranchesViaApi(OTHER_TENANT_ID, repoId))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void crossTenant_webhookEvents_areIsolated() {
        gitService.handleWebhook(TENANT_ID, "github",
                "{\"action\":\"opened\",\"repository\":{\"full_name\":\"o/r\"},\"ref\":\"refs/heads/main\",\"after\":\"abc123\"}");

        assertThat(gitService.listWebhookEvents(TENANT_ID, null, null, 10)).hasSize(1);
        assertThat(gitService.listWebhookEvents(OTHER_TENANT_ID, null, null, 10)).isEmpty();
    }

    @Test
    void webhook_nullTenant_throwsParamError() {
        assertThatThrownBy(() -> gitService.handleWebhook(null, "github", "{}"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
        assertThatThrownBy(() -> gitService.listWebhookEvents(null, null, null, 10))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    // --- cloneRepository / credential injection ---

    @Test
    void cloneRepository_notFound_throwsNotFound() {
        assertThatThrownBy(() -> gitService.cloneRepository(TENANT_ID, 999L, null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void buildCloneArguments_github_injectsBasicAuthHeader_keepsUrlClean() {
        String token = "ghp_secret123";
        gitService.registerRepository(TENANT_ID, "github", "o", "r", "https://github.com/o/r.git", "main", token);
        Map<String, Object> raw = rawRepo(TENANT_ID, 1L);

        List<String> args = gitService.buildCloneArguments(raw, "https://github.com/o/r.git", "r");

        // URL passed to git must be clean (no embedded token)
        assertThat(args).contains("https://github.com/o/r.git");
        assertThat(String.join(" ", args)).doesNotContain(token);

        // Credential injected via http.extraHeader Basic auth
        String expectedHeader = "http.extraHeader=Authorization: Basic "
                + Base64.getEncoder().encodeToString(("x-access-token:" + token).getBytes(StandardCharsets.UTF_8));
        assertThat(args).containsSequence("-c", expectedHeader, "clone", "https://github.com/o/r.git", "r");
    }

    @Test
    void buildCloneArguments_gitlab_usesOAuth2Username() {
        String token = "glpat-secret";
        gitService.registerRepository(TENANT_ID, "gitlab", "g", "p", "https://gitlab.com/g/p.git", "main", token);
        Map<String, Object> raw = rawRepo(TENANT_ID, 1L);

        List<String> args = gitService.buildCloneArguments(raw, "https://gitlab.com/g/p.git", "p");

        String expectedHeader = "http.extraHeader=Authorization: Basic "
                + Base64.getEncoder().encodeToString(("oauth2:" + token).getBytes(StandardCharsets.UTF_8));
        assertThat(args).contains(expectedHeader);
        assertThat(String.join(" ", args)).doesNotContain(token);
    }

    @Test
    void buildCloneArguments_noToken_plainClone() {
        gitService.registerRepository(TENANT_ID, "github", "o", "r", "https://github.com/o/r.git", "main", null);
        Map<String, Object> raw = rawRepo(TENANT_ID, 1L);

        List<String> args = gitService.buildCloneArguments(raw, "https://github.com/o/r.git", "r");

        assertThat(args).containsExactly("clone", "https://github.com/o/r.git", "r");
    }

    @Test
    void cloneRepository_injectsCredentialsNotUrl(@TempDir Path tempDir) throws Exception {
        gitService.registerRepository(TENANT_ID, "github", "o", "r", "https://github.com/o/r.git", "main", "tok-123");
        GitIntegrationService spyService = spy(gitService);
        doReturn("").when(spyService).executeGitCommandInDir(anyString(), anyList());

        spyService.cloneRepository(TENANT_ID, 1L, tempDir.resolve("r").toString());

        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(spyService).executeGitCommandInDir(anyString(), captor.capture());
        List<String> command = captor.getValue();
        assertThat(command).contains("https://github.com/o/r.git");
        assertThat(String.join(" ", command)).doesNotContain("tok-123");
        assertThat(command).contains("-c");
        assertThat(command.stream().anyMatch(a -> a.startsWith("http.extraHeader=Authorization: Basic "))).isTrue();
    }

    // --- pullRepository ---

    @Test
    void pullRepository_notFound_throwsNotFound() {
        assertThatThrownBy(() -> gitService.pullRepository(TENANT_ID, 999L, "/tmp/repo"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void pullRepository_nullPath_throwsParamError() {
        gitService.registerRepository(TENANT_ID, "github", "o", "r", "url", "main", "t");
        assertThatThrownBy(() -> gitService.pullRepository(TENANT_ID, 1L, null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    // --- pushRepository ---

    @Test
    void pushRepository_notFound_throwsNotFound() {
        assertThatThrownBy(() -> gitService.pushRepository(TENANT_ID, 999L, "/tmp/repo", "main"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void pushRepository_nullPath_throwsParamError() {
        gitService.registerRepository(TENANT_ID, "github", "o", "r", "url", "main", "t");
        assertThatThrownBy(() -> gitService.pushRepository(TENANT_ID, 1L, null, "main"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    // --- listBranches ---

    @Test
    void listBranches_nullPath_throwsParamError() {
        assertThatThrownBy(() -> gitService.listBranches(TENANT_ID, 1L, null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void listBranches_nullTenant_throwsParamError() {
        assertThatThrownBy(() -> gitService.listBranches(null, 1L, "/tmp"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    // --- createBranch ---

    @Test
    void createBranch_nullBranchName_throwsParamError() {
        assertThatThrownBy(() -> gitService.createBranch(TENANT_ID, 1L, "/tmp", null, "main"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void createBranch_nullTenant_throwsParamError() {
        assertThatThrownBy(() -> gitService.createBranch(null, 1L, "/tmp", "b", "main"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    // --- deleteBranch ---

    @Test
    void deleteBranch_nullBranchName_throwsParamError() {
        assertThatThrownBy(() -> gitService.deleteBranch(TENANT_ID, 1L, "/tmp", null, false))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void deleteBranch_nullTenant_throwsParamError() {
        assertThatThrownBy(() -> gitService.deleteBranch(null, 1L, "/tmp", "b", false))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    // --- handleOAuthCallback ---

    @Test
    void handleOAuthCallback_nullTenant_throwsParamError() {
        assertThatThrownBy(() -> gitService.handleOAuthCallback(null, "github", "code"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void handleOAuthCallback_nullProvider_throwsParamError() {
        assertThatThrownBy(() -> gitService.handleOAuthCallback(TENANT_ID, null, "code"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void handleOAuthCallback_nullCode_throwsParamError() {
        assertThatThrownBy(() -> gitService.handleOAuthCallback(TENANT_ID, "github", null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void handleOAuthCallback_success() {
        when(restTemplate.exchange(eq("https://github.com/login/oauth/access_token"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"access_token\":\"gho_authcode_exchange\",\"token_type\":\"bearer\"}"));

        Map<String, Object> result = gitService.handleOAuthCallback(TENANT_ID, "github", "authcode123");

        assertThat(result.get("provider")).isEqualTo("github");
        assertThat(result.get("status")).isEqualTo("connected");
    }

    // --- handleWebhook ---

    @Test
    void handleWebhook_nullProvider_throwsParamError() {
        assertThatThrownBy(() -> gitService.handleWebhook(TENANT_ID, null, "{}"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void handleWebhook_nullPayload_throwsParamError() {
        assertThatThrownBy(() -> gitService.handleWebhook(TENANT_ID, "github", null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void handleWebhook_invalidPayload_throwsParamError() {
        assertThatThrownBy(() -> gitService.handleWebhook(TENANT_ID, "github", "not-json"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void handleWebhook_missingRepository_throwsParamErrorWithoutStoringEvent() {
        String payload = "{\"action\":\"opened\",\"ref\":\"refs/heads/main\",\"after\":\"abc123\"}";

        assertThatThrownBy(() -> gitService.handleWebhook(TENANT_ID, "github", payload))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());

        assertThat(gitService.listWebhookEvents(TENANT_ID, null, null, 10)).isEmpty();
    }

    // --- listWebhookEvents ---

    @Test
    void listWebhookEvents_empty_returnsEmpty() {
        assertThat(gitService.listWebhookEvents(TENANT_ID, null, null, 10)).isEmpty();
    }

    @Test
    void listWebhookEvents_filtersByRepository() {
        gitService.registerRepository(TENANT_ID, "github", "o", "r", "url", "main", "t");
        // Can't easily trigger webhook without mocking ObjectMapper, but we can test filtering on empty store
        assertThat(gitService.listWebhookEvents(TENANT_ID, "nonexistent", null, 10)).isEmpty();
    }

    @Test
    void listWebhookEvents_respectsLimit() {
        assertThat(gitService.listWebhookEvents(TENANT_ID, null, null, 0)).isEmpty();
    }

    @Test
    void listWebhookEvents_zeroLimitWithStoredEvents_returnsEmpty() {
        gitService.registerRepository(TENANT_ID, "github", "o", "r", "url", "main", "t");
        gitService.handleWebhook(TENANT_ID, "github", "{\"action\":\"opened\",\"repository\":{\"full_name\":\"o/r\"},\"ref\":\"refs/heads/main\",\"after\":\"abc123\"}");

        assertThat(gitService.listWebhookEvents(TENANT_ID, null, null, 0)).isEmpty();
    }

    // --- fetchRepositoryInfo ---

    @Test
    void fetchRepositoryInfo_notFound_throwsNotFound() {
        assertThatThrownBy(() -> gitService.fetchRepositoryInfo(TENANT_ID, 999L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void fetchRepositoryInfo_unsupportedProvider_throwsParamError() {
        gitService.registerRepository(TENANT_ID, "bitbucket", "o", "r", "url", "main", "t");
        assertThatThrownBy(() -> gitService.fetchRepositoryInfo(TENANT_ID, 1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void fetchRepositoryInfo_github_success() {
        gitService.registerRepository(TENANT_ID, "github", "octocat", "hello-world", "url", "main", "token");
        when(restTemplate.exchange(eq("https://api.github.com/repos/octocat/hello-world"), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"name\":\"hello-world\"}"));

        String result = gitService.fetchRepositoryInfo(TENANT_ID, 1L);
        assertThat(result).isEqualTo("{\"name\":\"hello-world\"}");
    }

    @Test
    void fetchRepositoryInfo_gitlab_success() {
        gitService.registerRepository(TENANT_ID, "gitlab", "group", "project", "url", "main", "token");
        when(restTemplate.exchange(eq("https://gitlab.com/api/v4/projects/group%2Fproject"), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"name\":\"project\"}"));

        String result = gitService.fetchRepositoryInfo(TENANT_ID, 1L);
        assertThat(result).isEqualTo("{\"name\":\"project\"}");
    }

    // --- fetchBranchesViaApi ---

    @Test
    void fetchBranchesViaApi_github_success() {
        gitService.registerRepository(TENANT_ID, "github", "octocat", "hello-world", "url", "main", "token");
        when(restTemplate.exchange(eq("https://api.github.com/repos/octocat/hello-world/branches"), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("[]"));

        String result = gitService.fetchBranchesViaApi(TENANT_ID, 1L);
        assertThat(result).isEqualTo("[]");
    }

    @Test
    void fetchBranchesViaApi_gitlab_success() {
        gitService.registerRepository(TENANT_ID, "gitlab", "group", "project", "url", "main", "token");
        when(restTemplate.exchange(eq("https://gitlab.com/api/v4/projects/group%2Fproject/repository/branches"), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("[]"));

        String result = gitService.fetchBranchesViaApi(TENANT_ID, 1L);
        assertThat(result).isEqualTo("[]");
    }

    @Test
    void fetchBranchesViaApi_notFound_throwsNotFound() {
        assertThatThrownBy(() -> gitService.fetchBranchesViaApi(TENANT_ID, 999L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void fetchBranchesViaApi_unsupportedProvider_throwsParamError() {
        gitService.registerRepository(TENANT_ID, "bitbucket", "o", "r", "url", "main", "t");
        assertThatThrownBy(() -> gitService.fetchBranchesViaApi(TENANT_ID, 1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void fetchRepositoryInfo_github_withoutToken_success() {
        gitService.registerRepository(TENANT_ID, "github", "octocat", "hello-world", "url", "main", "");
        when(restTemplate.exchange(eq("https://api.github.com/repos/octocat/hello-world"), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{}"));

        String result = gitService.fetchRepositoryInfo(TENANT_ID, 1L);
        assertThat(result).isEqualTo("{}");
    }

    @Test
    void fetchRepositoryInfo_gitlab_withoutToken_success() {
        gitService.registerRepository(TENANT_ID, "gitlab", "group", "project", "url", "main", null);
        when(restTemplate.exchange(eq("https://gitlab.com/api/v4/projects/group%2Fproject"), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{}"));

        String result = gitService.fetchRepositoryInfo(TENANT_ID, 1L);
        assertThat(result).isEqualTo("{}");
    }

    @Test
    void handleWebhook_githubPayload_success() {
        gitService.registerRepository(TENANT_ID, "github", "o", "r", "url", "main", "t");
        String payload = "{\"action\":\"opened\",\"repository\":{\"full_name\":\"o/r\"},\"ref\":\"refs/heads/main\",\"after\":\"abc123\"}";
        gitService.handleWebhook(TENANT_ID, "github", payload);
        assertThat(gitService.listWebhookEvents(TENANT_ID, "o/r", null, 10)).hasSize(1);
    }

    @Test
    void handleWebhook_gitlabPayload_success() {
        gitService.registerRepository(TENANT_ID, "gitlab", "o", "r", "url", "main", "t");
        String payload = "{\"object_kind\":\"push\",\"project\":{\"path_with_namespace\":\"o/r\"},\"ref\":\"refs/heads/dev\",\"after\":\"def456\"}";
        gitService.handleWebhook(TENANT_ID, "gitlab", payload);
        assertThat(gitService.listWebhookEvents(TENANT_ID, "o/r", null, 10)).hasSize(1);
    }

    @Test
    void handleWebhook_unknownProvider_success() {
        gitService.registerRepository(TENANT_ID, "other", "o", "r", "url", "main", "t");
        String payload = "{\"event_type\":\"push\",\"repository\":{\"full_name\":\"o/r\"},\"ref\":\"refs/heads/main\",\"after\":\"ghi789\"}";
        gitService.handleWebhook(TENANT_ID, "other", payload);
        assertThat(gitService.listWebhookEvents(TENANT_ID, "o/r", null, 10)).hasSize(1);
    }

    @Test
    void listWebhookEvents_filtersByEventType() {
        gitService.registerRepository(TENANT_ID, "github", "o", "r", "url", "main", "t");
        gitService.handleWebhook(TENANT_ID, "github", "{\"action\":\"opened\",\"repository\":{\"full_name\":\"o/r\"},\"ref\":\"refs/heads/main\",\"after\":\"abc123\"}");
        assertThat(gitService.listWebhookEvents(TENANT_ID, null, "opened", 10)).hasSize(1);
        assertThat(gitService.listWebhookEvents(TENANT_ID, null, "closed", 10)).isEmpty();
    }

    @Test
    void listWebhookEvents_filtersByRepositoryAndEventType() {
        gitService.registerRepository(TENANT_ID, "github", "o", "r", "url", "main", "t");
        gitService.handleWebhook(TENANT_ID, "github", "{\"action\":\"opened\",\"repository\":{\"full_name\":\"o/r\"},\"ref\":\"refs/heads/main\",\"after\":\"abc123\"}");
        assertThat(gitService.listWebhookEvents(TENANT_ID, "o/r", "opened", 10)).hasSize(1);
        assertThat(gitService.listWebhookEvents(TENANT_ID, "other", "opened", 10)).isEmpty();
    }
}
