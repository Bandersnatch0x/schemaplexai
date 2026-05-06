package com.schemaplexai.agent.engine.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.agent.engine.model.LlmMessage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, LlmMessage> chatMessageRedisTemplate(
            RedisConnectionFactory factory,
            ObjectMapper objectMapper) {
        RedisTemplate<String, LlmMessage> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());

        Jackson2JsonRedisSerializer<LlmMessage> valueSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, LlmMessage.class);
        template.setValueSerializer(valueSerializer);
        template.afterPropertiesSet();
        return template;
    }
}
