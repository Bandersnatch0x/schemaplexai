# SchemaPlexAI 性能优化报告

- **审查日期**: 2026-05-07
- **审查范围**: 后端核心模块性能分析（Agent Engine, Context, Gateway, Workflow）
- **技术栈**: Spring Boot 3.2.5, Java 21, MyBatis-Plus 3.5.5, Redis, Milvus, RabbitMQ

---

## 总体评估

| 维度 | 评分 | 备注 |
|------|------|------|
| 数据库查询 | 6/10 | 基础 CRUD 规范，但存在 N+1 和缺索引风险 |
| 缓存策略 | 5/10 | Redis 已集成，但使用不充分 |
| 并发安全 | 4/10 | 多处共享可变状态，竞态条件风险高 |
| 连接管理 | 7/10 | HikariCP 默认配置，RestTemplate 有超时 |
| 内存管理 | 6/10 | 无明显泄漏，但进程级 Map 有增长风险 |

---

## CRITICAL — 影响可用性

### P-001: ExecutionEventBus 进程级 SSE，多副本部署事件丢失
- **位置**: `schemaplexai-agent-engine/.../ExecutionEventBus.java:24`
- **描述**: `ConcurrentHashMap<String, List<SseEmitter>>` 进程级存储，用户 SSE 连到 Replica A，Agent 在 Replica B 执行 → 事件丢失
- **影响**: 多副本部署时 SSE 通知完全失效
- **建议**: 迁移到 Redis Pub/Sub 或 Redis Stream 做事件分发
- **优先级**: 多副本部署前必须修复

### P-002: AgentStateMachine 进程级状态 Map，重启失忆
- **位置**: `schemaplexai-agent-engine/.../AgentStateMachine.java:23`
- **描述**: `ConcurrentHashMap<Long, AgentExecutionState>` 进程级，JVM 重启后所有 in-flight 执行状态丢失
- **影响**: 重启后 DB 停在中间态（INITIALIZING/THINKING），无法恢复
- **建议**: 状态完全下沉到 DB + 短 TTL Redis cache，去掉 executionStates Map
- **优先级**: 生产部署前必须修复

---

## HIGH — 影响性能

### P-003: CompositeChatMemoryStore 并发 turnIndex 重复
- **位置**: `schemaplexai-agent-engine/.../CompositeChatMemoryStore.java:62-96`
- **描述**: `SELECT MAX(turn_index) + INSERT` 非原子，并发写入导致重复 turnIndex
- **影响**: 聊天历史顺序错乱
- **建议**: DB 层加 `UNIQUE (conversation_id, turn_index)` 唯一约束 + 重试，或改用 `id ASC` 排序
- **优先级**: 合并前修复

### P-004: Redis L1 cache 回填无锁，重复数据
- **位置**: `schemaplexai-agent-engine/.../CompositeChatMemoryStore.java:38-58`
- **描述**: 两个并发 reader 同时 miss → 都从 PG 读 → 都 rightPushAll → Redis list 内容翻倍
- **影响**: 内存浪费 + 历史消息重复
- **建议**: SETNX 临时锁，或 LPUSH + LTRIM + RENAME swap 模式
- **优先级**: 合并前修复

### P-005: MilvusSyncServiceImpl 同步调外部系统在事务内
- **位置**: `schemaplexai-context/.../KnowledgeDocServiceImpl.java:24-31`
- **描述**: `@Transactional` 内同步调 Milvus，阻塞期间 PG 持有行锁
- **影响**: 高并发下数据库连接池耗尽
- **建议**: 解耦 — 保存 doc 后用 `@TransactionalEventListener(AFTER_COMMIT)` 异步触发 sync
- **优先级**: 合并后 Sprint 1

### P-006: ExecutionEventBus 同步广播，慢客户端阻塞所有人
- **位置**: `schemaplexai-agent-engine/.../ExecutionEventBus.java:82-94`
- **描述**: `emitter.send(event)` 阻塞循环，一个慢客户端拖住所有订阅者
- **影响**: SSE 广播延迟随客户端数线性增长
- **建议**: 每个 emitter 用独立线程池异步发送
- **优先级**: 合并后 Sprint 1

---

## MEDIUM — 可优化

### P-007: RateLimitFilter Redis INCR + EXPIRE 非原子
- **位置**: `schemaplexai-gateway/.../RateLimitFilter.java:39-47`
- **描述**: INCR 和 EXPIRE 分两步，EXPIRE 失败则 key 永不过期
- **建议**: 用 Lua 脚本原子完成 INCR + 条件 EXPIRE
- **优先级**: 合并后 Sprint 1

### P-008: JacksonConfig @Primary 覆盖默认 ObjectMapper
- **位置**: `schemaplexai-agent-engine/.../JacksonConfig.java:13-20`
- **描述**: 手写 ObjectMapper 跳过 Spring Boot 自动配置的 module 和 `spring.jackson.*` 配置
- **建议**: 删除该 Bean，改用 `application.yml` 的 `spring.jackson.*` 配置
- **优先级**: 合并后跟进

### P-009: EmbeddingServiceImpl embedBatch 串行执行
- **位置**: `schemaplexai-context/.../EmbeddingServiceImpl.java:252-258`
- **描述**: `embedBatch` 逐个调用 `embed()`，无并行/批量优化
- **影响**: 大文档 chunk 批量 embedding 时延迟线性增长
- **建议**: OpenAI API 支持 batch input，一次请求处理多个文本
- **优先级**: 合并后 Sprint 2

### P-010: MilvusSyncServiceImpl simulateExtractText 占位 mock
- **位置**: `schemaplexai-context/.../MilvusSyncServiceImpl.java:179-192`
- **描述**: 所有 doc 进 Milvus 的内容只随 title 变化，30 段固定文本
- **影响**: 向量空间中所有文档几乎相同，RAG 召回无意义
- **建议**: 集成 Apache Tika 或 LangChain4j DocumentParser
- **优先级**: 合并后 Sprint 1

---

## LOW — 建议

### P-011: 硬编码 1536 维 embedding
- **位置**: `RagSearchServiceImpl:29`, `EmbeddingServiceImpl:47`
- **描述**: 不同 provider 维度不同（openai=1536, bge-m3=1024, nomic=768）
- **建议**: 从配置读取维度，启动时校验与 Milvus collection schema 匹配

### P-012: RedisConfig 缺少 Hash 序列化器
- **位置**: `schemaplexai-agent-engine/.../RedisConfig.java:13-29`
- **描述**: 当前只用 `opsForList`，未来 `opsForHash` 会用 JDK 序列化
- **建议**: 补全 HashKey/HashValue 序列化器配置

---

## 优化路线图

### Phase 1 — 并发安全（合并前）
- [ ] P-003: turnIndex 唯一约束
- [ ] P-004: Redis cache 回填锁

### Phase 2 — 架构解耦（Sprint 1）
- [ ] P-001: Redis Pub/Sub SSE 分发
- [ ] P-002: 状态下沉 DB
- [ ] P-005: 事务解耦
- [ ] P-006: 异步 SSE 广播
- [ ] P-010: 真实文本提取

### Phase 3 — 性能优化（Sprint 2）
- [ ] P-007: Lua 限流脚本
- [ ] P-009: 批量 embedding
- [ ] P-011: 动态维度配置
- [ ] P-012: Redis 序列化补全

---

## 总结

项目当前为单副本部署模式，基础性能尚可。主要风险集中在**并发安全**（turnIndex 竞态、Redis cache 回填）和**多副本扩展**（进程级状态/SSE）。建议在生产部署前优先解决 P-001 和 P-002，合并前修复 P-003 和 P-004。
