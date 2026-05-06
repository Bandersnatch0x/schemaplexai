package com.schemaplexai.common.result;

import lombok.Getter;

@Getter
public enum ResultCode {

    SUCCESS(200, "success"),
    ERROR(500, "system error"),
    INTERNAL_ERROR(500, "internal server error"),
    PARAM_ERROR(400, "param error"),
    UNAUTHORIZED(401, "unauthorized"),
    FORBIDDEN(403, "forbidden"),
    NOT_FOUND(404, "not found"),
    METHOD_NOT_ALLOWED(405, "method not allowed"),
    REQUEST_TIMEOUT(408, "request timeout"),

    // Tenant
    TENANT_NOT_FOUND(1001, "tenant not found"),
    TENANT_DISABLED(1002, "tenant disabled"),

    // Auth
    TOKEN_EXPIRED(2001, "token expired"),
    TOKEN_INVALID(2002, "token invalid"),
    USER_NOT_FOUND(2003, "user not found"),
    PASSWORD_ERROR(2004, "password error"),

    // Agent
    AGENT_NOT_FOUND(3001, "agent not found"),
    AGENT_EXECUTION_FAILED(3002, "agent execution failed"),
    AGENT_RATE_LIMIT(3003, "agent rate limit exceeded"),
    TOKEN_BUDGET_EXCEEDED(3004, "token budget exceeded"),
    LOOP_DETECTED(3005, "agent loop detected"),

    // Workflow
    WORKFLOW_NOT_FOUND(4001, "workflow not found"),
    WORKFLOW_INSTANCE_NOT_FOUND(4002, "workflow instance not found"),

    // Spec
    SPEC_NOT_FOUND(5001, "spec not found"),

    // Quality
    QUALITY_GATE_BLOCKED(6001, "quality gate blocked"),

    // Integration
    INTEGRATION_NOT_FOUND(7001, "integration not found"),
    TOOL_EXECUTION_FAILED(7002, "tool execution failed"),

    // Context
    CONTEXT_NOT_FOUND(8001, "context not found"),
    KNOWLEDGE_DOC_NOT_FOUND(8002, "knowledge document not found"),

    // Sync
    SYNC_CURSOR_ERROR(9001, "sync cursor error");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
