package com.schemaplexai.agent.engine.tool;

public enum ToolErrorCategory {
    INVALID_ARGUMENTS(false, false),
    UNEXPECTED_ENVIRONMENT(false, true),
    PROVIDER_ERROR(false, true),
    USER_ABORTED(false, false),
    TIMEOUT(false, true),
    IRREVERSIBLE_OPERATION(true, false),
    ENVIRONMENT_MISMATCH(true, false);

    private final boolean securityRelated;
    private final boolean retryable;

    ToolErrorCategory(boolean securityRelated, boolean retryable) {
        this.securityRelated = securityRelated;
        this.retryable = retryable;
    }

    public boolean isSecurityRelated() {
        return securityRelated;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
