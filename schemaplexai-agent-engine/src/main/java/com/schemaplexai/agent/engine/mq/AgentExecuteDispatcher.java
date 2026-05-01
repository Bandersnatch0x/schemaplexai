package com.schemaplexai.agent.engine.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.agent.engine.AgentExecutionEngine;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.common.constants.CommonConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgentExecuteDispatcher {

    private final AgentExecutionEngine executionEngine;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = CommonConstants.RK_AGENT_EXECUTE)
    public void onMessage(String message) {
        try {
            AgentExecuteMessage payload = objectMapper.readValue(message, AgentExecuteMessage.class);
            SfAgentExecution execution = executionEngine.startExecution(
                payload.getAgentId(), payload.getTenantId(), payload.getPrompt());
            log.info("Dispatched execution {} for agent {}", execution.getId(), payload.getAgentId());
        } catch (Exception e) {
            log.error("Failed to dispatch agent execution message", e);
        }
    }
}
