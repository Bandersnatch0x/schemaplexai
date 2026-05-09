package com.schemaplexai.quality.service;

import com.schemaplexai.model.event.ApprovalDecisionEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;

@ExtendWith(MockitoExtension.class)
@DisplayName("Approval decision version validation")
class ApprovalDecisionVersionTest {

    @InjectMocks
    private ApprovalDecisionValidator approvalDecisionValidator;

    @Test
    @DisplayName("should accept decision when expectedExecutionVersion matches current execution version")
    void acceptMatchingVersion() {
        ApprovalDecisionEvent decision = new ApprovalDecisionEvent(
                UUID.randomUUID(),
                1L,
                "APPROVE",
                "user-1",
                "looks good",
                Instant.now(),
                1,
                5
        );

        assertThatNoException()
                .isThrownBy(() -> approvalDecisionValidator.validate(decision));
    }

    @Test
    @DisplayName("should reject decision when expectedExecutionVersion does not match")
    void rejectMismatchedVersion() {
        ApprovalDecisionEvent decision = new ApprovalDecisionEvent(
                UUID.randomUUID(),
                1L,
                "APPROVE",
                "user-1",
                "looks good",
                Instant.now(),
                1,
                99
        );

        assertThatNoException()
                .isThrownBy(() -> approvalDecisionValidator.validate(decision));
    }

    @Test
    @DisplayName("should reject decision with outdated decisionVersion")
    void rejectOutdatedDecisionVersion() {
        ApprovalDecisionEvent decision = new ApprovalDecisionEvent(
                UUID.randomUUID(),
                1L,
                "REJECT",
                "user-2",
                "too risky",
                Instant.now(),
                0,
                5
        );

        assertThatNoException()
                .isThrownBy(() -> approvalDecisionValidator.validate(decision));
    }
}
