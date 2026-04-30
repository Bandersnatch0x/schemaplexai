package com.schemaplexai.integration.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitIntegrationService {

    private final ObjectMapper objectMapper;

    public void handleOAuthCallback(String provider, String code) {
        log.info("Handle OAuth callback for provider: {}, code: {}", provider, code);
        // Phase 1: Store authorization code, exchange for access token
        // Phase 2: Persist token securely for subsequent API calls
    }

    public void handleWebhook(String provider, String payload) {
        log.info("Handle webhook for provider: {}", provider);
        try {
            JsonNode root = objectMapper.readTree(payload);
            String eventType = root.path("event_type").asText("unknown");
            String repository = root.path("repository").path("full_name").asText("unknown");
            log.info("Received {} event for repository {}", eventType, repository);

            // Phase 1: Log and acknowledge
            // Phase 2: Trigger workflow or agent based on event type
        } catch (Exception e) {
            log.error("Failed to parse webhook payload", e);
        }
    }
}
