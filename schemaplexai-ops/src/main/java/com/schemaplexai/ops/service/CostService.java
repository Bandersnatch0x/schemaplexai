package com.schemaplexai.ops.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.model.event.ExecutionEventMessage;
import com.schemaplexai.ops.entity.SfBudget;
import com.schemaplexai.ops.entity.SfCostRecord;
import com.schemaplexai.ops.mapper.BudgetMapper;
import com.schemaplexai.ops.mapper.SfCostRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class CostService {

    private static final BigDecimal GPT4_INPUT_RATE = new BigDecimal("0.03");
    private static final BigDecimal GPT4_OUTPUT_RATE = new BigDecimal("0.06");
    private static final BigDecimal GPT35_INPUT_RATE = new BigDecimal("0.0015");
    private static final BigDecimal GPT35_OUTPUT_RATE = new BigDecimal("0.002");
    private static final BigDecimal TOOL_CALL_BASE_FEE = new BigDecimal("0.01");
    private static final BigDecimal TOKEN_SCALE = new BigDecimal("1000");
    private static final int COST_SCALE = 4;

    private final BudgetMapper budgetMapper;
    private final SfCostRecordMapper costRecordMapper;
    private final BudgetService budgetService;
    private final ObjectMapper objectMapper;

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

    public void checkBudgetAlerts() {
        List<SfBudget> budgets = budgetMapper.selectList(null);
        for (SfBudget budget : budgets) {
            if (budget.getLimitAmount() == null || budget.getUsedAmount() == null) {
                continue;
            }

            BigDecimal ratio = budget.getUsedAmount()
                    .divide(budget.getLimitAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            if (ratio.compareTo(BigDecimal.valueOf(100)) >= 0) {
                log.warn("Budget exceeded: type={}, used={}/{} ({}%)",
                        budget.getBudgetType(), budget.getUsedAmount(),
                        budget.getLimitAmount(), ratio);
            } else if (budget.getAlertThreshold() != null &&
                    ratio.compareTo(budget.getAlertThreshold()) >= 0) {
                log.warn("Budget alert threshold reached: type={}, used={}/{} ({}%)",
                        budget.getBudgetType(), budget.getUsedAmount(),
                        budget.getLimitAmount(), ratio);
            }
        }
    }

    /**
     * Calculate cost for a given model and token usage.
     *
     * @param modelName    the LLM model name
     * @param inputTokens  number of input tokens
     * @param outputTokens number of output tokens
     * @return the calculated cost in USD
     */
    public BigDecimal calculateCost(String modelName, Long inputTokens, Long outputTokens) {
        if (modelName == null || inputTokens == null || outputTokens == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal inputRate;
        BigDecimal outputRate;

        if (modelName.contains("gpt-4") || modelName.equalsIgnoreCase("gpt-4")) {
            inputRate = GPT4_INPUT_RATE;
            outputRate = GPT4_OUTPUT_RATE;
        } else if (modelName.contains("gpt-3.5") || modelName.equalsIgnoreCase("gpt-3.5-turbo")) {
            inputRate = GPT35_INPUT_RATE;
            outputRate = GPT35_OUTPUT_RATE;
        } else {
            return BigDecimal.ZERO;
        }

        BigDecimal inputCost = inputRate.multiply(BigDecimal.valueOf(inputTokens))
                .divide(TOKEN_SCALE, COST_SCALE, RoundingMode.HALF_UP);
        BigDecimal outputCost = outputRate.multiply(BigDecimal.valueOf(outputTokens))
                .divide(TOKEN_SCALE, COST_SCALE, RoundingMode.HALF_UP);

        return inputCost.add(outputCost);
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

        try {
            switch (event.eventType()) {
                case "TOKEN_USED" -> processTokenUsedEvent(event);
                case "TOOL_CALL" -> processToolCallEvent(event);
                default -> log.debug("Unsupported event type for cost projection: {}", event.eventType());
            }
        } catch (Exception e) {
            log.error("Failed to process execution event for cost: eventId={}, eventType={}",
                    event.eventId(), event.eventType(), e);
        }
    }

    private void processTokenUsedEvent(ExecutionEventMessage event) {
        try {
            JsonNode payload = objectMapper.readTree(event.payload());
            String modelName = payload.has("modelName") ? payload.get("modelName").asText() : null;
            Long inputTokens = payload.has("inputTokens") ? payload.get("inputTokens").asLong() : 0L;
            Long outputTokens = payload.has("outputTokens") ? payload.get("outputTokens").asLong() : 0L;

            BigDecimal cost = calculateCost(modelName, inputTokens, outputTokens);
            if (cost.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("Zero cost calculated for event: eventId={}, model={}", event.eventId(), modelName);
            }

            SfCostRecord record = new SfCostRecord();
            record.setExecutionId(event.executionId());
            record.setTenantId(String.valueOf(event.tenantId()));
            record.setRequestType(event.eventType());
            record.setCostAmount(cost);
            record.setCurrency("USD");
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
        }
    }
}
