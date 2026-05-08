package com.schemaplexai.agent.engine.a2a;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("A2aMessageHandler")
class A2aMessageHandlerTest {

    private A2aMessageHandler requestHandler;
    private A2aMessageHandler errorHandler;

    @BeforeEach
    void setUp() {
        requestHandler = new A2aMessageHandler() {
            @Override
            public A2aMessage handleMessage(A2aMessage message) {
                return A2aMessage.builder()
                        .messageId("resp-" + message.getMessageId())
                        .senderAgentId(message.getRecipientAgentId())
                        .recipientAgentId(message.getSenderAgentId())
                        .messageType(A2aMessage.MessageType.RESPONSE)
                        .payload("handled: " + message.getPayload())
                        .correlationId(message.getCorrelationId())
                        .build();
            }

            @Override
            public boolean supports(String messageType) {
                return A2aMessage.MessageType.REQUEST.name().equals(messageType);
            }
        };

        errorHandler = new A2aMessageHandler() {
            @Override
            public A2aMessage handleMessage(A2aMessage message) {
                return A2aMessage.builder()
                        .messageId("ack-" + message.getMessageId())
                        .messageType(A2aMessage.MessageType.RESPONSE)
                        .payload("error acknowledged")
                        .build();
            }

            @Override
            public boolean supports(String messageType) {
                return A2aMessage.MessageType.ERROR.name().equals(messageType);
            }
        };
    }

    @Test
    @DisplayName("request handler should support REQUEST type")
    void requestHandlerShouldSupportRequest() {
        assertThat(requestHandler.supports("REQUEST")).isTrue();
        assertThat(requestHandler.supports("RESPONSE")).isFalse();
        assertThat(requestHandler.supports("STREAM")).isFalse();
        assertThat(requestHandler.supports("ERROR")).isFalse();
    }

    @Test
    @DisplayName("error handler should support ERROR type")
    void errorHandlerShouldSupportError() {
        assertThat(errorHandler.supports("ERROR")).isTrue();
        assertThat(errorHandler.supports("REQUEST")).isFalse();
        assertThat(errorHandler.supports("RESPONSE")).isFalse();
    }

    @Test
    @DisplayName("request handler should process message and return response")
    void requestHandlerShouldProcessMessage() {
        A2aMessage incoming = A2aMessage.builder()
                .messageId("msg-1")
                .senderAgentId("agent-a")
                .recipientAgentId("agent-b")
                .messageType(A2aMessage.MessageType.REQUEST)
                .payload("do something")
                .correlationId("corr-1")
                .build();

        A2aMessage result = requestHandler.handleMessage(incoming);

        assertThat(result.getMessageId()).isEqualTo("resp-msg-1");
        assertThat(result.getSenderAgentId()).isEqualTo("agent-b");
        assertThat(result.getRecipientAgentId()).isEqualTo("agent-a");
        assertThat(result.getMessageType()).isEqualTo(A2aMessage.MessageType.RESPONSE);
        assertThat(result.getPayload()).isEqualTo("handled: do something");
        assertThat(result.getCorrelationId()).isEqualTo("corr-1");
    }

    @Test
    @DisplayName("error handler should process error message")
    void errorHandlerShouldProcessErrorMessage() {
        A2aMessage incoming = A2aMessage.builder()
                .messageId("err-1")
                .messageType(A2aMessage.MessageType.ERROR)
                .payload("something went wrong")
                .build();

        A2aMessage result = errorHandler.handleMessage(incoming);

        assertThat(result.getMessageId()).isEqualTo("ack-err-1");
        assertThat(result.getMessageType()).isEqualTo(A2aMessage.MessageType.RESPONSE);
        assertThat(result.getPayload()).isEqualTo("error acknowledged");
    }

    @Test
    @DisplayName("handler should return null when no response is required")
    void handlerShouldReturnNull() {
        A2aMessageHandler fireAndForgetHandler = new A2aMessageHandler() {
            @Override
            public A2aMessage handleMessage(A2aMessage message) {
                return null;
            }

            @Override
            public boolean supports(String messageType) {
                return A2aMessage.MessageType.STREAM.name().equals(messageType);
            }
        };

        A2aMessage incoming = A2aMessage.builder()
                .messageId("stream-1")
                .messageType(A2aMessage.MessageType.STREAM)
                .payload("chunk")
                .build();

        A2aMessage result = fireAndForgetHandler.handleMessage(incoming);

        assertThat(result).isNull();
        assertThat(fireAndForgetHandler.supports("STREAM")).isTrue();
    }

    @Test
    @DisplayName("handler dispatch should route to correct handler")
    void handlerDispatchShouldRouteCorrectly() {
        List<A2aMessageHandler> handlers = List.of(requestHandler, errorHandler);

        A2aMessage requestMessage = A2aMessage.builder()
                .messageType(A2aMessage.MessageType.REQUEST)
                .build();

        A2aMessageHandler selected = handlers.stream()
                .filter(h -> h.supports(requestMessage.getMessageType().name()))
                .findFirst()
                .orElse(null);

        assertThat(selected).isNotNull();
        assertThat(selected).isSameAs(requestHandler);

        A2aMessage errorMessage = A2aMessage.builder()
                .messageType(A2aMessage.MessageType.ERROR)
                .build();

        A2aMessageHandler selectedError = handlers.stream()
                .filter(h -> h.supports(errorMessage.getMessageType().name()))
                .findFirst()
                .orElse(null);

        assertThat(selectedError).isNotNull();
        assertThat(selectedError).isSameAs(errorHandler);
    }

    @Test
    @DisplayName("handler dispatch should return null when no handler matches")
    void handlerDispatchShouldReturnNullWhenNoMatch() {
        List<A2aMessageHandler> handlers = List.of(requestHandler);

        A2aMessage streamMessage = A2aMessage.builder()
                .messageType(A2aMessage.MessageType.STREAM)
                .build();

        A2aMessageHandler selected = handlers.stream()
                .filter(h -> h.supports(streamMessage.getMessageType().name()))
                .findFirst()
                .orElse(null);

        assertThat(selected).isNull();
    }
}
