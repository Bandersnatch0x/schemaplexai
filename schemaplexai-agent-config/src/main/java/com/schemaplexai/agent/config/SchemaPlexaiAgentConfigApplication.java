package com.schemaplexai.agent.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.schemaplexai")
@MapperScan("com.schemaplexai.agent.config.mapper")
public class SchemaPlexaiAgentConfigApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchemaPlexaiAgentConfigApplication.class, args);
    }
}
