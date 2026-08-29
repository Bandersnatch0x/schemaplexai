package com.schemaplexai.spec;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(
        scanBasePackages = {"com.schemaplexai.spec", "com.schemaplexai.dao"},
        exclude = UserDetailsServiceAutoConfiguration.class)
@MapperScan("com.schemaplexai.spec.**.mapper")
public class SchemaPlexaiSpecApplication {
    public static void main(String[] args) {
        SpringApplication.run(SchemaPlexaiSpecApplication.class, args);
    }
}
