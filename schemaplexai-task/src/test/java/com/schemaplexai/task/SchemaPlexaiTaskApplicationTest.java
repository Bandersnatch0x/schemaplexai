package com.schemaplexai.task;

import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaPlexaiTaskApplicationTest {

    @Test
    void applicationStartsWithTestConfiguration() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(SchemaPlexaiTaskApplication.class)
                .run(
                        "--server.port=0",
                        "--spring.datasource.url=jdbc:h2:mem:task_smoke;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                        "--spring.datasource.driver-class-name=org.h2.Driver",
                        "--spring.datasource.username=sa",
                        "--spring.datasource.password=",
                        "--spring.rabbitmq.listener.simple.auto-startup=false",
                        "--spring.rabbitmq.username=guest",
                        "--spring.rabbitmq.password=guest",
                        "--jwt.secret=this-is-a-very-long-test-secret-for-task"
                )) {
            assertTrue(context.isRunning());
            assertNotNull(context.getBean(LockProvider.class));
            assertDoesNotThrow(() -> context.getBean(JdbcTemplate.class)
                    .queryForObject("select count(*) from shedlock", Integer.class));
        }
    }
}
