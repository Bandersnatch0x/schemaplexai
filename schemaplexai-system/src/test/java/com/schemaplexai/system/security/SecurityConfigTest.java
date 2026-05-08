package com.schemaplexai.system.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    @Test
    void passwordEncoder_returnsBCryptEncoder() {
        SecurityConfig config = new SecurityConfig();

        PasswordEncoder encoder = config.passwordEncoder();

        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
    }

    @Test
    void passwordEncoder_encodesAndMatches() {
        SecurityConfig config = new SecurityConfig();
        PasswordEncoder encoder = config.passwordEncoder();

        String encoded = encoder.encode("password123");

        assertThat(encoder.matches("password123", encoded)).isTrue();
        assertThat(encoder.matches("wrong", encoded)).isFalse();
    }
}
