package com.schemaplexai.task.scheduling;

import com.schemaplexai.ops.service.ClickHouseCostSyncService;
import com.schemaplexai.ops.service.CostService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CostStatisticsJobTest {

    @Mock
    private ClickHouseCostSyncService clickHouseCostSyncService;

    @Mock
    private CostService costService;

    @InjectMocks
    private CostStatisticsJob job;

    @Test
    void run_delegatesCostSyncAndBudgetAlerts() {
        job.run();

        verify(clickHouseCostSyncService).syncIncrementalData();
        verify(costService).checkBudgetAlerts();
    }

    @Test
    void run_syncThrowsException_propagatesWithoutBudgetAlertCheck() {
        doThrow(new RuntimeException("sync failed")).when(clickHouseCostSyncService).syncIncrementalData();

        assertThatThrownBy(() -> job.run())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("sync failed");

        verify(costService, never()).checkBudgetAlerts();
    }

    @Test
    void run_budgetAlertsThrowException_propagatesAfterCostSync() {
        doThrow(new RuntimeException("budget alert failed")).when(costService).checkBudgetAlerts();

        assertThatThrownBy(() -> job.run())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("budget alert failed");

        verify(clickHouseCostSyncService).syncIncrementalData();
    }
}
