package com.schemaplexai.ops.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.schemaplexai.ops.entity.SfBudget;
import com.schemaplexai.ops.mapper.BudgetMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CostServiceTest {

    @Mock
    private BudgetMapper budgetMapper;

    @InjectMocks
    private CostService costService;

    private Logger costServiceLogger;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        costServiceLogger = (Logger) LoggerFactory.getLogger(CostService.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        costServiceLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        costServiceLogger.detachAppender(logAppender);
    }

    // ------------------------------------------------------------------
    // queryCostByTenant
    // ------------------------------------------------------------------

    @Test
    void queryCostByTenant_returnsMapWithAllRequiredKeys() {
        when(budgetMapper.selectList(any())).thenReturn(Collections.emptyList());

        Map<String, BigDecimal> result = costService.queryCostByTenant("tenant-1");

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.containsKey("totalCost"));
        assertTrue(result.containsKey("todayCost"));
        assertTrue(result.containsKey("monthCost"));
    }

    @Test
    void queryCostByTenant_returnsZeroCostsWhenNoBudgets() {
        when(budgetMapper.selectList(any())).thenReturn(Collections.emptyList());

        Map<String, BigDecimal> result = costService.queryCostByTenant("tenant-1");

        assertEquals(0, BigDecimal.ZERO.compareTo(result.get("totalCost")));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.get("todayCost")));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.get("monthCost")));
    }

    @Test
    void queryCostByTenant_returnsAggregatedNonZeroCosts() {
        SfBudget budget1 = createBudget("API", BigDecimal.valueOf(1000), BigDecimal.valueOf(250.50), BigDecimal.valueOf(80));
        SfBudget budget2 = createBudget("TOKEN", BigDecimal.valueOf(5000), BigDecimal.valueOf(1200.75), BigDecimal.valueOf(90));
        when(budgetMapper.selectList(any())).thenReturn(List.of(budget1, budget2));

        Map<String, BigDecimal> result = costService.queryCostByTenant("tenant-1");

        BigDecimal expectedTotal = BigDecimal.valueOf(1451.25);
        assertEquals(0, expectedTotal.compareTo(result.get("totalCost")),
                "totalCost should aggregate usedAmount from all budgets");
        assertEquals(0, expectedTotal.compareTo(result.get("todayCost")),
                "todayCost should mirror totalCost in v1 short-path");
        assertEquals(0, expectedTotal.compareTo(result.get("monthCost")),
                "monthCost should mirror totalCost in v1 short-path");
    }

    @Test
    void queryCostByTenant_skipsNullUsedAmounts() {
        SfBudget budgetWithNull = createBudget("API", BigDecimal.valueOf(100), null, BigDecimal.valueOf(80));
        SfBudget budgetWithValue = createBudget("TOKEN", BigDecimal.valueOf(1000), BigDecimal.valueOf(500), null);
        when(budgetMapper.selectList(any())).thenReturn(List.of(budgetWithNull, budgetWithValue));

        Map<String, BigDecimal> result = costService.queryCostByTenant("tenant-1");

        assertEquals(0, BigDecimal.valueOf(500).compareTo(result.get("totalCost")));
    }

    @Test
    void queryCostByTenant_returnsNewMapEachCall() {
        when(budgetMapper.selectList(any())).thenReturn(Collections.emptyList());

        Map<String, BigDecimal> result1 = costService.queryCostByTenant("tenant-1");
        Map<String, BigDecimal> result2 = costService.queryCostByTenant("tenant-1");

        assertNotSame(result1, result2);
    }

    // ------------------------------------------------------------------
    // checkBudgetAlerts - no budgets
    // ------------------------------------------------------------------

    @Test
    void checkBudgetAlerts_noBudgets_doesNotLogAnyWarning() {
        when(budgetMapper.selectList(null)).thenReturn(Collections.emptyList());

        costService.checkBudgetAlerts();

        List<ILoggingEvent> warnings = getWarnEvents();
        assertTrue(warnings.isEmpty(), "Expected no warnings when no budgets exist");
        verify(budgetMapper, times(1)).selectList(null);
    }

    // ------------------------------------------------------------------
    // checkBudgetAlerts - null fields skipped
    // ------------------------------------------------------------------

    @Test
    void checkBudgetAlerts_nullLimitAmount_skipsBudgetAndDoesNotLogWarning() {
        SfBudget budget = createBudget("API", null, BigDecimal.valueOf(50), BigDecimal.valueOf(80));
        when(budgetMapper.selectList(null)).thenReturn(List.of(budget));

        costService.checkBudgetAlerts();

        List<ILoggingEvent> warnings = getWarnEvents();
        assertTrue(warnings.isEmpty(), "Expected no warnings when limitAmount is null");
    }

    @Test
    void checkBudgetAlerts_nullUsedAmount_skipsBudgetAndDoesNotLogWarning() {
        SfBudget budget = createBudget("API", BigDecimal.valueOf(100), null, BigDecimal.valueOf(80));
        when(budgetMapper.selectList(null)).thenReturn(List.of(budget));

        costService.checkBudgetAlerts();

        List<ILoggingEvent> warnings = getWarnEvents();
        assertTrue(warnings.isEmpty(), "Expected no warnings when usedAmount is null");
    }

    // ------------------------------------------------------------------
    // checkBudgetAlerts - budget exceeded (ratio >= 100%)
    // ------------------------------------------------------------------

    @Test
    void checkBudgetAlerts_budgetExceeded_logsExceededWarning() {
        SfBudget budget = createBudget("API", BigDecimal.valueOf(100), BigDecimal.valueOf(150), BigDecimal.valueOf(80));
        when(budgetMapper.selectList(null)).thenReturn(List.of(budget));

        costService.checkBudgetAlerts();

        List<ILoggingEvent> warnings = getWarnEvents();
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).getFormattedMessage().contains("Budget exceeded"));
        assertTrue(warnings.get(0).getFormattedMessage().contains("API"));
    }

    @Test
    void checkBudgetAlerts_exactlyAtLimit_logsExceededWarning() {
        SfBudget budget = createBudget("API", BigDecimal.valueOf(100), BigDecimal.valueOf(100), null);
        when(budgetMapper.selectList(null)).thenReturn(List.of(budget));

        costService.checkBudgetAlerts();

        List<ILoggingEvent> warnings = getWarnEvents();
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).getFormattedMessage().contains("Budget exceeded"));
    }

    @Test
    void checkBudgetAlerts_slightlyOverLimit_logsExceededWarning() {
        SfBudget budget = createBudget("API", BigDecimal.valueOf(100), BigDecimal.valueOf(100.01), null);
        when(budgetMapper.selectList(null)).thenReturn(List.of(budget));

        costService.checkBudgetAlerts();

        List<ILoggingEvent> warnings = getWarnEvents();
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).getFormattedMessage().contains("Budget exceeded"));
    }

    // ------------------------------------------------------------------
    // checkBudgetAlerts - threshold reached but not exceeded
    // ------------------------------------------------------------------

    @Test
    void checkBudgetAlerts_thresholdReached_logsThresholdWarning() {
        SfBudget budget = createBudget("API", BigDecimal.valueOf(100), BigDecimal.valueOf(85), BigDecimal.valueOf(80));
        when(budgetMapper.selectList(null)).thenReturn(List.of(budget));

        costService.checkBudgetAlerts();

        List<ILoggingEvent> warnings = getWarnEvents();
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).getFormattedMessage().contains("Budget alert threshold reached"));
    }

    @Test
    void checkBudgetAlerts_exactlyAtThreshold_logsThresholdWarning() {
        SfBudget budget = createBudget("API", BigDecimal.valueOf(100), BigDecimal.valueOf(80), BigDecimal.valueOf(80));
        when(budgetMapper.selectList(null)).thenReturn(List.of(budget));

        costService.checkBudgetAlerts();

        List<ILoggingEvent> warnings = getWarnEvents();
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).getFormattedMessage().contains("Budget alert threshold reached"));
    }

    @Test
    void checkBudgetAlerts_underThreshold_doesNotLogWarning() {
        SfBudget budget = createBudget("API", BigDecimal.valueOf(100), BigDecimal.valueOf(50), BigDecimal.valueOf(80));
        when(budgetMapper.selectList(null)).thenReturn(List.of(budget));

        costService.checkBudgetAlerts();

        List<ILoggingEvent> warnings = getWarnEvents();
        assertTrue(warnings.isEmpty(), "Expected no warnings when usage is under threshold");
    }

    @Test
    void checkBudgetAlerts_slightlyUnderThreshold_doesNotLogWarning() {
        SfBudget budget = createBudget("API", BigDecimal.valueOf(100), BigDecimal.valueOf(79.99), BigDecimal.valueOf(80));
        when(budgetMapper.selectList(null)).thenReturn(List.of(budget));

        costService.checkBudgetAlerts();

        List<ILoggingEvent> warnings = getWarnEvents();
        assertTrue(warnings.isEmpty(), "Expected no warnings when usage is slightly under threshold");
    }

    // ------------------------------------------------------------------
    // checkBudgetAlerts - null alert threshold
    // ------------------------------------------------------------------

    @Test
    void checkBudgetAlerts_nullAlertThreshold_underLimit_doesNotLogWarning() {
        SfBudget budget = createBudget("API", BigDecimal.valueOf(100), BigDecimal.valueOf(90), null);
        when(budgetMapper.selectList(null)).thenReturn(List.of(budget));

        costService.checkBudgetAlerts();

        List<ILoggingEvent> warnings = getWarnEvents();
        assertTrue(warnings.isEmpty(), "Expected no warnings when under limit and no threshold set");
    }

    @Test
    void checkBudgetAlerts_nullAlertThreshold_overLimit_logsExceededWarning() {
        SfBudget budget = createBudget("API", BigDecimal.valueOf(100), BigDecimal.valueOf(110), null);
        when(budgetMapper.selectList(null)).thenReturn(List.of(budget));

        costService.checkBudgetAlerts();

        List<ILoggingEvent> warnings = getWarnEvents();
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).getFormattedMessage().contains("Budget exceeded"));
    }

    // ------------------------------------------------------------------
    // checkBudgetAlerts - multiple budgets
    // ------------------------------------------------------------------

    @Test
    void checkBudgetAlerts_multipleBudgets_processesAllAndLogsAppropriately() {
        SfBudget exceeded = createBudget("API", BigDecimal.valueOf(100), BigDecimal.valueOf(150), null);
        SfBudget thresholdReached = createBudget("TOKEN", BigDecimal.valueOf(1000), BigDecimal.valueOf(850), BigDecimal.valueOf(80));
        SfBudget underThreshold = createBudget("STORAGE", BigDecimal.valueOf(500), BigDecimal.valueOf(100), BigDecimal.valueOf(80));
        when(budgetMapper.selectList(null)).thenReturn(List.of(exceeded, thresholdReached, underThreshold));

        costService.checkBudgetAlerts();

        List<ILoggingEvent> warnings = getWarnEvents();
        assertEquals(2, warnings.size(), "Expected 2 warnings: one exceeded, one threshold");

        long exceededCount = warnings.stream()
                .filter(e -> e.getFormattedMessage().contains("Budget exceeded"))
                .count();
        long thresholdCount = warnings.stream()
                .filter(e -> e.getFormattedMessage().contains("Budget alert threshold reached"))
                .count();

        assertEquals(1, exceededCount);
        assertEquals(1, thresholdCount);
    }

    @Test
    void checkBudgetAlerts_multipleBudgets_allExceeded_logsAllExceeded() {
        SfBudget budget1 = createBudget("API", BigDecimal.valueOf(100), BigDecimal.valueOf(200), BigDecimal.valueOf(50));
        SfBudget budget2 = createBudget("TOKEN", BigDecimal.valueOf(1000), BigDecimal.valueOf(1500), BigDecimal.valueOf(90));
        when(budgetMapper.selectList(null)).thenReturn(List.of(budget1, budget2));

        costService.checkBudgetAlerts();

        List<ILoggingEvent> warnings = getWarnEvents();
        assertEquals(2, warnings.size());
        assertTrue(warnings.stream().allMatch(e -> e.getFormattedMessage().contains("Budget exceeded")));
    }

    @Test
    void checkBudgetAlerts_multipleBudgets_allUnderThreshold_logsNothing() {
        SfBudget budget1 = createBudget("API", BigDecimal.valueOf(100), BigDecimal.valueOf(10), BigDecimal.valueOf(80));
        SfBudget budget2 = createBudget("TOKEN", BigDecimal.valueOf(1000), BigDecimal.valueOf(100), BigDecimal.valueOf(80));
        when(budgetMapper.selectList(null)).thenReturn(List.of(budget1, budget2));

        costService.checkBudgetAlerts();

        List<ILoggingEvent> warnings = getWarnEvents();
        assertTrue(warnings.isEmpty());
    }

    // ------------------------------------------------------------------
    // Edge cases
    // ------------------------------------------------------------------

    @Test
    void checkBudgetAlerts_zeroLimit_withNonZeroUsed_throwsArithmeticException() {
        SfBudget budget = createBudget("API", BigDecimal.ZERO, BigDecimal.valueOf(1), null);
        when(budgetMapper.selectList(null)).thenReturn(List.of(budget));

        assertThrows(ArithmeticException.class, () -> costService.checkBudgetAlerts());
    }

    @Test
    void checkBudgetAlerts_zeroUsed_withPositiveLimit_doesNotLogWarning() {
        SfBudget budget = createBudget("API", BigDecimal.valueOf(100), BigDecimal.ZERO, BigDecimal.valueOf(1));
        when(budgetMapper.selectList(null)).thenReturn(List.of(budget));

        costService.checkBudgetAlerts();

        List<ILoggingEvent> warnings = getWarnEvents();
        assertTrue(warnings.isEmpty());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private SfBudget createBudget(String type, BigDecimal limit, BigDecimal used, BigDecimal threshold) {
        SfBudget budget = new SfBudget();
        budget.setBudgetType(type);
        budget.setLimitAmount(limit);
        budget.setUsedAmount(used);
        budget.setAlertThreshold(threshold);
        return budget;
    }

    private List<ILoggingEvent> getWarnEvents() {
        return logAppender.list.stream()
                .filter(e -> e.getLevel() == Level.WARN)
                .toList();
    }
}
