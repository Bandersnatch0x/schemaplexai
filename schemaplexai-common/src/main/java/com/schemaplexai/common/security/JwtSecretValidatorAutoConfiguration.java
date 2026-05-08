package com.schemaplexai.common.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Auto-configures {@link JwtSecretStartupValidator} for any Spring Boot service that
 * depends on {@code schemaplexai-common}. Registered via
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports},
 * so downstream services do not need to widen their {@code @ComponentScan}.
 */
@AutoConfiguration
@Import(JwtSecretStartupValidator.class)
public class JwtSecretValidatorAutoConfiguration {
}
