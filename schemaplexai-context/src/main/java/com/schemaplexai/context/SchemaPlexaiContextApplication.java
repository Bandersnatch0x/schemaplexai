package com.schemaplexai.context;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.schemaplexai.context", "com.schemaplexai.dao"})
@MapperScan("com.schemaplexai.context.**.mapper")
public class SchemaPlexaiContextApplication {
    public static void main(String[] args) {
        SpringApplication.run(SchemaPlexaiContextApplication.class, args);
    }
}
