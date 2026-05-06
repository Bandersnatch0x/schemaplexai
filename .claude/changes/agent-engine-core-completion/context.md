---
change_id: agent-engine-core-completion
updated_at: 2026-05-04
---

# Context: Agent Engine Core Completion

## Related Modules

| 模块 | 关系 | 影响 |
|------|------|------|
| `schemaplexai-agent-engine` | 核心变更 | 新增 ~12 个 Java 文件，修改 ~6 个现有文件 |
| `schemaplexai-model` | 新增实体 | `TenantEnvironmentConfig.java` → `.entity.config` 包 |
| `schemaplexai-dao` | 新增 Mapper + 修改拦截器 | `TenantEnvironmentConfigMapper.java` + `TenantLineInterceptor.java` |
| `schemaplexai-common` | 无变更 | — |
| `schemaplexai-web` | 无变更 | — |

## Key Files to Modify

### 新增文件 (13)

| 文件 | 模块 | 完整路径 |
|------|------|----------|
| ToolRegistry.java | agent-engine | `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/tool/registry/ToolRegistry.java` |
| ToolAdapter.java | agent-engine | `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/tool/adapter/ToolAdapter.java` |
| ToolCallParser.java | agent-engine | `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/tool/parser/ToolCallParser.java` |
| OpenAiToolCallParser.java | agent-engine | `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/tool/parser/OpenAiToolCallParser.java` |
| AnthropicToolCallParser.java | agent-engine | `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/tool/parser/AnthropicToolCallParser.java` |
| FileReadAdapter.java | agent-engine | `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/tool/adapter/file/FileReadAdapter.java` |
| HttpCallAdapter.java | agent-engine | `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/tool/adapter/http/HttpCallAdapter.java` |
| RetryingStateHandler.java | agent-engine | `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/state/RetryingStateHandler.java` |
| ResumingStateHandler.java | agent-engine | `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/state/ResumingStateHandler.java` |
| ToolExecutionMetricsBinder.java | agent-engine | `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/metrics/ToolExecutionMetricsBinder.java` |
| SecurityPolicyLoader.java | agent-engine | `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/config/SecurityPolicyLoader.java` |
| TenantEnvironmentConfig.java | model | `schemaplexai-model/src/main/java/com/schemaplexai/model/entity/config/TenantEnvironmentConfig.java` |
| TenantEnvironmentConfigMapper.java | dao | `schemaplexai-dao/src/main/java/com/schemaplexai/dao/mapper/TenantEnvironmentConfigMapper.java` |

### 修改文件 (6)

| 文件 | 修改内容 |
|------|----------|
| `ToolCallingStateHandler.java` | 重构 parseToolCalls() → ToolRegistry.parse()；executeToolStub() → ToolAdapter.execute()；注入 ToolRegistry + AgentLoopDetectionService + ToolExecutionRecorder |
| `PausedStateHandler.java` | 添加 AgentExecutionLifecycleService.createSnapshot() 调用；持久化快照到 SfAgentExecutionSnapshotMapper |
| `GateBlockedStateHandler.java` | 添加 AdmissionResult 反馈 + 重试倒计时(60s) + AgentExecutionEventPublisher 事件发布 |
| `ThinkingStateHandler.java` | 注入 AgentLoopDetectionService；transition(TOOL_CALLING) 前调用 detectLoop() |
| `ToolErrorCategory.java` | 添加 `securityRelated`/`retryable` 字段 + 3 个新枚举值 (IRREVERSIBLE_OPERATION, ENVIRONMENT_MISMATCH, UNEXPECTED_ENVIRONMENT) |
| `TenantLineInterceptor.java` | ignoreTable() 添加 "sf_tenant_environment_config" |

## Decision Log

| # | 决策 | 日期 | 理由 |
|---|------|------|------|
| 1 | ToolRegistry 位于 ToolSandbox 上游 | 2026-05-04 | toolSandbox=沙箱安全, ToolRegistry=注册/解析, 职责分离 |
| 2 | TenantEnvironmentConfig 为全局表 | 2026-05-04 | 跨租户配置, 类似 sf_tenant, 排除 TenantLineInterceptor |
| 3 | ToolCallParser 统一抽象策略 | 2026-05-04 | OpenAI JSON + Anthropic XML 两套实现，统一接口可扩展 |
| 4 | AgentLoopDetectionService 独立 Service | 2026-05-04 | 被 Thinking + ToolCalling 共享，单一职责 |
| 5 | Metrics 使用内存计数器而非 DB 轮询 | 2026-05-04 | 避免 IO 影响 P99 延迟，AtomicLong 原子递增 |
| 6 | RetryingStateHandler 仅重放失败工具调用 | 2026-05-04 | Token 成本：200-500 vs 5000-20000 tokens |
| 7 | SecurityPolicyLoader Caffeine Cache 5min TTL | 2026-05-04 | 降低 DB 查询频率，接受 5 分钟最终一致性 |

## Dependencies (No New)

pom.xml 无需变更。以下依赖已存在：
- `micrometer-registry-prometheus` (行19-22)
- `caffeine` (行16)
- `langchain4j` + `langchain4j-open-ai` (行14-15)
- `spring-boot-starter-amqp` (行13) — 用于 AgentBlockedEvent

## Environment Checks Before Apply

- JAVA_HOME: `JDK 21` (已确认 project-progress.md)
- Target module pom.xml includes `schemaplexai-dao` (行10) and required starters
- All tests currently in `schemaplexai-agent-engine/src/test/`
