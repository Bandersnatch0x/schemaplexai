package com.schemaplexai.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.schemaplexai.web"})
public class SchemaPlexaiWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchemaPlexaiWebApplication.class, args);
    }
}
