# Agent Engine 补充核查 — Spec-to-Code 合规报告

- 核查日期: 2026-08-29
- 核查范围: `schemaplexai-agent-engine/src/main/java`（及执行点所在的 web / model / dao / common、application.yml、docker/postgres/init）
- 依据规格:
  - A = `docs/specs/agent-engine-core-completion.md` (status: approved)
  - B = `docs/specs/agent-engine-agentic-gaps-2026-05-07.md` (status: implemented)
  - C = `docs/specs/agent-engine-p1-p2-p4-batch-2026-05-08.md` (status: implemented)
- 工作树状态说明: `AgentExecutionLifecycleService.java`、`AgentRuntimeOrchestrator.java` 及其测试存在未提交修改（将暂停/恢复 Redis Key 改为租户作用域 `TenantRedisKeyResolver.executionPaused(...)`）。本报告按当前工作树代码核查；所引用行号均为工作树当前内容。

## 裁决汇总

| 裁决 | 数量 | REQ |
|---|---|---|
| implemented | 17 | 01, 03, 04, 06, 10, 11, 13, 14, 16, 17, 18, 19, 21, 23, 24, 25, 26 |
| partial | 7 | 02, 07, 08, 12, 15, 20, 22 |
| contradicted | 2 | 05, 09 |
| stronger-than-spec | 0 | （REQ-11 含超出规格的枚举值，记入 implemented 注记） |
| absent | 0 | — |
| undecidable | 0 | （见 notChecked） |

---

## REQ-01 — ToolRegistry + ToolAdapter 接口体系 + FileRead/HttpCall 适配器（来源: A）

> "ToolRegistry + ToolAdapter 接口体系 + FileReadAdapter, HttpCallAdapter" — A §1.2 In Scope；"ToolAdapter 接口：`ToolResult execute(ToolCall call, ExecutionContext ctx)`，FileReadAdapter 和 HttpCallAdapter 为首批实现" — A §2.1 关键设计决策

**Verdict:** implemented · confidence: high

**What this demands:** 存在工具注册中心（注册/发现/解析）、统一适配器接口及两个具体适配器，且被状态机消费。

**Where enforcement lives:**
- `tool/registry/ToolRegistry.java:27-125` — @Component，ConcurrentHashMap 存 adapters/parsers，`register()` L49、`resolve()` L61、`isRegistered()` L68、`parse()` L80。
- `tool/adapter/ToolAdapter.java:12-29` — 接口签名与规格逐字一致。
- `tool/adapter/file/FileReadAdapter.java:29-95`（toolName=`file_read` L33）、`tool/adapter/http/HttpCallAdapter.java:40-293`（toolName=`http_call` L63）均 @Component，经构造注入自动注册（`ToolRegistry.java:35-44`）。
- 消费方: `state/ToolCallingStateHandler.java:14`（import registry.ToolRegistry）、`:209`（resolve）。

**Paths walked:**
- ✓ 适配器自动发现（构造注入 `List<ToolAdapter>`）
- ✓ resolve 命中/未命中两分支
- ✓ 调用链 ToolCallingStateHandler → ToolRegistry.resolve()（与 A §2.1 一致）

**How the verdict was reached:** 注意工程内存在两个同名类型：`tool/ToolRegistry.java`（接口，面向 ToolDefinition，供 ReActPromptTemplate/ThinkingStateHandler 提示词渲染）与 `tool/registry/ToolRegistry.java`（本规格所指的具体注册中心）。ToolCallingStateHandler 显式导入后者（L14），无歧义。组件与接口签名完全符合规格。

---

## REQ-02 — 结构化工具调用解析（OpenAI tool_calls JSON + Anthropic tool_use XML）并集成到 TOOL_CALLING（来源: A）

> "结构化工具调用解析：OpenAI `tool_calls` JSON + Anthropic `tool_use` XML" — A §1.2；数据流 "b. ToolRegistry.parse(message.content) → List<ToolCall>" — A §2.2

**Verdict:** partial · confidence: high · **severity: Critical**

**What this demands:** 两个解析器存在且正确，并且状态机在 TOOL_CALLING 能实际解析出工具调用。

**Where enforcement lives:**
- `tool/parser/OpenAiToolCallParser.java:23-85`（getProviderName="OPENAI" L28-30，解析 `tool_calls[].function.name/arguments`，支持 arguments 为对象或 JSON 字符串两形态）。
- `tool/parser/AnthropicToolCallParser.java:23-72`（getProviderName="ANTHROPIC" L33-35，正则解析 `<tool_use>/<name>/<parameter>`）。
- `tool/registry/ToolRegistry.java:80-110`（按 provider 名路由解析器，白名单过滤未注册工具）。
- 集成点: `state/ToolCallingStateHandler.java:95` — `toolRegistry.parse(lastMessage.getContent(), null)`。

**Paths walked:**
- ✓ 解析器单元行为（代码审读：两种格式均正确实现，异常时返回空列表）
- ✗ 状态机路径: provider 传 `null` → `ToolRegistry.java:85` 计算 `providerName="GENERIC"` → L86 `parsers.get("GENERIC")` 为 null（全库仅注册 "OPENAI"/"ANTHROPIC" 两个解析器，见 Searched）→ L88-91 记 warn 并返回空列表 → `ToolCallingStateHandler.java:96-101` 判空 → 直接回 THINKING。**任何消息内容都无法解析出工具调用。**
- ✗ 后果链: ThinkingStateHandler 用启发式 `containsToolCalls()`（`ThinkingStateHandler.java:468-480`）探测到 "tool_calls"/"<tool_use>" 等子串后转入 TOOL_CALLING，但 TOOL_CALLING 永远解析为空 → 回 THINKING 再调 LLM → 相同响应哈希重复 ≥3 次触发循环检测（`AgentLoopDetectionService.java:50-53`）→ GATE_BLOCKED。即 LLM 一旦试图调工具，最终结局是被循环检测拦截，工具永远不会经状态机执行。

**Searched:**
- `implements ToolCallParser` → 2 命中（OpenAi/Anthropic 两实现，无 "GENERIC" 解析器）
- `GENERIC` in agent-engine/src/main → 1 命中（`tool/registry/ToolRegistry.java:85`，仅兜底常量）
- `toolRegistry.parse(` → 1 个生产调用点（`ToolCallingStateHandler.java:95`，provider 恒为 null）
- 测试侧 `integration/ToolExecutionFlowTest.java:34`、`tool/registry/ToolRegistryComponentTest.java:36` 均用 mock parser 注入，掩盖了该缺陷。

**How the verdict was reached:** 判 partial 而非 contradicted：解析器本身按规格实现且质量良好；缺陷在集成接线（恒空路由）。判 partial 而非 implemented：规格 §2.2 的数据流在唯一生产调用点上不可达，该规格的核心目标（替换启发式解析）实际未生效。此缺陷使 REQ-03/REQ-05 的执行路径一并失效。

---

## REQ-03 — TOOL_CALLING 内逐个执行：resolve→SafetyGuard.check→adapter.execute→recorder.record；白名单外 → INVALID_ARGUMENT（来源: A）

> "工具白名单：ToolRegistry.resolve() 返回 null → INVALID_ARGUMENT" — A §6；数据流 e 子步骤 — A §2.2

**Verdict:** implemented · confidence: high（附可达性注记）

**What this demands:** 每个 ToolCall 依次经过白名单解析、安全检查、适配器执行、审计记录；未注册工具返回 INVALID_ARGUMENT。

**Where enforcement lives:** `state/ToolCallingStateHandler.java`
- resolve + 白名单: L209-214（adapter==null → `ToolExecutionResult.failure(..., ToolErrorCategory.INVALID_ARGUMENT, "Tool not registered: ...")`）
- 租户策略加载: L217-221（`securityPolicyLoader.load(...)`）
- 安全检查: L224-230（`safetyGuard.check(name, args, environment)`，blocked → `ToolExecutionResult.blocked`）
- 执行: L233-242（`adapter.execute(toolCall, ctx)`，ctx 含 tenantId/executionId/workspaceRoot L235-239）
- 记录: L156（`executionRecorder.record(execution.getId(), result)`）
- 结果分流: L158-178（失败可重试→RETRYING、不可重试→FAILED、blocked→GATE_BLOCKED）、成功写入会话记忆 L183-184。

**Paths walked:**
- ✓ 白名单未注册 → INVALID_ARGUMENT
- ✓ safety blocked → GATE_BLOCKED
- ✓ ToolExecutionException → 按 errorCategory 分类；其他异常 → INTERNAL_ERROR（L243-251）
- ⚠ 可达性: 上游解析（REQ-02）恒返回空，本循环在生产中当前不会被进入；但分支逻辑本身完整正确。

**How the verdict was reached:** 代码逐条满足规格子步骤；不判 partial 因为本 REQ 的执行点逻辑无缺失，上游断裂已单独记在 REQ-02。

---

## REQ-04 — RetryingStateHandler：指数退避 + 最大重试 + 熔断器（来源: A）

> "指数退避 `min(100ms * 2^n, 30s)`，最大 3 次重试，3 次连续失败触发熔断器" — A §5.2；回退开关 "`agent.retry.enabled=false`" — A §8

**Verdict:** implemented · confidence: high

**What this demands:** RETRYING 状态按退避公式延时、限次、熔断，并可配置关闭。

**Where enforcement lives:** `state/RetryingStateHandler.java`
- 配置: L33-36（`agent.retry.enabled:true`、`max-retries:3`、`base-delay-ms:100`、`max-delay-ms:30000`）
- 禁用分支: L52-56 → FAILED
- 不可重试类别直接失败: L59-66（读 `lastErrorCategory` → `ToolErrorCategory.isRetryable()`）
- 限次: L69-76（retryCount > maxRetries → FAILED）
- 熔断: L79-86（连续失败计数 ≥3 → FAILED）
- 退避: L89 `Math.min(baseDelayMs * (1L << (retryCount - 1)), maxDelayMs)` — 与公式 `min(100ms*2^n, 30s)` 一致（n 从 0 起）
- 回到 TOOL_CALLING: L102-103（写 `retryContext` 元数据）。

**Paths walked:** ✓ 禁用 / ✓ 不可重试 / ✓ 超限 / ✓ 熔断 / ✓ 中断恢复（L94-99）/ ✓ 正常重试

**How the verdict was reached:** 交互注记（不影响裁决）：因熔断阈值(3)与最大重试数(3)同为 3，第 3 次进入 RETRYING 时熔断先触发，实际执行的物理重试为 2 次；这与"3 次连续失败触发熔断器"字面一致。另：`clearRetryState()`（L109）无任何调用方，成功路径不清理计数器（同一执行内后续失败会累计），为轻微泄漏，未达规格条款故不单列。

---

## REQ-05 — 重试仅重放失败的 ToolCall（来源: A）

> "仅重放失败的 ToolCall（非完整对话历史）" — A §5.2

**Verdict:** contradicted · confidence: high · **severity: High**

**What this demands:** 重试上下文下只执行上一次失败的那个工具调用。

**Where enforcement lives:**
- 过滤逻辑: `state/ToolCallingStateHandler.java:136-143`（isRetry 时 `isRetryTarget` 为 false 的调用被 skip）、`:285-289`（`isRetryTarget` 读元数据 `failedToolName`）。
- 失败记录方: `state/ToolCallingStateHandler.java:158-167` — 失败时只写 `lastErrorCategory`，**从未写 `failedToolName`**。

**Paths walked:**
- ✗ 重试路径: 进入重试上下文后 `failedToolName` 恒为 null → `isRetryTarget` 恒返回 false → **所有** ToolCall 被 skip → 循环结束后直接回 THINKING。实际行为是"重放 0 个调用"，与"仅重放失败调用"矛盾。
- （叠加 REQ-02：重试时解析结果本就为空，双重失效。）

**Searched:**
- `failedToolName` 全仓 → 3 命中：生产读取 1（`ToolCallingStateHandler.java:287`）；测试设置 2（`state/ToolCallingStateHandlerTest.java:312, 372`）；生产写入 0。

**How the verdict was reached:** 判 contradicted 而非 partial：机制骨架存在，但缺失的是唯一的写入方，导致行为与规格描述相反（应重放 1 个，实际重放 0 个）。测试通过仅因测试自行设置了 `failedToolName`。

---

## REQ-06 — PausedStateHandler：创建快照、持久化、等待外部 Resume（来源: A）

> "创建 ExecutionSnapshot，持久化到 DB，等待外部 Resume API 信号" — A §5.2

**Verdict:** implemented · confidence: high

**What this demands:** PAUSED 时以真实数据生成快照并落库，然后挂起不做自动转移。

**Where enforcement lives:**
- `state/PausedStateHandler.java:45-99`：从 `chatMemoryStore.loadMessages()` 构建 chatHistory（L49-60）、拷贝元数据为 contextVariables（L63-69）、`snapshotPersistence.saveSnapshot(snapshot)`（L88）、写回 `execution.snapshotId` 并保存（L91-92）、显式不自动转移（L98 注释）。
- `lifecycle/ExecutionSnapshotPersistence.java:23-38`：序列化 JSON + `HashUtils.sha256` 哈希，写 `sf_agent_execution_snapshot`（表存在于 `docker/postgres/init/02-init-schema-agent.sql:91`），并同步 `ExecutionSnapshotService` 缓存。
- 触发方: `orchestrator/AgentRuntimeOrchestrator.java:122-126`（Redis 暂停信号 → PAUSED）、`lifecycle/AgentExecutionLifecycleService.java:57-67`（API 主动暂停；工作树未提交修改已将 Redis Key 改为租户作用域）。

**Paths walked:** ✓ 消息为空（chatHistory=null 仍持久化）/ ✓ 持久化（含哈希）/ ✓ 无自动转移

**How the verdict was reached:** 注记：`consumedInputTokens/consumedOutputTokens` 置 null（`PausedStateHandler.java:83-84` 自注 "v1: not persisted yet"）；快照规格 §5.2 未强制该字段，不构成缺口。`execution.snapshotId` 取值错误的问题单独记入 REQ-07（属恢复侧失效）。

---

## REQ-07 — ResumingStateHandler：加载快照、归属校验、恢复并转入 THINKING（来源: A）

> "加载 SfAgentExecutionSnapshot 快照，恢复 chatMemoryStore 状态，validate snapshot belongs to execution（防止跨租户注入）。路径：PAUSED → RESUMING → THINKING" — A §5.2

**Verdict:** partial · confidence: high · **severity: High**

**What this demands:** Resume 时按快照 ID 加载、校验归属与完整性、恢复上下文并继续执行。

**Where enforcement lives:**
- `state/ResumingStateHandler.java:33-94`：snapshotId 判空（L36-41）→ `snapshotMapper.selectById`（L44）→ 归属校验（L52-57）→ JSON 完整性（L60-64）→ SHA-256 恒定时间比对防篡改（L67-77）→ 恢复上下文（L80-83）→ THINKING（L93）。
- 入口: `lifecycle/AgentExecutionLifecycleService.java:69-80`（resumeExecution → RESUMING）。
- **缺陷点**: `state/PausedStateHandler.java:91` — `execution.setSnapshotId(snapshot.getExecutionId() != null ? snapshot.getExecutionId() : execution.getId())`，而 `snapshot.getExecutionId()` 就是 execution 自身 ID（`PausedStateHandler.java:79` 设置）。`ExecutionSnapshotPersistence.saveSnapshot()` 为 void，MyBatis-Plus 生成的行主键（ASSIGN_ID 雪花值）被丢弃。

**Paths walked:**
- ✗ 正常恢复路径: `selectById(snapshotId==executionId)` 查不到行（快照行主键是独立雪花 ID）→ L45-49 "Snapshot not found" → FAILED。生产路径下 resume 必然失败。
- ✓ 归属校验分支（L52-57）、✓ 损坏快照分支（L60-64）、✓ 哈希篡改分支（L67-74）、✓ 无哈希遗留数据宽容分支（L75-77）。
- ⚠ "恢复 chatMemoryStore 状态"：实现仅把 snapshotJson 存入 `execution.metadata("restoredContext")`（L82），未向 chatMemoryStore 回写；会话历史依赖 L1/L2 存储自身持久化，属机制漂移。

**Searched:**
- `setSnapshotId` → 生产仅 `PausedStateHandler.java:91` 一处写入；无任何代码使用 `SfAgentExecutionSnapshot` 的生成主键回填。

**How the verdict was reached:** 校验逻辑（防跨租户注入、防篡改）比规格更强，但 ID 接线错误使整条恢复路径在实际数据下必然走 "Snapshot not found → FAILED"。缺失具体路径明确，判 partial。

---

## REQ-08 — Resume API 契约（POST /agent/execution/{executionId}/resume）（来源: A）

> "POST /agent/execution/{executionId}/resume … Request: {resumedBy, reason} … Response data: {executionId, previousState, newState}" — A §3

**Verdict:** partial · confidence: high · severity: Low

**What this demands:** 提供恢复端点，接收操作人/原因，返回状态迁移信息。

**Where enforcement lives:**
- `controller/AgentExecutionController.java:54-59` — `POST /agents/{id}/executions/{execId}/resume`，无请求体，返回 `Result<Void>`。
- `schemaplexai-web/.../controller/ExecutionWebController.java:55-57` — `POST /web/executions/{id}/resume`，同样无 body、`Result<Void>`。
- 委托: `lifecycle/AgentExecutionLifecycleService.java:69-80`。

**Paths walked:** ✓ 功能路径（清 Redis 暂停键 → RESUMING 迁移）；✗ 请求契约（`resumedBy`/`reason` 未接收，无审计）；✗ 响应契约（无 `previousState`/`newState`）；✗ 路径不一致（/agent/execution/{id}/resume vs 实际两处路径）。

**How the verdict was reached:** 功能存在（恢复信号确实触发 RESUMING），判 partial 而非 absent；契约三要素（路径、请求体、响应体）全部漂移，但属接口形状差异而非控制缺失，严重度 Low。下游恢复本身失效另见 REQ-07。

---

## REQ-09 — GateBlockedStateHandler：记录 AdmissionResult、重试倒计时、发布阻塞事件、转入 RETRYING（来源: A）

> "记录 AdmissionResult，可配置重试倒计时，发布 AgentBlockedEvent" — A §5.2；状态图 "GATE_BLOCKED --> RETRYING: retry timer" — A §5.1

**Verdict:** contradicted · confidence: high · **severity: Critical**

**What this demands:** GATE_BLOCKED 时记录准入结果、发出阻塞通知、（可重试时）进入重试。

**Where enforcement lives:**
- `state/GateBlockedStateHandler.java:35-80`：读 `blockedReason`/`admissionType`（L40-47）→ 可重试分支设倒计时元数据并保存（L49-53）→ `eventPublisher.publishExecutionEvent("AGENT_GATE_BLOCKED", Map.of(...))`（L56-62）→ `transition(RETRYING)`（L67）；不可重试分支 L68-79。
- `mq/AgentExecutionEventPublisher.java:22-28`：**首行** `payload.put("eventType", eventType)`。
- **致命缺陷**: 两处调用点（`GateBlockedStateHandler.java:56, 70`）传入的都是 `Map.of(...)` 不可变 Map → `payload.put` 必抛 `UnsupportedOperationException`；`@SneakyThrows` 不吞运行时异常。
- 异常传播链: `state/middleware/MiddlewarePipeline.java:76-87`（after 回调后 `throw e` 原样重抛）→ `state/AgentStateMachine.java:85-88`（handler 异常 → `transition(FAILED)`）。

**Paths walked:**
- ✗ 可重试路径: 事件发布（L56）先抛异常 → L67 的 RETRYING 转移永不到达 → 状态机兜底 FAILED。
- ✗ 不可重试路径: 发布同样抛异常；虽然终态碰巧也是 FAILED，但阻塞事件从未送达 MQ。
- ✗ 波及面: 所有进入 GATE_BLOCKED 的场景同归于尽——准入拒绝（`AgentRuntimeOrchestrator.java:106-110`）、护栏拦截（`ThinkingStateHandler.java:152-160`）、Token 预算（`ThinkingStateHandler.java:166-173, 200-208`）、循环检测（`ThinkingStateHandler.java:224-230`、`ToolCallingStateHandler.java:129-133`）、工具 blocked（`ToolCallingStateHandler.java:171-178`）、工具预算（`ToolCallingStateHandler.java:105-123`）。
- ✓ 唯一幸存: 异常发生前 `blockedReason`/`retryCountdown` 元数据已保存（L51-53 在 publish 之前）。

**How the verdict was reached:** 判 contradicted 而非 partial：规格宣称的三条行为（事件、倒计时等待、转重试）在**所有**可达路径上均不生效，且把本应可恢复的阻塞全部退化为 FAILED；这不是缺某个分支，而是整个控制点运行时必然失效。附带注记：倒计时为硬编码常量 60（`GateBlockedStateHandler.java:21`），"可配置"未实现；且即使修复 Map 问题，代码是立即转 RETRYING，并无真实计时等待。

---

## REQ-10 — AgentLoopDetectionService 集成到 Thinking 与 ToolCalling 处理器（来源: A）

> "`AgentLoopDetectionService` — 已完整实现但未集成到 ThinkingStateHandler 和 ToolCallingStateHandler"（问题陈述）；"AgentLoopDetectionService 集成" — A §1.1/§1.2；组件图 TSH→ALDS、TCSH→ALDS — A §2.1

**Verdict:** implemented · confidence: high

**What this demands:** 两个处理器在转移前调用循环检测，检出循环则阻断。

**Where enforcement lives:**
- ThinkingStateHandler: 构造注入（`ThinkingStateHandler.java:67`）；内联推理路径检测（L218-231，hash+toolNames → GATE_BLOCKED + `admissionType=LOOP`）；策略委托路径检测（L330-341）；直接答案完成时 `clearRecords`（L244、L325）。
- ToolCallingStateHandler: 注入（`ToolCallingStateHandler.java:51`）；执行前检测（L126-133 → GATE_BLOCKED）。
- 服务本体: `loop/AgentLoopDetectionService.java:33-72`（窗口 5、同哈希 ≥3、同工具序列 ≥3，均可配置）。

**Paths walked:** ✓ 哈希循环 / ✓ 工具序列循环 / ✓ 窗口不足不误报（L37-39）/ ✓ 清除记录。

**How the verdict was reached:** 注记（不计裁决）：A §8 风险缓解提到 "clearRecords() + TTL 驱逐"，服务只有显式 `clearRecords()`，无 TTL 驱逐；FAILED 路径不清理，`executionRecords` 随执行累积。因 §8 属风险表而非功能需求，仅记录为残留项。另：REQ-09 缺陷导致检出循环后 GATE_BLOCKED→FAILED（本 REQ 的"阻断"仍生效，只是终态更重）。

---

## REQ-11 — ToolErrorCategory 扩展（9 值 + securityRelated + retryable）（来源: A）

> "PERMISSION_DENIED(true, false), … UNEXPECTED_ENVIRONMENT(true, false); private final boolean securityRelated; private final boolean retryable;" — A §5.3

**Verdict:** implemented · confidence: high

**What this demands:** 枚举含规格 9 个值且两布尔标志逐一对应。

**Where enforcement lives:** `tool/ToolErrorCategory.java:11-61` — 9 个规格值的 (securityRelated, retryable) 标志与 §5.3 逐项一致（如 PERMISSION_DENIED(true,false) L11、TIMEOUT(false,true) L21、RATE_LIMITED(false,true) L31、IRREVERSIBLE_OPERATION(true,false) L41）；L63-77 字段与访问器。

**Paths walked:** ✓ 枚举值逐一比对（9/9 一致）；✓ 消费方：`RetryingStateHandler.java:61`（isRetryable）、`ToolCallingStateHandler.java:163`、`ToolExecutionRecorder.java:36`（isSecurityRelated 审计失败即抛异常）。

**How the verdict was reached:** 超出规格新增 `PATH_VIOLATION(true,false)`（L56）与 `SANDBOX_ERROR(false,true)`（L61），为后续沙箱规格服务，不构成冲突；按"不同机制满足=implemented"原则记 implemented，附 stronger 注记。

---

## REQ-12 — Prometheus 指标管道：6 项自定义指标经 /actuator/prometheus 暴露（来源: A）

> 指标表 "agent_tool_execution_total / agent_tool_execution_latency_seconds / agent_tool_keep_rate / agent_tool_blocked_rate / agent_tool_error_by_category / agent_tool_retry_total" — A §3；"Prometheus MeterRegistry + ToolExecutionMetricsBinder" — A §1.2

**Verdict:** partial · confidence: high · severity: Medium

**What this demands:** 指标真实反映工具执行情况并可被 Prometheus 抓取。

**Where enforcement lives:**
- `metrics/ToolExecutionMetricsBinder.java:47-91`（bindTo 注册全部 6 个指标名）、`:96-122`（recordSuccess/recordFailure/recordBlocked/recordRetry）。
- 依赖: `schemaplexai-agent-engine/pom.xml:39`（actuator）、`:43`（micrometer-registry-prometheus）。

**Paths walked:**
- ✗ 数据链路: 4 个 Counter 注册后**从不 increment**——record* 方法只更新内存 `LongAdder` Map（L40-44, L124-126），与已注册的 Micrometer Counter 无任何桥接。
- ✗ 调用方: 生产代码 0 处调用 record*（见 Searched）；`ToolExecutionRecorder.java:26-41` 只写审计日志，不触碰 Binder。
- ✗ 延迟指标: `recordSuccess(toolName, latencyMs)` 忽略 `latencyMs` 参数（L96-98），注册的 Timer（L65-68）从未 `record()`。
- ✗ 类别标签: `agent_tool_error_by_category`（L81-83）注册时未带规格要求的 `errorCategory` 标签。
- ✗ 端点暴露: `application.yml` 无 `management.endpoints.web.exposure`（全模块仅 schemaplexai-admin 占位配置含 exposure），Spring Boot 3.2 默认仅暴露 health → `/actuator/prometheus` 返回 404。
- ✓ 仅两个 Gauge（keep_rate/blocked_rate）能通过回调读到内存值——但因无人写内存，恒为 1.0/0.0。

**Searched:**
- `recordSuccess|recordFailure|recordBlocked|recordRetry|MetricsBinder` in agent-engine/src/main → 仅 Binder 自身文件内命中，0 外部调用方。
- `endpoints|prometheus` in agent-engine resources → 仅 grafana 仪表盘 JSON，无 exposure 配置。

**How the verdict was reached:** 指标"名义存在、实际恒零"：判 partial 而非 contradicted，因为组件与指标名齐全且 Gauge 机制可用，只是管道两端（采集调用、端点暴露）均未接通。影响为可观测性误导（看板恒零/恒 1.0），不改变执行语义，故 Medium。

---

## REQ-13 — TenantEnvironmentConfig 实体 + Mapper + 全局表（不过租户拦截器）（来源: A）

> "`sf_tenant_environment_config` | CREATE | 租户环境安全配置表（**全局表** — 不通过 TenantLineInterceptor 过滤）" — A §4.1；实体字段清单 — A §4.2

**Verdict:** implemented · confidence: high

**What this demands:** 实体 9 字段齐全；表被租户拦截器排除；Mapper 可用。

**Where enforcement lives:**
- 实体: `schemaplexai-model/.../entity/config/TenantEnvironmentConfig.java:16-44` — tenantId/environment/allowedTools/securityLevel/allowHttpCalls/allowFileRead/allowIrreversibleOps/maxConcurrentToolCalls/extraConfig 与 §4.2 逐字段一致，`@TableName("sf_tenant_environment_config")`。
- 全局表排除: `schemaplexai-dao/.../config/TenantLineInterceptor.java:31`（`ignoreTable` 返回 true），测试佐证 `schemaplexai-dao/src/test/.../TenantLineInterceptorTest.java:66`。
- Mapper: `schemaplexai-dao/.../mapper/TenantEnvironmentConfigMapper.java:10`（extends BaseMapperX）；引擎侧 Bean 注册 `config/CrossModuleMapperConfig.java:15-18`。

**Paths walked:** ✓ 实体字段比对 / ✓ 拦截器排除 / ✓ 跨模块 Mapper 装配。

**How the verdict was reached:** DDL 注记：`docker/postgres/init/*.sql` 无此表（grep 全空），表结构仅见于测试 schema（`schemaplexai-dao/src/test/resources/schema.sql:16`）。A §1.2 明确将"数据库 Migration 脚本"列为 Out of Scope、§7.2 注明"需要新表 DDL"，属部署侧事项，不计为代码缺口（见 Open questions）。

---

## REQ-14 — SecurityPolicyLoader：Caffeine 缓存(1000, 5min) + deny-by-default（来源: A）

> "缓存: SecurityPolicyLoader Caffeine Cache (maximumSize=1000, expireAfterWrite=5min)" — A §7.1；"deny-by-default 默认策略" — A §6

**Verdict:** implemented · confidence: high

**What this demands:** 缓存参数达标；无配置租户获得拒绝型默认策略；支持手动刷新。

**Where enforcement lives:** `config/SecurityPolicyLoader.java`
- Caffeine: L29-35（maximumSize=1000, expireAfterWrite=5min，与 §7.1 一致）
- 加载链: L43-78（cache → DB → default，异常兜底 default）
- deny-by-default: L95-105（environment="unknown"、securityLevel="HIGH"、allowHttpCalls=false、allowFileRead=false、allowIrreversibleOps=false、maxConcurrentToolCalls=1）
- 刷新: L84-89（refresh → invalidate）；管理端 `schemaplexai-web/.../config/TenantEnvironmentConfigController.java:62-70` 提供 PATCH refresh。

**Paths walked:** ✓ 命中缓存 / ✓ 未命中查库 / ✓ 查库无记录走默认 / ✓ 异常兜底 / ✓ 消费方 `HttpCallAdapter.java:84-91` 依 allowHttpCalls 拒绝。

**How the verdict was reached:** 文档漂移（Low，改文档即可）：类 Javadoc（L15-17）写 "environment=dev, securityLevel=LOW"，与实际默认（unknown/HIGH）不符；代码比文档更严，不损害安全。

---

## REQ-15 — 租户策略标志在工具执行中的强制（来源: A）

> 实体字段 "allowHttpCalls / allowFileRead / allowIrreversibleOps / maxConcurrentToolCalls / allowedTools / securityLevel"（语义为许可开关） — A §4.2；"全局表安全：… deny-by-default 默认策略" — A §6

**Verdict:** partial · confidence: high · severity: Medium

**What this demands:** 各许可标志在工具执行路径上被检查，未授权即拒绝。

**Where enforcement lives:**
- ✓ `allowHttpCalls`: `tool/adapter/http/HttpCallAdapter.java:84-91`（null/false → ENVIRONMENT_MISMATCH 拒绝）。
- ✗ `allowFileRead`: `tool/adapter/file/FileReadAdapter.java:37-94` 无任何策略检查（全文未引用 SecurityPolicyLoader）。
- ✗ `allowedTools`: 引擎内无按租户白名单过滤；`tool/ToolWhitelist.java:8-32` 是未装配的裸类（无 @Component、无调用方）。
- ✗ `securityLevel` / `maxConcurrentToolCalls`: 全引擎仅 `SecurityPolicyLoader.java:65` 的 debug 日志引用，无强制点。
- 注: `ToolSafetyGuard.check` 的 environment 参数来自策略（`ToolCallingStateHandler.java:217-227`），但只做 "prod" 字面匹配（`ToolSafetyGuard.java:47-54`），与 securityLevel 无关。

**Paths walked:** ✗ file_read 在默认（deny）租户下仍可读文件 / ✗ 并发工具数无上限检查 / ✗ 租户级工具白名单不生效。

**How the verdict was reached:** 规格未逐条写明"每个标志必须有强制点"，但字段语义即许可开关且 §6 宣示 deny-by-default；五个标志仅一个生效，判 partial。影响：安全配置的承诺面大于执行面（例如管理员关闭 allowFileRead 无实际效果）。

---

## REQ-16 — HttpCall SSRF 防护（内网 IP、DNS rebinding、重定向 ≤3、方法白名单）（来源: A）

> "HttpCall SSRF 防护：IPv4/IPv6 内网 IP 过滤、DNS rebinding guard（双重解析比对）、重定向深度限制（最大 3 次）、HTTP 方法白名单" — A §6

**Verdict:** implemented · confidence: high

**What this demands:** 四项 SSRF 控制在 HTTP 工具路径上生效。

**Where enforcement lives:** `tool/adapter/http/HttpCallAdapter.java` + `tool/adapter/http/SsrfProtectionUtil.java`
- 方法白名单: L44（GET/POST/PUT/DELETE/PATCH/HEAD/OPTIONS）、L76-79 拒绝。
- 私网过滤: L140-153 → `SsrfProtectionUtil.isPrivateAddress`（`SsrfProtectionUtil.java:27-79`：IPv4 10/8、172.16/12、192.168/16、127/8、169.254/16、0/8；IPv6 fc00::/7、fe80::/10、ff00::/8、IPv4-mapped 变体）。
- DNS rebinding: L155-169（二次解析比对，不一致即拒）。
- 重定向: `followRedirects(NEVER)` L57 + 手工跟随并逐跳复检（L189-206），深度上限 `MAX_REDIRECTS=3`（L46、L109-112）。
- 附加: 协议白名单（仅 http/https，L122-131）、租户主机 allowlist（L221-271）。

**Paths walked:** ✓ 各拒绝分支 / ✓ 相对重定向解析（L196-199）/ ✓ 每跳重新执行完整检查（递归回 executeWithRedirectProtection）。

**How the verdict was reached:** 残留风险（不降裁决，供后续处理）：① 双重解析后 `httpClient.send()` 会第三次解析 DNS，存在经典 TOCTOU 窗口（未 pin 已验 IP）；② 302 跟随强制改 GET（L205），语义可接受。规格所列四项机制全部在场且为主路径强制，判 implemented。

---

## REQ-17 — FileRead 路径遍历防护（规范化+工作区根、隐藏文件、符号链接）（来源: A）

> "FileRead 路径遍历防护：路径规范化 + workspace root 验证、所有路径组件检查隐藏文件、符号链接检测（NOFOLLOW_LINKS）" — A §6

**Verdict:** implemented · confidence: high

**What this demands:** 三项路径安全控制在文件读取工具上生效。

**Where enforcement lives:** `tool/adapter/file/FileReadAdapter.java`
- 规范化+根校验: L49-58（toAbsolutePath().normalize() → startsWith，越界 → ENVIRONMENT_MISMATCH）。
- 隐藏组件: L60-69（relativize 后**逐组件**检查 `.` 前缀）。
- 符号链接: L71-82（`Files.readAttributes(..., LinkOption.NOFOLLOW_LINKS)` + isSymbolicLink 拒绝）。

**Paths walked:** ✓ 越界（../）/ ✓ 隐藏目录任意层级 / ✓ 末端符号链接 / ✓ 文件不存在（IOException 吞掉后走读取失败分支）。

**How the verdict was reached:** 残留风险（记录不降裁决）：仅校验末端路径的链接属性；若工作区内某**目录**为符号链接（如 `ws/link -> /etc`），`ws/link/passwd` 的末端不是链接，可穿透读取。规格表述的三项机制均已实现，判 implemented。

---

## REQ-18 — 输入验证复用 ToolSafetyGuard.normalizeInput()（Unicode homoglyph + HTML entity + JSON escape）（来源: A）

> "输入验证：复用 ToolSafetyGuard.normalizeInput()（Unicode homoglyph + HTML entity + JSON escape 解码）" — A §6

**Verdict:** implemented · confidence: high

**What this demands:** 安全检查前对参数做三类去混淆归一化。

**Where enforcement lives:** `tool/ToolSafetyGuard.java`
- check 链: L26-57（不可逆工具名 L28-33 → 参数归一化后正则匹配不可逆命令 L36-44 → 环境不匹配 L47-54）。
- normalizeInput: L64-87 — JSON `\uXXXX` 解码（L118-137）→ HTML `&#N;` 实体解码（L139-161）→ NFKC（L78）→ 西里尔同形字符手工映射（L89-116）→ 空白折叠（L84）。
- 调用方: `ToolCallingStateHandler.java:224-227` 每次工具执行前调用 `safetyGuard.check(...)`。

**Paths walked:** ✓ 三类混淆输入均先解码再匹配（顺序：先 JSON escape、再 HTML 实体、再 NFKC/同形映射，可解嵌套混淆）。

**How the verdict was reached:** 规格三要素（homoglyph/HTML/JSON escape）全部在位且在主执行路径强制，判 implemented。

---

## REQ-19 — TokenEstimator 共享工具（来源: A）

> "`TokenEstimator.java` | 新增 | 共享 Token 估算工具" — A §9 实现文件清单

**Verdict:** implemented · confidence: high

**What this demands:** 提供统一的 Token 估算并被多处复用。

**Where enforcement lives:** `util/TokenEstimator.java:9-30`（1 token≈4 字符，字符串与消息列表两个重载）；消费方：`ThinkingStateHandler.java:464-466`（输入/输出预算估算）、`ToolCallingStateHandler.java:296-298`（工具输出估算）、测试 `util/TokenEstimatorTest.java`。

**Paths walked:** ✓ null/空串返回 0 / ✓ 非空至少 1。

**How the verdict was reached:** 直接满足，无分歧。

---

## REQ-20 — RAG 集成：ContextInjector.inject 拉取并注入检索上下文（来源: B）

> "`ContextInjector.inject(List<LlmMessage>, Long)` now extracts the latest user message, retrieves `tenantId` from `TenantContextHolder`, builds an `AgentContext`, and calls `retrieveRagContext()`. RAG results are injected as a `system` message before the last user message. Failures are non-blocking" — B §1 Solution

**Verdict:** partial · confidence: high · **severity: High**（规格 status=implemented 宣称不成立）

**What this demands:** THINKING 前实际注入租户隔离的 RAG 上下文。

**Where enforcement lives:**
- 行为实现与规格逐句对应: `context/ContextInjector.java:61-92`（extractLatestUserPrompt L94-102 → TenantContextHolder L73 → AgentContext L79-82 → retrieveRagContext L85 → 非阻塞 try/catch L89-91）；system 消息插在最后一条 user 之前（L104-118）。
- **失效点 1（依赖未装配）**: `MilvusIsolationService` 为裸类无注解（`rag/MilvusIsolationService.java:14`），`EmbeddingService` 是接口且引擎模块内无任何实现/工厂；ContextInjector 有三个构造器但均无 @Autowired，Spring 走无参构造（L36-39）→ `ragService==null` → `retrieveRagContext` 直接返回 ""（L188-191）。
- **失效点 2（租户上下文缺失）**: 执行跑在 @Async 线程池（`AgentExecutionEngine.java:23-27`），`AgentExecutionAsyncConfig.java:16-28` 未配置 TaskDecorator，TenantContextHolder 不传播；即使失效点 1 修复，`inject` 也会在 L73-77 因 tenantId==null 提前返回。
- 调用方在场: `ThinkingStateHandler.java:143`。

**Searched:**
- `implements EmbeddingService` → 仅命中 schemaplexai-context 模块（另一服务，引擎不可注入）与旧评审文档；引擎 main 内 0 实现。
- `new MilvusIsolationService|@Bean`（引擎 config 目录）→ 无工厂；仅测试代码构造。
- `setRagService|setEmbeddingService` in main → 0 调用（仅定义处）。

**How the verdict was reached:** 判 partial 而非 absent：`inject()` 的行为逻辑与规格逐句一致且有专门测试（`ContextInjectorTest.java`）；但两个独立的运行时断路使其在生产路径恒为空操作。规格 status=implemented 的宣称与运行时事实不符，故 High。

---

## REQ-21 — 护栏层：GuardrailsConfig 装配 + ThinkingStateHandler 前置校验（来源: B）

> "`GuardrailsConfig.java`: `@Configuration` class wiring all guardrail beans. `ThinkingStateHandler`: added `GuardrailsEngine` constructor parameter; calls `validateInput(prompt)` before LLM invocation; transitions to `GATE_BLOCKED` on failure with `admissionType=GUARDRAILS`" — B §2 Solution

**Verdict:** implemented · confidence: high

**What this demands:** 护栏成为 Spring Bean 且在 LLM 调用前强制。

**Where enforcement lives:**
- 装配: `guardrails/GuardrailsConfig.java:12-30`（@Configuration，BlacklistGuardrail/LengthGuardrail/GuardrailsEngine 三 Bean）。
- 注入: `state/ThinkingStateHandler.java:55, 69, 80`（构造参数）。
- 强制点: `state/ThinkingStateHandler.java:152-160` — LLM 调用（L195）之前 `validateInput(prompt)`；失败 → 写 `blockedReason` + `admissionType=GUARDRAILS` → GATE_BLOCKED。
- 引擎: `guardrails/GuardrailsEngine.java:26-34`（短路式逐规则校验）。

**Paths walked:** ✓ 校验通过 → 继续 / ✓ 校验失败 → GATE_BLOCKED（元数据齐备）/ ✓ 无护栏规则时返回 valid（GuardrailsEngine L16 空列表兜底）。

**How the verdict was reached:** 规格三条（装配、前置调用、失败迁移）逐字满足。运行时后果注记：拦截后进入的 GATE_BLOCKED 因 REQ-09 缺陷必然退化为 FAILED——该后果归因于 REQ-09，不在本条扣减。

---

## REQ-22 — 规划模式：PLANNING 状态介于 READY 与 THINKING 之间（来源: B）

> "`AgentExecutionState.PLANNING`: new state between `READY` and `THINKING`. `PlanningStateHandler`: LLM-based task decomposition into `SubTaskPlan`; stores plan JSON in execution metadata; transitions to `THINKING`. … `ThinkingStateHandler`: `resolveNextStateForPlan()` advances sub-tasks" — B §3 Solution

**Verdict:** partial · confidence: high · **severity: High**（规格 status=implemented 宣称不成立）

**What this demands:** 执行流实际经过 PLANNING 完成任务分解。

**Where enforcement lives:**
- 组件齐备: 枚举 `state/AgentExecutionState.java:7`；`state/PlanningStateHandler.java:55-112`（LLM 分解 L73-80 → 元数据存计划 JSON L99-100 → THINKING L104）；模型 `plan/SubTask.java:18-37`、`plan/SubTaskPlan.java:23-91`（依赖解析 findNextReadySubTask L42-59、isAllCompleted L67-73）；推进逻辑 `ThinkingStateHandler.java:569-607`。
- **缺失的接线**: 全库无任何指向 PLANNING 的转移。`state/ReadyStateHandler.java:17-20` 直接 READY→THINKING；`state/InitializingStateHandler.java:17-20` INITIALIZING→READY。

**Paths walked:**
- ✗ 主流程: QUEUED→INITIALIZING→READY→THINKING，PLANNING 不可达（死状态）。
- ✗ 计划创建: PlanningStateHandler 永不被调度 → `subTaskPlan` 元数据恒为 null → `resolveNextStateForPlan` 恒走 null 分支直接 COMPLETED（`ThinkingStateHandler.java:571-573`）。
- ✓ 组件孤立行为正确（解析、依赖推进代码审读无误，且有测试 `PlanningStateHandlerTest.java`、`SubTaskPlanTest.java`）。

**Searched:**
- `PLANNING|PlanningStateHandler` in agent-engine/src/main → 8 命中：枚举定义 1、PlanningStateHandler 自身 7；其余处理器/编排器 0 处引用（无 `transition(PLANNING`）。

**How the verdict was reached:** 判 partial 而非 absent：全部类与逻辑存在且与规格描述一致，只差一条转移接线；判非 contradicted 因无相反实现。"between READY and THINKING" 这一核心宣称在运行时不成立，故 High。

---

## REQ-23 — A2A 协议（P2）：a2a 包五文件 + 42 测试（来源: C）

> "**Package**: `com.schemaplexai.agent.engine.a2a` **Files**: AgentCard, A2aMessage, A2aClient, A2aMessageHandler, A2aProtocolException **Tests**: AgentCardTest (7), A2aMessageTest (8), A2aProtocolExceptionTest (4), A2aClientTest (16), A2aMessageHandlerTest (7)" — C §6

**Verdict:** implemented · confidence: high

**What this demands:** 包与文件齐备、实现非桩、测试文件齐备。

**Where enforcement lives:**
- `a2a/AgentCard.java`（26 行）、`a2a/A2aMessage.java`（36）、`a2a/A2aMessageHandler.java:8-25`（接口）、`a2a/A2aProtocolException.java`（32）、`a2a/A2aClient.java:26-211`（@Component，RestTemplate + 3 次重试 + 30s 超时 + 同步/异步发送）。
- 测试: `a2a/AgentCardTest.java`、`A2aMessageTest.java`、`A2aProtocolExceptionTest.java`、`A2aClientTest.java`、`A2aMessageHandlerTest.java` 全部存在。

**Paths walked:** ✓ 文件清单比对 5/5 / ✓ 实现抽查（重试循环、错误码枚举）/ ✓ 测试文件 5/5。

**How the verdict was reached:** 具体测试用例数（7/8/4/16/7）未逐条计数、未运行，列入 notChecked。反向注记：A2aClient 未被任何执行路径调用（独立能力），规格本身也只要求文件+测试。

---

## REQ-24 — 优先级调度（P2）：scheduler 包五文件 + 38 测试（来源: C）

> "**Files**: ExecutionPriority, PrioritizedExecution, ExecutionScheduler, SlaBreachEvent, SlaMonitor **Tests**: ExecutionPriorityTest (6), ExecutionSchedulerTest (18), SlaMonitorTest (14)" — C §7

**Verdict:** implemented · confidence: high

**Where enforcement lives:**
- `scheduler/ExecutionScheduler.java:25-170`（PriorityBlockingQueue + 索引 + 锁；submit 去重 L48-66、pollNext L73-85、reorder L94-119、cancel L127-145）。
- `scheduler/SlaMonitor.java:24-160`（@Service，@Scheduled 违约扫描、队列时长统计）。
- `scheduler/ExecutionPriority.java`（22 行，权重枚举）、`PrioritizedExecution.java`（65，withPriority 不可变更新）、`SlaBreachEvent.java`（45）。
- 测试: `scheduler/ExecutionPriorityTest.java`、`ExecutionSchedulerTest.java`、`SlaMonitorTest.java` 存在。

**Paths walked:** ✓ 文件 5/5 / ✓ 实现抽查（比较器三段排序、重复提交防护）/ ✓ 测试 3/3。

**How the verdict was reached:** 用例数未计数，列 notChecked。反向注记：调度器未接入真实执行分发（AgentExecuteDispatcher/AgentExecutionEngine 直接执行），规格未要求接线。

---

## REQ-25 — 学习/自适应（P2）：learning 包五文件 + 33 测试（来源: C）

> "**Files**: ToolFailurePattern, PromptPerformancePattern, FeedbackTrendAnalyzer, PromptOptimizer, ModelSelector **Tests**: FeedbackTrendAnalyzerTest (7), PromptOptimizerTest (14), ModelSelectorTest (12)" — C §8

**Verdict:** implemented · confidence: high

**Where enforcement lives:**
- `learning/ModelSelector.java:16-148`（@Service，5 模型画像 + 成本/时延/质量打分选择）。
- `learning/PromptOptimizer.java:14-107`（@Service，按阈值给优化建议）。
- `learning/FeedbackTrendAnalyzer.java`（147 行，@Service）、`learning/PromptPerformancePattern.java`（17，record）、`learning/ToolFailurePattern.java`（28，record）。
- 测试: `learning/FeedbackTrendAnalyzerTest.java`、`PromptOptimizerTest.java`、`ModelSelectorTest.java` 存在。

**Paths walked:** ✓ 文件 5/5 / ✓ 实现抽查 / ✓ 测试 3/3。

**How the verdict was reached:** 同 REQ-24：用例数 notChecked；组件未接入运行时（ModelResolver 走自有逻辑），规格未要求接线。

---

## REQ-26 — P1 模块测试覆盖（ops/quality/spec/workflow/task 指定测试文件）（来源: C）

> "ops Module Tests (56 tests) 6 controller test files … quality (45) 5 files … spec (51) 5 files … workflow (32) 4 files … task (98) 24 test files" — C §P1 1-5

**Verdict:** implemented · confidence: high

**What this demands:** 指定名的测试文件存在。

**Where enforcement lives:**（逐名比对结果）
- ops: `schemaplexai-ops/src/test/.../controller/` — Artifact/Budget/Cost/Delivery/Evaluation/Notification ControllerTest 6/6 ✓
- quality: AuditEvent/QualityGate/QualityIssue/Review/SecurityPolicy ControllerTest 5/5 ✓
- spec: Spec/SpecReview/SpecSteering/SpecTemplate/SpecVersion ControllerTest 5/5 ✓
- workflow: WorkflowBpmnControllerTest、WorkflowInstanceControllerTest、WorkflowTemplateControllerTest、WorkflowDeployServiceTest 4/4 ✓
- task: MQ 消费者 7、定时任务 6、配置 2、实体 4、DTO 3 共 24 个指定文件全部存在（24/24）✓

**Paths walked:** ✓ 文件枚举比对（find + 目录列表）。

**How the verdict was reached:** 仅验证文件存在与命名一致；各文件用例数与通过率未运行验证，列 notChecked。

---

## notChecked（无法静态裁决或超出本次深度）

1. 各处测试数量与通过率宣称：B "967 tests run, 0 failures"、C 各模块用例数（56/45/51/32/98、42/38/33、"282 new tests"）——未执行 `mvn test`。
2. A §7.1 性能宣称（resolve() <1ms、P99<5s、100 并发）——需基准测试；仅确认 ConcurrentHashMap 结构。
3. C §P4 "21 Agentic Design Patterns: 17/21 complete" —— 文档性宣称，未比对 `wiki/architecture-gap-analysis.md` 与 21 项逐条实现。
4. `sf_tenant_environment_config` 生产库 DDL 是否已由 DBA 执行（A 将其移出代码范围；`docker/postgres/init` 无该表，仅 `schemaplexai-dao/src/test/resources/schema.sql:16` 有测试版）。
5. `agent.retry.enabled=false` 的运行时回退演练（属性与分支存在，见 REQ-04）。

## 反向差距（代码存在、三份规格未提及的重要行为）

1. **工具审批闸门**: `ToolCallingStateHandler.java:149-153` 在执行前调用 `ToolApprovalService.checkAndRequestApproval`，可转 PAUSED——三份规格均未描述（与其他规格交叉）。
2. **双层工具调用预算**: 全局 `maxToolCalls` 与单轮 `maxToolCallsPerIteration`（`ToolCallingStateHandler.java:103-123`）。
3. **ReasoningStrategy 框架**: CoT/ReAct/CodeExecution 策略类存在（`reasoning/`）但均非 Bean，`ThinkingStateHandler.java:176-189` 的委托路径实际空置。
4. **MCP 工具子系统与容器/本地/Cube 沙箱**（`tool/mcp/`、`tool/sandbox/`）规模可观，不在三份规格内。
5. **快照防篡改哈希链**: `ExecutionSnapshotPersistence.java:30` SHA-256 + 恒定时间比对，超出规格要求（更强实现）。
6. **会话记忆加密**: `TenantKeyService` 加解密贯穿 `CompositeChatMemoryStore`，规格未提及；注意其依赖 TenantContextHolder，在 @Async 线程中为 null（行为待确认）。
7. **文档失真**: 根 `agents.md` 声称"项目中目前没有任何自动化测试"，与实际（各模块大量测试文件）矛盾——建议更新项目文档。

## Open questions

1. ToolCallingStateHandler 恒传 `provider=null`（REQ-02）是过渡性 TODO 还是遗漏？修复方向：由 Orchestrator/记忆中记录响应来源 provider 并传入。
2. `GateBlockedStateHandler` 使用 `Map.of` 传给会 `put` 的发布器（REQ-09）——是否有未合入的修复分支？
3. `PausedStateHandler.setSnapshotId(executionId)`（REQ-07）：是否计划让 `saveSnapshot` 返回生成主键？
4. 生产库是否已建 `sf_tenant_environment_config`（docker init 缺失，服务启动后 `securityPolicyLoader.load` 查库将走异常兜底默认策略——功能不挂但策略恒为拒绝态）。

## 反驳复核

> 独立复核员（非原分析作者）于 2026-08-29 基于当前工作树代码，对全部 severity ∈ {Critical, High} 且 verdict ∈ {contradicted, partial, absent} 的分歧条目（共 6 条：REQ-02/05/07/09/20/22）逐条尝试推翻。代码方向：重查被引用行、搜索遗漏的执行点（其他调用方、Bean 覆盖/子类、基类、异步配置、跨模块写入方、非测试入口）；规格方向：重读对应段落核查误读、豁免或条件子句（A status=approved 为需求基线；B、C status=implemented 的宣称已核对）。已确认工作树未提交修改仅涉及 `AgentExecutionLifecycleService.java` / `AgentRuntimeOrchestrator.java`（租户作用域 Redis Key），与下列发现无交互影响。

| REQ | 原判 | 裁定 | 理由（一行） | 证据 |
|---|---|---|---|---|
| REQ-02 | partial / Critical | 维持 | 唯一生产调用点恒传 provider=null → 路由 "GENERIC" 无解析器，恒返回空；全仓无替代解析路径，规格 A §1.2/§2.2 数据流无豁免子句 | `ToolCallingStateHandler.java:95`（null）；`tool/registry/ToolRegistry.java:85-91`；实现仅 OpenAi/AnthropicToolCallParser 两个（无 GENERIC）；入口启发式 `ThinkingStateHandler.java:218-237` 仍可转入 TOOL_CALLING，但该状态恒空转回 THINKING |
| REQ-05 | contradicted / High | 维持 | 全仓 `failedToolName` 生产写入方为 0，重试上下文必然 skip 全部调用（重放 0 个），与 A §5.2"仅重放失败的 ToolCall"直接相反 | grep 全仓：生产读 1（`ToolCallingStateHandler.java:287`）、生产写 0、测试设置 2（`ToolCallingStateHandlerTest.java:312,372`）；`RetryingStateHandler.java:102` 仅写 `retryContext`，其 javadoc 自述"仅重放失败调用"但机制缺写入方 |
| REQ-07 | partial / High | 维持 | `snapshotId` 恒被置为 executionId，而快照行主键为独立 BIGSERIAL/雪花 ID，`selectById` 必然查无 → 生产恢复路径必然 FAILED；A §5.2 无豁免 | `PausedStateHandler.java:79,91`（两分支均=executionId）；`ExecutionSnapshotPersistence.java:23-38`（void，生成主键被丢弃）；`02-init-schema-agent.sql:92`（`id BIGSERIAL`）+ `BaseEntity` `@TableId(ASSIGN_ID)`；`ResumingStateHandler.java:44-48`；两处 resume 端点均汇聚到 `AgentExecutionLifecycleService.resumeExecution` L78 |
| REQ-09 | contradicted / Critical | 维持 | 两处调用点均传 `Map.of` 不可变 Map，发布器首行 `payload.put` 必抛 `UnsupportedOperationException`，经中间件重抛后状态机兜底，全部 GATE_BLOCKED 场景退化为 FAILED 且事件不达 MQ；无子类/Bean 覆盖，`@SneakyThrows` 不吞运行时异常 | `GateBlockedStateHandler.java:56,70`（Map.of）；`AgentExecutionEventPublisher.java:23`（put）；`MiddlewarePipeline.java:86`（throw e）；`AgentStateMachine.java:85-88`（→FAILED）；倒计时硬编码 60 无 `@Value`（L21）；全仓 12 处 GATE_BLOCKED 转移均受波及；测试用 mock 发布器掩盖 |
| REQ-20 | partial / High | 维持 | ContextInjector 三个构造器均无 @Autowired → Spring 走无参构造 → RAG 依赖恒 null 返回 ""；叠加 @Async 无 TaskDecorator、TenantContextHolder 为裸 ThreadLocal 且执行链无人设置，`inject()` 恒提前返回；B 所述 "no-op" 问题在运行时仍成立，无条件子句 | `ContextInjector.java:36-39,73-77,188-191`；`MilvusIsolationService` 为裸类无注解、`EmbeddingService` 在引擎类路径无实现（pom 仅依赖 common/model/dao，`scanBasePackages` 限引擎包）；`AgentExecutionAsyncConfig.java:16-28` 无 TaskDecorator；入口（`AgentExecutionController:39-43`、`mq/AgentExecuteDispatcher:21-30`、`AgentExecutionEngine:25-27`）均只以参数传 tenantId，不设租户上下文 |
| REQ-22 | partial / High | 维持 | 全仓（含跨模块）不存在任何指向 PLANNING 的转移，READY 直转 THINKING，PlanningStateHandler 永不被调度、`subTaskPlan` 恒 null；B §3"between READY and THINKING" 运行时不成立 | 全部 `transition(AgentExecutionState.*` 目标枚举无 PLANNING；`ReadyStateHandler.java:19`（→THINKING）；`InitializingStateHandler.java:18`（→READY）；`ThinkingStateHandler.java:569-573`（plan null → 直接 COMPLETED）；`PlanningStateHandler` 生产引用仅自身文件 |

**复核结论**：6 条全部维持，0 条推翻。其中 REQ-07 另获独立补强证据（快照表 `id BIGSERIAL` 与雪花 executionId 数值空间不可能碰撞，且 `saveSnapshot` 将 `tenantId` 置 null，租户拦截语义下该行更难被命中）。
