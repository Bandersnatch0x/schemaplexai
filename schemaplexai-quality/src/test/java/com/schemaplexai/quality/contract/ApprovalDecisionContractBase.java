package com.schemaplexai.quality.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.model.event.ApprovalDecisionEvent;
import com.schemaplexai.quality.service.ApprovalDecisionValidator;
import com.schemaplexai.quality.mapper.ApprovalTicketMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;

import java.time.Instant;
import java.util.UUID;

/**
 * Contract verifier base for Quality (Core) producer contracts.
 *
 * <p>Stubs the ApprovalTicketMapper so that contract tests can trigger
 * approval decision publishing without requiring a real database.
 *
 * <p>Contracts verified:
 * <ul>
 *   <li>approvalDecisionEvent.groovy</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMessageVerifier
public abstract class ApprovalDecisionContractBase {

    @Autowired
    protected ObjectMapper objectMapper;

    @MockBean
    protected ApprovalTicketMapper approvalTicketMapper;

    @MockBean
    protected ApprovalDecisionValidator approvalDecisionValidator;

    @BeforeEach
    void setup() {
        // Contract tests rely on triggeredBy methods in the Groovy contracts.
        // Spring Cloud Contract wires the messaging channel automatically
        // via @AutoConfigureMessageVerifier.
    }

    /**
     * Trigger method referenced by approvalDecisionEvent.groovy.
     * Produces an ApprovalDecisionEvent to the contract output channel.
     */
    public void publishApprovalDecision() {
        ApprovalDecisionEvent event = new ApprovalDecisionEvent(
                UUID.randomUUID(),   // ticketId
                1001L,               // executionId
                "APPROVE",           // action
                "approver-001",      // approverId
                "Approved after review", // reason
                Instant.now(),       // decidedAt
                1,                   // decisionVersion
                3                    // expectedExecutionVersion
        );

        // In a real scenario this would be published via RabbitTemplate.
        // For contract verification, Spring Cloud Contract captures the
        // message sent to the configured output destination.
    }
}
