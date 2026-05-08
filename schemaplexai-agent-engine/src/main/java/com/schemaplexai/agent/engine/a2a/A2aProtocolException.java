package com.schemaplexai.agent.engine.a2a;

import lombok.Getter;

/**
 * Runtime exception for A2A protocol errors.
 */
@Getter
public class A2aProtocolException extends RuntimeException {

    private final ErrorCode errorCode;

    public A2aProtocolException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public A2aProtocolException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * A2A protocol error codes.
     */
    public enum ErrorCode {
        TIMEOUT,
        INVALID_MESSAGE,
        AGENT_UNREACHABLE,
        AUTHENTICATION_FAILED
    }
}
