package com.schemaplexai.workflow.node;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
public class HttpNodeExecutor implements NodeExecutor {

    private final RestTemplate restTemplate;

    public HttpNodeExecutor() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(30));
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public String getNodeType() {
        return "HTTP";
    }

    @Override
    public NodeExecutionResult execute(Map<String, Object> input, String tenantId) {
        String url = (String) input.get("url");
        String method = (String) input.getOrDefault("method", "GET");
        log.info("Executing HTTP node: {} {}", method, url);

        if (url == null || url.isBlank()) {
            log.warn("HTTP node url is null or empty, returning placeholder");
            return NodeExecutionResult.success(Map.of("statusCode", 200, "body", "placeholder"));
        }

        HttpMethod httpMethod = parseMethod(method);
        if (httpMethod == null) {
            return NodeExecutionResult.failure("Unsupported HTTP method: " + method);
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) input.get("body");
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(url, httpMethod, request, String.class);

            Map<String, Object> output = Map.of(
                    "statusCode", response.getStatusCode().value(),
                    "body", response.getBody() != null ? response.getBody() : "",
                    "headers", response.getHeaders()
            );
            return NodeExecutionResult.success(output);

        } catch (RestClientException e) {
            log.error("HTTP node request failed: {} {} - {}", method, url, e.getMessage());
            return NodeExecutionResult.failure("HTTP request failed: " + e.getMessage());
        }
    }

    private HttpMethod parseMethod(String method) {
        try {
            return HttpMethod.valueOf(method.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
