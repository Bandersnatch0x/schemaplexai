package com.schemaplexai.agent.engine.state;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.entity.SfAgentExecutionSnapshot;
import com.schemaplexai.agent.engine.mapper.SfAgentExecutionSnapshotMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Handles the RESUMING state - loads persisted snapshot and restores execution context.
 *
 * State transition path: PAUSED вЖТ (Resume API) вЖТ RESUMING вЖТ THINKING
 * This is a standalone handler (addressing Review Action Item #1).
 */
@Slf4j
@Component
public class ResumingStateHandler implements AgentStateHandler {

    private final SfAgentExecutionSnapshotMapper snapshotMapper;

    public ResumingStateHandler(SfAgentExecutionSnapshotMapper snapshotMapper) {
        this.snapshotMapper = snapshotMapper;
    }

    @Override
    public AgentExecutionState getState() {
        return AgentExecutionState.RESUMING;
    }

    @Override
    public void handle(AgentStateMachine stateMachine, SfAgentExecution execution) {
        log.info("Agent {} entering RESUMING state, execution {}", execution.getAgentId(), execution.getId());

        Long snapshotId = execution.getSnapshotId();
        if (snapshotId == null) {
            log.error("No snapshot ID found for paused execution {}", execution.getId());
            stateMachine.transition(AgentExecutionState.FAILED, execution);
            return;
        }

        // Load persisted snapshot
        SfAgentExecutionSnapshot snapshot = snapshotMapper.selectById(snapshotId);
        if (snapshot == null) {
            log.error("Snapshot {} not found for execution {}", snapshotId, execution.getId());
            stateMachine.transition(AgentExecutionState.FAILED, execution);
            return;
        }

        // Validate snapshot belongs to this execution (prevent cross-tenant snapshot injection)
        if (snapshot.getExecutionId() != null && !snapshot.getExecutionId().equals(execution.getId())) {
            log.error("Snapshot {} belongs to execution {}, not {}",
                    snapshotId, snapshot.getExecutionId(), execution.getId());
            stateMachine.transition(AgentExecutionState.FAILED, execution);
            return;
        }

        // Validate snapshot integrity
        if (snapshot.getSnapshotJson() == null || snapshot.getSnapshotJson().isBlank()) {
            log.error("Snapshot {} is corrupt (missing snapshotJson) for execution {}", snapshotId, execution.getId());
            stateMachine.transition(AgentExecutionState.FAILED, execution);
            return;
        }

        // Restore execution context from snapshot
        String snapshotJson = snapshot.getSnapshotJson();
        if (snapshotJson != null && !snapshotJson.isBlank()) {
            execution.setMetadata("restoredContext", snapshotJson);
        }

        // Restore state-specific data
        execution.setState(AgentExecutionState.RESUMING.name());
        stateMachine.saveExecution(execution);

        log.info("Snapshot {} restored for execution {}, transitioning to THINKING",
                snapshotId, execution.getId());

        // Agent continues from the pause point
        stateMachine.transition(AgentExecutionState.THINKING, execution);
    }
}
