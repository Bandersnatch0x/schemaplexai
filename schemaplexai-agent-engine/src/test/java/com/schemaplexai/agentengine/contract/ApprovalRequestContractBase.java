package com.schemaplexai.agentengine.contract;

import com.schemaplexai.agent.engine.approval.ApprovalRequestProducer;
import com.schemaplexai.agent.engine.mapper.ExecutionEventMapper;
import com.schemaplexai.agent.engine.mapper.ExecutionOutboxMapper;
import com.schemaplexai.agent.engine.service.ExecutionEventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;

/**
 * Contract verifier base for Agent-Engine producer contracts.
 *
 * <p>Stubs the outbound dependencies (ExecutionEventMapper, ExecutionOutboxMapper)
 * so that the contract tests can trigger {@link ApprovalRequestProducer#produce}
 * without requiring a real database.
 *
 * <p>Contracts verified:
 * <ul>
 *   <li>approvalRequestEvent.groovy</li>
 *   <li>executionStateChangedEvent.groovy</li>
 *   <li>costRecordedEvent.groovy</li>
 *   <li>auditEventRecorded.groovy</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMessageVerifier
public abstract class ApprovalRequestContractBase {

    @Autowired
    protected ApprovalRequestProducer approvalRequestProducer;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockBean
    protected ExecutionEventService executionEventService;

    @MockBean
    protected ExecutionEventMapper executionEventMapper;

    @MockBean
    protected ExecutionOutboxMapper executionOutboxMapper;

    @BeforeEach
    void setup() {
        // Contract tests rely on triggeredBy methods in the Groovy contracts.
        // No additional setup required — Spring Cloud Contract wires the
        // messaging channel automatically via @AutoConfigureMessageVerifier.
    }

    /**
     * Trigger method referenced by approvalRequestEvent.groovy.
     * Publishes an ApprovalRequestEvent to the contract output channel.
     */
    public void publishApprovalRequest() {
        approvalRequestProducer.produce(
                1001L,      // executionId
                1L,         // tenantId
                42L,        // agentId
                5,          // triggeringSeq
                "FAST",     // requestType
                "HIGH",     // riskLevel
                "Execute shell command: rm -rf /tmp/old-logs", // actionDescription
                3,          // executionVersion
                false       // deferred
        );
    }
}
