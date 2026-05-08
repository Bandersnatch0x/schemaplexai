package com.schemaplexai.common.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.ApplicationArguments;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class JwtSecretStartupValidatorTest {

    private final ApplicationArguments args = mock(ApplicationArguments.class);

    @Test
    @DisplayName("should pass when secret is exactly 32 bytes")
    void run_secretExactly32Bytes_passes() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("jwt.secret", "this-is-exactly-32-bytes-long-ok");

        JwtSecretStartupValidator validator = new JwtSecretStartupValidator(env);
        validator.run(args);
    }

    @Test
    @DisplayName("should pass when secret is longer than 32 bytes")
    void run_secretLongerThan32Bytes_passes() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("jwt.secret", "this-is-a-very-long-secret-key-that-is-well-over-32-bytes-long");

        JwtSecretStartupValidator validator = new JwtSecretStartupValidator(env);
        validator.run(args);
    }

    @Test
    @DisplayName("should throw IllegalStateException when secret is missing")
    void run_secretMissing_throwsIllegalStateException() {
        MockEnvironment env = new MockEnvironment();

        JwtSecretStartupValidator validator = new JwtSecretStartupValidator(env);

        assertThatThrownBy(() -> validator.run(args))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT secret is not configured");
    }

    @Test
    @DisplayName("should throw IllegalStateException when secret is shorter than 32 bytes")
    void run_secretTooShort_throwsIllegalStateException() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("jwt.secret", "short-secret");

        JwtSecretStartupValidator validator = new JwtSecretStartupValidator(env);

        assertThatThrownBy(() -> validator.run(args))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT secret must be at least 32 bytes");
    }

    @Test
    @DisplayName("should throw IllegalStateException when secret is blank")
    void run_secretBlank_throwsIllegalStateException() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("jwt.secret", "   ");

        JwtSecretStartupValidator validator = new JwtSecretStartupValidator(env);

        assertThatThrownBy(() -> validator.run(args))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT secret is not configured");
    }

    @Test
    @DisplayName("should throw IllegalStateException when secret is empty string")
    void run_secretEmpty_throwsIllegalStateException() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("jwt.secret", "");

        JwtSecretStartupValidator validator = new JwtSecretStartupValidator(env);

        assertThatThrownBy(() -> validator.run(args))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT secret is not configured");
    }
}
