package com.schemaplexai.agent.engine.security;

import com.schemaplexai.agent.engine.tool.ValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 基于 JWT 的 SSE Token 验证器。
 * 验证 token 是否有效且与 executionId 匹配。
 */
@Slf4j
@Component
public class JwtSseTokenValidator implements SseTokenValidator {

    // TODO: Integrate with actual JWT validation (e.g., Jwts parser + secret key)
    // For now, performs basic non-empty checks

    @Override
    public ValidationResult validate(String token, String executionId) {
        if (token == null || token.isBlank()) {
            return ValidationResult.invalid("Token is required");
        }
        if (executionId == null || executionId.isBlank()) {
            return ValidationResult.invalid("Execution ID is required");
        }

        // Placeholder: real JWT validation will verify signature, expiry, and
        // check that the token subject matches the executionId
        log.debug("SSE token validation passed for execution {}", executionId);
        return ValidationResult.valid();
    }
}
