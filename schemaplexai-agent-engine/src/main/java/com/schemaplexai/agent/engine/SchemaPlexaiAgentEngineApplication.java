package com.schemaplexai.agent.engine;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.schemaplexai")
@MapperScan("com.schemaplexai.agent.engine.mapper")
public class SchemaPlexaiAgentEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchemaPlexaiAgentEngineApplication.class, args);
    }
}
