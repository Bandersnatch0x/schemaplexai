package com.schemaplexai.integration.e2e;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.integration.service.JenkinsIntegrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * End-to-end integration tests for Jenkins integration service.
 * Covers: build trigger, status query, job listing, build callback handling.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Jenkins Integration End-to-End Tests")
class JenkinsIntegrationEndToEndTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private JenkinsIntegrationService jenkinsService;

    private static final String JENKINS_URL = "http://jenkins.local:8080";
    private static final String JOB_NAME = "deploy-prod";
    private static final String USERNAME = "admin";
    private static final String API_TOKEN = "jenkins-token";

    @BeforeEach
    void setUp() {
        jenkinsService = new JenkinsIntegrationService(restTemplate, new com.fasterxml.jackson.databind.ObjectMapper());
        jenkinsService.clearCache();
    }

    @Test
    @DisplayName("E2E: Trigger build, query status, list jobs, handle callback")
    void fullJenkinsLifecycle() {
        // Step 1: Trigger build
        when(restTemplate.postForObject(any(), any(), eq(String.class))).thenReturn("");
        jenkinsService.triggerBuild(JENKINS_URL, JOB_NAME, USERNAME, API_TOKEN, Map.of("ENV", "prod"));

        // Step 2: Query build status (mocked response)
        String buildJson = "{\"number\":42,\"result\":\"SUCCESS\",\"building\":false,\"duration\":15000,\"timestamp\":1715000000000,\"url\":\"http://jenkins/job/deploy-prod/42/\"}";
        when(restTemplate.exchange(eq(JENKINS_URL + "/job/" + JOB_NAME + "/lastBuild/api/json"), any(), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok(buildJson));

        Map<String, Object> status = jenkinsService.getBuildStatus(JENKINS_URL, JOB_NAME, null, USERNAME, API_TOKEN);
        assertThat(status.get("jobName")).isEqualTo(JOB_NAME);
        assertThat(status.get("result")).isEqualTo("SUCCESS");
        assertThat(status.get("building")).isEqualTo(false);

        // Step 3: List jobs
        String jobsJson = "{\"jobs\":[{\"name\":\"deploy-prod\",\"url\":\"http://jenkins/job/deploy-prod/\",\"color\":\"blue\",\"lastBuild\":{\"number\":42,\"result\":\"SUCCESS\"}}]}";
        when(restTemplate.exchange(eq(JENKINS_URL + "/api/json?tree=jobs[name,url,color,lastBuild[number,result]]"), any(), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok(jobsJson));

        List<Map<String, Object>> jobs = jenkinsService.listJobs(JENKINS_URL, USERNAME, API_TOKEN);
        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).get("name")).isEqualTo("deploy-prod");

        // Step 4: Handle build callback
        jenkinsService.handleBuildCallback(JOB_NAME, "SUCCESS");
        // Build callback stores in cache; getBuildStatus still returns from mocked REST call
        Map<String, Object> latestStatus = jenkinsService.getBuildStatus(JENKINS_URL, JOB_NAME, null, USERNAME, API_TOKEN);
        assertThat(latestStatus.get("result")).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("E2E: Trigger build with null URL throws param error")
    void triggerBuildNullUrl() {
        assertThatThrownBy(() -> jenkinsService.triggerBuild(null, JOB_NAME, USERNAME, API_TOKEN, Map.of()))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    @DisplayName("E2E: Get build status when Jenkins is unreachable returns timeout error")
    void getStatusUnreachable() {
        when(restTemplate.exchange(any(), any(), any(), eq(String.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        assertThatThrownBy(() -> jenkinsService.getBuildStatus(JENKINS_URL, JOB_NAME, 1, USERNAME, API_TOKEN))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.TOOL_EXECUTION_FAILED.getCode());
    }

    @Test
    @DisplayName("E2E: Handle build callback with null job name throws param error")
    void handleCallbackNullJob() {
        assertThatThrownBy(() -> jenkinsService.handleBuildCallback(null, "SUCCESS"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    @DisplayName("E2E: List jobs with null URL throws param error")
    void listJobsNullUrl() {
        assertThatThrownBy(() -> jenkinsService.listJobs(null, USERNAME, API_TOKEN))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }
}
