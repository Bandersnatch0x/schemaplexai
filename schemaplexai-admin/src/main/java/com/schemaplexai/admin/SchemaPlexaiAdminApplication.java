package com.schemaplexai.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.schemaplexai.admin", "com.schemaplexai.dao"})
@MapperScan("com.schemaplexai.admin.**.mapper")
public class SchemaPlexaiAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchemaPlexaiAdminApplication.class, args);
    }
}
