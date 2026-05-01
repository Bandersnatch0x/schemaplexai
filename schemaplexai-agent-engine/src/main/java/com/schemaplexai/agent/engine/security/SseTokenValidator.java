package com.schemaplexai.agent.engine.security;

import com.schemaplexai.agent.engine.tool.ValidationResult;

/**
 * SSE Token 验证接口，确保只有授权用户可以订阅 Agent 执行流。
 */
public interface SseTokenValidator {

    /**
     * 验证 SSE Token
     * @param token SSE Token
     * @param executionId Agent 执行 ID
     * @return 验证结果
     */
    ValidationResult validate(String token, String executionId);
}
