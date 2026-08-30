package com.schemaplexai.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaPlexaiIntegrationApplicationTest {

    @Test
    void applicationStartsWithTestConfiguration() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(SchemaPlexaiIntegrationApplication.class)
                .run(
                        "--server.port=0",
                        "--spring.datasource.url=jdbc:h2:mem:integration_smoke;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                        "--spring.datasource.driver-class-name=org.h2.Driver",
                        "--spring.datasource.username=sa",
                        "--spring.datasource.password=",
                        "--jwt.secret=this-is-a-very-long-test-secret-for-integration",
                        // ST-04: encryptor fails fast without a master secret; smoke
                        // contexts must supply an explicit test value.
                        "--integration.encryption.master-secret=integration-smoke-test-master-secret"
                )) {
            assertTrue(context.isRunning());
        }
    }
}
