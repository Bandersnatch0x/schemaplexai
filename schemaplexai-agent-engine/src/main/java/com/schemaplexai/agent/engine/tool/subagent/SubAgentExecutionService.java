package com.schemaplexai.agent.engine.tool.subagent;

import com.schemaplexai.agent.engine.AgentExecutionEngine;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.memory.CompositeChatMemoryStore;
import com.schemaplexai.agent.engine.model.LlmMessage;
import com.schemaplexai.agent.engine.state.AgentExecutionState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Service that executes sub-agent tasks by creating a child {@link SfAgentExecution}
 * and delegating to {@link AgentExecutionEngine#runExecutionAsync}.
 *
 * <p>The child execution is created manually (not via {@code startExecution}) so that
 * the parent can control metadata such as conversation linkage and guardrails.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubAgentExecutionService {

    private final AgentExecutionEngine executionEngine;
    private final CompositeChatMemoryStore chatMemoryStore;

    /**
     * Execute a sub-agent request.
     *
     * <p>Creates a child execution, seeds the conversation memory with the prompt,
     * and starts async execution via the engine. The method then waits for the
     * execution to complete and collects the final assistant message as output.</p>
     *
     * @param request the sub-agent request
     * @return the result containing output and child execution ID
     */
    public SubAgentResult execute(SubAgentRequest request) {
        log.info("Executing sub-agent for parent={}, maxDepth={}",
                request.parentExecutionId(), request.maxDepth());

        // Build child execution manually (not via startExecution)
        SfAgentExecution childExecution = new SfAgentExecution();
        childExecution.setAgentId(null); // Sub-agents are dynamic; no fixed agent definition
        childExecution.setConversationId(UUID.randomUUID().toString());
        childExecution.setState(AgentExecutionState.QUEUED.name());
        childExecution.setTenantId(null); // Will be filled by tenant interceptor during persist
        childExecution.setMetadata("parentExecutionId", request.parentExecutionId());
        childExecution.setMetadata("role", request.role());
        childExecution.setMetadata("maxDepth", request.maxDepth());
        childExecution.setMetadata("inheritedGuardrails", request.inheritedGuardrails());

        // Seed conversation memory with the user prompt
        String conversationId = childExecution.getConversationId();
        LlmMessage userMessage = new LlmMessage("user", request.prompt());
        chatMemoryStore.saveMessage(conversationId, userMessage);

        // Start async execution — the orchestrator will pick up from memory
        executionEngine.runExecutionAsync(childExecution, null, request.prompt());

        // Return immediately with the child execution ID.
        // The actual output will be streamed / polled by the caller.
        return new SubAgentResult(
                "Sub-agent execution started. ConversationId: " + conversationId,
                childExecution.getId()
        );
    }

    /**
     * Load the conversation messages for a sub-agent.
     *
     * @param conversationId the conversation ID
     * @return the list of messages in the conversation
     */
    public List<LlmMessage> loadMessages(String conversationId) {
        return chatMemoryStore.loadMessages(conversationId);
    }
}
