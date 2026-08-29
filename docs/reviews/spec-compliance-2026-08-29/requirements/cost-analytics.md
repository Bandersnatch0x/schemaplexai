# Spec 合规核查 — cost-analytics

- **权威 spec**: `docs/specs/2026-04-30-v1.0-cost-analytics.md`（任务声明 status=已批准；**注意：spec frontmatter 实际标注 `status: 草稿`**，按任务口径作为权威基准）
- **核查日期**: 2026-08-29（本次为独立复核：所有行号与公式均经逐文件通读重新验证）
- **核查范围**: `schemaplexai-ops`（主）、`schemaplexai-agent-engine`（采集侧）、`schemaplexai-task`（MQ/定时）、`schemaplexai-web`（查询 API）、`schemaplexai-model`、`schemaplexai-dao`、`schemaplexai-common`（MQ 常量）、`docker/postgres/init`、`docker/clickhouse/init`、各 `application.yml`、模块 `pom.xml`
- **方法**: 逐条提取规范性需求 → 文本搜索定位 → 通读实际代码（含调用方与被调用方）→ 走全路径 → 裁决；计费公式逐行核算
- **总裁决**: implemented 2 · partial 9 · contradicted 3 · absent 2 · undecidable 1（共 17 条）

**首要结论（管线断流）**: 整条成本管线的**采集源头不存在**——全仓枚举全部 8 处 `convertAndSend` 调用（见 REQ-01 Searched），没有任何生产代码发布 `TOKEN_USED` 事件、`CostRecordedEvent` 或向 `sf.cost` / `cost.*` 路由键发消息；成本相关消费链路（3 条）与存储端（PG/CH 表）齐备但全部空转。预算 `used_amount` 因此在生产路径上永不增长，告警与报表均基于恒为空的数据。

**架构注记**: `schemaplexai-task`、`schemaplexai-web`、`schemaplexai-agent-engine` 的 pom.xml 均直接依赖 `schemaplexai-ops`（task/pom.xml:12-13、web/pom.xml:18-19、agent-engine/pom.xml:12），task/web 通过内嵌 ops 的 Bean（`CostService`、`CostDataSyncService`、`BudgetGuard`）复用成本逻辑，而非跨服务调用。

---

## REQ-01 — Token 采集点：TokenUsageRecorder 记录每次 LLM 调用

> "TokenUsageRecorder (AOP / 拦截器)" … "**Token 成本采集**: 记录每次 LLM 调用的输入/输出 Token 数及成本" — 2026-04-30-v1.0-cost-analytics.md §1、§2 数据流

**Verdict:** absent · confidence: high

**What this demands:** Agent 执行中每次 LLM 调用后，存在一个采集组件（AOP/拦截器）把真实 input/output token 数与成本写入持久化管线（直接落库或发事件）。

**Where enforcement lives:** 无。最接近的位置：
- `schemaplexai-agent-engine/.../model/LlmProviderAdapter.java:81-85` — `Response<AiMessage> response = model.generate(chatMessages)`（:81）后仅 `LlmMessageConverter.extractText(response)`（:82）取文本；LangChain4j 返回的 `tokenUsage` **被丢弃**，未记录、未发事件。
- `schemaplexai-agent-engine/.../state/ThinkingStateHandler.java:163、196、464-465` — 调用前后用 `estimateTokens`（委托 `TokenEstimator.java:13-17`，`text.length()/4` 启发式）估算 token，但只用于 `TokenBudget` 门禁，不落成本库、不发成本事件。

**Paths walked:**
- ✗ AOP/拦截器路径：全仓无 `TokenUsageRecorder`/任何 LLM 计费切面。
- ✗ 事件路径 A（`sf.exchange`/`sf.agent.exec.event` → task `ExecutionEventConsumer.java:34,42-43` → ops `CostService.processExecutionEvent`）：消费链路存在，但唯一的发布者 `AgentExecutionEventPublisher.publishExecutionEvent`（AgentExecutionEventPublisher.java:22-28）只被 `GateBlockedStateHandler.java:56,70` 以 `"AGENT_GATE_BLOCKED"` 调用，永远走不进 `CostService.java:176`（`case "TOKEN_USED"`）。
- ✗ 事件路径 B（exchange `execution_events`/key `cost.*` → ops `CostEventConsumer.java:35-41`）：Outbox 发布器 `OutboxPublisher.java:38,67` 只发 outbox 表中的 topic；写 outbox 的仅 `ApprovalRequestProducer.java:78`（topic `approval.requests` / `approval.requests.deferred`，:34,37）与 `AgentExecutionLifecycleService.java:128`（`"execution.events"`），无 `cost.*` topic 写入者。
- ✗ 直接落库路径：engine 通过 `CrossModuleMapperConfig.java:32-33` 注册了 `SfCostRecordMapper`，但 engine 主代码无任何 `costRecordMapper` 使用点（仅该配置类的 import/注册）。

**Searched:**
- `TokenUsageRecorder`（全仓）→ 仅 spec/docs 命中，src 0 处
- `CostRecordedEvent|TOKEN_USED|publishCostRecorded`（glob `**/src/main/**`）→ 5 处：`CostService.java:176,216`（消费端）、`CostEventConsumer.java:5,47`（消费端）、`schemaplexai-model/.../event/CostRecordedEvent.java:11`（record 定义）；无生产者
- `tokenUsage|TokenUsage|estimateTokens`（agent-engine src/main）→ 9 个文件，全部为记忆/推理/实验统计/预算门禁用途，无 LLM 响应 `response.tokenUsage()` 读取
- `convertAndSend`（全部模块 src/main）→ 8 处全枚举：GapRecoveryJob.java:103（gap alert）、AgentExecutionEventPublisher.java:26,37（exec.event/config.shadow）、OutboxPublisher.java:67（outbox topic）、DeadLetterHandler.java:100（audit）、DeadLetterRetryService.java:59（失败重投）、MilvusReconciliationService.java:61、ApprovalTicketService.java:300（审批决定）——均与成本无关
- `setTopic|TOPIC_`（engine src/main）→ topic 集合为 approval.requests(.deferred)、execution.events，无 cost.*

**How the verdict was reached:** 不是 partial——partial 要求至少一条采集路径部分工作；这里三条候选路径的**源头全部为零命中**，消费端存在不构成采集。不是 undecidable——发布点可枚举且已全部枚举。生产者契约测试 `schemaplexai-agent-engine/src/test/resources/contracts/costRecordedEvent.groovy:15`（`sentTo('sf.cost')`，triggeredBy `publishCostRecorded()`）声明了意图，但全仓测试代码中不存在实现 `publishCostRecorded()` 的基类（grep 0 命中），契约无法运行——佐证这是已知未完成项而非隐藏实现。

**Severity: Critical** — 每一次 LLM 调用的成本都未被记录，后果按调用次数累积：成本报表恒空、预算 `used_amount` 恒零、告警永不触发，成本失控不可见。

---

## REQ-02 — PG 表 sf_token_usage 数据模型

> "**sf_token_usage** (PostgreSQL): id BIGINT 主键 / tenant_id / agent_id / execution_id / model VARCHAR / provider VARCHAR / input_tokens / output_tokens / cost DECIMAL(18,6) / created_at" — §3.1

**Verdict:** implemented（机制漂移）· confidence: medium

**What this demands:** PG 中存在一张按次记录 token 用量与成本的表，含租户/Agent/执行/模型/供应商/输入输出 token/成本（≥6 位小数）/时间字段。

**Where enforcement lives:** 表名不同、语义等价：`sf_cost_record`
- DDL：`schemaplexai-agent-engine/src/main/resources/db/migration/V2026_05_09_02__add_audit_cost_projection_tables.sql:18-38` — `tenant_id/record_id/service_name/model_name/provider/request_type/input_tokens/output_tokens/total_tokens/cost_amount NUMERIC(18,8)（:29）/currency/occurred_at/execution_id/agent_id/workflow_instance_id` 全部覆盖 spec 字段（cost→cost_amount，精度 8>6，且含 provider 字段 ✓）。
- 实体：`schemaplexai-ops/.../entity/SfCostRecord.java:18-34`（`@TableName("sf_cost_record")`）。

**Paths walked:**
- ✓ 写路径 1：`CostEventConsumer.java:55-73`（ops MQ 消费 → insert，:73）。
- ✓ 写路径 2：`CostService.processTokenUsedEvent`（CostService.java:194-207，insert :207）与 `processToolCallEvent`（:223-232）。
- ✓ 读路径：`BudgetGuard.getConsumedBudget`（BudgetGuard.java:100-110）、`CostService.queryCostByExecution`（CostService.java:68-71）。
- ✗ Docker 初始化路径：`docker/postgres/init/*.sql` 无 `sf_cost_record`（grep 仅 CH DDL 命中）——该表仅由 agent-engine 的 Flyway 迁移创建。纯 docker-compose 初始化 + 仅启动 ops 服务（不启动 engine 跑 Flyway）的环境中，上述写入会因表不存在而失败。

**Searched:**（证明 spec 原名不存在）`sf_token_usage|token_usage`（全仓）→ 仅 docs/specs/plans 命中，代码与 SQL 0 处。

**How the verdict was reached:** 代码以 `sf_cost_record` 不同机制满足了同一数据模型需求（字段超集、精度更高）→ implemented + 文档漂移，而非 absent。confidence 降为 medium 的原因：建表责任落在 agent-engine 的 Flyway 迁移而非共享初始化脚本，存在部署顺序耦合（Open question 1）；且当前无任何写路径实际产生数据（REQ-01）。

**Severity: Low（文档漂移）+ Medium（部署耦合，备注性）**

---

## REQ-03 — 成本计算公式与价格来源

> "cost = (inputTokens * inputPricePer1K / 1000) + (outputTokens * outputPricePer1K / 1000)" — §3.1；价格来自 "模型价格配置 (sf_ai_model 表扩展)"

**Verdict:** partial · confidence: high

**What this demands:** 成本 = 输入分量 + 输出分量，单价按模型从配置（sf_ai_model）读取，对每个被调用的模型均能算出非零成本。

**Where enforcement lives:** `schemaplexai-ops/.../service/CostService.java:139-163`（`calculateCost`）
- ✓ 公式结构逐行核对：`:157-158` `inputRate.multiply(BigDecimal.valueOf(inputTokens)).divide(TOKEN_SCALE, COST_SCALE, RoundingMode.HALF_UP)`（TOKEN_SCALE=1000，:34）；`:159-160` 输出分量同构；`:162` 两分量相加。乘除顺序与 spec 一致（先乘后除，无先除导致的精度损失）。
- ✗ 价格来源：`CostService.java:29-32` 硬编码 4 个常量（gpt-4: 0.03/0.06；gpt-3.5: 0.0015/0.002），完全不读 `sf_ai_model`（该表亦无价格列，见 REQ-04）。
- ✗ 模型覆盖：`CostService.java:147-154` — 仅 `contains("gpt-4")` / `contains("gpt-3.5")` 分支计价，其余模型直接 `return BigDecimal.ZERO`（:154）。engine 的 Anthropic 适配器默认模型为 `claude-3-sonnet-20240229`（AnthropicProvider.java:29，另有 :109 的 claude-3-haiku），即 **claude 系调用成本恒记 0**；运行时仅在 `CostService.java:190-192` log.warn 一行。
- ✗ 舍入精度：分量在 scale=4（`COST_SCALE`，CostService.java:35）处 HALF_UP，违反 6 位小数要求（详见 REQ-15）。

**Paths walked:**
- ✓ `processTokenUsedEvent` → `calculateCost` → insert + `budgetService.addUsedAmount`（CostService.java:182-211）
- ✓ `processToolCallEvent` → 固定费 0.01（CostService.java:221-244，spec 未提及，见反向差距）
- ✓ 上游唯一调用方：task `ExecutionEventConsumer.java:43`（pom 依赖 ops，见架构注记）

**Searched:**（价格配置化路径）`price_per_1k|input_price|output_price|inputPrice|outputPrice`（全仓）→ 3 处，全在 spec 文档本身；`price -i`（schemaplexai-model、schemaplexai-system src/main）→ 0 处。

**How the verdict was reached:** 公式骨架与 spec 一致（不是 contradicted 的"公式错误"），但价格来源与"每次调用都可计价"两个必要性质不成立 → partial 而非 implemented。

**Severity: Critical** — 采集恢复后该缺陷立即生效：所有非 gpt-4/gpt-3.5 模型（含已接入的 Anthropic）的成本被系统性记为 0，且价格无法随市场调价更新；偏差按每次调用累积，直接击穿预算与成本报表的可信性。

---

## REQ-04 — 模型价格配置（sf_ai_model 扩展字段）

> "模型价格配置 (sf_ai_model 表扩展): input_price_per_1k / output_price_per_1k / currency(默认 USD)" — §3.1

**Verdict:** absent · confidence: high

**What this demands:** `sf_ai_model` 具备两列单价与货币列，供计费读取。

**Where enforcement lives:** 无。
- DDL：`docker/postgres/init/01-init-schema.sql:98-109` — `sf_ai_model` 仅有 `name/provider_id/model_code/status/config_json`，无任何价格列。
- 实体：`schemaplexai-system/.../entity/SfAiModel.java:12-30` — 字段仅 `tenantId/name/providerId/modelCode/status`，无价格字段。

**Paths walked:**
- ✗ 计费读取路径：`CostService.calculateCost`（CostService.java:139-163）不注入任何 model mapper/service，硬编码价格。
- ✗ 迁移路径：`schemaplexai-*/src/main/resources/db/migration/` 全部 10 个迁移文件中无对 `sf_ai_model` 的 ALTER。

**Searched:**
- `price_per_1k|input_price|output_price|inputPrice|outputPrice`（全仓）→ 3 处，全在 spec 文档本身
- `price`（schemaplexai-model、schemaplexai-system src/main，-i）→ 0 处
- `sf_ai_model`（docker/postgres/init）→ 建表 :98-109，无价格列

**How the verdict was reached:** 列、实体字段、读取方三者全无 → absent。不是 partial：`config_json` 理论上可承载价格，但无任何代码按此解释它，不能算部分实现。

**Severity: High** — 与 REQ-03 联动：价格不可配置意味着新模型接入即计 0 成本，历史价格变更不可追溯。

---

## REQ-05 — ClickHouse 增量游标同步流程

> "ClickHouseCostSyncService: 1. 读取游标 (sf_sync_cursor) 2. 查询 PG: SELECT * FROM sf_token_usage WHERE created_at > cursor 3. 批量插入 ClickHouse 4. 更新游标 5. 记录批次日志 (sf_sync_batch_log)" — §3.2

**Verdict:** contradicted · confidence: high

**What this demands:** 把 PG 中的 **token 用量/成本数据** 按游标增量搬运到 ClickHouse 的成本表，并留痕。

**Where enforcement lives:** `schemaplexai-ops/.../service/ClickHouseCostSyncService.java:33-287` — 五步骨架齐全，但**搬运的根本不是成本数据**：
- `:36` `CURSOR_KEY = "sf_agent_execution"`；`:135-142` 源查询是 `SELECT … FROM sf_agent_execution WHERE id > ?`（执行状态元数据，无 token、无 cost 字段）；`:173-178` 目标是 CH 表 `sf_agent_execution` 的 INSERT。
- 目标表不存在：`docker/clickhouse/init/01-cost-analytics.sql` 与 `02-agent-timeline.sql` 均未建 `sf_agent_execution`；且 `01-cost-analytics.sql:6` 建的库是 `schemaplexai_costs`，而 ops `application.yml:40` CH database 默认 `schemaplexai` → 库名+表名双重错配，启用后必然报错。
- 游标/批次表结构错配（启用后第一步就挂）：实体 `SfSyncCursor.java:14-18` 映射列 `sync_table/last_sync_id/last_sync_time` 且继承 BaseEntity（隐含 `tenant_id/created_by/updated_by/deleted` 字段），但 DDL `docker/postgres/init/03-init-schema-others.sql:346-359` 的列是 `sync_name/source_table/target_table/last_sync_id/last_sync_time/sync_batch_size/sync_interval_sec/failed_count/last_error` 且**无 tenant_id**；`TenantLineInterceptor.java:28-33` 的忽略清单仅 `sf_tenant`、`sf_tenant_environment_config`、`act_*`，租户插件还会对这两张表强行追加 `tenant_id = ?`。`SfSyncBatchLog.java:14-24`（syncTable/batchSize/successCount/failCount/startTime/endTime）与 DDL :361-372（sync_name/batch_id/start_id/end_id/record_count/started_at/completed_at）同样互不匹配。`:123` `selectById(CURSOR_KEY)` 还把字符串 `"sf_agent_execution"` 当 BIGSERIAL 主键查询。
- 游标语义漂移（可接受项）：按 `id > ?` 而非 spec 的 `created_at > cursor`——若数据对了，这属于更稳的机制漂移，不单独扣分。

**Paths walked:**
- ✓ 定时触发：`:57` `@Scheduled(fixedDelay = 300_000)`
- ✓ MQ 触发：task `CostSyncConsumer.java:45,69` → `syncIncrementalData()`；task `CostStatisticsJob.java:26` 亦调用
- ✓ 失败路径：部分失败不推进游标并抛 `SYNC_CURSOR_ERROR`（:83-91），批次日志置 FAILED（:250-255）——这部分设计良好
- ✗ 默认关闭：ops `application.yml:37` `clickhouse.enabled: false`；`DisabledCostDataSyncService.java:9`（`havingValue="false", matchIfMissing=true`）为默认注册的 no-op 实现；task 模块 yml 无 clickhouse 配置

**Searched:**（证明无成本数据同步路径）`schemaplexai_costs|cost_records`（src/main + docker）→ 仅 CH DDL 与 `CostRecord.java:9` javadoc；全部 Java 代码中无任何向 `schemaplexai_costs.*` 的写入语句。

**How the verdict was reached:** 不是 partial——partial 意味着"同步了成本数据但流程有缺口"；这里同步对象（执行元数据）与 spec 要求（成本数据）**语义相反**，且启用后因三处 schema 错配无法运行。五步流程骨架的存在不改变"成本永远到不了 ClickHouse"这一后果。

**Severity: High** — CH 侧成本表与物化视图（REQ-06/07）永远为空；所有 OLAP 成本分析能力被架空（叠加 REQ-01 后为双重断流）。

---

## REQ-06 — ClickHouse cost_records 表结构

> "CREATE TABLE cost_records ( … cost Decimal(18, 6), created_at DateTime ) ENGINE = MergeTree() ORDER BY (tenant_id, created_at) PARTITION BY toYYYYMM(created_at);" — §3.2

**Verdict:** implemented（文档漂移）· confidence: high

**What this demands:** CH 中存在按次成本明细表，MergeTree，租户+时间有序，按月分区，成本精度 ≥6 位小数。

**Where enforcement lives:** `docker/clickhouse/init/01-cost-analytics.sql:13-49` — `schemaplexai_costs.sf_cost_record`
- 字段：spec 全字段覆盖 + 超集（record_id/service_name/total_tokens/currency/workflow_instance_id）
- `:31` `cost_amount Decimal128(12)`（12 位小数 ≥ 6 ✓）
- `:44` `ENGINE = MergeTree()` ✓；`:46` `ORDER BY (tenant_id, service_name, created_at)`（比 spec 多 service_name 维）；`:45` `PARTITION BY toYYYYMMDD(created_at)`（按日而非按月，2 年数据 ≈730 分区，可用但偏碎）
- `:47` `TTL created_at + INTERVAL 1 YEAR` — **与 REQ-16 的 2 年保留冲突，裁决记在 REQ-16**

**Paths walked:**
- ✓ DDL 静态核对（逐列）
- ✗ 写入路径：无代码写此表（见 REQ-05 Searched）——表是空壳，但"表结构存在且合规"本条仍成立
- ✗ 部署路径（备注）：`docker/docker-compose.yml:121-136` 的 clickhouse 服务**未挂载** `docker/clickhouse/init/` 目录（仅数据卷 :128），该 DDL 在 compose 环境不会自动执行

**How the verdict was reached:** 表名/库名/排序键/分区粒度与 spec 字面不同，但每一处都保持或增强了 spec 的性质（精度、维度、可查性）→ 不同机制满足 = implemented + 文档漂移；不是 stronger-than-spec，因为按日分区与额外排序键属中性权衡而非严格增强。

**Severity: Low（修文档）**

---

## REQ-07 — 物化视图 mv_daily_cost（日聚合）

> "CREATE MATERIALIZED VIEW mv_daily_cost ENGINE = SummingMergeTree() ORDER BY (tenant_id, agent_id, model, date) AS SELECT … sum(input_tokens), sum(output_tokens), sum(cost), count() as call_count FROM cost_records GROUP BY tenant_id, agent_id, model, date;" — §3.2

**Verdict:** partial · confidence: high

**What this demands:** 存在日粒度、(租户, Agent, 模型)三维的成本聚合视图，含调用次数。

**Where enforcement lives:** `docker/clickhouse/init/01-cost-analytics.sql`
- `:124-138` `mv_token_consumption_daily` → `sf_token_consumption_daily`（:104-121）：日粒度 ✓、有 `sum(cost)`/token 聚合 ✓，但 GROUP BY 仅 `(tenant_id, date)`（:136-138）— **缺 agent_id 与 model 维度，且无 call_count**
- `:79-98` `mv_model_usage_hourly` → `sf_model_usage_hourly`（:56-76）：有 model/provider 维与 `count() AS total_requests`（:87），但小时粒度、**缺 agent_id**
- `:145-170` `sf_agent_execution_cost`：有 agent 维，但是裸表，无任何 MV 填充（`TO schemaplexai_costs.sf_agent_execution_cost` 全仓 0 命中）

**Paths walked:**
- ✓ 两个 MV 的 SELECT 逐列核对
- ✗ (tenant, agent, model, date) 组合：任何单个对象都不提供；两 MV 拼接也不可行（小时表无 agent，日表无 agent/model）

**Searched:** `mv_daily_cost`（全仓）→ 仅 spec；`agent_id`（01-cost-analytics.sql）→ 仅 sf_cost_record 明细列（:37）与 sf_agent_execution_cost 裸表（:148）。

**How the verdict was reached:** 聚合机制存在且方向正确（SummingMergeTree + MV），但 spec 的关键分析维度（按 Agent 的日成本）无法从任何聚合对象得出，只能回退全表扫明细 → partial 而非 implemented（漂移）；也不是 absent，因为日聚合确实存在。

**Severity: Medium** — REQ-10 的 `byAgent`/`byModel` 日报只能靠明细表现算，P99<2s（REQ-17）在数据量大后难以达成；当前因断流暂无实际后果。

---

## REQ-08 — sf_budget 数据模型

> "sf_budget: id / tenant_id / type VARCHAR (MONTHLY / AGENT / MODEL) / target_id BIGINT(目标 ID，Agent ID 或 Model ID)/ limit_amount DECIMAL(18,6) / alert_threshold DECIMAL(3,2)(如 0.8 = 80%)" — §3.3

**Verdict:** partial · confidence: high

**What this demands:** 预算可按 月度/Agent/模型 三种类型定义，能指向具体目标对象，阈值以 0–1 小数表达。

**Where enforcement lives:**
- DDL：`docker/postgres/init/03-init-schema-others.sql:294-305` — `budget_type`（:297，注释枚举为 `MONTHLY / PROJECT`，**非 spec 的 MONTHLY/AGENT/MODEL**）；**无 `target_id` 列**；`limit_amount DECIMAL(18,4)`（:298，精度 4<6）；`alert_threshold DECIMAL(5,2) DEFAULT 80.00`（:300，**百分数语义**，非 spec 的 0.8 小数语义）；额外 `used_amount`（:299）与 `currency`（:301）。
- 实体：`schemaplexai-ops/.../entity/SfBudget.java:13-19` — `budgetType/limitAmount/usedAmount/alertThreshold`，同样**无 targetId**。

**Paths walked:**
- ✓ 写路径：`BudgetController.create`（BudgetController.java:23-28）；`allocateBudget`（BudgetServiceImpl.java:28-44）——`:38-40` 阈值缺省填 `0.8`（**小数语义**，与 DDL 缺省 80.00 百分数语义直接冲突，同一列两种单位并存）
- ✓ 读路径：`checkBudgetAlerts`（CostService.java:107-129）按**百分数**比较（:114-116 `ratio = used/limit×100`，:122-123 `ratio >= alertThreshold`）→ 经 `allocateBudget` 缺省创建的预算会在用量达 **0.8%**（而非 80%）时触发告警分支
- ✓ 消费路径：`addUsedAmount`（BudgetServiceImpl.java:106-122）只加到该租户**一条**预算（:115 `budgets.get(0)`；listBudgetsByTenant 按 createdAt 倒序 :83-86 → 实际是最新一条），多预算时归集对象不符合 spec 的 type/target 语义

**Searched:**（target_id）`target_id|targetId`（schemaplexai-ops src/main + docker/postgres/init sf_budget 段）→ 0 命中。

**How the verdict was reached:** 表和主要字段存在（不是 absent）；但 AGENT/MODEL 级预算在数据模型层不可表达（无 target_id），且阈值单位在 DDL 缺省/写入缺省/比较逻辑三处互相矛盾——这超出"文档漂移"，是实现内部的不一致 → partial。

**Severity: High** — 阈值单位冲突造成告警在错误的用量水平触发（0.8% vs 80%），缺 target_id 使 spec 的两类预算根本无法配置；均为持续性错误行为。

---

## REQ-09 — 预算告警检查（每小时定时，超阈值发通知）

> "@Scheduled(cron = \"0 0 * * * ?\") // 每小时 … 1. 查询各租户当前周期成本 2. 对比预算上限 3. 超过阈值 → 发送告警通知" 及 §5 "BudgetAlertJob | 每小时 | 预算告警检查" — §3.3、§5

**Verdict:** partial · confidence: high

**What this demands:** 每小时执行一次；比较当前周期实际成本与预算；越过阈值时**发出通知**（非仅日志）。

**Where enforcement lives:**
- 检查逻辑：`CostService.checkBudgetAlerts`（CostService.java:107-129）存在，但比较的是 `sf_budget.used_amount`（累计值，无"当前周期"概念）而非按周期聚合的成本；越限动作仅 `log.warn`（:119-126），**不调用 NotificationService、不发 MQ、不落告警记录**（`NotificationServiceImpl` 中 budget 相关 0 命中）。
- 调度：全仓唯一调用方是 `schemaplexai-task/.../scheduling/CostStatisticsJob.java:27` — cron `"0 0 1 * * ?"`（:21，**每日 01:00**，非每小时）；无独立 BudgetAlertJob。

**Paths walked:**
- ✓ CostStatisticsJob.run → checkBudgetAlerts（CostStatisticsJob.java:26-27）
- ✓ 告警两分支（≥100% 与 ≥threshold，CostService.java:118-127）——阈值单位缺陷见 REQ-08
- ✗ 通知路径：NotificationService 体系（含 `sf.notification` 路由，CommonConstants.java:39）无任何预算告警生产者/消息

**Searched:**
- `BudgetAlertJob`（全仓）→ 仅 docs
- `@Scheduled` 全量清单（全部模块 src/main）→ 13 处（含 fixedDelay 型），无一小时级 cron（`0 0 * * * ?` 模式 0 命中）
- `checkBudgetAlerts` 调用方（全部 src/main）→ 仅 CostStatisticsJob.java:27

**How the verdict was reached:** 检查骨架存在故非 absent；但频率（1/24）、周期语义、通知动作三个规范性性质均不满足 → partial。不是 contradicted：代码没有实现与 spec 相反的行为，只是弱化。

**Severity: High** — 预算超支最长延迟 24h 才被发现，且"发现"仅是服务日志一行 warn，无人接收；预算护栏形同虚设（叠加 REQ-01：当前连 used_amount 都不会增长）。

---

## REQ-10 — 成本查询 API（GET /ops/costs/summary）

> "GET /ops/costs/summary?tenantId={id}&startDate=2026-04-01&endDate=2026-04-30 → data: { totalCost, totalInputTokens, totalOutputTokens, callCount, byAgent, byModel, byDay }" — §4.1

**Verdict:** partial · confidence: high

**What this demands:** 支持日期区间过滤的租户成本汇总，含 token 总量、调用数与 Agent/模型/日 三个维度分解。

**Where enforcement lives:**
- ops：`CostController.java:17,24-28` — 仅 `GET /ops/costs?tenantId=`，返回 `{totalCost, todayCost, monthCost}`；**无 /summary 子路径、无日期参数**。数据源 `queryCostByTenant`（CostService.java:42-63）是 `sf_budget.used_amount` 之和（代理值，:47-54，代码自注 "PG short-path v1" :45-46），且 `todayCost`/`monthCost` 是 `totalCost` 的**镜像**（:60-61，代码自注 "v1 short-path" :58-59）。
- web：`CostWebController.java:19,27-31` — `GET /web/costs/summary`（X-Tenant-Id 头），经 `OpsCostQueryPort.getCostSummary`（OpsCostQueryPort.java:23-28）转发同一个 `queryCostByTenant`，VO 仅 `totalCost/todayCost/monthCost/currency`（CostSummaryVO.java:13-19）。
- 逐执行明细存在：`queryCostByExecution`（CostService.java:65-105）+ `CostWebController.java:34-39`（spec 未要求，见反向差距）。

**Paths walked:**
- ✓ /ops/costs → queryCostByTenant → budgetMapper（全程无日期、无维度）
- ✓ /web/costs/summary → OpsCostQueryPort → 同上
- ✗ byAgent/byModel/byDay：全仓无任何按维度聚合的查询实现；`startDate|endDate`（ops/web src/main）0 命中

**Searched:** `costs/summary`（src/main）→ 仅 CostWebController.java:27 一处（路径前缀 /web 非 /ops）；`startDate|endDate`（schemaplexai-ops、schemaplexai-web src/main）→ 0 命中；`totalInputTokens|callCount|byAgent|byModel|byDay`（src/main）→ 0 命中。

**How the verdict was reached:** "租户成本汇总 API 存在"这一核心以不同路径成立（部分实现 + 文档漂移），但契约的 7 个响应字段只兑现 1 个（totalCost），且该字段的数据源是预算代理而非成本记录 → partial；不是 contradicted，因为没有返回相反语义的字段（todayCost 镜像是弱化而非相反语义——但已注记其误导性）。

**Severity: High** — 前端/看板按 spec 契约开发会直接拿不到 6/7 的字段；todayCost==monthCost==totalCost 会在 UI 上呈现错误信息。

---

## REQ-11 — 预算管理 API（CRUD + alerts）

> "POST /ops/budgets(创建) GET /ops/budgets(列表) PUT /ops/budgets/{id}(更新) DELETE /ops/budgets/{id}(删除) GET /ops/budgets/alerts(告警记录)" — §4.2

**Verdict:** partial · confidence: high

**What this demands:** 5 个端点，其中告警记录可查询（隐含告警需持久化）。

**Where enforcement lives:** `schemaplexai-ops/.../controller/BudgetController.java:16`（`/ops/budgets`）
- ✓ POST（:23-28）、PUT /{id}（:30-35）、DELETE /{id}（:37-41）、GET 列表（:53-57）
- ✗ GET /ops/budgets/alerts：控制器无此路由；告警也从未持久化（checkBudgetAlerts 仅 log，CostService.java:119-126），无告警实体/表可查
- 超集端点（spec 未提及）：GET /{id}（:43-51）、POST /allocate（:59-63）、GET /{id}/check-limit（:65-69）、GET /{id}/usage（:71-75）、GET /by-tenant（:77-81）、POST /{id}/update-allocation（:83-87）

**Paths walked:** ✓ 全部 10 个现有端点 → BudgetServiceImpl 对应方法逐一读毕（BudgetServiceImpl.java:28-122）。

**Searched:**（alerts 端点与存储）
- `alerts|/alerts`（schemaplexai-ops src/main）→ 0 路由命中（唯一命中为方法名 checkBudgetAlerts）
- `budget_alert|alert_record|sf_alert`（docker + 全部 src/main）→ 0 命中

**How the verdict was reached:** 4/5 端点完整且行为正确 → 不是 absent；缺失的 alerts 端点连依赖的数据（告警记录）都不存在，不是简单补路由能解决 → partial，缺口明确。

**Severity: Medium** — 告警不可追溯、不可确认；运营无法审计"何时超了哪个预算"。

---

## REQ-12 — 定时任务 CostSyncJob（每 5 分钟 PG→CH 增量同步；NFR 同步延迟 <5 分钟）

> "CostSyncJob | 每 5 分钟 | PG → ClickHouse 增量同步" — §5；"同步延迟 | < 5 分钟" — §6

**Verdict:** partial · confidence: high

**What this demands:** 存在 5 分钟节奏的增量同步调度，使 CH 数据滞后 <5 分钟。

**Where enforcement lives:** 无独立 `CostSyncJob` 类；调度内联在 `ClickHouseCostSyncService.java:57` `@Scheduled(fixedDelay = 300_000)` — 节奏 ✓（名称属文档漂移，可接受）。另有 MQ 触发路径（task `CostSyncConsumer.java:45,69`）与日任务路径（CostStatisticsJob.java:26）作补充。

**Paths walked:**
- ✓ 调度注解与方法体（见 REQ-05）
- ✗ 生效路径：ops `application.yml:37` `clickhouse.enabled: false` → `@ConditionalOnProperty`（ClickHouseCostSyncService.java:32）使该 bean 默认不注册；默认注册的是 no-op `DisabledCostDataSyncService.java:9-15`（`havingValue="false", matchIfMissing=true`）。仓库内无任何 profile/环境将其置 true。
- ✗ 同步内容正确性：见 REQ-05（contradicted）

**Searched:** `CostSyncJob`（全仓）→ 仅 docs；`clickhouse.enabled`（全部 yml）→ ops application.yml:37 唯一，值 false。

**How the verdict was reached:** 调度机制与节奏正确存在 → 非 absent；默认全关 + 同步内容错误使 NFR"延迟<5 分钟"对成本数据不可能成立 → partial（内容错误的后果已计入 REQ-05，此处不重复计 High）。

**Severity: Medium** — 即使修复 REQ-05，仍需显式开启配置才有任何同步。

---

## REQ-13 — 定时任务 CostStatisticsJob（每日 01:00 日成本聚合）

> "CostStatisticsJob | 每日 01:00 | 日成本聚合" — §5

**Verdict:** partial · confidence: high

**What this demands:** 每日 01:00 产出日粒度成本聚合数据。

**Where enforcement lives:** `schemaplexai-task/.../scheduling/CostStatisticsJob.java:16-37`
- ✓ 名称与调度：`:21` cron `"0 0 1 * * ?"` = 每日 01:00；`:22` ShedLock 防多实例并发（spec 未要求，增强）
- ✗ 职责：方法体（:26-27）只做 `syncIncrementalData()` + `checkBudgetAlerts()`，**不做任何聚合**。聚合职责实际由 CH 物化视图承担（01-cost-analytics.sql:79-98,124-138，插入时自动聚合，机制漂移本可接受）——但因 REQ-05 同步内容错误 + REQ-01 断流，MV 的输入永远为空，聚合链路整体空转。

**Paths walked:** ✓ run() 全路径（含异常重抛，:29-35）；✓ 两个 MV 的定义（见 REQ-07）。

**How the verdict was reached:** 调度存在、聚合以不同机制存在但被上游截断；若仅看本条（假设上游有数据），MV 机制可判 implemented（漂移），但 job 本体名不副实且 MV 缺 REQ-07 维度 → partial。

**Severity: Medium（依附于 REQ-01/05 修复）**

---

## REQ-14 — 数据流：MQ 触发的增量同步（sf.cost）

> "│ 增量同步 (定时任务 / MQ) ▼ ClickHouse" — §2 数据流（结合项目约定 exchange `sf.exchange`、routing key `sf.cost`）

**Verdict:** partial · confidence: high

**What this demands:** 存在经 MQ 触发同步的完整通路：生产者 → sf.exchange/sf.cost → 队列 → 消费执行同步。

**Where enforcement lives:**
- ✓ 常量：`schemaplexai-common/.../CommonConstants.java:40` `RK_COST = "sf.cost"`
- ✓ 队列与绑定：task `RabbitMqConfig.java:83-96` — `sf.cost.queue`（durable，带 DLX）绑定 `sf.exchange`/`RK_COST`
- ✓ 消费者：task `CostSyncConsumer.java:45-87` — 幂等去重（`InboxDeduplicationService`，:59-63）、手动 ACK（:76）、失败落 fail-log 并 nack 不重投（:78-86）、拒绝 full/date-range 请求（:115-129）
- ✗ 生产者：全仓无任何向 `sf.cost` 发布的代码（8 处 `convertAndSend` 全枚举，见 REQ-01 Searched；`CostSyncMessage` 无任何构造+发送方）

**Paths walked:** ✓ onMessage 全路径（去重命中/业务异常/解析异常三分支）；✓ DLX 配置（RabbitMqConfig.java:86-87）。

**Searched:** `RK_COST`（src/main）→ 2 处：常量定义（CommonConstants.java:40）、绑定（RabbitMqConfig.java:95）；`"sf.cost"` 字面量（src/main）→ 仅常量定义；`CostSyncMessage`（src/main）→ 仅 task 内 dto 与 consumer。

**How the verdict was reached:** 通路四环节存在三环，生产端零命中 → partial（消费基础设施完好，不是 absent）。

**Severity: Medium** — MQ 触发路径永不发生；同步仅剩默认关闭的定时路径，叠加后 CH 侧无任何触发来源。

---

## REQ-15 — 非功能：成本计算精度 6 位小数

> "成本计算精度 | 6 位小数" — §6

**Verdict:** contradicted · confidence: high

**What this demands:** 计算过程与存储均保留 ≥6 位小数，舍入不引入系统性偏差。

**Where enforcement lives:**
- 计算：`CostService.java:35` `COST_SCALE = 4`；`:157-160` 两个分量各自 `divide(TOKEN_SCALE, 4, RoundingMode.HALF_UP)` — **在 4 位小数处舍入，精度低于 spec 两个数量级**。逐行核算示例：gpt-3.5 输入 33 tokens → 0.0015×33/1000 = 0.0000495 → scale4 HALF_UP → **0.0000**（整笔分量归零）；100 tokens → 0.00015 → **0.0002**（+33%）。分量各自舍入使误差叠加，按调用次数线性累积。
- 存储：PG `NUMERIC(18,8)`（V2026_05_09_02:29，达标）、CH `Decimal128(12)`（01-cost-analytics.sql:31，达标）、但 `sf_budget.limit_amount/used_amount DECIMAL(18,4)`（03-init-schema-others.sql:298-299，**4 位**，预算累计端同样截断）。

**Paths walked:** ✓ calculateCost 唯一计费路径；✓ addUsedAmount 累计路径（BudgetServiceImpl.java:106-122，BigDecimal.add 本身无损，受列精度 18,4 截断）。

**Searched:** `setScale(6|, 6,`（全部 src/main 成本相关代码）→ 0 处命中。

**How the verdict was reached:** 这不是"精度略低"的 partial：4 位小数对 per-1K 价格 0.0015 量级的模型意味着小额调用被系统性归零或放大，方向性偏差每次执行都在累积 → contradicted。

**Severity: High** — 计费公式类错误，累积后果：长期低估（小额归零）与单笔高估（HALF_UP 上跳）并存，月度账单与真实用量出现不可审计的偏差。

---

## REQ-16 — 非功能：数据保留（ClickHouse 2 年，PG 90 天）

> "数据保留 | ClickHouse 保留 2 年，PG 保留 90 天" — §6

**Verdict:** contradicted · confidence: high

**What this demands:** CH 成本数据可支撑 2 年回溯；PG 侧 90 天后清理/归档。

**Where enforcement lives:**
- CH：`01-cost-analytics.sql:47` `sf_cost_record` **TTL 1 YEAR**（明细表，2 年回溯的根基，少 1 年）；`:74` 小时聚合 TTL 1 YEAR；`:119` 日聚合 TTL 2 YEAR（唯一达标，但维度残缺见 REQ-07）；`:168` 执行成本表 TTL 1 YEAR。
- PG：无任何针对 `sf_cost_record` 的 90 天清理/归档任务。

**Paths walked:**
- ✓ 四张 CH 表 TTL 逐一核对
- ✗ PG 清理路径：全部 13 处 `@Scheduled` 枚举（见 REQ-09 Searched），唯一归档类任务 `ChatMessageArchiveJob.java:23`（cron `0 30 1 * * ?`，归档对象是聊天消息，非成本）；ops 无清理任务

**Searched:** `-i "90|retention|archiv"`（全部 src/main java）→ 命中均为聊天归档/工作区等无关项，成本相关 0 处；`TTL`（docker/clickhouse）→ 上列 4 处。

**How the verdict was reached:** CH 明细 TTL 与 spec 数值**方向性不符**（1 年 < 2 年，数据在第 366 天被 ClickHouse 物理删除，不可恢复）→ contradicted 而非 partial；PG 侧缺失是 absent 性缺口，并入本条。

**Severity: Medium** — 当前因断流无数据可删；一旦管线修通，第 2 年的合规/审计回溯将静默失败，且 PG 无限增长。

---

## REQ-17 — 非功能：报表查询延迟 P99 < 2s

> "报表查询延迟 | P99 < 2s" — §6

**Verdict:** undecidable · confidence: high（对"不可静态判定"这一点）

**What this demands:** 成本报表查询在生产负载下 P99 延迟低于 2 秒。

**Where enforcement lives:** 无法静态核查。无性能测试、无压测脚本、无延迟 SLO 监控规则（`docker/prometheus/prometheus.yml` 仅抓取配置，无 alert rule 文件）。

**Paths walked:** ✓ 现有查询实现的静态审视：`BudgetGuard.getConsumedBudget`（BudgetGuard.java:100-110）按月拉全部记录到 List 再内存求和；`queryCostByTenant`/`queryCostByExecution` 同为全行拉取 + 内存 reduce——数据量增长后风险高；但 P99 是运行时属性，不能据此裁决。

**How the verdict was reached:** 缺少任何可执行或可观测的验证手段 → undecidable（非 absent：这是对运行时行为的要求，不存在"代码位置"可判缺失）。

**Severity: —（建议补基准测试与 Grafana SLO 面板）**

---

## notChecked

无。spec 中全部可单独核查的规范性需求（17 条）均已裁决。§7"相关文档"为引用性内容，不构成需求。

## Open questions

1. **sf_cost_record 的建表责任**：仅存在于 agent-engine 的 Flyway 迁移（V2026_05_09_02），`docker/postgres/init/` 未包含；仅启动 ops 服务（不启动 engine 跑 Flyway）时该表不存在，`CostEventConsumer`/`CostService` 的 insert 会失败——未见部署顺序文档约定。
2. **CH 库名错配**：ops `application.yml:40` CH database 默认 `schemaplexai`，而 CH DDL 建库 `schemaplexai_costs`（01-cost-analytics.sql:6）；正确的环境变量注入未在任何 compose/文档中出现。
3. **CH DDL 未接入 compose**：`docker-compose.yml:121-136` 的 clickhouse 服务未挂载 `docker/clickhouse/init/`，DDL 需手工执行；无文档说明。
4. **两套并行执行事件拓扑**：`sf.execution.events.cost.queue`（ops CostEventConsumer.java:37，exchange `execution_events`/key `cost.*`，消息模型 `CostRecordedEvent`）与 `sf.execution.event.queue`（task RabbitMqConfig.java:128-141，exchange `sf.exchange`/key `sf.agent.exec.event`，消息模型 `ExecutionEventMessage`）并存——哪套是目标架构未在 spec/设计中裁定。
5. **定时任务的租户拦截器影响**：`checkBudgetAlerts` 用 `selectList(null)`（CostService.java:108），而 `TenantLineInterceptor.java:14-20` 在上下文无租户时返回 NullValue——若 MyBatis-Plus 据此追加 `tenant_id = NULL` 条件，调度任务上下文中的预算检查将查不到任何行（本核查未做运行时验证，列为待验证项）。
6. **spec frontmatter `status: 草稿`** 与任务声明"已批准"不一致，建议确认状态字段。

## 反向差距（代码有、spec 无）

| 行为 | 位置 | 说明 |
|------|------|------|
| 工具调用固定计费 0.01 USD/次 | CostService.java:33,221-244 | spec 只定义 token 计费 |
| 执行前预算准入熔断（BudgetGuard） | BudgetGuard.java:37-65；agent-engine ExecutionAdmissionService.java:66-78（硬编码预估 0.10，:69） | 以预估成本做准入；spec 只有事后告警 |
| 第二套预算体系 sf_budget_config | ops V2026_05_09_03 迁移:4-13；BudgetConfig/BudgetConfigMapper | 月度限额 + UNIQUE(tenant)，与 sf_budget 并存且互不联动 |
| 逐执行成本查询 API | CostService.java:65-105；CostWebController.java:34-39 | GET /web/costs/executions/{id} |
| 预算 CRUD 之外 6 个扩展端点 | BudgetController.java:43-87 | GET by id / allocate / check-limit / usage / by-tenant / update-allocation |
| CH 额外分析表 | 01-cost-analytics.sql:56,145；02-agent-timeline.sql:13 | sf_model_usage_hourly、sf_agent_execution_cost、agent_timeline_event |
| 执行级 TokenBudget 门禁（chars/4 估算） | ThinkingStateHandler.java:163,196,464-465；TokenEstimator.java:13-17 | 成本"预防"机制，与采集（事后计量）无衔接 |
| 同步/统计任务的幂等与分布式锁 | CostSyncConsumer.java:59-63（Redis 去重）；CostStatisticsJob.java:22（ShedLock） | 工程增强 |
| 跨模块复用：task/web/engine 直接依赖 ops 模块 | task/pom.xml:12-13；web/pom.xml:18-19；agent-engine/pom.xml:12 | 与 agents.md 的微服务拆分表述存在架构漂移 |

## 反驳复核

> 复核日期: 2026-08-29 · 复核人: 独立复核员（非原作者） · 范围: 全部 severity ∈ {Critical, High} 且 verdict ∈ {contradicted, partial, absent} 的条目，共 8 条（REQ-01/03/04/05/08/09/10/15）。
> 方法: ①代码方向——重读全部被引用行；独立重跑全仓发布点枚举（`convertAndSend`/`rabbitTemplate.send` 于 src/main 实为 11 处，原作者漏列 3 处：workflow `SpecReviewNotificationDelegate.java:132`、`NotifyEngineDelegate.java:73`，system `TenantPolicyService.java:79`——逐一核读后分别为通知/审批决定/租户策略事件，均与成本无关；outbox 写者仅 `AgentExecutionLifecycleService.java:128` 与 `ApprovalRequestProducer.java:78`，经 `ExecutionEventService.appendEventAndOutbox` 全路径复核无 `cost.*` topic）；agent-engine 内 `price|rate|单价` 0 命中，计费分支仅 `CostService.calculateCost` 一处（另注：`gpt-4o` 命中 `contains("gpt-4")` 分支，但 claude 系仍恒 0，不改变结论）；`TOKEN_USED` 生产者、`tokenUsage()` 读取、`sf.cost` 发布、`SfCostRecordMapper` 于 engine 的使用点均为 0。②规格方向——重读 §1/§2/§3.1/§3.2/§3.3/§4.1/§6 对应段落：所有被引条文均为规范性表述，无"示例价格/可扩展/建议项"等弱化措辞；§6 精度与保留期为硬性目标列。

| REQ | 原判 | 裁定 | 理由（一行） | 证据 |
|-----|------|------|------|------|
| REQ-01 | absent·Critical | 维持 | 全仓重枚举全部发布点（含原作者漏列的 3 处）均与成本无关，无 tokenUsage 读取、无 TOKEN_USED/CostRecordedEvent/sf.cost 生产者、engine 注册的成本 Mapper 零使用，契约文件存在但 `publishCostRecorded()` 无实现 | 独立重跑 `convertAndSend`/`rabbitTemplate.send`（11 处）与 `TOKEN_USED\|CostRecordedEvent\|publishCostRecorded`、`tokenUsage()`、outbox 写者枚举，全部 0 命中；LlmProviderAdapter.java:81-85 tokenUsage 丢弃复核无误；contracts/costRecordedEvent.groovy:11,15 |
| REQ-03 | partial·Critical | 维持 | 计费全仓唯一入口 `CostService.calculateCost` 仅 gpt-4/gpt-3.5 两分支、其余返回 0，价格硬编码不读配置；规格 §3.1 公式与价格来源均为规范条文，无允许硬编码或可扩展声明 | CostService.java:29-35,147-155 复核；`input_price\|output_price\|price_per_1k\|inputPrice\|outputPrice` 全仓（含 yml/xml）0 命中；agent-engine 内 price/rate 0 命中；AnthropicProvider.java:29 claude 默认模型落入 0 分支；规格 §3.1 无弱化措辞 |
| REQ-04 | absent·High | 维持 | `sf_ai_model` DDL 无价格列、全部迁移无 ALTER、实体无字段、无任何读取方；`config_json` 无按价格解释的代码 | 01-init-schema.sql:98-109 重读；`db/migration/*.sql` 中 `sf_ai_model` 0 命中；全仓价格标识符 0 命中 |
| REQ-05 | contradicted·High | 维持 | 同步源/目标均为 `sf_agent_execution` 执行元数据，与规格要求的成本数据语义相反；全仓无任何向 `schemaplexai_costs.*`/`cost_records` 的写入，无备选成本同步路径 | ClickHouseCostSyncService.java:36,135-142,173-178 逐行复核；03-init-schema-others.sql:346-372 游标/批次表列名与实体字段互不匹配复核无误；`schemaplexai_costs\|cost_records` Java 写入 0 命中 |
| REQ-08 | partial·High | 维持 | `sf_budget` 无 `target_id`（AGENT/MODEL 级预算不可表达）、`limit_amount` DECIMAL(18,4)、阈值单位三处冲突（DDL 缺省 80.00 百分数 / `allocateBudget` 缺省 0.8 小数 / 告警比较按百分数） | 03-init-schema-others.sql:294-305、SfBudget.java:13-19、BudgetServiceImpl.java:38-40,106-122、CostService.java:114-123 全部重读无误；`target_id\|targetId` 于 ops src/main 0 命中 |
| REQ-09 | partial·High | 维持 | `checkBudgetAlerts` 全仓唯一调用方为每日 01:00 的 CostStatisticsJob，13 处 `@Scheduled` 无每小时 cron，越限动作仅 `log.warn`；ops 虽存在 NotificationService 但 `sendNotification` 仅被 REST 控制器调用，预算告警路径零接入 | @Scheduled 全量重枚举（13 处，无 `0 0 * * * ?`）；CostStatisticsJob.java:21,27；`sendNotification\|checkBudgetAlerts` 调用方枚举——预算告警无通知生产者；规格 §3.3/§5 "每小时"+"发送告警通知"为规范条文 |
| REQ-10 | partial·High | 维持 | `/ops/costs/summary` 不存在（仅 `/web/costs/summary`），无日期参数，响应 7 字段仅兑现 totalCost（且数据源为预算代理），维度分解字段全仓无任何实现 | CostController.java:24-28、CostWebController.java:27-31 重读；`byAgent\|byModel\|byDay\|totalInputTokens\|callCount\|startDate\|endDate` 全仓 src/main 0 命中 |
| REQ-15 | contradicted·High | 维持 | 计费分量在 `COST_SCALE=4` 处 HALF_UP 舍入，规格 §6 明文"6 位小数"为硬性指标非建议项；小额调用（如 33 tokens × 0.0015/1000）被系统性归零，方向性偏差累积 | CostService.java:35,157-160 复核；全仓 `setScale(6`/scale-6 0 命中；sf_budget DECIMAL(18,4)（03-init-schema-others.sql:298-299）复核无误；规格 §6 表格"目标"列无弱化措辞 |

**复核结论**: 8/8 维持，0 推翻。独立重跑的证据检索覆盖了原作者遗漏的 3 处生产发布点与 outbox 间接路径，均未发现成本采集/发布/配置化价格的隐藏实现；规格方向未发现任何可将上述条文解读为建议项或可扩展默认值的措辞。原分析的总裁决与严重度定级成立。
