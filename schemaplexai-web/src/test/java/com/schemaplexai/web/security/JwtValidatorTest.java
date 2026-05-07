package com.schemaplexai.web.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtValidatorTest {

    private final JwtValidator validator = new JwtValidator();
    private final String secret = "this-is-a-very-long-secret-key-that-is-at-least-32-bytes";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(validator, "jwtSecret", secret);
    }

    @Test
    void validateJwtSecret_shouldPass_whenSecretIsLongEnough() {
        validator.validateJwtSecret();
    }

    @Test
    void validateJwtSecret_shouldThrow_whenSecretIsTooShort() {
        JwtValidator shortValidator = new JwtValidator();
        ReflectionTestUtils.setField(shortValidator, "jwtSecret", "short");
        assertThatThrownBy(shortValidator::validateJwtSecret)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT secret must be at least 32 bytes");
    }

    @Test
    void validateToken_shouldReturnFalse_whenTokenIsNull() {
        assertThat(validator.validateToken(null)).isFalse();
    }

    @Test
    void validateToken_shouldReturnFalse_whenTokenDoesNotStartWithBearer() {
        assertThat(validator.validateToken("Basic abc")).isFalse();
    }

    @Test
    void validateToken_shouldReturnTrue_whenTokenIsValid() {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder().subject("user1").signWith(key).compact();
        assertThat(validator.validateToken("Bearer " + token)).isTrue();
    }

    @Test
    void validateToken_shouldReturnFalse_whenTokenIsInvalid() {
        assertThat(validator.validateToken("Bearer invalid-token")).isFalse();
    }
}
