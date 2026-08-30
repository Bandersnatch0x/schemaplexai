package com.schemaplexai.integration;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.schemaplexai.integration", "com.schemaplexai.dao"})
@MapperScan("com.schemaplexai.integration.**.mapper")
@EnableScheduling
public class SchemaPlexaiIntegrationApplication {
    public static void main(String[] args) {
        SpringApplication.run(SchemaPlexaiIntegrationApplication.class, args);
    }
}
