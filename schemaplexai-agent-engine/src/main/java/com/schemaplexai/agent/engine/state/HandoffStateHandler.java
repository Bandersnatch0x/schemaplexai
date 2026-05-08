package com.schemaplexai.agent.engine.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.agent.engine.AgentExecutionEngine;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.entity.SfAgentExecutionSnapshot;
import com.schemaplexai.agent.engine.lifecycle.ExecutionSnapshot;
import com.schemaplexai.agent.engine.lifecycle.PauseReason;
import com.schemaplexai.agent.engine.mapper.SfAgentExecutionMapper;
import com.schemaplexai.agent.engine.mapper.SfAgentExecutionSnapshotMapper;
import com.schemaplexai.agent.engine.orchestrator.AgentRouter;
import com.schemaplexai.agent.engine.sse.ExecutionEventBus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Handles the HANDOFF state — agent self-routing when it detects a task outside its specialty.
 *
 * <p>Workflow:
 * <ol>
 *   <li>Agent detects it cannot handle the current task (wrong specialty)</li>
 *   <li>State machine transitions to HANDOFF</li>
 *   <li>This handler creates a snapshot of the current execution context</li>
 *   <li>Routes the task to the best-matching specialist agent via AgentRouter</li>
 *   <li>Dispatches a new execution for the specialist agent</li>
 *   <li>Original execution transitions to COMPLETED (handoff is a terminal action)</li>
 * </ol>
 *
 * <p>Required metadata on the execution:
 * <ul>
 *   <li>"handoffReason" — why the current agent is handing off</li>
 *   <li>"handoffPrompt" — the refined prompt for the target agent</li>
 *   <li>"handoffContext" — accumulated context to pass along (optional)</li>
 * </ul>
 */
@Slf4j
@Component
public class HandoffStateHandler implements AgentStateHandler {

    private final SfAgentExecutionMapper executionMapper;
    private final SfAgentExecutionSnapshotMapper snapshotMapper;
    private final AgentRouter agentRouter;
    private final AgentExecutionEngine executionEngine;
    private final ExecutionEventBus eventBus;
    private final ObjectMapper objectMapper;

    public HandoffStateHandler(SfAgentExecutionMapper executionMapper,
                               SfAgentExecutionSnapshotMapper snapshotMapper,
                               AgentRouter agentRouter,
                               AgentExecutionEngine executionEngine,
                               ExecutionEventBus eventBus,
                               ObjectMapper objectMapper) {
        this.executionMapper = executionMapper;
        this.snapshotMapper = snapshotMapper;
        this.agentRouter = agentRouter;
        this.executionEngine = executionEngine;
        this.eventBus = eventBus;
        this.objectMapper = objectMapper;
    }

    @Override
    public AgentExecutionState getState() {
        return AgentExecutionState.HANDOFF;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handle(AgentStateMachine stateMachine, SfAgentExecution execution) {
        log.info("Agent {} entering HANDOFF state, execution {}", execution.getAgentId(), execution.getId());

        String handoffReason = (String) execution.getMetadata("handoffReason");
        String handoffPrompt = (String) execution.getMetadata("handoffPrompt");
        String handoffContext = (String) execution.getMetadata("handoffContext");

        if (handoffReason == null || handoffReason.isBlank()) {
            handoffReason = "Task outside agent specialty";
        }

        if (handoffPrompt == null || handoffPrompt.isBlank()) {
            log.error("Handoff requested but no prompt provided for execution {}", execution.getId());
            stateMachine.transition(AgentExecutionState.FAILED, execution);
            return;
        }

        // Persist execution snapshot before handoff
        ExecutionSnapshot snapshot = new ExecutionSnapshot();
        snapshot.setExecutionId(execution.getId());
        snapshot.setState(AgentExecutionState.HANDOFF);
        snapshot.setPauseReason(PauseReason.HANDOFF_TARGET_MISMATCH);
        snapshot.setCreatedAt(LocalDateTime.now());

        String snapshotJson;
        try {
            snapshotJson = objectMapper.writeValueAsString(snapshot);
            SfAgentExecutionSnapshot persistentSnapshot = new SfAgentExecutionSnapshot();
            persistentSnapshot.setExecutionId(execution.getId());
            persistentSnapshot.setSnapshotJson(snapshotJson);
            snapshotMapper.insert(persistentSnapshot);
            log.info("Handoff snapshot persisted for execution {}", execution.getId());
        } catch (Exception e) {
            log.error("Failed to persist handoff snapshot for execution {}", execution.getId(), e);
            stateMachine.transition(AgentExecutionState.FAILED, execution);
            return;
        }

        // Route to the best-matching specialist agent
        List<AgentRouter.AgentCapability> availableAgents =
                (List<AgentRouter.AgentCapability>) execution.getMetadata("availableAgents");
        if (availableAgents == null || availableAgents.isEmpty()) {
            log.warn("No available agents for handoff routing, execution {} will remain in HANDOFF",
                    execution.getId());
            eventBus.publishOutput(execution.getId(),
                    "{\"handoff\":\"no_specialist_available\",\"reason\":\"" + handoffReason + "\"}",
                    parseTenantId(execution.getTenantId()));
            stateMachine.transition(AgentExecutionState.COMPLETED, execution);
            return;
        }

        var matchedAgent = agentRouter.route(handoffPrompt, availableAgents);
        if (matchedAgent.isEmpty()) {
            log.warn("No specialist agent matched handoff task for execution {}", execution.getId());
            eventBus.publishOutput(execution.getId(),
                    "{\"handoff\":\"no_match\",\"reason\":\"" + handoffReason + "\"}",
                    parseTenantId(execution.getTenantId()));
            stateMachine.transition(AgentExecutionState.COMPLETED, execution);
            return;
        }

        // Build the handoff prompt with accumulated context
        String effectivePrompt = handoffPrompt;
        if (handoffContext != null && !handoffContext.isBlank()) {
            effectivePrompt = handoffPrompt + "\n\nContext: " + handoffContext;
        }

        String targetAgentId = matchedAgent.get().agentId();
        log.info("Handoff routing execution {} to specialist agent '{}'", execution.getId(), targetAgentId);

        // Publish handoff event
        Long tenantId = parseTenantId(execution.getTenantId());
        eventBus.publishOutput(execution.getId(),
                "{\"handoff\":\"dispatched\",\"targetAgent\":\"" + targetAgentId
                        + "\",\"reason\":\"" + handoffReason + "\"}",
                tenantId);

        // Dispatch the specialist agent
        try {
            // Agent IDs from the router are strings; use hash-based conversion to numeric
            Long targetAgentIdLong = parseAgentId(targetAgentId);
            SfAgentExecution newExecution = executionEngine.startExecution(
                    targetAgentIdLong,
                    execution.getTenantId(),
                    effectivePrompt
            );
            // Link back to the original execution
            execution.setMetadata("handoffTargetExecutionId", newExecution.getId());
            log.info("Handoff complete: execution {} -> new execution {} via agent '{}'",
                    execution.getId(), newExecution.getId(), targetAgentId);
        } catch (Exception e) {
            log.error("Failed to dispatch handoff target for execution {}", execution.getId(), e);
            execution.setMetadata("handoffError", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }

        // Original execution completes (handoff is terminal)
        execution.setState(AgentExecutionState.COMPLETED.name());
        stateMachine.saveExecution(execution);
        eventBus.publishExecutionCompleted(execution.getId(), AgentExecutionState.COMPLETED.name());
        eventBus.complete(String.valueOf(execution.getId()));
        stateMachine.removeExecution(execution.getId());
    }

    /**
     * Safely parses a tenant ID string to Long. Handles both numeric strings
     * and prefixed formats like "tenant-1".
     */
    private Long parseTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(tenantId);
        } catch (NumberFormatException e) {
            // Try extracting numeric suffix (e.g., "tenant-1" -> 1)
            String[] parts = tenantId.split("-");
            if (parts.length > 0) {
                try {
                    return Long.valueOf(parts[parts.length - 1]);
                } catch (NumberFormatException ex) {
                    return null;
                }
            }
            return null;
        }
    }

    /**
     * Converts an agent ID string to a numeric Long for database lookup.
     * Handles both pure numeric IDs and string IDs (e.g., "db-agent" -> hash).
     */
    private Long parseAgentId(String agentId) {
        if (agentId == null || agentId.isBlank()) {
            return 0L;
        }
        try {
            return Long.valueOf(agentId);
        } catch (NumberFormatException e) {
            // For string agent IDs, use a positive hash
            return Math.abs((long) agentId.hashCode());
        }
    }
}
