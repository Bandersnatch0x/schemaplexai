package com.schemaplexai.agent.engine.security;

import com.schemaplexai.agent.engine.tool.ValidationResult;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * 基于 JWT 的 SSE Token 验证器。
 * <p>
 * 验证流程:
 * <ol>
 *   <li>校验 token 和 executionId 非空</li>
 *   <li>使用 HMAC-SHA 密钥验证 JWT 签名</li>
 *   <li>验证 token 未过期 (exp claim)</li>
 *   <li>验证 subject (sub claim) 与 executionId 匹配</li>
 * </ol>
 * <p>
 * 任一步骤失败均返回包含具体原因的 {@link ValidationResult#invalid(String)}。
 */
@Slf4j
@Component
public class JwtSseTokenValidator implements SseTokenValidator {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private volatile SecretKey signingKey;

    @PostConstruct
    void initializeKey() {
        validateSecretOrFail();
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 确保密钥在容器启动时就经过校验，避免运行时才暴露配置错误。
     */
    private void validateSecretOrFail() {
        if (!StringUtils.hasText(jwtSecret)) {
            throw new IllegalStateException(
                    "JWT secret is not configured. Set the JWT_SECRET environment variable.");
        }
        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "JWT secret must be at least 32 bytes long for HMAC-SHA256. "
                            + "Current length: " + jwtSecret.getBytes(StandardCharsets.UTF_8).length);
        }
    }

    @Override
    public ValidationResult validate(String token, String executionId) {
        // ---- 1. 空值校验 ----
        if (token == null || token.isBlank()) {
            log.warn("SSE token validation failed: token is blank");
            return ValidationResult.invalid("Token is required");
        }
        if (executionId == null || executionId.isBlank()) {
            log.warn("SSE token validation failed: executionId is blank");
            return ValidationResult.invalid("Execution ID is required");
        }

        // ---- 2. JWT 签名校验 ----
        final Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (SignatureException e) {
            log.warn("SSE token validation failed: invalid signature for execution {}", executionId, e);
            return ValidationResult.invalid("Token signature is invalid");
        } catch (ExpiredJwtException e) {
            log.warn("SSE token validation failed: token expired for execution {}", executionId, e);
            return ValidationResult.invalid("Token has expired");
        } catch (JwtException e) {
            log.warn("SSE token validation failed: malformed token for execution {}", executionId, e);
            return ValidationResult.invalid("Token is malformed or invalid");
        }

        // ---- 3. Subject 与 executionId 匹配 ----
        final String subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            log.warn("SSE token validation failed: missing subject claim for execution {}", executionId);
            return ValidationResult.invalid("Token is missing subject claim");
        }
        if (!subject.equals(executionId)) {
            log.warn("SSE token validation failed: subject mismatch (expected={}, actual={})",
                    executionId, subject);
            return ValidationResult.invalid("Token subject does not match execution ID");
        }

        log.debug("SSE token validated successfully for execution {}", executionId);
        return ValidationResult.valid();
    }
}
