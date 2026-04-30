package com.schemaplexai.workflow;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.schemaplexai.workflow", "com.schemaplexai.dao"})
@MapperScan("com.schemaplexai.workflow.**.mapper")
public class SchemaPlexaiWorkflowApplication {
    public static void main(String[] args) {
        SpringApplication.run(SchemaPlexaiWorkflowApplication.class, args);
    }
}
