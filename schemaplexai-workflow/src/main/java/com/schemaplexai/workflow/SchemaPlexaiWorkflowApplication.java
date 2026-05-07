package com.schemaplexai.workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.schemaplexai.workflow", "com.schemaplexai.dao"})
public class SchemaPlexaiWorkflowApplication {
    public static void main(String[] args) {
        SpringApplication.run(SchemaPlexaiWorkflowApplication.class, args);
    }
}
