package com.schemaplexai.agent.engine.groupchat;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.model.AiModelRouter;
import com.schemaplexai.agent.engine.model.LlmProvider;
import com.schemaplexai.agent.engine.state.AgentExecutionState;
import com.schemaplexai.agent.engine.state.AgentStateHandler;
import com.schemaplexai.agent.engine.state.AgentStateMachine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * FSM state handler for the {@link AgentExecutionState#GROUP_CHAT} state.
 *
 * <p>When an execution enters GROUP_CHAT, this handler delegates to
 * {@link GroupChatOrchestrator} to run the multi-agent conversation loop.
 * The orchestrator manages speaker selection, consensus detection, timeouts,
 * and SSE events. When the conversation completes (consensus or max rounds),
 * the orchestrator transitions the execution to COMPLETED or FAILED.
 *
 * <p>Required metadata on the execution:
 * <ul>
 *   <li>{@code groupChatAgents} — list of participating agent capabilities</li>
 *   <li>{@code groupChatPrompt} — shared task prompt (optional, defaults to generic)</li>
 *   <li>{@code groupChatMode} — speaker selection mode (optional, defaults to ROUND_ROBIN)</li>
 *   <li>{@code groupChatMaxRounds} — max rounds (optional, defaults to 3)</li>
 *   <li>{@code groupChatTimeoutSeconds} — timeout in seconds (optional, defaults to 300)</li>
 *   <li>{@code groupChatConsensusThreshold} — consensus keyword threshold (optional, defaults to 2)</li>
 * </ul>
 */
@Slf4j
@Component
public class GroupChatStateHandler implements AgentStateHandler {

    private final GroupChatOrchestrator orchestrator;
    private final AiModelRouter aiModelRouter;

    public GroupChatStateHandler(GroupChatOrchestrator orchestrator, AiModelRouter aiModelRouter) {
        this.orchestrator = orchestrator;
        this.aiModelRouter = aiModelRouter;
    }

    @Override
    public AgentExecutionState getState() {
        return AgentExecutionState.GROUP_CHAT;
    }

    @Override
    public void handle(AgentStateMachine stateMachine, SfAgentExecution execution) {
        log.info("Agent {} entering GROUP_CHAT state, execution {}",
                execution.getAgentId(), execution.getId());

        String modelId = (String) execution.getMetadata("modelId");
        if (modelId == null || modelId.isBlank()) {
            modelId = "gpt-4o";
        }

        LlmProvider llmProvider = aiModelRouter.route(modelId);
        orchestrator.runGroupChat(stateMachine, execution, llmProvider, modelId);
    }
}
