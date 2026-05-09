package com.schemaplexai.integration.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;

class GitHubToolExecutorTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private GitHubToolExecutor executor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        executor = new GitHubToolExecutor(restTemplate, objectMapper);
    }

    @Test
    void listRepositories_success_returnsJsonArray() {
        mockServer.expect(requestTo("https://api.github.com/orgs/acme/repos"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andRespond(MockRestResponseCreators.withSuccess(
                        "[{\"id\":1,\"name\":\"repo1\"}]", MediaType.APPLICATION_JSON));

        JsonNode result = executor.listRepositories("test-token", "acme");

        assertThat(result.isArray()).isTrue();
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).get("name").asText()).isEqualTo("repo1");
        mockServer.verify();
    }

    @Test
    void listRepositories_clientError_throwsBaseException() {
        mockServer.expect(requestTo("https://api.github.com/orgs/acme/repos"))
                .andRespond(MockRestResponseCreators.withStatus(org.springframework.http.HttpStatus.NOT_FOUND)
                        .body("{\"message\":\"Not Found\"}"));

        assertThatThrownBy(() -> executor.listRepositories("test-token", "acme"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.TOOL_EXECUTION_FAILED.getCode());
        mockServer.verify();
    }

    @Test
    void listRepositories_serverError_throwsBaseException() {
        mockServer.expect(requestTo("https://api.github.com/orgs/acme/repos"))
                .andRespond(MockRestResponseCreators.withServerError()
                        .body("Internal Server Error"));

        assertThatThrownBy(() -> executor.listRepositories("test-token", "acme"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.TOOL_EXECUTION_FAILED.getCode());
        mockServer.verify();
    }

    @Test
    void createIssue_success_returnsJsonNode() {
        mockServer.expect(requestTo("https://api.github.com/repos/acme/app/issues"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andExpect(MockRestRequestMatchers.jsonPath("$.title").value("Bug report"))
                .andExpect(MockRestRequestMatchers.jsonPath("$.body").value("Something is broken"))
                .andRespond(MockRestResponseCreators.withSuccess(
                        "{\"id\":42,\"title\":\"Bug report\"}", MediaType.APPLICATION_JSON));

        JsonNode result = executor.createIssue("test-token", "acme", "app", "Bug report", "Something is broken");

        assertThat(result.get("id").asInt()).isEqualTo(42);
        assertThat(result.get("title").asText()).isEqualTo("Bug report");
        mockServer.verify();
    }

    @Test
    void createIssue_clientError_throwsBaseException() {
        mockServer.expect(requestTo("https://api.github.com/repos/acme/app/issues"))
                .andRespond(MockRestResponseCreators.withBadRequest()
                        .body("{\"message\":\"Validation Failed\"}"));

        assertThatThrownBy(() -> executor.createIssue("test-token", "acme", "app", "Bug report", "body"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.TOOL_EXECUTION_FAILED.getCode());
        mockServer.verify();
    }

    @Test
    void createPullRequest_success_returnsJsonNode() {
        mockServer.expect(requestTo("https://api.github.com/repos/acme/app/pulls"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andExpect(MockRestRequestMatchers.jsonPath("$.title").value("Feature X"))
                .andExpect(MockRestRequestMatchers.jsonPath("$.head").value("feature-x"))
                .andExpect(MockRestRequestMatchers.jsonPath("$.base").value("main"))
                .andExpect(MockRestRequestMatchers.jsonPath("$.body").value("PR description"))
                .andRespond(MockRestResponseCreators.withSuccess(
                        "{\"id\":99,\"title\":\"Feature X\"}", MediaType.APPLICATION_JSON));

        JsonNode result = executor.createPullRequest(
                "test-token", "acme", "app", "Feature X", "feature-x", "main", "PR description");

        assertThat(result.get("id").asInt()).isEqualTo(99);
        assertThat(result.get("title").asText()).isEqualTo("Feature X");
        mockServer.verify();
    }

    @Test
    void createPullRequest_serverError_throwsBaseException() {
        mockServer.expect(requestTo("https://api.github.com/repos/acme/app/pulls"))
                .andRespond(MockRestResponseCreators.withServerError());

        assertThatThrownBy(() -> executor.createPullRequest(
                        "test-token", "acme", "app", "Feature X", "feature-x", "main", "desc"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.TOOL_EXECUTION_FAILED.getCode());
        mockServer.verify();
    }

    @Test
    void listRepositories_nullResponse_returnsNullNode() {
        mockServer.expect(requestTo("https://api.github.com/orgs/acme/repos"))
                .andRespond(MockRestResponseCreators.withSuccess("", MediaType.APPLICATION_JSON));

        JsonNode result = executor.listRepositories("test-token", "acme");

        assertThat(result.isNull()).isTrue();
        mockServer.verify();
    }
}
