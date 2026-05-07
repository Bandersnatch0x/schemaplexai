package com.schemaplexai.agent.engine.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    /**
     * Customizes Spring Boot's auto-configured ObjectMapper rather than replacing it.
     * This preserves all auto-registered modules and spring.jackson.* properties.
     */
    @Bean
    Jackson2ObjectMapperBuilderCustomizer agentEngineJacksonCustomizer() {
        return builder -> {
            builder.featuresToDisable(
                    com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
            );
        };
    }
}
