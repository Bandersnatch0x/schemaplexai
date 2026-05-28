package com.schemaplexai.task.scheduling;

import com.schemaplexai.task.service.MilvusReconciliationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MilvusReconciliationJobTest {

    @Mock
    private MilvusReconciliationService reconciliationService;

    @InjectMocks
    private MilvusReconciliationJob job;

    @Test
    void run_delegatesToReconciliationServiceWithDefaultBatchSize() {
        assertThatNoException().isThrownBy(() -> job.run());

        verify(reconciliationService).reconcilePendingDocuments(100);
    }

    @Test
    void run_reconciliationThrowsException_propagates() {
        when(reconciliationService.reconcilePendingDocuments(100))
                .thenThrow(new RuntimeException("reconciliation failed"));

        assertThatThrownBy(() -> job.run())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("reconciliation failed");
    }
}
