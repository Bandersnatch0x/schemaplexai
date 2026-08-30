# Spec 合规核查 — v1.0 Release Readiness + Test Fixes & Coverage

- 核查日期：2026-08-29
- 核查对象：
  - 文档 A：`docs/specs/2026-05-05-v1.0-release-readiness.md`（status: approved）
  - 文档 B：`docs/specs/2026-05-05-v1.0-test-fixes-and-coverage.md`（status: approved）
- 核查方式：只读静态核查（文件枚举、文本搜索计数、配置内容、`target/` 下 surefire / JaCoCo 构建产物解析、git 状态检查）。**未运行** `mvn test` / `npm test` / 任何构建。
- 关键方法说明：仓库 `target/surefire-reports/`（最新批次 2026-07-01/02）与 `target/site/jacoco/jacoco.csv` 是"最后一次有记录的测试/覆盖率运行"的物证，本报告以它作为实态基准，并与静态 `@Test` 计数（全仓 4,119 个 `@Test` + 2 个 `@ParameterizedTest`）交叉核对。

## 裁决统计

| Verdict | 数量 | REQ |
|---|---|---|
| implemented | 12 | REQ-01, 02, 03, 04, 07, 09, 11, 12, 13, 14, 15, 17 |
| partial | 2 | REQ-16, REQ-19 |
| contradicted | 7 | REQ-05, REQ-06, REQ-10, REQ-18, REQ-21, REQ-22, REQ-23 |
| undecidable | 2 | REQ-08, REQ-20 |
| 合计 | 23 | |

核心结论：**测试基础设施与两份规格列出的具体修复项基本落实（12 项 implemented），但两条发布门禁被仓库内最新构建产物证伪**——最近一次有记录的运行（2026-07-02）中 agent-engine 存在 2 个未修复失败 + 1 个错误（REQ-05/REQ-18），且 6 个目标模块中 model（21.0% 指令覆盖，2026-07-02 JaCoCo 产物）、dao（最后记录 44.6%）低于 80% 门禁（REQ-06）。CHANGELOG 的多项测试数量与前端覆盖率宣称与仓库产物不符（REQ-21/22/23）。

---

## REQ-01 — Stream 1：common/model/dao 基础模块测试 + 测试依赖（来源：2026-05-05-v1.0-release-readiness.md）

> "Stream 1: Test Infrastructure + Base Module Tests … Files to create/modify: `schemaplexai-common/src/test/java/**/*Test.java`、`schemaplexai-model/src/test/java/**/*Test.java`、`schemaplexai-dao/src/test/java/**/*Test.java`、`pom.xml` (add test dependencies if missing) … Scope: Unit tests for Result, ResultCode, BaseException, PageParam / BaseEntity, PageResult / TenantContextHolder / BaseMapperX" — §2 Stream 1

**Verdict:** implemented · confidence: high

**What this demands:** 三个基础模块存在 `src/test` 测试套件，点名类逐一有测试，pom 具备测试依赖。

**Where enforcement lives / Evidence:**
- common：16 个测试文件 / 129 个 `@Test`（`find schemaplexai-common/src/test -name "*.java" | wc -l` = 16）。点名类全覆盖：`result/ResultTest.java`、`result/ResultCodeTest.java`、`exception/BaseExceptionTest.java`、`page/PageParamTest.java`、`context/TenantContextHolderTest.java`。
- model：7 个测试文件 / 27 个 `@Test`，含 `dto/PageResultTest.java`、`entity/BaseEntityTest.java`。
- dao：6 个测试文件 / 21 个 `@Test`，含 `mapper/BaseMapperXTest.java`、`config/TenantLineInterceptorTest.java`。
- 依赖：`schemaplexai-common/pom.xml`、`schemaplexai-model/pom.xml`、`schemaplexai-dao/pom.xml`、`schemaplexai-gateway/pom.xml`、`schemaplexai-system/pom.xml`、`schemaplexai-agent-engine/pom.xml` 均含 `spring-boot-starter-test`（scope test）；dao 另含 `mybatis-spring-boot-starter-test` 3.0.3。
- 最近记录运行（`target/surefire-reports`，2026-07-02/07-01）：common 129 passed / model 27 passed / dao 20 passed，0 失败。

**How the verdict was reached:** 点名 8 个类全部有对应测试文件且最近记录运行全绿；不判 stronger-than-spec 是因为范围恰好对齐清单（额外的 CommonConstants/GlobalExceptionHandler 等测试不构成规格偏离）。

---

## REQ-02 — Stream 2：agent-engine 测试（状态机/工具/预算/循环检测/状态处理器）（来源：2026-05-05-v1.0-release-readiness.md）

> "Stream 2: Agent Engine Tests … Scope: State machine transitions (all 13 states) / ToolRegistry … / ToolSafetyGuard checks … / ToolExecutionRecorder persistence / TokenBudget operations / AgentLoopDetectionService detection / SecurityPolicyLoader caching / RetryingStateHandler … / ThinkingStateHandler … / ToolCallingStateHandler …" — §2 Stream 2

**Verdict:** implemented · confidence: high

**What this demands:** agent-engine 各核心组件有测试；状态机全部状态有转换测试。

**Where enforcement lives / Evidence:**
- 规模：`schemaplexai-agent-engine/src/test` 共 184 个测试 Java 文件、1,845 个 `@Test`（grep 计数）；最近记录运行 1,847 tests（2026-07-02，含失败，见 REQ-05）。
- 点名项逐一存在：`state/AgentStateMachineTest.java`（10 tests）、`tool/ToolRegistryTest.java` + `tool/registry/ToolRegistryComponentTest.java`、`tool/ToolSafetyGuardTest.java` + `ToolSafetyGuardObfuscationTest.java`、`tool/ToolExecutionRecorderTest.java`（+`TracedToolExecutionRecorderTest.java`）、`admission/TokenBudgetTest.java`（+`integration/TokenBudgetEnforcementTest.java`）、`loop/AgentLoopDetectionServiceTest.java`、`config/SecurityPolicyLoaderTest.java`、`state/RetryingStateHandlerTest.java`、`state/ThinkingStateHandlerTest.java`（+`ThinkingStateHandlerSkillInjectionTest.java`）、`state/ToolCallingStateHandlerTest.java`。
- 状态覆盖：主代码 15 个 `*StateHandler.java` 全部有对应 `*StateHandlerTest.java`（`ls` 对比 main/test state 目录）；另有 `AgentExecutionStateTest.java`。

**How the verdict was reached:** 规格写"13 states"，当前 `AgentExecutionState` 枚举已演化为 18 态（`AgentExecutionState.java:4-21`），且每个状态处理器都有测试——超出规格口径，但全部点名组件均在，故判 implemented 而非 stronger-than-spec（状态数增长记入反向差距）。测试运行红的问题归入 REQ-05，不在本条重复计罚。

---

## REQ-03 — Stream 3：gateway + system 测试（来源：2026-05-05-v1.0-release-readiness.md）

> "Stream 3: Gateway + System Tests … Scope: JWT filter validation / Tenant filter extraction / Rate limiter behavior / Auth service login/JWT generation / Tenant/User/Role CRUD" — §2 Stream 3

**Verdict:** implemented · confidence: high

**What this demands:** gateway 三个过滤器与 system 认证/租户/用户/角色均有测试。

**Where enforcement lives / Evidence:**
- gateway（8 个测试文件 / 49 个 `@Test`）：`filter/JwtAuthFilterTest.java`、`filter/TenantResolveFilterTest.java`、`filter/RateLimitFilterTest.java`（另有 LoggingFilter、TracePropagationFilter、GatewayConfig、GatewayIntegration、GatewayApplication 测试）。
- system（16 个测试文件 / 161 个 `@Test`）：`service/AuthServiceTest.java`、`security/JwtTokenProviderTest.java`、`service/TenantServiceTest.java`、`service/UserServiceTest.java`、`service/RoleServiceTest.java`（另含 RbacService、PermissionService、PermissionEvaluator、SecurityConfig 等）。
- 最近记录运行（2026-07-01/07-02）：gateway 47 passed（0 失败，剔除 1 个已删除测试类的过期报告，见 REQ-05）、system 161 passed。

**How the verdict was reached:** 五个点名范围全部命中且最近记录运行全绿。

---

## REQ-04 — Stream 4：P0 修复（DB 驱动/重复实体/重复启动类/JWT 硬编码等）（来源：2026-05-05-v1.0-release-readiness.md + CODE_REVIEW_REPORT.md）

> "Stream 4: P0 Issue Fixes … Files to modify: Per CODE_REVIEW_REPORT.md findings … Scope: Fix DB driver mismatch / Fix duplicate entities / Fix duplicate main class / Fix JWT secret hardcoding / Fix all other P0 issues" — §2 Stream 4

**Verdict:** implemented · confidence: high

**What this demands:** CODE_REVIEW_REPORT.md 所列全部 P0（报告称 9 个，实际编号 P0-001..008 共 8 个）及 JWT secret 硬编码（实为 P1-001）已修复。

**Where enforcement lives / Evidence:**
- P0-001（MySQL→PostgreSQL）：全部 11 个含数据源的 `application.yml` 均为 `driver-class-name: org.postgresql.Driver` + `jdbc:postgresql://`（逐文件 grep 验证；无任何 `com.mysql.cj.jdbc.Driver` 残留）。
- P0-002（连接凭据统一）：各模块改为 `username: ${DB_USERNAME:?required}` / `password: ${DB_PASSWORD:?required}`（如 `schemaplexai-system/src/main/resources/application.yml` datasource 段）；RabbitMQ 统一 `${RABBITMQ_USERNAME:?required}` / `${RABBITMQ_PASSWORD:?required}`（agent-engine、task 的 application.yml）；`grep ":sf_password}\|:root}\|:guest}"` 于全部 `src/main/resources/*.yml` = 0 命中。
- P0-003（AgentStateMachine 构造冲突）：`AgentStateMachine.java:29-30` 为显式 `@Autowired` 构造函数，类上无 `@RequiredArgsConstructor`。
- P0-004（JwtAuthFilter 二次 mutate 失效）：`JwtAuthFilter.java:90-102` 改为单一 `ServerHttpRequest.Builder`，tenant header 在 `build()` 前追加。
- P0-005（两套实体）：全仓 `find`（非 Sf 前缀的 `User.java/Tenant.java/Role.java/Permission.java/Menu.java`）= 0 命中；仅存 `schemaplexai-system/.../entity/SfUser.java`、`SfTenant.java`、`SfRole.java` 等。
- P0-006（双启动类）：`schemaplexai-system/src/main` 仅 `SchemaPlexaiSystemApplication.java` 1 个 `*Application.java`。
- P0-007（TenantLineInterceptor 返回类型）：`TenantLineInterceptor.java:14-19` 返回 `StringValue(tenantId)`，与 `BaseEntity.java:22 private String tenantId` 类型一致（报告两种分支中的 VARCHAR/StringValue 分支）。
- P0-008（RabbitMQ ACK）：agent-engine `application.yml` `listener.simple.acknowledge-mode: manual`。
- JWT secret（规格点名的"hardcoding"，即 P1-001）：4 处配置（agent-engine:69、gateway:45、system:44、web:18）均为 `secret: ${JWT_SECRET}` 无默认值；另有 `schemaplexai-common/.../security/JwtSecretStartupValidator.java`（启动期校验）+ 测试。

**How the verdict was reached:** 8 个 P0 逐条验证均落在"已修复"状态；不判 stronger-than-spec 因 JWT 启动校验器只是修复手段的强化。源报告"9 个 P0"与编号仅到 008 的内部矛盾见 Open questions。

---

## REQ-05 — AC：`mvn clean test` 通过（0 failures）（来源：2026-05-05-v1.0-release-readiness.md）

> "- [ ] `mvn clean test` passes (0 failures)" — §3 Acceptance Criteria

**Verdict:** contradicted · confidence: high（就"最后记录运行红"而言）；若主张"其后已修复"则需新运行证据，当前仓库无
**Severity:** High — 这是发布门禁之首；红的测试基线意味着任何"已就绪"宣称都会误导发布决策。

**What this demands:** 全部模块测试全绿。

**Where enforcement lives / Evidence:**
- `schemaplexai-agent-engine/target/surefire-reports/com.schemaplexai.agent.engine.orchestrator.AgentRuntimeOrchestratorTest.txt`（2026-07-02 09:53）：`Tests run: 13, Failures: 2` — `shouldNotForceCompletionOnMaxIterationsWhenPaused` 与 `shouldPauseWhenRedisPauseKeyExists` 均因 `stateMachine.transition(PAUSED, …)` mock 期望不匹配失败。
- `.../com.schemaplexai.agent.engine.lifecycle.AgentExecutionLifecycleServiceTest.txt`（2026-07-02 09:54）：`Tests run: 1, Errors: 1` — `pauseExecutionSetsRedisAndTransitionsState` 抛 `IllegalArgumentException: tenantId must not be blank`（`TenantRedisKeyResolver.tenantKey`）。
- 聚合：最近记录运行 16 个模块共约 4,129 tests，agent-engine 为 `tests=1847 failures=2 errors=1 skipped=4`。
- 时效交叉验证（git）：`git status --short` 显示 `AgentRuntimeOrchestrator.java`（main）为未提交修改，对应测试文件自 2026-07-01 18:52 后未变——失败运行之后**无任何对该失败的修复痕迹**；`AgentExecutionLifecycleServiceTest.java` 在失败运行后 3 分钟（09:57:47）被改动（未提交），可能已修但**无重跑产物**，记未验证。
- 其余 15 模块最近记录运行全绿（含 gateway 47 passed——gateway 唯一的 error 报告 `GatewayConfigIntegrationTest.txt` dated 2026-05-09，对应测试类已被删除，属过期残留）。

**Searched:** `grep -l "Failures: [1-9]\|Errors: [1-9]" schemaplexai-*/target/surefire-reports/*.txt` → 命中 3 个文件（上述）；`find schemaplexai-gateway/src/test -name GatewayConfigIntegrationTest.java` → 0 命中（已删除）。

**How the verdict was reached:** 不判 undecidable：虽无法现场跑测试，但仓库自带最近一次运行的 surefire 产物，且失败点的源码/测试在失败后无修复性变更（git 证据）。不判 partial：0-failures 是二值门禁，存在 2 个失败即为不满足。
**验证命令（复核所需）:** `mvn clean test`（全仓）；快速复核 `mvn test -pl schemaplexai-agent-engine -Dtest=AgentRuntimeOrchestratorTest`。

---

## REQ-06 — AC：common/model/dao/agent-engine/gateway/system 覆盖率 ≥ 80%（来源：2026-05-05-v1.0-release-readiness.md）

> "- [ ] Coverage >= 80% for: common, model, dao, agent-engine, gateway, system" — §3 Acceptance Criteria

**Verdict:** contradicted · confidence: medium（物证为 2026-05/07 的 JaCoCo 产物，无更新证据；按最新可得证据判）
**Severity:** High — 覆盖率是发布门禁，且根 pom 把它设为硬门槛（见下），虚报即误导发布。

**What this demands:** 6 个模块覆盖率（按执行口径）≥ 80%。

**Where enforcement lives / Evidence:**
- 门槛执行点：`pom.xml:307-322` JaCoCo check 规则 `INSTRUCTION COVEREDRATIO minimum 0.80` + `BRANCH 0.60`（common/model/dao/gateway/system/agent-engine 继承根配置）；`.github/workflows/ci.yml:117` 对全部 16 模块执行 `mvn jacoco:check`。
- 仓库内最新 JaCoCo 产物（`target/site/jacoco/jacoco.csv` 解析）：

  | 模块 | 指令覆盖 | 行覆盖 | 产物日期 |
  |---|---|---|---|
  | common | 83.9% ✓ | 81.8% | 2026-07-02 |
  | gateway | 94.6% ✓ | 92.8% | 2026-05-09 |
  | system | 88.6% ✓ | 89.5% | 2026-05-09 |
  | model | **21.0% ✗** | 66.7% | 2026-07-02 |
  | dao | 最后记录 44.6%（`docs/COVERAGE.md` 2026-05-06 基线）✗ | — | 无新报告（仅 `jacoco.exec`） |
  | agent-engine | 最后记录 92.9%（`docs/COVERAGE.md`）✓ | — | 无新 CSV（仅 2026-07-02 `jacoco.exec`） |

- 即：6 个门禁模块中 **2 个（model、dao）按最新可得证据不达标**；model 的 21.0% 是 2026-07-02 的较新产物，直接击穿 0.80 门槛——按此产物，CI 的 `mvn jacoco:check` 在 model 上必失败。
- 注意 `docs/COVERAGE.md:6-15` 自记"2026-05-06 基线已过期、需重跑 `mvn clean verify`"，且其后未再刷新百分比表——门禁所依赖的度量本身处于失修状态。

**Searched:** `find . -name "jacoco.csv" -o -name "jacoco.exec"`（产物清单见上）；`ls schemaplexai-dao/target/site/jacoco/` → 空（无报告目录）。

**How the verdict was reached:** 不判 undecidable——虽覆盖率需运行才产生，但仓库现存产物即为最新记录证据，model 21.0% 直接矛盾；不判 partial——门禁是 6 模块合取，2/6 证伪即整体不成立。若有人主张 7 月后覆盖率已提升，仓库内无任何更新的产物或记录支持。
**验证命令:** `mvn clean verify`（生成全模块 JaCoCo 报告并执行 check 规则）。

---

## REQ-07 — AC：所有 P0 问题已解决（来源：2026-05-05-v1.0-release-readiness.md）

> "- [ ] All P0 issues resolved" — §3 Acceptance Criteria

**Verdict:** implemented · confidence: high

**Where enforcement lives / Evidence:** 同 REQ-04 的逐条证据（P0-001..008 全部验证为已修复状态）。

**How the verdict was reached:** 与 REQ-04 共用证据；该条是结果态宣称，逐条核实均成立。源报告"9 个 P0"与编号仅 8 个的出入记入 Open questions（若有未编号的第 9 个，无法静态枚举）。

---

## REQ-08 — AC：无 CRITICAL 安全发现（来源：2026-05-05-v1.0-release-readiness.md）

> "- [ ] No CRITICAL security findings" — §3 Acceptance Criteria

**Verdict:** undecidable · confidence: low
**Severity:** 若虚报为 High；当前静态证据不足以定论。

**What this demands:** 安全扫描（依赖/镜像/秘密）无 CRITICAL。

**Where enforcement lives / Evidence:**
- CI 存在三轨扫描：`ci.yml:10-36` Trivy fs（severity CRITICAL,HIGH，仅产出 SARIF，未设 fail 条件）、`ci.yml:38-63` OWASP dependency-check（`-DfailBuildOnCVSS=9` 但 `continue-on-error: true` → 实际不阻塞）、`ci.yml:65-83` Gitleaks。
- Day-0（2026-05-08，`docs/reviews/v1-readiness/MASTER.md` §2.2）记录的 6 个安全 P0 的当前抽查：docker-compose 明文密码已改 `${...:?required}`（`docker/docker-compose.yml:11,71,173`）；Grafana admin/admin 已改 `${GRAFANA_ADMIN_PASSWORD:?required}`；CORS 由 `*` 收紧为 `localhost:5173/3000`（gateway application.yml:17-19）；`jdk21.zip` 已不在仓库（`find . -name jdk21.zip` = 0）；yml 无弱默认口令。
- 但仓库内**没有任何一次安全扫描的结果产物**（无 trivy/dependency-check/gitleaks 报告存档）。

**Searched:** `find . -name "*trivy*" -o -name "dependency-check-report*" -o -name "gitleaks*"`（仓库内无报告产物）。

**How the verdict was reached:** "无 CRITICAL 发现"只能由扫描运行判定；静态抽查只能证明已知 Day-0 问题多已修复，不能替代扫描结论。
**验证命令:** `trivy fs --severity CRITICAL,HIGH .`；`mvn org.owasp:dependency-check-maven:check -DfailBuildOnCVSS=9`；gitleaks 全历史扫描。另注意：即便扫出 CRITICAL，dependency-check 的 `continue-on-error: true` 也不会阻断 CI——门禁本身不硬。

---

## REQ-09 — 测试框架：spring-boot-starter-test 就位（来源：2026-05-05-v1.0-release-readiness.md）

> "Already in pom.xml via spring-boot-starter-test … Includes: JUnit 5, Mockito, AssertJ, Hamcrest, Spring Test." — §4 Test Framework

**Verdict:** implemented · confidence: high

**Where enforcement lives / Evidence:** 抽查的 6 个模块（common、model、dao、gateway、system、agent-engine）`pom.xml` 均声明 `spring-boot-starter-test`（scope test）；根 `pom.xml:208-238` 另在 dependencyManagement 提供 Testcontainers 1.19.7（postgresql/redis/rabbitmq/junit-jupiter）。实际测试代码大量使用 JUnit 5 + Mockito（4,119 个 `@Test`），与宣称一致。

**How the verdict was reached:** 宣称与实态直接一致，无偏差。

---

## REQ-10 — 总述宣称：修复 agent-engine 全部 21 个既有失败/错误（来源：2026-05-05-v1.0-test-fixes-and-coverage.md）

> "Fix all 21 pre-existing test failures/errors in schemaplexai-agent-engine and add JaCoCo coverage plugin. No architectural changes — all fixes are test corrections or minor source improvements." — Overview

**Verdict:** contradicted · confidence: high
**Severity:** Medium-High — 该宣称是"修复完成"的汇总口径；最新记录运行仍红（2 failures + 1 error），说明"全部修复"不成立（无论当前失败是否属于原 21 个）。

**Where enforcement lives / Evidence:** 见 REQ-05：2026-07-02 记录运行中 `AgentRuntimeOrchestratorTest`（2 failures，失败后源码/测试无修复变更）与 `AgentExecutionLifecycleServiceTest`（1 error）仍失败。各具体修复组（Group 1-6）的落地情况见 REQ-11..16，多数已实现——但汇总宣称被产物证伪。

**Searched:** 同 REQ-05 的 surefire 检索。

**How the verdict was reached:** 不判 partial：具体修复组大多落地，但"全部 21 个失败已修复"是一个整体事实宣称，而最近记录运行存在未修复失败；不判 undecidable：失败物证在仓库内。

---

## REQ-11 — Group 1：补齐 @Mock 声明（来源：2026-05-05-v1.0-test-fixes-and-coverage.md）

> "Add missing `@Mock` fields: ThinkingStateHandlerTest: `@Mock AgentLoopDetectionService loopDetection`; ToolCallingStateHandlerTest: `@Mock ToolRegistry toolRegistry`, `@Mock ToolSandbox sandbox`, `@Mock AgentLoopDetectionService loopDetection`, `@Mock SecurityPolicyLoader securityPolicyLoader`" — Fix Groups §Group 1

**Verdict:** implemented · confidence: high

**Where enforcement lives / Evidence:**
- `ThinkingStateHandlerTest.java:49-50`：`@Mock private AgentLoopDetectionService loopDetection;`
- `ToolCallingStateHandlerTest.java:37-38`（ToolRegistry）、`40-41`（ToolSandbox）、`46-47`（AgentLoopDetectionService）、`52-53`（SecurityPolicyLoader），全部为 `@Mock` 字段。

**How the verdict was reached:** 点名 5 个字段逐一存在，字面吻合。

---

## REQ-12 — Group 2：THOUGHT_PATTERN 正则前瞻修复（来源：2026-05-05-v1.0-test-fixes-and-coverage.md）

> "Fix `THOUGHT_PATTERN` lookahead to include `Thought` as boundary: `(?=\n(?:Thought|Action|Observation|Final\s+Answer)|$)`" — Fix Groups §Group 2

**Verdict:** implemented · confidence: high

**Where enforcement lives / Evidence:** `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/extractor/FinalAnswerExtractor.java:22-23`：
`Pattern.compile("Thought\\s*:\\s*(.+?)(?=\\n(?:Thought|Action|Observation|Final\\s+Answer)|$)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)` — 前瞻分支与规格字面一致（含 `Thought` 与 `Final\s+Answer` 边界）。

**How the verdict was reached:** 字面比对完全一致。

---

## REQ-13 — Group 3：不可变列表修复（来源：2026-05-05-v1.0-test-fixes-and-coverage.md）

> "Change constructor to wrap input list in `new ArrayList<>(recoveryStrategies)` before calling `.add()`." — Fix Groups §Group 3

**Verdict:** implemented · confidence: high

**Where enforcement lives / Evidence:** `state/ExceptionHandlingStateHandler.java:39-44`：构造函数 `this.recoveryStrategies = new ArrayList<>(recoveryStrategies);`，随后对空列表 `add` 两个默认策略——先复制后 `.add()`，与规格一致。

**How the verdict was reached:** 代码字面吻合。

---

## REQ-14 — Group 4：ContainerToolSandbox 校验顺序（来源：2026-05-05-v1.0-test-fixes-and-coverage.md）

> "Move `validate()` call before whitelist check in `execute()` method." — Fix Groups §Group 4

**Verdict:** implemented · confidence: high

**Where enforcement lives / Evidence:** `tool/ContainerToolSandbox.java:28-44`：`execute()` 先 `validate(toolCall)`（:30，非法即抛 `INVALID_ARGUMENT`），后 `whitelist.isAllowed`（:39）——validate 在白名单检查之前。

**How the verdict was reached:** 行序直接验证。

---

## REQ-15 — Group 5：测试期望修复（ExceptionHandling stub / MemoryStrategy 估算）（来源：2026-05-05-v1.0-test-fixes-and-coverage.md）

> "ExceptionHandlingStateHandlerTest: Stub `stateMachine.getCurrentState(1L)` to return non-null state; MemoryStrategyTest: Adjust expectations to match content-length-based token estimation" — Fix Groups §Group 5

**Verdict:** implemented · confidence: high

**Where enforcement lives / Evidence:**
- `state/ExceptionHandlingStateHandlerTest.java:48`：`lenient().when(stateMachine.getCurrentState(1L)).thenReturn(AgentExecutionState.THINKING);`（非空桩，吻合）。
- `memory/MemoryStrategyTest.java:63` 注释 "Budget allows roughly 1 message (content-length/4 estimation)"；实现侧 `SlidingWindowStrategy.java:92` 与 `SummarizationStrategy.java:116` 均为 `Math.max(1, message.getContent().length() / 4)`——期望与实现口径一致。

**How the verdict was reached:** 两处修复均落地且与实现一致。

---

## REQ-16 — Group 6：Spring 上下文修复（@SpringBootTest 排除 Redis/RabbitMQ）（来源：2026-05-05-v1.0-test-fixes-and-coverage.md）

> "Add `@SpringBootTest(properties = {...})` to exclude Redis and RabbitMQ auto-configuration." — Fix Groups §Group 6（Files: `ObservabilityRecorderTest.java`, `AgentRuntimeOrchestratorIntegrationTest.java`）

**Verdict:** partial · confidence: high

**What this demands:** 两个文件通过 `@SpringBootTest(properties=...)` 排除 Redis/RabbitMQ 自动装配，使上下文测试可独立运行。

**Where enforcement lives / Evidence:**
- 两文件均**不含** `@SpringBootTest`：`ObservabilityRecorderTest.java:19` 与 `AgentRuntimeOrchestratorIntegrationTest.java:23` 均为 `@ExtendWith(MockitoExtension.class)` 纯 Mockito 单测（无任何 Redis/RabbitMQ 上下文）。
- 即：规格指定的机制（@SpringBootTest + exclusion properties）缺失，但机制要解决的**目的**（测试不再依赖/触发 Redis、RabbitMQ 连接）以"去 Spring 化"的替代方式达成。

**Searched:** `grep -n "SpringBootTest|exclude|properties"` 于两文件 → 0 命中。

**How the verdict was reached:** 不判 absent——目标问题确已解决（纯 Mockito 路径下根本不存在 Redis/RabbitMQ 装配）；不判 implemented——字面机制缺失。附带偏差：`AgentRuntimeOrchestratorIntegrationTest` 名为 Integration 实为无 Spring 上下文的单测（命名漂移，记反向差距）。
**验证命令（若需确认目的达成）:** `mvn test -pl schemaplexai-agent-engine -Dtest=ObservabilityRecorderTest,AgentRuntimeOrchestratorIntegrationTest`。

---

## REQ-17 — Group 7：父 pom 加 JaCoCo 插件（80% 目标）（来源：2026-05-05-v1.0-test-fixes-and-coverage.md）

> "Add JaCoCo Maven plugin with 80% line coverage target." — Fix Groups §Group 7（File: parent `pom.xml`）

**Verdict:** implemented · confidence: high（带一处口径偏差说明）

**Where enforcement lives / Evidence:** `pom.xml:276-279`（`<plugins>` 启用 `org.jacoco:jacoco-maven-plugin`）+ `pom.xml:288-326`（pluginManagement：0.8.12，prepare-agent、report@verify、check@verify，规则 `INSTRUCTION COVEREDRATIO minimum 0.80`、`BRANCH 0.60`）。多数模块另设了阶段性降档门槛（如 web 0.20、integration 0.35、agent-config 0.40、context/ops 0.50、quality/spec/task 0.40、admin 0.40——各模块 pom `<build>` 段），属渐进执行策略。

**How the verdict was reached:** 偏差说明：规格写"80% **line** coverage"，实际执行计数器是 **INSTRUCTION**（0.80）而非 LINE。指令覆盖与行覆盖语义近似且通常更严，判 implemented；若按字面计数器苛求则为微偏差。不判 partial 因插件、80% 数值、verify 绑定三要素全部落地。

---

## REQ-18 — AC：`mvn clean test` 全模块 0 失败（来源：2026-05-05-v1.0-test-fixes-and-coverage.md）

> "`mvn clean test` — 0 failures across all modules" — Acceptance Criteria

**Verdict:** contradicted · confidence: high（就最后记录运行而言）
**Severity:** High — 与 REQ-05 同一门禁事实；发布就绪宣称与红测试基线直接冲突。

**Where enforcement lives / Evidence:** 与 REQ-05 完全相同：2026-07-02 记录运行中 agent-engine `AgentRuntimeOrchestratorTest` 2 failures（失败后无修复变更）、`AgentExecutionLifecycleServiceTest` 1 error（其后测试文件有未验证的编辑）。其余 15 模块最近记录运行 0 失败。

**How the verdict was reached:** 二值门禁被物证证伪；与 REQ-05 重复计录以反映两份规格各自的验收条款。
**验证命令:** `mvn clean test`。

---

## REQ-19 — AC：核心模块生成 JaCoCo 报告（来源：2026-05-05-v1.0-test-fixes-and-coverage.md）

> "JaCoCo reports generated for core modules" — Acceptance Criteria

**Verdict:** partial · confidence: high

**Where enforcement lives / Evidence:**
- 已有完整报告（`target/site/jacoco/jacoco.csv`）：admin、common、context、gateway、model、spec、system、workflow 共 8 模块。
- 核心 6 模块中 **agent-engine 与 dao 只有 `jacoco.exec`、无 site 报告**（`ls schemaplexai-agent-engine/target/site/jacoco/` 与 `ls schemaplexai-dao/target/site/jacoco/` 均为空/不存在）——即"报告生成"在 2/6 核心模块上未发生。
- 佐证：`docs/COVERAGE.md:34` 自述部分模块"no JaCoCo reports"。

**How the verdict was reached:** 多数模块（含 4/6 核心模块）有报告 → 非 absent；2/6 核心模块缺报告 → 非 implemented。
**验证命令:** `mvn clean verify -pl schemaplexai-agent-engine,schemaplexai-dao -am`。

---

## REQ-20 — AC：现有通过的测试无回归（来源：2026-05-05-v1.0-test-fixes-and-coverage.md）

> "No regression in currently passing tests" — Acceptance Criteria

**Verdict:** undecidable · confidence: low

**What this demands:** 相对修复前基线，原通过测试仍通过。

**Where enforcement lives / Evidence:** 无修复前的 surefire 基线存档可供对比。可观察事实：当前失败的 3 个测试（`AgentRuntimeOrchestratorTest` ×2、`AgentExecutionLifecycleServiceTest` ×1）不在规格 Group 1-6 的点名文件之列——若它们在修复前通过，则构成回归；但静态无法确立"修复前通过"这一前提（规格只说存在 21 个失败，未列名单）。

**How the verdict was reached:** 缺乏前置基线，既不能证实也不能证伪"无回归"。
**验证命令:** 在修复提交前后分别运行 `mvn test -pl schemaplexai-agent-engine` 对比（`git stash` / checkout 父提交前后各跑一次）。

---

## REQ-21 — CHANGELOG 后端测试数量宣称（来源：CHANGELOG.md [1.0.0] Testing / Release Statistics）

> "1,586 backend tests (agent-engine, 4 skipped for Docker) / 129 backend tests (context) / 111 backend tests (integration) / 63 backend tests (system) / 43 backend tests (web) / 41 backend tests (agent-config) / 31 backend tests (gateway) / 98 backend tests (common, model, dao, task) / 499 backend tests (dao, model, common combined) / Total Backend Tests 2,601" — CHANGELOG.md:80-88, 101

**Verdict:** contradicted · confidence: high
**Severity:** Low（数字陈旧、整体为低报而非虚报就绪；但总量与分项自相矛盾，属发布文档漂移）

**Where enforcement lives / Evidence:** 最近记录运行（2026-07-01/02，`target/surefire-reports` 聚合）实测：

| 模块 | CHANGELOG 宣称 | 记录运行实测 | 静态 @Test 计数 |
|---|---|---|---|
| agent-engine | 1,586 | 1,847 | 1,845 |
| context | 129 | 242 | 241 |
| integration | 111 | 270 | 270 |
| system | 63 | 161 | 161 |
| web | 43 | 140 | 133 |
| agent-config | 41 | 103 | 103 |
| gateway | 31 | 47 | 49 |
| common+model+dao+task | 98 | 342 | 343 |
| dao+model+common | 499 | 176 | 177 |
| **总计** | **2,601** | **≈4,129** | **4,119+2** |

宣称与实测/静态计数全面不符（偏差 30%–180%，远超 ±10% 口径容差）；且 "98 (common+model+dao+task)" 与 "499 (dao+model+common)" 两行互相矛盾（后者不可能大于前者的子集和）。

**Searched:** `grep -r "@Test" schemaplexai-*/src/test --include="*.java" | wc -l` = 4,119；surefire 逐模块聚合见上表。

**How the verdict was reached:** 不判 partial：不是"部分达成"，而是数字整体失真；方向上多为低报（陈旧的发布快照），故严重度记 Low 而非 High。

---

## REQ-22 — CHANGELOG 前端测试数与覆盖率宣称（来源：CHANGELOG.md [1.0.0]）

> "100 frontend tests (vitest) / Frontend Test Coverage (Statements) 78.21% / (Branches) 75.46% / (Functions) 68.21% / (Lines) 78.96%" — CHANGELOG.md:89, 105-108

**Verdict:** contradicted · confidence: medium（覆盖率数字与仓库产物不符；"100 tests"需运行判定精确值，但静态计数亦不支持）
**Severity:** Medium — 覆盖率被**高报**（78.21% vs 记录产物 73.09%），方向上属虚报就绪信号。

**Where enforcement lives / Evidence:**
- 仓库内前端覆盖率产物 `schemaplexai-ui/coverage/`（报告日期 2026-05-08 19:05，与发布同日）：`coverage-final.json` 解析（29 个被覆盖文件）得 **Statements 73.09% / Branches 68.31% / Functions 64.68%**（HTML 首页另示 Lines 74.67%）——四项全部低于 CHANGELOG 宣称（78.21/75.46/68.21/78.96），差距 1.6–7.6 个百分点，超出统计口径容差。
- 测试数量：现存 22 个 `*.test.*` 单元文件 + 3 个 e2e spec；`grep -E "^\s*(it|test)\("` 精确计数单元测试用例 **146**（e2e 另 4 个）——与"100"不符（+46%）。`docs/COVERAGE.md` 更早的前端记录（2026-05-06）为 "73 total, 4 failed"，三者（100/73/146）互不一致。
- 覆盖率报告之后仅新增 3 个测试文件（ChatMemory/Composer/SseViewer，`find -newermt "2026-05-08 19:05"`），不足以解释 5+ 个百分点的跃升；仓库无更新的覆盖率产物。

**Searched:** `find schemaplexai-ui/coverage -type f`；`grep -rE "^[[:space:]]*(it|test)\(" schemaplexai-ui/src --include="*.test.*" | wc -l` = 146。

**How the verdict was reached:** 覆盖率以仓库产物为准判 contradicted；测试数以静态计数+记录运行双证判 contradicted（精确运行值仍需 `npm test -- --run`）。
**验证命令:** `cd schemaplexai-ui && npm test -- --run --coverage`。

---

## REQ-23 — CHANGELOG Known Limitations 宣称（来源：CHANGELOG.md [1.0.0]）

> "Known Limitations: … `schemaplexai-admin` module is a stub (empty) / Some modules (ops, quality, spec, workflow) have no test coverage yet" — CHANGELOG.md:112-113

**Verdict:** contradicted · confidence: high
**Severity:** Low — 反向失真（低报了已有进展），不误导发布门禁，但发布文档与实态脱节。

**Where enforcement lives / Evidence:**
- admin 非空桩：`find schemaplexai-admin/src/main -name "*.java" | wc -l` = **25**（AuditLogController、TenantAdminController、UserAdminController 等）；13 个测试文件 / 111 个 `@Test`，记录运行 111 passed，JaCoCo 产物指令覆盖 81.6%。
- ops/quality/spec/workflow 均有测试：记录运行分别 231 / 275 / 150 / 210 passed（surefire 聚合），静态 `@Test` 231 / 275 / 150 / 210。

**Searched:** `find schemaplexai-admin/src/main -name "*.java"`；`grep -h "Tests run:" schemaplexai-{ops,quality,spec,workflow}/target/surefire-reports/*.txt` 聚合。

**How the verdict was reached:** 两条 limitation 均与当前实态相反。"Container sandbox tests require Docker" 与 "Frontend function coverage below 80%" 两条则与实态一致（agent-engine 记录运行恰有 4 skipped；前端函数覆盖记录为 64.68%）。

---

## Open questions

1. CODE_REVIEW_REPORT.md §二 声称"9 个 P0"，但编号只到 P0-008（8 个）。若存在未编号的第 9 个，无法静态枚举；本报告按可枚举的 8 个裁定。
2. `AgentExecutionLifecycleServiceTest` 在失败运行后 3 分钟被编辑（未提交），是否已修复无重跑产物——需 `mvn test -pl schemaplexai-agent-engine` 判定。
3. agent-engine 与 dao 的 `jacoco.exec`（2026-07-02 / 07-01）存在但无 site 报告：是报告生成被跳过还是产物被清理，无法静态判定。
4. CHANGELOG 的 2,601 / 100 等数字来自哪一次运行无法溯源（仓库无对应的历史 surefire 存档）。
5. 工作区存在 4 个未提交修改（`git status`：AgentRuntimeOrchestrator.java、AgentExecutionLifecycleService.java + 对应测试等）——"仓库实态"以含未提交改动的工作区为准。

## 反向差距（仓库超出/偏离两份清单的状态）

1. **AGENTS.md 严重过期**（与实态反向）：声称"没有任何自动化测试"（实为 4,119+ `@Test`、16 模块全有 `src/test`）；声称 application.yml 硬编码 MySQL（实为全 PostgreSQL + `${VAR:?required}`）；声称 agent-engine 为自动 ACK（实为 `acknowledge-mode: manual`）；声称 admin 为占位桩（实为 25 个主代码文件 + 111 测试）；声称 docker 存在 9000 端口冲突与 `gafana` 拼写错误（均已修复：ClickHouse native 改映射 `127.0.0.1:9004:9000`、服务名 `grafana`，且 compose 已有 7 处 healthcheck）。
2. 状态机由规格时期的 13 态演化为 **18 态**，15 个状态处理器全部有测试（超出"13 states"口径）。
3. `JwtSecretStartupValidator`（common 模块，含测试）——超出规格"移除默认值"要求的启动期强校验（stronger-than-spec）。
4. 规格未点名的 10 个模块（admin、agent-config、context、integration、ops、quality、spec、task、web、workflow）均建立了测试套件（合计约 2,280 个 `@Test`）。
5. CI（`ci.yml`）已远超规格时期形态：三轨安全扫描、全 16 模块 `jacoco:check`、前端 lint/test/tsc、Playwright e2e smoke。但 `dependency-check` 为 `continue-on-error: true`、Trivy 无 fail 条件——安全门禁非硬阻断。
6. `docs/reviews/v1-readiness/MASTER.md`（2026-05-08，Day-0 基线）判定 **NOT-READY-FOR-RELEASE**（4.0/10），与同日 CHANGELOG 发布 1.0.0 的叙事并存——两者时间戳相同但结论相反，属文档叙事冲突（MASTER 自述为基线快照）。
7. `schemaplexai-ui/vitest.config.ts:12` 仍保留 `passWithNoTests: true`。
8. `AgentRuntimeOrchestratorIntegrationTest` 命名含 Integration 但已无 Spring 上下文（纯 Mockito）。
9. 仓库根存在疑似误建目录 `D:code_spacefrigeschemaplexai-opssrcmainresourcesdbmigration/`、`D:code_spacefrigeschemaplexai-qualitysrcmainresourcesdbmigration/` 与 `com/`（路径分隔符被吞产生的垃圾目录），与任何规格无关。

---

## 反驳复核

复核身份：独立复核员（非原分析作者）。复核日期：2026-08-29。方法：只读复核——逐份重读原分析引用的 surefire / JaCoCo 物证并独立重算；按模块枚举全部 surefire 产物的最新时间戳，检索失败运行（2026-07-02 09:54:57）之后是否存在更新的运行产物（排除 .git/node_modules/target 后全仓按时间枚举）；用 `git status` / `git diff` / `git log` 与文件 mtime 交叉核对工作树未提交修改与失败运行的先后关系；重读两份规格验收条款原文。未运行任何构建或测试。复核范围：全部 severity=High 且 verdict=contradicted 条目（REQ-05、REQ-18、REQ-06），以及任务指定、同证据链的"修复完成"汇总宣称 REQ-10（原文 severity 为 Medium-High）。

| REQ | 原判 | 裁定 | 理由（一行） | 证据 |
|---|---|---|---|---|
| REQ-05 | contradicted · High | **维持** | 物证时间戳/失败数/模块归属复核无误，且未提交的主源码修改（09:45/09:50）早于失败运行（09:51–09:54）——失败恰是含该修改的运行结果，其后全仓无任何重跑产物，"0 failures"宣称失真成立 | `AgentRuntimeOrchestratorTest.txt`（2026-07-02 09:53）`Tests run: 13, Failures: 2`、`AgentExecutionLifecycleServiceTest.txt`（09:54）`Tests run: 1, Errors: 1` 原文重读一致；各模块最新 surefire 产物均 ≤ 2026-07-02 09:54:57，失败运行后按时间枚举全仓（排除 target/node_modules/.git）仅命中本复核目录；源文件 mtime：`AgentRuntimeOrchestrator.java` 09:50:15、`AgentExecutionLifecycleService.java` 09:45:42 均早于运行；`AgentRuntimeOrchestratorTest.java` 自 2026-07-01 18:52 未变；规格 §3 "`mvn clean test` passes (0 failures)" 为无条件二值门禁（COVERAGE.md 所载 2026-05-25 全绿记录早于红运行，不能作最新状态依据） |
| REQ-18 | contradicted · High | **维持** | 与 REQ-05 同一门禁事实，证据链独立复核全部成立，无更新反证 | 同 REQ-05；规格 B Acceptance Criteria "`mvn clean test` — 0 failures across all modules" 为硬性门禁；全仓仅 3 份 FAILURE 报告，其中 gateway 一份为 2026-05-09 过期残留（对应测试类已删除，`find src/test` = 0 命中），其余 2 份即 agent-engine 在跑失败 |
| REQ-06 | contradicted · High | **维持** | model 21.0%（复核重算 35/167 一致）与 dao 最后记录 44.6% 双双低于 80%，合取门禁被证伪；但发现 model CSV 仅列 12 个已编译类中的 5 个（有全绿测试的类缺失），21.0% 精确值存疑——即便如此，仓库全部 model 记录值（31.3%/21.0%）均远低于 80% 且无任何达标记录，裁定不变 | `schemaplexai-model/target/site/jacoco/jacoco.csv`（2026-07-02 09:38，awk 重算 21.0%/行覆盖 66.7%）仅 5 类，而 `target/classes` 有 12 个 .class（09:07 编译，早于报告）且同批 09:38:30 surefire 显示 BaseEntity/Notification/Observability 等测试全绿——CSV 疑似截断，精确值不可靠但方向不利不变；`docs/COVERAGE.md` 2026-05-06 基线 model 31.3% / dao 44.6%；dao 仅有 2026-07-01 `jacoco.exec`、无 site 报告（无更新数值记录）；根 `pom.xml` check 规则 INSTRUCTION 0.80 + BRANCH 0.60（model/dao pom 无覆盖），`ci.yml` `mvn jacoco:check` 覆盖全部 16 模块且无 `continue-on-error`（对比 spotbugs/checkstyle 均有），门禁为硬 |
| REQ-10 | contradicted · Medium-High | **维持** | 找到有利反证——COVERAGE.md 记录 2026-05-25 agent-engine 1843 tests 0 failures 全绿，且当前 3 个失败均属 2026-07-01 提交 c10296f（"agent lifecycle"）引入/改动的暂停-恢复路径，疑似新回归而非原 21 个遗留——但这只是归因细化：截至核查日最新记录运行仍红、无重跑产物，"修复完成"作为完成态宣称仍被最新物证证伪，且同文档 AC（0 failures）被违反 | `docs/COVERAGE.md` "Update 2026-05-25"（`rtk mvn test -pl schemaplexai-agent-engine -am` 0 failures 0 errors）；`git log`：c10296f（2026-07-01）同时改动两个失败测试文件，`AgentRuntimeOrchestratorTest` 初现于 53b5362（2026-05-07）；2026-07-02 09:51–09:54 红运行（2 failures + 1 error）为其后唯一记录运行；未提交修改使 `isPaused`/pause key 改用 `TenantRedisKeyResolver`，对应测试期望未同步更新且无重跑 |

复核结论：4 条分歧条目全部**维持**原判（推翻 0 条）。两处原分析可补强的细节（不改变裁定）：① model 21.0% 所依据的 jacoco.csv 为部分类报告（5/12），精确百分比不应作为唯一依据，但"未达 80%"由 dao 44.6% 与 model 全部记录值独立支撑；② 当前 3 个失败更可能是 2026-07-01 生命周期功能提交后的新回归（5 月 25 日有全绿记录），这改变失败的归因，不改变发布门禁被最新红记录证伪的事实。
