package com.schemaplexai.integration.mcp;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import lombok.Getter;

/**
 * Structured failure raised by {@link McpClient} whenever an MCP protocol
 * interaction cannot complete.
 * <p>
 * Issue 930: tool discovery must never degrade silently to an empty tool list
 * when the remote server is unreachable or speaks an invalid protocol — an
 * empty list is reserved for the legitimate case of a reachable server that
 * genuinely exposes no tools. Every failure therefore surfaces as this
 * exception, carrying a machine-readable {@link FailureKind} and the endpoint
 * that failed, so callers can isolate, log and degrade explicitly.
 */
@Getter
public class McpClientException extends BaseException {

    /** Classification of the protocol failure. */
    public enum FailureKind {
        /** Connection could not be established (refused, DNS failure, reset). */
        UNREACHABLE,
        /** Connect or read budget (30s module default) expired. */
        TIMEOUT,
        /** The server answered with a non-2xx HTTP status. */
        HTTP_ERROR,
        /** The server answered but the body is not a valid JSON-RPC message. */
        PROTOCOL_ERROR
    }

    private final FailureKind kind;
    private final String endpoint;

    public McpClientException(FailureKind kind, String endpoint, String message) {
        super(codeFor(kind), message);
        this.kind = kind;
        this.endpoint = endpoint;
    }

    public McpClientException(FailureKind kind, String endpoint, String message, Throwable cause) {
        super(codeFor(kind), message, cause);
        this.kind = kind;
        this.endpoint = endpoint;
    }

    private static ResultCode codeFor(FailureKind kind) {
        // UNREACHABLE follows the module precedent (issue 918) of mapping an
        // unusable external endpoint onto REQUEST_TIMEOUT; protocol/HTTP-level
        // failures are tool-execution failures.
        return switch (kind) {
            case UNREACHABLE, TIMEOUT -> ResultCode.REQUEST_TIMEOUT;
            case HTTP_ERROR, PROTOCOL_ERROR -> ResultCode.TOOL_EXECUTION_FAILED;
        };
    }
}
