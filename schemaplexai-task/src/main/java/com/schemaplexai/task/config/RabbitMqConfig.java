package com.schemaplexai.task.config;

import com.schemaplexai.common.constants.CommonConstants;
import com.schemaplexai.task.mq.filter.TenantContextCleanupAdvice;
import com.schemaplexai.task.mq.filter.TenantMqFilter;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMqConfig {

    private static final String DEAD_LETTER_EXCHANGE_ARGUMENT = "x-dead-letter-exchange";
    private static final String DEAD_LETTER_ROUTING_KEY_ARGUMENT = "x-dead-letter-routing-key";

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setAfterReceivePostProcessors(new TenantMqFilter());
        factory.setAdviceChain(new TenantContextCleanupAdvice());
        return factory;
    }

    @Bean
    public DirectExchange schemaplexaiExchange() {
        return new DirectExchange(CommonConstants.EXCHANGE_SCHEMAPLEXAI, true, false);
    }

    @Bean
    public Queue agentExecuteQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put(DEAD_LETTER_EXCHANGE_ARGUMENT, DeadLetterConfig.DLX_EXCHANGE);
        args.put(DEAD_LETTER_ROUTING_KEY_ARGUMENT, DeadLetterConfig.DLX_ROUTING_KEY);
        return new Queue("sf.agent.execute.queue", true, false, false, args);
    }

    @Bean
    public Binding agentExecuteBinding() {
        return BindingBuilder.bind(agentExecuteQueue())
                .to(schemaplexaiExchange())
                .with(CommonConstants.RK_AGENT_EXECUTE);
    }

    @Bean
    public Queue workflowTriggerQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put(DEAD_LETTER_EXCHANGE_ARGUMENT, DeadLetterConfig.DLX_EXCHANGE);
        args.put(DEAD_LETTER_ROUTING_KEY_ARGUMENT, DeadLetterConfig.DLX_ROUTING_KEY);
        return new Queue("sf.workflow.trigger.queue", true, false, false, args);
    }

    @Bean
    public Binding workflowTriggerBinding() {
        return BindingBuilder.bind(workflowTriggerQueue())
                .to(schemaplexaiExchange())
                .with(CommonConstants.RK_WORKFLOW_TRIGGER);
    }

    @Bean
    public Queue notificationQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put(DEAD_LETTER_EXCHANGE_ARGUMENT, DeadLetterConfig.DLX_EXCHANGE);
        args.put(DEAD_LETTER_ROUTING_KEY_ARGUMENT, DeadLetterConfig.DLX_ROUTING_KEY);
        return new Queue("sf.notification.queue", true, false, false, args);
    }

    @Bean
    public Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue())
                .to(schemaplexaiExchange())
                .with(CommonConstants.RK_NOTIFICATION);
    }

    @Bean
    public Queue costQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put(DEAD_LETTER_EXCHANGE_ARGUMENT, DeadLetterConfig.DLX_EXCHANGE);
        args.put(DEAD_LETTER_ROUTING_KEY_ARGUMENT, DeadLetterConfig.DLX_ROUTING_KEY);
        return new Queue("sf.cost.queue", true, false, false, args);
    }

    @Bean
    public Binding costBinding() {
        return BindingBuilder.bind(costQueue())
                .to(schemaplexaiExchange())
                .with(CommonConstants.RK_COST);
    }

    @Bean
    public Queue qualityQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put(DEAD_LETTER_EXCHANGE_ARGUMENT, DeadLetterConfig.DLX_EXCHANGE);
        args.put(DEAD_LETTER_ROUTING_KEY_ARGUMENT, DeadLetterConfig.DLX_ROUTING_KEY);
        return new Queue("sf.quality.queue", true, false, false, args);
    }

    @Bean
    public Binding qualityBinding() {
        return BindingBuilder.bind(qualityQueue())
                .to(schemaplexaiExchange())
                .with(CommonConstants.RK_QUALITY);
    }

    @Bean
    public Queue milvusSyncQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put(DEAD_LETTER_EXCHANGE_ARGUMENT, DeadLetterConfig.DLX_EXCHANGE);
        args.put(DEAD_LETTER_ROUTING_KEY_ARGUMENT, DeadLetterConfig.DLX_ROUTING_KEY);
        return new Queue("sf.milvus.sync.queue", true, false, false, args);
    }

    @Bean
    public Binding milvusSyncBinding() {
        return BindingBuilder.bind(milvusSyncQueue())
                .to(schemaplexaiExchange())
                .with(CommonConstants.RK_MILVUS_SYNC);
    }

    @Bean
    public Queue executionEventQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put(DEAD_LETTER_EXCHANGE_ARGUMENT, DeadLetterConfig.DLX_EXCHANGE);
        args.put(DEAD_LETTER_ROUTING_KEY_ARGUMENT, DeadLetterConfig.DLX_ROUTING_KEY);
        return new Queue("sf.execution.event.queue", true, false, false, args);
    }

    @Bean
    public Binding executionEventBinding() {
        return BindingBuilder.bind(executionEventQueue())
                .to(schemaplexaiExchange())
                .with(CommonConstants.RK_AGENT_EXEC_EVENT);
    }
}
