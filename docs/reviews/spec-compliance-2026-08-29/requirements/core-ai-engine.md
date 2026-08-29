# Spec 合规核查 — core-ai-engine (Phase 2)

- **规格文档**: `D:\code_space\frige\docs\specs\core-ai-engine.md`（front-matter `status: draft`，v1.0，2026-05-01）
- **核查目标**: `schemaplexai-agent-engine/src/main/java`（含 web/model/common 模块、`application.yml`、`docker/postgres/init/*.sql`、`db/migration/*.sql` 中的执行点）
- **核查日期**: 2026-08-29 · 按当前工作树状态核查（`AgentExecutionLifecycleService.java`、`AgentRuntimeOrchestrator.java` 有未提交修改：暂停/恢复/取消的 Redis Key 已改为租户作用域 `TenantRedisKeyResolver.executionPaused(...)`，本报告以工作树版本为准）
- **裁决标尺说明**: 规格为草案（draft），未实现项严重度默认不高于 Medium，除非涉及数据一致性。

---

## REQ-01 — 执行启动异步化（@Async("agentExecutionExecutor")，实际执行异步进行）

> "`AgentExecutionEngine.startExecution()` 标记 `@Async("agentExecutionExecutor")`" / "返回时 `execution.state = INITIALIZING`，实际状态转换由 orchestrator 驱动" / "Response: Result<Long> // 返回 executionId，实际执行异步进行" — §3.1

**Verdict:** contradicted · confidence: high

**What this demands:** HTTP/MQ 触发执行后立即返回，执行循环运行在 `agentExecutionExecutor` 线程池。

**Where enforcement lives:** `AgentExecutionEngine.java:23-40` — `@Async` 标注在 `runExecutionAsync()`（L23-27），而非 `startExecution()`；`startExecution()`（L30-40）在 L38 以 `runExecutionAsync(execution, tenantId, prompt)` 裸调用（即 `this.` 自调用）。

**Paths walked:**
- ✓ 线程池 bean 存在（见 REQ-02）
- ✗ REST 路径：`AgentExecutionController.java:43` 通过 `AgentExecutionStarter` 代理调用 `startExecution` → 内部 `this.runExecutionAsync(...)` 绕过 Spring AOP 代理 → `orchestrator.run()` 在 HTTP 请求线程同步跑完整个状态机循环
- ✗ MQ 路径：`AgentExecuteDispatcher.java:24` 同样调用 `startExecution` → 同样同步
- ✓（唯一真正异步的调用方）`SubAgentExecutionService.java:62` 通过 `ObjectProvider<AgentExecutionRunner>` 拿到代理对象调用 `runExecutionAsync`，@Async 仅在子执行路径生效

**How the verdict was reached:** 不是 absent——@Async 注解与执行器都存在；不是 partial——主启动路径 100% 同步，异步只在子 Agent 路径生效。经典自调用绕过代理。`pom.xml` 无 aspectj 织入、`@EnableAsync` 为默认代理模式（`AgentExecutionAsyncConfig.java:11`），排除了 AspectJ 模式补救的可能。后果：POST /agents/{id}/execute 阻塞整个执行时长；严重度 High（P0 核心行为失效，属实现缺陷而非草案未实现项）。

---

## REQ-02 — agentExecutionExecutor 线程池（隔离队列 + 拒绝策略）

> "`@Async` 线程池 | 不存在 | `agentExecutionExecutor` bean，隔离队列+拒绝策略 | P0" — §6

**Verdict:** implemented · confidence: high

**What this demands:** 名为 `agentExecutionExecutor` 的独立线程池，有界队列 + 明确拒绝策略。

**Where enforcement lives:** `AgentExecutionAsyncConfig.java:12-28` — `@EnableAsync` + `@Bean(name = "agentExecutionExecutor")`：corePoolSize=10、maxPoolSize=50、queueCapacity=200、`CallerRunsPolicy`（L23）、优雅关闭（L24-25）。

**Paths walked:**
- ✓ bean 定义与命名
- ✓ 队列容量 200（隔离队列）
- ✓ 拒绝策略 `CallerRunsPolicy`
- 注：受 REQ-01 影响，主路径任务根本不经过此池；`CallerRunsPolicy` 一旦触发会在调用线程回退执行，属配置权衡不构成违规。

**How the verdict was reached:** 需求三要素（命名、队列、拒绝策略）全部满足，判 implemented；池未被主路径使用的问题记在 REQ-01。

---

## REQ-03 — startExecution 返回时 state = INITIALIZING

> "返回时 `execution.state = INITIALIZING`" — §3.1

**Verdict:** contradicted · confidence: high

**What this demands:** 持久化的初始状态为 `INITIALIZING`。

**Where enforcement lives:** `AgentExecutionEngine.java:34` — `execution.setState(AgentExecutionState.QUEUED.name())`，随后 insert。`QUEUED` 不在规格 11 状态集内（§5.1）。数据库列默认值 `DEFAULT 'INITIALIZING'`（`02-init-schema-agent.sql:67`）被代码显式覆盖为 `QUEUED`。`INITIALIZING` 实际由 `AgentStateMachine.start()` 在 orchestrator 内部触发（`AgentRuntimeOrchestrator.java:116`）。

**Paths walked:**
- ✗ 返回对象状态 = QUEUED（非 INITIALIZING）
- ✓ 后续 orchestrator 会 transition 到 INITIALIZING（若非准入拒绝）

**How the verdict was reached:** 状态名与规格明确不符且多出一个规格外状态；功能上"创建后由 orchestrator 驱动"部分达成，故不升为 High。严重度 Low（草案、无数据一致性后果）。

---

## REQ-04 — SSE 事件流（路径 / Bearer 鉴权 / UnifiedMessage + 8 个事件名）

> "GET /agents/execute/stream?executionId={id}"、"Authorization: Bearer {token}"、"使用 `UnifiedMessage` + `MessageType.SSE_EVENT`"、事件表 `THINKING_STARTED`/`THINKING_CHUNK`/`TOOL_CALLING`/`TOOL_RESULT`/`PAUSED`/`COMPLETED`/`FAILED`/`ERROR` — §3.2

**Verdict:** partial · confidence: high

**What this demands:** 指定路径的 SSE 端点、Bearer 头鉴权、载荷为 UnifiedMessage、8 个约定事件名。

**Where enforcement lives:**
- engine 端点：`AgentExecutionController.java:78-96` — `GET /agents/{id}/executions/{execId}/events?token=...`，token 为 **URL 查询参数**，由 `JwtSseTokenValidator.java:63-107` 校验（JWT 签名 + sub==executionId）
- web 端点：`schemaplexai-web/.../SseController.java:39-55` — `GET /sse/executions/{executionId}/events`（Authorization 头、支持回放）
- 事件广播：`ExecutionEventBus.java:54-152` — 事件名为 `state-transition` / `execution-completed` / `thought` / `tool-call` / `tool-result` / `plan` / `output` / `error`，载荷为普通 Map JSON

**Paths walked:**
- ✓ SSE 能力存在（两处端点、emitter 注册/注销/超时清理、密钥脱敏 `SecretMasker`）
- ✗ 路径不符（`/agents/execute/stream` 无任何实现）
- ✗ 8 个事件名 0 个匹配（大小写与命名全部不同）
- ✗ 载荷未用 `UnifiedMessage`/`MessageType.SSE_EVENT`（该类存在于 common 模块，仅测试引用，见 `schemaplexai-common/src/test/.../UnifiedMessageTest.java`）
- ✗ engine 端点 JWT 走 URL 参数而非 Bearer 头（与 agents.md 已知安全缺陷一致）
- ✗ 无 `THINKING_CHUNK` 对应的流式片段：LLM 响应一次性返回后才发 `output` 事件（`ThinkingStateHandler.java:195-198`）

**Searched:** `execute/stream` → 仅规格/文档命中；`THINKING_STARTED|THINKING_CHUNK` → 仅规格文档命中，代码 0 命中。

**How the verdict was reached:** SSE 基础设施齐备但契约面（路径/鉴权方式/事件名/载荷格式）全部漂移，判 partial 而非 contradicted：能力存在、契约不符。严重度 Medium（草案）。

---

## REQ-05 — pause/resume/cancel 的状态校验

> "仅 RUNNING/THINKING/TOOL_CALLING 可暂停"、"仅 PAUSED 可恢复"、"非终端状态均可取消" — §3.3

**Verdict:** partial · confidence: high

**What this demands:** 三个操作分别校验当前状态合法性，非法时拒绝。

**Where enforcement lives:** `AgentExecutionLifecycleService.java:57-111`（工作树版本）；对外暴露于 `AgentExecutionController.java:47-66`（路径 `/agents/{id}/executions/{execId}/...`）与 web `ExecutionWebController.java:48-66`（`/web/executions/{id}/...`，经 `EngineExecutionLifecyclePort.java` 委托同一服务）。

**Paths walked:**
- ✗ pause：无任何当前状态检查（L57-67），QUEUED/INITIALIZING/GATE_BLOCKED 均可暂停；仅 `AgentStateMachine.transition` 的终态守卫（对 COMPLETED/FAILED/CANCELLED/REJECTED）兜底
- ✗ resume：无"仅 PAUSED"校验（L69-80），对非 PAUSED 执行也会删除暂停 Key 并 transition 到 RESUMING
- ✓/✗ cancel：`AgentStateMachine.java:48` 终态守卫阻止从终态取消（规格意图满足），但代码中 GATE_BLOCKED 非终态（见 REQ-09），故 GATE_BLOCKED 也可被取消，与规格终态集冲突
- ✗ 暂停/恢复的持久化链路缺陷：`PausedStateHandler.java:91` 将 `snapshotId` 设为 **executionId** 而非快照行 ID；`ResumingStateHandler.java:44` 用它 `selectById` 查快照——几乎必然查不到 → resume 走 FAILED（L45-48）；且 `SfAgentExecution.snapshotId`（`SfAgentExecution.java:33`）在全部 DDL/迁移中无对应列（`grep snapshot_id db/migration + docker/postgres/init` 0 命中），`updateById` 会生成不存在的列

**How the verdict was reached:** 操作与端点齐全（机制存在）但三条状态校验全部缺失、暂停-恢复往返存在实现缺陷，判 partial。严重度 Medium（草案；状态校验缺失可致非法生命周期操作被接受）。

---

## REQ-06 — GET 执行状态 + 当前 token 消耗

> "GET /agents/execution/{id} // 查询完整状态 + 当前 token 消耗" — §3.3

**Verdict:** partial · confidence: high

**What this demands:** 查询端点返回完整状态与当前 token 消耗。

**Where enforcement lives:** web `ExecutionWebController.java:32-35` — `GET /web/executions/{id}` 返回 `ExecutionStatusVO`；VO 含 `state`/`currentRound`/`consumedTokens`（`ExecutionStatusVO.java:13-30`）；装配在 `schemaplexai-web/.../mapper/ExecutionMapper.java:13-24`。

**Paths walked:**
- ✓ 状态查询存在（路径不同：`/web/executions/{id}`，非 `/agents/execution/{id}`；engine 模块无此端点）
- ✗ `toStatusVO` 只填 executionId/agentId/state/createdAt/updatedAt——`consumedTokens`、`currentRound`、`agentName` 从未赋值，响应中恒为 null

**How the verdict was reached:** 能力一半存在（状态）一半空转（token 消耗），判 partial。严重度 Medium。

---

## REQ-07 — 状态枚举为 11 状态

> §5.1 状态定义表（11 状态）；"状态机 | `sf_agent_execution.state` 枚举值" — §4.2

**Verdict:** contradicted · confidence: high

**What this demands:** 状态集 = {INITIALIZING, READY, THINKING, TOOL_CALLING, OBSERVATION, PAUSED, GATE_BLOCKED, RETRYING, COMPLETED, FAILED, CANCELLED}。

**Where enforcement lives:** `AgentExecutionState.java:3-21` — 实际 18 个状态：规格 11 个全在，另多 `QUEUED`、`PLANNING`、`RESUMING`、`REFLECTING`、`HANDOFF`、`GROUP_CHAT`、`REJECTED`。

**Paths walked:**
- ✓ 规格 11 状态均可表达
- ✗ 多出 7 个规格外状态，其中 `RESUMING`/`PLANNING`/`REFLECTING`/`HANDOFF`/`GROUP_CHAT` 有对应 Handler 参与实际流转（`ResumingStateHandler.java:93`、`ObservationStateHandler.java:57` 等）

**How the verdict was reached:** 超集本身危害小，但它是 REQ-09（终态集）、REQ-08（转换矩阵）漂移的根源，且规格对转换矩阵的约定未覆盖新状态。严重度 Low（草案，文档漂移方向）。

---

## REQ-08 — 非法状态转换抛 BaseException、状态不变（转换矩阵约束）

> "状态非法转换 | PAUSED → TOOL_CALLING（不允许） | 抛出 BaseException，不改变状态 | 400" — §7；"状态转换必须由 `AgentStateMachine.transition()` 统一入口执行" — §5.2

**Verdict:** partial · confidence: high

**What this demands:** 存在转换矩阵校验；非法转换抛 `BaseException` 且状态回滚/不变。

**Where enforcement lives:** `AgentStateMachine.transition()`（`AgentStateMachine.java:46-52`）——唯一校验是"终态 → 非 FAILED 拒绝"，且仅 `log.warn` + return，**不抛异常**。

**Paths walked:**
- ✓ 唯一入口性：所有流转（orchestrator、各 Handler、lifecycle）均经 `transition()`（Handler 内 `setState+saveExecution` 仅重写同一状态，如 `GateBlockedStateHandler.java:52`、`CompletedStateHandler.java:16-18`）
- ✗ 无矩阵：PAUSED → TOOL_CALLING、QUEUED → COMPLETED 等任意非终态间转换均被接受
- ✗ 不抛异常：非法（终态）转换只记日志（L49-51），调用方拿到正常返回
- ✗ 终态 → FAILED 被显式放行（L48 `newState != FAILED`），规格规定终态"不可再转换"无例外

**Searched:** `ALLOWED_TRANSITIONS|validateTransition|legalTransition|transitionMatrix|canTransition` → engine 模块 0 命中。

**How the verdict was reached:** 有入口统一性和终态守卫（部分约束），但矩阵与异常语义完全缺失，判 partial 而非 absent。严重度 Medium（草案；缺失矩阵是 REQ-09 绕过链得以成立的前提）。

---

## REQ-09 — 终态集（含 GATE_BLOCKED）不可再转换 + 终态触发 removeExecution 清理

> "终端状态（`COMPLETED`/`FAILED`/`CANCELLED`/`GATE_BLOCKED`）不可再转换"、"终端状态触发 `AgentStateMachine.removeExecution()` 内存清理" — §5.2 关键约束

**Verdict:** contradicted · confidence: high

**What this demands:** GATE_BLOCKED 为终态；任何终态不再流转并清理内存。

**Where enforcement lives:**
- `AgentExecutionState.java:23-25` — `isTerminal()` = {COMPLETED, FAILED, CANCELLED, REJECTED}，**不含 GATE_BLOCKED**（多出规格外的 REJECTED）
- `GateBlockedStateHandler.java:49-67` — GATE_BLOCKED 被当作可重试：置 60s 倒计时后 `transition(RETRYING)`；仅 `admissionType == "FATAL"` 才转 FAILED（L68-78）
- `AgentStateMachine.java:100-104` — 终态清理逻辑本身存在（publishExecutionCompleted + eventBus.complete + removeExecution）

**Paths walked:**
- ✗ 准入拒绝路径（数据一致性后果）：`AgentRuntimeOrchestrator.java:106-110` transition(GATE_BLOCKED) → handler 链同步执行：GATE_BLOCKED（admissionType 未设置 → 视为可重试）→ RETRYING（`RetryingStateHandler.java:102-103`，errorCategory 为空不拦截）→ 退避后 TOOL_CALLING → `ToolCallingStateHandler.java:81-85`（无消息）→ **COMPLETED**。即被准入拒绝的执行最终标记为 COMPLETED，准入门禁被完全绕过
- ✗ GATE_BLOCKED 未触发 removeExecution（非终态），内存表项仅在后续到达真终态时移除；若停在 GATE_BLOCKED→RETRYING 循环则依赖重试计数兜底
- ✓ COMPLETED/FAILED/CANCELLED 触发清理（L100-104）
- ✗ 终态 → FAILED 放行（L48），违反"不可再转换"

**How the verdict was reached:** 不是文档漂移——规格明示 GATE_BLOCKED 为终端且代码注释亦自认将转入 RETRYING（`GateBlockedStateHandler.java:13-15`），行为与规格正面冲突，并产生"准入拒绝仍完成执行"的成本门禁失效后果。严重度 High（涉及成本/准入数据一致性，不受草案降级保护）。

---

## REQ-10 — 状态转换统一入口 + isTerminal() 定义

> "状态转换必须由 `AgentStateMachine.transition()` 统一入口执行"、"`AgentExecutionState.isTerminal()` 定义终端状态判断逻辑" — §5.2

**Verdict:** implemented · confidence: medium

**What this demands:** 机制存在：统一入口方法与终态判断方法。

**Where enforcement lives:** `AgentStateMachine.java:46`（`transition` 唯一入口，含 DB 持久化 + 乐观锁 + 事件发布 + Handler 调度）；`AgentExecutionState.java:23-25`（`isTerminal()`）。

**Paths walked:**
- ✓ orchestrator（`AgentRuntimeOrchestrator.java:108,116,124,131,139,146,151`）、lifecycle（`AgentExecutionLifecycleService.java:65,78,108`）、全部 StateHandler 均走 `transition()`
- 注：`transition()` 允许"转入当前相同状态"（orchestrator 循环 `transition(currentState, ...)` L139 会重放当前 Handler）——规格未约定，记录为未文档化行为

**How the verdict was reached:** 入口与判断逻辑存在并被一致使用（机制满足），终态集合内容的分歧已归入 REQ-09，避免重复计分。严重度 —。

---

## REQ-11 — RetryingStateHandler（重试、指数退避、最大重试次数）

> "`RetryingStateHandler` | 不存在（文件缺失） | 重试逻辑、指数退避、最大重试次数 | P2" — §6；§5.2 "RETRYING → THINKING 重试完成（预留）"

**Verdict:** implemented · confidence: high

**What this demands:** 文件存在，实现重试决策、指数退避、最大次数上限。

**Where enforcement lives:** `RetryingStateHandler.java:49-104` — 可配置 `agent.retry.*`（enabled/maxRetries=3/baseDelayMs=100/maxDelayMs=30000，L32-41）；指数退避 `min(base*2^(n-1), max)`（L89）；超限转 FAILED（L71-76）；连续失败 3 次熔断（L79-86）；携带 `retryContext` 转 TOOL_CALLING 只重放失败工具（L101-103 + `ToolCallingStateHandler.java:136-143`）。

**Paths walked:**
- ✓ 重试禁用路径（L52-56）
- ✓ 不可重试错误类别直转 FAILED（L59-66）
- ✓ 超过 maxRetries（L71-76）与熔断（L79-86）
- 注：出口为 TOOL_CALLING 而非规格的"预留" THINKING——不同机制满足"重试完成后继续"语义（文档漂移注记）；熔断/可重试分类为超规格增强

**How the verdict was reached:** P2 缺件已补齐且路径完整，判 implemented（含 stronger-than-spec 成分：熔断器、错误类别可重试性）。

---

## REQ-12 — ThinkingStateHandler：真实 LLM、精确 Token 估算、流式输出

> "骨架（estimateTokens 粗糙，无真实 LLM 调用） | 接入真实 LLM、Token 精确估算、流式输出 | P0" — §6

**Verdict:** partial · confidence: high

**What this demands:** 三项：真实 LLM 调用；优于"字符/4"的精确估算；流式输出。

**Where enforcement lives:** `ThinkingStateHandler.java:114-253`。

**Paths walked:**
- ✓ 真实 LLM：`modelRouter.generateWithFallback(prompt, modelId, 0.7)`（L195）；推理策略委托路径 `executeWithStrategy`（L311-369）
- ✗ Token 估算：仍是 `TokenEstimator.estimate`（L163, util/TokenEstimator.java，字符近似），规格点名的"粗糙"问题未解决
- ✗ 流式：`LlmProvider`/`AiModelRouter` 无任何 streaming API（model 包内 `stream` 仅命中 Java Stream），响应整段返回后一次性发事件（L197-198）
- ✓ 预算门/护栏/循环检测/记忆持久化路径齐全（L124-173, L213-246）

**How the verdict was reached:** 三缺一半（1/3 达成、估算未改进、流式缺失），判 partial。严重度 Medium（草案 P0，但真实调用主链路已通）。

---

## REQ-13 — ToolCallingStateHandler：工具注册表、并行读/串行写、超时控制

> "骨架（parseToolCalls 返回空列表） | 工具注册表、并行读/串行写策略、超时控制 | P0" — §6

**Verdict:** partial · confidence: high

**What this demands:** 三项：注册表；读并行/写串行调度策略；超时控制。

**Where enforcement lives:** `ToolCallingStateHandler.java:73-201`。

**Paths walked:**
- ✓ 注册表：`toolRegistry.parse(...)` 结构化解析（L95，OpenAI/Anthropic 解析器 `tool/parser/*`）+ `resolve()` 白名单解析（`executeToolWithGuard` L209-214）
- ✗ 并行读/串行写：工具在单个 for 循环中串行执行（L137-188）；无读写分类（`readOnly|mutating|isWrite` 在 ToolDefinition/ToolRegistry 0 命中）
- ✓/部分 超时：适配层各自实现——`HttpCallAdapter.java:36`（5s/30s）、沙箱 `ShellCommand.java:14-30`（超时参数）；Handler 层无统一每工具超时
- ✓ 安全链：安全守卫（L224-230）、审批门（L149-153）、工具调用预算（L104-123）、循环检测（L126-133）

**Searched:** `parallel|Parallel` 于 tool/state 包 → 仅 `McpToolDiscoveryService` 命中（与工具调度无关）。

**How the verdict was reached:** 注册表与超时（分层）达成，调度策略整项缺失，判 partial。严重度 Medium（草案）。

---

## REQ-14 — LlmProvider / AiModelRouter：LangChain4j 封装、generateWithFallback、流式

> "骨架（fallback 逻辑存在，generate 未实现） | LangChain4j 封装、generateWithFallback 完整实现、流式输出 | P0" — §6

**Verdict:** partial · confidence: high

**What this demands:** LangChain4j 防腐层、完整 fallback 链、流式输出。

**Where enforcement lives:**
- LangChain4j：`OpenAiProvider.java:60-74`（`OpenAiChatModel.builder()` + 超时 + 重试）、`AnthropicProvider`、公共基类 `LlmProviderAdapter`（generate/generateWithMessages/generateWithTools + 模型缓存）；依赖 `langchain4j 0.31.0`（根 `pom.xml:50`、engine `pom.xml:17-19`）
- Fallback：`AiModelRouter.java:42-64` — 主 provider 失败 → 5 分钟冷却（Redis `sf:model:cooldown:*`，L120-123）→ 遍历健康 provider；三个生成方法同构实现（L66-113）

**Paths walked:**
- ✓ generate 已实现（非骨架）；健康检查（`OpenAiProvider.java:87-111`）；mock profile 替身（`MockLlmProvider.java`）
- ✓ 全失败抛 `IllegalStateException` → 上层 `ThinkingStateHandler` catch 转 FAILED
- ✗ 流式输出：无任何流式接口（同 REQ-12 证据）

**How the verdict was reached:** 两项达成、流式缺失，判 partial。严重度 Medium（草案）。

---

## REQ-15 — AgentLoopDetectionService：哈希检测 + 工具序列检测

> "骨架（简单重复消息检测） | 哈希检测、工具序列检测、准确率 > 95% | P1" — §6

**Verdict:** implemented · confidence: high

**What this demands:** 基于响应哈希与工具序列两类信号的循环检测（可核查的机制部分）。

**Where enforcement lives:** `AgentLoopDetectionService.java:33-72` — 滑动窗口 `window-size=5`（默认，L25），窗口内同哈希 ≥3 次判哈希循环（L44-53），同工具序列 ≥3 次判序列循环（L57-68）；参数可配置 `agent.loop-detection.*`（L25-27）；`clearRecords` 在直接回答完成时调用（`ThinkingStateHandler.java:244`）。

**Paths walked:**
- ✓ 窗口不足 5 轮不判定（L37-39）
- ✓ 空工具名列表跳过序列检测（L57）
- ✓ 调用方：ThinkingStateHandler（L221-231）与 ToolCallingStateHandler（L128-133）双入口
- 注：规格字面"5 轮内重复"，代码要求窗口内重复 ≥3 次——更保守的阈值（可配置），视为机制满足；准确率 >95% 属运行时指标，列入 notChecked

**How the verdict was reached:** 机制完整、可配置、双检测器齐备，判 implemented（阈值解释差异注记）。触发后的行为分歧归 REQ-16。

---

## REQ-16 — 循环检测触发 → 强制 COMPLETED，reason = LOOP_DETECTED

> "循环检测触发 | 5 轮内哈希或工具序列重复 | 强制转 COMPLETED，reason = LOOP_DETECTED | 200" — §7

**Verdict:** contradicted · confidence: high

**What this demands:** 检出循环后终止于 COMPLETED 并携带 LOOP_DETECTED 原因。

**Where enforcement lives:**
- `ThinkingStateHandler.java:224-231` — 检出后 `blockedReason=agent_loop_{HASH_LOOP|TOOL_SEQUENCE_LOOP}` + `admissionType=LOOP` → **GATE_BLOCKED**
- `ToolCallingStateHandler.java:129-133` — 同样转 GATE_BLOCKED
- 后续：GATE_BLOCKED → RETRYING → TOOL_CALLING（REQ-09 路径）→ 循环检测再次触发 → 反复直至 `RetryingStateHandler` 重试上限 → **FAILED**

**Paths walked:**
- ✗ 无一路径到达 COMPLETED + LOOP_DETECTED
- ✗ 原因字符串为 `agent_loop_HASH_LOOP`/`agent_loop_TOOL_SEQUENCE_LOOP`，仅存于瞬态 metadata（`SfAgentExecution.metadata` 为 `transient`，`SfAgentExecution.java:30`），不落库
- `PauseReason.LOOP_DETECTED`（`PauseReason.java`）存在但循环处理路径未使用

**How the verdict was reached:** 终态与规格相反（GATE_BLOCKED/FAILED vs COMPLETED），判 contradicted。严重度 Medium（草案；循环最终仍能终止，但借道重试计数且原因不持久）。

---

## REQ-17 — CompositeChatMemoryStore：L1 Redis List + L2 PostgreSQL + 压缩

> "骨架（仅 Redis L1，无 L2 PG 层） | L1 Redis List、L2 PostgreSQL、压缩策略 | P1" — §6

**Verdict:** partial · confidence: high

**What this demands:** 双层存储齐备且压缩在两层一致生效。

**Where enforcement lives:**
- L1：`CompositeChatMemoryStore.java:42-49`（Redis List range + trim 50 条 + 7d TTL）
- L2：`saveMessage` 先 PG 后 Redis（L106-138，`SfChatMessageMapper.insert`）；L1 miss 回源 PG 并回填（L68-91，含 backfill 锁 L52-65）；租户加密（`TenantKeyService`）
- 压缩：`AutoCompactionService.java:33-91` — Layer0 工具结果压缩 → Layer1 滑动窗口 → Layer2 摘要 + 25% 头部截断重试（≤3 次）

**Paths walked:**
- ✓ 读：L1 命中 / L1 miss→L2→回填 / 并发回填锁
- ✓ 写：PG 成功后 Redis，Redis 失败仅告警（L135-137，L2 为准，正确）
- ✗ 压缩一致性：`replaceMessages`（L217-236）只删/重写 **Redis**，PG 全量历史原样保留 → L1 过期/逐出后 `loadMessages` 从 PG 恢复未压缩历史，压缩效果丢失（跨层数据不一致）

**How the verdict was reached:** 三层组件都存在（非 absent），但压缩只作用于 L1 缓存层、L2 永不压实——具体缺失路径明确，判 partial。严重度 Medium（涉及数据一致性，草案下不升级）。

---

## REQ-18 — ExecutionAdmissionService：配置化阈值 + tenant/model/provider 维度

> "骨架（仅 rate + concurrency + token + cost 四维，阈值硬编码） | 配置化阈值、tenant/model/provider 维度完整实现 | P1" — §6

**Verdict:** partial · confidence: high

**What this demands:** 阈值可配置；维度覆盖 tenant、model、provider。

**Where enforcement lives:** `ExecutionAdmissionService.java:26-95`。

**Paths walked:**
- ✗ 配置化：速率上限 `60` 硬编码（L33）、并发上限 `5` 硬编码（L43）——规格点名的"阈值硬编码"问题未解决
- ✗ 维度：仅 tenant+agent 级（rate/concurrency，Key 由 `TenantRedisKeyResolver.admissionRate/Concurrency(tenantId, agentId)` 构成）；无 model 维度、无 provider 维度（`admit` 签名不接收 modelId）
- ✓ 超规格维度：成本预算 `BudgetGuard`（L66-81）、租户日工具调用预算（L84-89）、token 预算检查（L52-64）

**How the verdict was reached:** 维度数量增加但规格要求的两个方向（配置化、model/provider 维度）都未达成，判 partial。严重度 Medium（草案）。

---

## REQ-19 — 准入拒绝 → GATE_BLOCKED + 返回 admission.reason + 429

> "准入拒绝 | 租户/Agent/模型并发超限 | 状态转 GATE_BLOCKED，返回 admission.reason | 429" — §7

**Verdict:** partial · confidence: high

**What this demands:** 拒绝时状态落 GATE_BLOCKED、原因可获取、调用方收到 429。

**Where enforcement lives:** `AgentRuntimeOrchestrator.java:105-110` — `admission.isAllowed()` 为假 → `log.warn` 记录 reason → transition(GATE_BLOCKED)。

**Paths walked:**
- ✓ 状态转 GATE_BLOCKED
- ✗ reason：`admission.getReason()` 只进日志，未写入 `blockedReason` metadata（Thinking/ToolCalling 的阻塞路径会写，准入这条不写）→ `GateBlockedStateHandler.java:40-43` 兜底为 "admission_denied"，具体原因丢失；且 metadata 为瞬态不持久（`SfAgentExecution.java:30`）
- ✗ 429：拒绝发生在执行启动之后的异步（实际同步）循环内，REST 始终返回 200 + 执行对象（`AgentExecutionController.java:36-45`），无 429 路径
- ✗ 后续演变见 REQ-09（GATE_BLOCKED→…→COMPLETED，门禁失效）

**How the verdict was reached:** 状态动作存在、原因回传与错误码缺失，判 partial；绕过后果已在 REQ-09 计为 High，本条就"原因/429"单独计。严重度 Medium（草案）。

---

## REQ-20 — TokenBudget：超限处理链（压缩→截断→终止）+ 结构化 JSON

> "超限处理链（压缩→截断→终止）、结构化 JSON 序列化 | P1" — §6；"token_budget_json 当前为逗号分隔字符串……Phase 2 计划改为结构化 JSON" — §4.2；"Token 预算不足 | 预检时 input token > 剩余预算 | 触发记忆压缩 → 截断 → 终止 | 507" — §7

**Verdict:** partial · confidence: high

**What this demands:** 超限时按 压缩→截断→终止 递进；预算字段结构化存储。

**Where enforcement lives:**
- 链：`ThinkingStateHandler.java:124-140` 预检触发 `AutoCompactionService.compactIfNeeded`（压缩+滑动窗口+摘要）→ 摘要失败重试内 `truncateHead` 25%（截断，`AutoCompactionService.java:86-99`）→ 链耗尽 `compaction_failed` → GATE_BLOCKED（终止的替代物，非规格 507 语义）
- 消费门：`budget.consumeInput/consumeOutput` CAS（`TokenBudget.java:27-51`），超限 → GATE_BLOCKED（`ThinkingStateHandler.java:166-173, 200-208`）
- 序列化：混合格式——orchestrator 启动时写**旧 CSV** `"maxIn,maxOut,0,0,maxToolCalls,0"`（`AgentRuntimeOrchestrator.java:162-164`，6 段，规格记的是 4 段）；ThinkingStateHandler 之后写 JSON（L544-558）并双格式解析（L500-541）

**Paths walked:**
- ✓ 压缩→截断两级存在且按预算余量收敛
- ✗ "终止"落为 GATE_BLOCKED（其后又按 REQ-09 被重试），无 507 错误码路径
- ✗ 结构化 JSON 未贯彻：首写仍为 CSV
- ✗ 字段污染：`ObservationStateHandler.java:81-94` 把 `",iterations=N"` 追加进 `token_budget_json`（甚至整体写成 `"iterations=1"`），破坏 JSON/CSV 两种格式的可解析性 → `ThinkingStateHandler.loadBudget` 解析失败返回 null（L538-541），预算门随之失效

**How the verdict was reached:** 处理链骨架存在（非 absent）但终止语义、序列化一致性均漂移，且存在破坏该字段的可运行路径，判 partial。严重度 Medium（token_budget_json 字段被迭代计数器复用属数据一致性缺陷，草案封顶 Medium）。

---

## REQ-21 — LLM 调用超时 30s → FAILED + 释放并发（504）

> "LLM 调用超时 | 30s 无响应 | 标记失败，状态转 FAILED，释放并发 | 504" — §7

**Verdict:** partial · confidence: high

**What this demands:** 30s 超时、失败转 FAILED、并发释放、504。

**Where enforcement lives:**
- 超时：`LlmProviderProperties.java:25` 默认 **60s**（`application.yml:60,65` 可配 `OPENAI_TIMEOUT`）；传入 `OpenAiChatModel.timeout(...)`（`OpenAiProvider.java:70`）
- 失败路径：超时异常 → `AiModelRouter` 全 provider 失败抛出 → `ThinkingStateHandler.java:247-252` catch → FAILED
- 释放并发：`AgentRuntimeOrchestrator.java:155-159` finally 中 `releaseConcurrency`（任何退出路径都释放）

**Paths walked:**
- ✗ 默认 60s ≠ 30s（可配置，未配置到规格值）
- ✓ 超时 → FAILED → 并发释放 链路完整
- ✗ 无 504 语义映射（执行态失败，非 HTTP 错误码）
- ✗ 附带缺陷：准入拒绝路径同样执行 `releaseConcurrency`，而 admit 已在拒绝时回退过计数器（`ExecutionAdmissionService.java:44`）→ 计数器双减，可为负（`ExecutionAdmissionService.java:97-100` 无下界保护）

**How the verdict was reached:** 行为链正确、参数与错误码不符，判 partial。严重度 Low（草案、参数性差异；双减计数器问题并记于此）。

---

## REQ-22 — 工具失败：单工具异常继续其他工具，全失败转 FAILED

> "工具执行失败 | 单工具异常 | 记录错误，继续其他工具；全失败则转 FAILED | 500" — §7

**Verdict:** contradicted · confidence: high

**What this demands:** 批内容错：一个工具失败不中断其余工具；仅当全部失败才 FAILED。

**Where enforcement lives:** `ToolCallingStateHandler.java:155-168` — 首个失败即分流：可重试错误类别 → `transition(RETRYING); return`（L163-165）；否则 `transition(FAILED); return`（L166-168）。`return` 直接跳出工具循环，后续工具不再执行。

**Paths walked:**
- ✗ "继续其他工具"：无任何失败继续路径（blocked 亦整批中止，L171-178）
- ✗ "全失败则转 FAILED"：单个失败即 RETRYING/FAILED，无成功/失败计数聚合
- ✓ 错误有记录（`executionRecorder.record` L156、L196-198 落 `sf_agent_execution_log`）

**How the verdict was reached:** 与规格行为模型正面冲突（快速失败+重试 vs 批内容错），判 contradicted。严重度 Medium（草案）。

---

## REQ-23 — 外部取消：当前轮完成后转 CANCELLED + 资源清理

> "执行被外部取消 | CANCELLED 信号 | 当前轮完成后转 CANCELLED，清理资源 | 200" — §7

**Verdict:** partial · confidence: high

**What this demands:** 取消信号隔离到目标执行、轮边界生效、资源清理。

**Where enforcement lives:** `AgentExecutionLifecycleService.cancelExecution`（L82-111，工作树版本）：删暂停 Key → `orchestrator.cancel()`（L92）→ 关闭沙箱会话（L95-103）→ outbox 取消事件（L106, L113-138）→ transition(CANCELLED)（L108）+ removeExecution（L109）。轮询点：`AgentRuntimeOrchestrator.java:129-133`（每轮迭代检查）。

**Paths walked:**
- ✓ 信号检测与轮边界：循环每轮检查 `cancelled` 标志/中断（L129），符合"当前轮完成后"
- ✓ 资源清理：沙箱会话关闭、状态机表项移除、事件发布
- ✗ **信号隔离缺陷**：`cancelled` 是 orchestrator **单例上的共享 volatile 字段**（`AgentRuntimeOrchestrator.java:53`），`cancel()` 不接收 executionId —— 取消任一执行会置位全局标志，**所有并发执行**在下一轮检查点一并转入 CANCELLED；`resetCancelled()`（L94）在新执行启动时清旗，竞争窗口内互相践踏

**How the verdict was reached:** 机制齐全但隔离性破坏使"取消执行 X"变为"取消所有执行"，具体路径明确，判 partial。严重度 High（跨执行/跨租户误取消，数据一致性后果，不受草案降级保护）。

---

## REQ-24 — sf_agent_execution_log 记录状态转换历史

> "Phase 2 新增 `sf_agent_execution_log` 表记录状态转换历史" — §4.2

**Verdict:** partial · confidence: high

**What this demands:** 表存在；状态转换历史写入该表。

**Where enforcement lives:**
- 表：`02-init-schema-agent.sql:80-89`（execution_id/state/message/details_json）；实体 `SfAgentExecutionLog.java` + mapper
- 实际写入方：`ToolExecutionRecorder.java:24-53` — 仅记录工具失败/阻断事件（state 取值 `TOOL_FAILURE`/`TOOL_BLOCKED`）
- 状态转换记录路径：`AuditMiddleware.java:31-39` → `AuditTrail.record` —— 而 `AuditTrail.java:19,24` 明示 **in-memory、不落库**（"v1 implementation — entries are not persisted to a database"）

**Paths walked:**
- ✓ 表与实体存在、有写入方（工具事件）
- ✗ "状态转换历史"：仅存在于内存 AuditTrail + SLF4J 日志，进程重启即失

**How the verdict was reached:** 表已建且有部分事件入库，但规格指定的"状态转换历史"未入该表，判 partial。严重度 Medium（草案）。

---

## REQ-25 — 状态机内存泄漏为 0（终态清理验证）

> "状态机内存泄漏 | 0 | 终端状态清理验证" — §8

**Verdict:** partial · confidence: medium

**What this demands:** 执行结束后所有按 executionId 的内存结构被清理。

**Where enforcement lives:**
- ✓ `AgentStateMachine.executionStates`：终态路径 `removeExecution`（`AgentStateMachine.java:103`）；取消路径显式清理（`AgentExecutionLifecycleService.java:109`）；FAILED handler 重入兜底清理（`AgentStateMachine.java:94`）
- ✗ `AgentLoopDetectionService.executionRecords`：仅直接回答完成时 `clearRecords`（`ThinkingStateHandler.java:244`）；FAILED/CANCELLED/策略分支未清理（`AgentLoopDetectionService.java:22` 的 Map 无界、无 TTL）
- ✗ `RetryingStateHandler.retryCounters/failureStreaks`：失败路径清理（`RetryingStateHandler.java:114-117`），成功完成路径的 `clearRetryState`（L109-112）无调用方（grep 仅定义处命中）
- ✗ GATE_BLOCKED 非终态（REQ-09）使状态机表项延迟清理依赖后续流转

**Paths walked:**
- ✓ 真终态四个状态的清理
- ✗ 三个泄漏路径如上

**How the verdict was reached:** 主清理存在但有可枚举的泄漏路径，判 partial（运行时验证不可得，confidence medium）。严重度 Medium（草案）。

---

## notChecked（未核查项）

| 项 | 原因 |
|---|---|
| 循环检测准确率 > 95%（§6/§8） | 运行时指标，需集成测试度量，静态不可核查 |
| Token 预算精度误差 < 5%（§8） | 依赖真实分词器对比，静态不可核查 |
| 单执行延迟 P99 < 30s、并发 100+、线程池利用率 < 80%（§8） | 压测指标 |
| 错误码 429/507/504/500 的网关/全局映射 | 已在各 REQ 内注明缺失，未单独展开核查 `schemaplexai-gateway` 错误处理链 |

## Open questions

1. `SfAgentExecutionSnapshot` 插入时 `entity.setTenantId(null)`（`ExecutionSnapshotPersistence.java:31`）——是否依赖 `TenantLineInterceptor` 自动填充？若否，违反 `sf_agent_execution_snapshot.tenant_id NOT NULL`（`02-init-schema-agent.sql:93`）。
2. `AgentStateMachine.transition` 的乐观锁失败回退为直接 `updateById`（`AgentStateMachine.java:65-69`）——并发冲突时静默覆盖，是否符合控制面设计意图？
3. engine 与 web 两套 SSE 机制（`ExecutionEventBus` 直推 vs `SseController` + 事件回放）之间的职责边界，规格未提及。

## 反向差距（代码有、规格未提）

1. **规格外状态与 Handler**：QUEUED/PLANNING/RESUMING/REFLECTING/HANDOFF/GROUP_CHAT/REJECTED 及对应 Handler（`PlanningStateHandler`、`ReflectingStateHandler`、`HandoffStateHandler`、`GroupChatStateHandler`）。
2. **多 Agent 编排**（规格 §1.3 明确列为非目标）：`orchestrator/` 包含 `AgentRouter`、`CoordinatorAgent`、`ParallelAgentExecutor`、`SequentialAgentExecutor`；`groupchat/GroupChatOrchestrator`；`tool/subagent/SubAgentExecutionService` —— 与非目标声明冲突，需规格追认或移除。
3. **HITL 审批流**：`approval/` 包 + `ToolApprovalService` 在工具执行前可暂停等待审批（`ToolCallingStateHandler.java:149-153`）+ MQ 审批消费者。
4. **事件溯源控制面**：`sf_execution_event`/`sf_execution_outbox` 表、`EventReorderingConsumer`、outbox 发布、ClickHouse 时间线（`TimelineClickHouseService`）。
5. **安全增强**：消息内容租户级加密（`TenantKeyService`）、快照 SHA-256 防篡改校验（`ResumingStateHandler.java:66-77`）、SSE 载荷密钥脱敏（`SecretMasker`）、SSRF 防护（`SsrfProtectionUtil`）、沙箱 provider（Local/Docker/Cube）。
6. **其他**：Guardrails 输入校验接入 THINKING（`ThinkingStateHandler.java:152-160`）、技能/角色分级注入、SubTaskPlan 计划推进（`resolveNextStateForPlan`）、ReasoningStrategy 插件化、影子评审/学习包（`shadow/`、`learning/`、`evaluation/`）、乐观锁 version 控制。
7. **实现缺陷清单**（规格未提及的既有行为，供修复参考）：
   - @Async 自调用失效（REQ-01）
   - 准入拒绝执行最终 COMPLETED 的门禁绕过链（REQ-09）
   - `orchestrator.cancel()` 单例共享标志误伤并发执行（REQ-23）
   - 准入计数器双减可为负（REQ-21 附注）
   - `snapshotId` 错绑 + `snapshot_id` 列缺失 → resume 失败（REQ-05）
   - `replaceMessages` 只压实 L1、L2 不变（REQ-17）
   - `ObservationStateHandler` 向 `token_budget_json` 写 `iterations=N` 破坏预算序列化（REQ-20）
   - `ObservationStateHandler` 无进入路径：主流程 TOOL_CALLING 直接回 THINKING（`ToolCallingStateHandler.java:191`），规格矩阵 `TOOL_CALLING → OBSERVATION` 实际不走（OBSERVATION 成为死状态）

## 反驳复核

> 独立复核员复核（2026-08-29，以当前工作树为准，`AgentExecutionLifecycleService.java` / `AgentRuntimeOrchestrator.java` 含未提交修改）。复核范围：全部 severity=High 且 verdict ∈ {contradicted, partial, absent} 的条目，共 3 条（REQ-01 / REQ-09 / REQ-23；其余条目因严重度 ≤ Medium 或 verdict=implemented 不在范围内）。对每条均执行代码方向（重读被引用行 + 全库检索遗漏执行点/代理类/配置覆盖/基类/跨模块调用方）与规格方向（重读草案原文及已批准基础规格 `2026-04-30-v1.0-agent-execution-engine.md`，排查豁免/条件子句与草案降级适用性）的双向反驳尝试。

| REQ | 原判 | 裁定 | 理由（一行） | 证据 |
|-----|------|------|-------------|------|
| REQ-01 | contradicted · High | 维持 | 未发现任何使主路径异步化的代理/配置覆盖：`@Async` 位于 `runExecutionAsync`（规格 §3.1 明确要求标注在 `startExecution()`），L38 `this.` 自调用绕过 Spring 代理，全库唯一 `@EnableAsync` 为默认代理模式且无 AspectJ 织入；REST（Controller L43）、两处 `AgentExecuteDispatcher`（engine L24、task L63）、`CoordinatorAgent` L123、`HandoffStateHandler` L154 全部只调 `startExecution`，唯一经代理的异步调用方是子执行路径（`SubAgentExecutionService` L62）——属实现缺陷而非草案未实现项，High 不适用草案降级。 | `AgentExecutionEngine.java:23-40`；`AgentExecutionAsyncConfig.java:11`；`AgentExecutionController.java:43`；`schemaplexai-task/.../AgentExecuteDispatcher.java:63`；grep `runExecutionAsync\|startExecution` 全库调用点枚举 |
| REQ-09 | contradicted · High | 维持 | 绕过链逐步复跑成立：准入拒绝路径（orchestrator L106-109）不写 `admissionType`（该键全库仅 Thinking/ToolCalling 阻塞路径设置）→ `GateBlockedStateHandler` L46-47 视为可重试 → RETRYING → `RetryingStateHandler` L59-66 因 `lastErrorCategory` 为空放行 → L103 转 TOOL_CALLING → `ToolCallingStateHandler` L81-85 因新建 conversationId 无消息转 COMPLETED，即准入拒绝执行终态为 COMPLETED；规格 §5.1/§5.2 矩阵/mermaid/关键约束四处明示 GATE_BLOCKED 为终端，无豁免子句；`isTerminal()` 亦不含 GATE_BLOCKED。成本/准入门禁失效属数据一致性后果，不受草案封顶。 | `AgentRuntimeOrchestrator.java:104-110`；`GateBlockedStateHandler.java:46-67`；`RetryingStateHandler.java:59-103`；`ToolCallingStateHandler.java:80-85`；`AgentExecutionState.java:23-25`；grep `admissionType` 全库写入点枚举 |
| REQ-23 | partial · High | 维持 | 未发现任何按 executionId 隔离的取消机制（取消类 Redis Key 全库 0 命中）：`cancelled` 为单例 `@Component` 上的共享 `volatile boolean`（L53），`cancel()`（L71-73）不接收 executionId，生产代码唯一调用点为 `AgentExecutionLifecycleService.cancelExecution` L92；所有并发执行共用 L129 检查点，且新执行启动 `resetCancelled()`（L94）还会清掉他人取消信号——"取消执行 X"波及全部并发执行（含跨租户），数据一致性后果不适用草案降级。 | `AgentRuntimeOrchestrator.java:53,71-73,78-80,94,129-133`（工作树版）；`AgentExecutionLifecycleService.java:82-111`；grep `\.cancel\(\)` 生产代码仅 1 命中 |
