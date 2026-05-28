package com.schemaplexai.system;

import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaPlexaiSystemApplicationTest {

    @Test
    void applicationStartsWithTestConfiguration() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(SchemaPlexaiSystemApplication.class)
                .run(
                        "--server.port=0",
                        "--jwt.secret=this-is-a-very-long-test-secret-for-system"
                )) {
            assertTrue(context.isRunning());
        }
    }
}
