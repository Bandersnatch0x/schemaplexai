package com.schemaplexai.agent.engine.state;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.mq.QualityCheckEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class CompletedStateHandler implements AgentStateHandler {

    private final QualityCheckEventPublisher qualityCheckEventPublisher;

    public CompletedStateHandler(QualityCheckEventPublisher qualityCheckEventPublisher) {
        this.qualityCheckEventPublisher = qualityCheckEventPublisher;
    }

    @Override
    public AgentExecutionState getState() {
        return AgentExecutionState.COMPLETED;
    }

    @Override
    public void handle(AgentStateMachine stateMachine, SfAgentExecution execution) {
        log.info("Agent {} entering COMPLETED state, execution {}", execution.getAgentId(), execution.getId());
        execution.setState(AgentExecutionState.COMPLETED.name());
        execution.setCompletedAt(LocalDateTime.now());
        stateMachine.saveExecution(execution);

        // Ticket 924 / REQ-01 trigger point 1: post-execution quality gate check.
        // Published via MQ (sf.quality) so the quality module evaluates its own
        // gate configuration without a cross-module bean dependency. The verdict
        // returns on sf.quality.verdict (QualityVerdictConsumer) and can set the
        // execution to GATE_BLOCKED on a BLOCK disposition. Publishing is
        // best-effort: an MQ outage must not turn a completed execution FAILED.
        try {
            String lastOutput = (String) execution.getMetadata("lastOutput");
            qualityCheckEventPublisher.publishPostExecutionCheck(
                    execution.getId(), execution.getAgentId(), execution.getTenantId(), lastOutput);
        } catch (Exception e) {
            log.error("Post-execution quality check trigger failed for execution {} — gate skipped: {}",
                    execution.getId(), e.getMessage());
        }
    }
}
