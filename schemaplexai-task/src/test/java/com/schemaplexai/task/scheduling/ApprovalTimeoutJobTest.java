package com.schemaplexai.task.scheduling;

import com.schemaplexai.quality.service.EscalationPolicyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ApprovalTimeoutJobTest {

    @Mock
    private EscalationPolicyService escalationPolicyService;

    @InjectMocks
    private ApprovalTimeoutJob job;

    @Test
    void run_delegatesToEscalationPolicyService() {
        assertThatNoException().isThrownBy(() -> job.run());

        verify(escalationPolicyService).checkEscalations();
    }

    @Test
    void run_checkEscalationsThrowsException_propagates() {
        doThrow(new RuntimeException("escalation failed"))
                .when(escalationPolicyService)
                .checkEscalations();

        assertThatThrownBy(() -> job.run())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("escalation failed");
    }
}
