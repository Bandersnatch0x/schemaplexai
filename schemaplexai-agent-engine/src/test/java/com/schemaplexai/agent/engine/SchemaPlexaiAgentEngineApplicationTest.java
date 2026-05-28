package com.schemaplexai.agent.engine;

import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaPlexaiAgentEngineApplicationTest {

    @Test
    void applicationStartsWithTestConfiguration() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(SchemaPlexaiAgentEngineApplication.class)
                .run(
                        "--server.port=0",
                        "--spring.datasource.url=jdbc:h2:mem:agent_engine_smoke;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                        "--spring.datasource.driver-class-name=org.h2.Driver",
                        "--spring.datasource.username=sa",
                        "--spring.datasource.password=",
                        "--spring.rabbitmq.listener.simple.auto-startup=false",
                        "--spring.rabbitmq.username=guest",
                        "--spring.rabbitmq.password=guest",
                        "--clickhouse.enabled=false",
                        "--spring.flyway.enabled=false",
                        "--agent.execution.gap-recovery.enabled=false",
                        "--outbox.publisher.enabled=false",
                        "--mcp.discovery.enabled=false",
                        "--jwt.secret=this-is-a-very-long-test-secret-for-agent-engine"
                )) {
            assertTrue(context.isRunning());
        }
    }
}
