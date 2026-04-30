package com.schemaplexai.task;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
@SpringBootApplication(scanBasePackages = {"com.schemaplexai"})
public class SchemaPlexaiTaskApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchemaPlexaiTaskApplication.class, args);
    }
}
