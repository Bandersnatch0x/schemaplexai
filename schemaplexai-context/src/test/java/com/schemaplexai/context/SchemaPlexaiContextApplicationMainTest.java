package com.schemaplexai.context;

import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaPlexaiContextApplicationMainTest {

    @Test
    void applicationStartsWithTestConfiguration() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(SchemaPlexaiContextApplication.class)
                .run(
                        "--server.port=0",
                        "--jwt.secret=this-is-a-very-long-test-secret-for-context"
                )) {
            assertTrue(context.isRunning());
        }
    }
}
