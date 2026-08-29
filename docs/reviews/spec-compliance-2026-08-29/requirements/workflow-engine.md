# Spec 合规核查 — workflow-engine

- **Spec**: `docs/specs/2026-04-30-v1.0-workflow-engine.md`（front-matter `status: 草稿`，任务方声明"已批准"，治理状态不一致，见 Open questions #1）
- **核查对象**: `schemaplexai-workflow/src/main/java`（全部源文件逐一阅读或检索）+ 关联执行点：`schemaplexai-web`、`schemaplexai-task`（MQ 触发）、`schemaplexai-common`（Routing Key）、`application.yml`、`docker/postgres/init/03-init-schema-others.sql`、`resources/processes/*.bpmn20.xml`
- **核查日期**: 2026-08-29
- **裁决计数**: implemented 3 · partial 13 · absent 6 · undecidable 2 · contradicted 0 · stronger-than-spec 0（强于 spec 的行为列入反向差距）

> 事实更正：任务前提"该模块无测试覆盖"不成立 —— `schemaplexai-workflow/src/test/java` 下有 21 个测试文件（controller/delegate/deployer/node/service/approval 各包，含 `ApprovalWorkflowE2ETest.java`）。本核查仅裁决 main 代码的合规性，测试存在与否不改变裁决，但与 agents.md 的项目描述相矛盾（见反向差距 #9）。

---

## REQ-01 — NodeExecutor 节点接口契约

> "public interface NodeExecutor { String getNodeType(); NodeExecutionResult execute(NodeExecutionContext context); }" — spec §3.1

**Verdict:** implemented · confidence: high · 严重度 Low（文档漂移）

**What this demands:** 存在统一的节点执行器接口：类型标识 + 结构化执行结果。

**Where enforcement lives:**
- `schemaplexai-workflow/src/main/java/com/schemaplexai/workflow/node/NodeExecutor.java:5-10` — 接口存在，`getNodeType()` + `execute(...)` 返回 `NodeExecutionResult`
- `node/NodeExecutionResult.java:12-28` — success/message/output 三元组 + 静态工厂
- `service/WorkflowNodeEngine.java:32-36` — `@PostConstruct` 按 `getNodeType()` 注册全部执行器为 Map

**Paths walked:**
- ✓ 注册：Spring 注入 `List<NodeExecutor>` → toMap（WorkflowNodeEngine.java:26,34-35）
- ✓ 分派：`executors.get(nodeType)`，未命中抛 BaseException（WorkflowNodeEngine.java:40-44）

**How the verdict was reached:** 接口的两项本质性质（类型标识、结构化结果）成立。签名与 spec 字面不同：实际为 `execute(Map<String, Object> input, String tenantId)`（NodeExecutor.java:9），全模块不存在 `NodeExecutionContext` 类型。以不同形参机制满足同等能力 = implemented + 文档漂移（修文档或补 Context 类型），不判 contradicted：spec §3.1 未对 Context 内容提出可核查要求，且引擎侧传递的信息（input + tenantId）与"上下文"语义等价。

---

## REQ-02 — AGENT 节点类型与配置契约

> "**类型**: `AGENT` … 职责: 调用 Agent 执行引擎完成 AI 任务 … agentId | Long | 是 … prompt | String | 是（支持变量替换）… timeoutSeconds | Integer | 否 … waitForCompletion | Boolean | 否" — spec §3.2

**Verdict:** partial · confidence: high · **严重度 High**

**What this demands:** 存在类型名为 AGENT 的执行器；必填 agentId + prompt；调用语义为"创建 Agent 执行并跟踪"。

**Where enforcement lives:**
- 最接近实现：`node/AIModelNodeExecutor.java:25-27` —— 类型注册为 `"AI_MODEL"` 而非 `"AGENT"`
- `AIModelNodeExecutor.java:31-34` — prompt 必填校验 ✓
- `AIModelNodeExecutor.java:36-37` — 只消费可选 `modelId`，**没有 agentId 参数**
- `AIModelNodeExecutor.java:58,70` — 同步 POST `{agent.engine.url}/agent/execute`（配置项 L21-22），非 `createExecution(agentId, prompt, tenantId)` 的"创建执行"语义

**Paths walked:**
- ✓ 成功：prompt → agent-engine HTTP → `response.data` → output.generatedText（L70-74）
- ✓ 失败：空响应 → failure（L75-76）；RestClientException → failure（L78-81）
- ✗ agentId：无法指定调用哪个 Agent（参数不存在）
- ✗ timeoutSeconds / waitForCompletion：不存在（见 REQ-04）

**Searched:**
- `getNodeType()` 全仓库 grep → main 代码注册类型全集恰为 9 个：START/END/CONDITION/CONCURRENT/JOIN/AI_MODEL/HTTP/SCRIPT/TOOL_CALL（StartNodeExecutor.java:16、AIModelNodeExecutor.java:26 等），**无 AGENT**
- `createExecution` in schemaplexai-workflow → 0 命中

**How the verdict was reached:** "工作流节点调用 AI 引擎"的能力真实存在（故非 absent）；但类型名不符（AI_MODEL≠AGENT，命名漂移）、核心配置契约 agentId 整体缺失、调用语义从"创建执行+状态跟踪"退化为一次性同步 HTTP——故非 implemented。

---

## REQ-03 — Agent 节点变量替换 `${input.xxx}`

> "1. 变量替换：`prompt` 中的 `${input.xxx}` 替换为上游节点输出" — spec §3.2 执行流程

**Verdict:** absent · confidence: high · **严重度 High**

**What this demands:** 节点参数支持占位符替换，上游节点输出可传递给下游（节点间数据流）。

**Where enforcement lives:** 无。

**Paths walked:**
- ✗ AIModelNodeExecutor.java:31 — prompt 原样取自 input，无任何替换
- ✗ WorkflowInstanceServiceImpl.java:70-90 — 顺序执行循环中，前序节点的 `result.getOutput()` 从未注入后续节点 input：L83 拿到 result 后仅消费 `isSuccess()`（L84）；每个节点的 input 恒等于模板静态配置（L77 来自 `config.getInput()`）

**Searched:**
- `substitut|\$\{input|StrSubstitutor` in schemaplexai-workflow/src/main（-i）→ 0 命中（唯一 `$` 相关命中是 BPMN 条件表达式与 Spring `@Value("${agent.engine.url}")` AIModelNodeExecutor.java:21）
- HttpNodeExecutor / ScriptNodeExecutor / AIModelNodeExecutor 逐行阅读 → 均无替换逻辑

**How the verdict was reached:** 不仅替换语法缺失，承载它的"上游输出传递"机制也不存在——编排循环丢弃每个节点的 output。这不是机制漂移而是能力空缺，故判 absent。后果：多节点工作流无法串联数据，spec 的编排价值主张不成立。

---

## REQ-04 — Agent 节点 waitForCompletion 轮询与超时（默认 300s）

> "3. 如果 `waitForCompletion=true`，轮询状态至终端状态" / "timeoutSeconds | Integer | 否 | 超时时间（默认 300）" — spec §3.2

**Verdict:** absent · confidence: high · **严重度 High**

**What this demands:** 异步执行可轮询至终端状态；调用受默认 300s 超时保护，可产生 TIMEOUT 终态。

**Where enforcement lives:** 无。

**Paths walked:**
- ✗ AIModelNodeExecutor.java:19 — `new RestTemplate()` 无参构造，默认工厂无连接/读取超时 → 调用可无限阻塞
- ✗ AIModelNodeExecutor.java:70 — 单次同步 `postForObject`：无轮询、无执行状态查询、无 TIMEOUT 产出

**Searched:**
- `waitForCompletion` in schemaplexai-workflow/src/main → 0 命中
- `timeoutSeconds` in schemaplexai-workflow/src/main → 仅 ConcurrentNodeExecutor.java:19 javadoc 示例，无生效代码

**How the verdict was reached:** 轮询与超时两个性质均无执行点，判 absent。叠加 `WorkflowNodeEngine.executeNode` 的 `@Transactional`（WorkflowNodeEngine.java:38）与 `trigger()` 的实例级事务（WorkflowInstanceServiceImpl.java:38），一次挂起的 LLM 调用会无限占用数据库事务与 Web 线程，后果重于"缺默认值"，评 High。

---

## REQ-05 — 节点状态机 PENDING → RUNNING → COMPLETED / FAILED / TIMEOUT

> "PENDING → RUNNING → COMPLETED ↓ FAILED / TIMEOUT" — spec §3.2 状态转换

**Verdict:** partial · confidence: high · **严重度 High**

**What this demands:** 节点执行记录经历完整状态流转；失败（含异常）与超时终态均可观测落库。

**Where enforcement lives:**
- PENDING：WorkflowInstanceServiceImpl.java:75（插入时）；DDL 默认 'PENDING'（03-init-schema-others.sql:41）
- RUNNING：WorkflowNodeEngine.java:46-47
- COMPLETED / FAILED（软失败）：WorkflowNodeEngine.java:53-55
- FAILED（异常路径）：WorkflowNodeEngine.java:61-63

**Paths walked:**
- ✓ 成功：PENDING→RUNNING→COMPLETED，output 落库（L53-55）
- ✓ 软失败（执行器返回 failure）：FAILED 落库（L53），实例标 FAILED（WorkflowInstanceServiceImpl.java:84-89）
- ✗ 异常失败：L61-63 写 FAILED 后 L64 抛 BaseException；`executeNode` 标注 `@Transactional(rollbackFor = Exception.class)`（L38）且被 `trigger()` 同事务调用（默认 REQUIRED，WorkflowInstanceServiceImpl.java:38-39）→ FAILED 状态更新**随事务回滚**，库中无失败痕迹，调用方只见异常
- ✗ TIMEOUT：任何路径都不产生（见 REQ-04）
- ✗ Flowable 桥接路径：两个 delegate 构建的 nodeExecution 从未 insert，executeNode 仅 `updateById`（WorkflowNodeEngine.java:47,55,63），id 为 null → 更新 0 行，节点状态无记录（详见 REQ-19）

**Searched:**
- `"TIMEOUT"` in schemaplexai-workflow/src/main → 0 命中

**How the verdict was reached:** 主链 PENDING→RUNNING→COMPLETED/FAILED 存在 → 非 absent；TIMEOUT 缺失 + 异常路径回滚使"失败可观测"在可达分支不成立 → 非 implemented。

---

## REQ-06 — HTTP 节点

> "使用 WebClient 发送请求（非阻塞）… 超时处理：超时后标记为 FAILED … 响应解析：将 JSON 响应转为 Map 供下游使用 … headers | Map | 否 … timeoutSeconds | Integer | 否 | 默认 30" — spec §3.3

**Verdict:** partial · confidence: high · 严重度 Medium

**What this demands:** url/method/headers/body/timeoutSeconds 五项配置生效；超时→FAILED；JSON 响应解析为 Map。

**Where enforcement lives:** `node/HttpNodeExecutor.java`
- 类型 "HTTP"：L27-29 ✓
- url 必填校验：L37-40 ✓；method 解析（含 GET/POST/PUT/DELETE）：L69-75 ✓
- 超时：构造器硬编码 connect 5s / read 30s（L21-22）——默认 30s 符合，但**不可按节点配置**
- 超时/异常 → RestClientException → failure → 引擎标 FAILED：L63-66 ✓

**Paths walked:**
- ✓ 成功：exchange → statusCode/body/headers 输出（L54-61）
- ✓ 失败/超时：catch → failure（L63-66）
- ✗ headers 配置：`input.get("headers")` 从未读取，Content-Type 硬编码 APPLICATION_JSON（L48-49）
- ✗ timeoutSeconds：无读取（L21-22 固定值）
- ✗ JSON→Map：body 以原始 String 返回（L58），下游无法按字段取值

**Searched:**
- `get("headers")` in HttpNodeExecutor.java → 0 命中
- `WebClient|webflux` in schemaplexai-workflow → 0 命中；pom.xml 无 webflux 依赖（pom.xml:12 仅 flowable-spring-boot-starter 等）

**How the verdict was reached:** RestTemplate 替代 WebClient 属实现选型漂移（同步/非阻塞不改变功能性质），单独不扣分；但 headers、按节点超时、JSON 解析三个契约点缺失且为 spec 明文要求 → partial。

---

## REQ-07 — SCRIPT 节点执行能力（groovy / javascript）

> "**类型**: `SCRIPT` … 职责: 执行脚本代码 … language | String | 是 | `groovy` / `javascript` … script | String | 是 … timeoutSeconds 默认 60" — spec §3.4

**Verdict:** absent · confidence: high · **严重度 High**

**What this demands:** 能实际执行 groovy/javascript 脚本并返回结果。

**Where enforcement lives:** 仅存注册占位：`node/ScriptNodeExecutor.java:13-22` —— 无条件返回 `failure("SCRIPT node execution is not implemented...")`（L21）。

**Paths walked:**
- ✗ 唯一路径：任何输入 → 固定 failure（L18-22）；`language` 参数甚至未被读取

**Searched:**
- `groovy|nashorn|graal|ScriptEngine` in schemaplexai-workflow（-i）→ 0 命中
- workflow pom.xml 依赖清单 → 无任何脚本引擎依赖

**How the verdict was reached:** 执行器注册了但没有任何一条输入能产生脚本执行——"永败存根"不满足 partial 所要求的至少一条成立路径，判 absent。spec §1 宣称的"补充缺失节点执行器"目标在 SCRIPT 上未达成。同类存根（非本规格要求，列反向差距）：TOOL_CALL（ToolCallNodeExecutor.java:26-27）、CONCURRENT（ConcurrentNodeExecutor.java:49-50）。

---

## REQ-08 — SCRIPT 沙箱安全约束

> "使用沙箱执行（Groovy Sandbox / Nashorn with security manager）；禁止访问文件系统、网络、反射；仅允许访问白名单类" — spec §3.4 安全约束

**Verdict:** absent · confidence: high · 严重度 Low（当前空洞成立）

**What this demands:** 脚本执行受沙箱与类白名单约束。

**Where enforcement lives:** 无。

**Paths walked:**
- ✗ 无脚本执行路径存在（REQ-07），沙箱无可附着点

**Searched:**
- `sandbox|SecurityManager|whitelist|allowlist` in schemaplexai-workflow/src/main（-i）→ 0 命中

**How the verdict was reached:** 约束依附于执行能力；能力缺失使约束"空洞地未被违反"（当前无任意代码执行风险，故 Low）。必须记为 absent 而非 implemented：一旦补执行器而不补沙箱即变 Critical。建议将本条作为 SCRIPT 实现任务的验收门禁。

---

## REQ-09 — HUMAN_APPROVAL 节点

> "**类型**: `HUMAN_APPROVAL` … 1. 创建审批任务（写入 `sf_approval` 表）2. 工作流状态设为 `WAITING_APPROVAL` 3. 审批完成后，通过回调继续工作流 4. 超时未审批，自动拒绝" — spec §3.5

**Verdict:** partial · confidence: high · **严重度 High**

**What this demands:** AI 节点工作流可暂停等待人工审批：sf_approval 表、WAITING_APPROVAL 状态、审批回调续跑。

**Where enforcement lives:**（以不同机制在 Flowable 侧部分满足）
- `resources/processes/agent-execution-approval.bpmn20.xml:45-52,69-76` — userTask（assignee 表达式 + candidateGroups="senior-approvers"）实现"暂停等人"
- `deployer/BpmnApprovalDeployer.java:75-96` — `completeTask(taskId, approved, reason)` 写 approvalDecision 并 complete = "回调继续工作流"
- `delegate/NotifyEngineDelegate.java:60-73` — 决策经 MQ 事件（exchange "approval"）回传引擎
- 注意：spec §7 边界表声明"人工审批流程 → 使用 Flowable=是 / WorkflowNodeEngine=否"，与 §3.5 把 HUMAN_APPROVAL 定义为 NodeEngine 节点类型**属 spec 内部矛盾**（Open question #3）

**Paths walked:**
- ✓ Flowable 审批：暂停 → completeTask(APPROVE/REJECT) → 网关 f4-approve/f4-reject → notify → end（agent-execution-approval.bpmn20.xml:128-133,156-157）
- ✗ AI 节点工作流中的 HUMAN_APPROVAL：模板含该类型 → `executors.get("HUMAN_APPROVAL")` = null → BaseException（WorkflowNodeEngine.java:40-44）
- ✗ WAITING_APPROVAL 实例状态：`trigger()` 只写 RUNNING/FAILED/COMPLETED（WorkflowInstanceServiceImpl.java:66,85,92）
- ✗ sf_approval 表：不存在（见 Searched）

**Searched:**
- `HUMAN_APPROVAL` 全仓库（java/xml/sql）→ 代码 0 命中（仅 spec 与报告文件）
- `WAITING_APPROVAL` 全仓库 → 代码 0 命中
- `sf_approval` 全仓库 → 仅相近物：`sf_approval_ticket`（agent-engine Flyway V2026_05_09…sql:51、quality V2026_05_10_1…sql:5，属 Agent 执行审批票据，非本规格表）；`docker/postgres/init/*.sql` 无 sf_approval

**How the verdict was reached:** "人工审批暂停/继续/拒绝"的能力在 Flowable 路径真实可走通 → 非 absent；但 §3.5 的三项落地契约（HUMAN_APPROVAL 节点类型、sf_approval、WAITING_APPROVAL）在 AI 节点工作流侧全部缺失 → 非 implemented。

---

## REQ-10 — 审批超时自动拒绝（默认 24h）

> "4. 超时未审批，自动拒绝" / "timeoutHours | Integer | 否 | 默认 24" — spec §3.5

**Verdict:** partial · confidence: high · 严重度 Medium

**What this demands:** 审批任务有超时定时器，到期自动走拒绝路径，默认 24 小时。

**Where enforcement lives:**
- `agent-execution-approval.bpmn20.xml:55-60` — primarySlaTimer boundary（`PT${slaTimeoutHours != null ? slaTimeoutHours : 4}H`，cancelActivity=true）；f3-sla（L125）→ **升级**而非直接拒绝
- `agent-execution-approval.bpmn20.xml:79-84,145` — escalatedSlaTimer PT24H → notifyAutoReject
- `delegate/SlaAutoRejectListener.java:17-19` — 置 approvalDecision=REJECT + rejectionReason
- `delegate/InitApprovalDelegate.java:31-33` — slaTimeoutHours 默认 = CRITICAL?2:4（**非 spec 的 24**；24 仅出现在升级后的第二级，EscalationDelegate.java:41-43）
- 定时器可实际触发：application.yml:34 `async-executor-activate: true`

**Paths walked:**
- ✓ 升级后超时自动拒绝：escalatedSlaTimer → SlaAutoRejectListener → notifyAutoReject → rejectedEnd（f6-sla L145、f10 L158）
- ✓ 主审超时：→ 升级（先升后拒，两级合计约 4h+24h）——语义强于"直接拒绝"（列入反向差距）
- ✗ `spec-review-approval.bpmn20.xml`：全文件无任何 timerEventDefinition/boundaryEvent（逐行阅读确认）→ 该审批流超时将永远挂起
- ✗ AI 节点工作流：无审批节点（REQ-09）

**How the verdict was reached:** 核心审批流程的超时自动拒绝真实存在且有专用监听器 → 非 absent；默认时长与 spec 不一致（4h/2h vs 24h）、spec-review 流程完全无超时保护、AI 节点路径缺失 → 非 implemented。

---

## REQ-11 — 实例生命周期状态（RUNNING / WAITING_APPROVAL / COMPLETED / FAILED / CANCELLED）

> "RUNNING 实例执行中 … WAITING_APPROVAL 等待人工审批 … COMPLETED 正常完成 … FAILED 执行失败 … CANCELLED 用户取消" — spec §4

**Verdict:** partial · confidence: high · **严重度 High**

**What this demands:** 实例状态覆盖五个运行期状态，且 WAITING_APPROVAL、CANCELLED 可由正常操作触达。

**Where enforcement lives:**
- RUNNING：WorkflowInstanceServiceImpl.java:66；DDL 默认 'RUNNING'（03-init-schema-others.sql:25）
- COMPLETED：WorkflowInstanceServiceImpl.java:92
- FAILED：WorkflowInstanceServiceImpl.java:57（拓扑失配）、:85（节点软失败）

**Paths walked:**
- ✓ RUNNING→COMPLETED（全节点成功，L66→L92）
- ✓ RUNNING→FAILED（软失败 L84-89；拓扑失配 L52-60）
- ✗ →CANCELLED：无任何代码写入该状态、无取消入口（见 REQ-17）；`cancelledEndEvent`（ai-agent-execution.bpmn20.xml:90）仅是 Flowable 流程终点命名，不写实例表
- ✗ →WAITING_APPROVAL：代码 0 命中（REQ-09 Searched）
- ✗ 异常失败分支：FAILED 因事务回滚不落库（REQ-05）

**Searched:**
- `CANCELLED` in schemaplexai-workflow/src/main → Java 代码 0 命中（仅 BPMN XML 的 endEvent id）

**How the verdict was reached:** 三个状态可达 → 非 absent；五个中两个（含用户取消这一强制能力）不可达 → 非 implemented。补充：同步阻塞执行模型（trigger 内联跑完全部节点才返回，WorkflowInstanceServiceImpl.java:39-95）使运行中实例在当前架构下也无"取消"落点。

---

## REQ-12 — DRAFT 模板不可执行 / PUBLISHED 才可创建实例

> "`DRAFT` | 模板编辑中，不可执行；`PUBLISHED` | 模板已发布，可创建实例" — spec §4

**Verdict:** absent · confidence: high · **严重度 Critical**

**What this demands:** 存在状态门禁：非发布态模板不能触发执行。

**Where enforcement lives:** 无。

**Paths walked:**
- ✗ 创建实例：WorkflowInstanceController.java:23-27 直接 `save(instance)`，不校验 templateId 有效性、不校验模板状态
- ✗ 触发实例：WorkflowInstanceServiceImpl.java:45-48 仅校验模板存在，`template.getStatus()` 从未被读取 → 任何状态（含 draft/编辑中）的模板均可触发
- （对照）模板侧存在状态机 draft/deployed/inactive（WorkflowTemplateServiceImpl.java:24-26），但只约束 deploy/deactivate 自身转换（L34-36、L97-99），从不被实例路径消费

**Searched:**
- `getStatus` in WorkflowInstanceServiceImpl.java → 0 命中
- `STATUS_DEPLOYED` in schemaplexai-workflow/src/main → 仅 WorkflowTemplateServiceImpl.java:34,86,97，实例侧 0 引用

**How the verdict was reached:** 门禁机制完全不存在，且不是"不同机制满足"：spec 的强制控制项（DRAFT 不可执行）在完全可达的路径上（创建模板 → 创建实例 → `POST /{id}/trigger`）不生效，按后果评级为 Critical；裁决 absent（代码未实现相反规则，而是没有规则，故不判 contradicted）。

---

## REQ-13 — 数据模型 sf_workflow_template

> "definition | JSONB | 节点定义数组；status | VARCHAR | DRAFT / PUBLISHED；version | INT | 版本号" — spec §5.1

**Verdict:** partial · confidence: high · 严重度 Medium

**What this demands:** 模板表含 definition（JSONB）、status（DRAFT/PUBLISHED）、version（INT）字段。

**Where enforcement lives:**
- DDL：`docker/postgres/init/03-init-schema-others.sql:7-19` — id/tenant_id/name ✓；`node_config_json TEXT`（L12）承载 definition（字段名+类型漂移）；`status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE'`（L13）
- 实体：`entity/SfWorkflowTemplate.java:13-16` — name/description/nodeConfigJson/status（+ BaseEntity 的 tenant_id 等）

**Paths walked:**
- ✓ definition 语义：nodeConfigJson 被解析为节点数组执行（WorkflowInstanceServiceImpl.java:97-107）——机制漂移但功能等价
- ✗ version：实体与 DDL 均无该列；clone 不递增版本（WorkflowTemplateServiceImpl.java:70-76 直接复制 nodeConfigJson）→ 无版本管理能力
- ✗ status 词汇三方不一致：spec = DRAFT/PUBLISHED；代码 = draft/deployed/inactive（WorkflowTemplateServiceImpl.java:24-26）；DDL 默认 = 'ACTIVE'（L13）。'ACTIVE' 是死值：模板创建时 MP 省略 null 字段 → DB 落 'ACTIVE'，任何代码路径都不识别它（deployTemplate 仅拦 "已 deployed"，L34-37）

**Searched:**
- `version` in SfWorkflowTemplate.java / 03-init-schema-others.sql L7-19 → 0 命中

**How the verdict was reached:** 表存在、核心字段可用 → 非 absent；version 缺失 + status 三方漂移（含 DDL 死值）→ 非 implemented。

---

## REQ-14 — 数据模型 sf_workflow_instance

> "input_data | JSONB | 输入参数；output_data | JSONB | 输出结果；start_time | TIMESTAMP | 开始时间；end_time | TIMESTAMP | 结束时间" — spec §5.2

**Verdict:** partial · confidence: high · **严重度 High**

**What this demands:** 实例记录输入、输出与起止时间。

**Where enforcement lives:**
- DDL：03-init-schema-others.sql:21-33 — id/tenant_id/template_id/status ✓；`started_at`/`completed_at`（L28-29，对应 start_time/end_time，命名漂移）；另有 spec 未提的 trigger_type/trigger_config（L26-27）
- 实体：`entity/SfWorkflowInstance.java:13-17` — templateId/status/triggerType/triggerConfig/topologyHash

**Paths walked:**
- ✗ input_data/output_data：DDL 无列、实体无字段、`trigger()` 不接收也不产出实例级输入输出（WorkflowInstanceServiceImpl.java:39-95）
- ✗ started_at/completed_at：DDL 有列但实体未映射（SfWorkflowInstance.java 无对应字段），`trigger()` 全程不写时间 → 两列永远为 NULL
- ✓ template_id/tenant_id/status：实体+DDL+代码路径齐备

**Searched:**
- `input_data|output_data` in 03-init-schema-others.sql → 0 命中
- `startedAt|completedAt` in SfWorkflowInstance.java → 0 命中

**How the verdict was reached:** 表与主干字段在 → 非 absent；四个 spec 字段两个完全缺失、两个有列无代码写入（数据永远 NULL）→ 非 implemented。后果：实例无输入输出留痕、无耗时统计。

---

## REQ-15 — 数据模型 sf_workflow_node_execution

> "node_type | VARCHAR | AGENT / HTTP / SCRIPT / HUMAN_APPROVAL；error_message | TEXT | 错误信息" — spec §5.3

**Verdict:** partial · confidence: high · 严重度 Low

**What this demands:** 节点执行表记录类型、输入输出与独立错误信息字段。

**Where enforcement lives:**
- DDL：03-init-schema-others.sql:35-49 — instance_id/node_id/node_type/status/input_json/output_json ✓（input_data→input_json、TEXT 非 JSONB，漂移）；另有 started_at/completed_at（L44-45，同样无代码写入）
- 实体：`entity/SfWorkflowNodeExecution.java:13-18` ✓
- 错误信息承载：WorkflowNodeEngine.java:62 — 异常时把 `{"error":"..."}` 写进 **output_json**（不同机制承载 error_message 语义）

**Paths walked:**
- ✓ 成功/软失败：output_json 落库（WorkflowNodeEngine.java:54-55）
- ✗ error_message 独立列：DDL/实体均无；且异常路径的 error JSON 随事务回滚实际不落库（REQ-05）
- 注：DDL 注释的 node_type 词汇（L40：TRIGGER/DOCUMENT/AGENT/APPROVAL/…）与代码实际注册类型（9 类，见 REQ-02）完全不同——注释性漂移

**Searched:**
- `error_message|errorMessage` in schemaplexai-workflow/src/main + 03-init-schema-others.sql → 0 命中

**How the verdict was reached:** 错误信息以 output_json 内嵌承载 = 不同机制满足；主干齐备。扣为 partial 仅因：异常分支错误留痕被回滚吞掉 + error_message 独立列缺失。严重度 Low。

---

## REQ-16 — 模板管理 API

> "POST /workflow/templates；GET /workflow/templates；GET /workflow/templates/{id}；PUT /workflow/templates/{id}；POST /workflow/templates/{id}/publish；DELETE /workflow/templates/{id}" — spec §6.1

**Verdict:** implemented · confidence: high · 严重度 Low（文档漂移）

**What this demands:** 模板 CRUD + 列表 + 发布六项能力可用。

**Where enforcement lives:** `controller/WorkflowTemplateController.java`（@RequestMapping "/workflow/templates" L17）
- POST 创建 L25-29 ✓ · PUT 更新 L32-36 ✓ · DELETE L39-42 ✓ · GET /{id} L45-52 ✓
- 列表 → `GET /workflow/templates/page`（L55-60，分页）——非裸 `GET /workflow/templates`
- 发布 → `POST /{id}/deploy`（L63-66）——路径名为 deploy 非 publish

**Paths walked:**
- ✓ 六项能力全部连通 Service（save/updateById/removeById/getById/page/deployTemplate）；deploy 含重复部署防护（WorkflowTemplateServiceImpl.java:34-36）
- ✗（契约字面）：裸 `GET /workflow/templates` 与 `POST /{id}/publish` 无映射——按 spec 编写的客户端会 404

**How the verdict was reached:** 全部能力以等价端点提供（list→/page、publish→/deploy）= 不同机制满足 → implemented + 文档漂移（修 spec 或加路径别名）。另有 spec 未提的 /validate、/clone、/deactivate、/deployed 四端点（L69-90），列反向差距。

---

## REQ-17 — 实例执行 API（创建并触发 / 取消 / 审批通过 / 审批拒绝）

> "POST /workflow/instances # 创建并触发实例 … POST /workflow/instances/{id}/cancel # 取消 … /{id}/approve # 人工审批通过 … /{id}/reject # 人工审批拒绝" — spec §6.2

**Verdict:** partial · confidence: high · **严重度 High**

**What this demands:** 创建即触发；提供取消与实例级审批回调端点。

**Where enforcement lives:** `controller/WorkflowInstanceController.java`（"/workflow/instances" L15）
- POST 创建 L23-27 —— 仅 `save()` **不触发**；触发为独立的 `POST /{id}/trigger`（L61-65，该端点 spec 未定义）
- GET /{id} L43-50 ✓；列表在 `/page` L53-58（同 REQ-16 漂移）

**Paths walked:**
- ✓ 创建 + 手动触发两步可达执行（create → trigger）
- △ 创建路径健壮性（静态推断，medium confidence）：DDL `trigger_type` NOT NULL 且无默认值（03-init-schema-others.sql:26），模块无 MetaObjectHandler 自动填充（config/MyBatisPlusConfig.java:17-23 仅注册拦截器）→ 请求体不带 triggerType 时 INSERT 违反非空约束
- ✗ "创建并触发"原子语义：create 不调 trigger（L23-27）
- ✗ `POST /{id}/cancel`：控制器无映射；服务接口只有 `trigger(Long)`（WorkflowInstanceService.java:8）
- ✗ `POST /{id}/approve`、`/{id}/reject`：控制器无映射。最接近的物是 web 模块的 `/web/approvals/{ticketId}/approve|reject`（ApprovalWebController.java:40-52）与 `/web/executions/{id}/cancel`（ExecutionWebController.java:62）——作用于审批票据与 Agent 执行，资源、路径、语义均不同，不构成等价实现

**Searched:**
- `Mapping.*(approve|reject|cancel)` in schemaplexai-web/schemaplexai-workflow → 命中仅上述票据/执行端点，无 workflow instance 端点
- `cancel` in WorkflowInstanceService*.java → 0 命中

**How the verdict was reached:** 创建/查询/触发存在 → 非 absent；取消与实例级审批回调缺失、创建-触发拆分改变原子语义 → 非 implemented。与 REQ-11（CANCELLED 不可达）互为印证。

---

## REQ-18 — Flowable 职责边界

> "人工审批流程 | 是(Flowable)；会签/或签 | 是；定时触发 | 是；Agent 执行 | 否(=WorkflowNodeEngine)；HTTP 调用 | 否；脚本执行 | 否；复杂条件分支 | 是" — spec §7

**Verdict:** partial · confidence: medium · 严重度 Medium

**What this demands:** 审批/会签/定时归 Flowable；Agent/HTTP/Script 归 NodeEngine。

**Where enforcement lives:**
- 人工审批 → Flowable ✓：三个 BPMN 的 userTask + BpmnApprovalDeployer.completeTask（BpmnApprovalDeployer.java:75-96）
- Agent/HTTP → NodeEngine ✓：AIModelNodeExecutor、HttpNodeExecutor 注册于引擎（WorkflowNodeEngine.java:32-36）
- 复杂条件分支 → Flowable ✓：exclusiveGateway + 条件表达式（agent-execution-approval.bpmn20.xml:128-136,148-153；ai-agent-execution.bpmn20.xml:98-103,119-124；spec-review-approval.bpmn20.xml:94-99）

**Paths walked:**
- ✗ 会签/或签：三个 BPMN 均无 multiInstanceLoopCharacteristics（见 Searched）；candidateGroups（spec-review-approval.bpmn20.xml:70）只是候选组，非会签
- ✗ 定时触发：无 timer **start** event；timerEventDefinition 仅存在于 agent-execution-approval 的两个 boundary SLA 定时器（:57,81）。MQ 触发链路（`sf.workflow.trigger`）的 handler 是抛错桩：UnsupportedWorkflowTriggerRequestHandler.java:14-20
- ✗ 脚本执行 → NodeEngine：类型注册但为永败存根（REQ-07）
- △ AI 节点经 Flowable delegate 进入 NodeEngine（AiAgentExecutionDelegate、FlowableDelegateAdapter）是 spec 自己的桥接设计，不算违反

**Searched:**
- `multiInstance` in resources/processes/ → 0 命中
- `timerEventDefinition` in resources/processes/ → 4 命中，全为 boundary 事件（agent-execution-approval.bpmn20.xml:57,59,81,83）

**How the verdict was reached:** 边界表 7 行中 4 行成立（审批、Agent、HTTP、条件分支），3 行缺失（会签、定时触发、脚本）。confidence medium：边界表语义介于"归属划分"与"必须已实现"之间，但结合 §1"补充缺失节点执行器"的目标，缺失项仍计缺口。

---

## REQ-19 — FlowableDelegateAdapter 桥接

> "**桥接方式**: `FlowableDelegateAdapter` 将 Flowable ServiceTask 代理到 `WorkflowNodeEngine`" — spec §7

**Verdict:** partial · confidence: high · **严重度 Critical**

**What this demands:** Flowable ServiceTask 经适配器能实际执行 NodeEngine 节点、得到结果并留下执行记录。

**Where enforcement lives:**
- `service/FlowableDelegateAdapter.java:16-39` — JavaDelegate，构建 SfWorkflowNodeExecution → `nodeEngine.executeNode`（L36）→ 回写 nodeResult/nodeOutput（L37-38）：结构桥接成立
- `delegate/AiAgentExecutionDelegate.java:66-73` — 平行 AI 桥接：setNodeType("AI_AGENT")（L68）→ executeNode（L73）

**Paths walked:**
- ✗ **AI_AGENT 路径必炸**：引擎注册表无 "AI_AGENT"（注册全集见 REQ-02 Searched）→ WorkflowNodeEngine.java:40-44 抛 BaseException → AiAgentExecutionDelegate.java:98-107 catch 后 rethrow AGENT_EXECUTION_FAILED（L106-107）→ 随附的 ai-agent-execution.bpmn20.xml（executeAgentTask L45-46、reExecuteAgentTask L75-76）**任何一次执行都以异常终止**
- ✗ 默认类型路径：FlowableDelegateAdapter.java:31-33 在 nodeType 变量缺省时回落 "SCRIPT" —— 永败存根（REQ-07），默认桥接必失败
- ✗ 审计路径：两个桥接类构建的 nodeExecution 均**未 insert**（无 instanceId、无 id），而 executeNode 只做 `updateById`（WorkflowNodeEngine.java:47,55,63）→ id 为 null 更新 0 行，**BPMN 路径的节点执行记录从不落库**
- ✗ 租户路径：FlowableDelegateAdapter 从不设置 tenantId（L28-36 无 setTenantId 调用）→ 执行器收到 tenantId=null，租户隔离在桥接路径失效（对照 AiAgentExecutionDelegate.java:70 有设置）
- ✓ 仅当流程变量显式指定已实现类型（如 nodeType=HTTP）时，执行与变量回写可走通（L36-38）

**How the verdict was reached:** 适配器类存在且机制正确 → 非 absent；但三条主要运行路径中两条必然失败、一条丢审计、租户丢失，"桥接可用"仅在窄条件下成立 → partial。Critical 依据后果：模块自带的 BPMN AI 执行流程（spec §1 两种模式的结合点）在当前代码下必失败。

---

## REQ-20 — 节点执行超时可配置（默认 30s HTTP / 300s Agent）

> "节点执行超时 | 可配置，默认 30s（HTTP）/ 300s（Agent）" — spec §8

**Verdict:** partial · confidence: high · **严重度 High**

**What this demands:** 两类节点均有默认超时且可按配置覆盖。

**Where enforcement lives:**
- HTTP：HttpNodeExecutor.java:21-22 — read 30s（默认值符合）/ connect 5s，**硬编码**
- Agent（AI_MODEL）：AIModelNodeExecutor.java:19 — `new RestTemplate()` **无任何超时**

**Paths walked:**
- ✓ HTTP 默认 30s 生效（读超时 → RestClientException → FAILED，HttpNodeExecutor.java:63-66 + WorkflowNodeEngine.java:53）
- ✗ HTTP 可配置：节点配置中的 timeoutSeconds 从未读取（见 REQ-06 Searched）
- ✗ Agent 默认 300s：不存在；且调用处于 @Transactional 内（REQ-04），可无限阻塞并占用事务

**Searched:**
- `timeoutSeconds` in schemaplexai-workflow/src/main/java → 仅 javadoc/注释，无生效代码
- `setReadTimeout|setConnectTimeout` in AIModelNodeExecutor.java → 0 命中

**How the verdict was reached:** HTTP 半条成立（默认值对、不可配）；Agent 整条缺失且方向更危险（无界阻塞）。非 contradicted：未实现相反的超时策略，是缺失。

---

## REQ-21 — 失败重试 3 次、指数退避

> "失败重试 | 支持 3 次重试，指数退避" — spec §8

**Verdict:** absent · confidence: high · **严重度 High**

**What this demands:** 节点/工作流失败后自动重试至多 3 次，间隔指数增长。

**Where enforcement lives:** 无。

**Paths walked:**
- ✗ NodeEngine：失败即标 FAILED 返回/抛出（WorkflowNodeEngine.java:53,58-64），无重试循环
- ✗ 实例编排：首个失败节点即终止实例（WorkflowInstanceServiceImpl.java:84-89）
- ✗ MQ 消费侧：WorkflowTriggerConsumer.java:59,63 — `basicNack(requeue=false)`，显式放弃重投
- △ BPMN `retryCount`（AiAgentExecutionDelegate.java:44-47,91-95；ai-agent-execution.bpmn20.xml:122-127 aflow8-fail→humanFeedbackTask→reExecuteAgentTask）是**人工反馈后重跑**计数器：需人介入、无次数上限、无退避——语义不同，不构成等价

**Searched:**
- `retry|backoff` in schemaplexai-workflow/src/main（-i）→ 仅上述 retryCount 人工循环命中
- workflow pom.xml → 无 spring-retry / resilience4j

**How the verdict was reached:** 自动重试与指数退避两个性质均无执行点；人工重跑循环不满足"自动 + 3 次上限 + 指数退避"三要素，判 absent。

---

## REQ-22 — 工作流触发延迟 P99 < 500ms

> "工作流触发延迟 | P99 < 500ms" — spec §8

**Verdict:** undecidable · confidence: high

**What this demands:** 运行时性能指标，需压测/监控数据佐证。

**Where enforcement lives:** 静态代码无法证实或证伪；无性能基准测试。

**Paths walked:**
- △ 结构性反向信号：`POST /{id}/trigger` 同步内联执行**全部**节点（含外部 HTTP/LLM 调用）后才返回（WorkflowInstanceServiceImpl.java:39-95）。若"触发延迟"按 API 响应时间度量，任何含 AI_MODEL/HTTP 节点的流程几乎必然超标；若按"状态落 RUNNING 时间"度量则不可静态判定

**How the verdict was reached:** 无运行数据 → undecidable。记录结构性张力：同步阻塞设计与该 NFR 冲突，建议先定义测量点（Open question #4）。

---

## REQ-23 — 单服务并发实例 100+

> "并发实例数 | 单服务 100+" — spec §8

**Verdict:** undecidable · confidence: high

**What this demands:** 运行时容量指标。

**Where enforcement lives:** 无压测数据。结构性信号：每个运行中实例占用一个 Web 线程 + 一个数据库事务直至全部节点完成（WorkflowInstanceServiceImpl.java:38-95）；AI_MODEL 无超时（REQ-20）放大占用；application.yml 未调整线程/连接池参数（application.yml:1-2 仅端口）。

**Paths walked:** 不适用（容量属性）。

**How the verdict was reached:** 不可静态裁决；记录结构性风险供容量测试关注。

---

## REQ-24 — BPMN 流程部署与启动（Flowable 模式）

> "**BPMN 工作流**: 基于 Flowable 引擎的标准业务流程（审批、会签、定时触发）" — spec §1；架构图含 "Flowable Engine (BPMN)" — spec §2

**Verdict:** implemented · confidence: high

**What this demands:** BPMN 定义可部署到 Flowable、可启动实例、可查询。

**Where enforcement lives:**
- `service/WorkflowDeployService.java:40-66` — ApplicationReadyEvent 扫描 `classpath:processes/*.bpmn20.xml`（L35）自动部署；按部署名去重防重复（L71-79）
- 资源：`resources/processes/` 3 个流程（agent-execution-approval / ai-agent-execution / spec-review-approval），均 isExecutable=true
- 启动：WorkflowDeployService.java:131-154（含 businessKey 分支、定义缺失抛 WORKFLOW_NOT_FOUND L139-142）；REST 入口 WorkflowBpmnController.java:36-49
- 配置：application.yml:32-46 — `flowable.deployment.enabled: false`（L37-38，改用显式部署服务）、`async-executor-activate: true`（L34）、`history-level: full`（L35）

**Paths walked:**
- ✓ 启动自动部署 + 去重 + 失败抛 BaseException（L62-65、L94-98）
- ✓ 按 key 启动实例；挂起/激活定义（L159-192 —— spec 未要求，强于 spec，列反向差距）
- ✓ 流程列表（WorkflowBpmnController.java:30-33）

**How the verdict was reached:** 部署与启动两条主路径完整、有防重与异常处理 → implemented。会签/定时触发的缺口已在 REQ-18 单独裁决，不重复扣分。（注意：部署机制正常 ≠ 被部署的流程可正常执行——ai-agent-execution 的执行路径必炸，见 REQ-19。）

---

## 反向差距（模块存在、规格未提及的重要行为）

1. **拓扑哈希防护**：触发时用 SHA-256 校验模板拓扑，失配 → 实例标 FAILED 并抛错（WorkflowInstanceServiceImpl.java:50-64；TopologyHasher.java:30-63；实体字段 SfWorkflowInstance.java:17）。spec 无此概念。
2. **7 个 spec 外节点类型**：START/END/CONDITION/JOIN（已实现：StartNodeExecutor、EndNodeExecutor、ConditionNodeExecutor、JoinNodeExecutor）+ CONCURRENT/TOOL_CALL（显式未实现存根：ConcurrentNodeExecutor.java:49-50、ToolCallNodeExecutor.java:26-27）+ AI_MODEL（替代 AGENT）。spec 只定义 AGENT/HTTP/SCRIPT/HUMAN_APPROVAL 四类。
3. **模板扩展 API 与 inactive 状态**：/validate、/clone、/deactivate、/deployed（WorkflowTemplateController.java:69-90）；状态机含 inactive（WorkflowTemplateServiceImpl.java:26,92-104）。
4. **BPMN 管理面**：/workflow/bpmn/processes 列表、/start、/suspend、/activate（WorkflowBpmnController.java:29-63）。
5. **多级审批升级链**：ESCALATE 决策、senior-approvers 候选组、两级 SLA（4h→24h）、EscalationDelegate 升级审计（agent-execution-approval.bpmn20.xml:66-84；EscalationDelegate.java:28-52）——强于 spec 的"超时直接拒绝"。
6. **审批决策 MQ 事件**：NotifyEngineDelegate.java:27-28,73 发布到 exchange `"approval"` / routing key `"approval.decisions"`——既不在本 spec，也不走 agents.md 约定的 `sf.exchange`（跨文档不一致）。
7. **MQ 触发链路半成品**：队列/绑定/常量齐备（task RabbitMqConfig.java:58；CommonConstants.java:38；WorkflowTriggerConsumer.java:46 含解析与校验），但 handler 为固定抛错的 `UnsupportedWorkflowTriggerRequestHandler`（:14-20）→ 所有 MQ 触发进失败日志并 nack 不重投。spec 未提 MQ 触发。
8. **执行模型为平铺顺序列表**：definition 解析为 `List<NodeConfig>{nodeId,nodeType,input}`（WorkflowInstanceServiceImpl.java:112-116）逐个执行，无边/分支接线；CONDITION 输出的 `branch`（ConditionNodeExecutor.java:29-31）无消费者；且 definition 解析失败被吞（L103-106 返回空列表）→ **损坏/非法模板触发后直接标 COMPLETED**（L69→L92 空循环）。
9. **测试存在**：`src/test` 下 21 个测试文件（含 ApprovalWorkflowE2ETest），与 agents.md"全部 16 个模块无测试"的描述矛盾——项目文档过期。
10. **实例创建隐式约束**：`trigger_type` NOT NULL 无默认值（03-init-schema-others.sql:26）+ 无字段自动填充 → `POST /workflow/instances` 不带 triggerType 时 INSERT 违反约束（静态推断，见 REQ-17）。

## Open questions

1. spec front-matter `status: 草稿` vs 任务声明"已批准"——以哪个为准，影响"修文档还是修代码"的整改方向。
2. AGENT→AI_MODEL 的改名与 createExecution→同步 HTTP 的语义变更是否有决策记录（本 spec 之外的 docs/ 未核查到）。
3. spec §3.5（HUMAN_APPROVAL 为 NodeEngine 节点、sf_approval 表）与 §7（人工审批归 Flowable）内部矛盾，需 spec 修订裁决；且 `sf_approval` 疑似已被 `sf_approval_ticket`（agent-engine/quality 迁移）+ Flowable ACT_* 表取代。
4. REQ-22 "触发延迟"的测量点（API 返回时刻 vs 状态落 RUNNING 时刻）未定义，无法设计验收。
5. FlowableDelegateAdapter 的节点执行不落库、租户丢失（REQ-19）是设计取舍（Flowable 自有历史表）还是缺陷？若为取舍，spec 应声明审计边界。

---

## 反驳复核

**复核员**: 独立复核（非原分析作者）· **复核日期**: 2026-08-29 · **方法**: 对全部 13 条（severity ∈ {Critical, High} 且 verdict ∈ {partial, absent}；本报告 contradicted=0）逐条重读被引用代码行、全仓库交叉检索遗漏执行点（含 schemaplexai-web / schemaplexai-task / 实体基类 / BPMN / DDL）、并重读规格原文核对引用。

**规格 §3.5 vs §7 内部矛盾核实**: 矛盾**属实** —— §1、§2 架构图、§3.5、§5.3 均把 HUMAN_APPROVAL 定义为 WorkflowNodeEngine 节点类型（写 `sf_approval`、置 `WAITING_APPROVAL`），而 §7 边界表声明"人工审批流程 | Flowable=是 | NodeEngine=否"。**但该矛盾不构成改判理由**：无论采信哪一侧，规格其余强制物在代码中均不存在——§4 的 `WAITING_APPROVAL` 实例状态、§6.2 的 `/{id}/approve|reject` 回调端点、`sf_approval` 表（全仓库仅存语义不同的 `sf_approval_ticket`）零实现；且现存 Flowable 审批流程（BpmnApprovalDeployer）与 `sf_workflow_instance` 无任何关联，AI 节点工作流实例无法进入审批暂停。故 REQ-09 **不改判为纯文档问题**，维持 partial（原分析已在 Open questions #3 记录该矛盾，处置方向正确：spec 修订与代码补齐须并行）。

| REQ | 原判 | 裁定 | 理由（一行） | 证据 |
|-----|------|------|--------------|------|
| REQ-02 | partial·High | 维持 | 全仓库执行器注册类型恰为 9 种，无 AGENT；agentId 参数无人消费，无"创建执行"语义 | `node/*.java` getNodeType 全集（AI_MODEL: AIModelNodeExecutor.java:26）；:31-37 仅消费 prompt/modelId；`createExecution` 全模块 0 命中 |
| REQ-03 | absent·High | 维持 | 替换语法与"上游输出传递"机制均不存在，编排循环丢弃每个节点 output | `substitut`/`${input` 全模块 0 命中；WorkflowInstanceServiceImpl.java:70-90 input 恒等于 config.getInput()（L77），result.getOutput() 无下游消费者 |
| REQ-04 | absent·High | 维持 | waitForCompletion 全仓库 0 命中；RestTemplate 无参构造无超时，单次同步调用无轮询 | AIModelNodeExecutor.java:19,70；`waitForCompletion` grep 全仓库 java = 0 |
| REQ-05 | partial·High | 维持 | 异常路径的 FAILED 写入随 @Transactional（REQUIRED 同事务）回滚不落库；TIMEOUT 全模块 0 命中；桥接路径 updateById(id=null) 更新 0 行 | WorkflowNodeEngine.java:38,47,55,61-64 + WorkflowInstanceServiceImpl.java:38,81-83；"TIMEOUT" grep 0 命中 |
| REQ-07 | absent·High | 维持 | SCRIPT 为永败存根，language 参数未被读取，无任何脚本引擎依赖 | ScriptNodeExecutor.java:18-22 固定 failure；pom.xml 依赖清单无 groovy/nashorn/graal/ScriptEngine |
| REQ-09 | partial·High | 维持 | §3.5/§7 矛盾属实但非豁免理由：两种解读下 WAITING_APPROVAL/sf_approval/approve-reject 均缺失，Flowable 审批未与实例表打通（见上段） | `HUMAN_APPROVAL`/`WAITING_APPROVAL` 代码 0 命中；`sf_approval` 仅存 `sf_approval_ticket`（agent-engine V2026_05_09…sql:51、quality V2026_05_10_1…sql:5，票据语义）；BpmnApprovalDeployer.java 不触碰 sf_workflow_instance |
| REQ-11 | partial·High | 维持 | CANCELLED/WAITING_APPROVAL 无任何代码写入者；cancelledEndEvent 仅 BPMN 终点命名，不写实例表 | "CANCELLED"/"WAITING_APPROVAL" 于 workflow/src/main 0 命中；ai-agent-execution.bpmn20.xml:90；WorkflowInstanceServiceImpl.java 仅写 RUNNING/FAILED/COMPLETED（:66,:57/:85,:92） |
| REQ-12 | absent·Critical | 维持 | 触发路径仅校验模板存在，`template.getStatus()` 从未被读取；创建实例直接 save() 无门禁——复查未发现任何其他入口存在状态门禁 | WorkflowInstanceServiceImpl.java:45-48（无 getStatus 调用）；WorkflowInstanceController.java:23-27,61-65；WorkflowTemplateServiceImpl.java:34-36 状态机仅自用 |
| REQ-14 | partial·High | 维持 | input_data/output_data 在 DDL 无列；started_at/completed_at 有列但实体与 BaseEntity 均未映射，代码从不写入，两列恒 NULL | 03-init-schema-others.sql:21-33（无 input/output 列）；SfWorkflowInstance.java:13-17 无对应字段；BaseEntity.java:19-38 亦无；trigger() 无时间写入 |
| REQ-17 | partial·High | 维持 | 控制器无 cancel/approve/reject 映射；web 模块最近似端点作用于审批票据与 Agent 执行（资源/路径/语义均不同）；创建不触发，原子语义被拆分 | WorkflowInstanceController.java 全文无对应映射（仅 :61-65 trigger）；ApprovalWebController.java:40-48、ExecutionWebController.java:62 |
| REQ-19 | partial·Critical | 维持 | AI_AGENT 不在注册表，BPMN AI 执行必炸；**复核新证据加重**：无任何 BPMN 或 Java 代码引用 FlowableDelegateAdapter（死桥接），且无 BPMN 设置 nodeType 变量→缺省恒落 SCRIPT 永败存根；桥接节点从不 insert 只 updateById→记录恒不落库 | getNodeType 全集无 AI_AGENT；AiAgentExecutionDelegate.java:68,73,106-107；FlowableDelegateAdapter.java:31-33；三个 BPMN `nodeType` grep = 0 命中；adapter 仅被 FlowableDelegateAdapterTest.java:19,28（测试）引用；WorkflowNodeEngine.java:47,55,63 仅 updateById |
| REQ-20 | partial·High | 维持 | HTTP 超时硬编码（默认值巧合符合）不可按节点配置；AI_MODEL 完全无超时且处于事务内可无限阻塞 | HttpNodeExecutor.java:21-22（5s/30s 硬编码，节点配置 timeoutSeconds 无读取）；AIModelNodeExecutor.java:19；WorkflowNodeEngine.java:38 事务包裹 |
| REQ-21 | absent·High | 维持 | 无自动重试循环与退避；MQ 消费侧显式 nack(requeue=false) 放弃重投；BPMN retryCount 为人工反馈后重跑（需人介入、无上限、无退避），语义不等价 | WorkflowTriggerConsumer.java:59,63；AiAgentExecutionDelegate.java:44-47,91-95 + ai-agent-execution.bpmn20.xml:122-127；pom.xml 无 spring-retry/resilience4j |

**复核结论**: 复核 13 条，推翻 0 条，维持 13 条。原分析的行号引用经逐一重读全部属实，未发现被遗漏的执行点或对规格的误读；独立检索额外发现两条加重证据（FlowableDelegateAdapter 为无任何流程引用的死代码；spec-review-approval.bpmn20.xml 确无任何定时器，与原判一致）。§3.5/§7 规格内部矛盾属实，处置如上：不构成改判，须以 spec 修订解决。
