package com.schemaplexai.agent.engine.config;

import com.schemaplexai.common.constants.CommonConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the queues the engine consumes passively.
 *
 * <p>Live defect (browser verification round 2): on a fresh broker the engine
 * failed at startup with "no queue 'sf.agent.execute'" — the passive
 * {@code @RabbitListener(queues = ...)} consumers expected queues that no
 * module ever declared. The task module declares its own
 * {@code sf.agent.execute.queue}; the engine-side queue and the approval
 * queues are declared here.
 */
@Configuration
public class EngineRabbitMqConfig {

    public static final String EXCHANGE_APPROVAL = "approval";
    public static final String QUEUE_APPROVAL_DECISIONS = "approval.decisions";
    public static final String QUEUE_APPROVAL_DEFERRED_CREATED = "approval.deferred.created";

    @Bean
    public DirectExchange sfExchange() {
        return new DirectExchange(CommonConstants.EXCHANGE_SCHEMAPLEXAI, true, false);
    }

    @Bean
    public Queue agentExecuteQueue() {
        return new Queue(CommonConstants.RK_AGENT_EXECUTE, true);
    }

    @Bean
    public Binding agentExecuteBinding(Queue agentExecuteQueue, DirectExchange sfExchange) {
        return BindingBuilder.bind(agentExecuteQueue).to(sfExchange).with(CommonConstants.RK_AGENT_EXECUTE);
    }

    @Bean
    public DirectExchange approvalExchange() {
        return new DirectExchange(EXCHANGE_APPROVAL, true, false);
    }

    @Bean
    public Queue approvalDecisionsQueue() {
        return new Queue(QUEUE_APPROVAL_DECISIONS, true);
    }

    @Bean
    public Binding approvalDecisionsBinding(Queue approvalDecisionsQueue, DirectExchange approvalExchange) {
        return BindingBuilder.bind(approvalDecisionsQueue).to(approvalExchange).with(QUEUE_APPROVAL_DECISIONS);
    }

    @Bean
    public Queue approvalDeferredCreatedQueue() {
        return new Queue(QUEUE_APPROVAL_DEFERRED_CREATED, true);
    }

    @Bean
    public Binding approvalDeferredCreatedBinding(Queue approvalDeferredCreatedQueue, DirectExchange approvalExchange) {
        return BindingBuilder.bind(approvalDeferredCreatedQueue).to(approvalExchange).with(QUEUE_APPROVAL_DEFERRED_CREATED);
    }
}
