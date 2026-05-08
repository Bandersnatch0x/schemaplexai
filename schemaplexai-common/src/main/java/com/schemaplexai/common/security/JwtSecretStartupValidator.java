package com.schemaplexai.common.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Fail-fast startup validator that enforces JWT_SECRET presence and minimum length.
 * <p>
 * Registered as an {@link ApplicationRunner} so it executes early in the Spring Boot
 * lifecycle. If the secret is missing or shorter than 32 bytes, the application logs a
 * CRITICAL error and throws {@link IllegalStateException}, causing the context to fail
 * fast and the JVM to exit with a non-zero code.
 * <p>
 * This bean is auto-configured via {@code schemaplexai-common} so every downstream
 * service inherits the check without additional code.
 */
@Component
public class JwtSecretStartupValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(JwtSecretStartupValidator.class);

    private static final String JWT_SECRET_PROPERTY = "jwt.secret";
    private static final int MIN_SECRET_LENGTH = 32;

    private final Environment environment;

    public JwtSecretStartupValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        String secret = environment.getProperty(JWT_SECRET_PROPERTY);

        if (!StringUtils.hasText(secret)) {
            log.error("CRITICAL: JWT secret is not configured. Set the JWT_SECRET environment variable or jwt.secret property.");
            throw new IllegalStateException(
                    "JWT secret is not configured. Set the JWT_SECRET environment variable or jwt.secret property.");
        }

        int length = secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        if (length < MIN_SECRET_LENGTH) {
            log.error("CRITICAL: JWT secret must be at least {} bytes (256 bits) long. Current length: {} bytes.",
                    MIN_SECRET_LENGTH, length);
            throw new IllegalStateException(
                    "JWT secret must be at least " + MIN_SECRET_LENGTH + " bytes (256 bits) long. "
                            + "Current length: " + length + " bytes.");
        }

        log.info("JWT secret validated successfully ({} bytes).", length);
    }
}
