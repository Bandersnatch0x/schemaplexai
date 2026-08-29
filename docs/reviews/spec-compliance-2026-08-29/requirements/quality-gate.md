# Spec 合规核查报告 — quality-gate

- **Spec**: `docs/specs/2026-04-30-v1.0-quality-gate.md`(注意:文件 frontmatter 实际标注 `status: 草稿`,与核查任务声称的"已批准"不一致 — 文档状态漂移,见 Open questions)
- **核查目标**: `schemaplexai-quality/src/main/java` 及关联执行点(agent-engine / task / gateway / web / common / docker SQL)
- **核查日期**: 2026-08-29
- **方法**: 逐条需求 grep 定位 → 读实际代码 → 走通全部调用路径(重点核查门禁被绕过的可达路径)

**总计 25 条需求**:implemented 6 · partial 7 · absent 11 · undecidable 1 · contradicted 0

**一句话总评**:门禁的"骨架"(Orchestrator、规则注册表、2/5 条规则、门禁 CRUD API)已存在且有单测,但**门禁在任何生产执行路径上都不会被触发**——`QualityOrchestrator.evaluate()` 无任何生产调用方,`sf.quality` MQ 无生产者,引擎在 OBSERVATION 后走的是自研 REFLECTING/Guardrails 机制;叠加"默认门禁零配置"与"gate 表 status 字段类型不匹配",质量门禁当前是一个完全旁路的能力。

---

## REQ-01 — 检查时机:执行后 / 工具调用后 / 工作流节点后三处触发

> "检查时机: Agent 执行完成后（后置检查）/ 工具调用结果返回后（中间检查）/ 工作流节点完成后（节点级检查）" — spec §1

**Verdict:** absent · confidence: high
**What this demands:** 三个执行时点上存在对质量门禁检查的实际调用(进程内调用、HTTP 或 MQ 均可)。
**Where enforcement lives:** 无。唯一的检查入口 `QualityOrchestrator.evaluate()`(QualityOrchestrator.java:43)在生产代码中零调用方(仅 `QualityGateServiceImpl.evaluateGate` QualityGateServiceImpl.java:127 调用它,而 `evaluateGate` 本身也无生产调用方,只被测试调用)。
**Paths walked:**
- ✗ Agent 执行完成后:engine 的 `ObservationStateHandler` 在无 Final Answer 时转 REFLECTING(ObservationStateHandler.java:51-57),REFLECTING 走 LLM 自评 + GuardrailsEngine(ReflectingStateHandler.java:72, 89),全程不接触 quality 模块;engine 主代码对 `com.schemaplexai.quality` 零 import。
- ✗ 工具调用后:`ToolCallingStateHandler` 的 GATE_BLOCKED 转移(ToolCallingStateHandler.java:110-176)全部由**审批/预算**触发,非质量规则。
- ✗ 工作流节点后:workflow 模块自行计算 mock 分数 `computeQualityScore`(AiAgentExecutionDelegate.java:83-85,注释明言 "mock quality score"),不调用 quality 模块。
- ✗ MQ 路径:`sf.quality` 队列在 task 模块有消费者(QualityEventConsumer.java:46)但**全仓库无任何生产者**(见 Searched)。
**Searched:**
- `QualityOrchestrator|checkQualityGate|runQualityPipeline` 全仓库 → 仅 quality 模块自身 + 其测试,无外部调用方
- `quality`(忽略大小写)于 `schemaplexai-agent-engine/src/main` → 仅注释/无关命名,无跨模块调用
- `convertAndSend(.*RK_QUALITY|.*"sf.quality"` 全仓库 → 0 命中(RK_QUALITY 仅在 CommonConstants.java:41 定义、RabbitMqConfig.java:103,110 绑定队列)
**How the verdict was reached:** 不是 partial——三个时点无一存在调用;不是 implemented(不同机制)——引擎的 REFLECTING/Guardrails 是引擎内部机制,不经过本 spec 规定的规则注册表/门禁配置/问题落库,不能视为等价实现(仅在 REQ-18 中注记为相邻机制)。
**Severity: Critical** — 整个门禁能力在所有执行路径上被旁路;按指示"整个能力缺失如实记 absent"。

---

## REQ-02 — 处理策略:PASS 继续 / WARN 告警继续 / BLOCK 暂停待人工 / FAIL 终止

> "处理策略: `PASS`: 继续执行 / `WARN`: 记录告警，继续执行 / `BLOCK`: 暂停执行，等待人工确认 / `FAIL`: 终止执行，标记失败" — spec §1

**Verdict:** absent · confidence: high
**What this demands:** 检查结果按四级处置语义驱动执行流(暂停/终止/继续)。
**Where enforcement lives:** 无。`QualityCheckResult` 只有 `passed` 布尔 + severity 枚举为 `CRITICAL/HIGH/MEDIUM/LOW`(QualityCheckResult.java:12-14),不存在 PASS/WARN/BLOCK/FAIL 处置枚举;`evaluate()` 对所有失败一视同仁——插入 `sf_quality_issue` 并置 `allPassed=false`(QualityOrchestrator.java:73-82),之后**不采取任何处置动作**。
**Paths walked:**
- ✓ PASS → 继续:trivially 成立(无动作即继续)。
- ✗ WARN → 记录告警:仅落库 issue,无告警/通知发布(evaluate() 无 MQ/notification 调用)。
- ✗ BLOCK → 暂停待人工:engine 有 `PauseReason.QUALITY_GATE_BLOCKED`(PauseReason.java:6)和 `ResultCode.QUALITY_GATE_BLOCKED(6001)`(ResultCode.java:44),但两者均为**定义后从未使用**;quality 模块的 ApprovalTicket 人工确认流(ApprovalRequestConsumer.java:28-58)只服务于工具审批,与质量检查结果无关联。
- ✗ FAIL → 终止执行:无任何代码将检查失败映射为执行终止。
**Searched:**
- `"(BLOCK|WARN|PASS|FAIL)"` 于 `schemaplexai-quality/src/main` → 0 命中
- `QUALITY_GATE_BLOCKED|PauseReason` 全仓库 → 定义处 + USER_REQUEST/HANDOFF 用法,QUALITY_GATE_BLOCKED 无使用点
- `ResultCode.QUALITY_GATE_BLOCKED` 全仓库 → 0 使用
**How the verdict was reached:** 不是 partial——四级语义中除 vacuous 的 PASS 外无一实现;severity 枚举 CRITICAL/HIGH/MEDIUM/LOW 是"问题分级"而非"处置策略",两者语义不同,不构成不同机制的等价实现。
**Severity: High** — 即使未来接通触发点,BLOCK/FAIL 也不会拦截任何东西。

---

## REQ-03 — QualityOrchestrator 与规则注册表 Map<String, QualityRule>

> "QualityOrchestrator / 规则注册表 (Map<String, QualityRule>)" — spec §2

**Verdict:** implemented · confidence: high
**What this demands:** 存在编排器组件,持有规则名→规则实现的注册表并逐规则执行。
**Where enforcement lives:** QualityOrchestrator.java:27-40 — `@Component`,注入 `List<QualityRule>`,`@PostConstruct init()` 构建 `Map<String, QualityRule> rules`;evaluate() 按 gate 的规则名查表执行(:60-71)。
**Paths walked:**
- ✓ 正常路径:规则命中 → check() → 结果聚合(:70-71)。
- ✓ 规则名无实现:记 CRITICAL fail 结果并 `allPassed=false`,继续下一条(:62-68)——fail-closed,未静默跳过。
- ✓ rulesJson 解析失败:记 CRITICAL fail(:52-58)——fail-closed。
**How the verdict was reached:** 结构与 spec 架构图一致;注册表挂 5 个规则位中只注册了 2 个的问题归 REQ-11/12/13 单独裁决,不折损本条。

---

## REQ-04 — QualityRule 接口契约

> "public interface QualityRule { String getRuleName(); QualityCheckResult check(QualityContext context); }" — spec §3.1

**Verdict:** implemented · confidence: high
**What this demands:** 接口具备 getRuleName + check(QualityContext) 两方法。
**Where enforcement lives:** QualityRule.java:3-8,与 spec 逐字一致;两个实现类(SecurityScanRule.java:15, SpecComplianceRule.java:15)均实现该接口。
**Paths walked:** ✓ 接口即契约,无分支路径。
**How the verdict was reached:** 完全匹配。

---

## REQ-05 — QualityContext 含 agentOutput / specContent / metadata

> "private String agentOutput; // Agent 输出内容 / private String specContent; // 关联 Spec 内容 / private Map<String, Object> metadata;" — spec §3.1

**Verdict:** partial · confidence: high
**What this demands:** 规则可从上下文直接获取 Agent 输出全文与关联 Spec 内容。
**Where enforcement lives:** QualityContext.java:12-16 — 实际字段为 `executionId / specId / metadata`;无 `agentOutput`、无 `specContent`。输出内容仅以约定键 `metadata.get("output")` 传递(SecurityScanRule.java:27),Spec 内容完全无载体。
**Paths walked:**
- ✓ metadata 存在。
- ✗ agentOutput:生产调用方构造 context 时一律传 `Map.of()` 空 metadata(QualityOrchestrator.java:94, 101;QualityGateServiceImpl.java:126),即使约定键机制存在,实际路径中输出内容永远为 null。
- ✗ specContent:无字段、无键约定、无加载逻辑(SpecComplianceRule 只看 `specId` 是否存在,不读 spec 内容)。
**Searched:** `agentOutput|specContent` 于 quality 模块 → 0 命中(仅 docs)。
**How the verdict was reached:** metadata 一项成立、以键约定部分替代 agentOutput 可算机制漂移,但 specContent 能力性缺失使规则根本无法比对 Spec → partial 而非 implemented(doc drift)。
**Severity: Medium**

---

## REQ-06 — SPEC_COMPLIANCE 规则:检查输出是否满足关联 Spec

> "**规则名**: `SPEC_COMPLIANCE` / **职责**: 检查 Agent 输出是否满足关联 Spec 的要求" — spec §3.2

**Verdict:** partial · confidence: high
**What this demands:** 存在该名称规则,且真正对"输出 vs Spec 要求"做比对。
**Where enforcement lives:** SpecComplianceRule.java:19(名称正确并注册)。但 check()(:23-48)不做任何输出比对——只检查 metadata 中的证据旗标:`specId` 缺失→fail MEDIUM(:28-30),`specComplianceViolations` 非空→fail HIGH(:32-35),`specCompliancePassed==false`→fail HIGH(:37-39),`checked&&passed` 才 pass(:41-47)。即它假设**别处已做过检查**,自己只验旗标,而全仓库无任何生产代码写入这些旗标。
**Paths walked:**
- ✓ specId 缺失 → fail(生产调用方传空 metadata,实际永远走此路径 → 规则恒 fail,fail-closed 而非旁路)。
- ✓ 有 violations → fail;✓ passed=false → fail;✓ checked&&passed → pass。
- ✗ 对输出内容与 Spec 的任何实际比对逻辑:不存在。
**Searched:** `specComplianceChecked|specCompliancePassed|specComplianceViolations` 全仓库 → 仅该规则自身 + SpecComplianceRuleTest,无旗标生产者。
**How the verdict was reached:** 规则存在且 fail-closed(不构成放行漏洞),但职责的核心("检查输出满足 Spec")委托给了不存在的上游 → partial;非 contradicted,因为没有反向行为。
**Severity: Medium**

---

## REQ-07 — SPEC_COMPLIANCE 分级策略 L1 关键词 / L2 结构化 / L3 LLM

> "| L1 | 关键词匹配 | … | L2 | 结构化匹配 | … | L3 | LLM 评估 | …" — spec §3.2

**Verdict:** absent · confidence: high
**What this demands:** 规则内部存在三级实现策略(关键词、模板结构、LLM 评估)。
**Where enforcement lives:** 无。SpecComplianceRule.java 全文(66 行)无任何匹配/评估逻辑。
**Paths walked:** ✗ L1 ✗ L2 ✗ L3 — 均不存在。
**Searched:**
- `complianceScore|score|keyword|template|llm`(忽略大小写)于 `schemaplexai-quality/src/main` → 仅 V2026_05_10_1 迁移文件中的 `risk_score` 列(审批工单,无关)
- quality 模块 pom/import 中 LangChain4j / LLM client → SpecComplianceRule 无任何模型调用依赖
**How the verdict was reached:** 三级策略无一存在,连降级壳都没有 → absent 而非 partial。
**Severity: Medium**

---

## REQ-08 — 合规评分阈值:≥0.8 PASS / ≥0.5 WARN / 否则 BLOCK

> "if (complianceScore >= 0.8) return PASS; if (complianceScore >= 0.5) return WARN; return BLOCK;" — spec §3.2

**Verdict:** absent · confidence: high
**What this demands:** 存在 0~1 合规分及 0.8/0.5 两档阈值到 PASS/WARN/BLOCK 的映射。
**Where enforcement lives:** 无。SpecComplianceRule 返回值只有 pass / fail(MEDIUM|HIGH),无评分概念(SpecComplianceRule.java:29,34,38,44,47)。
**Paths walked:** ✗ 无评分路径可走。
**Searched:** `complianceScore|0\.8|0\.5` 于 `schemaplexai-quality/src/main` → 0 命中(见 REQ-07 搜索记录)。
**How the verdict was reached:** 评分机制整体缺失 → absent。
**Severity: Medium**

---

## REQ-09 — SECURITY_SCAN 规则存在并检测输出安全隐患

> "**规则名**: `SECURITY_SCAN` / **职责**: 检测输出中的安全隐患" — spec §3.3

**Verdict:** partial · confidence: high
**What this demands:** 该名称规则存在,并对输出内容做安全检测。
**Where enforcement lives:** SecurityScanRule.java:19(名称正确并注册)。实际检测仅两个朴素子串:`output.contains("password=") || output.contains("secret=")` → fail CRITICAL(:29-31);其余逻辑与 SpecComplianceRule 同型——验 metadata 证据旗标(`securityScanFindings`/`securityScanPassed`/`securityScanCompleted`,:37-50),无旗标生产者。
**Paths walked:**
- ✓ output 含 `password=`/`secret=` → fail CRITICAL(唯一的真实内容检测)。
- ✓ metadata 为 null → fail HIGH "Missing security scan evidence"(:33-35)— fail-closed。
- ✓ findings 非空 → fail;✓ passed=false → fail;✓ completed/passed=true → pass;✓ 无证据 → fail(:50)。
- ✗ 绕过路径核查:`output.contains("password=")` 是大小写敏感的精确子串——`PASSWORD = "x"`(大写或等号旁有空格)不命中;但因整体 fail-closed(无证据即 fail),该弱检测不构成放行漏洞,只构成检测能力缺失。
**How the verdict was reached:** 规则存在、fail-closed,但"检测安全隐患"的实质能力(见 REQ-10)绝大部分缺失 → partial。
**Severity: High**(与 REQ-10 合并评估:安全扫描名存实亡)

---

## REQ-10 — 安全扫描六项检测项及等级映射

> "| 硬编码密码 | `password\s*=\s*[\"'][^\"']+[\"']` | BLOCK | 密钥泄露 | `api[_-]?key…` | BLOCK | 敏感信息 | 手机号、身份证号、银行卡号正则 | BLOCK | SQL 注入痕迹 | `'; DROP\s+TABLE` 等 | FAIL | XSS 载荷 | `<script>`、事件处理器 | FAIL | PII 泄露 | 邮箱、地址模式 | WARN |" — spec §3.3

**Verdict:** partial · confidence: high
**What this demands:** 六类模式检测各自存在,且映射到规定的处置等级。
**Where enforcement lives:** 仅 SecurityScanRule.java:29-31 近似覆盖第 1 项(子串而非 regex,且等级为 CRITICAL 而非 BLOCK);`secret=` 子串可勉强算第 2 项的远亲(但 spec 的 `api[_-]?key` 模式不存在)。
**Paths walked:**
- ~ 硬编码密码:contains("password=") — 无 regex、大小写敏感、不校验引号值;等级映射错(CRITICAL≠BLOCK,且系统无 BLOCK 语义,见 REQ-02)。
- ✗ 密钥泄露 `api[_-]?key`:无。✗ 手机号/身份证/银行卡:无。✗ SQL 注入:无。✗ XSS:无。✗ PII 邮箱/地址:无。
**Searched:**
- `DROP\s+TABLE|<script>|api[_-]?key|Pattern.compile(.*(password|key|script|phone|身份证)` 全仓库 → 命中仅 2 处:`schemaplexai-common/.../PiiRedactor.java:13-14`(password/api-key regex,但用途是**日志脱敏**而非门禁扫描,且 quality 模块未引用它)、engine 测试文件 SkillLoaderSecurityTest.java:31(无关)。
- `手机|身份证|银行卡|1[3-9]\d{9}` 于 quality 模块 → 0 命中。
**How the verdict was reached:** 6 项中 1 项弱近似、5 项完全缺失,且等级体系整体不符 → partial(偏向 absent 一侧);PiiRedactor 属不同用途组件,不构成不同机制的等价实现。
**Severity: High** — 安全扫描是 spec 默认策略中唯一 BLOCK 级规则,其实质检测能力缺失意味着即使门禁被接通,密钥/PII/注入内容也几乎不会被识别。

---

## REQ-11 — GROUNDING_CHECK 规则(RAG 事实性验证)

> "**规则名**: `GROUNDING_CHECK` / **职责**: 验证输出中的事实性声明是否基于提供的知识库" — spec §3.4

**Verdict:** absent · confidence: high
**What this demands:** 存在 GROUNDING_CHECK 规则:提取声明 → RAG 检索 → 一致性比对 → 标记不可验证项。
**Where enforcement lives:** 无。
**Paths walked:** ✗ 无任何路径。
**Searched:**
- `GROUNDING|Grounding` 全仓库 → 4 个文件全部为 docs(docs/ui/quality-center、docs/specs/README、本 spec、docs/requirements/prd-core),0 个 Java/SQL/YAML 命中
- `gate/rules/` 目录枚举 → 仅 SecurityScanRule.java、SpecComplianceRule.java 两个实现
**How the verdict was reached:** 能力整体缺失,如实记 absent。
**Severity: Medium**

---

## REQ-12 — OUTPUT_FORMAT 规则(JSON 有效性 / Schema / 长度 / 编码)

> "**规则名**: `OUTPUT_FORMAT` … JSON 有效性 / Schema 合规 / 长度限制 / 编码检查" — spec §3.5

**Verdict:** absent · confidence: high
**What this demands:** quality 模块存在 OUTPUT_FORMAT 规则,含四项检测。
**Where enforcement lives:** quality 模块内无。相邻机制注记:engine 的 `LengthGuardrail`(schemaplexai-agent-engine/.../guardrails/LengthGuardrail.java:30-32, 45-57)在 REFLECTING 态对输出做长度上限校验(默认 100_000,LengthGuardrail.java:10)——覆盖"长度限制"一个子项,但位于引擎内部、不经门禁配置、无 JSON/Schema/编码检查,不构成本规则的等价实现。
**Paths walked:** ✗ JSON 有效性 ✗ Schema ✗ 编码 — 全仓库无;~ 长度(引擎侧异构机制)。
**Searched:** `OUTPUT_FORMAT|OutputFormat` 全仓库 → 仅 docs 4 文件命中(同 REQ-11 搜索);`JsonSchema|schema.*valid`(忽略大小写)于 quality 模块 → 0。
**How the verdict was reached:** spec 范围限定 schemaplexai-quality 服务,该规则在此范围完全缺失 → absent,引擎侧部分重叠仅作注记。
**Severity: Medium**

---

## REQ-13 — ARTIFACT_CHECK 规则(语法 / 依赖 / 规范检查)

> "**规则名**: `ARTIFACT_CHECK` / **职责**: 验证生成的代码/文档/配置文件的质量" — spec §3.6

**Verdict:** absent · confidence: high
**What this demands:** 存在 ARTIFACT_CHECK 规则,检查制品语法/依赖/编码规范。
**Where enforcement lives:** 无。
**Paths walked:** ✗ 无任何路径。
**Searched:** `ARTIFACT_CHECK|ArtifactCheck|Artifact` 全仓库 Java → 0 门禁相关命中(仅 docs);`gate/rules/` 目录仅 2 个规则类(见 REQ-11)。
**How the verdict was reached:** 能力整体缺失 → absent。
**Severity: Medium**

---

## REQ-14 — sf_quality_gate 表结构:rule_name / severity / enabled / config JSONB

> "| id | BIGINT | 主键 | tenant_id | BIGINT | 租户隔离 | name | VARCHAR | 门禁名称 | rule_name | VARCHAR | 规则名 | severity | VARCHAR | PASS / WARN / BLOCK / FAIL | enabled | BOOLEAN | 是否启用 | config | JSONB | 规则特定配置 |" — spec §4.1

**Verdict:** partial · confidence: high
**What this demands:** 每条门禁记录绑定单一规则、有独立处置等级、可启停、有 JSONB 配置。
**Where enforcement lives:** DDL:docker/postgres/init/03-init-schema-others.sql:134-143 — 实际列为 `id, tenant_id, name, rules_json TEXT, status VARCHAR(32) DEFAULT 'ACTIVE', created_at, updated_at, deleted`。实体:SfQualityGate.java:13-15(`name, rulesJson, status Integer`)。
**Paths walked:**
- ✓ id/tenant_id/name 存在(tenant_id 由 DDL:136 与 BaseEntity 双重保证)。
- ✗ rule_name → 变为 rules_json(一个 gate 持有规则名 JSON 数组,QualityOrchestrator.java:51 解析)— 机制漂移可接受,但:
- ✗ severity:无列。门禁级处置等级不可配置,直接使 REQ-02/REQ-15 的 severity 配置无处落地。
- ✗ enabled:被 status 取代,但语义未被执行——`QualityOrchestrator.evaluate()` 用 `gateMapper.selectList(null)` 加载**全部** gate,不过滤 status(QualityOrchestrator.java:44),INACTIVE(甚至 DEPRECATED)门禁照常评估;而 `QualityGateServiceImpl.save()` 默认新 gate 为 INACTIVE(QualityGateServiceImpl.java:39-41)。启停开关形同虚设。
- ✗ config JSONB:无列,规则无按门禁的特定配置。
- ✗ **类型不匹配缺陷**:DDL `status VARCHAR(32) DEFAULT 'ACTIVE'`(SQL:139)vs 实体 `Integer status` + 常量 0/1/2(QualityGateServiceImpl.java:29-31)。PostgreSQL 参数化插入 int4 → varchar 列将报 42804 类型错;`evaluateGate` 按 `status=1` 查询(QualityGateServiceImpl.java:122)对存量 'ACTIVE' 行永不命中 → 恒抛 NOT_FOUND。门禁配置持久层在 PG 上大概率整体不可用(未运行验证,列入 Open questions)。
**Searched:** `sf_quality_gate` 于 docker/ 与 quality 模块 resources → 仅 03-init-schema-others.sql:134;quality 模块 Flyway 目录(db/migration/)仅 processed_event 与 approval_ticket 两个迁移,无 gate 表修正。
**How the verdict was reached:** 表存在且承担同一职能(机制漂移),但 severity/enabled/config 三项能力性缺失 + enabled 语义被 evaluate() 无视 + 实体/DDL 类型冲突 → partial,不是 implemented-with-drift。
**Severity: High** — 后果:门禁无法按规则配置处置等级,启停失效,且 CRUD 在真实 PG 上疑似不可用。
**Open questions:** status 类型冲突的实际运行时行为未经启动验证(项目无可运行的 PG 集成测试)。

---

## REQ-15 — 默认门禁策略:SECURITY_SCAN(BLOCK) + SPEC_COMPLIANCE(WARN) + OUTPUT_FORMAT(WARN) 默认启用

> "默认策略: - rule: SECURITY_SCAN severity: BLOCK enabled: true - rule: SPEC_COMPLIANCE severity: WARN enabled: true - rule: OUTPUT_FORMAT severity: WARN enabled: true" — spec §4.2

**Verdict:** absent · confidence: high
**What this demands:** 全新部署即有三条默认门禁生效(种子数据或代码内建缺省)。
**Where enforcement lives:** 无。
**Paths walked:**
- ✗ SQL 种子:docker/postgres/init/ 三个脚本无 `INSERT INTO sf_quality_gate`。
- ✗ 代码内建:QualityOrchestrator 无空表兜底——`gates` 为空时循环体不执行,直接返回 `allPassed=true`(QualityOrchestrator.java:44-48, 86)。
- ✗ 配置文件:quality 模块 application.yml 仅 port/jaeger/logging(全文 17 行),无门禁配置。
- **可达放行路径**:全新环境 → 零 gate → 任何 evaluate 调用 vacuous 通过。这是"门禁被绕过"的第二层(第一层是 REQ-01 根本不调用)。
**Searched:**
- `INSERT INTO sf_quality` 于 docker/postgres/init/ → 0 命中
- `SECURITY_SCAN|SPEC_COMPLIANCE` 全仓库 → 命中仅规则类 getRuleName、其测试、task 模块 TODO 注释、docs;无种子/配置命中
**How the verdict was reached:** 默认策略在任何介质(SQL/代码/配置)中均不存在 → absent。
**Severity: High** — 即使触发点接通,默认部署下门禁仍为空转放行。

---

## REQ-16 — QualityReport 结构:executionId / allPassed / results / checkedAt

> "private Long executionId; private boolean allPassed; private List<QualityCheckResult> results; private LocalDateTime checkedAt;" — spec §5

**Verdict:** partial · confidence: high
**What this demands:** 报告对象含四字段,尤其检查时间戳。
**Where enforcement lives:** QualityReport.java:12-17 — 有 executionId/allPassed/results,**无 checkedAt**(构造调用 QualityOrchestrator.java:86 亦仅传三参)。
**Paths walked:** ✓ 三字段赋值路径正确;✗ checkedAt 无字段。
**Searched:** `checkedAt` 于 quality 模块 → 0 命中。
**How the verdict was reached:** 4 缺 1 → partial。
**Severity: Low** — 审计时间可从 sf_quality_issue 的 created_at 间接恢复。

---

## REQ-17 — 报告内容字段:ruleName / passed / severity / message / suggestion

> "| executionId | 关联执行 ID | ruleName | 规则名称 | passed | 是否通过 | severity | 严重等级 | message | 详细说明 | suggestion | 改进建议 |" — spec §5

**Verdict:** partial · confidence: high
**What this demands:** 每条检查结果可回溯到规则名并携带改进建议。
**Where enforcement lives:** QualityCheckResult.java:12-14 — 仅 passed/severity/message;**无 ruleName、无 suggestion**。规则名仅在失败落库时以 `issueType` 保存(QualityOrchestrator.java:77),报告对象里的 results 列表无法区分各结果来自哪条规则(通过的检查完全无规则归属)。
**Paths walked:** ✓ passed/severity/message;✗ ruleName(仅失败路径经 issue 间接保留);✗ suggestion(全模块无此概念)。
**Searched:** `suggestion` 于 quality 模块 → 0 命中。
**How the verdict was reached:** 5 项中 3 项直接成立、1 项半间接、1 项缺失 → partial。
**Severity: Low**

---

## REQ-18 — 与 Agent 执行引擎集成:OBSERVATION 后 QualityGate Check 并按结果分流

> "Agent 执行流程: THINKING → TOOL_CALLING → OBSERVATION → QualityGate Check → PASS 继续执行 / WARN/BLOCK 记录问题/告警 人工确认后继续 / FAIL 终止执行" — spec §6

**Verdict:** absent · confidence: high
**What this demands:** 引擎状态机在 OBSERVATION 之后调用质量门禁,并按结果分流到继续/人工确认/终止。
**Where enforcement lives:** 无对接。实际引擎流:OBSERVATION → REFLECTING(ObservationStateHandler.java:51-57)→ LLM 自评(最多 2 轮,ReflectingStateHandler.java:25, 57-63)+ GuardrailsEngine.validateOutput(ReflectingStateHandler.java:72-79;Guardrail 仅黑名单关键词 BlacklistGuardrail.java:12-20 与长度 LengthGuardrail.java)→ 通过则 COMPLETED、需修订回 THINKING、Guardrails 拦截则 FAILED。该机制是**引擎内部自研旁路**:不查 sf_quality_gate 配置、不执行注册表规则、不落 sf_quality_issue、结果不可租户级配置。引擎的 GATE_BLOCKED 态由准入控制(AgentRuntimeOrchestrator.java:104-109)和工具审批(ToolCallingStateHandler.java:110-176)触发,与质量规则无关。
**Paths walked:**
- ✗ OBSERVATION → quality 模块调用:无(HTTP/MQ/进程内均无,见 REQ-01 Searched)。
- ✗ WARN/BLOCK → 人工确认后继续:quality 模块的 ApprovalTicket 人工流(ApprovalRequestConsumer.java)只接 `ApprovalRequestEvent`(工具审批),质量检查结果无法进入该流。
- ✗ FAIL → 终止:ReflectingStateHandler 的 Guardrails 拦截会 FAILED(:77),但那是引擎 Guardrails 而非门禁规则;且 LLM 反思异常时**接受当前输出转 COMPLETED**(ReflectingStateHandler.java:111-116)——引擎侧质量自评的降级路径是放行。
**Searched:** 同 REQ-01(QualityOrchestrator 调用方 / engine 内 quality 引用 / sf.quality 生产者 → 全部 0)。
**How the verdict was reached:** REFLECTING/Guardrails 与 spec 的门禁在"检查输出质量"目的上重叠,但绕开了本 spec 全部核心机制(配置表、规则注册表、报告、处置语义),不能记 implemented(不同机制);集成本身缺失 → absent。
**Severity: Critical** — 门禁与执行引擎完全断连,spec 的核心闭环(检查→分流)不存在;引擎侧替代机制的异常降级路径(反思失败即放行)进一步放大后果。

---

## REQ-19 — GET /quality/gates 获取门禁配置列表

> "GET    /quality/gates              # 获取门禁配置列表" — spec §7

**Verdict:** implemented · confidence: high
**Where enforcement lives:** QualityGateController.java:52-56(`@GetMapping` on `/quality/gates`,返回 `Result<List<SfQualityGate>>`);gateway 路由 `/quality/**` → lb://schemaplexai-quality(GatewayConfig.java:28-29)。
**Paths walked:** ✓ list() 无过滤全量返回(含 INACTIVE/DEPRECATED,租户隔离由 DAO TenantLineInterceptor 承担)。
**How the verdict was reached:** 路径/动词/语义匹配;返回体为实际实体结构(rulesJson/status)而非 spec 数据模型——payload 漂移已在 REQ-14 记录,不重复折损。

---

## REQ-20 — POST /quality/gates 创建门禁规则

> "POST   /quality/gates              # 创建门禁规则" — spec §7

**Verdict:** implemented · confidence: medium
**Where enforcement lives:** QualityGateController.java:22-27 → QualityGateServiceImpl.save(:37-48,校验 name 非空且 ≤128,默认 status=INACTIVE)。
**Paths walked:** ✓ 正常创建;✓ name 缺失/超长 → BaseException PARAM_ERROR(:157-162);✗ rulesJson 内容不校验(可存任意文本,解析失败推迟到 evaluate 时以 CRITICAL fail 兜底,QualityOrchestrator.java:52-58)。
**How the verdict was reached:** 端点契约实现;confidence 降为 medium 因 REQ-14 所述 status Integer↔VARCHAR 冲突可能使该端点在真实 PG 上 500(未运行验证)。

---

## REQ-21 — PUT /quality/gates/{id} 更新门禁规则

> "PUT    /quality/gates/{id}         # 更新门禁规则" — spec §7

**Verdict:** implemented · confidence: high
**Where enforcement lives:** QualityGateController.java:29-34 → QualityGateServiceImpl.updateById(:54-71,校验 id、存在性,name 非空时复验)。
**Paths walked:** ✓ 正常更新;✓ id 缺失 → PARAM_ERROR(:56);✓ 不存在 → NOT_FOUND(:59-61, 67-69)。
**How the verdict was reached:** 契约匹配,错误路径完备。

---

## REQ-22 — DELETE /quality/gates/{id} 删除门禁规则

> "DELETE /quality/gates/{id}         # 删除门禁规则" — spec §7

**Verdict:** implemented · confidence: high
**Where enforcement lives:** QualityGateController.java:36-40 → IService.removeById(BaseEntity `deleted` 逻辑删除,DDL:142)。
**Paths walked:** ✓ 删除即软删;✓ 不存在时返回 false(未抛错,可接受)。
**How the verdict was reached:** 契约匹配。

---

## REQ-23 — POST /quality/check 手动触发质量检查

> "POST   /quality/check              # 手动触发质量检查" — spec §7

**Verdict:** absent · confidence: high
**What this demands:** 存在手动触发检查的 HTTP 入口(这是当前唯一可能触达 evaluate() 的用户路径)。
**Where enforcement lives:** 无。quality 模块 6 个 Controller 的全部映射为:/quality/gates、/quality/issues、/quality/reviews、/quality/compliance、审计与安全策略,无 /quality/check。服务层虽有 `evaluateGate`/`checkQualityGate` 可作后端(QualityGateServiceImpl.java:118;QualityOrchestrator.java:91),但无 Controller 暴露、无任何生产调用方(仅测试调用,见 grep 记录)。
**Paths walked:** ✗ 无端点可走。
**Searched:**
- `quality/check` 全仓库 → 仅本 spec:235 与 docs/ui 命中
- `@PostMapping("/check")|/check|manualCheck|triggerCheck` 于 schemaplexai-quality → 0 命中
- `evaluateGate|checkQualityGate` 调用方 → 仅 QualityGateServiceImplTest / QualityOrchestratorTest
**How the verdict was reached:** 端点缺失且后端能力未暴露 → absent。
**Severity: Medium** — 叠加 REQ-01/18 后,evaluate() 在系统中成为完全不可达代码。

---

## REQ-24 — GET /quality/reports/{executionId} 获取质量报告

> "GET    /quality/reports/{executionId}  # 获取质量报告" — spec §7

**Verdict:** absent · confidence: high
**What this demands:** 按执行 ID 查询质量检查报告(规则/通过/等级/说明/建议)。
**Where enforcement lives:** 无该端点。最接近的是 GET /quality/compliance/executions/{executionId}(ComplianceReportController.java:30-33),但其内容是**审计事件**汇总(eventCount/corruptedCount/events,ComplianceReportService.java:22-35),不含任何质量规则检查结果——不构成等价实现。质量问题数据只能经 /quality/issues 全量列表获取(QualityIssueController.java:53-56),且该端点无 executionId 过滤参数。
**Paths walked:** ✗ 无按执行 ID 的质量报告路径;~ getGateSummary(executionId)(QualityGateServiceImpl.java:142-154)有此能力但未暴露、无调用方。
**Searched:** `quality/reports` 全仓库 → 仅本 spec 与 docs/ui;`reports` 于 quality 模块 Controller → 0。
**How the verdict was reached:** 端点与可达等价物均缺失 → absent。
**Severity: Medium**

---

## REQ-25 — 非功能需求:P99<500ms / 50+ 并发 / 误报<5% / 漏报<1%

> "| 检查延迟 | P99 < 500ms（不含 LLM 评估） | 并发检查 | 支持 50+ 并发 | 误报率 | < 5% | 漏报率 | < 1% |" — spec §8

**Verdict:** undecidable · confidence: high
**What this demands:** 可度量的性能/准确率指标达标。
**Where enforcement lives:** 无任何基准测试、压测脚本或指标埋点针对门禁检查(quality 模块无 Micrometer 自定义指标;application.yml 无 metrics 配置)。静态可判断的仅有:当前两规则为纯内存子串/旗标判断,单次 evaluate 的耗时主要在 `selectList(null)` 全表扫描 + 每失败一次 issue insert(QualityOrchestrator.java:44, 81)——同步事务内逐条 insert 在高失败率下会放大延迟,但无 LLM 调用,P99<500ms 大概率可满足;误报/漏报率因规则实质检测能力缺失(REQ-10)而无意义可评。
**Paths walked:** 不适用(无可执行验证)。
**Searched:** `Benchmark|jmh|Gatling|k6` 全仓库 → 0;quality 模块 `Timer|Counter|@Timed` → 0。
**How the verdict was reached:** 指标类需求无实现物也无度量手段,静态核查无法裁决 → undecidable(非 absent,因为 NFR 本身不要求代码构件,只要求达标)。
**Severity: Low**

---

## 反向差距(模块存在、spec 未提及的重要行为)

1. **工具审批人工闭环子系统**(约占模块一半代码):ApprovalTicket / ToolApprovalService / ApprovalRequestConsumer(:28-58)/ ApprovalEscalationService / ApprovalDecisionValidator / ApprovalWorkflowBridge / EscalationPolicyService + Flyway V2026_05_10_1。engine 的 GATE_BLOCKED→PAUSED 审批恢复流(ApprovalDeferredCreatedConsumer.java:84-97)对接的是它,而非本 spec 的质量门禁——spec §6 图中"人工确认后继续"的基础设施实际长在审批域。
2. **审计事件投影与防篡改**:AuditEventConsumer(SHA-256 content_hash,AuditEventConsumer.java:94, 114-123)+ AuditIntegrityJob + ComplianceReportController(/quality/compliance/*)。
3. **安全策略 CRUD**(SecurityPolicyController/SfSecurityPolicy)与**评审记录 CRUD**(ReviewController/SfReviewRecord)。
4. **收件箱幂等去重**(InboxDeduplicationService + sf_processed_event)。
5. **门禁生命周期死代码**:activateGate/deactivateGate/deprecateGate/listActiveGates/getGateSummary(QualityGateServiceImpl.java:76-154)未被任何 Controller/消费者引用,仅测试可达。
6. **QualityIssue CRUD API**(/quality/issues)— spec 只定义了 issue 的产生,未定义其管理 API。
7. **sf.quality 队列的孤儿消费者**:task 模块 QualityEventConsumer(QualityEventConsumer.java:46,类注释仍是大段 TODO)有队列绑定但全仓库无生产者。
8. **接口签名噪声**:`checkQualityGate(executionId, gateName)` 与 `evaluateGate` 的 gateName 参数在 evaluate() 内被完全忽略——evaluate 始终评估全部 gate(QualityOrchestrator.java:91-97, 44)。

## Open questions / 文档漂移记录

- spec frontmatter `status: 草稿` vs 核查任务声称"已批准"——以文件为准记录漂移。
- 任务背景称"CHANGELOG 已知 quality 模块无测试覆盖"已过时:模块现有 20+ 测试类(含 QualityOrchestratorTest、SecurityScanRuleTest、SpecComplianceRuleTest、QualityGateServiceImplTest),核心编排逻辑的单测覆盖实际良好——但全部为 mock 单测,无法暴露 REQ-14 的 DDL/实体类型冲突。
- sf_quality_gate.status 类型冲突(VARCHAR DDL vs Integer 实体)的真实运行时表现未经启动验证;evaluateGate 按 status=1 查询对 DDL 默认值 'ACTIVE' 永不命中(QualityGateServiceImpl.java:122 vs SQL:139)为静态可证。
- sf_quality_issue 的 severity 注释(SQL:150 `LOW/MEDIUM/HIGH/CRITICAL`)与代码一致,说明 CRITICAL/HIGH/MEDIUM/LOW 体系是有意设计——spec §3.3/§4.1 的 PASS/WARN/BLOCK/FAIL 等级体系属于 spec 与实现的系统性分歧,建议修 spec 或补处置映射层。

## 反驳复核

> 独立复核(2026-08-29,第二复核人)。范围:7 条 severity ∈ {Critical, High} 且 verdict ∈ {contradicted, partial, absent} 的分歧条目,逐条尝试推翻。
> **规格状态核实**:规格文件 `docs/specs/2026-04-30-v1.0-quality-gate.md` frontmatter 第 5 行确为 `status: 草稿`,正文抬头(第 14 行)亦标注"状态: 草稿"——报告关于文档漂移的记录属实。草稿状态影响该规格的约束效力(缺口整改优先级可按草稿对待),但不改变"规格所载要求与代码现状不符"的事实认定,故各条事实裁定不受影响。
> **复核方法**:重读全部被引用代码行;另寻原作者可能遗漏的接线点(含反射调用、Outbox 动态 topic、web/task 模块对 quality 的 pom 依赖与组件扫描范围、Flyway 迁移、种子数据);交叉验证规格段落。
> **补充排查结论**(适用于多条):① 全仓库无任何 `Class.forName`/反射调用指向 quality 类(仅引擎 Docker 沙箱适配器用于 docker-java,无关);② `schemaplexai-task` 与 `schemaplexai-web` 的 pom 虽依赖 `schemaplexai-quality`,但两者 `scanBasePackages` 分别仅为 `com.schemaplexai.task` / `com.schemaplexai.web`(SchemaPlexaiTaskApplication / SchemaPlexaiWebApplication),QualityOrchestrator 等门禁 Bean 不会在 task/web 运行时实例化;③ 引擎 OutboxPublisher 的动态 topic 仅 `execution.events`/`approval.requests(.deferred)` 且发往 `execution_events` 交换器,不触达 `sf.exchange` 的 `sf.quality` 路由;④ 全仓库 12 处 `convertAndSend` 调用点逐一核查,无一使用 `RK_QUALITY`;⑤ quality 模块 Flyway 仅 2 个迁移(`processed_event`、`approval_ticket`),全仓库 SQL 仅 `03-init-schema-others.sql:134` 一处定义 `sf_quality_gate`,无修正迁移。

| REQ | 原判 | 裁定 | 理由(一行) | 证据 |
|-----|------|------|------------|------|
| REQ-01 | absent · Critical | 维持 | 复核穷举进程内/HTTP/跨运行时/Outbox/反射五类路径,均无 evaluate() 生产调用方;task 与 web 模块虽以 pom 依赖 quality,但各自 `scanBasePackages` 不含 quality 包,门禁 Bean 不随其启动;`sf.quality` 无生产者,其唯一消费者最终委托 `UnsupportedQualityEventRequestHandler.handle()` 直接抛"not implemented"(即消息到达也不会触发检查) | QualityOrchestrator.java:43;SchemaPlexaiTaskApplication `scanBasePackages={"com.schemaplexai.task"}`;SchemaPlexaiWebApplication `scanBasePackages={"com.schemaplexai.web"}`;UnsupportedQualityEventRequestHandler.java:14-18;OutboxPublisher.java:38,67-71;ApprovalRequestProducer.java:34,37 |
| REQ-02 | absent · High | 维持 | 复核未发现任何处置映射层:全仓库 `QUALITY_GATE_BLOCKED` 仅 2 处定义、0 处使用;`QualityCheckResult` 仅 `passed` 布尔 + CRITICAL/HIGH/MEDIUM/LOW 字符串,无 PASS/WARN/BLOCK/FAIL 处置枚举;`evaluate()` 对失败仅落 `sf_quality_issue` 后返回,不驱动暂停/终止 | ResultCode.java:44;PauseReason.java:6;QualityCheckResult.java:12-14;QualityOrchestrator.java:73-82 |
| REQ-09 | partial · High | 维持 | SecurityScanRule 逐行复核与原引文一致:实质检测仅 `contains("password=")`/`contains("secret=")` 两个大小写敏感子串,其余为证据旗标校验且全仓库无旗标生产者;quality 模块亦不使用 PiiRedactor/SecretMasker(模块内 0 引用),无被遗漏的扫描能力 | SecurityScanRule.java:27-50;quality 模块内 `PiiRedactor|SecretMasker` grep 0 命中 |
| REQ-10 | partial · High | 维持 | 规则类全文复核确认六检测项中五项无任何实现(无 `api[_-]?key`、PII、SQL 注入、XSS 模式),等级映射为 CRITICAL 而非 spec 的 BLOCK;且生产调用路径一律传 `Map.of()` 空 metadata → 规则恒走"缺证据 fail"分支,检测项不存在可运行的命中或放行路径 | SecurityScanRule.java:29-31,50;QualityOrchestrator.java:94,101;QualityGateServiceImpl.java:126 |
| REQ-14 | partial · High | 维持 | 类型冲突真实存在且独立复核加强:除 `sf_quality_gate` 外,`sf_quality_issue.status` 同为 DDL `VARCHAR(32) DEFAULT 'OPEN'` vs 实体 `Integer`,而 `evaluate()` 落库 `issue.setStatus(0)`——检查一旦命中失败,PG 上 issue 插入同样因 int4→varchar 类型错(42804)失败;无任何迁移脚本修正两表;原报告"未运行验证"的保留意见继续有效 | 03-init-schema-others.sql:139,152;SfQualityGate.java:15;SfQualityIssue.java:17;QualityOrchestrator.java:80;QualityGateServiceImpl.java:29-31,122;quality 模块 db/migration/ 仅 2 文件均不涉及门禁表 |
| REQ-15 | absent · High | 维持 | 复核扩大搜索至全部 5 个初始化脚本(含 009/04)、Flyway 迁移、`CommandLineRunner`/`ApplicationRunner` 与 yml 配置:均无默认门禁种子或内建缺省;空表时 `evaluate()` 直接返回 `allPassed=true` 的空通过亦复核属实 | 全仓库 `INSERT INTO sf_quality_gate` 0 命中;`SECURITY_SCAN|SPEC_COMPLIANCE|OUTPUT_FORMAT` 于 docker/ 0 命中;quality 模块无 Runner 类;QualityOrchestrator.java:44-48,86 |
| REQ-18 | absent · Critical | 维持 | 状态机出口逐一复核:OBSERVATION 仅转 COMPLETED/REFLECTING;REFLECTING 仅用引擎内部 GuardrailsEngine + LLM 自评,且反思异常时转 COMPLETED 放行(复核属实);CompletedStateHandler 仅落库完成时间,无任何后置检查钩子;引擎主代码对 `com.schemaplexai.quality` 零 import | ObservationStateHandler.java:34-57;ReflectingStateHandler.java:71-116;CompletedStateHandler.java:19-24 |

**复核结论**:7/7 条维持,0 条推翻。原分析的搜索覆盖面经独立复现,未发现被遗漏的生产接线点;规格确为草稿(已按事实记录),各条缺口裁定作为"代码现状与规格文本的差异描述"全部成立。
