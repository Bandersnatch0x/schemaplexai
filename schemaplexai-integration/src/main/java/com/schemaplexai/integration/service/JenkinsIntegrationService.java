package com.schemaplexai.integration.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class JenkinsIntegrationService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // In-memory build status cache: jobName#buildNumber -> status info
    private final Map<String, Map<String, Object>> buildCache = new ConcurrentHashMap<>();

    public void triggerBuild(String jenkinsUrl, String jobName, String username, String apiToken,
                             Map<String, String> parameters) {
        log.info("Trigger Jenkins build for job: {}, url: {}", jobName, jenkinsUrl);
        if (jenkinsUrl == null || jenkinsUrl.isBlank() || jobName == null || jobName.isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Jenkins URL and job name are required");
        }

        String buildUrl = jenkinsUrl + "/job/" + jobName + "/buildWithParameters";
        HttpHeaders headers = buildHeaders(username, apiToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        StringBuilder body = new StringBuilder();
        if (parameters != null) {
            parameters.forEach((key, value) -> {
                if (!body.isEmpty()) body.append("&");
                body.append(key).append("=").append(value);
            });
        }

        HttpEntity<String> request = new HttpEntity<>(body.toString(), headers);
        try {
            restTemplate.postForObject(buildUrl, request, String.class);
            log.info("Jenkins build triggered successfully for job: {}", jobName);
        } catch (Exception e) {
            log.error("Failed to trigger Jenkins build for job: {}", jobName, e);
            throw new BaseException(ResultCode.TOOL_EXECUTION_FAILED,
                    "Failed to trigger Jenkins build: " + e.getMessage());
        }
    }

    public void handleBuildCallback(String jobName, String buildResult) {
        log.info("Handle Jenkins build callback for job: {}, result: {}", jobName, buildResult);
        if (jobName == null || jobName.isBlank() || buildResult == null || buildResult.isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Job name and build result are required");
        }

        String cacheKey = jobName + "#latest";
        Map<String, Object> status = new ConcurrentHashMap<>();
        status.put("jobName", jobName);
        status.put("result", buildResult);
        status.put("timestamp", Instant.now().toString());
        status.put("processed", true);
        buildCache.put(cacheKey, status);

        // Phase 2: Update workflow status or notify agent engine
        processBuildResult(jobName, buildResult);
    }

    public Map<String, Object> getBuildStatus(String jenkinsUrl, String jobName, Integer buildNumber,
                                               String username, String apiToken) {
        if (jenkinsUrl == null || jenkinsUrl.isBlank() || jobName == null || jobName.isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Jenkins URL and job name are required");
        }

        String buildPath = buildNumber != null
                ? "/job/" + jobName + "/" + buildNumber + "/api/json"
                : "/job/" + jobName + "/lastBuild/api/json";
        String url = jenkinsUrl + buildPath;

        HttpHeaders headers = buildHeaders(username, apiToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());

            Map<String, Object> status = new ConcurrentHashMap<>();
            status.put("jobName", jobName);
            status.put("buildNumber", root.path("number").asInt());
            status.put("result", root.path("result").asText("UNKNOWN"));
            status.put("building", root.path("building").asBoolean());
            status.put("duration", root.path("duration").asLong());
            status.put("timestamp", root.path("timestamp").asLong());
            status.put("url", root.path("url").asText());

            String cacheKey = jobName + "#" + root.path("number").asInt();
            buildCache.put(cacheKey, status);

            return status;
        } catch (ResourceAccessException e) {
            log.warn("Jenkins unreachable at {}: {}", url, e.getMessage());
            throw new BaseException(ResultCode.REQUEST_TIMEOUT, "Jenkins is unreachable");
        } catch (IOException e) {
            log.error("Failed to parse Jenkins response for job: {}", jobName, e);
            throw new BaseException(ResultCode.ERROR, "Failed to parse Jenkins response");
        } catch (Exception e) {
            log.error("Failed to get build status for job: {}", jobName, e);
            throw new BaseException(ResultCode.TOOL_EXECUTION_FAILED,
                    "Failed to get build status: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> listJobs(String jenkinsUrl, String username, String apiToken) {
        if (jenkinsUrl == null || jenkinsUrl.isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Jenkins URL is required");
        }

        String url = jenkinsUrl + "/api/json?tree=jobs[name,url,color,lastBuild[number,result]]";
        HttpHeaders headers = buildHeaders(username, apiToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode jobs = root.path("jobs");

            List<Map<String, Object>> result = new ArrayList<>();
            if (jobs.isArray()) {
                for (JsonNode job : jobs) {
                    Map<String, Object> jobInfo = new ConcurrentHashMap<>();
                    jobInfo.put("name", job.path("name").asText());
                    jobInfo.put("url", job.path("url").asText());
                    jobInfo.put("color", job.path("color").asText());

                    JsonNode lastBuild = job.path("lastBuild");
                    if (!lastBuild.isMissingNode()) {
                        jobInfo.put("lastBuildNumber", lastBuild.path("number").asInt());
                        jobInfo.put("lastBuildResult", lastBuild.path("result").asText());
                    }
                    result.add(jobInfo);
                }
            }
            return result;
        } catch (ResourceAccessException e) {
            log.warn("Jenkins unreachable at {}: {}", url, e.getMessage());
            throw new BaseException(ResultCode.REQUEST_TIMEOUT, "Jenkins is unreachable");
        } catch (IOException e) {
            log.error("Failed to parse Jenkins jobs list", e);
            throw new BaseException(ResultCode.ERROR, "Failed to parse Jenkins response");
        } catch (Exception e) {
            log.error("Failed to list Jenkins jobs", e);
            throw new BaseException(ResultCode.TOOL_EXECUTION_FAILED,
                    "Failed to list jobs: " + e.getMessage());
        }
    }

    // --- Private helpers ---

    private HttpHeaders buildHeaders(String username, String apiToken) {
        HttpHeaders headers = new HttpHeaders();
        if (username != null && !username.isBlank() && apiToken != null && !apiToken.isBlank()) {
            String auth = Base64.getEncoder().encodeToString((username + ":" + apiToken).getBytes());
            headers.set("Authorization", "Basic " + auth);
        }
        return headers;
    }

    private void processBuildResult(String jobName, String buildResult) {
        log.info("Processing build result for job {}: {}", jobName, buildResult);
        // TODO: integrate with workflow engine or agent engine to trigger downstream actions
        // e.g., notify agent engine, update spec review status, trigger deployment workflow
    }

    /** Public helper for test isolation. */
    public void clearCache() {
        buildCache.clear();
    }
}
