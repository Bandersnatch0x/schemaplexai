package com.schemaplexai.web;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        classes = SchemaPlexaiWebApplication.class,
        properties = {
                "jwt.secret=this-is-a-very-long-test-secret-for-web-context",
                "spring.rabbitmq.listener.simple.auto-startup=false",
                "spring.rabbitmq.listener.direct.auto-startup=false"
        }
)
class SchemaPlexaiWebApplicationTest {

    @Test
    void contextLoads() {
    }
}
