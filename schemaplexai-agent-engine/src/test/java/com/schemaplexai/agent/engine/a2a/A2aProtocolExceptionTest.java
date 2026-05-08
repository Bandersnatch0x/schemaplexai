package com.schemaplexai.agent.engine.a2a;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("A2aProtocolException")
class A2aProtocolExceptionTest {

    @Test
    @DisplayName("should create exception with error code and message")
    void shouldCreateWithErrorCodeAndMessage() {
        A2aProtocolException ex = new A2aProtocolException(
                A2aProtocolException.ErrorCode.TIMEOUT,
                "Request timed out"
        );

        assertThat(ex.getErrorCode()).isEqualTo(A2aProtocolException.ErrorCode.TIMEOUT);
        assertThat(ex.getMessage()).isEqualTo("Request timed out");
        assertThat(ex.getCause()).isNull();
    }

    @Test
    @DisplayName("should create exception with error code, message and cause")
    void shouldCreateWithErrorCodeMessageAndCause() {
        Throwable cause = new RuntimeException("root cause");
        A2aProtocolException ex = new A2aProtocolException(
                A2aProtocolException.ErrorCode.AGENT_UNREACHABLE,
                "Agent not reachable",
                cause
        );

        assertThat(ex.getErrorCode()).isEqualTo(A2aProtocolException.ErrorCode.AGENT_UNREACHABLE);
        assertThat(ex.getMessage()).isEqualTo("Agent not reachable");
        assertThat(ex.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("should support all error codes")
    void shouldSupportAllErrorCodes() {
        assertThat(A2aProtocolException.ErrorCode.TIMEOUT.name()).isEqualTo("TIMEOUT");
        assertThat(A2aProtocolException.ErrorCode.INVALID_MESSAGE.name()).isEqualTo("INVALID_MESSAGE");
        assertThat(A2aProtocolException.ErrorCode.AGENT_UNREACHABLE.name()).isEqualTo("AGENT_UNREACHABLE");
        assertThat(A2aProtocolException.ErrorCode.AUTHENTICATION_FAILED.name()).isEqualTo("AUTHENTICATION_FAILED");
    }

    @Test
    @DisplayName("should be a RuntimeException")
    void shouldBeRuntimeException() {
        A2aProtocolException ex = new A2aProtocolException(
                A2aProtocolException.ErrorCode.INVALID_MESSAGE,
                "bad msg"
        );

        assertThat(ex).isInstanceOf(RuntimeException.class);
    }
}
