package com.schemaplexai.integration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JenkinsIntegrationServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private JenkinsIntegrationService jenkinsService;

    @BeforeEach
    void setUp() {
        jenkinsService.clearCache();
        ReflectionTestUtils.setField(jenkinsService, "objectMapper", new ObjectMapper());
    }

    // --- triggerBuild ---

    @Test
    void triggerBuild_nullUrl_throwsParamError() {
        assertThatThrownBy(() -> jenkinsService.triggerBuild(null, "job", "user", "token", Map.of()))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void triggerBuild_nullJobName_throwsParamError() {
        assertThatThrownBy(() -> jenkinsService.triggerBuild("http://jenkins", null, "user", "token", Map.of()))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void triggerBuild_success() {
        jenkinsService.triggerBuild("http://jenkins", "my-job", "user", "token", Map.of("BRANCH", "main"));
        // no exception
    }

    @Test
    void triggerBuild_nullParameters_usesEmptyBody() {
        jenkinsService.triggerBuild("http://jenkins", "my-job", "user", "token", null);
        // no exception
    }

    @Test
    void triggerBuild_emptyParameters_usesEmptyBody() {
        jenkinsService.triggerBuild("http://jenkins", "my-job", "user", "token", Map.of());
        // no exception
    }

    @Test
    void triggerBuild_urlEncodesParameterNamesAndValues() {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("BRANCH", "feature/my branch");
        parameters.put("CAUSE", "manual run");

        jenkinsService.triggerBuild("http://jenkins", "deploy", "user", "token", parameters);

        @SuppressWarnings({"rawtypes", "unchecked"})
        ArgumentCaptor<HttpEntity<String>> requestCaptor =
                (ArgumentCaptor) ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(
                eq("http://jenkins/job/deploy/buildWithParameters"),
                requestCaptor.capture(),
                eq(String.class));
        assertThat(requestCaptor.getValue().getBody())
                .isEqualTo("BRANCH=feature%2Fmy+branch&CAUSE=manual+run");
    }

    @Test
    void triggerBuild_restTemplateThrows_wrapsInBaseException() {
        when(restTemplate.postForObject(any(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("connection refused"));

        assertThatThrownBy(() -> jenkinsService.triggerBuild("http://jenkins", "my-job", "user", "token", Map.of()))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.TOOL_EXECUTION_FAILED.getCode());
    }

    // --- handleBuildCallback ---

    @Test
    void handleBuildCallback_nullJobName_throwsParamError() {
        assertThatThrownBy(() -> jenkinsService.handleBuildCallback(null, "SUCCESS"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void handleBuildCallback_nullResult_throwsParamError() {
        assertThatThrownBy(() -> jenkinsService.handleBuildCallback("job", null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void handleBuildCallback_success() {
        jenkinsService.handleBuildCallback("my-job", "SUCCESS");
        // Verify cache directly via reflection
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> cache = (Map<String, Map<String, Object>>)
                ReflectionTestUtils.getField(jenkinsService, "buildCache");
        assertThat(cache).isNotNull();
        Map<String, Object> status = cache.get("my-job#latest");
        assertThat(status.get("jobName")).isEqualTo("my-job");
        assertThat(status.get("result")).isEqualTo("SUCCESS");
        assertThat(status.get("processed")).isEqualTo(true);
    }

    // --- getBuildStatus ---

    @Test
    void getBuildStatus_nullUrl_throwsParamError() {
        assertThatThrownBy(() -> jenkinsService.getBuildStatus(null, "job", 1, "user", "token"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void getBuildStatus_nullJobName_throwsParamError() {
        assertThatThrownBy(() -> jenkinsService.getBuildStatus("http://jenkins", null, 1, "user", "token"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void getBuildStatus_withoutAuth_success() throws Exception {
        String json = "{\"number\":42,\"result\":\"SUCCESS\",\"building\":false,\"duration\":5000,\"timestamp\":1609459200000,\"url\":\"http://jenkins/job/my-job/42/\"}";
        when(restTemplate.exchange(eq("http://jenkins/job/my-job/42/api/json"), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(json));

        Map<String, Object> status = jenkinsService.getBuildStatus("http://jenkins", "my-job", 42, null, null);
        assertThat(status.get("buildNumber")).isEqualTo(42);
    }

    @Test
    void getBuildStatus_specificBuildNumber_success() throws Exception {
        String json = "{\"number\":42,\"result\":\"SUCCESS\",\"building\":false,\"duration\":5000,\"timestamp\":1609459200000,\"url\":\"http://jenkins/job/my-job/42/\"}";
        when(restTemplate.exchange(eq("http://jenkins/job/my-job/42/api/json"), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(json));

        Map<String, Object> status = jenkinsService.getBuildStatus("http://jenkins", "my-job", 42, "user", "token");
        assertThat(status.get("buildNumber")).isEqualTo(42);
        assertThat(status.get("result")).isEqualTo("SUCCESS");
        assertThat(status.get("building")).isEqualTo(false);
    }

    @Test
    void getBuildStatus_latestBuild_success() throws Exception {
        String json = "{\"number\":99,\"result\":\"FAILURE\",\"building\":false,\"duration\":3000,\"timestamp\":1609459200000,\"url\":\"http://jenkins/job/my-job/99/\"}";
        when(restTemplate.exchange(eq("http://jenkins/job/my-job/lastBuild/api/json"), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(json));

        Map<String, Object> status = jenkinsService.getBuildStatus("http://jenkins", "my-job", null, "user", "token");
        assertThat(status.get("buildNumber")).isEqualTo(99);
        assertThat(status.get("result")).isEqualTo("FAILURE");
    }

    @Test
    void getBuildStatus_resourceAccessException_throwsRequestTimeout() {
        when(restTemplate.exchange(eq("http://jenkins/job/my-job/1/api/json"), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        assertThatThrownBy(() -> jenkinsService.getBuildStatus("http://jenkins", "my-job", 1, "user", "token"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.REQUEST_TIMEOUT.getCode());
    }

    @Test
    void getBuildStatus_ioException_throwsError() throws Exception {
        when(restTemplate.exchange(eq("http://jenkins/job/my-job/1/api/json"), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("not-json"));

        assertThatThrownBy(() -> jenkinsService.getBuildStatus("http://jenkins", "my-job", 1, "user", "token"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.ERROR.getCode());
    }

    @Test
    void getBuildStatus_genericException_throwsToolExecutionFailed() {
        when(restTemplate.exchange(eq("http://jenkins/job/my-job/1/api/json"), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> jenkinsService.getBuildStatus("http://jenkins", "my-job", 1, "user", "token"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.TOOL_EXECUTION_FAILED.getCode());
    }

    // --- listJobs ---

    @Test
    void listJobs_nullUrl_throwsParamError() {
        assertThatThrownBy(() -> jenkinsService.listJobs(null, "user", "token"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void listJobs_withoutAuth_success() throws Exception {
        String json = "{\"jobs\":[{\"name\":\"job1\",\"url\":\"http://jenkins/job/job1/\",\"color\":\"blue\"}]}";
        when(restTemplate.exchange(eq("http://jenkins/api/json?tree=jobs[name,url,color,lastBuild[number,result]]"), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(json));

        List<Map<String, Object>> jobs = jenkinsService.listJobs("http://jenkins", null, null);
        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).get("name")).isEqualTo("job1");
    }

    @Test
    void listJobs_success() throws Exception {
        String json = "{\"jobs\":[{\"name\":\"job1\",\"url\":\"http://jenkins/job/job1/\",\"color\":\"blue\",\"lastBuild\":{\"number\":10,\"result\":\"SUCCESS\"}}]}";
        when(restTemplate.exchange(eq("http://jenkins/api/json?tree=jobs[name,url,color,lastBuild[number,result]]"), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(json));

        List<Map<String, Object>> jobs = jenkinsService.listJobs("http://jenkins", "user", "token");
        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).get("name")).isEqualTo("job1");
        assertThat(jobs.get(0).get("lastBuildNumber")).isEqualTo(10);
        assertThat(jobs.get(0).get("lastBuildResult")).isEqualTo("SUCCESS");
    }

    @Test
    void listJobs_emptyJobsArray_returnsEmpty() throws Exception {
        String json = "{\"jobs\":[]}";
        when(restTemplate.exchange(eq("http://jenkins/api/json?tree=jobs[name,url,color,lastBuild[number,result]]"), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(json));

        List<Map<String, Object>> jobs = jenkinsService.listJobs("http://jenkins", "user", "token");
        assertThat(jobs).isEmpty();
    }

    @Test
    void listJobs_noLastBuild_skipsLastBuildFields() throws Exception {
        String json = "{\"jobs\":[{\"name\":\"job1\",\"url\":\"http://jenkins/job/job1/\",\"color\":\"blue\"}]}";
        when(restTemplate.exchange(eq("http://jenkins/api/json?tree=jobs[name,url,color,lastBuild[number,result]]"), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(json));

        List<Map<String, Object>> jobs = jenkinsService.listJobs("http://jenkins", "user", "token");
        assertThat(jobs.get(0)).doesNotContainKey("lastBuildNumber");
    }

    @Test
    void listJobs_resourceAccessException_throwsRequestTimeout() {
        when(restTemplate.exchange(eq("http://jenkins/api/json?tree=jobs[name,url,color,lastBuild[number,result]]"), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        assertThatThrownBy(() -> jenkinsService.listJobs("http://jenkins", "user", "token"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.REQUEST_TIMEOUT.getCode());
    }

    @Test
    void listJobs_ioException_throwsError() throws Exception {
        when(restTemplate.exchange(eq("http://jenkins/api/json?tree=jobs[name,url,color,lastBuild[number,result]]"), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("not-json"));

        assertThatThrownBy(() -> jenkinsService.listJobs("http://jenkins", "user", "token"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.ERROR.getCode());
    }

    @Test
    void listJobs_genericException_throwsToolExecutionFailed() {
        when(restTemplate.exchange(eq("http://jenkins/api/json?tree=jobs[name,url,color,lastBuild[number,result]]"), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> jenkinsService.listJobs("http://jenkins", "user", "token"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.TOOL_EXECUTION_FAILED.getCode());
    }
}
