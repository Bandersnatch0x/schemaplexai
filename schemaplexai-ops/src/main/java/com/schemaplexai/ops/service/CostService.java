package com.schemaplexai.ops.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.model.event.CostRecordedEvent;
import com.schemaplexai.model.event.ExecutionEventMessage;
import com.schemaplexai.ops.entity.AiModelPrice;
import com.schemaplexai.ops.entity.SfBudget;
import com.schemaplexai.ops.entity.SfCostRecord;
import com.schemaplexai.ops.mapper.AiModelPriceMapper;
import com.schemaplexai.ops.mapper.BudgetMapper;
import com.schemaplexai.ops.mapper.SfCostRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class CostService {

    private static final BigDecimal TOOL_CALL_BASE_FEE = new BigDecimal("0.01");
    private static final BigDecimal TOKEN_SCALE = new BigDecimal("1000");
    /**
     * Cost precision required by the cost-analytics spec §6: 6 decimal places.
     * The final cost is rounded once (after summing both components) so small
     * invocations are not zeroed out and per-component rounding bias does not
     * accumulate across calls.
     */
    static final int COST_SCALE = 6;
    /** Intermediate precision for each cost component before the final rounding. */
    private static final int INTERMEDIATE_SCALE = 10;
    /** Spec §3.1: currency defaults to USD. */
    static final String DEFAULT_CURRENCY = "USD";

    // ------------------------------------------------------------------
    // Built-in fallback rates (per 1K tokens), used ONLY when the model has
    // no configured price in sf_ai_model. Configured prices always win.
    // ------------------------------------------------------------------
    static final BigDecimal GPT4_INPUT_RATE = new BigDecimal("0.03");
    static final BigDecimal GPT4_OUTPUT_RATE = new BigDecimal("0.06");
    static final BigDecimal GPT35_INPUT_RATE = new BigDecimal("0.0015");
    static final BigDecimal GPT35_OUTPUT_RATE = new BigDecimal("0.002");
    static final BigDecimal CLAUDE3_OPUS_INPUT_RATE = new BigDecimal("0.015");
    static final BigDecimal CLAUDE3_OPUS_OUTPUT_RATE = new BigDecimal("0.075");
    static final BigDecimal CLAUDE3_SONNET_INPUT_RATE = new BigDecimal("0.003");
    static final BigDecimal CLAUDE3_SONNET_OUTPUT_RATE = new BigDecimal("0.015");
    static final BigDecimal CLAUDE3_HAIKU_INPUT_RATE = new BigDecimal("0.00025");
    static final BigDecimal CLAUDE3_HAIKU_OUTPUT_RATE = new BigDecimal("0.00125");

    private final BudgetMapper budgetMapper;
    private final SfCostRecordMapper costRecordMapper;
    private final BudgetService budgetService;
    private final ObjectMapper objectMapper;
    private final AiModelPriceMapper aiModelPriceMapper;
    private final BudgetAlertNotifier budgetAlertNotifier;

    /**
     * Resolved per-1K pricing for a model.
     *
     * @param inputRate  input price per 1K tokens
     * @param outputRate output price per 1K tokens
     * @param currency   currency of the rates
     * @param configured true when the price came from sf_ai_model, false for built-in fallback
     */
    record ModelPricing(BigDecimal inputRate, BigDecimal outputRate, String currency, boolean configured) {
    }

    public Map<String, BigDecimal> queryCostByTenant(String tenantId) {
        log.info("Query cost for tenant: {}", tenantId);

        // PG short-path v1: aggregate from sf_budget.used_amount as real-time cost proxy
        // TODO(v2): replace with ClickHouse sf_cost_record for per-transaction accuracy
        LambdaQueryWrapper<SfBudget> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SfBudget::getTenantId, tenantId);
        List<SfBudget> budgets = budgetMapper.selectList(wrapper);

        BigDecimal totalCost = budgets.stream()
                .map(SfBudget::getUsedAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> result = new HashMap<>();
        result.put("totalCost", totalCost);
        // v1 short-path: todayCost and monthCost mirror totalCost until
        // time-series cost records are available in PostgreSQL
        result.put("todayCost", totalCost);
        result.put("monthCost", totalCost);
        return result;
    }

    public Map<String, Object> queryCostByExecution(String tenantId, Long executionId) {
        log.info("Query cost for tenant={}, execution={}", tenantId, executionId);

        LambdaQueryWrapper<SfCostRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SfCostRecord::getTenantId, tenantId)
                .eq(SfCostRecord::getExecutionId, executionId);
        List<SfCostRecord> records = costRecordMapper.selectList(wrapper);

        BigDecimal totalCost = records.stream()
                .map(SfCostRecord::getCostAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long inputTokens = records.stream()
                .map(SfCostRecord::getInputTokens)
                .filter(Objects::nonNull)
                .reduce(0L, Long::sum);
        long outputTokens = records.stream()
                .map(SfCostRecord::getOutputTokens)
                .filter(Objects::nonNull)
                .reduce(0L, Long::sum);
        long totalTokens = records.stream()
                .map(SfCostRecord::getTotalTokens)
                .filter(Objects::nonNull)
                .reduce(0L, Long::sum);
        String currency = records.stream()
                .map(SfCostRecord::getCurrency)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse("USD");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("executionId", executionId);
        result.put("tenantId", tenantId);
        result.put("totalCost", totalCost);
        result.put("currency", currency);
        result.put("inputTokens", inputTokens);
        result.put("outputTokens", outputTokens);
        result.put("totalTokens", totalTokens);
        result.put("recordCount", records.size());
        return result;
    }

    public void checkBudgetAlerts() {
        List<SfBudget> budgets = budgetMapper.selectList(null);
        for (SfBudget budget : budgets) {
            if (budget.getLimitAmount() == null || budget.getUsedAmount() == null) {
                continue;
            }

            // Spec §3.3: threshold is a decimal fraction (0.8 = 80%). Usage is compared
            // in the same unit so the legacy percent rows (e.g. 80.00) can no longer be
            // mixed with decimal rows (0.8) — legacy values are normalized on the fly.
            BigDecimal ratio = budget.getUsedAmount()
                    .divide(budget.getLimitAmount(), COST_SCALE, RoundingMode.HALF_UP);
            BigDecimal usagePercent = ratio.multiply(BigDecimal.valueOf(100));
            BigDecimal threshold = normalizeAlertThreshold(budget.getAlertThreshold());

            if (ratio.compareTo(BigDecimal.ONE) >= 0) {
                log.warn("Budget exceeded: type={}, used={}/{} ({}%)",
                        budget.getBudgetType(), budget.getUsedAmount(),
                        budget.getLimitAmount(), usagePercent);
                budgetAlertNotifier.dispatchBudgetAlert(budget, BudgetAlertNotifier.LEVEL_EXCEEDED, usagePercent);
            } else if (threshold != null && ratio.compareTo(threshold) >= 0) {
                log.warn("Budget alert threshold reached: type={}, used={}/{} ({}%)",
                        budget.getBudgetType(), budget.getUsedAmount(),
                        budget.getLimitAmount(), usagePercent);
                budgetAlertNotifier.dispatchBudgetAlert(budget, BudgetAlertNotifier.LEVEL_THRESHOLD_REACHED, usagePercent);
            }
        }
    }

    /**
     * Normalize a budget alert threshold to the spec's decimal-fraction semantics
     * (0.8 = 80%). Values above 1 are legacy percentages and are divided by 100;
     * e.g. the old DDL default 80.00 becomes 0.80, preventing alerts from firing
     * at 0.8% usage.
     *
     * @param rawThreshold the stored threshold (nullable)
     * @return the decimal threshold in [0,1], or null when unset
     */
    static BigDecimal normalizeAlertThreshold(BigDecimal rawThreshold) {
        if (rawThreshold == null) {
            return null;
        }
        if (rawThreshold.compareTo(BigDecimal.ONE) > 0) {
            BigDecimal normalized = rawThreshold.divide(BigDecimal.valueOf(100), COST_SCALE, RoundingMode.HALF_UP);
            // debug: can fire every hourly run for legacy rows; the persisted row
            // should be migrated to decimal semantics (sf_budget.alert_threshold)
            log.debug("Legacy percent alert threshold {} normalized to decimal {}", rawThreshold, normalized);
            return normalized;
        }
        return rawThreshold;
    }

    /**
     * Calculate cost for a given model and token usage without tenant context.
     * Only the built-in fallback rates can apply (no sf_ai_model lookup).
     *
     * @param modelName    the LLM model name
     * @param inputTokens  number of input tokens
     * @param outputTokens number of output tokens
     * @return the calculated cost (6 decimal places, HALF_UP)
     */
    public BigDecimal calculateCost(String modelName, Long inputTokens, Long outputTokens) {
        return calculateCost(null, modelName, inputTokens, outputTokens);
    }

    /**
     * Calculate cost for a given tenant, model and token usage.
     * <p>
     * Price source precedence (cost-analytics spec §3.1):
     * <ol>
     *   <li>Configured price from {@code sf_ai_model}
     *       ({@code input_price_per_1k} / {@code output_price_per_1k}) for the tenant;</li>
     *   <li>Built-in fallback rates for known model families (gpt-4, gpt-3.5, claude)
     *       with an explicit warning prompting configuration;</li>
     *   <li>Zero cost with an explicit warning for models that match no fallback.</li>
     * </ol>
     * Formula: {@code cost = inputTokens * inputPricePer1K / 1000 + outputTokens * outputPricePer1K / 1000},
     * rounded once to 6 decimal places (spec §6).
     *
     * @param tenantId     the tenant ID (nullable; skips the configured-price lookup when absent)
     * @param modelName    the LLM model name
     * @param inputTokens  number of input tokens
     * @param outputTokens number of output tokens
     * @return the calculated cost (6 decimal places, HALF_UP)
     */
    public BigDecimal calculateCost(String tenantId, String modelName, Long inputTokens, Long outputTokens) {
        if (modelName == null || inputTokens == null || outputTokens == null) {
            return BigDecimal.ZERO;
        }
        ModelPricing pricing = resolvePricing(tenantId, modelName);
        return computeCost(pricing, inputTokens, outputTokens);
    }

    /**
     * Resolve the per-1K pricing for a model: configured price first, then fallback rates.
     * Never resolves silently: every non-configured resolution logs an explicit warning.
     */
    ModelPricing resolvePricing(String tenantId, String modelName) {
        if (tenantId != null && !tenantId.isBlank()) {
            AiModelPrice configured = findConfiguredPrice(tenantId, modelName);
            if (configured != null
                    && configured.getInputPricePer1k() != null
                    && configured.getOutputPricePer1k() != null) {
                String currency = configured.getCurrency() != null && !configured.getCurrency().isBlank()
                        ? configured.getCurrency()
                        : DEFAULT_CURRENCY;
                return new ModelPricing(configured.getInputPricePer1k(), configured.getOutputPricePer1k(),
                        currency, true);
            }
        }

        BigDecimal[] fallback = fallbackRatesFor(modelName);
        if (fallback != null) {
            log.warn("No price configured for model '{}' (tenant={}); billing with built-in fallback rates. "
                    + "Configure sf_ai_model.input_price_per_1k/output_price_per_1k to override.",
                    modelName, tenantId);
            return new ModelPricing(fallback[0], fallback[1], DEFAULT_CURRENCY, false);
        }

        log.warn("No price configured and no fallback rate available for model '{}' (tenant={}); "
                + "cost will be recorded as 0. Configure sf_ai_model pricing for this model.",
                modelName, tenantId);
        return null;
    }

    private AiModelPrice findConfiguredPrice(String tenantId, String modelName) {
        LambdaQueryWrapper<AiModelPrice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiModelPrice::getTenantId, tenantId)
                .and(condition -> condition
                        .eq(AiModelPrice::getModelCode, modelName)
                        .or()
                        .eq(AiModelPrice::getName, modelName))
                .orderByAsc(AiModelPrice::getId)
                .last("LIMIT 1");
        return aiModelPriceMapper.selectOne(wrapper);
    }

    /**
     * Built-in fallback rates per model family so models outside gpt-4/gpt-3.5
     * (notably the claude family) are never silently billed as zero.
     *
     * @return {@code [inputRate, outputRate]} or null when no family matches
     */
    static BigDecimal[] fallbackRatesFor(String modelName) {
        String normalized = modelName.toLowerCase(Locale.ROOT);
        if (normalized.contains("gpt-4")) {
            return new BigDecimal[]{GPT4_INPUT_RATE, GPT4_OUTPUT_RATE};
        }
        if (normalized.contains("gpt-3.5")) {
            return new BigDecimal[]{GPT35_INPUT_RATE, GPT35_OUTPUT_RATE};
        }
        if (normalized.contains("opus")) {
            return new BigDecimal[]{CLAUDE3_OPUS_INPUT_RATE, CLAUDE3_OPUS_OUTPUT_RATE};
        }
        if (normalized.contains("haiku")) {
            return new BigDecimal[]{CLAUDE3_HAIKU_INPUT_RATE, CLAUDE3_HAIKU_OUTPUT_RATE};
        }
        if (normalized.contains("claude")) {
            // generic claude fallback (covers e.g. claude-3-sonnet-20240229)
            return new BigDecimal[]{CLAUDE3_SONNET_INPUT_RATE, CLAUDE3_SONNET_OUTPUT_RATE};
        }
        return null;
    }

    private BigDecimal computeCost(ModelPricing pricing, long inputTokens, long outputTokens) {
        if (pricing == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal inputCost = pricing.inputRate()
                .multiply(BigDecimal.valueOf(inputTokens))
                .divide(TOKEN_SCALE, INTERMEDIATE_SCALE, RoundingMode.HALF_UP);
        BigDecimal outputCost = pricing.outputRate()
                .multiply(BigDecimal.valueOf(outputTokens))
                .divide(TOKEN_SCALE, INTERMEDIATE_SCALE, RoundingMode.HALF_UP);
        return inputCost.add(outputCost).setScale(COST_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Process an execution event and persist cost record.
     *
     * @param event the execution event message
     */
    public void processExecutionEvent(ExecutionEventMessage event) {
        if (event == null || event.eventType() == null) {
            return;
        }

        switch (event.eventType()) {
            case "TOKEN_USED" -> processTokenUsedEvent(event);
            case "TOOL_CALL" -> processToolCallEvent(event);
            default -> log.debug("Unsupported event type for cost projection: {}", event.eventType());
        }
    }

    /**
     * Process a cost-recorded event published by the Agent engine on
     * {@code sf.exchange} / {@code sf.cost} after an LLM call, and persist the
     * cost record to PG {@code sf_cost_record}.
     *
     * <p>The engine reports raw token usage; pricing and cost calculation
     * happen here (spec §3.1) so the configured model prices remain the single
     * source of truth. Mirrors {@link #processTokenUsedEvent} semantics plus
     * the record metadata carried by {@link CostRecordedEvent}.
     *
     * @param event the cost-recorded event (token usage + execution identity)
     */
    public void processCostRecordedEvent(CostRecordedEvent event) {
        if (event == null) {
            return;
        }
        try {
            String tenantId = event.tenantId() != null ? String.valueOf(event.tenantId()) : null;
            long inputTokens = event.inputTokens() != null ? event.inputTokens() : 0L;
            long outputTokens = event.outputTokens() != null ? event.outputTokens() : 0L;
            long totalTokens = event.totalTokens() != null ? event.totalTokens() : inputTokens + outputTokens;

            ModelPricing pricing = event.modelName() != null
                    ? resolvePricing(tenantId, event.modelName()) : null;
            BigDecimal cost = computeCost(pricing, inputTokens, outputTokens);
            if (cost.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("Zero cost calculated for cost-recorded event: eventId={}, model={}",
                        event.eventId(), event.modelName());
            }

            SfCostRecord record = new SfCostRecord();
            record.setExecutionId(event.executionId());
            record.setTenantId(tenantId);
            record.setAgentId(event.agentId());
            record.setRecordId(event.eventId() != null ? event.eventId().toString() : null);
            record.setServiceName("agent-engine");
            record.setModelName(event.modelName());
            record.setProvider(event.provider());
            record.setRequestType(event.requestType() != null ? event.requestType() : "TOKEN_USED");
            record.setInputTokens(inputTokens);
            record.setOutputTokens(outputTokens);
            record.setTotalTokens(totalTokens);
            record.setCostAmount(cost);
            record.setCurrency(pricing != null ? pricing.currency()
                    : (event.currency() != null ? event.currency() : DEFAULT_CURRENCY));
            record.setOccurredAt(event.occurredAt() != null
                    ? LocalDateTime.ofInstant(event.occurredAt(), ZoneId.systemDefault())
                    : LocalDateTime.now());

            costRecordMapper.insert(record);

            if (tenantId != null) {
                budgetService.addUsedAmount(tenantId, cost);
            }

            log.info("Cost record saved from CostRecordedEvent: executionId={}, model={}, cost={}",
                    event.executionId(), event.modelName(), cost);
        } catch (Exception e) {
            log.error("Failed to process CostRecordedEvent: eventId={}", event.eventId(), e);
            throw propagateCostProjectionFailure(e);
        }
    }

    private void processTokenUsedEvent(ExecutionEventMessage event) {
        try {
            JsonNode payload = objectMapper.readTree(event.payload());
            String modelName = payload.has("modelName") ? payload.get("modelName").asText() : null;
            Long inputTokens = payload.has("inputTokens") ? payload.get("inputTokens").asLong() : 0L;
            Long outputTokens = payload.has("outputTokens") ? payload.get("outputTokens").asLong() : 0L;

            String tenantId = event.tenantId() != null ? String.valueOf(event.tenantId()) : null;
            ModelPricing pricing = modelName != null ? resolvePricing(tenantId, modelName) : null;
            BigDecimal cost = computeCost(pricing, inputTokens, outputTokens);
            if (cost.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("Zero cost calculated for event: eventId={}, model={}", event.eventId(), modelName);
            }

            SfCostRecord record = new SfCostRecord();
            record.setExecutionId(event.executionId());
            record.setTenantId(String.valueOf(event.tenantId()));
            record.setRequestType(event.eventType());
            record.setCostAmount(cost);
            record.setCurrency(pricing != null ? pricing.currency() : DEFAULT_CURRENCY);
            record.setModelName(modelName);
            record.setInputTokens(inputTokens);
            record.setOutputTokens(outputTokens);
            record.setTotalTokens(inputTokens + outputTokens);
            record.setOccurredAt(LocalDateTime.now());
            record.setAgentId(event.agentId());

            costRecordMapper.insert(record);

            if (event.tenantId() != null) {
                budgetService.addUsedAmount(String.valueOf(event.tenantId()), cost);
            }

            log.info("Cost record saved: executionId={}, eventType={}, cost={}",
                    event.executionId(), event.eventType(), cost);
        } catch (Exception e) {
            log.error("Failed to process TOKEN_USED event: eventId={}", event.eventId(), e);
            throw propagateCostProjectionFailure(e);
        }
    }

    private void processToolCallEvent(ExecutionEventMessage event) {
        try {
            SfCostRecord record = new SfCostRecord();
            record.setExecutionId(event.executionId());
            record.setTenantId(String.valueOf(event.tenantId()));
            record.setRequestType(event.eventType());
            record.setCostAmount(TOOL_CALL_BASE_FEE);
            record.setCurrency("USD");
            record.setOccurredAt(LocalDateTime.now());
            record.setAgentId(event.agentId());

            costRecordMapper.insert(record);

            if (event.tenantId() != null) {
                budgetService.addUsedAmount(String.valueOf(event.tenantId()), TOOL_CALL_BASE_FEE);
            }

            log.info("Cost record saved: executionId={}, eventType={}, cost={}",
                    event.executionId(), event.eventType(), TOOL_CALL_BASE_FEE);
        } catch (Exception e) {
            log.error("Failed to process TOOL_CALL event: eventId={}", event.eventId(), e);
            throw propagateCostProjectionFailure(e);
        }
    }

    private RuntimeException propagateCostProjectionFailure(Exception e) {
        if (e instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException("Failed to process execution event for cost projection", e);
    }
}
