package com.schemaplexai.integration.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = IntegrationConfig.class)
class IntegrationConfigTest {

    @Test
    void restTemplateBeanExists(ApplicationContext ctx) {
        assertThat(ctx.getBean(RestTemplate.class)).isNotNull();
    }
}
