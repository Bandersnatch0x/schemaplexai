package com.schemaplexai.quality;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.schemaplexai.quality", "com.schemaplexai.dao"})
@MapperScan("com.schemaplexai.quality.**.mapper")
public class SchemaPlexaiQualityApplication {
    public static void main(String[] args) {
        SpringApplication.run(SchemaPlexaiQualityApplication.class, args);
    }
}
