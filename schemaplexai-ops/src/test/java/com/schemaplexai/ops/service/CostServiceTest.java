package com.schemaplexai.ops.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.schemaplexai.model.event.CostRecordedEvent;
import com.schemaplexai.model.event.ExecutionEventMessage;
import com.schemaplexai.ops.entity.AiModelPrice;
import com.schemaplexai.ops.entity.SfBudget;
import com.schemaplexai.ops.entity.SfCostRecord;
import com.schemaplexai.ops.mapper.AiModelPriceMapper;
import com.schemaplexai.ops.mapper.BudgetMapper;
import com.schemaplexai.ops.mapper.SfCostRecordMapper;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CostServiceTest {

    @Mock
    private BudgetMapper budgetMapper;

    @Mock
    private SfCostRecordMapper costRecordMapper;

    @Mock
    private BudgetService budgetService;

    @Mock
    private AiModelPriceMapper aiModelPriceMapper;

    @Mock
    private BudgetAlertNotifier budgetAlertNotifier;

    private CostService costService;

    private Logger costServiceLogger;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        costService = new CostService(budgetMapper, costRecordMapper, budgetService,
                new com.fasterxml.jackson.databind.ObjectMapper(), aiModelPriceMapper, budgetAlertNotifier);
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

    @Test
    void queryCostByExecution_returnsAggregatedCostAndTokens() {
        SfCostRecord tokenRecord = createCostRecord(
                "tenant-1", 42L, new BigDecimal("10.25"), 1000L, 500L, 1500L, "USD");
        SfCostRecord toolRecord = createCostRecord(
                "tenant-1", 42L, new BigDecimal("5.50"), null, null, null, "USD");
        when(costRecordMapper.selectList(any())).thenReturn(List.of(tokenRecord, toolRecord));

        Map<String, Object> result = costService.queryCostByExecution("tenant-1", 42L);

        assertEquals(42L, result.get("executionId"));
        assertEquals(0, new BigDecimal("15.75").compareTo((BigDecimal) result.get("totalCost")));
        assertEquals("USD", result.get("currency"));
        assertEquals(1000L, result.get("inputTokens"));
        assertEquals(500L, result.get("outputTokens"));
        assertEquals(1500L, result.get("totalTokens"));
        assertEquals(2, result.get("recordCount"));
    }

    @Test
    void queryCostByExecution_returnsZeroCostWhenNoRecords() {
        when(costRecordMapper.selectList(any())).thenReturn(Collections.emptyList());

        Map<String, Object> result = costService.queryCostByExecution("tenant-1", 42L);

        assertEquals(42L, result.get("executionId"));
        assertEquals(0, BigDecimal.ZERO.compareTo((BigDecimal) result.get("totalCost")));
        assertEquals("USD", result.get("currency"));
        assertEquals(0L, result.get("inputTokens"));
        assertEquals(0L, result.get("outputTokens"));
        assertEquals(0L, result.get("totalTokens"));
        assertEquals(0, result.get("recordCount"));
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
    // checkBudgetAlerts — unified decimal threshold semantics (issue 921)
    // ------------------------------------------------------------------

    @Test
    void checkBudgetAlerts_decimalThreshold_firesAtFractionNotPercent() {
        // threshold 0.8 means 80%: usage 85% fires, usage 5% must NOT misfire
        // (the old percent-vs-decimal mix alerted the latter at 0.8% usage)
        SfBudget over = createBudget("API", BigDecimal.valueOf(100), BigDecimal.valueOf(85), new BigDecimal("0.8"));
        SfBudget under = createBudget("TOKEN", BigDecimal.valueOf(100), BigDecimal.valueOf(5), new BigDecimal("0.8"));
        when(budgetMapper.selectList(null)).thenReturn(List.of(over, under));

        costService.checkBudgetAlerts();

        List<ILoggingEvent> warnings = getWarnEvents();
        assertEquals(1, warnings.size(), "Only the 85% budget may alert; 5% must not misfire");
        assertTrue(warnings.get(0).getFormattedMessage().contains("Budget alert threshold reached"));
    }

    @Test
    void checkBudgetAlerts_legacyPercentThreshold_normalizedLikeDecimal() {
        // legacy row storing 80.00 (percent) behaves exactly like 0.8 (decimal)
        SfBudget legacy = createBudget("API", BigDecimal.valueOf(100), BigDecimal.valueOf(85), new BigDecimal("80.00"));
        when(budgetMapper.selectList(null)).thenReturn(List.of(legacy));

        costService.checkBudgetAlerts();

        List<ILoggingEvent> warnings = getWarnEvents();
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).getFormattedMessage().contains("Budget alert threshold reached"));
    }

    @Test
    void checkBudgetAlerts_legacyPercentThreshold_lowUsageDoesNotMisfire() {
        SfBudget legacy = createBudget("API", BigDecimal.valueOf(100), BigDecimal.valueOf(5), new BigDecimal("80.00"));
        when(budgetMapper.selectList(null)).thenReturn(List.of(legacy));

        costService.checkBudgetAlerts();

        assertTrue(getWarnEvents().isEmpty(),
                "Legacy percent threshold must not fire at low usage after normalization");
    }

    @Test
    void checkBudgetAlerts_exceeded_dispatchesExceededAlert() {
        SfBudget budget = createBudget("API", BigDecimal.valueOf(100), BigDecimal.valueOf(150), new BigDecimal("0.8"));
        when(budgetMapper.selectList(null)).thenReturn(List.of(budget));

        costService.checkBudgetAlerts();

        verify(budgetAlertNotifier).dispatchBudgetAlert(
                eq(budget), eq(BudgetAlertNotifier.LEVEL_EXCEEDED), any(BigDecimal.class));
    }

    @Test
    void checkBudgetAlerts_thresholdReached_dispatchesThresholdAlert() {
        SfBudget budget = createBudget("API", BigDecimal.valueOf(100), BigDecimal.valueOf(85), new BigDecimal("0.8"));
        when(budgetMapper.selectList(null)).thenReturn(List.of(budget));

        costService.checkBudgetAlerts();

        verify(budgetAlertNotifier).dispatchBudgetAlert(
                eq(budget), eq(BudgetAlertNotifier.LEVEL_THRESHOLD_REACHED), any(BigDecimal.class));
    }

    @Test
    void checkBudgetAlerts_underThreshold_dispatchesNothing() {
        SfBudget budget = createBudget("API", BigDecimal.valueOf(100), BigDecimal.valueOf(50), new BigDecimal("0.8"));
        when(budgetMapper.selectList(null)).thenReturn(List.of(budget));

        costService.checkBudgetAlerts();

        verifyNoInteractions(budgetAlertNotifier);
    }

    @Test
    void normalizeAlertThreshold_decimalPassesThrough() {
        assertEquals(0, new BigDecimal("0.8").compareTo(CostService.normalizeAlertThreshold(new BigDecimal("0.8"))));
        assertEquals(0, BigDecimal.ONE.compareTo(CostService.normalizeAlertThreshold(BigDecimal.ONE)));
        assertEquals(0, BigDecimal.ZERO.compareTo(CostService.normalizeAlertThreshold(BigDecimal.ZERO)));
        assertNull(CostService.normalizeAlertThreshold(null));
    }

    @Test
    void normalizeAlertThreshold_legacyPercentDividedBy100() {
        assertEquals(0, new BigDecimal("0.8").compareTo(CostService.normalizeAlertThreshold(new BigDecimal("80.00"))));
        assertEquals(0, new BigDecimal("0.95").compareTo(CostService.normalizeAlertThreshold(new BigDecimal("95"))));
    }

    // ------------------------------------------------------------------
    // calculateCost
    // ------------------------------------------------------------------

    @Test
    void calculateCost_gpt4InputTokens_returnsCorrectCost() {
        BigDecimal result = costService.calculateCost("gpt-4", 1000L, 0L);

        // gpt-4: $0.03 per 1K input tokens = $0.03 for 1000 input tokens
        assertEquals(0, new BigDecimal("0.0300").compareTo(result));
    }

    @Test
    void calculateCost_gpt4OutputTokens_returnsCorrectCost() {
        BigDecimal result = costService.calculateCost("gpt-4", 0L, 1000L);

        // gpt-4: $0.06 per 1K output tokens = $0.06 for 1000 output tokens
        assertEquals(0, new BigDecimal("0.0600").compareTo(result));
    }

    @Test
    void calculateCost_gpt4MixedTokens_returnsCorrectCost() {
        BigDecimal result = costService.calculateCost("gpt-4", 2000L, 1000L);

        // input: 2000 * 0.03 / 1000 = 0.06, output: 1000 * 0.06 / 1000 = 0.06, total = 0.12
        assertEquals(0, new BigDecimal("0.1200").compareTo(result));
    }

    @Test
    void calculateCost_gpt35InputTokens_returnsCorrectCost() {
        BigDecimal result = costService.calculateCost("gpt-3.5-turbo", 1000L, 0L);

        // gpt-3.5: $0.0015 per 1K input tokens
        assertEquals(0, new BigDecimal("0.0015").compareTo(result));
    }

    @Test
    void calculateCost_gpt35OutputTokens_returnsCorrectCost() {
        BigDecimal result = costService.calculateCost("gpt-3.5-turbo", 0L, 1000L);

        // gpt-3.5: $0.002 per 1K output tokens
        assertEquals(0, new BigDecimal("0.0020").compareTo(result));
    }

    @Test
    void calculateCost_unknownModel_returnsZeroAndLogsExplicitWarning() {
        BigDecimal result = costService.calculateCost("unknown-model", 1000L, 500L);

        assertEquals(0, BigDecimal.ZERO.compareTo(result));
        List<ILoggingEvent> warnings = getWarnEvents();
        assertTrue(warnings.stream()
                        .anyMatch(e -> e.getFormattedMessage().contains("no fallback rate available")),
                "Unpriced model must produce an explicit warning instead of a silent zero");
    }

    @Test
    void calculateCost_nullTokens_returnsZeroCost() {
        assertEquals(0, BigDecimal.ZERO.compareTo(costService.calculateCost("gpt-4", null, 500L)));
        assertEquals(0, BigDecimal.ZERO.compareTo(costService.calculateCost("gpt-4", 500L, null)));
    }

    @Test
    void calculateCost_claudeSonnet_fallbackNonZeroCost() {
        // claude models were previously billed 0 (issue 920 / REQ-03)
        BigDecimal result = costService.calculateCost("claude-3-sonnet-20240229", 1000L, 1000L);

        // 0.003 + 0.015 = 0.018
        assertEquals(0, new BigDecimal("0.018000").compareTo(result));
    }

    @Test
    void calculateCost_claudeHaiku_fallbackNonZeroCost() {
        BigDecimal result = costService.calculateCost("claude-3-haiku-20240307", 1000L, 1000L);

        // 0.00025 + 0.00125 = 0.0015
        assertEquals(0, new BigDecimal("0.001500").compareTo(result));
    }

    @Test
    void calculateCost_claudeOpus_fallbackNonZeroCost() {
        BigDecimal result = costService.calculateCost("claude-3-opus-20240229", 1000L, 1000L);

        // 0.015 + 0.075 = 0.09
        assertEquals(0, new BigDecimal("0.090000").compareTo(result));
    }

    @Test
    void calculateCost_genericClaudeName_fallsBackToSonnetRates() {
        BigDecimal result = costService.calculateCost("claude-instant-1.2", 1000L, 0L);

        assertEquals(0, new BigDecimal("0.003000").compareTo(result));
    }

    @Test
    void calculateCost_gpt4o_matchesGpt4Family() {
        BigDecimal result = costService.calculateCost("gpt-4o", 1000L, 0L);

        assertEquals(0, new BigDecimal("0.030000").compareTo(result));
    }

    @Test
    void calculateCost_fallbackUseLogsExplicitWarning() {
        costService.calculateCost("gpt-4", 100L, 100L);

        List<ILoggingEvent> warnings = getWarnEvents();
        assertTrue(warnings.stream()
                        .anyMatch(e -> e.getFormattedMessage().contains("built-in fallback rates")),
                "Fallback pricing must warn so operators configure sf_ai_model prices");
    }

    @Test
    void calculateCost_smallAmount_notZeroedAtSixDecimals() {
        // REQ-15 acceptance: 33 tokens at gpt-3.5 input rate must survive rounding
        BigDecimal result = costService.calculateCost("gpt-3.5-turbo", 33L, 0L);

        // 33 * 0.0015 / 1000 = 0.0000495 -> 0.000050 at 6 decimals (was 0.0000 at scale 4)
        assertTrue(result.compareTo(BigDecimal.ZERO) > 0, "Small invocations must not round to zero");
        assertEquals(0, new BigDecimal("0.000050").compareTo(result));
        assertEquals(CostService.COST_SCALE, result.scale());
    }

    @Test
    void calculateCost_singleToken_nonZero() {
        BigDecimal result = costService.calculateCost("gpt-3.5-turbo", 1L, 0L);

        // 0.0000015 -> 0.000002 (HALF_UP at 6 decimals)
        assertEquals(0, new BigDecimal("0.000002").compareTo(result));
    }

    @Test
    void calculateCost_smallMixedAmount_roundsOnceAtFinalScale() {
        // Rounding happens once on the sum, so sub-scale components are not individually zeroed
        BigDecimal result = costService.calculateCost("gpt-3.5-turbo", 33L, 33L);

        // 0.0000495 + 0.000066 = 0.0001155 -> 0.000116
        assertEquals(0, new BigDecimal("0.000116").compareTo(result));
    }

    @Test
    void calculateCost_tenantWithConfiguredPrice_overridesBuiltInRates() {
        AiModelPrice configured = new AiModelPrice();
        configured.setModelCode("gpt-4");
        configured.setInputPricePer1k(new BigDecimal("0.01"));
        configured.setOutputPricePer1k(new BigDecimal("0.02"));
        configured.setCurrency("USD");
        when(aiModelPriceMapper.selectOne(any())).thenReturn(configured);

        BigDecimal result = costService.calculateCost("tenant-9", "gpt-4", 1000L, 1000L);

        // configured rates win over the built-in 0.03/0.06
        assertEquals(0, new BigDecimal("0.030000").compareTo(result));
        assertTrue(getWarnEvents().isEmpty(), "Configured pricing must not emit fallback warnings");
    }

    @Test
    void calculateCost_configuredPriceCoversUnlistedModel() {
        AiModelPrice configured = new AiModelPrice();
        configured.setModelCode("acme-llm-9b");
        configured.setInputPricePer1k(new BigDecimal("0.005"));
        configured.setOutputPricePer1k(new BigDecimal("0.01"));
        when(aiModelPriceMapper.selectOne(any())).thenReturn(configured);

        BigDecimal result = costService.calculateCost("tenant-9", "acme-llm-9b", 2000L, 500L);

        // 2000*0.005/1000 + 500*0.01/1000 = 0.01 + 0.005 = 0.015
        assertEquals(0, new BigDecimal("0.015000").compareTo(result));
    }

    @Test
    void calculateCost_incompleteConfiguredPrice_fallsBackToFamilyRates() {
        AiModelPrice incomplete = new AiModelPrice();
        incomplete.setModelCode("gpt-4");
        incomplete.setInputPricePer1k(new BigDecimal("0.01"));
        incomplete.setOutputPricePer1k(null);
        when(aiModelPriceMapper.selectOne(any())).thenReturn(incomplete);

        BigDecimal result = costService.calculateCost("tenant-9", "gpt-4", 1000L, 0L);

        assertEquals(0, new BigDecimal("0.030000").compareTo(result));
        assertTrue(getWarnEvents().stream()
                .anyMatch(e -> e.getFormattedMessage().contains("built-in fallback rates")));
    }

    @Test
    void processExecutionEvent_configuredCurrency_propagatesToCostRecord() {
        AiModelPrice configured = new AiModelPrice();
        configured.setModelCode("claude-3-sonnet-20240229");
        configured.setInputPricePer1k(new BigDecimal("0.003"));
        configured.setOutputPricePer1k(new BigDecimal("0.015"));
        configured.setCurrency("EUR");
        when(aiModelPriceMapper.selectOne(any())).thenReturn(configured);

        ExecutionEventMessage event = new ExecutionEventMessage(
                java.util.UUID.randomUUID(), 7L, 1, "TOKEN_USED",
                "{\"modelName\":\"claude-3-sonnet-20240229\",\"inputTokens\":1000,\"outputTokens\":1000}",
                java.time.Instant.now(), 1L, 1L, "NORMAL"
        );

        costService.processExecutionEvent(event);

        verify(costRecordMapper).insert(argThat(record ->
                "EUR".equals(record.getCurrency()) &&
                new BigDecimal("0.018000").compareTo(record.getCostAmount()) == 0
        ));
        verify(budgetService).addUsedAmount("1", new BigDecimal("0.018000"));
    }

    @Test
    void calculateCost_nullModel_returnsZeroCost() {
        BigDecimal result = costService.calculateCost(null, 1000L, 500L);

        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    @Test
    void calculateCost_zeroTokens_returnsZeroCost() {
        BigDecimal result = costService.calculateCost("gpt-4", 0L, 0L);

        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    @Test
    void calculateCost_fractionalTokens_returnsScaledCost() {
        BigDecimal result = costService.calculateCost("gpt-4", 500L, 250L);

        // input: 500 * 0.03 / 1000 = 0.015, output: 250 * 0.06 / 1000 = 0.015, total = 0.03
        assertEquals(0, new BigDecimal("0.0300").compareTo(result));
        assertEquals(CostService.COST_SCALE, result.scale());
    }

    // ------------------------------------------------------------------
    // processExecutionEvent
    // ------------------------------------------------------------------

    @Test
    void processExecutionEvent_tokenUsedEvent_savesCostRecord() {
        ExecutionEventMessage event = new ExecutionEventMessage(
                java.util.UUID.randomUUID(), 1L, 1, "TOKEN_USED",
                "{\"modelName\":\"gpt-4\",\"inputTokens\":1000,\"outputTokens\":500}",
                java.time.Instant.now(), 1L, 1L, "NORMAL"
        );

        costService.processExecutionEvent(event);

        verify(costRecordMapper).insert(argThat(record ->
                record.getExecutionId().equals(1L) &&
                record.getTenantId().equals("1") &&
                "TOKEN_USED".equals(record.getRequestType()) &&
                new BigDecimal("0.0600").compareTo(record.getCostAmount()) == 0 &&
                "USD".equals(record.getCurrency()) &&
                "gpt-4".equals(record.getModelName()) &&
                record.getTotalTokens().equals(1500L)
        ));
    }

    @Test
    void processExecutionEvent_toolCallEvent_savesCostRecordWithBaseFee() {
        ExecutionEventMessage event = new ExecutionEventMessage(
                java.util.UUID.randomUUID(), 1L, 1, "TOOL_CALL",
                "{\"toolName\":\"search\",\"durationMs\":100}",
                java.time.Instant.now(), 1L, 1L, "NORMAL"
        );

        costService.processExecutionEvent(event);

        verify(costRecordMapper).insert(argThat(record ->
                record.getExecutionId().equals(1L) &&
                "TOOL_CALL".equals(record.getRequestType()) &&
                new BigDecimal("0.0100").compareTo(record.getCostAmount()) == 0 &&
                "USD".equals(record.getCurrency()) &&
                record.getTotalTokens() == null
        ));
    }

    @Test
    void processExecutionEvent_unsupportedEventType_doesNotSave() {
        ExecutionEventMessage event = new ExecutionEventMessage(
                java.util.UUID.randomUUID(), 1L, 1, "UNKNOWN",
                "{}",
                java.time.Instant.now(), 1L, 1L, "NORMAL"
        );

        costService.processExecutionEvent(event);

        verify(costRecordMapper, never()).insert(any());
    }

    @Test
    void processExecutionEvent_updatesBudgetUsedAmount() {
        ExecutionEventMessage event = new ExecutionEventMessage(
                java.util.UUID.randomUUID(), 1L, 1, "TOKEN_USED",
                "{\"modelName\":\"gpt-4\",\"inputTokens\":1000,\"outputTokens\":500}",
                java.time.Instant.now(), 1L, 1L, "NORMAL"
        );

        costService.processExecutionEvent(event);

        verify(budgetService).addUsedAmount("1", new BigDecimal("0.060000"));
    }

    @Test
    void processExecutionEvent_tokenUsedInsertFailure_propagatesWithoutBudgetUpdate() {
        ExecutionEventMessage event = new ExecutionEventMessage(
                java.util.UUID.randomUUID(), 1L, 1, "TOKEN_USED",
                "{\"modelName\":\"gpt-4\",\"inputTokens\":1000,\"outputTokens\":500}",
                java.time.Instant.now(), 1L, 1L, "NORMAL"
        );
        doThrow(new RuntimeException("insert failed")).when(costRecordMapper).insert(any());

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> costService.processExecutionEvent(event));

        assertTrue(error.getMessage().contains("insert failed"));
        verify(budgetService, never()).addUsedAmount(anyString(), any());
    }

    @Test
    void processExecutionEvent_toolCallInsertFailure_propagatesWithoutBudgetUpdate() {
        ExecutionEventMessage event = new ExecutionEventMessage(
                java.util.UUID.randomUUID(), 1L, 1, "TOOL_CALL",
                "{\"toolName\":\"search\",\"durationMs\":100}",
                java.time.Instant.now(), 1L, 1L, "NORMAL"
        );
        doThrow(new RuntimeException("tool insert failed")).when(costRecordMapper).insert(any());

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> costService.processExecutionEvent(event));

        assertTrue(error.getMessage().contains("tool insert failed"));
        verify(budgetService, never()).addUsedAmount(anyString(), any());
    }

    @Test
    void processExecutionEvent_budgetUpdateFailure_propagatesAfterCostRecordInsert() {
        ExecutionEventMessage event = new ExecutionEventMessage(
                java.util.UUID.randomUUID(), 1L, 1, "TOKEN_USED",
                "{\"modelName\":\"gpt-4\",\"inputTokens\":1000,\"outputTokens\":500}",
                java.time.Instant.now(), 1L, 1L, "NORMAL"
        );
        doThrow(new RuntimeException("budget failed"))
                .when(budgetService).addUsedAmount("1", new BigDecimal("0.060000"));

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> costService.processExecutionEvent(event));

        assertTrue(error.getMessage().contains("budget failed"));
        verify(costRecordMapper).insert(any());
    }

    // ------------------------------------------------------------------
    // processCostRecordedEvent (ticket 919: engine cost collection chain)
    // ------------------------------------------------------------------

    @Test
    void processCostRecordedEvent_persistsRecordWithPricingAndBudget() {
        CostRecordedEvent event = new CostRecordedEvent(
                UUID.randomUUID(), 1001L, 10L, 42L,
                "gpt-4o", "OPENAI", "chat",
                1000L, 500L, 1500L,
                null, "USD", java.time.Instant.now());

        costService.processCostRecordedEvent(event);

        verify(costRecordMapper).insert(argThat(record ->
                record.getExecutionId().equals(1001L) &&
                "10".equals(record.getTenantId()) &&
                record.getAgentId().equals(42L) &&
                "gpt-4o".equals(record.getModelName()) &&
                "OPENAI".equals(record.getProvider()) &&
                "chat".equals(record.getRequestType()) &&
                record.getInputTokens().equals(1000L) &&
                record.getOutputTokens().equals(500L) &&
                record.getTotalTokens().equals(1500L) &&
                record.getRecordId() != null &&
                "agent-engine".equals(record.getServiceName()) &&
                record.getOccurredAt() != null &&
                "USD".equals(record.getCurrency()) &&
                new BigDecimal("0.060000").compareTo(record.getCostAmount()) == 0
        ));
        verify(budgetService).addUsedAmount(eq("10"), any(BigDecimal.class));
    }

    @Test
    void processCostRecordedEvent_configuredPriceAndCurrency_propagate() {
        AiModelPrice configured = new AiModelPrice();
        configured.setModelCode("claude-3-sonnet-20240229");
        configured.setInputPricePer1k(new BigDecimal("0.003"));
        configured.setOutputPricePer1k(new BigDecimal("0.015"));
        configured.setCurrency("EUR");
        when(aiModelPriceMapper.selectOne(any())).thenReturn(configured);

        CostRecordedEvent event = new CostRecordedEvent(
                UUID.randomUUID(), 7L, 1L, 1L,
                "claude-3-sonnet-20240229", "ANTHROPIC", "chat",
                1000L, 1000L, 2000L,
                null, null, java.time.Instant.now());

        costService.processCostRecordedEvent(event);

        // configured rates: 0.003 + 0.015 = 0.018, currency EUR from sf_ai_model
        verify(costRecordMapper).insert(argThat(record ->
                "EUR".equals(record.getCurrency()) &&
                new BigDecimal("0.018000").compareTo(record.getCostAmount()) == 0
        ));
        verify(budgetService).addUsedAmount("1", new BigDecimal("0.018000"));
    }

    @Test
    void processCostRecordedEvent_nullTokens_defaultToZero() {
        CostRecordedEvent event = new CostRecordedEvent(
                UUID.randomUUID(), 1L, 1L, null,
                "gpt-4", "OPENAI", "chat",
                null, null, null,
                null, null, java.time.Instant.now());

        costService.processCostRecordedEvent(event);

        verify(costRecordMapper).insert(argThat(record ->
                record.getInputTokens().equals(0L) &&
                record.getOutputTokens().equals(0L) &&
                record.getTotalTokens().equals(0L) &&
                BigDecimal.ZERO.compareTo(record.getCostAmount()) == 0
        ));
    }

    @Test
    void processCostRecordedEvent_nullEvent_isNoOp() {
        costService.processCostRecordedEvent(null);

        verifyNoInteractions(costRecordMapper);
        verifyNoInteractions(budgetService);
    }

    @Test
    void processCostRecordedEvent_missingTenantId_persistsWithoutBudget() {
        CostRecordedEvent event = new CostRecordedEvent(
                UUID.randomUUID(), 1L, null, null,
                "gpt-4", "OPENAI", "chat",
                100L, 50L, 150L,
                null, null, java.time.Instant.now());

        costService.processCostRecordedEvent(event);

        verify(costRecordMapper).insert(argThat(record -> record.getTenantId() == null));
        verifyNoInteractions(budgetService);
    }

    @Test
    void processCostRecordedEvent_insertFails_propagatesRuntime() {
        doThrow(new RuntimeException("insert failed")).when(costRecordMapper).insert(any());

        CostRecordedEvent event = new CostRecordedEvent(
                UUID.randomUUID(), 1L, 1L, 1L,
                "gpt-4", "OPENAI", "chat",
                100L, 50L, 150L,
                null, null, java.time.Instant.now());

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> costService.processCostRecordedEvent(event));

        assertTrue(error.getMessage().contains("insert failed"));
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

    private SfCostRecord createCostRecord(String tenantId, Long executionId, BigDecimal costAmount,
                                          Long inputTokens, Long outputTokens, Long totalTokens,
                                          String currency) {
        SfCostRecord record = new SfCostRecord();
        record.setTenantId(tenantId);
        record.setExecutionId(executionId);
        record.setCostAmount(costAmount);
        record.setInputTokens(inputTokens);
        record.setOutputTokens(outputTokens);
        record.setTotalTokens(totalTokens);
        record.setCurrency(currency);
        return record;
    }

    private List<ILoggingEvent> getWarnEvents() {
        return logAppender.list.stream()
                .filter(e -> e.getLevel() == Level.WARN)
                .toList();
    }
}
