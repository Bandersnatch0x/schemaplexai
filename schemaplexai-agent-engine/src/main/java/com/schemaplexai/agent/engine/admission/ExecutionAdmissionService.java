package com.schemaplexai.agent.engine.admission;

import com.schemaplexai.common.constants.CommonConstants;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionAdmissionService {

    private final StringRedisTemplate redisTemplate;

    private static final String RATE_LIMIT_PREFIX = "sf:admission:rate:";
    private static final String CONCURRENCY_PREFIX = "sf:admission:concurrency:";
    private static final String TOKEN_PREFIX = "sf:admission:token:";
    private static final String COST_PREFIX = "sf:admission:cost:";

    public AdmissionResult admit(String tenantId, Long agentId, TokenBudget tokenBudget) {
        // Dimension 1: Rate limit (requests per minute)
        String rateKey = RATE_LIMIT_PREFIX + tenantId + ":" + agentId;
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
        String concurrencyKey = CONCURRENCY_PREFIX + tenantId + ":" + agentId;
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

        // Dimension 4: Cost budget check (simplified)
        String costKey = COST_PREFIX + tenantId;
        String costValue = redisTemplate.opsForValue().get(costKey);
        double currentCost = costValue == null ? 0.0 : Double.parseDouble(costValue);
        if (currentCost > 100.0) {
            return AdmissionResult.builder()
                    .allowed(false)
                    .reason("Daily cost budget exceeded")
                    .build();
        }

        return AdmissionResult.builder()
                .allowed(true)
                .reason("OK")
                .build();
    }

    public void releaseConcurrency(String tenantId, Long agentId) {
        String concurrencyKey = CONCURRENCY_PREFIX + tenantId + ":" + agentId;
        redisTemplate.opsForValue().decrement(concurrencyKey);
    }
}
