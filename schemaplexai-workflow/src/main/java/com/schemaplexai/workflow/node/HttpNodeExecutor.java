package com.schemaplexai.workflow.node;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
public class HttpNodeExecutor implements NodeExecutor {

    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    private static final int CONNECT_TIMEOUT_SECONDS = 5;

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
            log.warn("HTTP node url is null or empty");
            return NodeExecutionResult.failure("Missing or empty required field: url");
        }

        HttpMethod httpMethod = parseMethod(method);
        if (httpMethod == null) {
            return NodeExecutionResult.failure("Unsupported HTTP method: " + method);
        }

        int timeoutSeconds = readTimeoutSeconds(input);
        RestTemplate restTemplate = buildRestTemplate(timeoutSeconds);

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

        } catch (ResourceAccessException e) {
            // Connect/read timeout or connection failure: transient -> engine retries
            // with exponential backoff (spec §8).
            log.error("HTTP node request failed (transient): {} {} after {}s - {}",
                    method, url, timeoutSeconds, e.getMessage());
            return NodeExecutionResult.retryableFailure("HTTP request failed: " + e.getMessage());
        } catch (RestClientException e) {
            log.error("HTTP node request failed: {} {} - {}", method, url, e.getMessage());
            return NodeExecutionResult.retryableFailure("HTTP request failed: " + e.getMessage());
        }
    }

    private int readTimeoutSeconds(Map<String, Object> input) {
        Object raw = input.get("timeoutSeconds");
        if (raw instanceof Number number) {
            int value = number.intValue();
            if (value > 0) {
                return value;
            }
        }
        return DEFAULT_TIMEOUT_SECONDS;
    }

    private RestTemplate buildRestTemplate(int timeoutSeconds) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(Math.min(CONNECT_TIMEOUT_SECONDS, timeoutSeconds)));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        return new RestTemplate(factory);
    }

    private HttpMethod parseMethod(String method) {
        if (method == null) {
            return null;
        }
        return switch (method.toUpperCase()) {
            case "GET", "POST", "PUT", "DELETE" -> HttpMethod.valueOf(method.toUpperCase());
            default -> null;
        };
    }
}
