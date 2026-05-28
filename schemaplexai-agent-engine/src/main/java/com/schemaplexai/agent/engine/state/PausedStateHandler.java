package com.schemaplexai.agent.engine.state;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.lifecycle.ExecutionSnapshot;
import com.schemaplexai.agent.engine.lifecycle.ExecutionSnapshotPersistence;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Handles PAUSED state — persists execution snapshot and waits for external Resume signal.
 *
 * State transition: PAUSED → (POST /agent/execution/{id}/resume) → RESUMING
 */
@Slf4j
@Component
public class PausedStateHandler implements AgentStateHandler {

    private final ExecutionSnapshotPersistence snapshotPersistence;

    public PausedStateHandler(ExecutionSnapshotPersistence snapshotPersistence) {
        this.snapshotPersistence = snapshotPersistence;
    }

    @Override
    public AgentExecutionState getState() {
        return AgentExecutionState.PAUSED;
    }

    @Override
    public void handle(AgentStateMachine stateMachine, SfAgentExecution execution) {
        log.info("Agent {} entering PAUSED state, execution {}", execution.getAgentId(), execution.getId());

        // Create and persist execution snapshot
        ExecutionSnapshot snapshot = new ExecutionSnapshot();
        snapshot.setExecutionId(execution.getId());
        snapshot.setState(AgentExecutionState.valueOf(execution.getState()));
        snapshot.setCreatedAt(LocalDateTime.now());

        snapshotPersistence.saveSnapshot(snapshot);

        // Update execution with snapshot reference
        execution.setSnapshotId(snapshot.getExecutionId() != null ? snapshot.getExecutionId() : execution.getId());
        stateMachine.saveExecution(execution);

        log.info("Snapshot persisted for paused execution {}, waiting for external resume signal",
                execution.getId());
        // No automatic transition — external POST /agent/execution/{id}/resume triggers RESUME
    }
}
