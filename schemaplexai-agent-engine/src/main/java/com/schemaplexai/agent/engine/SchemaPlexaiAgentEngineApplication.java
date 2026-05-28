package com.schemaplexai.agent.engine;

import com.schemaplexai.dao.config.TenantLineInterceptor;
import com.schemaplexai.ops.service.BudgetGuard;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.schemaplexai.agent.engine")
@MapperScan("com.schemaplexai.agent.engine.mapper")
@EnableScheduling
@Import({TenantLineInterceptor.class, BudgetGuard.class})
public class SchemaPlexaiAgentEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchemaPlexaiAgentEngineApplication.class, args);
    }
}
