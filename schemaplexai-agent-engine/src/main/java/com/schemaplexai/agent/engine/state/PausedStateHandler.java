package com.schemaplexai.agent.engine.state;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.lifecycle.ExecutionSnapshot;
import com.schemaplexai.agent.engine.lifecycle.ExecutionSnapshotPersistence;
import com.schemaplexai.agent.engine.memory.CompositeChatMemoryStore;
import com.schemaplexai.agent.engine.model.LlmMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles PAUSED state — persists execution snapshot with real data and waits for external Resume signal.
 *
 * State transition: PAUSED → (POST /agent/execution/{id}/resume) → RESUMING
 */
@Slf4j
@Component
public class PausedStateHandler implements AgentStateHandler {

    private final ExecutionSnapshotPersistence snapshotPersistence;
    private final CompositeChatMemoryStore chatMemoryStore;
    private final ObjectMapper objectMapper;

    public PausedStateHandler(ExecutionSnapshotPersistence snapshotPersistence,
                              CompositeChatMemoryStore chatMemoryStore,
                              ObjectMapper objectMapper) {
        this.snapshotPersistence = snapshotPersistence;
        this.chatMemoryStore = chatMemoryStore;
        this.objectMapper = objectMapper;
    }

    @Override
    public AgentExecutionState getState() {
        return AgentExecutionState.PAUSED;
    }

    @Override
    public void handle(AgentStateMachine stateMachine, SfAgentExecution execution) {
        log.info("Agent {} entering PAUSED state, execution {}", execution.getAgentId(), execution.getId());

        // Load real chat history from memory store
        List<LlmMessage> messages = chatMemoryStore.loadMessages(execution.getConversationId());
        List<Map<String, Object>> chatHistory = null;
        if (messages != null && !messages.isEmpty()) {
            chatHistory = messages.stream()
                    .map(msg -> {
                        Map<String, Object> entry = new HashMap<>();
                        entry.put("role", msg.getRole());
                        entry.put("content", msg.getContent());
                        return entry;
                    })
                    .toList();
        }

        // Capture context variables from execution metadata
        Map<String, Object> contextVariables = new HashMap<>();
        Object metadata = execution.getMetadata();
        if (metadata instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> metaMap = (Map<String, Object>) metadata;
            contextVariables.putAll(metaMap);
        }

        // Parse current round and token consumption from execution
        Integer currentRound = null;
        if (execution.getLastEventSeq() != null) {
            currentRound = execution.getLastEventSeq();
        }

        // Create and persist execution snapshot with real data
        ExecutionSnapshot snapshot = new ExecutionSnapshot();
        snapshot.setExecutionId(execution.getId());
        snapshot.setState(AgentExecutionState.valueOf(execution.getState()));
        snapshot.setChatHistory(chatHistory);
        snapshot.setContextVariables(contextVariables);
        snapshot.setConsumedInputTokens(null);  // v1: not persisted yet, will be populated via TokenBudget tracking in v1.1
        snapshot.setConsumedOutputTokens(null); // v1: not persisted yet, will be populated via TokenBudget tracking in v1.1
        snapshot.setCurrentRound(currentRound);
        snapshot.setCreatedAt(LocalDateTime.now());

        // Persist and capture the REAL snapshot-row primary key (issue 907 / REQ-07).
        // The old code stored the executionId here, but the snapshot row lives under
        // its own generated key — resume's selectById(snapshotId) could never find it.
        Long snapshotRowId = snapshotPersistence.saveSnapshot(snapshot);
        if (snapshotRowId == null) {
            log.error("Snapshot persistence returned no row id for execution {}; resume would be impossible",
                    execution.getId());
            execution.setMetadata("failureReason", "snapshot_primary_key_unavailable");
            stateMachine.transition(AgentExecutionState.FAILED, execution);
            return;
        }

        // Update execution with the actual snapshot row reference
        execution.setSnapshotId(snapshotRowId);
        stateMachine.saveExecution(execution);

        log.info("Snapshot {} persisted for paused execution {} (chatHistory={} msgs, currentRound={})",
                snapshotRowId,
                execution.getId(),
                chatHistory != null ? chatHistory.size() : 0,
                currentRound);
        // No automatic transition — external POST /agent/execution/{id}/resume triggers RESUME
    }
}
