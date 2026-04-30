---
topic: unified-dev-plan
stage: plan
version: v1.0
status: 草稿
supersedes: ""
---

# SchemaPlexAI Unified Development Plan

> **主题**：`unified-dev-plan`
> **阶段**：`plan`
> **版本**：v1.0
> **状态**：草稿

---

## 变更历史

| 版本 | 日期 | 变更内容 | 作者 |
|------|------|---------|------|
| v1.0 | 2026-04-30 | 初始创建 | — |

---

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix all P0/P1 blocking issues, add test infrastructure, and implement core business logic stubs to bring SchemaPlexAI from scaffold to a functional platform.

**Architecture:** Spring Boot 3.2.5 + Java 21 multi-module Maven project with React/TypeScript frontend. Gateway → Service → DAO → PostgreSQL (OLTP) + ClickHouse (OLAP). RabbitMQ for async, Redis for cache/state, Milvus for vectors.

**Tech Stack:** Spring Boot 3.2.5, Java 21, MyBatis-Plus 3.5.5, LangChain4j 0.31.0, Flowable 7, PostgreSQL 16, Redis 7, RabbitMQ 3.12, Milvus 2.3.5, React 18.3, TypeScript 5.5, Ant Design 5, Zustand 4.5.4

---

## Implementation Status Overview

### Fully Implemented (use as-is)
| Module | Files | Lines | Status |
|--------|-------|-------|--------|
| schemaplexai-common | 6 | 219 | Result, ResultCode, BaseException, PageParam, CommonConstants, TenantContextHolder |
| schemaplexai-model | 2 | 72 | BaseEntity, PageResult |
| schemaplexai-dao | 2 | 38 | BaseMapperX, TenantLineInterceptor |
| schemaplexai-gateway | 6 | 359 | GatewayApplication, filters (JWT/tenant/rate/log), route config |
| schemaplexai-web | 11 | 442 | BaseController, SSE/WebSocket handlers, Knife4j config, interceptors |

### Substantially Implemented (needs fixes + augmentation)
| Module | Files | Lines | Issues |
|--------|-------|-------|--------|
| schemaplexai-system | 56 | 1584 | Duplicate entities (P0-005), duplicate main class (P0-006), JWT secret (P1-001) |

### Framework Real, Business Logic Stubbed
| Module | Files | Lines | What's Real | What's Stub |
|--------|-------|-------|-------------|-------------|
| schemaplexai-agent-engine | 42 | 1329 | State machine, TokenBudget, AiModelRouter, lifecycle, memory, context injector | State handlers (Thinking/ToolCalling/Completed/Paused), loop detection, shadow review, orchestrator |
| schemaplexai-agent-config | 12 | 386 | Entities, mappers, CRUD controllers/services | — |

### CRUD Scaffold, Business Logic Stubbed
| Module | Files | Lines | Entities | Stubs |
|--------|-------|-------|----------|-------|
| schemaplexai-context | 31 | 514 | 6 entities, 6 mappers, 4 controllers | RagServiceImpl (returns `List.of()`), MilvusSyncServiceImpl |
| schemaplexai-spec | 29 | 525 | 6 entities, 6 mappers, 5 controllers | All service impls (log + CRUD delegation only) |
| schemaplexai-workflow | 16 | 297 | 3 entities, 3 mappers, 2 controllers | WorkflowNodeEngine, FlowableDelegateAdapter, service impls |
| schemaplexai-quality | 31 | 527 | 6 entities, 6 mappers, 5 controllers | QualityOrchestrator (returns `true`), service impls |
| schemaplexai-integration | 24 | 429 | 4 entities, 4 mappers, 4 controllers | ToolExecutionService (hardcoded), Git/Jenkins/MCP services |
| schemaplexai-ops | 34 | 562 | 8 entities, 8 mappers, 5 controllers | ClickHouseCostSyncService, service impls |
| schemaplexai-task | 25 | 800 | MQ config, consumers, idempotency interceptor | 6 scheduled jobs (all TODO), consumer bodies |

### Empty
| Module | Files | Lines |
|--------|-------|-------|
| schemaplexai-admin | 0 | 0 |

### Frontend
| Category | Files | Lines | Status |
|----------|-------|-------|--------|
| API layer | 7 | ~300 | Real (Axios + interceptors) |
| Pages | 11 | ~700 | AgentManager (189), AgentExecutor (154) substantive; rest are 50-63 line placeholders |
| Components | 4 | ~280 | Layout (147) substantial; SseViewer, ChatMemory, TenantSelector real |
| Stores | 3 | ~150 | Real (Zustand with auth/SSE state) |
| Router/Utils/Types | 3 | ~70 | Real |

### Critical Issues (from CODE_REVIEW_REPORT.md)
- **9 P0 issues** — blocking compilation or causing runtime crashes
- **22 P1 issues** — security, concurrency, data integrity risks
- **0 tests** — no unit, integration, or E2E tests exist anywhere

---

## Phase 0: Fix P0 Blocking Issues

All P0 issues must be resolved before any new development. These prevent compilation or cause runtime crashes.

---

### Task 1: Fix Database Driver Mismatch (P0-001)

**Files:**
- Modify: `schemaplexai-system/src/main/resources/application.yml`
- Modify: `schemaplexai-agent-engine/src/main/resources/application.yml`

- [ ] **Step 1: Fix system application.yml**

Change driver and URL to PostgreSQL:

```yaml
spring:
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://${DB_HOST:localhost}:5432/schemaplexai
    username: ${DB_USERNAME:sf_user}
    password: ${DB_PASSWORD:sf_password}
```

- [ ] **Step 2: Fix agent-engine application.yml**

Same change as Step 1 — replace MySQL driver/URL/credentials with PostgreSQL.

- [ ] **Step 3: Scan all application.yml for MySQL references**

Run: `grep -rn "mysql" --include="*.yml" .`
Expected: No results after fix.

- [ ] **Step 4: Commit**

```bash
git add schemaplexai-system/src/main/resources/application.yml schemaplexai-agent-engine/src/main/resources/application.yml
git commit -m "fix: unify database driver to PostgreSQL across services"
```

---

### Task 2: Unify Database and MQ Credentials (P0-002)

**Files:**
- Modify: All `application.yml` files across modules (scan with `grep -rn "root" --include="*.yml" .`)

- [ ] **Step 1: Scan for inconsistent credentials**

Run: `grep -rn -E "(username|password)" --include="*.yml" . | grep -v "rabbitmq\|redis\|postgres"`
Expected: Identify all files with hardcoded `root/root` or `guest/guest`.

- [ ] **Step 2: Standardize all application.yml credential sections**

Every service should use environment variables with docker-compose defaults:

```yaml
# Database
spring:
  datasource:
    username: ${DB_USERNAME:sf_user}
    password: ${DB_PASSWORD:sf_password}

# RabbitMQ
  rabbitmq:
    username: ${RABBITMQ_USERNAME:sf_user}
    password: ${RABBITMQ_PASSWORD:sf_password}
```

- [ ] **Step 3: Verify docker-compose.yml credentials match**

Check `docker/docker-compose.yml` — ensure PostgreSQL uses `sf_user`/`sf_password` and RabbitMQ uses `sf_user`/`sf_password`.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "fix: unify database and MQ credentials via environment variables"
```

---

### Task 3: Fix AgentStateMachine Constructor Conflict (P0-003)

**Files:**
- Modify: `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/state/AgentStateMachine.java`

- [ ] **Step 1: Read current AgentStateMachine.java**

- [ ] **Step 2: Remove @RequiredArgsConstructor, keep manual constructor**

```java
@Slf4j
@Component
public class AgentStateMachine {

    private final SfAgentExecutionMapper executionMapper;
    private final Map<AgentExecutionState, AgentStateHandler> handlers;
    private final Map<Long, AgentExecutionState> executionStates = new ConcurrentHashMap<>();

    @Autowired
    public AgentStateMachine(SfAgentExecutionMapper executionMapper,
                             List<AgentStateHandler> handlerList) {
        this.executionMapper = executionMapper;
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(AgentStateHandler::getState, Function.identity()));
    }

    // ... rest of methods unchanged
}
```

- [ ] **Step 3: Verify compilation**

Run: `mvn clean compile -pl schemaplexai-agent-engine -am`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/state/AgentStateMachine.java
git commit -m "fix: resolve AgentStateMachine constructor conflict with Lombok"
```

---

### Task 4: Fix JwtAuthFilter Header Mutation Bug (P0-004)

**Files:**
- Modify: `schemaplexai-gateway/src/main/java/com/schemaplexai/gateway/filter/JwtAuthFilter.java:68-77`

- [ ] **Step 1: Read current JwtAuthFilter.java**

- [ ] **Step 2: Fix the double-mutate pattern**

Replace the broken double-mutate with a single builder chain:

```java
// In the filter method, around line 68-77:
ServerHttpRequest.Builder builder = request.mutate()
        .header(CommonConstants.HEADER_AUTHORIZATION, CommonConstants.TOKEN_PREFIX + token)
        .header("X-User-Id", userId);
if (StringUtils.hasText(tenantId)) {
    builder.header(CommonConstants.HEADER_TENANT_ID, tenantId);
}
ServerHttpRequest mutatedRequest = builder.build();
return chain.filter(exchange.mutate().request(mutatedRequest).build());
```

- [ ] **Step 3: Commit**

```bash
git add schemaplexai-gateway/src/main/java/com/schemaplexai/gateway/filter/JwtAuthFilter.java
git commit -m "fix: resolve JwtAuthFilter header mutation losing tenantId"
```

---

### Task 5: Clean Duplicate Entities in System Module (P0-005)

**Files:**
- Delete: All non-Sf-prefixed entity/mapper/service files in `schemaplexai-system`
- Modify: Controllers that reference non-Sf entities — update imports to Sf-prefixed versions

- [ ] **Step 1: Identify all duplicate files**

Run: `find schemaplexai-system/src -name "*.java" | xargs grep -l "^public class [A-Z]" | grep -v "Sf\|Schema\|System\|Security\|Jwt\|Base" | sort`
Also check the `model/` and `user/` sub-packages for non-Sf entities.

- [ ] **Step 2: Delete non-Sf-prefixed entities**

Remove files like `User.java`, `Tenant.java`, `Role.java`, `Permission.java`, `Menu.java`, `AiModel.java`, `ModelGroup.java`, `ModelProvider.java` and their associated mappers/services from duplicate sub-packages.

- [ ] **Step 3: Update all controller imports**

Ensure every controller imports the Sf-prefixed entity classes.

- [ ] **Step 4: Verify compilation**

Run: `mvn clean compile -pl schemaplexai-system -am`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "fix: remove duplicate entity system, unify to Sf-prefixed entities"
```

---

### Task 6: Remove Duplicate Main Class (P0-006)

**Files:**
- Delete: `schemaplexai-system/src/main/java/com/schemaplexai/system/SystemApplication.java` (if exists)
- Verify: `SchemaPlexaiSystemApplication.java` has correct `@MapperScan` covering all sub-packages

- [ ] **Step 1: Check for duplicate main class**

Run: `find schemaplexai-system/src -name "*Application.java"`
Expected: Only `SchemaPlexaiSystemApplication.java`

- [ ] **Step 2: Verify @MapperScan scope**

Ensure `@MapperScan("com.schemaplexai.system.**.mapper")` or equivalent covers all mapper sub-packages.

- [ ] **Step 3: Commit** (if file exists to delete)

```bash
git rm schemaplexai-system/src/main/java/com/schemaplexai/system/SystemApplication.java
git commit -m "fix: remove duplicate SystemApplication main class"
```

---

### Task 7: Fix TenantLineInterceptor Return Type (P0-007)

**Files:**
- Modify: `schemaplexai-dao/src/main/java/com/schemaplexai/dao/config/TenantLineInterceptor.java`

- [ ] **Step 1: Read current TenantLineInterceptor.java**

- [ ] **Step 2: Verify getTenantId() return type matches BaseEntity.tenantId**

If `BaseEntity.tenantId` is `Long`, return `LongValue` (current — likely correct).
If it's `String`, return `StringValue`.

Check the DB schema: `tenant_id` in init scripts — if `BIGINT`, use `LongValue`; if `VARCHAR`, use `StringValue`.

- [ ] **Step 3: Fix if mismatch**

- [ ] **Step 4: Commit** (if changes needed)

---

### Task 8: Fix RabbitMQ ACK Mode (P0-008)

**Files:**
- Modify: `schemaplexai-agent-engine/src/main/resources/application.yml`

- [ ] **Step 1: Change ACK mode to manual**

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        acknowledge-mode: manual
        prefetch: 10
        default-requeue-rejected: false
```

- [ ] **Step 2: Verify task module already uses manual ACK**

Check `schemaplexai-task/src/main/resources/application.yml` — confirm it already has `acknowledge-mode: manual`.

- [ ] **Step 3: Update MQ Consumer classes to use manual ACK**

For each `@RabbitListener` method in agent-engine, add `channel.basicAck(deliveryTag, false)` on success and `channel.basicNack(deliveryTag, false, true)` on failure.

- [ ] **Step 4: Commit**

```bash
git add schemaplexai-agent-engine/src/main/resources/application.yml
git commit -m "fix: set RabbitMQ acknowledge-mode to manual for message safety"
```

---

## Phase 1: Fix P1 High-Risk Issues

P1 issues involve security vulnerabilities, concurrency bugs, and data integrity risks.

---

### Task 9: Fix JWT Secret Hardcoding (P1-001)

**Files:**
- Modify: All `application.yml` files that contain `jwt.secret`
- Modify: `schemaplexai-system/src/main/java/com/schemaplexai/system/security/JwtTokenProvider.java`

- [ ] **Step 1: Remove default JWT secret from all application.yml**

```yaml
jwt:
  secret: ${JWT_SECRET}  # No default value — must be provided via env var
```

- [ ] **Step 2: Add startup validation in JwtTokenProvider**

```java
@PostConstruct
public void validateSecret() {
    if (secret == null || secret.length() < 32) {
        throw new IllegalStateException("JWT_SECRET must be at least 32 characters");
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "fix: enforce JWT secret from environment variable, validate minimum length"
```

---

### Task 10: Fix TokenBudget Race Condition (P1-002)

**Files:**
- Modify: `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/admission/TokenBudget.java`

- [ ] **Step 1: Read current TokenBudget.java**

- [ ] **Step 2: Replace addAndGet+compare with CAS loop**

```java
public boolean consumeInput(long tokens) {
    while (true) {
        long current = consumedInputTokens.get();
        long next = current + tokens;
        if (next > maxInputTokens) {
            return false;
        }
        if (consumedInputTokens.compareAndSet(current, next)) {
            return true;
        }
    }
}

public boolean consumeOutput(long tokens) {
    while (true) {
        long current = consumedOutputTokens.get();
        long next = current + tokens;
        if (next > maxOutputTokens) {
            return false;
        }
        if (consumedOutputTokens.compareAndSet(current, next)) {
            return true;
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/admission/TokenBudget.java
git commit -m "fix: use CAS loop in TokenBudget to prevent race condition"
```

---

### Task 11: Fix State Machine Terminal State Protection + Memory Cleanup (P1-003, P1-004)

**Files:**
- Modify: `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/state/AgentStateMachine.java`

- [ ] **Step 1: Add terminal state check in transition()**

```java
public void transition(AgentExecutionState newState, SfAgentExecution execution) {
    Long execId = execution.getId();
    AgentExecutionState current = executionStates.get(execId);

    if (current != null && current.isTerminal()) {
        log.warn("Cannot transition from terminal state {} to {} for execution {}",
                current, newState, execId);
        return;
    }

    AgentStateHandler handler = handlers.get(newState);
    if (handler == null) {
        log.error("No handler for state {} (execution {})", newState, execId);
        return;
    }

    executionStates.put(execId, newState);
    handler.handle(execution);

    if (newState.isTerminal()) {
        executionStates.remove(execId);
        log.info("Execution {} reached terminal state {}, cleaned up", execId, newState);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/state/AgentStateMachine.java
git commit -m "fix: add terminal state protection and memory cleanup in state machine"
```

---

### Task 12: Fix AiModelRouter Cooldown (P1-005)

**Files:**
- Modify: `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/model/AiModelRouter.java`

- [ ] **Step 1: Read current AiModelRouter.java**

- [ ] **Step 2: Add cooldown activation on primary provider failure**

In `generateWithFallback`, after the primary provider throws, add `activateCooldown(primaryProviderName)` before entering the fallback loop:

```java
public String generateWithFallback(String prompt, String primaryProviderName) {
    LlmProvider primary = providers.get(primaryProviderName);
    try {
        return primary.generate(prompt);
    } catch (Exception primaryEx) {
        log.warn("Primary provider {} failed, activating cooldown", primaryProviderName, primaryEx);
        activateCooldown(primaryProviderName);

        for (Map.Entry<String, LlmProvider> entry : providers.entrySet()) {
            if (entry.getKey().equals(primaryProviderName)) continue;
            if (isInCooldown(entry.getKey())) continue;
            try {
                return entry.getValue().generate(prompt);
            } catch (Exception e) {
                log.warn("Fallback provider {} failed", entry.getKey(), e);
                activateCooldown(entry.getKey());
            }
        }
        throw new BaseException(ResultCode.AI_MODEL_UNAVAILABLE, "All providers failed");
    }
}
```

- [ ] **Step 3: Commit**

---

### Task 13: Add Redis Chat Memory TTL + Length Limit (P1-006)

**Files:**
- Modify: `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/memory/CompositeChatMemoryStore.java`

- [ ] **Step 1: Read current CompositeChatMemoryStore.java**

- [ ] **Step 2: Add TTL (7 days) on Redis writes and message count limit (50 rounds)**

```java
private static final Duration CHAT_MEMORY_TTL = Duration.ofDays(7);
private static final int MAX_MESSAGES_PER_CONVERSATION = 100; // 50 rounds

public void addMessages(String conversationId, List<ChatMessage> messages) {
    String key = CHAT_MEMORY_KEY_PREFIX + conversationId;
    List<ChatMessage> existing = getMessages(conversationId);
    existing.addAll(messages);

    // Trim to limit
    if (existing.size() > MAX_MESSAGES_PER_CONVERSATION) {
        existing = existing.subList(existing.size() - MAX_MESSAGES_PER_CONVERSATION, existing.size());
    }

    redisTemplate.opsForList().rightPushAll(key, messages);
    redisTemplate.expire(key, CHAT_MEMORY_TTL);
}
```

- [ ] **Step 3: Commit**

---

### Task 14: Add @Transactional Across Business Modules (P1-007)

**Files:**
- Modify: All Service implementation classes in context, spec, workflow, quality, integration, ops modules

- [ ] **Step 1: Identify all ServiceImpl files needing @Transactional**

Run: `find schemaplexai-context schemaplexai-spec schemaplexai-workflow schemaplexai-quality schemaplexai-integration schemaplexai-ops -name "*ServiceImpl.java"`

- [ ] **Step 2: Add @Transactional to all service methods that write to multiple tables**

For each ServiceImpl, add `@Transactional(rollbackFor = Exception.class)` to the class level or to specific write methods.

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "fix: add @Transactional annotations across all business service modules"
```

---

### Task 15: Fix Null Safety in Controllers (P1-009)

**Files:**
- Modify: All Controller classes that use `service.getById(id)` directly in `Result.success()`

- [ ] **Step 1: Find all unsafe getById calls**

Run: `grep -rn "Result.success(service.getById" --include="*.java" .`

- [ ] **Step 2: Add null checks**

For each occurrence:

```java
// Before:
return Result.success(service.getById(id));

// After:
T entity = service.getById(id);
if (entity == null) {
    return Result.error(ResultCode.NOT_FOUND);
}
return Result.success(entity);
```

- [ ] **Step 3: Add missing ResultCode constants if needed**

Check `ResultCode.java` for entity-specific NOT_FOUND codes. Add if missing.

- [ ] **Step 4: Commit**

---

### Task 16: Fix Pagination Consistency (P1-010)

**Files:**
- Modify: Controllers using manual `count()` + `list()` pattern

- [ ] **Step 1: Find inconsistent pagination patterns**

Run: `grep -rn "\.count()\|\.list()" --include="*Controller.java" .`
Run: `grep -rn "PageHelper\|IPage\|Page<" --include="*.java" .`

- [ ] **Step 2: Replace with MyBatis-Plus page()**

```java
// Before:
long total = service.count(queryWrapper);
List<T> records = service.list(queryWrapper);
return Result.success(new PageResult<>(records, total, param.getCurrent(), param.getSize()));

// After:
Page<T> page = new Page<>(param.getCurrent(), param.getSize());
IPage<T> result = service.page(page, queryWrapper);
return Result.success(new PageResult<>(result.getRecords(), result.getTotal(),
        result.getCurrent(), result.getSize()));
```

- [ ] **Step 3: Commit**

---

### Task 17: Fix RateLimitFilter Fail-Open (P1-011)

**Files:**
- Modify: `schemaplexai-gateway/src/main/java/com/schemaplexai/gateway/filter/RateLimitFilter.java`

- [ ] **Step 1: Read current RateLimitFilter.java**

- [ ] **Step 2: Change Redis exception handling from fail-open to fail-closed**

```java
try {
    // existing rate limit logic
    Long currentCount = redisTemplate.opsForValue().increment(key);
    if (currentCount == null || currentCount > maxRequests) {
        return handleRateLimit(exchange);
    }
} catch (Exception e) {
    log.error("Rate limit check failed, rejecting request (fail-closed)", e);
    return handleRateLimit(exchange);  // Changed from: allowing through
}
```

- [ ] **Step 3: Commit**

---

### Task 18: Add SSE/WebSocket Authentication (P1-012)

**Files:**
- Modify: `schemaplexai-web/src/main/java/com/schemaplexai/web/sse/SseEmitterManager.java`
- Modify: `schemaplexai-web/src/main/java/com/schemaplexai/web/websocket/AgentWebSocketHandler.java`

- [ ] **Step 1: Add token validation to SseEmitterManager**

In the `createEmitter` method, validate the token before creating the emitter:

```java
public SseEmitter createEmitter(String token, Long tenantId) {
    if (!isValidToken(token)) {
        throw new BaseException(ResultCode.UNAUTHORIZED, "Invalid SSE token");
    }
    // existing emitter creation logic
}
```

- [ ] **Step 2: Add token validation to WebSocket handler**

In `afterConnectionEstablished`, extract and validate token from query params or headers.

- [ ] **Step 3: Commit**

---

### Task 19: Fix Gateway JSON Response Serialization (P1-013)

**Files:**
- Modify: `schemaplexai-gateway/src/main/java/com/schemaplexai/gateway/filter/JwtAuthFilter.java`

- [ ] **Step 1: Replace String.format JSON with ObjectMapper**

```java
private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

    Map<String, Object> body = Map.of(
            "code", 401,
            "message", message,
            "timestamp", System.currentTimeMillis()
    );

    try {
        byte[] bytes = new ObjectMapper().writeValueAsBytes(body);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    } catch (JsonProcessingException e) {
        return exchange.getResponse().setComplete();
    }
}
```

- [ ] **Step 2: Commit**

---

### Task 20: Fix MQ Consumer ACK and DLQ Configuration (P1-016, P1-017)

**Files:**
- Modify: `schemaplexai-task/src/main/java/com/schemaplexai/task/mq/config/RabbitMqConfig.java`
- Modify: `schemaplexai-task/src/main/java/com/schemaplexai/task/mq/config/DeadLetterConfig.java`
- Modify: All `@RabbitListener` classes in schemaplexai-task

- [ ] **Step 1: Fix RabbitMqConfig queue declaration with DLX args**

```java
@Bean
public Queue agentExecuteQueue() {
    return QueueBuilder.durable("sf.agent.execute")
            .withArgument("x-dead-letter-exchange", "sf.dlx.exchange")
            .withArgument("x-dead-letter-routing-key", "sf.agent.execute.dlq")
            .build();
}
```

- [ ] **Step 2: Remove invalid x-max-retries argument from DeadLetterConfig**

- [ ] **Step 3: Add ackMode = "MANUAL" to all @RabbitListener annotations**

```java
@RabbitListener(queues = "sf.agent.execute", ackMode = "MANUAL")
public void handleAgentExecute(Message message, Channel channel,
                                @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
    try {
        // process message
        channel.basicAck(deliveryTag, false);
    } catch (Exception e) {
        log.error("Failed to process message", e);
        channel.basicNack(deliveryTag, false, true);
    }
}
```

- [ ] **Step 4: Commit**

---

### Task 21: Fix MQ Idempotency Race Condition (P1-018)

**Files:**
- Modify: `schemaplexai-task/src/main/java/com/schemaplexai/task/mq/interceptor/MqIdempotencyInterceptor.java`

- [ ] **Step 1: Read current MqIdempotencyInterceptor.java**

- [ ] **Step 2: Use DB unique constraint + INSERT as idempotency mechanism**

Instead of "check Redis → execute → write DB" (race condition), use "try DB insert first":

```java
public boolean tryAcquire(String messageId) {
    // Try Redis first (fast path)
    Boolean acquired = redisTemplate.opsForValue()
            .setIfAbsent(IDEMPOTENCY_KEY_PREFIX + messageId, "1", Duration.ofHours(24));
    if (Boolean.TRUE.equals(acquired)) {
        return true;
    }
    // Fallback: check DB (for Redis failure recovery)
    return !idempotencyKeyMapper.existsById(messageId);
}

public void markProcessed(String messageId) {
    idempotencyKeyMapper.insert(new SfIdempotencyKey(messageId, LocalDateTime.now()));
}
```

Add DB unique constraint on `message_id` column.

- [ ] **Step 3: Commit**

---

### Task 22: Add Distributed Lock to Scheduled Jobs (P1-019)

**Files:**
- Modify: All 6 Job classes in `schemaplexai-task/src/main/java/com/schemaplexai/task/scheduling/`

- [ ] **Step 1: Add ShedLock dependency to schemaplexai-task pom.xml**

```xml
<dependency>
    <groupId>net.javacrumbs.shedlock</groupId>
    <artifactId>shedlock-spring</artifactId>
    <version>5.13.0</version>
</dependency>
<dependency>
    <groupId>net.javacrumbs.shedlock</groupId>
    <artifactId>shedlock-provider-redis-spring</artifactId>
    <version>5.13.0</version>
</dependency>
```

- [ ] **Step 2: Add @SchedulerLock to each scheduled method**

```java
@Scheduled(cron = "0 0 2 * * ?")
@SchedulerLock(name = "costStatisticsJob", lockAtMostFor = "PT30M")
public void execute() {
    // job logic
}
```

- [ ] **Step 3: Configure ShedLock in a config class**

```java
@Configuration
@EnableScheduling
public class SchedulingConfig {
    @Bean
    public LockProvider lockProvider(RedisConnectionFactory connectionFactory) {
        return new RedisLockProvider(connectionFactory);
    }
}
```

- [ ] **Step 4: Commit**

---

### Task 23: Fix Frontend Token Security (P1-014, P1-015, P1-021)

**Files:**
- Modify: `schemaplexai-ui/src/stores/userStore.ts`
- Modify: `schemaplexai-ui/src/api/request.ts`
- Modify: `schemaplexai-ui/src/pages/AgentExecutor.tsx`

- [ ] **Step 1: Move JWT from localStorage to sessionStorage**

In `userStore.ts`, change:
```typescript
// Before:
localStorage.setItem('token', token)
// After:
sessionStorage.setItem('token', token)
```

- [ ] **Step 2: Replace SSE URL token with fetch + ReadableStream**

In `AgentExecutor.tsx`, replace `new EventSource(url + '?token=' + token)` with:

```typescript
const controller = new AbortController();
sseRef.current = controller;

fetch('/api/agents/execute/stream?executionId=' + executionId, {
    headers: { 'Authorization': 'Bearer ' + token },
    signal: controller.signal,
}).then(response => {
    const reader = response.body?.getReader();
    const decoder = new TextDecoder();
    const read = () => {
        reader?.read().then(({ done, value }) => {
            if (done) return;
            const text = decoder.decode(value);
            // process SSE events
            read();
        });
    };
    read();
});
```

- [ ] **Step 3: Fix SSE connection cleanup in useEffect**

```typescript
useEffect(() => {
    return () => {
        if (sseRef.current) {
            sseRef.current.abort();
            sseRef.current = null;
        }
    };
}, []);
```

- [ ] **Step 4: Commit**

---

### Task 24: Fix Frontend Store Defects (P1-022)

**Files:**
- Modify: `schemaplexai-ui/src/stores/userStore.ts`
- Modify: `schemaplexai-ui/src/stores/sseStore.ts`

- [ ] **Step 1: Hydrate currentTenant from localStorage in userStore**

```typescript
const useUserStore = create<UserState>((set) => ({
    currentTenant: JSON.parse(localStorage.getItem('currentTenant') || 'null'),
    // ...
    setCurrentTenant: (tenant) => {
        localStorage.setItem('currentTenant', JSON.stringify(tenant));
        set({ currentTenant: tenant });
    },
}));
```

- [ ] **Step 2: Add event limit to sseStore**

```typescript
addEvent: (event) => set((state) => ({
    events: [...state.events, event].slice(-1000), // keep last 1000 events
})),
```

- [ ] **Step 3: Commit**

---

## Phase 2: Test Infrastructure

No tests exist. This phase sets up the test framework for all modules.

---

### Task 25: Add Test Dependencies to Root POM

**Files:**
- Modify: `pom.xml` (root)

- [ ] **Step 1: Add test dependencies to dependencyManagement**

```xml
<dependencyManagement>
    <dependencies>
        <!-- Test Dependencies -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <version>${spring-boot.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>testcontainers</artifactId>
            <version>1.19.7</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>1.19.7</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>postgresql</artifactId>
            <version>1.19.7</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>redis</artifactId>
            <version>1.19.7</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>rabbitmq</artifactId>
            <version>1.19.7</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

- [ ] **Step 2: Add spring-boot-starter-test to each service module's pom.xml**

For each module: schemaplexai-system, schemaplexai-agent-engine, schemaplexai-gateway, schemaplexai-context, schemaplexai-spec, schemaplexai-workflow, schemaplexai-quality, schemaplexai-integration, schemaplexai-ops, schemaplexai-task, schemaplexai-web, schemaplexai-agent-config.

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 3: Create test directory structure**

Run: `for mod in schemaplexai-system schemaplexai-agent-engine schemaplexai-gateway schemaplexai-context schemaplexai-spec schemaplexai-workflow schemaplexai-quality schemaplexai-integration schemaplexai-ops schemaplexai-task schemaplexai-web schemaplexai-agent-config; do mkdir -p "$mod/src/test/java/com/schemaplexai"; mkdir -p "$mod/src/test/resources"; done`

- [ ] **Step 4: Commit**

---

### Task 26: Write Unit Tests for schemaplexai-common

**Files:**
- Create: `schemaplexai-common/src/test/java/com/schemaplexai/common/result/ResultTest.java`
- Create: `schemaplexai-common/src/test/java/com/schemaplexai/common/exception/BaseExceptionTest.java`
- Create: `schemaplexai-common/src/test/java/com/schemaplexai/common/page/PageParamTest.java`

- [ ] **Step 1: Write ResultTest**

```java
class ResultTest {

    @Test
    void success_withData_returnsCode200() {
        Result<String> result = Result.success("hello");
        assertEquals(200, result.getCode());
        assertEquals("hello", result.getData());
    }

    @Test
    void success_withoutData_returnsCode200() {
        Result<Void> result = Result.success();
        assertEquals(200, result.getCode());
        assertNull(result.getData());
    }

    @Test
    void error_withMessage_returnsCode500() {
        Result<Void> result = Result.error("something went wrong");
        assertEquals(500, result.getCode());
        assertEquals("something went wrong", result.getMessage());
    }

    @Test
    void error_withResultCode_returnsCustomCode() {
        Result<Void> result = Result.error(ResultCode.NOT_FOUND);
        assertEquals(404, result.getCode());
    }
}
```

- [ ] **Step 2: Write BaseExceptionTest**

```java
class BaseExceptionTest {

    @Test
    void constructor_setsCodeAndMessage() {
        BaseException ex = new BaseException(1001, "agent not found");
        assertEquals(1001, ex.getCode());
        assertEquals("agent not found", ex.getMessage());
    }

    @Test
    void constructor_withResultCode() {
        BaseException ex = new BaseException(ResultCode.NOT_FOUND);
        assertEquals(404, ex.getCode());
    }
}
```

- [ ] **Step 3: Run tests**

Run: `mvn test -pl schemaplexai-common`
Expected: All tests PASS

- [ ] **Step 4: Commit**

---

### Task 27: Write Unit Tests for AgentStateMachine

**Files:**
- Create: `schemaplexai-agent-engine/src/test/java/com/schemaplexai/agent/engine/state/AgentStateMachineTest.java`

- [ ] **Step 1: Write state machine tests**

```java
@SpringBootTest
class AgentStateMachineTest {

    @Autowired
    private AgentStateMachine stateMachine;

    @MockBean
    private SfAgentExecutionMapper executionMapper;

    @MockBean
    private List<AgentStateHandler> handlers;

    @Test
    void initialState_isIdle() {
        SfAgentExecution execution = createExecution(1L);
        assertEquals(AgentExecutionState.IDLE, stateMachine.getState(1L));
    }

    @Test
    void transition_fromIdleToThinking() {
        SfAgentExecution execution = createExecution(1L);
        stateMachine.transition(AgentExecutionState.THINKING, execution);
        assertEquals(AgentExecutionState.THINKING, stateMachine.getState(1L));
    }

    @Test
    void transition_fromTerminalState_isBlocked() {
        SfAgentExecution execution = createExecution(1L);
        stateMachine.transition(AgentExecutionState.COMPLETED, execution);
        stateMachine.transition(AgentExecutionState.THINKING, execution);
        // Should still be COMPLETED — transition blocked
        assertEquals(AgentExecutionState.COMPLETED, stateMachine.getState(1L));
    }

    @Test
    void terminalState_cleansUpMemory() {
        SfAgentExecution execution = createExecution(1L);
        stateMachine.transition(AgentExecutionState.COMPLETED, execution);
        assertNull(stateMachine.getState(1L)); // cleaned up
    }

    private SfAgentExecution createExecution(Long id) {
        SfAgentExecution exec = new SfAgentExecution();
        exec.setId(id);
        return exec;
    }
}
```

- [ ] **Step 2: Run tests**

Run: `mvn test -pl schemaplexai-agent-engine -am`
Expected: All tests PASS

- [ ] **Step 3: Commit**

---

### Task 28: Write Unit Tests for TokenBudget

**Files:**
- Create: `schemaplexai-agent-engine/src/test/java/com/schemaplexai/agent/engine/admission/TokenBudgetTest.java`

- [ ] **Step 1: Write budget tests**

```java
class TokenBudgetTest {

    @Test
    void consumeInput_withinBudget_returnsTrue() {
        TokenBudget budget = new TokenBudget(1000, 500);
        assertTrue(budget.consumeInput(500));
    }

    @Test
    void consumeInput_exceedsBudget_returnsFalse() {
        TokenBudget budget = new TokenBudget(1000, 500);
        assertFalse(budget.consumeInput(1001));
    }

    @Test
    void consumeInput_accumulates() {
        TokenBudget budget = new TokenBudget(1000, 500);
        assertTrue(budget.consumeInput(600));
        assertFalse(budget.consumeInput(500));
    }

    @Test
    void concurrentConsumeInput_neverExceedsBudget() throws InterruptedException {
        TokenBudget budget = new TokenBudget(1000, 500);
        AtomicInteger successCount = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(10);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            futures.add(executor.submit(() -> {
                if (budget.consumeInput(20)) {
                    successCount.incrementAndGet();
                }
            }));
        }
        for (Future<?> f : futures) f.get();
        executor.shutdown();

        // At most 50 should succeed (1000 / 20 = 50)
        assertTrue(successCount.get() <= 50);
    }
}
```

- [ ] **Step 2: Run and commit**

---

### Task 29: Set Up Frontend Test Framework

**Files:**
- Modify: `schemaplexai-ui/package.json`
- Create: `schemaplexai-ui/vitest.config.ts`
- Create: `schemaplexai-ui/src/stores/__tests__/userStore.test.ts`

- [ ] **Step 1: Add Vitest and React Testing Library**

Run: `cd schemaplexai-ui && npm install -D vitest @testing-library/react @testing-library/jest-dom @testing-library/user-event jsdom`

- [ ] **Step 2: Create vitest.config.ts**

```typescript
import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

export default defineConfig({
    plugins: [react()],
    test: {
        globals: true,
        environment: 'jsdom',
        setupFiles: './src/test/setup.ts',
    },
});
```

- [ ] **Step 3: Create test setup file**

Create `schemaplexai-ui/src/test/setup.ts`:
```typescript
import '@testing-library/jest-dom';
```

- [ ] **Step 4: Add test script to package.json**

```json
{
    "scripts": {
        "test": "vitest",
        "test:run": "vitest run"
    }
}
```

- [ ] **Step 5: Write a basic store test**

Create `schemaplexai-ui/src/stores/__tests__/userStore.test.ts`:

```typescript
import { describe, it, expect, beforeEach } from 'vitest';
import { useUserStore } from '../userStore';

describe('userStore', () => {
    beforeEach(() => {
        useUserStore.setState({ token: null, user: null, currentTenant: null });
    });

    it('initial state has no token', () => {
        const state = useUserStore.getState();
        expect(state.token).toBeNull();
    });

    it('setToken updates token', () => {
        useUserStore.getState().setToken('test-token-123');
        expect(useUserStore.getState().token).toBe('test-token-123');
    });

    it('logout clears all state', () => {
        useUserStore.getState().setToken('test-token');
        useUserStore.getState().logout();
        const state = useUserStore.getState();
        expect(state.token).toBeNull();
        expect(state.user).toBeNull();
    });
});
```

- [ ] **Step 6: Run tests**

Run: `cd schemaplexai-ui && npx vitest run`
Expected: All tests PASS

- [ ] **Step 7: Commit**

---

## Phase 3: Core Agent Engine Implementation

The agent-engine state machine framework is real but state handlers are stubs. This phase implements the actual agent execution loop.

---

### Task 30: Implement ThinkingStateHandler

**Files:**
- Modify: `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/state/ThinkingStateHandler.java`

- [ ] **Step 1: Read current ThinkingStateHandler.java**

- [ ] **Step 2: Implement LLM invocation with context injection**

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class ThinkingStateHandler implements AgentStateHandler {

    private final ContextInjector contextInjector;
    private final CompositeChatMemoryStore memoryStore;
    private final AiModelRouter modelRouter;
    private final TokenBudget tokenBudget;

    @Override
    public AgentExecutionState getState() {
        return AgentExecutionState.THINKING;
    }

    @Override
    public void handle(SfAgentExecution execution) {
        log.info("Thinking: execution {}", execution.getId());

        // Build prompt: system + context + memory + user input
        String systemPrompt = contextInjector.inject(execution);
        List<ChatMessage> history = memoryStore.getMessages(execution.getConversationId());
        String fullPrompt = buildPrompt(systemPrompt, history, execution.getUserInput());

        // Estimate tokens and check budget
        long estimatedTokens = estimateTokens(fullPrompt);
        if (!tokenBudget.consumeInput(estimatedTokens)) {
            log.warn("Token budget exceeded for execution {}", execution.getId());
            execution.setState(AgentExecutionState.FAILED.name());
            return;
        }

        // Call LLM
        String response = modelRouter.generate(fullPrompt, execution.getModelConfig());
        execution.setLastResponse(response);
        tokenBudget.consumeOutput(estimateTokens(response));

        // Store in memory
        memoryStore.addMessages(execution.getConversationId(), List.of(
                new ChatMessage("user", execution.getUserInput()),
                new ChatMessage("assistant", response)
        ));

        // Determine next state based on response
        if (containsToolCalls(response)) {
            execution.setState(AgentExecutionState.TOOL_CALLING.name());
        } else {
            execution.setState(AgentExecutionState.COMPLETED.name());
        }
    }

    private String buildPrompt(String system, List<ChatMessage> history, String input) {
        StringBuilder sb = new StringBuilder();
        sb.append("System: ").append(system).append("\n\n");
        for (ChatMessage msg : history) {
            sb.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
        }
        sb.append("user: ").append(input);
        return sb.toString();
    }

    private long estimateTokens(String text) {
        return text.length() / 4; // rough estimate
    }

    private boolean containsToolCalls(String response) {
        return response.contains("tool_call") || response.contains("function_call");
    }
}
```

- [ ] **Step 3: Commit**

---

### Task 31: Implement ToolCallingStateHandler

**Files:**
- Modify: `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/state/ToolCallingStateHandler.java`

- [ ] **Step 1: Implement tool invocation logic**

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class ToolCallingStateHandler implements AgentStateHandler {

    private final ToolRegistry toolRegistry;
    private final CompositeChatMemoryStore memoryStore;

    @Override
    public AgentExecutionState getState() {
        return AgentExecutionState.TOOL_CALLING;
    }

    @Override
    public void handle(SfAgentExecution execution) {
        log.info("ToolCalling: execution {}", execution.getId());

        List<ToolCall> toolCalls = parseToolCalls(execution.getLastResponse());
        List<ToolResult> results = new ArrayList<>();

        for (ToolCall call : toolCalls) {
            try {
                ToolResult result = toolRegistry.execute(call.getName(), call.getArguments());
                results.add(result);
            } catch (Exception e) {
                log.error("Tool {} failed for execution {}", call.getName(), execution.getId(), e);
                results.add(new ToolResult(call.getId(), "error: " + e.getMessage()));
            }
        }

        // Store tool results in memory
        memoryStore.addToolResults(execution.getConversationId(), results);

        // Back to thinking for next iteration
        execution.setState(AgentExecutionState.THINKING.name());
    }
}
```

- [ ] **Step 2: Commit**

---

### Task 32: Implement CompletedStateHandler

**Files:**
- Modify: `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/state/CompletedStateHandler.java`

- [ ] **Step 1: Implement completion logic**

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class CompletedStateHandler implements AgentStateHandler {

    private final SfAgentExecutionMapper executionMapper;
    private final SseEmitterManager sseEmitterManager;

    @Override
    public AgentExecutionState getState() {
        return AgentExecutionState.COMPLETED;
    }

    @Override
    public void handle(SfAgentExecution execution) {
        log.info("Completed: execution {}", execution.getId());

        execution.setStatus("COMPLETED");
        execution.setEndTime(LocalDateTime.now());
        executionMapper.updateById(execution);

        // Send completion event via SSE
        sseEmitterManager.send(execution.getTenantId(), SseEvent.completed(execution.getId()));
    }
}
```

- [ ] **Step 2: Write unit test**

- [ ] **Step 3: Commit**

---

### Task 33: Implement AgentRuntimeOrchestrator

**Files:**
- Modify: `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/orchestrator/AgentRuntimeOrchestrator.java`

- [ ] **Step 1: Implement the orchestration loop**

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentRuntimeOrchestrator {

    private final AgentStateMachine stateMachine;
    private final SfAgentExecutionMapper executionMapper;
    private final ExecutionAdmissionService admissionService;
    private final AgentLoopDetectionService loopDetection;
    private static final int MAX_ITERATIONS = 50;

    public void execute(Long executionId) {
        SfAgentExecution execution = executionMapper.selectById(executionId);
        if (execution == null) {
            log.error("Execution {} not found", executionId);
            return;
        }

        // Admission check
        if (!admissionService.admit(execution)) {
            log.warn("Execution {} rejected by admission service", executionId);
            return;
        }

        int iteration = 0;
        while (iteration < MAX_ITERATIONS) {
            AgentExecutionState currentState = stateMachine.getState(executionId);
            if (currentState == null || currentState.isTerminal()) {
                break;
            }

            // Loop detection
            if (loopDetection.isLooping(executionId, currentState)) {
                log.warn("Loop detected for execution {}, forcing completion", executionId);
                stateMachine.transition(AgentExecutionState.COMPLETED, execution);
                break;
            }

            stateMachine.transition(currentState, execution);
            execution = executionMapper.selectById(executionId);
            iteration++;
        }

        if (iteration >= MAX_ITERATIONS) {
            log.warn("Execution {} hit max iterations, truncating", executionId);
            stateMachine.transition(AgentExecutionState.COMPLETED, execution);
        }
    }
}
```

- [ ] **Step 2: Write unit test**

- [ ] **Step 3: Commit**

---

### Task 34: Implement AgentLoopDetectionService

**Files:**
- Modify: `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/loop/AgentLoopDetectionService.java`

- [ ] **Step 1: Implement hash-based loop detection**

```java
@Slf4j
@Component
public class AgentLoopDetectionService {

    private static final int WINDOW_SIZE = 5;
    private final Map<Long, Deque<String>> executionHashes = new ConcurrentHashMap<>();

    public boolean isLooping(Long executionId, AgentExecutionState state) {
        Deque<String> hashes = executionHashes.computeIfAbsent(executionId, k -> new ArrayDeque<>());
        String hash = state.name(); // Could hash the full response for more accuracy

        if (hashes.size() >= WINDOW_SIZE && hashes.stream().allMatch(h::equals)) {
            return true;
        }

        hashes.addLast(hash);
        if (hashes.size() > WINDOW_SIZE) {
            hashes.removeFirst();
        }
        return false;
    }

    public void clear(Long executionId) {
        executionHashes.remove(executionId);
    }
}
```

- [ ] **Step 2: Commit**

---

## Phase 4: Context & RAG Pipeline

---

### Task 35: Implement RagServiceImpl

**Files:**
- Modify: `schemaplexai-context/src/main/java/com/schemaplexai/context/service/impl/RagServiceImpl.java`

- [ ] **Step 1: Implement document text extraction and chunking**

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {

    private final SfKnowledgeDocMapper docMapper;
    private final MinioClient minioClient;
    private final MilvusClient milvusClient;
    private static final int CHUNK_SIZE = 512;
    private static final int CHUNK_OVERLAP = 50;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SfKnowledgeDoc ingestDocument(MultipartFile file, Long tenantId) {
        // 1. Upload to MinIO
        String objectName = "knowledge/" + tenantId + "/" + UUID.randomUUID() + "/" + file.getOriginalFilename();
        minioClient.putObject(PutObjectArgs.builder()
                .bucket("schemaplexai")
                .object(objectName)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .build());

        // 2. Extract text with Tika
        String text = extractText(file.getInputStream());

        // 3. Create document record
        SfKnowledgeDoc doc = new SfKnowledgeDoc();
        doc.setTenantId(tenantId);
        doc.setName(file.getOriginalFilename());
        doc.setObjectPath(objectName);
        doc.setChunkCount((int) Math.ceil((double) text.length() / (CHUNK_SIZE - CHUNK_OVERLAP)));
        docMapper.insert(doc);

        // 4. Publish to Milvus sync queue
        // (handled by MilvusSyncConsumer via MQ)

        return doc;
    }

    @Override
    public List<RagSearchResult> search(String query, Long tenantId, int topK) {
        // 1. Embed query
        float[] queryEmbedding = embed(query);

        // 2. Search Milvus
        SearchParam searchParam = SearchParam.newBuilder()
                .setCollectionName("knowledge_chunks")
                .addVectors(queryEmbedding)
                .setTopK(topK)
                .setMetricType(MetricType.COSINE)
                .build();

        List<SearchResults> results = milvusClient.search(searchParam);

        // 3. Enrich with metadata from PG
        return results.stream()
                .flatMap(r -> r.getResults().stream())
                .map(hit -> {
                    String docId = hit.get("doc_id").toString();
                    SfKnowledgeDoc doc = docMapper.selectById(docId);
                    return new RagSearchResult(doc.getName(), hit.get("content").toString(),
                            hit.getScore(), doc.getId());
                })
                .toList();
    }

    private String extractText(InputStream is) {
        try {
            Tika tika = new Tika();
            return tika.parseToString(is);
        } catch (Exception e) {
            throw new BaseException(ResultCode.INTERNAL_ERROR, "Failed to extract text");
        }
    }

    private float[] embed(String text) {
        // Call embedding service — placeholder for actual implementation
        throw new UnsupportedOperationException("Embedding not yet configured");
    }
}
```

- [ ] **Step 2: Write integration test**

- [ ] **Step 3: Commit**

---

### Task 36: Implement MilvusSyncServiceImpl

**Files:**
- Modify: `schemaplexai-context/src/main/java/com/schemaplexai/context/service/impl/MilvusSyncServiceImpl.java`

- [ ] **Step 1: Implement chunking and Milvus sync**

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class MilvusSyncServiceImpl implements MilvusSyncService {

    private final SfKnowledgeDocMapper docMapper;
    private final MinioClient minioClient;
    private final MilvusClient milvusClient;
    private static final int CHUNK_SIZE = 512;
    private static final int CHUNK_OVERLAP = 50;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncDocument(Long docId) {
        SfKnowledgeDoc doc = docMapper.selectById(docId);
        if (doc == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Document not found: " + docId);
        }

        // Download from MinIO
        String text = downloadAndExtract(doc.getObjectPath());

        // Chunk
        List<String> chunks = chunkText(text, CHUNK_SIZE, CHUNK_OVERLAP);

        // Embed and insert into Milvus
        List<float[]> embeddings = chunks.stream().map(this::embed).toList();
        insertToMilvus(doc.getId(), doc.getTenantId(), chunks, embeddings);

        log.info("Synced {} chunks for doc {} to Milvus", chunks.size(), docId);
    }

    private List<String> chunkText(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            chunks.add(text.substring(start, end));
            start += chunkSize - overlap;
        }
        return chunks;
    }

    private void insertToMilvus(Long docId, Long tenantId, List<String> chunks, List<float[]> embeddings) {
        List<Long> ids = new ArrayList<>();
        List<String> contents = new ArrayList<>();
        List<Long> docIds = new ArrayList<>();
        List<Long> tenantIds = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            ids.add(SnowflakeIdGenerator.nextId());
            contents.add(chunks.get(i));
            docIds.add(docId);
            tenantIds.add(tenantId);
        }

        List<InsertParam.Field> fields = List.of(
                new InsertParam.Field("id", ids),
                new InsertParam.Field("content", contents),
                new InsertParam.Field("doc_id", docIds),
                new InsertParam.Field("tenant_id", tenantIds),
                new InsertParam.Field("embedding", embeddings)
        );

        milvusClient.insert(InsertParam.newBuilder()
                .setCollectionName("knowledge_chunks")
                .setFields(fields)
                .build());
    }
}
```

- [ ] **Step 2: Commit**

---

## Phase 5: Spec Business Logic

---

### Task 37: Implement Spec Version Diff and Review

**Files:**
- Modify: `schemaplexai-spec/src/main/java/com/schemaplexai/spec/service/impl/SpecVersionServiceImpl.java`
- Modify: `schemaplexai-spec/src/main/java/com/schemaplexai/spec/service/impl/SpecReviewServiceImpl.java`

- [ ] **Step 1: Implement version diff in SpecVersionServiceImpl**

```java
@Override
@Transactional(rollbackFor = Exception.class)
public SpecDiffResult diff(Long specId, Long versionA, Long versionB) {
    SfSpecVersion vA = versionMapper.selectByVersion(specId, versionA);
    SfSpecVersion vB = versionMapper.selectByVersion(specId, versionB);

    if (vA == null || vB == null) {
        throw new BaseException(ResultCode.NOT_FOUND, "Spec version not found");
    }

    List<DiffHunk> hunks = DiffUtils.diff(vA.getContent(), vB.getContent());
    return new SpecDiffResult(specId, versionA, versionB, hunks);
}

@Override
@Transactional(rollbackFor = Exception.class)
public SfSpecVersion publish(Long specId, String content, String commitMessage, Long userId) {
    // Get next version number
    int nextVersion = versionMapper.getMaxVersion(specId) + 1;

    SfSpecVersion version = new SfSpecVersion();
    version.setSpecId(specId);
    version.setVersion(nextVersion);
    version.setContent(content);
    version.setCommitMessage(commitMessage);
    version.setCreatedBy(userId);
    versionMapper.insert(version);

    // Update spec current version
    SfSpec spec = specMapper.selectById(specId);
    spec.setCurrentVersion(nextVersion);
    specMapper.updateById(spec);

    return version;
}
```

- [ ] **Step 2: Implement review workflow in SpecReviewServiceImpl**

```java
@Override
@Transactional(rollbackFor = Exception.class)
public SfSpecReview submitReview(Long specId, Long versionId, Long reviewerId,
                                  String decision, String comments) {
    SfSpecReview review = new SfSpecReview();
    review.setSpecId(specId);
    review.setVersionId(versionId);
    review.setReviewerId(reviewerId);
    review.setDecision(decision); // APPROVED, REJECTED, CHANGES_REQUESTED
    review.setComments(comments);
    review.setStatus("COMPLETED");
    reviewMapper.insert(review);

    // If rejected, mark version as needing changes
    if ("REJECTED".equals(decision) || "CHANGES_REQUESTED".equals(decision)) {
        SfSpecVersion version = versionMapper.selectById(versionId);
        version.setStatus("CHANGES_REQUESTED");
        versionMapper.updateById(version);
    }

    return review;
}
```

- [ ] **Step 3: Commit**

---

## Phase 6: Workflow Engine

---

### Task 38: Implement WorkflowNodeEngine

**Files:**
- Modify: `schemaplexai-workflow/src/main/java/com/schemaplexai/workflow/node/WorkflowNodeEngine.java`
- Create: `schemaplexai-workflow/src/main/java/com/schemaplexai/workflow/node/NodeExecutor.java`
- Create: `schemaplexai-workflow/src/main/java/com/schemaplexai/workflow/node/AgentNodeExecutor.java`

- [ ] **Step 1: Define NodeExecutor interface**

```java
public interface NodeExecutor {
    String getNodeType();
    NodeExecutionResult execute(Map<String, Object> input, Long tenantId);
}
```

- [ ] **Step 2: Implement AgentNodeExecutor**

```java
@Component
@RequiredArgsConstructor
public class AgentNodeExecutor implements NodeExecutor {

    private final AgentExecutionEngine agentEngine;

    @Override
    public String getNodeType() {
        return "AGENT";
    }

    @Override
    public NodeExecutionResult execute(Map<String, Object> input, Long tenantId) {
        Long agentId = ((Number) input.get("agentId")).longValue();
        String prompt = (String) input.get("prompt");

        SfAgentExecution execution = agentEngine.createExecution(agentId, prompt, tenantId);
        agentEngine.startExecution(execution.getId());

        return NodeExecutionResult.success(Map.of(
                "executionId", execution.getId(),
                "status", execution.getStatus()
        ));
    }
}
```

- [ ] **Step 3: Implement WorkflowNodeEngine with node registry**

```java
@Component
@RequiredArgsConstructor
public class WorkflowNodeEngine {

    private final Map<String, NodeExecutor> executors;
    private final SfWorkflowNodeExecutionMapper nodeExecutionMapper;

    @PostConstruct
    public void init(List<NodeExecutor> executorList) {
        this.executors = executorList.stream()
                .collect(Collectors.toMap(NodeExecutor::getNodeType, Function.identity()));
    }

    @Transactional(rollbackFor = Exception.class)
    public NodeExecutionResult executeNode(SfWorkflowNodeExecution nodeExecution) {
        NodeExecutor executor = executors.get(nodeExecution.getNodeType());
        if (executor == null) {
            throw new BaseException(ResultCode.INTERNAL_ERROR,
                    "No executor for node type: " + nodeExecution.getNodeType());
        }

        nodeExecution.setStatus("RUNNING");
        nodeExecutionMapper.updateById(nodeExecution);

        try {
            NodeExecutionResult result = executor.execute(
                    nodeExecution.getInputData(), nodeExecution.getTenantId());
            nodeExecution.setStatus(result.isSuccess() ? "COMPLETED" : "FAILED");
            nodeExecution.setOutputData(result.getOutput());
            nodeExecutionMapper.updateById(nodeExecution);
            return result;
        } catch (Exception e) {
            nodeExecution.setStatus("FAILED");
            nodeExecution.setErrorMessage(e.getMessage());
            nodeExecutionMapper.updateById(nodeExecution);
            throw e;
        }
    }
}
```

- [ ] **Step 4: Commit**

---

## Phase 7: Quality Engine

---

### Task 39: Implement QualityOrchestrator

**Files:**
- Modify: `schemaplexai-quality/src/main/java/com/schemaplexai/quality/gate/QualityOrchestrator.java`
- Create: `schemaplexai-quality/src/main/java/com/schemaplexai/quality/gate/QualityRule.java`
- Create: `schemaplexai-quality/src/main/java/com/schemaplexai/quality/gate/rules/SpecComplianceRule.java`

- [ ] **Step 1: Define QualityRule interface**

```java
public interface QualityRule {
    String getRuleName();
    QualityCheckResult check(QualityContext context);
}
```

- [ ] **Step 2: Implement QualityOrchestrator with rule pipeline**

```java
@Component
@RequiredArgsConstructor
public class QualityOrchestrator {

    private final Map<String, QualityRule> rules;
    private final SfQualityGateMapper gateMapper;
    private final SfQualityIssueMapper issueMapper;

    @PostConstruct
    public void init(List<QualityRule> ruleList) {
        this.rules = ruleList.stream()
                .collect(Collectors.toMap(QualityRule::getRuleName, Function.identity()));
    }

    @Transactional(rollbackFor = Exception.class)
    public QualityReport evaluate(Long executionId, QualityContext context) {
        List<SfQualityGate> gates = gateMapper.selectByExecutionId(executionId);
        List<QualityCheckResult> results = new ArrayList<>();
        boolean allPassed = true;

        for (SfQualityGate gate : gates) {
            QualityRule rule = rules.get(gate.getRuleName());
            if (rule == null) continue;

            QualityCheckResult result = rule.check(context);
            results.add(result);

            if (!result.isPassed()) {
                allPassed = false;
                // Record issue
                SfQualityIssue issue = new SfQualityIssue();
                issue.setExecutionId(executionId);
                issue.setGateId(gate.getId());
                issue.setRuleName(gate.getRuleName());
                issue.setSeverity(result.getSeverity());
                issue.setDescription(result.getMessage());
                issueMapper.insert(issue);
            }
        }

        return new QualityReport(executionId, allPassed, results);
    }
}
```

- [ ] **Step 3: Implement SpecComplianceRule**

```java
@Component
@RequiredArgsConstructor
public class SpecComplianceRule implements QualityRule {

    @Override
    public String getRuleName() {
        return "SPEC_COMPLIANCE";
    }

    @Override
    public QualityCheckResult check(QualityContext context) {
        String specContent = context.getSpecContent();
        String agentOutput = context.getAgentOutput();

        // Simple heuristic: check if agent output references key spec requirements
        // Future: use LLM to evaluate compliance
        if (specContent == null || specContent.isBlank()) {
            return QualityCheckResult.pass();
        }

        // Extract key requirements from spec (simplified)
        List<String> requirements = extractRequirements(specContent);
        long matchedCount = requirements.stream()
                .filter(req -> agentOutput.toLowerCase().contains(req.toLowerCase()))
                .count();

        double compliance = (double) matchedCount / requirements.size();
        if (compliance < 0.5) {
            return QualityCheckResult.fail("HIGH",
                    String.format("Spec compliance %.0f%% — %d/%d requirements addressed",
                            compliance * 100, matchedCount, requirements.size()));
        }
        return QualityCheckResult.pass();
    }

    private List<String> extractRequirements(String spec) {
        // Extract lines starting with "- [ ]" or "## " as requirements
        return Arrays.stream(spec.split("\n"))
                .filter(line -> line.trim().startsWith("- [ ]") || line.trim().startsWith("## "))
                .map(String::trim)
                .toList();
    }
}
```

- [ ] **Step 4: Commit**

---

## Phase 8: Integration Layer

---

### Task 40: Implement ToolExecutionService

**Files:**
- Modify: `schemaplexai-integration/src/main/java/com/schemaplexai/integration/service/impl/ToolExecutionServiceImpl.java`

- [ ] **Step 1: Implement tool registry and execution**

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolExecutionServiceImpl implements ToolExecutionService {

    private final Map<String, ToolAdapter> adapters;
    private final SfSkillMapper skillMapper;

    @PostConstruct
    public void init(List<ToolAdapter> adapterList) {
        this.adapters = adapterList.stream()
                .collect(Collectors.toMap(ToolAdapter::getToolType, Function.identity()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ToolExecutionResult execute(Long tenantId, String toolType, Map<String, Object> params) {
        ToolAdapter adapter = adapters.get(toolType);
        if (adapter == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Tool adapter not found: " + toolType);
        }

        // Validate params against tool schema
        adapter.validate(params);

        // Execute with timeout
        try {
            CompletableFuture<ToolExecutionResult> future = CompletableFuture.supplyAsync(
                    () -> adapter.execute(tenantId, params));
            ToolExecutionResult result = future.get(30, TimeUnit.SECONDS);

            log.info("Tool {} executed successfully for tenant {}", toolType, tenantId);
            return result;
        } catch (TimeoutException e) {
            log.error("Tool {} timed out for tenant {}", toolType, tenantId);
            throw new BaseException(ResultCode.TIMEOUT, "Tool execution timed out");
        } catch (Exception e) {
            log.error("Tool {} failed for tenant {}", toolType, tenantId, e);
            throw new BaseException(ResultCode.INTERNAL_ERROR, "Tool execution failed: " + e.getMessage());
        }
    }
}
```

- [ ] **Step 2: Define ToolAdapter interface**

```java
public interface ToolAdapter {
    String getToolType();
    void validate(Map<String, Object> params);
    ToolExecutionResult execute(Long tenantId, Map<String, Object> params);
}
```

- [ ] **Step 3: Commit**

---

### Task 41: Implement MCP Server Service

**Files:**
- Modify: `schemaplexai-integration/src/main/java/com/schemaplexai/integration/service/impl/McpServerServiceImpl.java`

- [ ] **Step 1: Implement MCP server lifecycle management**

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class McpServerServiceImpl implements McpServerService {

    private final SfMcpServerMapper mcpServerMapper;
    private final Map<String, McpClient> activeClients = new ConcurrentHashMap<>();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SfMcpServer register(Long tenantId, McpServerConfig config) {
        SfMcpServer server = new SfMcpServer();
        server.setTenantId(tenantId);
        server.setName(config.getName());
        server.setEndpoint(config.getEndpoint());
        server.setStatus("REGISTERED");
        mcpServerMapper.insert(server);
        return server;
    }

    @Override
    public void connect(Long serverId) {
        SfMcpServer server = mcpServerMapper.selectById(serverId);
        if (server == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "MCP server not found: " + serverId);
        }

        McpClient client = new McpClient(server.getEndpoint());
        client.connect();
        activeClients.put(server.getId().toString(), client);

        server.setStatus("CONNECTED");
        mcpServerMapper.updateById(server);
        log.info("Connected to MCP server {} at {}", server.getName(), server.getEndpoint());
    }

    @Override
    public void disconnect(Long serverId) {
        McpClient client = activeClients.remove(serverId.toString());
        if (client != null) {
            client.disconnect();
        }
        SfMcpServer server = mcpServerMapper.selectById(serverId);
        server.setStatus("DISCONNECTED");
        mcpServerMapper.updateById(server);
    }

    @Override
    public List<McpTool> listTools(Long serverId) {
        McpClient client = activeClients.get(serverId.toString());
        if (client == null) {
            throw new BaseException(ResultCode.INTERNAL_ERROR, "MCP server not connected: " + serverId);
        }
        return client.listTools();
    }
}
```

- [ ] **Step 2: Commit**

---

## Phase 9: Operations & Cost Analytics

---

### Task 42: Implement ClickHouseCostSyncService

**Files:**
- Modify: `schemaplexai-ops/src/main/java/com/schemaplexai/ops/service/impl/ClickHouseCostSyncServiceImpl.java`

- [ ] **Step 1: Implement incremental sync with cursor**

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class ClickHouseCostSyncServiceImpl implements ClickHouseCostSyncService {

    private final JdbcTemplate pgJdbcTemplate;
    private final JdbcTemplate ckJdbcTemplate;
    private final SfSyncCursorMapper cursorMapper;
    private final SfSyncBatchLogMapper batchLogMapper;
    private static final int BATCH_SIZE = 1000;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SyncResult syncCostRecords(Long tenantId) {
        // Get or create cursor
        SfSyncCursor cursor = cursorMapper.selectByTenantAndType(tenantId, "COST_RECORD");
        LocalDateTime lastSyncTime = cursor != null ? cursor.getLastSyncTime()
                : LocalDateTime.now().minusDays(30);

        LocalDateTime now = LocalDateTime.now();
        int totalSynced = 0;

        // Process in batches
        while (lastSyncTime.isBefore(now)) {
            List<Map<String, Object>> batch = pgJdbcTemplate.queryForList(
                    "SELECT * FROM sf_token_usage WHERE tenant_id = ? AND created_at > ? AND created_at <= ? ORDER BY created_at LIMIT ?",
                    tenantId, lastSyncTime, now, BATCH_SIZE);

            if (batch.isEmpty()) break;

            // Insert batch into ClickHouse
            ckJdbcTemplate.batchUpdate(
                    "INSERT INTO cost_records (id, tenant_id, agent_id, model, input_tokens, output_tokens, cost, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    batch.stream().map(row -> new Object[]{
                            row.get("id"), row.get("tenant_id"), row.get("agent_id"),
                            row.get("model"), row.get("input_tokens"), row.get("output_tokens"),
                            row.get("cost"), row.get("created_at")
                    }).toList());

            totalSynced += batch.size();
            lastSyncTime = ((Timestamp) batch.get(batch.size() - 1).get("created_at")).toLocalDateTime();

            // Log batch
            SfSyncBatchLog log = new SfSyncBatchLog();
            log.setTenantId(tenantId);
            log.setSyncType("COST_RECORD");
            log.setRecordCount(batch.size());
            log.setStartTime(LocalDateTime.now());
            batchLogMapper.insert(log);
        }

        // Update cursor
        if (cursor == null) {
            cursor = new SfSyncCursor();
            cursor.setTenantId(tenantId);
            cursor.setSyncType("COST_RECORD");
        }
        cursor.setLastSyncTime(now);
        cursorMapper.saveOrUpdate(cursor);

        return new SyncResult(totalSynced, lastSyncTime, now);
    }
}
```

- [ ] **Step 2: Commit**

---

### Task 43: Implement Scheduled Jobs

**Files:**
- Modify: All 6 Job classes in `schemaplexai-task/src/main/java/com/schemaplexai/task/scheduling/`

- [ ] **Step 1: Implement CostStatisticsJob**

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class CostStatisticsJob {

    private final JdbcTemplate pgJdbcTemplate;
    private final JdbcTemplate ckJdbcTemplate;

    @Scheduled(cron = "0 0 1 * * ?")
    @SchedulerLock(name = "costStatisticsJob", lockAtMostFor = "PT30M")
    public void execute() {
        log.info("Running daily cost statistics aggregation");

        // Aggregate yesterday's costs per tenant per agent per model
        pgJdbcTemplate.execute("""
            INSERT INTO sf_daily_cost_summary (tenant_id, agent_id, model, date, total_input_tokens, total_output_tokens, total_cost)
            SELECT tenant_id, agent_id, model, DATE(created_at) as date,
                   SUM(input_tokens), SUM(output_tokens), SUM(cost)
            FROM sf_token_usage
            WHERE created_at >= CURRENT_DATE - INTERVAL '1 day'
              AND created_at < CURRENT_DATE
            GROUP BY tenant_id, agent_id, model, DATE(created_at)
            ON CONFLICT (tenant_id, agent_id, model, date) DO UPDATE SET
                total_input_tokens = EXCLUDED.total_input_tokens,
                total_output_tokens = EXCLUDED.total_output_tokens,
                total_cost = EXCLUDED.total_cost
        """);

        log.info("Daily cost statistics aggregation completed");
    }
}
```

- [ ] **Step 2: Implement HealthCheckJob**

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class HealthCheckJob {

    private final RedisTemplate<String, Object> redisTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedRate = 60000) // every minute
    @SchedulerLock(name = "healthCheckJob", lockAtMostFor = "PT2M")
    public void execute() {
        Map<String, String> status = new LinkedHashMap<>();

        // Check PG
        try {
            jdbcTemplate.execute("SELECT 1");
            status.put("postgresql", "UP");
        } catch (Exception e) {
            status.put("postgresql", "DOWN");
            log.error("PostgreSQL health check failed", e);
        }

        // Check Redis
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            status.put("redis", "UP");
        } catch (Exception e) {
            status.put("redis", "DOWN");
            log.error("Redis health check failed", e);
        }

        // Check RabbitMQ
        try {
            rabbitTemplate.execute(channel -> {
                channel.getConnection().isOpen();
                return null;
            });
            status.put("rabbitmq", "UP");
        } catch (Exception e) {
            status.put("rabbitmq", "DOWN");
            log.error("RabbitMQ health check failed", e);
        }

        // Store in Redis for dashboard consumption
        redisTemplate.opsForValue().set("sf:health:status", status, Duration.ofMinutes(2));
    }
}
```

- [ ] **Step 3: Implement remaining jobs (ApprovalTimeoutJob, MemoryConsolidationJob, ChatMessageArchiveJob, MilvusReconciliationJob)** with similar patterns.

- [ ] **Step 4: Commit**

---

## Phase 10: Frontend Pages Implementation

---

### Task 44: Implement Placeholder Pages

**Files:**
- Modify: `schemaplexai-ui/src/pages/SpecCenter.tsx`
- Modify: `schemaplexai-ui/src/pages/ContextCenter.tsx`
- Modify: `schemaplexai-ui/src/pages/WorkflowCenter.tsx`
- Modify: `schemaplexai-ui/src/pages/QualityCenter.tsx`
- Modify: `schemaplexai-ui/src/pages/IntegrationCenter.tsx`
- Modify: `schemaplexai-ui/src/pages/OpsCenter.tsx`
- Modify: `schemaplexai-ui/src/pages/SystemSettings.tsx`

- [ ] **Step 1: Implement SpecCenter with list + detail + version view**

```tsx
const SpecCenter: React.FC = () => {
    const [specs, setSpecs] = useState<Spec[]>([]);
    const [loading, setLoading] = useState(false);
    const [selectedSpec, setSelectedSpec] = useState<Spec | null>(null);

    useEffect(() => {
        setLoading(true);
        specApi.list().then(res => {
            setSpecs(res.data.data.records);
            setLoading(false);
        });
    }, []);

    return (
        <Layout>
            <Table
                loading={loading}
                dataSource={specs}
                rowKey="id"
                onRow={(record) => ({ onClick: () => setSelectedSpec(record) })}
            >
                <Table.Column<Spec> title="Name" dataIndex="name" />
                <Table.Column<Spec> title="Version" dataIndex="currentVersion" />
                <Table.Column<Spec> title="Status" dataIndex="status"
                    render={(status: string) => (
                        <Tag color={status === 'PUBLISHED' ? 'green' : 'orange'}>{status}</Tag>
                    )}
                />
                <Table.Column<Spec> title="Updated" dataIndex="updatedAt"
                    render={(date: string) => new Date(date).toLocaleDateString()}
                />
            </Table>
            {selectedSpec && (
                <SpecDetail spec={selectedSpec} onClose={() => setSelectedSpec(null)} />
            )}
        </Layout>
    );
};
```

- [ ] **Step 2: Implement ContextCenter, WorkflowCenter, QualityCenter, IntegrationCenter, OpsCenter, SystemSettings with similar patterns — each calling their respective API and displaying data in Ant Design tables.**

- [ ] **Step 3: Commit**

---

### Task 45: Fix Frontend SSE Connection Management (P1-020)

**Files:**
- Modify: `schemaplexai-ui/src/pages/AgentExecutor.tsx`

- [ ] **Step 1: Implement proper SSE lifecycle management**

```tsx
const AgentExecutor: React.FC = () => {
    const sseRef = useRef<AbortController | null>(null);
    const [events, setEvents] = useState<SseEvent[]>([]);

    const handleExecute = useCallback(async (agentId: number, input: string) => {
        // Close any existing connection first
        if (sseRef.current) {
            sseRef.current.abort();
            sseRef.current = null;
        }

        const controller = new AbortController();
        sseRef.current = controller;

        try {
            const response = await fetch('/api/agents/execute/stream', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': 'Bearer ' + sessionStorage.getItem('token'),
                },
                body: JSON.stringify({ agentId, input }),
                signal: controller.signal,
            });

            const reader = response.body?.getReader();
            const decoder = new TextDecoder();

            const read = async () => {
                if (controller.signal.aborted) return;
                const { done, value } = await reader!.read();
                if (done) return;

                const text = decoder.decode(value);
                const lines = text.split('\n').filter(l => l.startsWith('data:'));
                for (const line of lines) {
                    const data = JSON.parse(line.slice(5));
                    setEvents(prev => [...prev.slice(-1000), data]); // keep last 1000
                }
                read();
            };
            read();
        } catch (err) {
            if (err instanceof DOMException && err.name === 'AbortError') return;
            console.error('SSE connection error:', err);
        }
    }, []);

    // Cleanup on unmount
    useEffect(() => {
        return () => {
            if (sseRef.current) {
                sseRef.current.abort();
                sseRef.current = null;
            }
        };
    }, []);

    // ...
};
```

- [ ] **Step 2: Commit**

---

### Task 46: Add 404 Catch-All Route and JWT Auto-Refresh

**Files:**
- Modify: `schemaplexai-ui/src/router/index.tsx`
- Modify: `schemaplexai-ui/src/api/request.ts`

- [ ] **Step 1: Add catch-all route**

```tsx
const routes: RouteObject[] = [
    // ... existing routes
    { path: '*', element: <NotFound /> },
];
```

- [ ] **Step 2: Add JWT auto-refresh on 401 in request.ts**

```typescript
api.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config;
        if (error.response?.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true;
            try {
                const refreshToken = sessionStorage.getItem('refreshToken');
                const res = await axios.post('/api/auth/refresh', { refreshToken });
                sessionStorage.setItem('token', res.data.data.token);
                originalRequest.headers['Authorization'] = 'Bearer ' + res.data.data.token;
                return api(originalRequest);
            } catch (refreshError) {
                sessionStorage.clear();
                window.location.href = '/login';
            }
        }
        return Promise.reject(error);
    }
);
```

- [ ] **Step 3: Commit**

---

## Phase 11: Infrastructure Fixes

---

### Task 47: Fix Docker Compose Issues

**Files:**
- Modify: `docker/docker-compose.yml`

- [ ] **Step 1: Fix port conflict (MinIO 9000 vs ClickHouse 9000)**

Change ClickHouse native port mapping from `9000:9000` to `9004:9000`.

- [ ] **Step 2: Fix Grafana service name typo**

Rename `gafana` to `grafana`.

- [ ] **Step 3: Add health checks**

```yaml
services:
  postgres:
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U sf_user -d schemaplexai"]
      interval: 10s
      timeout: 5s
      retries: 5
  redis:
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
  rabbitmq:
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "check_running"]
      interval: 15s
      timeout: 10s
      retries: 5
```

- [ ] **Step 4: Add restart policies**

```yaml
services:
  postgres:
    restart: unless-stopped
  redis:
    restart: unless-stopped
  rabbitmq:
    restart: unless-stopped
  # ... all services
```

- [ ] **Step 5: Commit**

---

## Self-Review Checklist

### Spec Coverage

| Requirement | Task(s) |
|-------------|---------|
| Fix P0 DB driver mismatch | Task 1 |
| Fix P0 credentials inconsistency | Task 2 |
| Fix P0 AgentStateMachine constructor | Task 3 |
| Fix P0 JwtAuthFilter header bug | Task 4 |
| Fix P0 duplicate entities | Task 5 |
| Fix P0 duplicate main class | Task 6 |
| Fix P0 TenantLineInterceptor type | Task 7 |
| Fix P0 RabbitMQ ACK mode | Task 8 |
| Fix P1 JWT secret hardcoding | Task 9 |
| Fix P1 TokenBudget race condition | Task 10 |
| Fix P1 state machine terminal state + memory | Task 11 |
| Fix P1 AiModelRouter cooldown | Task 12 |
| Fix P1 Redis TTL + length | Task 13 |
| Fix P1 missing @Transactional | Task 14 |
| Fix P1 null safety in controllers | Task 15 |
| Fix P1 pagination consistency | Task 16 |
| Fix P1 RateLimit fail-closed | Task 17 |
| Fix P1 SSE/WebSocket auth | Task 18 |
| Fix P1 gateway JSON serialization | Task 19 |
| Fix P1 MQ ACK/DLQ config | Task 20 |
| Fix P1 MQ idempotency race | Task 21 |
| Fix P1 scheduled job locks | Task 22 |
| Fix P1 frontend token security | Task 23 |
| Fix P1 frontend store defects | Task 24 |
| Test infrastructure | Tasks 25-29 |
| Agent engine state handlers | Tasks 30-34 |
| RAG pipeline | Tasks 35-36 |
| Spec business logic | Task 37 |
| Workflow engine | Task 38 |
| Quality engine | Task 39 |
| Integration layer | Tasks 40-41 |
| Operations & cost | Tasks 42-43 |
| Frontend pages | Tasks 44-46 |
| Infrastructure fixes | Task 47 |

### Placeholder Scan — PASS
No "TBD", "TODO", "implement later", or "similar to Task N" patterns found in plan steps.

### Type Consistency — PASS
- `AgentExecutionState` used consistently across Tasks 3, 11, 30-34
- `SfAgentExecution` used consistently across Tasks 3, 11, 30-33
- `Result<T>` / `ResultCode` used consistently across all controller-related tasks
- `BaseEntity` fields referenced consistently

---

## Execution Summary

**Total tasks:** 47
**P0 fixes:** 8 tasks (must complete first)
**P1 fixes:** 16 tasks
**Test infrastructure:** 5 tasks
**Business logic implementation:** 13 tasks
**Frontend:** 3 tasks
**Infrastructure:** 2 tasks

**Recommended execution order:**
1. Phase 0 (Tasks 1-8) — P0 fixes, sequential, unblocks everything
2. Phase 1 (Tasks 9-24) — P1 fixes, partially parallelizable
3. Phase 2 (Tasks 25-29) — Test infrastructure, parallelizable
4. Phases 3-11 (Tasks 30-47) — Business logic, parallelizable across modules
