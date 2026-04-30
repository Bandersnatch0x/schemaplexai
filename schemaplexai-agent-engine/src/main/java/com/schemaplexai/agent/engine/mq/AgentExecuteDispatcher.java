package com.schemaplexai.agent.engine.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.agent.engine.AgentExecutionEngine;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.common.constants.CommonConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgentExecuteDispatcher {

    private final AgentExecutionEngine executionEngine;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = CommonConstants.RK_AGENT_EXECUTE)
    public void onMessage(String message) {
        try {
            Map<String, Object> payload = objectMapper.readValue(message, Map.class);
            Long agentId = Long.valueOf(payload.get("agentId").toString());
            String tenantId = (String) payload.get("tenantId");
            String prompt = (String) payload.get("prompt");
            SfAgentExecution execution = executionEngine.startExecution(agentId, tenantId, prompt);
            log.info("Dispatched execution {} for agent {}", execution.getId(), agentId);
        } catch (Exception e) {
            log.error("Failed to dispatch agent execution message", e);
        }
    }
}
