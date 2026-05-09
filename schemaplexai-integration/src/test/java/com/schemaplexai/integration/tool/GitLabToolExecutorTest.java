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

class GitLabToolExecutorTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private GitLabToolExecutor executor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        executor = new GitLabToolExecutor(restTemplate, objectMapper);
    }

    @Test
    void listProjects_success_returnsJsonArray() {
        mockServer.expect(requestTo("https://gitlab.example.com/api/v4/groups/my-group/projects"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andRespond(MockRestResponseCreators.withSuccess(
                        "[{\"id\":1,\"name\":\"project1\"}]", MediaType.APPLICATION_JSON));

        JsonNode result = executor.listProjects("test-token", "https://gitlab.example.com", "my-group");

        assertThat(result.isArray()).isTrue();
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).get("name").asText()).isEqualTo("project1");
        mockServer.verify();
    }

    @Test
    void listProjects_withTrailingSlash_normalizesHost() {
        mockServer.expect(requestTo("https://gitlab.example.com/api/v4/groups/my-group/projects"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(MockRestResponseCreators.withSuccess("[]", MediaType.APPLICATION_JSON));

        JsonNode result = executor.listProjects("test-token", "https://gitlab.example.com/", "my-group");

        assertThat(result.isArray()).isTrue();
        assertThat(result.size()).isEqualTo(0);
        mockServer.verify();
    }

    @Test
    void listProjects_clientError_throwsBaseException() {
        mockServer.expect(requestTo("https://gitlab.example.com/api/v4/groups/my-group/projects"))
                .andRespond(MockRestResponseCreators.withStatus(org.springframework.http.HttpStatus.NOT_FOUND)
                        .body("{\"message\":\"404 Group Not Found\"}"));

        assertThatThrownBy(() -> executor.listProjects("test-token", "https://gitlab.example.com", "my-group"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.TOOL_EXECUTION_FAILED.getCode());
        mockServer.verify();
    }

    @Test
    void listProjects_serverError_throwsBaseException() {
        mockServer.expect(requestTo("https://gitlab.example.com/api/v4/groups/my-group/projects"))
                .andRespond(MockRestResponseCreators.withServerError());

        assertThatThrownBy(() -> executor.listProjects("test-token", "https://gitlab.example.com", "my-group"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.TOOL_EXECUTION_FAILED.getCode());
        mockServer.verify();
    }

    @Test
    void createIssue_success_returnsJsonNode() {
        mockServer.expect(requestTo("https://gitlab.example.com/api/v4/projects/42/issues"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andExpect(MockRestRequestMatchers.jsonPath("$.title").value("Bug report"))
                .andExpect(MockRestRequestMatchers.jsonPath("$.description").value("Something is broken"))
                .andRespond(MockRestResponseCreators.withSuccess(
                        "{\"id\":101,\"title\":\"Bug report\"}", MediaType.APPLICATION_JSON));

        JsonNode result = executor.createIssue("test-token", "https://gitlab.example.com", "42", "Bug report", "Something is broken");

        assertThat(result.get("id").asInt()).isEqualTo(101);
        assertThat(result.get("title").asText()).isEqualTo("Bug report");
        mockServer.verify();
    }

    @Test
    void createIssue_withSlashInProjectId_success() {
        mockServer.expect(requestTo("https://gitlab.example.com/api/v4/projects/group/project/issues"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withSuccess(
                        "{\"id\":102}", MediaType.APPLICATION_JSON));

        JsonNode result = executor.createIssue("test-token", "https://gitlab.example.com", "group/project", "Title", "Desc");

        assertThat(result.get("id").asInt()).isEqualTo(102);
        mockServer.verify();
    }

    @Test
    void createIssue_clientError_throwsBaseException() {
        mockServer.expect(requestTo("https://gitlab.example.com/api/v4/projects/42/issues"))
                .andRespond(MockRestResponseCreators.withBadRequest());

        assertThatThrownBy(() -> executor.createIssue("test-token", "https://gitlab.example.com", "42", "Title", "Desc"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.TOOL_EXECUTION_FAILED.getCode());
        mockServer.verify();
    }

    @Test
    void createMergeRequest_success_returnsJsonNode() {
        mockServer.expect(requestTo("https://gitlab.example.com/api/v4/projects/42/merge_requests"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andExpect(MockRestRequestMatchers.jsonPath("$.title").value("Feature X"))
                .andExpect(MockRestRequestMatchers.jsonPath("$.source_branch").value("feature-x"))
                .andExpect(MockRestRequestMatchers.jsonPath("$.target_branch").value("main"))
                .andExpect(MockRestRequestMatchers.jsonPath("$.description").value("MR description"))
                .andRespond(MockRestResponseCreators.withSuccess(
                        "{\"id\":201,\"title\":\"Feature X\"}", MediaType.APPLICATION_JSON));

        JsonNode result = executor.createMergeRequest(
                "test-token", "https://gitlab.example.com", "42", "Feature X",
                "feature-x", "main", "MR description");

        assertThat(result.get("id").asInt()).isEqualTo(201);
        assertThat(result.get("title").asText()).isEqualTo("Feature X");
        mockServer.verify();
    }

    @Test
    void createMergeRequest_serverError_throwsBaseException() {
        mockServer.expect(requestTo("https://gitlab.example.com/api/v4/projects/42/merge_requests"))
                .andRespond(MockRestResponseCreators.withServerError());

        assertThatThrownBy(() -> executor.createMergeRequest(
                        "test-token", "https://gitlab.example.com", "42", "Feature X",
                        "feature-x", "main", "desc"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.TOOL_EXECUTION_FAILED.getCode());
        mockServer.verify();
    }

    @Test
    void listProjects_nullResponse_returnsNullNode() {
        mockServer.expect(requestTo("https://gitlab.example.com/api/v4/groups/my-group/projects"))
                .andRespond(MockRestResponseCreators.withSuccess("", MediaType.APPLICATION_JSON));

        JsonNode result = executor.listProjects("test-token", "https://gitlab.example.com", "my-group");

        assertThat(result.isNull()).isTrue();
        mockServer.verify();
    }
}
