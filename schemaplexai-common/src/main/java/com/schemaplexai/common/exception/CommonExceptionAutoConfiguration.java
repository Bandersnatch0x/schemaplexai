package com.schemaplexai.common.exception;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Auto-configures {@link GlobalExceptionHandler} for any Spring Boot service that
 * depends on {@code schemaplexai-common}. Registered via
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports},
 * so downstream services do not need to widen their {@code @ComponentScan}.
 *
 * <p>Only effective for Spring MVC services. The WebFlux gateway uses its own
 * exception handling chain.</p>
 */
@AutoConfiguration
@Import(GlobalExceptionHandler.class)
public class CommonExceptionAutoConfiguration {
}
