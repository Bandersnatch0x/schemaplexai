---
title: ExecutionAdmissionService
type: service
source: schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/admission/ExecutionAdmissionService.java
creation_date: 2026-05-01
update_date: 2026-05-01
tags: [service, agent, admission, rate-limit, concurrency, budget]
confidence: high
---

# ExecutionAdmissionService

> One-sentence summary: Multi-dimensional admission controller that gates agent execution based on rate limits, concurrency caps, token budgets, and daily cost budgets.

## Responsibilities

1. **Rate limiting** — Max 60 requests/minute per tenant + agent
2. **Concurrency limiting** — Max 5 concurrent executions per tenant + agent
3. **Token budget check** — Verify input/output token limits
4. **Cost budget check** — Daily cost cap ($100 default)
5. **Concurrency release** — Decrement counter in `finally` block

## Key Code

```java
public AdmissionResult admit(String tenantId, Long agentId, TokenBudget tokenBudget) {
    // 1. Rate limit (60 req/min)
    String rateKey = "sf:admission:rate:" + tenantId + ":" + agentId;
    Long currentRate = redisTemplate.opsForValue().increment(rateKey);
    if (currentRate != null && currentRate == 1) {
        redisTemplate.expire(rateKey, Duration.ofMinutes(1));
    }
    if (currentRate != null && currentRate > 60) {
        return AdmissionResult.builder().allowed(false).reason("Rate limit exceeded").build();
    }

    // 2. Concurrency limit (5 max)
    String concurrencyKey = "sf:admission:concurrency:" + tenantId + ":" + agentId;
    Long concurrency = redisTemplate.opsForValue().increment(concurrencyKey);
    if (concurrency != null && concurrency > 5) {
        redisTemplate.opsForValue().decrement(concurrencyKey);
        return AdmissionResult.builder().allowed(false).reason("Concurrency limit exceeded").build();
    }

    // 3. Token budget
    if (tokenBudget.isExceeded()) {
        return AdmissionResult.builder()
            .allowed(false)
            .reason("Token budget exceeded")
            .suggestedCompression(CompressionStrategy.SUMMARIZE)
            .build();
    }

    // 4. Cost budget ($100/day)
    String costKey = "sf:admission:cost:" + tenantId;
    String costValue = redisTemplate.opsForValue().get(costKey);
    double currentCost = costValue == null ? 0.0 : Double.parseDouble(costValue);
    if (currentCost > 100.0) {
        return AdmissionResult.builder().allowed(false).reason("Daily cost budget exceeded").build();
    }

    return AdmissionResult.builder().allowed(true).reason("OK").build();
}

public void releaseConcurrency(String tenantId, Long agentId) {
    String concurrencyKey = "sf:admission:concurrency:" + tenantId + ":" + agentId;
    redisTemplate.opsForValue().decrement(concurrencyKey);
}
```

## Admission Dimensions

| Dimension | Limit | Redis Key Pattern |
|-----------|-------|-------------------|
| Rate | 60 req/min | `sf:admission:rate:{tenantId}:{agentId}` |
| Concurrency | 5 | `sf:admission:concurrency:{tenantId}:{agentId}` |
| Token | 32k input / 4k output | Checked via `TokenBudget.isExceeded()` |
| Cost | $100/day | `sf:admission:cost:{tenantId}` |

## Redis Keys

| Key Pattern | TTL | Purpose |
|-------------|-----|---------|
| `sf:admission:rate:{tenant}:{agent}` | 1 min | Sliding window rate counter |
| `sf:admission:concurrency:{tenant}:{agent}` | None | Active execution counter |
| `sf:admission:token:{tenant}:{agent}` | — | Token usage (reserved) |
| `sf:admission:cost:{tenant}` | — | Daily cost accumulator |

## Dependencies

| Component | Role |
|-----------|------|
| `StringRedisTemplate` | All counters and state |

## Backlinks

- Orchestrator: [[services/agent-runtime-orchestrator]]
- Entity: [[entities/agent]]
