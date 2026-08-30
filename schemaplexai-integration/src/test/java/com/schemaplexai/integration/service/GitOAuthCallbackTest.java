package com.schemaplexai.integration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.integration.config.IntegrationOAuthProperties;
import com.schemaplexai.integration.security.IntegrationCredentialEncryptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Issue 916: OAuth authorization_code exchange and encrypted token storage.
 * The external provider token endpoint is mocked via RestTemplate.
 */
@ExtendWith(MockitoExtension.class)
class GitOAuthCallbackTest {

    private static final Long TENANT_ID = 1L;
    private static final Long OTHER_TENANT_ID = 2L;
    private static final String GITHUB_TOKEN_URL = "https://github.com/login/oauth/access_token";

    @Mock
    private RestTemplate restTemplate;

    private IntegrationCredentialEncryptor encryptor;
    private GitIntegrationService gitService;

    @BeforeEach
    void setUp() {
        encryptor = new IntegrationCredentialEncryptor("oauth-test-master-secret");
        gitService = new GitIntegrationService(new ObjectMapper(), restTemplate, encryptor,
                GitIntegrationServiceTest.oauthProperties());
        gitService.clearStore();
    }

    @Test
    @SuppressWarnings("unchecked")
    void handleOAuthCallback_exchangesCodeWithAuthorizationCodeGrant() {
        when(restTemplate.exchange(eq(GITHUB_TOKEN_URL), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"access_token\":\"gho_abc\",\"token_type\":\"bearer\"}"));

        gitService.handleOAuthCallback(TENANT_ID, "github", "the-auth-code");

        ArgumentCaptor<HttpEntity<?>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(eq(GITHUB_TOKEN_URL), eq(HttpMethod.POST), captor.capture(), eq(String.class));
        MultiValueMap<String, String> form = (MultiValueMap<String, String>) captor.getValue().getBody();

        assertThat(form.getFirst("grant_type")).isEqualTo("authorization_code");
        assertThat(form.getFirst("code")).isEqualTo("the-auth-code");
        assertThat(form.getFirst("client_id")).isEqualTo("test-client-id");
        assertThat(form.getFirst("client_secret")).isEqualTo("test-client-secret");
        assertThat(captor.getValue().getHeaders().getAccept().toString()).contains("application/json");
    }

    @Test
    void handleOAuthCallback_storesTokenEncrypted_neverPlaintext() {
        String token = "gho_secret_oauth_token";
        when(restTemplate.exchange(eq(GITHUB_TOKEN_URL), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"access_token\":\"" + token + "\"}"));

        Map<String, Object> result = gitService.handleOAuthCallback(TENANT_ID, "github", "code-1");

        assertThat(result.get("status")).isEqualTo("connected");
        assertThat(result.values()).noneMatch(v -> v instanceof String s && s.contains(token));

        Map<String, Object> connection = gitService.getOAuthConnection(TENANT_ID, "github");
        assertThat(connection).isNotNull();
        String cipher = (String) connection.get("tokenCipher");
        assertThat(cipher).startsWith(IntegrationCredentialEncryptor.CIPHER_PREFIX);
        assertThat(cipher).doesNotContain(token);
        assertThat(connection.values()).noneMatch(v -> v instanceof String s && s.contains(token));
        assertThat(encryptor.decrypt(cipher, TENANT_ID)).isEqualTo(token);
    }

    @Test
    void handleOAuthCallback_gitlab_usesGitlabTokenEndpoint() {
        when(restTemplate.exchange(eq("https://gitlab.com/oauth/token"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"access_token\":\"glpat-oauth\"}"));

        Map<String, Object> result = gitService.handleOAuthCallback(TENANT_ID, "gitlab", "gl-code");

        assertThat(result.get("provider")).isEqualTo("gitlab");
        assertThat(gitService.getOAuthConnection(TENANT_ID, "gitlab")).isNotNull();
    }

    @Test
    void handleOAuthCallback_providerCaseInsensitive() {
        when(restTemplate.exchange(eq(GITHUB_TOKEN_URL), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"access_token\":\"tok\"}"));

        Map<String, Object> result = gitService.handleOAuthCallback(TENANT_ID, "GitHub", "code-2");

        assertThat(result.get("provider")).isEqualTo("github");
        assertThat(gitService.getOAuthConnection(TENANT_ID, "GITHUB")).isNotNull();
    }

    @Test
    void handleOAuthCallback_providerError_degradesAndStoresNothing() {
        when(restTemplate.exchange(eq(GITHUB_TOKEN_URL), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(
                        "{\"error\":\"bad_verification_code\",\"error_description\":\"The code passed is incorrect or expired.\"}"));

        assertThatThrownBy(() -> gitService.handleOAuthCallback(TENANT_ID, "github", "stale-code"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.TOOL_EXECUTION_FAILED.getCode());

        assertThat(gitService.getOAuthConnection(TENANT_ID, "github")).isNull();
    }

    @Test
    void handleOAuthCallback_missingAccessToken_degradesAndStoresNothing() {
        when(restTemplate.exchange(eq(GITHUB_TOKEN_URL), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"token_type\":\"bearer\"}"));

        assertThatThrownBy(() -> gitService.handleOAuthCallback(TENANT_ID, "github", "code-3"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.TOOL_EXECUTION_FAILED.getCode());

        assertThat(gitService.getOAuthConnection(TENANT_ID, "github")).isNull();
    }

    @Test
    void handleOAuthCallback_endpointUnreachable_degradesAndStoresNothing() {
        when(restTemplate.exchange(eq(GITHUB_TOKEN_URL), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new ResourceAccessException("connection timed out"));

        assertThatThrownBy(() -> gitService.handleOAuthCallback(TENANT_ID, "github", "code-4"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.TOOL_EXECUTION_FAILED.getCode());

        assertThat(gitService.getOAuthConnection(TENANT_ID, "github")).isNull();
    }

    @Test
    void handleOAuthCallback_unconfiguredProvider_degradesWithoutHttpCall() {
        assertThatThrownBy(() -> gitService.handleOAuthCallback(TENANT_ID, "bitbucket", "code-5"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.ERROR.getCode());

        assertThat(gitService.getOAuthConnection(TENANT_ID, "bitbucket")).isNull();
        verify(restTemplate, never()).exchange(any(String.class), any(HttpMethod.class), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void handleOAuthCallback_blankClientCredentials_degradesWithoutHttpCall() {
        IntegrationOAuthProperties props = GitIntegrationServiceTest.oauthProperties();
        props.provider("github").setClientId("");
        GitIntegrationService unconfigured = new GitIntegrationService(new ObjectMapper(), restTemplate, encryptor, props);

        assertThatThrownBy(() -> unconfigured.handleOAuthCallback(TENANT_ID, "github", "code-6"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.ERROR.getCode());

        verify(restTemplate, never()).exchange(any(String.class), any(HttpMethod.class), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void getOAuthConnection_otherTenant_cannotSeeToken() {
        when(restTemplate.exchange(eq(GITHUB_TOKEN_URL), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"access_token\":\"tenant-1-token\"}"));
        gitService.handleOAuthCallback(TENANT_ID, "github", "code-7");

        assertThat(gitService.getOAuthConnection(OTHER_TENANT_ID, "github")).isNull();
        assertThat(gitService.getOAuthConnection(TENANT_ID, "github")).isNotNull();
    }

    @Test
    void getOAuthConnection_unknownProvider_returnsNull() {
        assertThat(gitService.getOAuthConnection(TENANT_ID, "github")).isNull();
        assertThat(gitService.getOAuthConnection(null, "github")).isNull();
        assertThat(gitService.getOAuthConnection(TENANT_ID, null)).isNull();
    }
}
