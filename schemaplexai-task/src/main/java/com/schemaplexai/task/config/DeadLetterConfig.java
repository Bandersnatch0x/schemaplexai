package com.schemaplexai.task.config;

import com.schemaplexai.common.constants.CommonConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class DeadLetterConfig {

    public static final String DLX_EXCHANGE = "sf.dlx.exchange";
    public static final String DLX_QUEUE = "sf.dlx.queue";
    public static final String DLX_ROUTING_KEY = "sf.dlx.routing.key";

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue deadLetterQueue() {
        return new Queue(DLX_QUEUE, true);
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(DLX_ROUTING_KEY);
    }

    public static Map<String, Object> getDeadLetterArgs() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", DLX_EXCHANGE);
        args.put("x-dead-letter-routing-key", DLX_ROUTING_KEY);
        args.put("x-message-ttl", 60000);
        return args;
    }
}
