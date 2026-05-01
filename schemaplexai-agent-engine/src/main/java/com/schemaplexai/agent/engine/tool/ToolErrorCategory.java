package com.schemaplexai.agent.engine.tool;

public enum ToolErrorCategory {
    INVALID_ARGUMENTS(false),
    UNEXPECTED_ENVIRONMENT(false),
    PROVIDER_ERROR(false),
    USER_ABORTED(false),
    TIMEOUT(false),
    UNAUTHORIZED_SCOPE(true);

    private final boolean securityRelated;

    ToolErrorCategory(boolean securityRelated) {
        this.securityRelated = securityRelated;
    }

    public boolean isSecurityRelated() {
        return securityRelated;
    }
}
