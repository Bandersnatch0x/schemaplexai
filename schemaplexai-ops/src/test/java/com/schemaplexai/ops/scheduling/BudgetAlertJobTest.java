package com.schemaplexai.ops.scheduling;

import com.schemaplexai.ops.service.CostService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetAlertJobTest {

    @Mock
    private CostService costService;

    @InjectMocks
    private BudgetAlertJob budgetAlertJob;

    @Test
    void scheduledWithSpecHourlyCron() throws NoSuchMethodException {
        Method run = BudgetAlertJob.class.getMethod("run");
        Scheduled scheduled = run.getAnnotation(Scheduled.class);

        assertNotNull(scheduled, "BudgetAlertJob.run must be @Scheduled");
        assertEquals("0 0 * * * ?", scheduled.cron(),
                "Spec §3.3/§5 requires an hourly budget alert check");
        assertEquals(BudgetAlertJob.HOURLY_CRON, scheduled.cron());
    }

    @Test
    void run_delegatesToCostServiceAlertCheck() {
        budgetAlertJob.run();

        verify(costService, times(1)).checkBudgetAlerts();
    }

    @Test
    void run_swallowsExceptionsSoScheduleSurvives() {
        doThrow(new ArithmeticException("Division by zero")).when(costService).checkBudgetAlerts();

        assertDoesNotThrow(() -> budgetAlertJob.run(),
                "A bad budget row must not kill the hourly schedule");
        verify(costService).checkBudgetAlerts();
    }
}
