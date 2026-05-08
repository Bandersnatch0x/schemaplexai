package com.schemaplexai.agent.engine.a2a;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("A2aClient")
class A2aClientTest {

    @Mock
    private RestTemplate restTemplate;

    private A2aClient client;

    private AgentCard recipient;

    @BeforeEach
    void setUp() {
        client = new A2aClient(restTemplate, 3, 30);
        recipient = AgentCard.builder()
                .name("target-agent")
                .endpointUrl("http://target:8080")
                .authenticationType("bearer")
                .build();
    }

    @Test
    @DisplayName("should send message and return response")
    void shouldSendMessageAndReturnResponse() {
        A2aMessage request = A2aMessage.builder()
                .messageId("req-1")
                .senderAgentId("agent-a")
                .recipientAgentId("agent-b")
                .messageType(A2aMessage.MessageType.REQUEST)
                .payload("hello")
                .build();

        A2aMessage responseBody = A2aMessage.builder()
                .messageId("resp-1")
                .messageType(A2aMessage.MessageType.RESPONSE)
                .payload("world")
                .build();

        when(restTemplate.exchange(
                eq("http://target:8080/a2a/message"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(A2aMessage.class)
        )).thenReturn(ResponseEntity.ok(responseBody));

        A2aMessage result = client.sendMessage(recipient, request);

        assertThat(result.getMessageId()).isEqualTo("resp-1");
        assertThat(result.getMessageType()).isEqualTo(A2aMessage.MessageType.RESPONSE);
        assertThat(result.getPayload()).isEqualTo("world");
    }

    @Test
    @DisplayName("should throw INVALID_MESSAGE when response body is empty")
    void shouldThrowWhenResponseBodyIsEmpty() {
        A2aMessage request = A2aMessage.builder()
                .messageId("req-1")
                .messageType(A2aMessage.MessageType.REQUEST)
                .build();

        when(restTemplate.exchange(
                any(String.class),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(A2aMessage.class)
        )).thenReturn(ResponseEntity.ok(null));

        assertThatThrownBy(() -> client.sendMessage(recipient, request))
                .isInstanceOf(A2aProtocolException.class)
                .satisfies(ex -> {
                    A2aProtocolException a2aEx = (A2aProtocolException) ex;
                    assertThat(a2aEx.getErrorCode()).isEqualTo(A2aProtocolException.ErrorCode.INVALID_MESSAGE);
                    assertThat(a2aEx.getMessage()).contains("Empty response body");
                });
    }

    @Test
    @DisplayName("should throw AUTHENTICATION_FAILED on 401")
    void shouldThrowAuthenticationFailedOn401() {
        A2aMessage request = A2aMessage.builder()
                .messageId("req-1")
                .messageType(A2aMessage.MessageType.REQUEST)
                .build();

        when(restTemplate.exchange(
                any(String.class),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(A2aMessage.class)
        )).thenThrow(new RestClientResponseException("Unauthorized", 401, "Unauthorized", null, null, null));

        assertThatThrownBy(() -> client.sendMessage(recipient, request))
                .isInstanceOf(A2aProtocolException.class)
                .satisfies(ex -> {
                    A2aProtocolException a2aEx = (A2aProtocolException) ex;
                    assertThat(a2aEx.getErrorCode()).isEqualTo(A2aProtocolException.ErrorCode.AUTHENTICATION_FAILED);
                });
    }

    @Test
    @DisplayName("should throw AUTHENTICATION_FAILED on 403")
    void shouldThrowAuthenticationFailedOn403() {
        A2aMessage request = A2aMessage.builder()
                .messageId("req-1")
                .messageType(A2aMessage.MessageType.REQUEST)
                .build();

        when(restTemplate.exchange(
                any(String.class),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(A2aMessage.class)
        )).thenThrow(new RestClientResponseException("Forbidden", 403, "Forbidden", null, null, null));

        assertThatThrownBy(() -> client.sendMessage(recipient, request))
                .isInstanceOf(A2aProtocolException.class)
                .satisfies(ex -> {
                    A2aProtocolException a2aEx = (A2aProtocolException) ex;
                    assertThat(a2aEx.getErrorCode()).isEqualTo(A2aProtocolException.ErrorCode.AUTHENTICATION_FAILED);
                });
    }

    @Test
    @DisplayName("should throw INVALID_MESSAGE on 4xx other than 401/403")
    void shouldThrowInvalidMessageOn4xx() {
        A2aMessage request = A2aMessage.builder()
                .messageId("req-1")
                .messageType(A2aMessage.MessageType.REQUEST)
                .build();

        when(restTemplate.exchange(
                any(String.class),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(A2aMessage.class)
        )).thenThrow(new RestClientResponseException("Bad Request", 400, "Bad Request", null, null, null));

        assertThatThrownBy(() -> client.sendMessage(recipient, request))
                .isInstanceOf(A2aProtocolException.class)
                .satisfies(ex -> {
                    A2aProtocolException a2aEx = (A2aProtocolException) ex;
                    assertThat(a2aEx.getErrorCode()).isEqualTo(A2aProtocolException.ErrorCode.INVALID_MESSAGE);
                });
    }

    @Test
    @DisplayName("should retry on 5xx and eventually throw AGENT_UNREACHABLE")
    void shouldRetryOn5xxAndEventuallyThrow() {
        A2aMessage request = A2aMessage.builder()
                .messageId("req-1")
                .messageType(A2aMessage.MessageType.REQUEST)
                .build();

        when(restTemplate.exchange(
                any(String.class),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(A2aMessage.class)
        )).thenThrow(new RestClientResponseException("Server Error", 500, "Server Error", null, null, null));

        assertThatThrownBy(() -> client.sendMessage(recipient, request))
                .isInstanceOf(A2aProtocolException.class)
                .satisfies(ex -> {
                    A2aProtocolException a2aEx = (A2aProtocolException) ex;
                    assertThat(a2aEx.getErrorCode()).isEqualTo(A2aProtocolException.ErrorCode.AGENT_UNREACHABLE);
                    assertThat(a2aEx.getMessage()).contains("after 3 attempts");
                });

        verify(restTemplate, times(3)).exchange(
                any(String.class),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(A2aMessage.class)
        );
    }

    @Test
    @DisplayName("should retry on ResourceAccessException and eventually throw AGENT_UNREACHABLE")
    void shouldRetryOnResourceAccessException() {
        A2aMessage request = A2aMessage.builder()
                .messageId("req-1")
                .messageType(A2aMessage.MessageType.REQUEST)
                .build();

        when(restTemplate.exchange(
                any(String.class),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(A2aMessage.class)
        )).thenThrow(new ResourceAccessException("Connection refused"));

        assertThatThrownBy(() -> client.sendMessage(recipient, request))
                .isInstanceOf(A2aProtocolException.class)
                .satisfies(ex -> {
                    A2aProtocolException a2aEx = (A2aProtocolException) ex;
                    assertThat(a2aEx.getErrorCode()).isEqualTo(A2aProtocolException.ErrorCode.AGENT_UNREACHABLE);
                });
    }

    @Test
    @DisplayName("should succeed on retry after initial failure")
    void shouldSucceedOnRetry() {
        A2aMessage request = A2aMessage.builder()
                .messageId("req-1")
                .messageType(A2aMessage.MessageType.REQUEST)
                .build();

        A2aMessage responseBody = A2aMessage.builder()
                .messageId("resp-1")
                .messageType(A2aMessage.MessageType.RESPONSE)
                .payload("ok")
                .build();

        when(restTemplate.exchange(
                any(String.class),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(A2aMessage.class)
        ))
                .thenThrow(new ResourceAccessException("Connection refused"))
                .thenReturn(ResponseEntity.ok(responseBody));

        A2aMessage result = client.sendMessage(recipient, request);

        assertThat(result.getPayload()).isEqualTo("ok");
        verify(restTemplate, times(2)).exchange(
                any(String.class),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(A2aMessage.class)
        );
    }

    @Test
    @DisplayName("should send message asynchronously and return response")
    void shouldSendMessageAsync() throws Exception {
        A2aMessage request = A2aMessage.builder()
                .messageId("req-1")
                .messageType(A2aMessage.MessageType.REQUEST)
                .build();

        A2aMessage responseBody = A2aMessage.builder()
                .messageId("resp-1")
                .messageType(A2aMessage.MessageType.RESPONSE)
                .payload("async-ok")
                .build();

        when(restTemplate.exchange(
                any(String.class),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(A2aMessage.class)
        )).thenReturn(ResponseEntity.ok(responseBody));

        CompletableFuture<A2aMessage> future = client.sendMessageAsync(recipient, request);
        A2aMessage result = future.get();

        assertThat(result.getPayload()).isEqualTo("async-ok");
    }

    @Test
    @DisplayName("should discover agent via well-known endpoint")
    void shouldDiscoverAgent() {
        AgentCard discovered = AgentCard.builder()
                .name("remote-agent")
                .version("1.0.0")
                .capabilities(List.of("echo"))
                .endpointUrl("http://remote:8080")
                .authenticationType("none")
                .maxConcurrentExecutions(5)
                .supportedMessageFormats(List.of("json"))
                .build();

        when(restTemplate.getForEntity(
                "http://remote:8080/.well-known/agent.json",
                AgentCard.class
        )).thenReturn(ResponseEntity.ok(discovered));

        AgentCard result = client.discoverAgent("http://remote:8080");

        assertThat(result.getName()).isEqualTo("remote-agent");
        assertThat(result.getVersion()).isEqualTo("1.0.0");
        assertThat(result.getCapabilities()).containsExactly("echo");
    }

    @Test
    @DisplayName("should discover agent with trailing slash endpoint")
    void shouldDiscoverAgentWithTrailingSlash() {
        AgentCard discovered = AgentCard.builder()
                .name("remote-agent")
                .version("1.0.0")
                .endpointUrl("http://remote:8080/")
                .build();

        when(restTemplate.getForEntity(
                "http://remote:8080/.well-known/agent.json",
                AgentCard.class
        )).thenReturn(ResponseEntity.ok(discovered));

        AgentCard result = client.discoverAgent("http://remote:8080/");

        assertThat(result.getName()).isEqualTo("remote-agent");
    }

    @Test
    @DisplayName("should throw INVALID_MESSAGE when discovery response is empty")
    void shouldThrowWhenDiscoveryResponseIsEmpty() {
        when(restTemplate.getForEntity(
                any(String.class),
                eq(AgentCard.class)
        )).thenReturn(ResponseEntity.ok(null));

        assertThatThrownBy(() -> client.discoverAgent("http://remote:8080"))
                .isInstanceOf(A2aProtocolException.class)
                .satisfies(ex -> {
                    A2aProtocolException a2aEx = (A2aProtocolException) ex;
                    assertThat(a2aEx.getErrorCode()).isEqualTo(A2aProtocolException.ErrorCode.INVALID_MESSAGE);
                });
    }

    @Test
    @DisplayName("should throw AGENT_UNREACHABLE when discovery endpoint is unreachable")
    void shouldThrowWhenDiscoveryEndpointIsUnreachable() {
        when(restTemplate.getForEntity(
                any(String.class),
                eq(AgentCard.class)
        )).thenThrow(new ResourceAccessException("Connection refused"));

        assertThatThrownBy(() -> client.discoverAgent("http://remote:8080"))
                .isInstanceOf(A2aProtocolException.class)
                .satisfies(ex -> {
                    A2aProtocolException a2aEx = (A2aProtocolException) ex;
                    assertThat(a2aEx.getErrorCode()).isEqualTo(A2aProtocolException.ErrorCode.AGENT_UNREACHABLE);
                });
    }

    @Test
    @DisplayName("should throw INVALID_MESSAGE on discovery 4xx")
    void shouldThrowInvalidMessageOnDiscovery4xx() {
        when(restTemplate.getForEntity(
                any(String.class),
                eq(AgentCard.class)
        )).thenThrow(new RestClientResponseException("Not Found", 404, "Not Found", null, null, null));

        assertThatThrownBy(() -> client.discoverAgent("http://remote:8080"))
                .isInstanceOf(A2aProtocolException.class)
                .satisfies(ex -> {
                    A2aProtocolException a2aEx = (A2aProtocolException) ex;
                    assertThat(a2aEx.getErrorCode()).isEqualTo(A2aProtocolException.ErrorCode.INVALID_MESSAGE);
                });
    }

    @Test
    @DisplayName("should include X-A2A-Auth-Type header when authenticationType is set")
    void shouldIncludeAuthHeader() {
        A2aMessage request = A2aMessage.builder()
                .messageId("req-1")
                .messageType(A2aMessage.MessageType.REQUEST)
                .build();

        A2aMessage responseBody = A2aMessage.builder()
                .messageId("resp-1")
                .messageType(A2aMessage.MessageType.RESPONSE)
                .build();

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);

        when(restTemplate.exchange(
                any(String.class),
                eq(HttpMethod.POST),
                captor.capture(),
                eq(A2aMessage.class)
        )).thenReturn(ResponseEntity.ok(responseBody));

        client.sendMessage(recipient, request);

        HttpEntity<?> captured = captor.getValue();
        assertThat(captured.getHeaders().get("X-A2A-Auth-Type")).containsExactly("bearer");
    }

    @Test
    @DisplayName("should not include X-A2A-Auth-Type header when authenticationType is blank")
    void shouldNotIncludeAuthHeaderWhenBlank() {
        AgentCard noAuthRecipient = AgentCard.builder()
                .name("open-agent")
                .endpointUrl("http://open:8080")
                .authenticationType(" ")
                .build();

        A2aMessage request = A2aMessage.builder()
                .messageId("req-1")
                .messageType(A2aMessage.MessageType.REQUEST)
                .build();

        A2aMessage responseBody = A2aMessage.builder()
                .messageId("resp-1")
                .messageType(A2aMessage.MessageType.RESPONSE)
                .build();

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);

        when(restTemplate.exchange(
                any(String.class),
                eq(HttpMethod.POST),
                captor.capture(),
                eq(A2aMessage.class)
        )).thenReturn(ResponseEntity.ok(responseBody));

        client.sendMessage(noAuthRecipient, request);

        HttpEntity<?> captured = captor.getValue();
        assertThat(captured.getHeaders().containsKey("X-A2A-Auth-Type")).isFalse();
    }
}
