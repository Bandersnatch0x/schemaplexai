package com.schemaplexai.web;

import com.schemaplexai.agent.config.service.impl.AgentShadowConfigServiceImpl;
import com.schemaplexai.agent.config.service.impl.TenantEnvironmentConfigServiceImpl;
import com.schemaplexai.agent.engine.config.SecurityPolicyLoader;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication(scanBasePackages = {"com.schemaplexai.web"})
@Import({
        AgentShadowConfigServiceImpl.class,
        TenantEnvironmentConfigServiceImpl.class,
        SecurityPolicyLoader.class
})
@MapperScan({
        "com.schemaplexai.agent.config.mapper",
        "com.schemaplexai.agent.engine.mapper",
        "com.schemaplexai.dao.mapper"
})
public class SchemaPlexaiWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchemaPlexaiWebApplication.class, args);
    }
}
