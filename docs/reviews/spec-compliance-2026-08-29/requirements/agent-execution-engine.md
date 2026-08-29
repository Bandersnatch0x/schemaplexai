# Spec-to-Code 合规核查：Agent 执行引擎

- **规格文档**: `docs/specs/2026-04-30-v1.0-agent-execution-engine.md`（v1.0，status 已批准）
- **核查对象**: `schemaplexai-agent-engine/src/main/java`（含 web/model/common/docker SQL 中的执行点）
- **核查日期**: 2026-08-29
- **工作树说明**: `AgentExecutionLifecycleService.java`、`AgentRuntimeOrchestrator.java` 及其测试存在未提交修改（暂停/恢复/取消的 Redis Key 由全局 `CommonConstants.REDIS_KEY_EXECUTION_PAUSED` 迁移为租户作用域 `TenantRedisKeyResolver.executionPaused(tenantId, executionId)`，读写两侧一致）。本报告按当前工作树状态核查。

**裁决计数**: implemented 8 · partial 15 · contradicted 5 · absent 1 · stronger-than-spec 0（1 条注记） · undecidable 0 · 共 29 条
**notChecked**: §6 非功能指标（压测/集测类，见文末）

---

## REQ-01 — 状态机存储结构

> "状态存储: ConcurrentHashMap<Long, AgentExecutionState>" — §3.1 AgentStateMachine

**Verdict:** implemented · confidence: high
**What this demands:** 执行状态以 `ConcurrentHashMap<Long, AgentExecutionState>` 形式在内存中按 executionId 存储。
**Where enforcement lives:** `state/AgentStateMachine.java:27` — `private final Map<Long, AgentExecutionState> executionStates = new ConcurrentHashMap<>();`；写入 `:74`，读取 `:47,:116`。
**Paths walked:** ✓ transition 写入（L74）✓ getCurrentState 读取（L116）✓ removeExecution 移除（L111-113）
**How the verdict was reached:** 类型与键值语义与规格逐字一致，非"不同机制等价"。

---

## REQ-02 — 终端状态不可再转换

> "Terminal 状态（COMPLETED/FAILED/CANCELLED）不可再转换" — §3.1 关键约束

**Verdict:** contradicted · confidence: high · severity: **High**
**What this demands:** 进入任一终端状态后，任何后续 `transition()` 调用必须被拒绝。
**Where enforcement lives:** `state/AgentStateMachine.java:48` — `if (current != null && current.isTerminal() && newState != AgentExecutionState.FAILED)`：终端→FAILED 被显式放行。
**Paths walked:**
- ✗ terminal → FAILED：L48 例外直接放行。可达路径：`orchestrator/AgentRuntimeOrchestrator.java:148-154` catch 块在 `transition(FAILED)` 前的任何异常（如 L144 `isPaused()` 的 Redis 调用抛错）都会对已 COMPLETED 的执行再次 `transition(FAILED)`，穿过守卫，把 DB 状态从 COMPLETED 翻转为 FAILED。
- ✗ `AgentStateMachine.java:85-91`：handler 抛异常且 `newState != FAILED` 时无条件 `transition(FAILED)`，若当前已终端同样被放行。
- ✓ terminal → 其他非终端：被拦截并告警（L49-52）。
**How the verdict was reached:** 守卫存在但带例外，且例外在常规异常处理路径上可达——不是 absent（有机制），也不是 partial（被违反的是规格明文的无条件约束）。影响：已交付结果可被事后翻转为 FAILED，SSE 二次广播 `execution-completed`。

---

## REQ-03 — 终端状态触发内存清理

> "终端状态触发内存清理（remove from map）" — §3.1 关键约束；§6 "状态机内存泄漏 | 0 | 终端状态清理验证"

**Verdict:** implemented · confidence: high
**What this demands:** 到达终端状态时 executionId 必须从 `executionStates` 移除。
**Where enforcement lives:** `state/AgentStateMachine.java:100-104`（正常终端路径 `removeExecution`）、`:90-95`（FAILED handler 再抛异常的兜底路径也移除）；`lifecycle/AgentExecutionLifecycleService.java:109` cancel 后显式移除。
**Paths walked:** ✓ 正常终端（L100-104）✓ handler 异常→FAILED（L87-88 递归进入后仍走终端分支）✓ FAILED handler 再抛（L90-95）✓ cancel（lifecycle L108-109）
**How the verdict was reached:** 所有到达终端的路径均调用 `removeExecution`。备注：PAUSED（非终端）条目在用户永不 resume 时滞留，属设计内语义，不违反该约束。

---

## REQ-04 — 状态集合定义（含 IDLE）

> 状态定义表：`IDLE`(初始)/`THINKING`/`TOOL_CALLING`/`PAUSED`/`COMPLETED`/`FAILED`/`CANCELLED` — §3.1

**Verdict:** partial · confidence: high · severity: **Low**
**What this demands:** 状态枚举 = 规格 7 态；执行创建时为初始态（规格语义为 IDLE）。
**Where enforcement lives:** `state/AgentExecutionState.java:3-26` — 18 个状态：含规格 6 态（无 `IDLE`），另增 QUEUED、INITIALIZING、READY、PLANNING、OBSERVATION、RESUMING、GATE_BLOCKED、RETRYING、REFLECTING、HANDOFF、GROUP_CHAT、REJECTED。初始态为 QUEUED（`AgentExecutionEngine.java:34`），启动经 `start()` → INITIALIZING（`AgentStateMachine.java:42-44`）。
**Paths walked:** ✓ 初始创建（QUEUED）✓ 启动转换（INITIALIZING→READY→…）✗ `IDLE` 不存在。
**How the verdict was reached:** 超集演进：规格 6/7 态存在且语义保持，`IDLE` 由 QUEUED/INITIALIZING 承接，无行为缺失——故 partial 而非 contradicted；判 contradicted 需要行为被破坏，此处是词汇漂移。

---

## REQ-05 — 状态转换矩阵约束

> 状态转换矩阵表（如 "TOOL_CALLING | THINKING, PAUSED, FAILED"） — §3.1

**Verdict:** contradicted · confidence: high · severity: **Medium**
**What this demands:** 仅允许矩阵内列出的转换；矩阵外转换应被拒绝。
**Where enforcement lives:** 不存在。`AgentStateMachine.transition()`（`state/AgentStateMachine.java:46-105`）只做终端守卫，无逐对校验。
**Searched:** `allowedTransition|TRANSITION|canTransition|transitionMatrix`（agent-engine/src/main）→ 命中均为 javadoc/注释（approval/*、ToolApprovalService 等），无矩阵数据结构或校验逻辑。
**Paths walked:**
- ✗ 常规路径违规实例：`state/ToolCallingStateHandler.java:83,90` — TOOL_CALLING→COMPLETED（矩阵仅允许 THINKING/PAUSED/FAILED）；`ThinkingStateHandler.java:598` — THINKING→THINKING 自转换；各处理器→GATE_BLOCKED/RETRYING（矩阵无此目标）。
- ✗ 任意非终端态之间均可互转（无校验代码可拦截）。
**How the verdict was reached:** 与 absent 的区分：终端行（矩阵的"—（终端）"行）由 REQ-02 的守卫部分承接；但整表约束在常规路径上被实际违反，故选 contradicted。影响属设计漂移：系统以扩展状态图自洽运行，但矩阵作为控制项未生效。

---

## REQ-06 — 准入四维并发限流

> "维度1: 租户级并发限制 / 维度2: Agent 级并发限制 / 维度3: 模型级并发限制 / 维度4: 供应商级并发限制（含冷却期）" — §3.2

**Verdict:** contradicted · confidence: high · severity: **High**
**What this demands:** 准入时按租户、Agent、模型、供应商四个并发维度分别限流，供应商维度含冷却期。
**Where enforcement lives:** `admission/ExecutionAdmissionService.java:26-95` — 实际实现 5 个维度，与规格不同：①速率限制 60 次/分（L28-38）②tenant×agent 并发=5（L41-49）③TokenBudget 检查（L52-64）④成本预算 BudgetGuard（L67-81）⑤租户日工具调用预算（L84-89）。
**Paths walked:**
- ✗ 模型级并发限制：全模块无实现（见 Searched）。
- ✗ 供应商级并发限制：无。冷却期存在于另一层 `model/AiModelRouter.java:24,115-123`（provider 失败后 5 分钟 Redis 冷却），属 LLM 路由容错而非准入并发维度，且时长 300s≠规格 60s。
- ✗ 租户级**总量**并发：并发 Key 为 `admissionConcurrency(tenantId, agentId)`（每 agent 5），租户跨 agent 无聚合上限。
**Searched:** `admission|cooldown|max-concurrent|maxConcurrent`（全仓 yml/java/properties）→ 主代码仅命中上述实现与 `TenantRedisKeyResolver` 的 Key 帮助方法（含未使用的 `modelCooldown` 帮助器），无模型/供应商并发计数器。
**How the verdict was reached:** 准入控制存在且更丰富，但规格点名的维度 3/4 与租户总量控制未生效；这是"控制项缺失"而非"机制不同等价"（等价的必要条件是覆盖同样的限流语义），故 contradicted。影响：单租户可借多 agent 突破总量约束；热点模型/供应商无并发保护。

---

## REQ-07 — 准入限流参数配置化

> ```yaml
> agent:
>   admission:
>     tenant-max-concurrent: 100
>     agent-max-concurrent: 10
>     model-max-concurrent: 50
>     provider-max-concurrent: 30
>     provider-cooldown-seconds: 60
> ``` — §3.2 限流参数（配置化）

**Verdict:** absent · confidence: high · severity: **Medium**
**What this demands:** 上述 5 个参数可通过配置调整，且默认值如规格所列。
**Where enforcement lives:** 无。实际限流值硬编码：速率 60（`ExecutionAdmissionService.java:33`）、并发 5（`:43`）、冷却 5 分钟（`AiModelRouter.java:24`）。
**Searched:**
- `admission|max-concurrent|provider-cooldown` 于 `schemaplexai-agent-engine/src/main/resources/application.yml` → 0 命中（该文件 `agent:` 段仅有 `agent.model`/`agent.llm`，L53-66）。
- `@ConfigurationProperties(prefix = "agent...")` → 仅 `config/AgentEngineProperties.java:12`（`agent.engine`，只含 maxToolCalls/maxToolCallsPerIteration）与 `LlmProviderProperties`、`CubeSandboxProperties` 等，无 `agent.admission`。
**Paths walked:** ✗ 无配置绑定路径；运维无法在不改代码的情况下调整任一限流值。
**How the verdict was reached:** 判 absent 而非 partial：不是数值不同，而是"配置化"这一需求本身不存在；数值差异（10→5、60s→300s）是附带偏差。

---

## REQ-08 — TokenBudget 结构、CAS 线程安全与预检/后扣

> "CAS 循环保证线程安全 / public boolean consumeInput(long tokens); public boolean consumeOutput(long tokens);" — §3.3；"1. 预检：调用前预估输入 Token，预算不足直接拒绝 2. 后扣：调用完成后扣除实际输出 Token" — §3.3 预算策略

**Verdict:** implemented · confidence: high（附 stronger-than-spec 注记）
**What this demands:** 字段 `AtomicLong consumedInput/OutputTokens` + `maxInput/OutputTokens`；consumeInput/consumeOutput 以 CAS 循环实现；LLM 调用前预估扣输入、调用后扣输出。
**Where enforcement lives:** `admission/TokenBudget.java:10-15`（字段，另多一个 `maxToolCalls` 维度）、`:27-51`（两个 CAS 循环）；调用侧 `state/ThinkingStateHandler.java:162-173`（TokenEstimator 预估→`consumeInput`，不足→GATE_BLOCKED 拒绝）、`:195-210`（`generateWithFallback` 后 `consumeOutput`）。
**Paths walked:** ✓ 预检拒绝（L166-173）✓ 调用后扣除（L201）✓ 输出超预算拒绝（L201-208）✓ 预算状态序列化回写（L209, saveBudget L544-558）✓ 跨轮恢复（loadBudget L500-542，orchestrator 以 `32000/4096` 初始化，`CommonConstants.java:49-50`）。
**How the verdict was reached:** 结构与算法逐字符合；额外的工具调用预算维度属增强（`consumeToolCall` L61-72），不改变本裁决。注：拒绝后的落点是 GATE_BLOCKED 而非直接终止，见 REQ-09。

---

## REQ-09 — 超限处理级联：压缩→截断→终止

> "3. 超限处理：先压缩记忆 → 再截断上下文 → 最终终止执行" — §3.3 预算策略

**Verdict:** partial · confidence: high · severity: **Medium**
**What this demands:** Token 超限时按序执行：记忆压缩 → 上下文截断 → 仍不行则终止执行。
**Where enforcement lives:** `memory/compaction/AutoCompactionService.java:33-91`（Layer0 工具结果压缩→Layer1 滑动窗口截断→Layer2 摘要+`truncateHead` 25% 重试 3 次）；触发点 `state/ThinkingStateHandler.java:124-140`；失败后 `GateBlockedStateHandler.java:46-79`。
**Paths walked:**
- ✓ 压缩/截断级联存在且顺序执行（AutoCompactionService L45-88）。
- ✗ "最终终止执行"：级联耗尽后转 GATE_BLOCKED（ThinkingStateHandler L132-139），而 `GateBlockedStateHandler.java:47` 判定 `isRetryable = !"FATAL".equalsIgnoreCase(admissionType)`；`admissionType=COMPACTION/BUDGET/LOOP` 均≠FATAL → RETRYING（L67）→ 重回 TOOL_CALLING（RetryingStateHandler.java:103）→ 至多 3 次重试后才 FAILED（RetryingStateHandler.java:71-75）。终止是"重试耗尽"的副产品，不是规格的直接终止语义；中间产生无收益的状态循环。
- ✗ `CompositeChatMemoryStore.java:44-47,131-134`：>50 条消息时 Redis 直接 `trim` 丢弃旧消息，未经摘要（与 §3.8 压缩策略交叉，见 REQ-24）。
**How the verdict was reached:** 级联前两步满足，第三步语义不符且有可观察的重试环路，故 partial。

---

## REQ-10 — 四层 Prompt 构建

> "1. 构建四层 Prompt（System → Context → Memory → User）" — §3.4 处理流程

**Verdict:** partial · confidence: medium · severity: **Low**
**What this demands:** Prompt 按 System、Context、Memory、User 四层有序组装。
**Where enforcement lives:** `state/ThinkingStateHandler.java:371-421` `buildPrompt()`：Role overlay（L378-384）→ Skill 指令（L387-395）→ 可用 Skills 列表（L398-405）→ 工具描述（L408-411）→ 全部历史消息 `role: content` 平铺（L414-418）；`context/ContextInjector.java:61-92` 将 RAG 知识作为 system 消息插入到最后一条 user 消息之前（L104-118）。
**Paths walked:**
- ✓ Context 层（RAG 注入，失败降级不阻塞，ContextInjector L89-91）。
- ✓ Memory/User 内容在场（历史消息含 user 轮次）。
- △ System 层由 RoleOverlay/Skill 近似，无独立系统提示词层。
- ✗ 四层边界与顺序未结构化保证：最终压平为单个字符串经 `generateWithFallback(prompt,...)`（L195）发出，User 层与 Memory 层不可区分。
**How the verdict was reached:** 各语义成分大体在场（非 absent），但"四层有序结构"这一性质不成立（非 implemented）；对行为的实际损害无法确立，严重度 Low。

---

## REQ-11 — ThinkingStateHandler 主流程

> "2. 预估 Token → tokenBudget.consumeInput() 3. 调用 AiModelRouter.generateWithFallback() 4. 扣除输出 Token → tokenBudget.consumeOutput() 5. 存储至 CompositeChatMemoryStore 6. 解析响应，判断是否含工具调用"；"输出: 状态转换至 TOOL_CALLING 或 COMPLETED" — §3.4

**Verdict:** implemented · confidence: high
**What this demands:** THINKING 处理按 2→3→4→5→6 顺序执行，并据解析结果转换到 TOOL_CALLING 或 COMPLETED。
**Where enforcement lives:** `state/ThinkingStateHandler.java:162-246`：预估+`consumeInput`（L163-173）→ `modelRouter.generateWithFallback(prompt, modelId, 0.7)`（L195）→ `consumeOutput`（L201）→ `chatMemoryStore.saveMessage(...)`（L215）→ `containsToolCalls` 解析（L218）→ TOOL_CALLING（L237）或 `resolveNextStateForPlan`→COMPLETED（L240-245）。
**Paths walked:** ✓ 正常工具调用分支 ✓ 直接回答分支 ✓ 异常→FAILED（L247-252）✓ 循环检测前置拦截（L218-231，见 REQ-22）✓ 守卫链（guardrails 输入校验 L152-160）。
**How the verdict was reached:** 六步与两个目标转换全部落实。注记：①另有 `ReasoningStrategy` 委托分支（L176-189），该分支不做输出 Token 后扣，但 `ReActStrategy`/`CoTStrategy` 未注册为 Spring Bean（无 @Component、无 @Bean 工厂，已搜索确认），`reasoningStrategies` 运行期为空，该偏差当前不可达；②`resolveNextStateForPlan` 在多子任务计划下可回到 THINKING（L598），属规格外扩展，不改本条裁决。

---

## REQ-12 — ToolErrorCategory 错误分类体系

> 枚举定义 7 项：`INVALID_ARGUMENTS(false,false)`、`UNEXPECTED_ENVIRONMENT(false,true)`、`PROVIDER_ERROR(false,true)`、`USER_ABORTED(false,false)`、`TIMEOUT(false,true)`、`IRREVERSIBLE_OPERATION(true,false)`、`ENVIRONMENT_MISMATCH(true,false)`；"每个分类携带两个标志位：securityRelated / retryable" — §3.5.1

**Verdict:** partial · confidence: high · severity: **Medium**
**What this demands:** 7 个分类以规格标志位存在，并驱动安全/重试决策。
**Where enforcement lives:** `tool/ToolErrorCategory.java:6-78`。
**Paths walked:**
- ✓ `TIMEOUT(false,true)`（L21）✓ `IRREVERSIBLE_OPERATION(true,false)`（L41）✓ `ENVIRONMENT_MISMATCH(true,false)`（L46）完全一致；双标志位载体与访问器（L63-77）✓；`INVALID_ARGUMENT(false,false)`（L16）= 规格 `INVALID_ARGUMENTS` 更名，标志一致。
- ✗ `UNEXPECTED_ENVIRONMENT`：规格 `(false,true)`（可重试），代码 `(true,false)`（L51，安全相关+不可重试）——标志翻转。后果：`ToolCallingStateHandler.java:163` 依 `isRetryable()` 决定 RETRYING；`ToolCallingStateHandler.java:196-198` 通用异常即记为该类别 → 本应自动重试的环境异常被直通 FAILED。
- ✗ `PROVIDER_ERROR`、`USER_ABORTED` 缺失（供应商错误实际落入 `INTERNAL_ERROR`/`RATE_LIMITED`，均 retryable，重试语义侥幸保留；用户中断无分类可记）。
- 增强项：`PERMISSION_DENIED`、`PATH_VIOLATION`、`SANDBOX_ERROR` 等 4 个规格外类别。
**How the verdict was reached:** 框架与多数条目符合，但一个条目的标志位与规格直接矛盾且改变可达行为，两个条目缺失——超出"文档漂移"范畴，却也非整体推翻，故 partial 并点名矛盾子项。

---

## REQ-13 — ToolSafetyGuard 四维安全检查

> "工具名称黑名单…参数内容审查…环境匹配…输入规范化（Unicode NFKC、HTML 实体、JSON 转义解码、Cyrillic 同形异义字映射）" — §3.5.2

**Verdict:** implemented · confidence: high
**What this demands:** 执行前完成黑名单、破坏性参数、环境匹配、反混淆规范化四项检查。
**Where enforcement lives:** `tool/ToolSafetyGuard.java`：黑名单 `IRREVERSIBLE_TOOLS`（L11-13，含规格示例 `volumeDelete`、`databaseDrop`，检查 L28-33）；参数审查 `IRREVERSIBLE_PATTERN`（L15-17，`DROP TABLE`/`RM -RF` 等，检查 L36-44）；环境匹配（L47-54，非 prod 环境拦截含 "prod" 的参数，环境值经 `SecurityPolicyLoader` 按租户解析——`ToolCallingStateHandler.java:217-221`）；输入规范化 `normalizeInput`（L64-87：JSON `\uXXXX` 解码 L118-137 → HTML 数字实体解码 L139-161 → NFKC L78 → Cyrillic 同形字映射 L89-116 → 空白折叠）。
**Paths walked:** ✓ 四维各自独立生效 ✓ 检查先于执行（`ToolCallingStateHandler.java:224-230`）✓ 拦截产出 `SafetyCheckResult.reject` 带类别与原因。
**How the verdict was reached:** 四维全部落实且含规格点名的全部规范化手段；环境匹配实现较粗（子串 "prod" 判定），但与规格示例语义一致，不构成缺口。

---

## REQ-14 — 执行审计：记录内容与写失败异常

> "记录每次工具调用的完整上下文：工具名称、参数、输出、延迟（latencyMs）、Token 消耗、错误分类（如被拦截）"；"审计日志写入失败时抛出 ToolExecutionAuditException，阻止执行继续（安全事件不可静默丢失）" — §3.5.3

**Verdict:** partial · confidence: high · severity: **Medium**
**What this demands:** ①每次工具调用记录名称/参数/输出/延迟/Token/错误分类；②写失败一律抛 `ToolExecutionAuditException` 中断执行。
**Where enforcement lives:** `tool/ToolExecutionRecorder.java`：`formatMessage`（L76-96）记录 tool、status、category、error、latencyMs、tokens；`record`（L26-41）插入 `sf_agent_execution_log`，写失败时仅当 `result.errorCategory().isSecurityRelated()` 才抛 `ToolExecutionAuditException`（L36-39）。
**Paths walked:**
- ✗ 参数与输出未写入审计（formatMessage 无 params/output 字段）——"完整上下文"不成立。
- ✓ 名称/延迟/Token/错误分类记录。
- ✗ 非安全类别（如 `INTERNAL_ERROR`、`INVALID_ARGUMENT`）写失败仅 `log.error`（L35），执行继续——违反规格无条件抛出语句；安全类别满足（抛出后经 `ToolCallingStateHandler.java:156`→外层 catch（L192-200）→FAILED，确实阻止继续）。
**How the verdict was reached:** 两个子需求各有明确缺口但审计主干存在，故 partial。规格自注的理由句（"安全事件不可静默丢失"）恰是代码实际覆盖的范围，但规格正文是无条件式，按正文裁决。

---

## REQ-15 — 评估优先的工具执行顺序

> "工具执行流程（评估优先）：parseToolCalls() → safetyGuard.check() → executeTool() → executionRecorder.record() → stateMachine.transition()" — §3.5.4

**Verdict:** implemented · confidence: high
**What this demands:** 安全检查必须先于实际执行，审计记录必须先于状态转换。
**Where enforcement lives:** `state/ToolCallingStateHandler.java`：`toolRegistry.parse`（L95，结构化解析替代启发式 `parseToolCalls`）→ 循环内 `executeToolWithGuard`（L155）其中 `safetyGuard.check`（L224-227）先于 `adapter.execute`（L240）→ `executionRecorder.record`（L156）→ 状态转换（L164/167/176/191）。
**Paths walked:** ✓ 正常链 ✓ 拦截链（check blocked→未执行即返回，L228-230）✓ 异常链（catch 内补记审计后转 FAILED，L196-199）。
**How the verdict was reached:** 方法名不同（机制演进），但顺序语义逐点成立，按"不同机制满足需求"判 implemented（文档漂移）。

---

## REQ-16 — 工具结果的状态转换规则

> "安全拦截（blocked=true）→ 状态转换至 FAILED；执行失败（success=false）→ 状态转换至 FAILED；执行成功 → 状态转换至 THINKING（继续推理）" — §3.5.4

**Verdict:** partial · confidence: high · severity: **Medium**
**What this demands:** 三种结果各自映射到指定状态。
**Where enforcement lives:** `state/ToolCallingStateHandler.java`。
**Paths walked:**
- ✗ blocked=true：转 **GATE_BLOCKED**（L171-178），非 FAILED。后续经 `GateBlockedStateHandler.java:49-67` 视为可重试→RETRYING→可能重回 TOOL_CALLING——被安全拦截的调用存在重试环。
- △ success=false：非可重试类别→FAILED ✓（L167）；可重试类别→先 RETRYING（L163-165），重试耗尽（3 次）或熔断（连续 3 败）后才 FAILED（`RetryingStateHandler.java:71-86`）——最终 FAILED 可达，视为 stronger-than-spec 的恢复增强，但即时语义与规格不符。
- ✓ success=true：批内全部成功后转 THINKING（L191）。
**How the verdict was reached:** 三分支一符、一增强、一矛盾，整体 partial；矛盾分支（安全拦截→非 FAILED）是本条严重度来源。

---

## REQ-17 — 并行读 / 串行写

> "并行读：多个读工具可并发执行；串行写：写工具必须串行，保证一致性" — §3.6 执行策略

**Verdict:** contradicted · confidence: high · severity: **Medium**
**What this demands:** 批内只读工具并发执行；写工具串行。
**Where enforcement lives:** `state/ToolCallingStateHandler.java:137-188` — 单一 for 循环顺序执行全部工具调用，无并发分支、无读写分类。
**Searched:** `isReadOnly|readOnly|ReadOnly`（agent-engine/src/main）→ 命中仅沙箱/挂载类（`MountSpec`、`DockerCreateOptions`、`ContainerSandboxProvider`）与审批风险注释（`ToolRiskClassifier.java:16`），工具执行路径上无读写判定；两个 `ToolAdapter` 接口均无 `isReadOnly()`。
**Paths walked:** ✗ 任何多工具批次均为全串行。
**How the verdict was reached:** "串行写"因全串行而平凡满足，但"并行读"这一明确需求在唯一执行路径上不生效，故 contradicted 而非 partial。影响为吞吐而非正确性，严重度 Medium。

---

## REQ-18 — 单工具 30 秒超时

> "超时：单工具 30 秒超时" — §3.6 执行策略

**Verdict:** partial · confidence: high · severity: **Medium**
**What this demands:** 每个工具调用有 30 秒上限，超时按错误处理。
**Where enforcement lives:** 部分工具自带：`tool/adapter/http/HttpCallAdapter.java:47-48`（connect 5s/read 30s）；沙箱会话默认 30s（`tool/sandbox/SandboxSessionConfig.java:24`、`tool/SandboxConfig.java:10`），用于 `reasoning/CodeExecutionReasoner.java:56,79`。
**Paths walked:**
- ✓ http_call 工具 30s 读超时。
- ✓ 沙箱 shell 执行 30s（CodeExecutionReasoner 路径）。
- ✗ 主路径无兜底：`ToolCallingStateHandler.executeToolWithGuard`（L207-252）直接同步调用 `adapter.execute`，无超时包裹；`FileReadAdapter.java:84-93` 同步 `Files.readString` 无时限；注入的 `ToolSandbox sandbox`（L37,57）在该方法中从未使用。
**Searched:** `30_000|Duration.ofSeconds(30)|ofSeconds(30`（src/main）→ 命中上列各点，`ToolCallingStateHandler` 无命中。
**How the verdict was reached:** 超只在个别适配器生效，通用控制不存在；一个挂死的文件读取会无限阻塞执行线程（虽为虚拟线程，执行本身不终止），故 partial 偏重。

---

## REQ-19 — 单工具失败不影响其他工具

> "错误处理：单工具失败记录错误，不影响其他工具" — §3.6 执行策略

**Verdict:** contradicted · confidence: high · severity: **Medium**
**What this demands:** 批内某工具失败时记录并继续执行其余工具。
**Where enforcement lives:** `state/ToolCallingStateHandler.java:158-178`。
**Paths walked:**
- ✗ 失败（非可重试）：立即 `transition(FAILED)` + `return`（L167-168），同批后续工具被跳过。
- ✗ 失败（可重试）：立即 `transition(RETRYING)` + `return`（L164-165），同样跳过后续。
- ✗ 拦截：`return`（L176-177）。
- △ 重试恢复后仅重放失败的那一个工具（`isRetryTarget` L285-289），其余同批工具永不执行。
**How the verdict was reached:** 规格要求"继续"，代码在所有失败分支上"中止"，行为直接相反，判 contradicted。

---

## REQ-20 — ToolAdapter 接口契约

> ```java
> public interface ToolAdapter {
>     String getToolName();
>     ToolResult execute(Map<String, Object> params, Long tenantId);
>     boolean isReadOnly();
> }
> ``` — §3.6 工具注册

**Verdict:** partial · confidence: high · severity: **Low**
**What this demands:** 适配器接口含工具名、带租户的执行方法、只读标志。
**Where enforcement lives:** `tool/adapter/ToolAdapter.java:12-29` — `getToolName()` ✓；`execute(ToolCall call, ExecutionContext ctx)`（L22）：参数 Map 在 `ToolCall.parameters()`、tenantId 在 `ExecutionContext`（`ToolCallingStateHandler.java:235-239`）——数据等价、签名不同；`isReadOnly()` ✗ 缺失。另有遗留接口 `tool/ToolAdapter.java:7-29`（`execute(ToolCall)`/`supports`/`toolName()`），两接口并存。
**Paths walked:** ✓ 主执行链使用 adapter.ToolAdapter（ToolRegistry.resolve→execute）✗ 只读标志无处声明（联动 REQ-17）。
**How the verdict was reached:** 名称与执行语义满足（机制漂移），`isReadOnly` 缺失且与并行读需求联动，故 partial。

---

## REQ-21 — 循环检测策略与窗口

> "1. 哈希检测：连续 N 次响应内容哈希相同 2. 工具序列检测：连续 N 轮调用相同工具序列 3. 窗口大小: 5 轮" — §3.7 检测策略

**Verdict:** implemented · confidence: high
**What this demands:** 基于响应哈希与工具序列的两类检测，窗口 5 轮。
**Where enforcement lives:** `loop/AgentLoopDetectionService.java:24-31`（`agent.loop-detection.window-size:5` 默认 5 ✓、`max-same-hash:3`、`max-same-tool-sequence:3`）；哈希检测 L44-54；工具序列检测 L57-69（List 全等比较）。
**Paths walked:** ✓ 窗口不足 5 不判（L37-39）✓ 哈希命中（L50-53）✓ 序列命中（L64-67）✓ THINKING 与 TOOL_CALLING 两处入口均调用（ThinkingStateHandler.java:221-222、ToolCallingStateHandler.java:128）✓ 完成后清理记录（ThinkingStateHandler.java:244 clearRecords）。
**How the verdict was reached:** 算法为"窗口内出现≥3 次相同"而非严格"连续 N 次"，检出集略宽，仍覆盖规格意图，按机制等价判 implemented（注记漂移）。

---

## REQ-22 — 循环处理：强制 COMPLETED 并记录原因

> "处理: 检测到循环后，强制转换至 COMPLETED，记录循环原因" — §3.7

**Verdict:** contradicted · confidence: high · severity: **Medium**
**What this demands:** 循环检出 → 立即 COMPLETED；原因落记录。
**Where enforcement lives:** `state/ThinkingStateHandler.java:224-231`、`state/ToolCallingStateHandler.java:129-133`、`state/GateBlockedStateHandler.java:46-79`、`state/RetryingStateHandler.java:68-103`。
**Paths walked:**
- ✗ 目标状态：两处均转 **GATE_BLOCKED**，随后（`admissionType=LOOP`≠FATAL→可重试）RETRYING→TOOL_CALLING→大概率再检出循环，至多 3 次重试后 FAILED（RetryingStateHandler.java:71-75）。任何路径都不产生 COMPLETED。
- △ 原因记录：THINKING 路径写 `blockedReason=agent_loop_HASH_LOOP/TOOL_SEQUENCE_LOOP`（ThinkingStateHandler.java:227）✓；TOOL_CALLING 路径检出后**未**设置 blockedReason（ToolCallingStateHandler.java:129-133），`GateBlockedStateHandler.java:41-43` 回退为 "admission_denied" ✗。
**How the verdict was reached:** 终止性由重试耗尽兜底（不会死循环，另有 `MAX_ITERATIONS=50` 总闸，AgentRuntimeOrchestrator.java:50,144-147），但规格指定的终态与原因记录两项均不符，判 contradicted。

---

## REQ-23 — 双层记忆：L1 Redis（7 天 TTL）+ L2 PostgreSQL

> 表格 "L1 | Redis List | 7 天 | 活跃对话快速读取；L2 | PostgreSQL | 永久 | 历史消息持久化" — §3.8

**Verdict:** implemented · confidence: high
**What this demands:** Redis List 作热层、7 天 TTL；PG 作持久层；读穿透与写双投。
**Where enforcement lives:** `memory/CompositeChatMemoryStore.java:26`（`CHAT_HISTORY_TTL = Duration.ofDays(7)`）；读：Redis `opsForList().range`（L42）miss 后回落 `SfChatMessageMapper`（L68-73）并回填 Redis+续期（L88-89，含 backfill 锁防重复回源，L52-65）；写：先 PG 插入（L117-122，synchronized 防 turnIndex 竞态，L111-123）后 Redis push+expire（L129-130），Redis 失败降级不损 L2（L135-137）。
**Paths walked:** ✓ L1 命中 ✓ L1 miss→L2→回填 ✓ 写双投 ✓ 租户级内容加密（`TenantKeyService` L101-108，规格外增强）。
**How the verdict was reached:** 存储介质、TTL、读写路径与表格逐项一致。

---

## REQ-24 — 压缩策略：>50 轮触发，保留最近 10 轮+摘要

> "当消息数 > 50 轮时触发压缩；压缩方式：摘要提取（保留最近 10 轮 + 历史摘要）" — §3.8

**Verdict:** partial · confidence: high · severity: **Low**
**What this demands:** 计数触发（>50 轮）；摘要式压缩；保留最近 10 轮。
**Where enforcement lives:** `memory/CompositeChatMemoryStore.java:25,44-47,131-134`（MAX_MESSAGES=50）；`memory/compaction/*`（Token 预算触发的三策略级联）；`memory/SummarizationStrategy.java:16,46-57`。
**Paths walked:**
- ✗ 触发条件：>50 条时执行的是 Redis `trim`（硬丢弃最旧消息，无摘要，L44-47）；摘要压缩由 **Token 预算**触发（`AutoCompactionService.compactIfNeeded` L40，比较 `TokenEstimator.estimate <= budget.remainingInput()`），与轮数无关。
- ✗ 保留策略：`SummarizationCompactionStrategy.java:54-59` 产出"摘要+最近文件上下文"，无"最近 10 轮"；`SummarizationStrategy` 保留条数由预算决定（最少 2 条，L16）；`SlidingWindowCompactionStrategy.java:36-51` 二分求预算内最大尾部，条数不固定。
- ✓ 摘要能力本身存在且可被 THINKING 前置调用（ThinkingStateHandler.java:126-131）。
- 缓解：trim 只作用于 L1 缓存，L2 全量保留，无数据灭失；`replaceMessages` 仅重写 Redis 视图（L217-236），PG 原史不动。
**How the verdict was reached:** 压缩能力存在但触发条件与保留形态均非规格所述，故 partial；因不丢数据、仅缓存视图差异，严重度 Low。

---

## REQ-25 — 执行启动 API

> ```http
> POST /agents/{id}/execute
> { "input": ..., "contextId": 123, "conversationId": "conv_456" }
> Response: Result<Long>  // 返回 executionId
> ``` — §4.1

**Verdict:** partial · confidence: high · severity: **Medium**
**What this demands:** 路径、请求体字段、返回体形状三者符合契约。
**Where enforcement lives:** `controller/AgentExecutionController.java:34-45`；`AgentExecutionEngine.java:30-40`。
**Paths walked:**
- ✓ 路径 `POST /agents/{id}/execute`。
- ✗ 请求体：代码读 `body.get("prompt")`（L42）——按规格发 `input` 的客户端得到 null prompt；`contextId`、`conversationId` 不被读取，conversationId 由服务端 `UUID.randomUUID()` 生成（AgentExecutionEngine.java:33），客户端无法续接既有会话。
- ✗ 返回体：`Result<SfAgentExecution>`（L44）而非 `Result<Long>`；executionId 需从实体 `id` 取。
- ✓ 租户取自网关注入头而非请求体（L39-41，安全增强）。
**How the verdict was reached:** 端点与功能存在，契约三要素两项不符，判 partial。

---

## REQ-26 — SSE 事件流端点与事件类型

> "GET /agents/execute/stream?executionId={id}，Authorization: Bearer"；事件：THINKING_STARTED / THINKING_CHUNK / TOOL_CALLING / TOOL_RESULT / PAUSED / COMPLETED / FAILED / ERROR — §4.2

**Verdict:** partial · confidence: high · severity: **Medium**
**What this demands:** 指定订阅端点与 8 类事件名。
**Where enforcement lives:** 引擎侧 `controller/AgentExecutionController.java:78-96`（`GET /agents/{id}/executions/{execId}/events?token=`，`JwtSseTokenValidator.java:62-107` 校验签名/过期/subject 匹配）；web 侧 `schemaplexai-web/.../SseController.java:39-55`（`GET /sse/executions/{executionId}/events`，带 lastSeq 回放）；事件发射 `sse/ExecutionEventBus.java:54-118`。
**Paths walked:**
- ✗ 端点路径与规格不同（两处实现均非 `/agents/execute/stream`）；鉴权用 query 参数 `token` 而非 Authorization 头（项目级已知缺陷，见 agents.md）。
- △ 事件映射：TOOL_CALLING→`tool-call`（L89）、TOOL_RESULT→`tool-result`（L97）、ERROR→`error`（L116）、COMPLETED/FAILED→`execution-completed`（携带 state，L67-77）、PAUSED→`state-transition`（L54-65）。
- ✗ `THINKING_STARTED`、`THINKING_CHUNK` 无对应事件：LLM 调用为非流式 `generateWithFallback`（AiModelRouter.java:42-64），"流式输出片段"能力不存在。
- ✓ 发射失败自愈（emitter 移除，L142-151）、终态 `complete()`（L154-165，状态机终端调用，AgentStateMachine.java:102）、载荷脱敏（SecretMasker，L129）。
**How the verdict was reached:** 订阅与多数事件语义在场，但路径契约与两个思考类事件（含流式能力本身）缺失，判 partial。

---

## REQ-27 — 执行控制 API（pause/resume/cancel/查询）

> ```http
> POST /agents/execution/{id}/pause / resume / cancel
> GET  /agents/execution/{id}
> ``` — §4.3

**Verdict:** partial · confidence: high · severity: **Critical**（cancel 跨执行副作用）
**What this demands:** 四个操作端点；控制仅作用于指定执行。
**Where enforcement lives:** `controller/AgentExecutionController.java:47-73`（`/agents/{id}/executions/{execId}/pause|resume|cancel|snapshot`）；`lifecycle/AgentExecutionLifecycleService.java:57-111`；`orchestrator/AgentRuntimeOrchestrator.java:53,61-73,120-147`。
**Paths walked:**
- △ 路径形状不同（嵌套 `/agents/{id}/executions/{execId}/*`），pause 强制 `reason` 参数（L49），状态查询以 `.../snapshot`（L68-73）替代。
- ✓ pause：写暂停 Key（TTL 24h，L62-64，未提交修改后为租户作用域键）+ 转 PAUSED（终端守卫阻止对已终态执行的暂停）；PausedStateHandler 持久化快照（PausedStateHandler.java:78-92）。
- ✓ resume：删 Key→RESUMING（L74-78）→ResumingStateHandler 恢复快照后→THINKING（ResumingStateHandler.java:93）——PAUSED→THINKING 语义经中间态达成。
- ✗ **cancel 跨执行污染**：`orchestrator.cancel()`（L92 调用）置 `AgentRuntimeOrchestrator`（单例 @Component）上的全局 `volatile boolean cancelled`（AgentRuntimeOrchestrator.java:53,71-73）；运行循环检测该标志（L129-133）。并发≥2 个执行时，取消任一执行会使**同实例所有在跑执行**转入 CANCELLED；新执行 `run()` 起始 `resetCancelled()`（L94）又可能抹掉针对它的在先取消信号。控制项影响范围超出调用方。
- ✓ cancel 附带清理：沙箱会话关闭（LifecycleService L95-103）、outbox 事件（L113-138）、状态机移除（L109）。
**How the verdict was reached:** 功能存在但路径漂移（partial 由来）；Critical 单独来自取消控制的可达越界——规格隐含"取消该执行"的对象限定被全局标志破坏。

---

## REQ-28 — 数据模型 sf_agent_execution

> 字段表：id / agent_id / tenant_id / status / user_input / last_response / model_config(JSONB) / token_used_input / token_used_output / start_time / end_time / conversation_id — §5.1

**Verdict:** partial · confidence: high · severity: **Medium**
**What this demands:** 表/实体含规格 12 字段。
**Where enforcement lives:** `docker/postgres/init/02-init-schema-agent.sql:62-78`；`entity/SfAgentExecution.java:15-53`。
**Paths walked:**
- ✓ id、agent_id、tenant_id、conversation_id；status 以 `state VARCHAR(32)` 承载（DDL L67，名称漂移）。
- ✗ `user_input`：无列；用户输入仅存于 `sf_chat_message`（orchestrator L113）。
- ✗ `last_response`：无列；最后响应在会话记忆与 `last_response` 语义无处可查。
- ✗ `model_config`：无列；模型选择经瞬态 `metadata.modelId`（`ModelResolver.java:67-73`），不落库。
- ✗ `token_used_input`/`token_used_output`：无列；消耗量序列化在 `token_budget_json` 文本块内（ThinkingStateHandler.java:544-558），无法直接聚合查询。
- △ start_time/end_time：以 `created_at`/`completed_at` 近似（DDL L73-75）；异常终止（FAILED/CANCELLED）时 `completed_at` 不写。
- 增强列：pause_reason、paused_at、cancel_reason、cancelled_at、failure_reason、version（乐观锁）。
**How the verdict was reached:** 核心执行字段在场，但规格用于审计/计费/回放的四类字段（输入、响应、模型配置、Token 用量）均无独立列，判 partial。

---

## REQ-29 — 数据模型 sf_agent_execution_snapshot

> 字段表：id / execution_id / state / context_data(JSONB) / memory_summary / created_at — §5.2

**Verdict:** partial · confidence: medium · severity: **Low**
**What this demands:** 快照表含规格 6 字段。
**Where enforcement lives:** `docker/postgres/init/02-init-schema-agent.sql:91-98`；`entity/SfAgentExecutionSnapshot.java:11-20`；`lifecycle/ExecutionSnapshot.java:18-31`；`lifecycle/ExecutionSnapshotPersistence.java:23-38`。
**Paths walked:**
- ✓ id、execution_id、created_at。
- △ state、context_data：未独立成列，整体序列化进 `snapshot_json TEXT`（ExecutionSnapshot 含 state/contextVariables/chatHistory，L23-28）；`context_data` 非 JSONB 类型。
- ✗ `memory_summary`：无对应字段；快照携带的是 chatHistory 明细而非摘要。
- 增强：`snapshot_hash` + SHA-256 完整性校验（SfAgentExecutionSnapshot.java:19；ExecutionSnapshotPersistence.java:63-68）、Redis 缓存层（`ExecutionSnapshotService`）。
- Open question：`ExecutionSnapshotPersistence.java:31` `setTenantId(null)` 而 DDL `tenant_id BIGINT NOT NULL`；实际是否依赖 `TenantLineInterceptor` 自动补列未验证（运行时行为未决）。
**How the verdict was reached:** 信息以打包形式存在但列级契约不满足，判 partial；confidence medium 因快照写入的租户列行为未决。

---

## notChecked（§6 非功能需求）

| 指标 | 原因 |
|------|------|
| 单执行延迟 P99 < 30s | 需压测环境，静态核查不可裁决 |
| 并发执行数单实例 100+ | 同上；注意 `AgentRuntimeOrchestrator.cancelled` 全局标志（REQ-27）与串行工具执行（REQ-17）构成潜在制约 |
| Token 预算精度误差 < 5% | 需基准测试；存在 `TokenBudgetTest`、`integration/TokenBudgetEnforcementTest` 可作起点；估算器为 4 字符≈1 token（`util/TokenEstimator.java`） |
| 循环检测准确率 > 95% | 需集测；存在 `loop/AgentLoopDetectionServiceTest` |
| （§6 状态机内存泄漏=0 已由 REQ-03 覆盖） | — |

---

## 反向差距（代码存在、规格未提及）

1. **人工审批闸门**：`approval/*`（ToolApprovalService→PAUSED 等待、ApprovalService、MQ ApprovalDecisionConsumer/ApprovalDeferredCreatedConsumer）——整条审批流未见于规格。
2. **扩展状态与恢复机制**：RETRYING（指数退避+熔断，RetryingStateHandler）、GATE_BLOCKED（准入拒绝态+重试倒计时）、RESUMING、PLANNING/REFLECTING/HANDOFF/GROUP_CHAT 等。
3. **守卫与治理**：GuardrailsEngine 输入/输出校验（ThinkingStateHandler.java:152-160）、Skill/Role/Tier 分级注入（L386-405）、SubTaskPlan 多步计划推进（L569-607）。
4. **安全增强**：会话消息租户密钥加密（TenantKeyService）、SSE 载荷脱敏（SecretMasker）、快照 SHA-256 防篡改、执行乐观锁（version）。
5. **可观测与可靠投递**：OTel 工具审计 span、ClickHouse 时间线、outbox+事件回放（web SSE lastSeq 重放）。
6. **疑似死代码/未接线**：`TracedToolExecutionRecorder`（仅被自身测试引用，主链直用 `ToolExecutionRecorder`）；`ContainerToolSandbox` 注入 ToolCallingStateHandler 但未使用（`executeInContainer` 亦为抛错占位，L80-90）。
7. **准入容量泄漏隐患**：速率限流拒绝发生在并发计数之前（ExecutionAdmissionService.java:28-38），但 `AgentRuntimeOrchestrator.java:158` finally 无条件 `releaseConcurrency` → 并发计数器可被递减为负，放大后续准入。
8. **未提交修改评估**：暂停/恢复/取消 Redis Key 迁移为租户作用域（`TenantRedisKeyResolver.executionPaused`），读写两侧（LifecycleService L62-63/74-75/87-88 与 Orchestrator L61-66）键构造一致，方向正确；未引入新规格需求。

## Open questions

1. `ExecutionSnapshotPersistence.java:31` 快照写入 `tenantId=null` vs DDL `NOT NULL`——是否由租户拦截器补列（影响 REQ-29 confidence）。
2. `PausedStateHandler.java:83-84` 注释声明 Token 消耗"v1.1 持久化"，与 §5.1 `token_used_*` 字段路线的关系。
3. `AgentStateMachine.java:57-69` 乐观锁失败时回退直接 `updateById`——并发窗口内转换丢失的可能性未深究（超出本规格条文）。

---

## 反驳复核

> 复核人：独立合规复核员（非原分析作者）· 复核基准：当前工作树实际代码（含 `AgentRuntimeOrchestrator.java`、`AgentExecutionLifecycleService.java` 未提交修改，经 `git diff` 核实仅为暂停 Key 租户作用域化，未触及下述机制）。
> 复核范围：仅 severity ∈ {Critical, High} 且 verdict ∈ {contradicted, partial, absent} 的条目，共 3 条（REQ-02 / REQ-06 / REQ-27）。
> 复核方法：代码方向另寻原作者可能遗漏的执行点——中间件管道（`state/middleware/impl/*`）、`ExecutionConcurrencyService`、`TenantRedisKeyResolver` 全部 Key 帮助器、LLM Provider 各实现、`AgentRouter`/`AgentCard`、异步执行器配置、DDL 触发器/约束、全仓 `Semaphore|maxConcurrent|throttle` 搜索；规格方向重读 §3.1 / §3.2 / §4.3 原文，确认均为无条件强制表述，无豁免、条件或分阶段交付子句。

| REQ | 原判 | 裁定 | 理由(一行) | 证据 |
|-----|------|------|-----------|------|
| REQ-02 | contradicted · High | 维持 | 规格"终端不可再转换"为无条件约束，守卫显式放行 terminal→FAILED，且复核发现更宽缺口：终端清理先移除 Map 条目致 `current==null` 守卫整体失效；未发现任何补强执行点（中间件仅 Audit/Logging/Metrics，`ExecutionConcurrencyService` 只做版本校验且冲突回退无条件写库，DDL 无触发器/约束） | `AgentStateMachine.java:48`（FAILED 例外）、`:100-104`（终端即 `removeExecution`）、`:85-88`（handler 异常递归 `transition(FAILED)`）、`:65-68`（乐观锁冲突回退直接 `updateById`）；`AgentRuntimeOrchestrator.java:122`（终态后循环仍有一次 Redis `isPaused`，抛错入 `:148-154` catch→`transition(FAILED)` 翻转已 COMPLETED 的 DB 状态）、`:144`；`ExecutionConcurrencyService.java:19-30`；`02-init-schema-agent.sql`（sf_agent_execution 无触发器/状态约束）；规格 §3.1 无豁免 |
| REQ-06 | contradicted · High | 维持 | 全仓未见模型级/供应商级并发计数器的任何执行点；唯一供应商相关机制为路由层失败冷却（非准入并发维度、300s≠60s、全局键），准入并发键仅按 tenant×agent 无租户聚合上限 | `ExecutionAdmissionService.java:26-95`（5 维均非规格维度 3/4）；`TenantRedisKeyResolver.java:124-137`（准入键仅 rate/concurrency/cost，`modelCooldown` 帮助器 L170-172 无调用方）；`AiModelRouter.java:24,115-123`；LlmProvider 实现（OpenAi/Anthropic/Adapter/Mock）无并发控制（全仓 `Semaphore|tryAcquire` 搜索无准入命中）；`AgentRouter.java:31-48`（`maxConcurrent` 仅描述性字段不参与校验）；规格 §3.2 四维无条件 |
| REQ-27 | partial · Critical | 维持 | 取消信号为单例编排器全局 `volatile boolean`，`cancel()` 不带 executionId 且无按执行作用域的取消键；执行经线程池（core 10/max 50）并发分发，越界取消与 `resetCancelled()` 竞态实际可达；未提交修改未触及该机制 | `AgentRuntimeOrchestrator.java:53,71-73,78-80,94,129-133`；`AgentExecutionLifecycleService.java:92`（`orchestrator.cancel()` 无参）、`:87-89`（仅删暂停键）；`TenantRedisKeyResolver.java`（无取消键帮助器）；`AgentExecutionEngine.java:23-27`+`AgentExecutionAsyncConfig.java:17-28`（@Async 线程池并发）；`git diff` 核实未提交改动仅为 `executionPaused` 键租户作用域化；规格 §4.3 `/{id}/cancel` 对象限定被破坏 |

**复核结论**：3 条全部维持，0 条推翻。其中 REQ-02 经复核获得补强证据（守卫失效面大于原判描述：终态条目被移除后守卫前提消失，任意后续 `transition` 均可穿过）；REQ-06、REQ-27 在穷尽中间件、其他模块、DDL、配置与未提交修改等潜在豁免/执行点后维持。
