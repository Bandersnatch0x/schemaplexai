package com.schemaplexai.integration.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = IntegrationConfig.class)
class IntegrationConfigTest {

    @Test
    void restTemplateBeanExists(ApplicationContext ctx) {
        assertThat(ctx.getBean(RestTemplate.class)).isNotNull();
    }

    @Test
    void restTemplate_defaultsTo30sConnectAndReadTimeout(ApplicationContext ctx) {
        RestTemplate restTemplate = ctx.getBean(RestTemplate.class);
        ClientHttpRequestFactory factory = restTemplate.getRequestFactory();

        // Issue 918: outbound HTTP is bounded (SPEC-INT §7: 30s), never infinite.
        assertThat(factory).isInstanceOf(SimpleClientHttpRequestFactory.class);
        assertThat(ReflectionTestUtils.getField(factory, "connectTimeout")).isEqualTo(30_000);
        assertThat(ReflectionTestUtils.getField(factory, "readTimeout")).isEqualTo(30_000);
    }

    @Test
    void restTemplate_appliesConfiguredTimeouts() {
        IntegrationConfig config = new IntegrationConfig();
        RestTemplate restTemplate = config.restTemplate(Duration.ofSeconds(5), Duration.ofSeconds(15));
        ClientHttpRequestFactory factory = restTemplate.getRequestFactory();

        assertThat(factory).isInstanceOf(SimpleClientHttpRequestFactory.class);
        assertThat(ReflectionTestUtils.getField(factory, "connectTimeout")).isEqualTo(5_000);
        assertThat(ReflectionTestUtils.getField(factory, "readTimeout")).isEqualTo(15_000);
    }
}
