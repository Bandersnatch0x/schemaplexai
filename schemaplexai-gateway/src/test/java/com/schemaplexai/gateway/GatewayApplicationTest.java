package com.schemaplexai.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayApplicationTest {

    @Test
    void applicationStartsWithTestConfiguration() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(GatewayApplication.class)
                .run(
                        "--server.port=0",
                        "--jwt.secret=this-is-a-very-long-test-secret-for-gateway"
                )) {
            assertTrue(context.isRunning());
        }
    }
}
