package com.schemaplexai.task;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
@MapperScan({
        "com.schemaplexai.task.mapper",
        "com.schemaplexai.dao.mapper.notification"
})
@SpringBootApplication(scanBasePackages = {"com.schemaplexai.task"})
public class SchemaPlexaiTaskApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchemaPlexaiTaskApplication.class, args);
    }
}
