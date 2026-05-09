package com.schemaplexai.integration.tool;

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
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GitLabToolExecutor {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public JsonNode listProjects(String token, String host, String group) {
        log.info("Listing GitLab projects for group: {} on host: {}", group, host);
        String baseUrl = normalizeHost(host);
        String url = baseUrl + "/groups/" + group + "/projects";
        String response = executeGet(url, token);
        return parseJson(response);
    }

    public JsonNode createIssue(String token, String host, String projectId, String title, String description) {
        log.info("Creating GitLab issue in project: {}", projectId);
        String baseUrl = normalizeHost(host);
        String url = baseUrl + "/projects/" + projectId + "/issues";
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", title);
        payload.put("description", description);
        String response = executePost(url, token, payload);
        return parseJson(response);
    }

    public JsonNode createMergeRequest(
            String token, String host, String projectId,
            String title, String sourceBranch, String targetBranch, String description) {
        log.info("Creating GitLab merge request in project: {}", projectId);
        String baseUrl = normalizeHost(host);
        String url = baseUrl + "/projects/" + projectId + "/merge_requests";
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", title);
        payload.put("source_branch", sourceBranch);
        payload.put("target_branch", targetBranch);
        payload.put("description", description);
        String response = executePost(url, token, payload);
        return parseJson(response);
    }

    private String normalizeHost(String host) {
        String trimmed = host.trim();
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed + "/api/v4";
    }

    private String executeGet(String url, String token) {
        HttpHeaders headers = createHeaders(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("GitLab API client error: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BaseException(ResultCode.TOOL_EXECUTION_FAILED,
                    "GitLab API error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        } catch (HttpServerErrorException e) {
            log.error("GitLab API server error: {}", e.getStatusCode());
            throw new BaseException(ResultCode.TOOL_EXECUTION_FAILED,
                    "GitLab API server error: " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("GitLab API request failed: {}", url, e);
            throw new BaseException(ResultCode.TOOL_EXECUTION_FAILED,
                    "GitLab request failed: " + e.getMessage(), e);
        }
    }

    private String executePost(String url, String token, Map<String, Object> payload) {
        HttpHeaders headers = createHeaders(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("GitLab API client error: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BaseException(ResultCode.TOOL_EXECUTION_FAILED,
                    "GitLab API error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        } catch (HttpServerErrorException e) {
            log.error("GitLab API server error: {}", e.getStatusCode());
            throw new BaseException(ResultCode.TOOL_EXECUTION_FAILED,
                    "GitLab API server error: " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("GitLab API request failed: {}", url, e);
            throw new BaseException(ResultCode.TOOL_EXECUTION_FAILED,
                    "GitLab request failed: " + e.getMessage(), e);
        }
    }

    private HttpHeaders createHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private JsonNode parseJson(String response) {
        if (response == null || response.isBlank()) {
            return objectMapper.nullNode();
        }
        try {
            return objectMapper.readTree(response);
        } catch (Exception e) {
            log.error("Failed to parse GitLab response", e);
            throw new BaseException(ResultCode.TOOL_EXECUTION_FAILED,
                    "Failed to parse GitLab response: " + e.getMessage(), e);
        }
    }
}
