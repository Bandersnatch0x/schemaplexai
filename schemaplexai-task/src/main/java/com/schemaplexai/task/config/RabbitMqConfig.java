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
public class RabbitMqConfig {

    @Bean
    public DirectExchange schemaplexaiExchange() {
        return new DirectExchange(CommonConstants.EXCHANGE_SCHEMAPLEXAI, true, false);
    }

    @Bean
    public Queue agentExecuteQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", DeadLetterConfig.DLX_EXCHANGE);
        args.put("x-dead-letter-routing-key", DeadLetterConfig.DLX_ROUTING_KEY);
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
        args.put("x-dead-letter-exchange", DeadLetterConfig.DLX_EXCHANGE);
        args.put("x-dead-letter-routing-key", DeadLetterConfig.DLX_ROUTING_KEY);
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
        args.put("x-dead-letter-exchange", DeadLetterConfig.DLX_EXCHANGE);
        args.put("x-dead-letter-routing-key", DeadLetterConfig.DLX_ROUTING_KEY);
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
        args.put("x-dead-letter-exchange", DeadLetterConfig.DLX_EXCHANGE);
        args.put("x-dead-letter-routing-key", DeadLetterConfig.DLX_ROUTING_KEY);
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
        args.put("x-dead-letter-exchange", DeadLetterConfig.DLX_EXCHANGE);
        args.put("x-dead-letter-routing-key", DeadLetterConfig.DLX_ROUTING_KEY);
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
        args.put("x-dead-letter-exchange", DeadLetterConfig.DLX_EXCHANGE);
        args.put("x-dead-letter-routing-key", DeadLetterConfig.DLX_ROUTING_KEY);
        return new Queue("sf.milvus.sync.queue", true, false, false, args);
    }

    @Bean
    public Binding milvusSyncBinding() {
        return BindingBuilder.bind(milvusSyncQueue())
                .to(schemaplexaiExchange())
                .with(CommonConstants.RK_MILVUS_SYNC);
    }
}
