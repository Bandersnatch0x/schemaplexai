package com.schemaplexai.integration;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.schemaplexai.integration", "com.schemaplexai.dao"})
@MapperScan("com.schemaplexai.integration.**.mapper")
public class SchemaPlexaiIntegrationApplication {
    public static void main(String[] args) {
        SpringApplication.run(SchemaPlexaiIntegrationApplication.class, args);
    }
}
