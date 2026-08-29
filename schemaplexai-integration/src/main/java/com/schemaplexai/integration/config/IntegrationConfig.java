package com.schemaplexai.integration.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(IntegrationOAuthProperties.class)
public class IntegrationConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
