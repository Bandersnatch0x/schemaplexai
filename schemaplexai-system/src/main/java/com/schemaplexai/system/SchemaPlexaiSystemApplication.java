package com.schemaplexai.system;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.schemaplexai.system", "com.schemaplexai.dao"})
@MapperScan("com.schemaplexai.system.**.mapper")
public class SchemaPlexaiSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchemaPlexaiSystemApplication.class, args);
    }
}
