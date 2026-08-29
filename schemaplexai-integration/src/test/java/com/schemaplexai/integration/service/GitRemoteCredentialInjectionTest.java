package com.schemaplexai.integration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.integration.security.IntegrationCredentialEncryptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * NEW-01: credential injection must cover every remote-touching git command,
 * not just clone. Clone persists a credential-free {@code origin} URL, and
 * with {@code GIT_TERMINAL_PROMPT=0} a bare {@code git pull}/{@code push}
 * against a private remote can never prompt — so pull/fetch/push re-inject
 * the stored token through the same {@code http.extraHeader} mechanism, and
 * an unauthenticated/private-remote failure degrades to a structured
 * {@link BaseException} instead of hanging.
 */
@ExtendWith(MockitoExtension.class)
class GitRemoteCredentialInjectionTest {

    private static final Long TENANT_ID = 1L;

    @Mock
    private RestTemplate restTemplate;

    private GitIntegrationService gitService;

    @TempDir
    Path workingDir;

    @BeforeEach
    void setUp() {
        gitService = new GitIntegrationService(new ObjectMapper(), restTemplate,
                new IntegrationCredentialEncryptor("remote-credential-test-secret"),
                GitIntegrationServiceTest.oauthProperties());
        gitService.clearStore();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> rawRepo(Long tenantId, Long repoId) {
        Map<Long, Map<Long, Map<String, Object>>> repoStore =
                (Map<Long, Map<Long, Map<String, Object>>>) ReflectionTestUtils.getField(gitService, "repoStore");
        Map<Long, Map<String, Object>> tenantRepos = repoStore.get(tenantId);
        return tenantRepos != null ? tenantRepos.get(repoId) : null;
    }

    private static String expectedHeader(String username, String token) {
        return "http.extraHeader=Authorization: Basic "
                + Base64.getEncoder().encodeToString((username + ":" + token).getBytes(StandardCharsets.UTF_8));
    }

    // --- argument construction ---

    @Test
    void buildPullArguments_githubWithToken_injectsExtraHeaderBeforePull() {
        String token = "ghp_pull_secret";
        gitService.registerRepository(TENANT_ID, "github", "o", "r", "https://github.com/o/r.git", "main", token);

        List<String> args = gitService.buildPullArguments(rawRepo(TENANT_ID, 1L));

        assertThat(args).containsSequence("-c", expectedHeader("x-access-token", token), "pull");
        // The token only appears base64-encoded inside the header, never raw
        assertThat(String.join(" ", args)).doesNotContain(token);
    }

    @Test
    void buildFetchArguments_githubWithToken_injectsExtraHeaderBeforeFetchOrigin() {
        String token = "ghp_fetch_secret";
        gitService.registerRepository(TENANT_ID, "github", "o", "r", "https://github.com/o/r.git", "main", token);

        List<String> args = gitService.buildFetchArguments(rawRepo(TENANT_ID, 1L));

        assertThat(args).containsSequence("-c", expectedHeader("x-access-token", token), "fetch", "origin");
        assertThat(String.join(" ", args)).doesNotContain(token);
    }

    @Test
    void buildPushArguments_githubWithToken_injectsExtraHeaderBeforePushOriginBranch() {
        String token = "ghp_push_secret";
        gitService.registerRepository(TENANT_ID, "github", "o", "r", "https://github.com/o/r.git", "main", token);

        List<String> args = gitService.buildPushArguments(rawRepo(TENANT_ID, 1L), "dev");

        assertThat(args).containsSequence("-c", expectedHeader("x-access-token", token), "push", "origin", "dev");
        assertThat(String.join(" ", args)).doesNotContain(token);
    }

    @Test
    void buildPullArguments_gitlabWithToken_usesOAuth2Username() {
        String token = "glpat-pull-secret";
        gitService.registerRepository(TENANT_ID, "gitlab", "g", "p", "https://gitlab.com/g/p.git", "main", token);

        List<String> args = gitService.buildPullArguments(rawRepo(TENANT_ID, 1L));

        assertThat(args).containsSequence("-c", expectedHeader("oauth2", token), "pull");
        assertThat(String.join(" ", args)).doesNotContain(token);
    }

    @Test
    void buildRemoteArguments_noToken_plainCommandsWithoutConfig() {
        gitService.registerRepository(TENANT_ID, "github", "o", "r", "https://github.com/o/r.git", "main", null);
        Map<String, Object> raw = rawRepo(TENANT_ID, 1L);

        assertThat(gitService.buildPullArguments(raw)).containsExactly("pull");
        assertThat(gitService.buildFetchArguments(raw)).containsExactly("fetch", "origin");
        assertThat(gitService.buildPushArguments(raw, "main")).containsExactly("push", "origin", "main");
    }

    // --- command execution carries the injection ---

    @Test
    @SuppressWarnings("unchecked")
    void pullRepository_withToken_executesCredentialedPull() throws Exception {
        String token = "tok-pull-123";
        gitService.registerRepository(TENANT_ID, "github", "o", "r", "https://github.com/o/r.git", "main", token);
        GitIntegrationService spyService = spy(gitService);
        doReturn("").when(spyService).executeGitCommandInDir(anyString(), anyList());

        spyService.pullRepository(TENANT_ID, 1L, workingDir.toString());

        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(spyService).executeGitCommandInDir(anyString(), captor.capture());
        List<String> command = captor.getValue();
        assertThat(command).containsSequence("-c", expectedHeader("x-access-token", token), "pull");
        assertThat(String.join(" ", command)).doesNotContain(token);
    }

    @Test
    @SuppressWarnings("unchecked")
    void fetchRepository_withToken_executesCredentialedFetch() throws Exception {
        String token = "tok-fetch-456";
        gitService.registerRepository(TENANT_ID, "github", "o", "r", "https://github.com/o/r.git", "main", token);
        GitIntegrationService spyService = spy(gitService);
        doReturn("").when(spyService).executeGitCommandInDir(anyString(), anyList());

        spyService.fetchRepository(TENANT_ID, 1L, workingDir.toString());

        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(spyService).executeGitCommandInDir(anyString(), captor.capture());
        List<String> command = captor.getValue();
        assertThat(command).containsSequence("-c", expectedHeader("x-access-token", token), "fetch", "origin");
        assertThat(String.join(" ", command)).doesNotContain(token);
    }

    @Test
    @SuppressWarnings("unchecked")
    void pushRepository_withToken_executesCredentialedPushOnDefaultBranch() throws Exception {
        String token = "tok-push-789";
        gitService.registerRepository(TENANT_ID, "github", "o", "r", "https://github.com/o/r.git", "main", token);
        GitIntegrationService spyService = spy(gitService);
        doReturn("").when(spyService).executeGitCommandInDir(anyString(), anyList());

        spyService.pushRepository(TENANT_ID, 1L, workingDir.toString(), null);

        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(spyService).executeGitCommandInDir(anyString(), captor.capture());
        List<String> command = captor.getValue();
        assertThat(command).containsSequence("-c", expectedHeader("x-access-token", token), "push", "origin", "main");
        assertThat(String.join(" ", command)).doesNotContain(token);
    }

    // --- no-credential degradation: explicit structured error, never a hang ---

    private Process authFailureProcess() throws Exception {
        Process process = mock(Process.class);
        when(process.getInputStream()).thenReturn(emptyStream());
        // git exits 128 immediately when auth fails with terminal prompts disabled
        when(process.waitFor(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(process.exitValue()).thenReturn(128);
        return process;
    }

    private InputStream emptyStream() {
        return new ByteArrayInputStream(new byte[0]);
    }

    private Process authFailureProcessWithOutput() throws Exception {
        Process process = mock(Process.class);
        when(process.getInputStream()).thenReturn(new ByteArrayInputStream(
                "fatal: could not read Username for 'https://github.com': terminal prompts disabled\n"
                        .getBytes(StandardCharsets.UTF_8)));
        when(process.waitFor(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(process.exitValue()).thenReturn(128);
        return process;
    }

    private GitIntegrationService spyWithProcess(Process process) throws Exception {
        GitIntegrationService spyService = spy(gitService);
        doReturn(process).when(spyService).startProcess(anyList(), any(File.class));
        return spyService;
    }

    @Test
    void pullRepository_privateRepoAuthFailure_degradesToStructuredError() throws Exception {
        gitService.registerRepository(TENANT_ID, "github", "o", "r", "https://github.com/o/private.git", "main", "bad-token");
        GitIntegrationService spyService = spyWithProcess(authFailureProcessWithOutput());

        // Structured failure with git's auth error surfaced — the command fails
        // fast instead of hanging on a credential prompt.
        assertThatThrownBy(() -> spyService.pullRepository(TENANT_ID, 1L, workingDir.toString()))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> {
                    assertThat(((BaseException) e).getCode()).isEqualTo(ResultCode.TOOL_EXECUTION_FAILED.getCode());
                    assertThat(e.getMessage())
                            .contains("128")
                            .contains("terminal prompts disabled");
                });
    }

    @Test
    void pullRepository_noStoredToken_degradesToStructuredErrorInsteadOfHanging() throws Exception {
        // No credential stored: the pull runs unauthenticated, git cannot prompt
        // (GIT_TERMINAL_PROMPT=0) and exits non-zero immediately — an explicit
        // degradation, never a hang.
        gitService.registerRepository(TENANT_ID, "github", "o", "r", "https://github.com/o/private.git", "main", null);
        GitIntegrationService spyService = spyWithProcess(authFailureProcessWithOutput());

        assertThatThrownBy(() -> spyService.pullRepository(TENANT_ID, 1L, workingDir.toString()))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> {
                    assertThat(((BaseException) e).getCode()).isEqualTo(ResultCode.TOOL_EXECUTION_FAILED.getCode());
                    assertThat(e.getMessage()).contains("terminal prompts disabled");
                });
    }

    @Test
    void pushRepository_privateRepoAuthFailure_degradesToStructuredError() throws Exception {
        gitService.registerRepository(TENANT_ID, "github", "o", "r", "https://github.com/o/private.git", "main", "bad-token");
        GitIntegrationService spyService = spyWithProcess(authFailureProcess());

        assertThatThrownBy(() -> spyService.pushRepository(TENANT_ID, 1L, workingDir.toString(), "main"))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> {
                    assertThat(((BaseException) e).getCode()).isEqualTo(ResultCode.TOOL_EXECUTION_FAILED.getCode());
                    assertThat(e.getMessage()).contains("128");
                });
    }

    @Test
    void fetchRepository_privateRepoAuthFailure_degradesToStructuredError() throws Exception {
        gitService.registerRepository(TENANT_ID, "github", "o", "r", "https://github.com/o/private.git", "main", "bad-token");
        GitIntegrationService spyService = spyWithProcess(authFailureProcess());

        assertThatThrownBy(() -> spyService.fetchRepository(TENANT_ID, 1L, workingDir.toString()))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> {
                    assertThat(((BaseException) e).getCode()).isEqualTo(ResultCode.TOOL_EXECUTION_FAILED.getCode());
                    assertThat(e.getMessage()).contains("128");
                });
    }

    @Test
    void configureProcessBuilder_disablesTerminalPrompt_noHangGuarantee() {
        ProcessBuilder pb = new ProcessBuilder("git", "pull");

        gitService.configureProcessBuilder(pb, workingDir.toFile());

        // Interactive prompts are disabled for every git invocation, so missing
        // credentials surface as an immediate non-zero exit rather than a hang.
        assertThat(pb.environment()).containsEntry("GIT_TERMINAL_PROMPT", "0");
        assertThat(pb.directory()).isEqualTo(workingDir.toFile());
        assertThat(pb.redirectErrorStream()).isTrue();
    }

    // --- fetchRepository validation ---

    @Test
    void fetchRepository_notFound_throwsNotFound() {
        assertThatThrownBy(() -> gitService.fetchRepository(TENANT_ID, 999L, "/tmp/repo"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void fetchRepository_nullPath_throwsParamError() {
        gitService.registerRepository(TENANT_ID, "github", "o", "r", "url", "main", "t");
        assertThatThrownBy(() -> gitService.fetchRepository(TENANT_ID, 1L, null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void fetchRepository_crossTenant_throwsNotFound() {
        Long repoId = gitService.registerRepository(TENANT_ID, "github", "o", "r", "url", "main", "t");
        assertThatThrownBy(() -> gitService.fetchRepository(2L, repoId, "/tmp/repo"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }
}
