package com.schemaplexai.agent.engine.orchestrator;

import com.schemaplexai.agent.engine.observability.ObservabilityRecorder;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@SpringBootTest
class AgentRuntimeOrchestratorIntegrationTest {

    @Autowired
    private AgentRuntimeOrchestrator orchestrator;

    @MockBean
    private ObservabilityRecorder observabilityRecorder;

    @Test
    void shouldCallObservabilityRecorderDuringExecution() {
        SfAgentExecution execution = new SfAgentExecution();
        execution.setId(1L);
        execution.setConversationId("conv-1");
        execution.setCreatedBy(100L);
        execution.setState("IDLE");

        try {
            orchestrator.run(execution, "tenant-1", "test prompt");
        } catch (Exception e) {
            // Expected possible exception due to stub implementation
        }

        verify(observabilityRecorder, atLeastOnce())
            .startTrace(any(), any(), any(), any(), any());
    }
}
