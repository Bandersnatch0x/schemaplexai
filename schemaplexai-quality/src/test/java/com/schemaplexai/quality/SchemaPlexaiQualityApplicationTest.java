package com.schemaplexai.quality;

import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaPlexaiQualityApplicationTest {

    @Test
    void applicationStartsWithTestConfiguration() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(SchemaPlexaiQualityApplication.class)
                .run(
                        "--server.port=0",
                        "--spring.datasource.url=jdbc:h2:mem:quality_smoke;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                        "--spring.datasource.driver-class-name=org.h2.Driver",
                        "--spring.datasource.username=sa",
                        "--spring.datasource.password=",
                        "--spring.rabbitmq.username=test",
                        "--spring.rabbitmq.password=test",
                        "--spring.rabbitmq.listener.simple.auto-startup=false",
                        "--spring.rabbitmq.listener.direct.auto-startup=false",
                        "--jwt.secret=this-is-a-very-long-test-secret-for-quality"
                )) {
            assertTrue(context.isRunning());
        }
    }
}
