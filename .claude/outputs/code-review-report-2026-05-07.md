# SchemaPlexAI 项目架构与已实现代码审查报告

- **审查日期**: 2026-05-07
- **审查 commit**: master @ f121ecd（含未提交的 5 个 modified + 25+ untracked 改动）
- **审查范围**: 16 个 Maven 模块 / 网关与多租户 / agent-engine 核心 / context RAG-Milvus / 测试覆盖
- **审查方式**: Claude 直读源码（codex 后端因模型侧 tool-call JSON 格式 bug 连续 3 次崩出后回退）

---

## 总体结论

| 维度 | 评分 | 备注 |
|---|---|---|
| 架构合理性 | **6.5 / 10** | 模块拆分合理，多租户与网关思路清晰；但当前实现里仍存在大量 mock 占位（embedding/RAG/extract）与状态机进程级单点，距离"可上线"还差关键改造 |
| 代码质量 | **5.5 / 10** | 基础类（BaseEntity/BaseMapperX/BaseController）干净；但**全项目无全局异常处理器**导致 Result 信封形同虚设，多个 Critical 级 bug（事务回滚抹掉失败状态、随机向量做查询、租户头注入）需先修 |
| 是否建议合并当前 git status 改动 | **❌ 部分修复后再合** | 尤其 `MilvusSyncServiceImpl`/`RagSearchServiceImpl`/`AgentStateMachine` 的 Critical 项必须先修 |

> **后续 3 个最重要演进方向**
> 1. **把所有 mock 占位换成真实实现或显式禁用**（embedding mock 默认值、RAG 随机向量、extractText mock）；上线前增加 Profile 启动期硬校验
> 2. **补齐全局异常处理器 + BaseException(cause)**，把 Result 信封落到错误路径上
> 3. **agent-engine 的进程级状态/事件总线下沉到 Redis Stream / DB**，否则多副本部署直接破功

---

# 🔴 Critical（阻塞合并，必须先修）

## C-1. RagSearchServiceImpl 用随机向量做查询，整条 RAG 链路在产品线上等于无效
**文件**: `schemaplexai-context/src/main/java/com/schemaplexai/context/service/impl/RagSearchServiceImpl.java:42-89`

```java
List<Float> queryEmbedding = generateSimulatedEmbedding();  // line 43

private List<Float> generateSimulatedEmbedding() {
    List<Float> embedding = new ArrayList<>(EMBEDDING_DIMENSION);
    for (int i = 0; i < EMBEDDING_DIMENSION; i++) {
        embedding.add(RANDOM.nextFloat());                   // line 86
    }
    return embedding;
}
```

**风险**: 任何走 Milvus topK 检索的业务（agent RAG、知识检索）拿到的都是**随机近邻**，与查询语义完全不相关。线上没有任何报错——用户只会感觉"AI 答非所问"。`@ConditionalOnProperty(milvus.enabled=true)` 还不会生效到 fallback 上。

**修复**:
1. 注入 `EmbeddingService` 复用真实 embedding；
2. embed 失败必须抛出而不是回退到 RANDOM；
3. 删除 `generateSimulatedEmbedding` 方法。

```java
@RequiredArgsConstructor
public class RagSearchServiceImpl implements RagSearchService {
    private final MilvusClientV2 milvusClient;
    private final MilvusProperties milvusProperties;
    private final EmbeddingService embeddingService;   // 注入

    public List<KnowledgeChunk> search(String query, String tenantId, int topK) {
        if (query == null || query.isBlank()) return List.of();
        float[] qEmb = embeddingService.embed(query);  // 失败抛异常
        FloatVec vec = new FloatVec(toBoxed(qEmb));
        // ...
    }
}
```

---

## C-2. MilvusSyncServiceImpl 的 catch+update+throw 写法被事务回滚抹掉，FAILED 永远落不下去
**文件**: `schemaplexai-context/src/main/java/com/schemaplexai/context/service/impl/MilvusSyncServiceImpl.java:73-79`（结合类上 `@Transactional(rollbackFor = Exception.class)`）

```java
@Transactional(rollbackFor = Exception.class)
public class MilvusSyncServiceImpl implements MilvusSyncService {
    public void syncToMilvus(Long docId) {
        try {
            ...
            doc.setStatus("SYNCED");
            knowledgeDocMapper.updateById(doc);
        } catch (Exception e) {
            log.error(...);
            doc.setStatus("FAILED");
            knowledgeDocMapper.updateById(doc);   // <- 这条 update 也在事务里
            throw new BaseException(...);          // <- throw 触发整个事务回滚
        }
    }
}
```

**风险**: `throw` 触发 `@Transactional` rollback，刚刚写下去的 `FAILED` 状态会跟着被回滚，doc 永远停留在 `UPLOADED/PENDING`。**failed 路径不可观测**，靠这个状态做重试/告警的下游逻辑全部失效。

**修复**: 把 FAILED 状态写入提取到一个 `REQUIRES_NEW` 子事务里，或者在事务方法外面用编程式 TX 写状态。

```java
@Service
@RequiredArgsConstructor
public class MilvusSyncServiceImpl implements MilvusSyncService {
    private final FailedStatusWriter failedStatusWriter; // 单独类，REQUIRES_NEW

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncToMilvus(Long docId) {
        try {
            ... // happy path
        } catch (Exception e) {
            failedStatusWriter.markFailed(docId, e.getMessage()); // 子事务独立提交
            throw new BaseException(ResultCode.INTERNAL_ERROR,
                    "Milvus sync failed for doc: " + docId, e);
        }
    }
}

@Component
@RequiredArgsConstructor
class FailedStatusWriter {
    private final SfKnowledgeDocMapper mapper;
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markFailed(Long docId, String reason) {
        SfKnowledgeDoc d = mapper.selectById(docId);
        if (d == null) return;
        d.setStatus("FAILED");
        mapper.updateById(d);
    }
}
```

---

## C-3. JwtAuthFilter 在 JWT 无 tenantId claim 时不剥离客户端原始 X-Tenant-Id Header，构成租户假冒漏洞
**文件**: `schemaplexai-gateway/src/main/java/com/schemaplexai/gateway/filter/JwtAuthFilter.java:78-90`

```java
String tenantId = claims.get("tenantId", String.class);
ServerHttpRequest.Builder builder = request.mutate()
    .header(CommonConstants.HEADER_AUTHORIZATION, ...)
    .header("X-User-Id", userId);
if (StringUtils.hasText(tenantId)) {
    builder.header(CommonConstants.HEADER_TENANT_ID, tenantId);  // 仅在有 claim 时覆盖
}
```

**风险**: 如果一个用户的 JWT 里没有 tenantId claim（比如平台管理员 / 跨租户用户 / 旧 token），客户端原本伪造的 `X-Tenant-Id: 别人的租户` Header 会**原样透传到下游**，下游 `TenantLineInterceptor` 会照此自动拼 SQL，**直接绕过多租户隔离**。这是任何多租户系统的核心安全边界。

**修复**: **无条件**先移除入站 Header，再仅在 JWT 校验通过且有 tenantId 时设置。

```java
ServerHttpRequest.Builder builder = request.mutate()
    .headers(h -> h.remove(CommonConstants.HEADER_TENANT_ID))   // 先剥
    .header(CommonConstants.HEADER_AUTHORIZATION, CommonConstants.TOKEN_PREFIX + token)
    .header("X-User-Id", userId);
if (StringUtils.hasText(tenantId)) {
    builder.header(CommonConstants.HEADER_TENANT_ID, tenantId);
}
// 同时把白名单（/auth/**）的请求里的 X-Tenant-Id 也剥掉，避免登录前注入
```

另一处同样问题：白名单路径直接 `chain.filter(exchange)` 透传（line 64-66），**也没有剥离客户端 X-Tenant-Id**——攻击者可以携带 `X-Tenant-Id: victim` 调用任何白名单接口，下游若误信此 Header 就会出问题。

---

## C-4. 全项目无 `@RestControllerAdvice`/全局异常处理器，BaseException 的 code/message 永远到不了客户端
**审查证据**: `grep -r "@RestControllerAdvice\|GlobalExceptionHandler"` → 0 个文件命中

**风险**:
- 所有 Service 里 `throw new BaseException(ResultCode.NOT_FOUND, "...")` 最终走 Spring 默认 `/error` 路径，返回 500 + Whitelabel 页面或简单 JSON `{timestamp, status, error, path}`，**完全不是 `Result<T>` 信封**
- 前端 axios interceptor（CLAUDE.md 提到的 401 自动刷新等）会因 `code` 字段缺失而走错分支
- 异常栈信息可能直接泄漏到响应体（Spring 默认错误页含 trace）

**修复**: 在 `schemaplexai-common` 加一个全局异常处理器并被各服务的 `WebMvcConfigurer` 自动扫描到（包路径 `com.schemaplexai.common.exception`，确保 Spring Boot 服务的 ComponentScan 覆盖到）。

```java
package com.schemaplexai.common.exception;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(BaseException.class)
    public Result<Void> handleBase(BaseException e) {
        log.warn("Business exception: code={}, msg={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ":" + fe.getDefaultMessage())
            .collect(Collectors.joining(","));
        return Result.error(ResultCode.PARAM_ERROR.getCode(), msg);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleAll(Exception e) {
        log.error("Unhandled exception", e);
        return Result.error(ResultCode.INTERNAL_ERROR.getCode(), "Internal error");
        // 注意：不要把 e.getMessage() 直接返回给客户端，避免泄漏
    }
}
```

---

## C-5. AgentStateMachine 的 FAILED handler 异常会无限递归直到栈溢出
**文件**: `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/state/AgentStateMachine.java:53-65`

```java
} catch (Exception e) {
    log.error("State handler error for state {} execution {}", newState, execution.getId(), e);
    if (executionStates.get(execution.getId()) != null) {       // 永远 != null（刚 put 完）
        transition(AgentExecutionState.FAILED, execution);       // <- 递归
    }
    return;
}
```

**风险**:
- `executionStates.get(...)` 在 `transition` 入口刚刚把当前 state put 进去，恒非 null，守卫无效
- 如果 `FAILED` 的 handler 也抛异常，再次走到这里，又递归 `transition(FAILED, ...)` —— **StackOverflowError 之前线程堆栈无限增长**
- 即便 FAILED handler 不抛，也能多次 publish 到 EventBus，前端会重复收到 transition 事件

**修复**: 在递归前判断当前 state 是否就是 FAILED，如果是直接放弃。

```java
} catch (Exception e) {
    log.error("State handler error for state {} execution {}", newState, execution.getId(), e);
    if (newState != AgentExecutionState.FAILED) {                // 关键：避免 FAILED 自己的 handler 再触发
        transition(AgentExecutionState.FAILED, execution);
    } else {
        log.error("FAILED handler also threw for execution {}, giving up", execution.getId(), e);
        // 直接走 terminal 清理路径
        eventBus.publishExecutionCompleted(execution.getId(), AgentExecutionState.FAILED.name());
        eventBus.complete(String.valueOf(execution.getId()));
        removeExecution(execution.getId());
    }
    return;
}
```

---

## C-6. EmbeddingServiceImpl 默认 provider=mock，且无 @Primary 也无生产 Profile 校验，线上极易"沉默回退"
**文件**: `schemaplexai-context/src/main/java/com/schemaplexai/context/service/impl/EmbeddingServiceImpl.java:33,39,79-89`

```java
@Service
@ConditionalOnMissingBean(EmbeddingService.class)            // line 33
public class EmbeddingServiceImpl implements EmbeddingService {
    @Value("${embedding.provider:mock}")                      // line 39 - 默认 mock
    private String provider;

    private float[] embedMock(String text) {                  // line 79
        long seed = computeHashSeed(text);
        Random random = new Random(seed);
        ...
    }
}
```

**风险**: 
- 当 `embedding.provider` 在配置里没显式设置时，自动用基于 SHA-256 seed 的伪 embedding。**生产部署只要漏了一行 yaml 就静默退化**，搜索结果完全失真，但日志只在启动时 INFO 打一行 `provider: mock`，运维大概率漏看
- 与 `RagSearchServiceImpl` 的随机向量叠加，构成"双重 mock"——RAG 完全是噪声

**修复**:
1. 默认值改为不可用占位，强制配置：`@Value("${embedding.provider}")` 不给默认；
2. 启动时如果 provider=mock 且 active profile 含 `prod`，直接抛错；
3. 或者把 mock 实现拆成独立类 `MockEmbeddingService`，仅 `@Profile({"dev","test"})` 下生效。

```java
@Service
@Profile({"dev", "test"})           // 生产环境绝不加载
public class MockEmbeddingService implements EmbeddingService { ... }

@Service
@Profile("!dev & !test")            // 生产唯一可选
@RequiredArgsConstructor
public class OpenAiEmbeddingService implements EmbeddingService { ... }
```

---

# 🟠 Major（强烈建议合并前修复）

## M-1. KnowledgeDocServiceImpl 在 `@Transactional` 内同步调 Milvus，跨系统一致性窗口很大
**文件**: `schemaplexai-context/src/main/java/com/schemaplexai/context/service/impl/KnowledgeDocServiceImpl.java:24-31`

```java
@Transactional(rollbackFor = Exception.class)
public void uploadAndVectorize(SfKnowledgeDoc doc) {
    doc.setStatus("UPLOADED");
    save(doc);
    log.info(...);
    milvusSyncService.syncToMilvus(doc.getId());   // 同步外部系统在事务内
}
```

**风险**: 
- Milvus 调用阻塞期间，PG 事务持有行锁
- Milvus 写成功但 PG 提交失败 → Milvus 里有"幽灵 chunk"，PG 里没有 doc
- `syncToMilvus` 自身又是 `@Transactional` —— Spring 的事务传播是默认 REQUIRED，会嵌套在外层事务里，C-2 提到的子事务方案需要 REQUIRES_NEW 才生效

**修复**: 解耦
- 同步保存 doc + 状态 = `UPLOADED`
- 用 MQ 或 Spring `ApplicationEventPublisher` 异步触发 sync（commit 后），结合幂等校验

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void uploadAndVectorize(SfKnowledgeDoc doc) {
    doc.setStatus("UPLOADED");
    save(doc);
    // 仅发事件，不直接调
    eventPublisher.publishEvent(new KnowledgeDocUploadedEvent(doc.getId()));
}

@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onUploaded(KnowledgeDocUploadedEvent event) {
    milvusSyncService.syncToMilvus(event.docId());
}
```

---

## M-2. CompositeChatMemoryStore.computeNextTurnIndex 并发条件下产生重复 turnIndex
**文件**: `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/memory/CompositeChatMemoryStore.java:62-96`

```java
public void saveMessage(String conversationId, LlmMessage message) {
    Integer turnIndex = computeNextTurnIndex(conversationId);   // SELECT MAX
    SfChatMessage entity = new SfChatMessage();
    entity.setTurnIndex(turnIndex);
    chatMessageMapper.insert(entity);                           // INSERT
    ...
}
```

**风险**: 
- 两个并发的 saveMessage 在同一个 conversationId 上同时 SELECT，都拿到 N，都 INSERT turnIndex=N+1 → 历史顺序错乱（同回合两条不同消息共享同一个 index）
- 当前代码看不到对 `(conversation_id, turn_index)` 的唯一约束兜底

**修复（任选其一）**:
1. **DB 层加唯一索引** `UNIQUE (conversation_id, turn_index)`，并在 insert 失败时重试 + 重新 SELECT MAX；
2. 改用 PostgreSQL **sequence per conversation** 或行级锁 `SELECT ... FOR UPDATE`；
3. 直接用 `id ASC` 排序而不依赖 turn_index（如果只关心顺序，turn_index 可以从 `created_at` 或自增 id 推导）；
4. 业务层用 `synchronized(conversationId.intern())` 串行化（小流量可接受）。

## M-3. CompositeChatMemoryStore L1 miss 反填 Redis 没有锁，并发场景下 Redis list 里出现重复消息
**文件**: 同上 `:38-58`

```java
List<LlmMessage> messages = redisTemplate.opsForList().range(key, 0, -1);
if (messages != null && !messages.isEmpty()) { return ...; }
List<SfChatMessage> dbMessages = chatMessageMapper.selectList(...);
...
redisTemplate.opsForList().rightPushAll(key, ...);   // 没有 lock
redisTemplate.expire(key, CHAT_HISTORY_TTL);
```

**风险**: 两个并发 reader 同时 miss → 都从 PG 读 → 都 rightPushAll → Redis list 内容翻倍。

**修复**: 用 `SETNX` 加临时 lock，或者改用 `LPUSH` + `LTRIM` + `RENAME` 的 swap 模式：先写到 `${key}.tmp` 再原子 RENAME。

## M-4. ExecutionEventBus 进程级 emitter Map + 同步广播，多副本部署直接破功 + 慢客户端阻塞所有人
**文件**: `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/sse/ExecutionEventBus.java:24,82-94`

```java
private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();   // 进程级
...
for (SseEmitter emitter : list) {
    try { emitter.send(event); }                                                     // 同步循环
    catch (IOException e) { ... }
}
```

**风险**:
- **多副本部署**: 用户 SSE 连到 Replica A，Agent 在 Replica B 上执行 → A 的 emitters 里没有 executionId，事件丢失。
- **慢客户端**: `emitter.send` 是阻塞的（HTTP 写阻塞），一个慢客户端会把广播线程拖住，影响其他订阅者。
- emitter 写失败时 `unregister(...)` 是在循环中调用 `list.remove(emitter)`——`CopyOnWriteArrayList` 安全但每次 remove 都 O(n) 复制。

**修复**:
- 多副本：用 Redis Pub/Sub 或 Redis Stream 做事件分发，每个 Replica 订阅 channel `execution:{id}`，本地广播给本节点连接
- 慢客户端：每个 emitter 用独立的 `Executor.execute(() -> emitter.send(event))`，加 `try-finally` 兜底
- 限制单 executionId 最大订阅数（防 DoS）

## M-5. AgentStateMachine 状态流转无事务保护，DB 与 SSE 与 in-memory map 三处可能错位
**文件**: `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/state/AgentStateMachine.java:46-72`

```java
execution.setState(newState.name());
saveExecution(execution);                       // 1. DB 写
executionStates.put(execution.getId(), newState); // 2. 内存 map
eventBus.publishStateTransition(...);            // 3. SSE 推送（同步阻塞）
handler.handle(this, execution);                 // 4. 业务 handler 可能再 transition
```

**风险**: 
- DB 写成功但 SSE 推送前 JVM 崩溃 → 客户端永远看不到 transition 通知（SSE 不会重发）
- handler 内部递归 transition 时，外层尚未完成的 publishStateTransition 已经发出去了 → 前端可能收到 `INITIALIZING → READY → INITIALIZING` 这种诡异序列

**修复**:
- 状态变更入库 + 写一条事件到 `outbox` 表（同事务）
- 单独的 outbox poller 异步推送到 EventBus（保证 at-least-once）
- 这是经典的 transactional outbox pattern

## M-6. AgentStateMachine 的 executionStates 是进程内 ConcurrentHashMap，多副本/重启失忆
**文件**: 同上 `:23`

```java
private final Map<Long, AgentExecutionState> executionStates = new ConcurrentHashMap<>();
```

**风险**:
- `getCurrentState` 在 Replica A 调用，但执行实际在 Replica B → null
- JVM 重启后所有 in-flight 的执行的 in-memory 状态丢失，但 DB 里还停在中间态（INITIALIZING/THINKING）
- 没有"启动时扫 DB 重建 in-memory map"的 hook

**修复**: 把状态完全下沉到 DB，`getCurrentState` 改为查 DB（带短 TTL Redis cache），完全去掉 `executionStates` map。

## M-7. JacksonConfig 用 `@Primary` 替换 Spring Boot 默认 ObjectMapper，丢失自动配置能力
**文件**: `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/config/JacksonConfig.java:13-20`

```java
@Bean @Primary
public ObjectMapper agentEngineObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    return mapper;
}
```

**风险**:
- Spring Boot 默认通过 `Jackson2ObjectMapperBuilder` 自动注册若干 module（含 `JavaTimeModule`）+ 应用 `spring.jackson.*` 配置
- 这个手写 mapper 跳过了所有 `spring.jackson.*` 项（如 `default-property-inclusion`、`time-zone`、`fail-on-unknown-properties`）
- 注册 `@Primary` 还会顶替默认 `MappingJackson2HttpMessageConverter` 的 mapper —— Knife4j、actuator 等的序列化都改用此 mapper

**修复**:
- 删除该 Bean，改用 `application.yml` 的 `spring.jackson.*` 配置；
- 或注入 `Jackson2ObjectMapperBuilder` 复用默认基线：
```java
@Bean @Primary
public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
    return builder.build();   // 已含 JavaTimeModule + ISO format
}
```

## M-8. RateLimitFilter 在反向代理后无法识别真实客户端 IP，匿名流量共享一个限流桶
**文件**: `schemaplexai-gateway/src/main/java/com/schemaplexai/gateway/filter/RateLimitFilter.java:64-69`

```java
String ip = request.getRemoteAddress() != null
        ? request.getRemoteAddress().getAddress().getHostAddress()
        : "unknown";
return "ip:" + ip;
```

**风险**: 部署在 Nginx/ALB/Cloudfront 之后，所有匿名请求 (`/auth/login` 等白名单) 的 `getRemoteAddress()` 都是代理 IP，等于把全网用户共享一个 100 req/min 桶。**登录接口本身就成了 DoS 入口**。

**修复**:
- 配置 Spring Cloud Gateway 的 `forwarded-headers-strategy: framework`
- `resolveClientId` 优先读 `X-Forwarded-For`（取第一个 IP）/`X-Real-IP`，仅在受信代理列表里才信任这些 Header

## M-9. RateLimitFilter Redis INCR + EXPIRE 非原子，TTL 丢失风险
**文件**: 同上 `:39-47`

```java
return reactiveStringRedisTemplate.opsForValue()
    .increment(key)
    .flatMap(count -> {
        if (count == 1) {
            return reactiveStringRedisTemplate.expire(key, WINDOW).thenReturn(count);
        }
        return Mono.just(count);
    })
```

**风险**: 高并发下两个 INCR 操作之间可能都看到 count==1 并发起 EXPIRE（多余但无害）；更严重的是，如果某个分支看到 `count == 1` 但 EXPIRE 操作失败/Redis 闪断，key 就永远不过期，除非 Redis 自己 evict。

**修复**: 用 Lua 脚本一次完成 INCR + 条件 EXPIRE；或用 Redis 7 的 `SET key 1 EX 60 NX` + `INCR` 模式。

```lua
-- 原子限流
local v = redis.call('INCR', KEYS[1])
if v == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end
return v
```

## M-10. MilvusSyncServiceImpl 的 `simulateExtractText` 是占位 mock，所有文档都拿到一样的假内容
**文件**: `schemaplexai-context/src/main/java/com/schemaplexai/context/service/impl/MilvusSyncServiceImpl.java:82-94`

```java
private String simulateExtractText(SfKnowledgeDoc doc) {
    log.info("Simulating text extraction for: {}", doc.getFileName());
    StringBuilder sb = new StringBuilder();
    sb.append("Document: ").append(doc.getTitle()).append("\n");
    sb.append("This is simulated content for testing purposes. ");
    ...
    for (int i = 0; i < 30; i++) {
        sb.append("Paragraph ").append(i + 1)
          .append(" contains enough text to test the document chunking pipeline thoroughly. ");
    }
    return sb.toString();
}
```

**风险**:
- 所有 doc 进 Milvus 的内容**只随 title 变化**，30 段固定文本——从向量空间看几乎完全相同
- 即便 RAG 查询向量是真实 embedding，召回结果也无意义

**修复**: 集成 Apache Tika（已经在 pom 候选里）或 LangChain4j 的 `DocumentParser`，并在 `embedding.provider=mock` 时**也禁用本地 mock**，改抛 `BaseException` 强制配置：

```java
private final DocumentParser documentParser;  // tika-based

private String extractText(SfKnowledgeDoc doc) {
    try (InputStream is = openInputStream(doc.getFileUrl())) {
        return documentParser.parse(is);
    } catch (Exception e) {
        throw new BaseException(ResultCode.INTERNAL_ERROR,
            "Failed to extract text from " + doc.getFileUrl(), e);
    }
}
```

## M-11. RagSearchServiceImpl 的 Milvus filter 字符串拼接，存在 expr 注入
**文件**: `schemaplexai-context/src/main/java/com/schemaplexai/context/service/impl/RagSearchServiceImpl.java:51-53`

```java
if (tenantId != null && !tenantId.isBlank()) {
    searchBuilder.filter("tenant_id == '" + tenantId + "'");
}
```

**风险**:
- `tenantId` 来自 X-Tenant-Id Header（结合 C-3 已知该值在某些场景下可被客户端注入）
- 攻击者传 `' or '1'='1` 之类，整个 filter 失效，跨租户检索别人的向量数据

**修复**: 校验 tenantId 格式（仅允许数字+短横线/UUID），并对 `'` 转义；更安全的做法是用 Milvus partition key 隔离租户而不是 filter。

```java
if (tenantId == null || !TENANT_ID_PATTERN.matcher(tenantId).matches()) {
    throw new BaseException(ResultCode.PARAM_ERROR, "Invalid tenantId");
}
searchBuilder.filter(String.format("tenant_id == \"%s\"", tenantId.replace("\"", "\\\"")));
```

## M-12. BaseException 缺少 `(code, message, Throwable cause)` 构造器，原始堆栈丢失
**文件**: `schemaplexai-common/src/main/java/com/schemaplexai/common/exception/BaseException.java:7-30`

```java
public class BaseException extends RuntimeException {
    private final Integer code;
    public BaseException(String message) { ... }
    public BaseException(ResultCode resultCode) { ... }
    public BaseException(ResultCode resultCode, String message) { ... }
    public BaseException(Integer code, String message) { ... }
    // ❌ 没有任何接收 cause 的构造器
}
```

**风险**: 各 Service 的 catch 块 `throw new BaseException(ResultCode.INTERNAL_ERROR, "...")` —— 原始 e 的 stack 完全丢失，排查困难。

**修复**: 补 cause 构造器并改 super 调用。

```java
public BaseException(ResultCode rc, String message, Throwable cause) {
    super(message, cause);
    this.code = rc.getCode();
}
public BaseException(ResultCode rc, Throwable cause) {
    super(rc.getMessage(), cause);
    this.code = rc.getCode();
}
```

---

# 🟡 Minor（合并后跟进）

## m-1. JwtAuthFilter 的 `whiteList` 包含 `/system/tenants/**`，把整个租户管理接口暴露成无鉴权
**文件**: `schemaplexai-gateway/src/main/java/com/schemaplexai/gateway/filter/JwtAuthFilter.java:50-57`

`/system/tenants/**` 通常包括"创建租户/删除租户/查询租户配额"等管理动作。如果设计上是给"未登录的注册流程"用，应该收窄到具体子路径如 `/system/tenants/register` 或 `/system/tenants/lookup-by-domain`，而不是整个目录树。

## m-2. TenantResolveFilter 包含死代码（line 24 读取永远不存在的 attribute）
**文件**: `schemaplexai-gateway/src/main/java/com/schemaplexai/gateway/filter/TenantResolveFilter.java:24`

```java
String tokenTenantId = exchange.getAttribute("tenantId");  // 没有任何上游 filter 会 set 这个 attribute
```

JwtAuthFilter 只往 mutate 后的 request 里加了 X-Tenant-Id Header，从未调用 `exchange.getAttributes().put("tenantId", ...)`。建议直接删掉 line 23-28 的 fallback 块，或者修复 JwtAuthFilter 让两边一致。

## m-3. AgentStateMachine 的 transition 在 handler 失败的 catch 路径里**先**清掉 `executionStates`，再递归 transition(FAILED)
**文件**: 同 C-5

实际代码 line 58 是 `if (executionStates.get(...) != null)`，意图似乎是想检测"已被 removeExecution 清理"但逻辑上 put 在 line 48 之后永远非 null。把"避免重复 FAILED 递归"和"已终结清理"两件事用同一个 map 键来表示是混淆的。建议引入显式 `boolean alreadyFailed` flag 或者 sentinel state。

## m-4. RedisConfig 没配 Hash 序列化器，将来用到 opsForHash 会出现 JdkSerializationRedisSerializer 默认值
**文件**: `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/config/RedisConfig.java:13-29`

当前 `CompositeChatMemoryStore` 只用 `opsForList`，无 immediate bug，但未来加 `opsForHash` 会写出 JDK 序列化的 binary blob，redis-cli 看不懂。建议补全：

```java
template.setHashKeySerializer(new StringRedisSerializer());
template.setHashValueSerializer(valueSerializer);
```

## m-5. `MilvusCollectionInitializer` 混用 v1 / v2 SDK 包
**文件**: `schemaplexai-context/src/main/java/com/schemaplexai/context/milvus/MilvusCollectionInitializer.java:9,146-149`

```java
import io.milvus.v2.service.index.request.CreateIndexReq;          // v2
...
indexParamBuilder.extraParams(io.milvus.param.IndexExtraParam.builder() // v1, deprecated
    ...
```

milvus-sdk-java 2.x 已经把核心 API 迁到 `io.milvus.v2.*`，`io.milvus.param.*` 是兼容层。混用会在升级时踩坑。建议统一到 v2 的 `Map<String, Object>` extraParams 写法。

## m-6. JwtAuthFilter 每次失败响应都 `new ObjectMapper()`，浪费且与 JacksonConfig 不一致
**文件**: `schemaplexai-gateway/src/main/java/com/schemaplexai/gateway/filter/JwtAuthFilter.java:121`

```java
byte[] bytes = new ObjectMapper().writeValueAsBytes(body);  // 每次新建
```

注入 `ObjectMapper` 替换。

## m-7. MilvusSyncServiceImpl InsertReq 列序与 Milvus collection schema 字段顺序耦合
**文件**: `schemaplexai-context/src/main/java/com/schemaplexai/context/service/impl/MilvusSyncServiceImpl.java:127-130`

```java
InsertReq insertReq = InsertReq.builder()
    .collectionName(collectionName)
    .data(List.of(ids, docIds, chunkIndexes, contents, embeddingLists, tenantIds, createdAts))
    .build();
```

`data(List.of(...))` 的列序必须严格匹配 collection schema 字段定义顺序，schema 一旦字段重排或新增就静默写错位。Milvus v2 SDK 支持 `Map<String, Object>` 或 `List<JsonObject>` 形式，按字段名注入更安全。

## m-8. `chunks.size()` vs `embeddings.size()` 没校验
**文件**: 同上 `:110-125`

如果 `embedBatch` 因为重试/分页返回长度不匹配，line 119 `embeddings.get(i)` 直接 IndexOutOfBoundsException。补一行：

```java
if (chunks.size() != embeddings.size()) {
    throw new BaseException(ResultCode.INTERNAL_ERROR,
        "Embedding count mismatch: chunks=" + chunks.size() + ", embeddings=" + embeddings.size());
}
```

## m-9. CompositeChatMemoryStore.clear 用 LambdaQueryWrapper.delete 走的是 MyBatis-Plus 逻辑删除（deleted=1），但 saveMessage 取 turnIndex 时没排除 deleted 记录
**文件**: `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/memory/CompositeChatMemoryStore.java:88-95,98-106`

`computeNextTurnIndex` 的 `selectOne(LambdaQueryWrapper)` 默认会被全局 `@TableLogic` 自动加 `deleted=0`。所以 clear 之后再 saveMessage，turnIndex 会从 0 重新开始，**软删的历史回不来了**——这要么是 by design，要么需要明确文档。

## m-10. AgentStateMachine handler 调用顺序：先 publish SSE 再 invoke handler
**文件**: 同 C-5 line 50-55

如果 handler 里立即触发了下一次 transition，前端会先收到 `OUTER → NEW`，再收到 `NEW → INNER` —— 顺序正确，但中间 publish 与 handler 是同步的，handler 执行慢会让 SSE 推送也慢。可以异步化 publish。

## m-11. 大量字符串字面量状态码（"UPLOADED" / "PENDING" / "FAILED" / "SYNCED"）
**文件**: `MilvusSyncServiceImpl:48-49,69,75`、`KnowledgeDocServiceImpl:27`

应改成 enum：

```java
public enum DocStatus {
    UPLOADED, PENDING, SYNCED, FAILED;
}
```

## m-12. 硬编码 1536 维 embedding
**文件**: `RagSearchServiceImpl:29`、`EmbeddingServiceImpl:37`

不同 provider 维度不同（openai text-embedding-3-small 是 1536，bge-m3 是 1024，nomic-embed-text 是 768）。需要从配置读，或者在启动时校验真实 provider 输出维度匹配 Milvus collection schema。

## m-13. NoOpMilvusSyncServiceImpl 的 `@ConditionalOnMissingBean` 与 Real impl 的 `@ConditionalOnProperty` 组合较为脆弱
**文件**: `schemaplexai-context/src/main/java/com/schemaplexai/context/service/impl/NoOpMilvusSyncServiceImpl.java`

建议改为对称写法 `@ConditionalOnProperty(name="milvus.enabled", havingValue="false", matchIfMissing=true)`，避免与第三方 EmbeddingService 实现共注册时冲突。

---

# 🔵 Suggestion（设计/演进建议）

## S-1. agent-engine 状态机 + EventBus 整体迁移到 Redis Stream
现状：`AgentStateMachine` 进程级 + `ExecutionEventBus` 进程级 SSE 路由。两者都阻碍水平扩展。

建议方案：
- 状态机产出的每个事件写到 Redis Stream `agent:exec:{id}`；
- 每个 Replica 维护一个本地 SSE Hub，订阅自己持有连接的 executionId 的 Stream；
- 状态查询走 DB（实时） + Redis cache（短 TTL）；
- 终态写一条 `terminal` 事件并截断 stream。

收益：多副本部署即可、SSE 不丢事件（stream 可以 replay）、用户重连 SSE 时能 catch up 历史事件。

## S-2. 引入「事务性 Outbox」模式统一事件发布
当前 `KnowledgeDocServiceImpl`、`AgentStateMachine` 都有"DB 写 + 外部副作用"耦合。建议统一抽象 `OutboxEventPublisher`：
- DB 同事务写一条 outbox 记录
- 后台 poller 定时扫描未发送项 → 推 MQ / Redis Stream / SSE
- 标记已发送

可显著降低跨系统不一致风险。

## S-3. 引入 `@Profile` + `@ConditionalOnProperty` 矩阵，把所有 mock 实现关进 dev/test
本次审查发现的多个 mock 占位（embedding mock、extractText simulate、random embedding for query）若不被 Profile 隔离，长期就会偷跑到生产。建议建立项目级约定：

```java
@Service
@Profile("dev | test")
public class MockEmbeddingService { ... }

@Service
@Profile("staging | prod")
public class OpenAiEmbeddingService { ... }
```

并在 `application.yml` 增加启动校验：

```java
@PostConstruct
void validateProductionConfig() {
    if (Arrays.asList(env.getActiveProfiles()).contains("prod")
        && embeddingProvider.equals("mock")) {
        throw new IllegalStateException("Mock embedding provider is not allowed in production");
    }
}
```

## S-4. 测试覆盖补齐
- **空白模块**: `admin`（空）、`ops`（实测有部分 service test 但缺 controller/integration 测试）、`quality`、`spec`（部分）、`workflow`（仅 template 测试，缺 BPMN engine 集成测试）、`integration`（GitHub/GitLab/Jenkins/MCP 完全无测试）、`task`（MQ 消费者无 contract 测试）
- **租户隔离断言**: 项目里有 `TenantIsolationFlowTest`，建议把它扩展为所有 Service 的 baseline test —— 用 Testcontainers 起 PG，验证 tenant A 创建的数据 tenant B 查不到
- **外部系统集成测试**:
  - Milvus: 用 Testcontainers `milvusio/milvus:standalone-2.3.5`
  - Redis: `Testcontainers Redis Module`
  - MQ: `RabbitMQContainer`
  - 集成测试单独跑（`@Tag("integration")`）防慢化主测试集

## S-5. 全面替换硬编码字符串为 Enum
- DocStatus（UPLOADED/PENDING/SYNCED/FAILED）
- AgentExecutionState 已经是 enum，但 SfAgentExecution.state 字段是 `String` —— 改为 `@TableField(typeHandler = ...)` 直接持久化 enum
- HTTP 状态码 / Header 名称（`X-Tenant-Id` 已有 CommonConstants，但 `X-User-Id` 等还在裸字符串）

## S-6. JWT secret 的运维硬度可再提升
- `validateJwtSecret` 检查 ≥ 32 字节 ✔️
- 但 secret 通常应该用 Asymmetric（RS256/ES256）防止网关密钥泄漏导致全栈被签发
- 至少加一层 `kid` (key id) header，方便密钥轮换

## S-7. 前端 SSE 消费需要适配多副本
当前 SSE endpoint 假设 `executionId` 注册到当前节点的 emitter map。多副本部署后前端连接到任意 replica 都要能收到事件。配合 S-1 的 Redis Stream 方案，前端可使用 `Last-Event-ID` 头实现断线重连续传。

## S-8. 把 `Result<T>` 错误信封下沉为 Spring Boot 自动配置
当前每个 Controller 类都需要 extends `BaseController` 才能 `success()/error()`。可以：
- 把 GlobalExceptionHandler（C-4 修复）放进 `schemaplexai-common`
- 加一个 `Result.fromException(BaseException)` 静态工厂
- Controller 不必继承 BaseController 也能拿到统一信封

## S-9. 网关 filter 顺序补全 traceId / requestId 注入
当前 -100/-90/-50 三层 filter 没有任何 traceId 注入，多服务间 log 关联困难。建议在 -110 补一个 `TraceIdFilter`，从 Header 读取或新生成 UUID，注入到 MDC + 透传到下游 Header。

## S-10. CompositeChatMemoryStore 若要支撑高 QPS，turnIndex 应去掉
- 用 `id ASC`（雪花 id 自增有序）作为顺序键，免去 SELECT MAX
- 或者完全不维护 turnIndex，按 created_at 排序

---

# 附：未审查到但需要后续关注的清单

- `schemaplexai-workflow` 的 Flowable BPMN 与 AI 节点 Delegate（`AiAgentExecutionDelegate` 等）—— 涉及编排正确性
- `schemaplexai-integration` 的 GitHub/GitLab/Jenkins/MCP webhook 接入与签名校验
- `schemaplexai-ops` 的费用结算逻辑（`BudgetServiceImpl`、`CostAnalyticsServiceImpl`）
- `schemaplexai-quality` 的 drift detection 与 security scan
- `schemaplexai-task` 的 MQ 消费者幂等性
- 前端 axios 401 自动刷新逻辑是否处理 race（多个并发请求同时 401）
- `TenantContextHolder` 在 `@Async` / Reactor 异步上下文下是否正确传播

---

# 总结

当前提交里已经定型的基础设施（Gateway 三层 filter、BaseEntity/BaseMapperX/BaseController、TenantLineInterceptor、Redis L1+PG L2 dual cache 思路）方向是正确的，但**正在改的几个文件里同时存在 6 个 Critical 级缺陷**（事务回滚抹掉失败状态、随机查询向量、租户头注入、无全局异常处理、状态机栈溢出递归、mock embedding 静默回退）。其中前 3 个会在生产线上**直接造成业务功能失效或数据安全事故**，后 3 个会在故障路径上**让排错和恢复变得极其困难**。

**强烈建议**先把 Critical 全部修复并补单测后再合并；Major 中至少 M-1/M-2/M-3 也应一并修，因为它们涉及并发数据正确性。Minor/Suggestion 可分批跟进。
