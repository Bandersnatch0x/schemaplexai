package com.schemaplexai.integration.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class JenkinsIntegrationService {

    private final RestTemplate restTemplate = new RestTemplate();

    public void triggerBuild(String jenkinsUrl, String jobName, String username, String apiToken,
                             Map<String, String> parameters) {
        log.info("Trigger Jenkins build for job: {}, url: {}", jobName, jenkinsUrl);

        String buildUrl = jenkinsUrl + "/job/" + jobName + "/buildWithParameters";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        if (username != null && apiToken != null) {
            String auth = Base64.getEncoder().encodeToString((username + ":" + apiToken).getBytes());
            headers.set("Authorization", "Basic " + auth);
        }

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
        }
    }

    public void handleBuildCallback(String jobName, String buildResult) {
        log.info("Handle Jenkins build callback for job: {}, result: {}", jobName, buildResult);
        // Phase 1: Log build result
        // Phase 2: Update workflow status or notify agent engine
    }
}
