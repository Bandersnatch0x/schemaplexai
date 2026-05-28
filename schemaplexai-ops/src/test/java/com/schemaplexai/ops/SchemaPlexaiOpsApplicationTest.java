package com.schemaplexai.ops;

import com.schemaplexai.ops.service.CostDataSyncService;
import com.schemaplexai.ops.service.DisabledCostDataSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaPlexaiOpsApplicationTest {

    @Test
    void applicationStartsWithTestConfiguration() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(SchemaPlexaiOpsApplication.class)
                .run(
                        "--server.port=0",
                        "--spring.datasource.url=jdbc:h2:mem:ops_smoke;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                        "--spring.datasource.driver-class-name=org.h2.Driver",
                        "--spring.datasource.username=sa",
                        "--spring.datasource.password=",
                        "--spring.rabbitmq.listener.simple.auto-startup=false",
                        "--spring.rabbitmq.listener.direct.auto-startup=false",
                        "--clickhouse.enabled=false",
                        "--jwt.secret=this-is-a-very-long-test-secret-for-ops"
                )) {
            assertTrue(context.isRunning());
            assertInstanceOf(DisabledCostDataSyncService.class, context.getBean(CostDataSyncService.class));
        }
    }
}
