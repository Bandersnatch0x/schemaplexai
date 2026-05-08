package com.schemaplexai.agent.engine.a2a;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * HTTP client for inter-agent A2A communication.
 * Supports synchronous and asynchronous message sending, agent discovery,
 * and configurable timeout with retry logic.
 */
@Slf4j
@Component
public class A2aClient {

    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final long DEFAULT_TIMEOUT_SECONDS = 30;

    private final RestTemplate restTemplate;
    private final int maxRetries;
    private final long timeoutSeconds;

    public A2aClient() {
        this(new RestTemplate(), DEFAULT_MAX_RETRIES, DEFAULT_TIMEOUT_SECONDS);
    }

    public A2aClient(RestTemplate restTemplate, int maxRetries, long timeoutSeconds) {
        this.restTemplate = restTemplate;
        this.maxRetries = maxRetries;
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * Send a message to a remote agent synchronously.
     *
     * @param recipient the target agent card
     * @param message   the message to send
     * @return the response message
     * @throws A2aProtocolException if the request fails after retries
     */
    public A2aMessage sendMessage(AgentCard recipient, A2aMessage message) {
        String url = resolveMessageUrl(recipient);
        HttpEntity<A2aMessage> request = buildRequest(message, recipient);

        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxRetries) {
            attempt++;
            try {
                log.debug("Sending A2A message to {} (attempt {}/{})", recipient.getEndpointUrl(), attempt, maxRetries);
                ResponseEntity<A2aMessage> response = restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        request,
                        A2aMessage.class
                );

                if (response.getBody() == null) {
                    throw new A2aProtocolException(
                            A2aProtocolException.ErrorCode.INVALID_MESSAGE,
                            "Empty response body from agent: " + recipient.getName()
                    );
                }

                return response.getBody();
            } catch (RestClientResponseException e) {
                lastException = e;
                if (e.getStatusCode().is4xxClientError()) {
                    if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
                        throw new A2aProtocolException(
                                A2aProtocolException.ErrorCode.AUTHENTICATION_FAILED,
                                "Authentication failed for agent: " + recipient.getName(),
                                e
                        );
                    }
                    throw new A2aProtocolException(
                            A2aProtocolException.ErrorCode.INVALID_MESSAGE,
                            "Invalid message rejected by agent: " + recipient.getName(),
                            e
                    );
                }
                log.warn("A2A request failed (attempt {}/{}): {}", attempt, maxRetries, e.getMessage());
            } catch (ResourceAccessException e) {
                lastException = e;
                log.warn("A2A agent unreachable (attempt {}/{}): {}", attempt, maxRetries, e.getMessage());
            }

            if (attempt < maxRetries) {
                sleepBeforeRetry(attempt);
            }
        }

        throw new A2aProtocolException(
                A2aProtocolException.ErrorCode.AGENT_UNREACHABLE,
                "Agent unreachable after " + maxRetries + " attempts: " + recipient.getName(),
                lastException
        );
    }

    /**
     * Send a message to a remote agent asynchronously.
     *
     * @param recipient the target agent card
     * @param message   the message to send
     * @return a future that completes with the response message
     */
    public CompletableFuture<A2aMessage> sendMessageAsync(AgentCard recipient, A2aMessage message) {
        return CompletableFuture.supplyAsync(() -> sendMessage(recipient, message))
                .orTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    Throwable cause = (ex instanceof java.util.concurrent.CompletionException && ex.getCause() != null)
                            ? ex.getCause()
                            : ex;
                    if (cause instanceof A2aProtocolException) {
                        throw (A2aProtocolException) cause;
                    }
                    throw new A2aProtocolException(
                            A2aProtocolException.ErrorCode.TIMEOUT,
                            "Async A2A request timed out or failed for agent: " + recipient.getName(),
                            cause
                    );
                });
    }

    /**
     * Discover an agent by querying its well-known endpoint.
     *
     * @param endpoint the base URL of the agent (e.g., http://agent-host:8080)
     * @return the discovered agent card
     * @throws A2aProtocolException if discovery fails
     */
    public AgentCard discoverAgent(String endpoint) {
        String discoveryUrl = endpoint.endsWith("/") ? endpoint + ".well-known/agent.json" : endpoint + "/.well-known/agent.json";

        try {
            log.debug("Discovering agent at {}", discoveryUrl);
            ResponseEntity<AgentCard> response = restTemplate.getForEntity(discoveryUrl, AgentCard.class);

            if (response.getBody() == null) {
                throw new A2aProtocolException(
                        A2aProtocolException.ErrorCode.INVALID_MESSAGE,
                        "Empty discovery response from endpoint: " + endpoint
                );
            }

            return response.getBody();
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().is4xxClientError()) {
                throw new A2aProtocolException(
                        A2aProtocolException.ErrorCode.INVALID_MESSAGE,
                        "Discovery endpoint returned client error: " + e.getStatusCode(),
                        e
                );
            }
            throw new A2aProtocolException(
                    A2aProtocolException.ErrorCode.AGENT_UNREACHABLE,
                    "Discovery failed for endpoint: " + endpoint,
                    e
            );
        } catch (ResourceAccessException e) {
            throw new A2aProtocolException(
                    A2aProtocolException.ErrorCode.AGENT_UNREACHABLE,
                    "Agent discovery endpoint unreachable: " + endpoint,
                    e
            );
        }
    }

    private String resolveMessageUrl(AgentCard recipient) {
        String base = recipient.getEndpointUrl();
        return base.endsWith("/") ? base + "a2a/message" : base + "/a2a/message";
    }

    private HttpEntity<A2aMessage> buildRequest(A2aMessage message, AgentCard recipient) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        if (recipient.getAuthenticationType() != null && !recipient.getAuthenticationType().isBlank()) {
            headers.set("X-A2A-Auth-Type", recipient.getAuthenticationType());
        }

        return new HttpEntity<>(message, headers);
    }

    private void sleepBeforeRetry(int attempt) {
        long delayMs = Duration.ofMillis(500L * (1L << (attempt - 1))).toMillis();
        try {
            Thread.sleep(Math.min(delayMs, 10_000L));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new A2aProtocolException(
                    A2aProtocolException.ErrorCode.TIMEOUT,
                    "Retry sleep interrupted", e
            );
        }
    }
}
