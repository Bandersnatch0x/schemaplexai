package com.schemaplexai.agent.config;

import com.schemaplexai.agent.engine.config.SecurityPolicyLoader;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication(scanBasePackages = {"com.schemaplexai.agent.config", "com.schemaplexai.dao"})
@MapperScan({"com.schemaplexai.agent.config.mapper", "com.schemaplexai.dao.mapper"})
@Import(SecurityPolicyLoader.class)
public class SchemaPlexaiAgentConfigApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchemaPlexaiAgentConfigApplication.class, args);
    }
}
