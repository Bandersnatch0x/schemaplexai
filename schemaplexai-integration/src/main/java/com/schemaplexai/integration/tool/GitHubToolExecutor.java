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
public class GitHubToolExecutor {

    private static final String GITHUB_API_BASE = "https://api.github.com";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public JsonNode listRepositories(String token, String org) {
        log.info("Listing repositories for org: {}", org);
        String url = GITHUB_API_BASE + "/orgs/" + org + "/repos";
        String response = executeGet(url, token);
        return parseJson(response);
    }

    public JsonNode createIssue(String token, String owner, String repo, String title, String body) {
        log.info("Creating issue in {}/{}", owner, repo);
        String url = GITHUB_API_BASE + "/repos/" + owner + "/" + repo + "/issues";
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", title);
        payload.put("body", body);
        String response = executePost(url, token, payload);
        return parseJson(response);
    }

    public JsonNode createPullRequest(
            String token, String owner, String repo,
            String title, String head, String base, String body) {
        log.info("Creating pull request in {}/{}", owner, repo);
        String url = GITHUB_API_BASE + "/repos/" + owner + "/" + repo + "/pulls";
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", title);
        payload.put("head", head);
        payload.put("base", base);
        payload.put("body", body);
        String response = executePost(url, token, payload);
        return parseJson(response);
    }

    private String executeGet(String url, String token) {
        HttpHeaders headers = createHeaders(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("GitHub API client error: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BaseException(ResultCode.TOOL_EXECUTION_FAILED,
                    "GitHub API error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        } catch (HttpServerErrorException e) {
            log.error("GitHub API server error: {}", e.getStatusCode());
            throw new BaseException(ResultCode.TOOL_EXECUTION_FAILED,
                    "GitHub API server error: " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("GitHub API request failed: {}", url, e);
            throw new BaseException(ResultCode.TOOL_EXECUTION_FAILED,
                    "GitHub request failed: " + e.getMessage(), e);
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
            log.error("GitHub API client error: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BaseException(ResultCode.TOOL_EXECUTION_FAILED,
                    "GitHub API error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        } catch (HttpServerErrorException e) {
            log.error("GitHub API server error: {}", e.getStatusCode());
            throw new BaseException(ResultCode.TOOL_EXECUTION_FAILED,
                    "GitHub API server error: " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("GitHub API request failed: {}", url, e);
            throw new BaseException(ResultCode.TOOL_EXECUTION_FAILED,
                    "GitHub request failed: " + e.getMessage(), e);
        }
    }

    private HttpHeaders createHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("Accept", "application/vnd.github+json");
        headers.set("X-GitHub-Api-Version", "2022-11-28");
        return headers;
    }

    private JsonNode parseJson(String response) {
        if (response == null || response.isBlank()) {
            return objectMapper.nullNode();
        }
        try {
            return objectMapper.readTree(response);
        } catch (Exception e) {
            log.error("Failed to parse GitHub response", e);
            throw new BaseException(ResultCode.TOOL_EXECUTION_FAILED,
                    "Failed to parse GitHub response: " + e.getMessage(), e);
        }
    }
}
