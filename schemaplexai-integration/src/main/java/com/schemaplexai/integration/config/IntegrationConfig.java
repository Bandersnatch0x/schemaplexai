package com.schemaplexai.integration.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(IntegrationOAuthProperties.class)
public class IntegrationConfig {

    /** External calls are bounded by a 30s connect/read budget per SPEC-INT §7. */
    @Bean
    public RestTemplate restTemplate(
            @Value("${integration.http.connect-timeout:30s}") Duration connectTimeout,
            @Value("${integration.http.read-timeout:30s}") Duration readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return new RestTemplate(factory);
    }
}
