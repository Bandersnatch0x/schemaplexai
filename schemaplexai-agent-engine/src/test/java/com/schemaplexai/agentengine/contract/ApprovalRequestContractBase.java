package com.schemaplexai.agentengine.contract;

import com.schemaplexai.agent.engine.approval.ApprovalRequestProducer;
import com.schemaplexai.agent.engine.mapper.ExecutionEventMapper;
import com.schemaplexai.agent.engine.mapper.ExecutionOutboxMapper;
import com.schemaplexai.agent.engine.mq.CostRecordedEventPublisher;
import com.schemaplexai.agent.engine.service.ExecutionEventService;
import com.schemaplexai.model.event.CostRecordedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;

import java.time.Instant;
import java.util.UUID;

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
    protected CostRecordedEventPublisher costRecordedEventPublisher;

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

    /**
     * Trigger method referenced by costRecordedEvent.groovy.
     * Publishes a CostRecordedEvent to {@code sf.exchange} / {@code sf.cost}.
     *
     * <p>Production shape note: {@code costAmount} is null because pricing is
     * owned by the consuming CostService (schemaplexai-ops); the engine reports
     * raw token usage only.
     */
    public void publishCostRecorded() {
        costRecordedEventPublisher.publishCostRecorded(new CostRecordedEvent(
                UUID.randomUUID(),    // eventId
                1001L,                // executionId
                1L,                   // tenantId
                42L,                  // agentId
                "gpt-4o",             // modelName
                "OPENAI",             // provider
                "chat",               // requestType
                1000L,                // inputTokens
                500L,                 // outputTokens
                1500L,                // totalTokens
                null,                 // costAmount — computed by the consumer
                "USD",                // currency
                Instant.now()         // occurredAt
        ));
    }
}
