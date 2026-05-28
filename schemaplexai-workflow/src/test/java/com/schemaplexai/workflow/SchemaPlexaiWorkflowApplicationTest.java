package com.schemaplexai.workflow;

import org.flowable.engine.RepositoryService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaPlexaiWorkflowApplicationTest {

    @Test
    void applicationStartsWithTestConfiguration() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(SchemaPlexaiWorkflowApplication.class)
                .run(
                        "--server.port=0",
                        "--spring.datasource.url=jdbc:h2:mem:workflow_smoke;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                        "--spring.datasource.driver-class-name=org.h2.Driver",
                        "--spring.datasource.username=sa",
                        "--spring.datasource.password=",
                        "--flowable.async-executor-activate=false",
                        "--flowable.history-level=none",
                        "--flowable.deployment.enabled=false",
                        "--jwt.secret=this-is-a-very-long-test-secret-for-workflow"
                )) {
            assertTrue(context.isRunning());
            assertNotNull(context.getBean(RepositoryService.class));
        }
    }
}
