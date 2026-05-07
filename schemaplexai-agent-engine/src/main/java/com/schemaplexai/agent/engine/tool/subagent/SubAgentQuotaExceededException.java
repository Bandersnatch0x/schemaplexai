package com.schemaplexai.agent.engine.tool.subagent;

public class SubAgentQuotaExceededException extends RuntimeException {
    public SubAgentQuotaExceededException(String message) {
        super(message);
    }
}
