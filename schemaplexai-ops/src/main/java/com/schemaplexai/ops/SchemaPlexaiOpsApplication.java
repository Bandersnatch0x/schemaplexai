package com.schemaplexai.ops;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.schemaplexai.ops", "com.schemaplexai.dao"})
@MapperScan("com.schemaplexai.ops.**.mapper")
@EnableScheduling
public class SchemaPlexaiOpsApplication {
    public static void main(String[] args) {
        SpringApplication.run(SchemaPlexaiOpsApplication.class, args);
    }
}
