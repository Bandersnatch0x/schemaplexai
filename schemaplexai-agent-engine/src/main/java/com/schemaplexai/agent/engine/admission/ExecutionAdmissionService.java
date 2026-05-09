package com.schemaplexai.agent.engine.admission;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.redis.TenantRedisKeyResolver;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.agent.engine.tool.ToolCallBudgetService;
import com.schemaplexai.ops.service.BudgetGuard;
import com.schemaplexai.ops.service.BudgetStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionAdmissionService {

    private final StringRedisTemplate redisTemplate;
    private final ToolCallBudgetService toolCallBudgetService;
    private final BudgetGuard budgetGuard;

    public AdmissionResult admit(String tenantId, Long agentId, TokenBudget tokenBudget) {
        // Dimension 1: Rate limit (requests per minute)
        String rateKey = TenantRedisKeyResolver.admissionRate(tenantId, String.valueOf(agentId));
        Long currentRate = redisTemplate.opsForValue().increment(rateKey);
        if (currentRate != null && currentRate == 1) {
            redisTemplate.expire(rateKey, Duration.ofMinutes(1));
        }
        if (currentRate != null && currentRate > 60) {
            return AdmissionResult.builder()
                    .allowed(false)
                    .reason("Rate limit exceeded")
                    .build();
        }

        // Dimension 2: Concurrency limit
        String concurrencyKey = TenantRedisKeyResolver.admissionConcurrency(tenantId, String.valueOf(agentId));
        Long concurrency = redisTemplate.opsForValue().increment(concurrencyKey);
        if (concurrency != null && concurrency > 5) {
            redisTemplate.opsForValue().decrement(concurrencyKey);
            return AdmissionResult.builder()
                    .allowed(false)
                    .reason("Concurrency limit exceeded")
                    .build();
        }

        // Dimension 3: Token budget check
        if (tokenBudget != null && tokenBudget.isExceeded()) {
            if (tokenBudget.isToolCallsExceeded()) {
                return AdmissionResult.builder()
                        .allowed(false)
                        .reason("Tool-call budget exceeded")
                        .build();
            }
            return AdmissionResult.builder()
                    .allowed(false)
                    .reason("Token budget exceeded")
                    .suggestedCompression(CompressionStrategy.SUMMARIZE)
                    .build();
        }

        // Dimension 4: Cost budget check via BudgetGuard
        try {
            Long tenantIdLong = Long.parseLong(tenantId);
            BigDecimal projectedCost = new BigDecimal("0.10");
            BudgetStatus budgetStatus = budgetGuard.checkBudget(tenantIdLong, projectedCost);
            if (budgetStatus == BudgetStatus.EXCEEDED) {
                return AdmissionResult.builder()
                        .allowed(false)
                        .reason("Budget exceeded")
                        .build();
            } else if (budgetStatus == BudgetStatus.WARNING) {
                log.warn("Budget warning for tenant {}", tenantId);
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid tenantId for budget check: {}", tenantId);
        }

        // Dimension 5: Per-tenant daily tool-call budget
        if (!toolCallBudgetService.hasRemainingBudget(tenantId)) {
            return AdmissionResult.builder()
                    .allowed(false)
                    .reason("Daily tool-call budget exceeded (" + toolCallBudgetService.getDailyLimit() + "/day)")
                    .build();
        }

        return AdmissionResult.builder()
                .allowed(true)
                .reason("OK")
                .build();
    }

    public void releaseConcurrency(String tenantId, Long agentId) {
        String concurrencyKey = TenantRedisKeyResolver.admissionConcurrency(tenantId, String.valueOf(agentId));
        redisTemplate.opsForValue().decrement(concurrencyKey);
    }
}
