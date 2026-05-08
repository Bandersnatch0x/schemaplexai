---
title: TestDocSentinel 测试与文档守门
agent: TestDocSentinel
date: 2026-05-08
domain: test-and-docs
---

> 角色：测试与文档守门员（TestDocSentinel）。范围：JaCoCo 全模块强制、E2E smoke、Knife4j 注解、wiki/CLAUDE.md drift、PR/同步主分支策略。

## 1. 0-10 评分表

| 子维度 | 当前分 | 10 分定义 | 证据 |
|---|---|---|---|
| 后端覆盖率门禁 | 4 | 全 16 模块 BUNDLE ≥ 80% INSTRUCTION + ≥ 60% BRANCH，CI required，main 分支保护强制 | `pom.xml:282-298` 阈值正确，但 `.github/workflows/ci.yml:41` 仅 7/16 模块强制：common/model/dao/gateway/system/agent-engine/context。web/agent-config/integration/ops/admin/spec/quality/workflow/task **9 个模块完全没有覆盖率门禁** |
| 后端测试体量 | 7 | unit + integration + 租户隔离 baseline 覆盖全 16 模块 | 313 个 `*Test.java`、2,601 测试用例，agent-engine 36 套已优秀；但 spec/quality/integration/task/workflow 几乎无测试（见模块清单） |
| 前端覆盖率 | 3 | line ≥ 70% / functions ≥ 70%，CI 上传 coverage 报告 | `vitest.config.ts` **未配置 coverage provider**（无 v8/istanbul），`passWithNoTests: true` 直接放水；CI `npm test -- --run` 不含 `--coverage`；100 测试 / 20 文件，覆盖率不可观测 |
| E2E smoke | **1** | CI 含 ≥ 3 个核心 smoke（登录 / 首页 / Agent 流），均稳定通过 | **致命 drift**：`.github/workflows/ci.yml:152` 引用 `e2e/smoke.spec.ts`，实际目录只有 `figma-regression.spec.ts` + `screenshots.spec.ts`，**CI 必跑文件不存在，e2e job 100% 失败或被默认 missing 跳过** |
| 测试金字塔 | 5 | unit 70% / integration 25% / e2e 5%，比例稳定 | unit 占绝对多数，integration 主要在 agent-engine（Testcontainers 未广泛使用），e2e 几乎为零；Pact / contract 测试 0；倒金字塔趋向"unit-only" |
| Knife4j 覆盖 | **9** | 全 controller `@Tag` + 全公开方法 `@Operation`，仅抽象基类豁免 | 经实际校验：100% Controller 有 `@Tag`，`BaseController` / `BaseAdminController` 等抽象基类不需注解。**memory 中"29 个缺"的旧记录已严重过时** |
| Wiki/docs drift | 3 | doc-gardener 周扫 drift = 0，CLAUDE.md / README / wiki 三处一致 | CLAUDE.md 多处 drift（"12 模块"实际 16；声称 admin "empty"，gaps.md 已列出 6 个 admin Service；前端测试文件数对不上），active-areas.md 自动生成块全空，gaps.md 列出 17 个 Undocumented Service |
| ADR 时效 | 6 | 每个架构决策有最新 ADR、有 superseded 标记、变更回流主索引 | 9 个 ADR 在 `wiki/decisions.md`，结构整洁，但近 2 周 v1.0 状态机/Outbox/事件总线变更未沉淀新 ADR |
| 总分 | **38/80（4.75/10）** | — | — |

## 2. 模块覆盖率攻坚清单

> 数值取自 8 模块基线评估（旧数据需 W2 重跑 `mvn jacoco:report -pl <module>` 后刷新到表格）。攻坚周次对齐 `v1-release-readiness` 30 周计划。

| 模块 | 当前 | 目标 | 缺口 | 攻坚周次 | 攻坚策略 |
|---|---|---|---|---|---|
| **schemaplexai-web** | 33% | 80% | 47 pp | W3 | Controller 层缺 `@WebMvcTest` slice 测试。优先：`AgentExecutionController` / `AuthController` / `SseController` / 上传链路；用 `MockMvc` + `@MockBean Service`，每个 controller 至少 3 用例（happy / 4xx / 5xx）。SSE 用 `WebTestClient` 流式断言。 |
| **schemaplexai-context** | 55% | 80% | 25 pp | W3 | RAG 链路是当前 6 个 Critical bug 集中区，必须先把 `RagSearchServiceImpl` / `MilvusSyncServiceImpl` / `KnowledgeDocServiceImpl` 全分支用 Testcontainers `milvusio/milvus:standalone-2.3.5` 覆盖；同时验证 C-1（embedding 真实化）、C-2（FAILED 状态落地）、M-11（filter 注入）的修复回归。 |
| **schemaplexai-system** | 61% | 80% | 19 pp | W4 | RBAC 决策树缺 negative case；用参数化测试遍历 role × permission 矩阵；多租户隔离断言扩展为 baseline test（参考 `TenantIsolationFlowTest` 模板）。JWT 刷新链路必须有并发 race 测试。 |
| **schemaplexai-integration** | 61% | 80% | 19 pp | W4 | GitHub/GitLab/Jenkins/MCP webhook **0 测试**。攻坚：每个 connector 至少 1 套 WireMock + 签名校验（HMAC-SHA256）+ 重放攻击防护测试；MCP 协议握手用 fake transport stub。 |
| **schemaplexai-admin** | 66% | 80% | 14 pp | W4 | 模块此前被 CLAUDE.md 误称 "empty"，实际有 6 个 admin Service（gaps.md 已列）。攻坚：每个 Service 走 `@DataJpaTest`-style 路径 + 审计日志写入断言；`AuditLogService` 必须有不可篡改性合约测试。 |
| **schemaplexai-agent-config** | 67% | 80% | 13 pp | W4 | shadow config / canary 切换逻辑缺集成测试；`AgentShadowConfigService` 用 `@SpringBootTest(webEnvironment=RANDOM_PORT)` 模拟双版本并行。 |
| **schemaplexai-ops** | 69% | 80% | 11 pp | W5 | 费用结算（`BudgetService` / `CostAnalyticsService`）需要 ClickHouse Testcontainer 集成；`ClickHouseCostSyncService` 加幂等性 + 断点续传测试。 |
| **schemaplexai-gateway** | 74% | 80% | 6 pp | W5 | 缺口集中在 RateLimitFilter（M-9 Lua 原子化路径）、JwtAuthFilter 白名单边界、TenantResolveFilter（m-2 死代码删除后回归）。WebFlux `WebTestClient` 路径覆盖。 |
| schemaplexai-spec | (未测) | 80% | TBD | W6 | 0 测试，先建 baseline；spec 模板渲染、版本 diff、review 状态机各补一套。 |
| schemaplexai-quality | (未测) | 80% | TBD | W6 | drift 检测、安全策略 0 测试；先做 contract 测试桩（外部 SAST 工具假设）。 |
| schemaplexai-workflow | (未测) | 80% | TBD | W7 | Flowable BPMN engine 集成测试 + AI 节点 delegate 测试（`AiAgentExecutionDelegate`）。用 Flowable 自带 `@Deployment` 注解 + `runtimeService` 断言。 |
| schemaplexai-task | (未测) | 80% | TBD | W7 | RabbitMQ 消费者幂等 + 失败重投契约。Testcontainers `RabbitMQContainer`。 |

> **关键阻塞**：在 W3 之前必须先把 `mvn jacoco:check` 的 module list 改造成"全模块"，否则即使写完测试，CI 仍然不会强制。

## 3. 已检测到的文档 drift（必须修）

| 文档 | drift 内容 | 真实状态 | 修法 |
|---|---|---|---|
| `CLAUDE.md:13` | "Many business modules are stubs. `schemaplexai-admin` is empty." | admin 已有 ≥ 6 个 Service：AuditLog / PlatformHealth / RoleAdmin / SystemConfig / TenantAdmin / UserAdmin（见 `wiki/gaps.md:18-23`） | 改为"admin 模块已实现核心 6 个 Service，wiki 文档待补"；同步删除"empty"字样 |
| `CLAUDE.md:13` | "Tests: 78 backend + 14 frontend test files" | 实测 313 个 `*Test.java` + 20 个 `*.spec.ts/*.test.ts`，2,601 + 100 用例 | 数据回写实际值，并加注 "as of 2026-05-08, run `git ls-files \| grep Test` 重测" |
| `CLAUDE.md:42-57` Service Map | 列 12 Service，实际 16 模块（缺 common/model/dao/admin） | 见 `pom.xml:21-38` | Service Map 加备注："common/model/dao 为基础库无端口；admin 为后台管理服务（端口 待定）"；或单独画 16 模块依赖图 |
| `CLAUDE.md:13` | "agent-engine (36)..." 旧统计 | 数字未变但语境过时（v1 readiness 阶段已过） | 新增"v1.0 readiness 进行中"段落取代静态计数 |
| memory `project-progress.md` | 写"29 个缺 Knife4j @Tag" | 已 100% 覆盖（仅抽象基类无注解，正确） | 删除该条；改为"Knife4j @Tag 覆盖 100%（2026-05-07 校验）" |
| `wiki/active-areas.md:18-21` | "Active Specs / Active Plans" 自动生成块**全空** | docs/specs / docs/plans 下有 ≥ 6 个进行中文档（v1-release-readiness、open-source-agent 等） | 修 `scripts/sync-wiki.sh` 的 grep 规则，按 `status: 进行中/评审中` front-matter 重新扫描 |
| `.github/workflows/ci.yml:152` | 引用 `e2e/smoke.spec.ts` | 文件**不存在**，仅 figma-regression / screenshots | 列入 W2 必修：补 smoke.spec.ts；同时修 CI 让缺失 spec 直接 fail（去掉静默跳过） |
| `wiki/index.md:81-83` 引用 docs | 引用 2026-04-29 / 2026-04-30 设计稿，未链接 v1.0 中期评审产物 | docs/reviews/v1-readiness/ 即将产出 7 份评审 | 评审完成后 wiki/index.md 加节点链接 |
| README.md（未读但需检查） | quickstart 是否含最新 Knife4j 路径 / 16 模块结构 | — | doc-gardener 在 W2 跑一次，导出 drift list |

## 4. 关键发现（≥ 6 条带证据）

1. **JaCoCo 强制只覆盖 7/16 模块（44%），等于一半模块默认裸奔**——`pom.xml` 的 80%/60% 阈值是写对了，但 `ci.yml:41` 的 `-pl` list 把 web / agent-config / integration / ops / admin / spec / quality / workflow / task 9 模块全部排除。这意味着低覆盖率代码可以无门槛合入 main，且 web 33% 这种数字根本不会触发 CI 红灯。
2. **CI 引用了不存在的 e2e 文件，e2e job 是"幽灵 step"**——`.github/workflows/ci.yml:152` 跑 `playwright test e2e/smoke.spec.ts`，但 `schemaplexai-ui/e2e/` 仅含 `figma-regression.spec.ts` + `screenshots.spec.ts`。GitHub Actions 中 Playwright 对 missing file 的默认行为是 exit 1，e2e job 实际状态长期失败但无人响应（或被开发者用 `continue-on-error` 兜住——需查最近 10 次 run）。
3. **前端覆盖率 0 可观测**——`vitest.config.ts` 没配 `coverage.provider`，`passWithNoTests: true`，CI 仅跑 `npm test -- --run` 不带 `--coverage`，`npx tsc --noEmit` 是类型检查不是覆盖率。结论：前端 100 个测试是否覆盖到关键 page / store / api 层无任何度量。
4. **Knife4j 旧统计严重落后真实**——memory 中"29 缺"的口径已被 commit `e134899 feat(docs): add Knife4j annotations to all controllers, boost test coverage` 覆盖。当前真实状态是 100% Controller 注解齐全，仅 `BaseController` / `BaseAdminController` 抽象基类无 `@Tag`（合理）。这条 drift 直接影响下游 PlanModerator 的优先级判断。
5. **CLAUDE.md 关于 admin 的描述与代码事实矛盾**——CLAUDE.md 第 13 行说 "admin is empty"，但 `wiki/gaps.md` 自动扫描列出 admin 6 个 Service（AuditLog / PlatformHealth / RoleAdmin / SystemConfig / TenantAdmin / UserAdmin）。CLAUDE.md 是 session 启动 always-loaded 文件，**它误导 every conversation 的初始上下文**，影响极大。
6. **测试金字塔倒挂趋势**——313 backend Test 中绝大多数是 unit + slice，integration（Testcontainers / `@SpringBootTest`）比例低；e2e 名义在 CI 但文件不存在。结合 code-review 报告 6 个 Critical bug 集中在跨进程一致性（事务+Milvus、SSE 多副本、状态机递归），**当前测试结构对这类 bug 完全测不出来**——unit test 不会触发事务回滚抹掉 FAILED 状态。
7. **PR / 主分支保护规则缺失证据**——`.github/PULL_REQUEST_TEMPLATE.md` 不存在；`.github/dependabot.yml` 不存在；CI 三个 job（backend / frontend / e2e）是否都被 GitHub branch protection 设为 required check 不可在仓库内确认（需 admin 抓 settings 截图）。这是 v1 release 流程上最大的盲点。
8. **active-areas.md 自动生成块全空 = sync-wiki 脚本损坏**——`wiki/active-areas.md:18-21` 的 "Active Specs / Active Plans" 自动生成节都是空的，但 `.claude/changes/` 有 8 个活跃任务、docs/specs/ 有正在进行的 spec。说明 `scripts/sync-wiki.sh` 的 grep 逻辑挂了或路径变了，"自维护知识库"的核心承诺当前**已断**。
9. **9 个 ADR 写到 2026-04，近 2 周空白**——v1.0 期间引入了状态机、Outbox 模式（待）、Embedding Profile 隔离等多项架构决策，wiki/decisions.md 没有新增条目，决策追溯链断裂。

## 5. 测试基建强化方案

### 5.1 JaCoCo 全模块强制（W2 必）
修 `.github/workflows/ci.yml:41`，把 `-pl` 列表改为全模块或直接删 `-pl`，让 reactor 自然遍历：

```yaml
- name: Check test coverage (JaCoCo BUNDLE >= 80%/60%)
  run: mvn jacoco:check -fae
```

`-fae`（fail-at-end）保证一次跑完拿到全模块红绿矩阵而非首个失败即退；同时在 main 分支保护里把这个 step 设为 required check。短期可在低覆盖模块开 `<excludes>` 临时白名单（W3-W7 逐周拆除）。

### 5.2 E2E smoke 用例补全（W2 必）
立即在 `schemaplexai-ui/e2e/` 新建：
- `smoke.spec.ts`：登录（admin / 123456）→ 进首页 → 列出 Agent → 调一次执行看 SSE 流出 ≥ 1 个 token；
- `spec-creation.spec.ts`：创建 spec → 提交 review → 看到 review record 出现；
- `multi-tenant.spec.ts`：tenant A 创建数据 → 切到 tenant B 看不到（验证 C-3 / M-11 修复）。

CI 同步改为 `npx playwright test e2e/` 跑全套，并 `--reporter=html` 上传 artifact。

### 5.3 前端覆盖率门禁
`vitest.config.ts` 加 coverage：

```ts
test: {
  coverage: {
    provider: 'v8',
    reporter: ['text', 'html', 'lcov'],
    thresholds: { lines: 70, functions: 70, branches: 60, statements: 70 },
    exclude: ['e2e/**', '**/__tests__/**', '**/*.d.ts'],
  },
  passWithNoTests: false,
}
```

CI 增加 `npm test -- --run --coverage` 步骤；上传 `coverage/lcov.info` 到 Codecov。

### 5.4 Contract 测试（W6+）
- 引入 Spring Cloud Contract（与 Spring Boot 生态自然），让 Gateway ↔ 后端、agent-engine ↔ context 之间产生 stub jar，前后端独立构建期可消费。
- integration 模块的外部 connector（GitHub / GitLab / Jenkins / MCP）用 Pact 做 consumer-driven contract，连 broker 上传到 Pactflow。

### 5.5 Mutation 测试（W8+）
PIT mutation testing（`org.pitest:pitest-maven`）对 agent-engine 核心 reasoning / planning / state-machine 跑变异，目标 mutation score ≥ 60%——能检验 unit test 是否真的"测了行为"而不是"覆盖了行"。

### 5.6 多租户 baseline test
把现有 `TenantIsolationFlowTest` 提取成 `@TenantIsolationContract` 接口测试模板，每个新增 Service 必须实现一遍 tenant A 写 / tenant B 读 = 0 行 的断言。

## 6. 文档同步策略

### 6.1 doc-gardener 周扫制度化
- 已在 `~/.claude/agents/` 有 doc-gardener 角色，**周一 09:00 cron 触发**（GitHub Actions schedule + `claude-code` run），输出到 `.claude/outputs/<YYYY-MM-DD>/doc-drift.md`。
- 任何 `CRITICAL` drift（CLAUDE.md / README / wiki/index）打开 issue + 自动指派当周 on-call。

### 6.2 PR 模板新建（W2）
新建 `.github/PULL_REQUEST_TEMPLATE.md`：

```markdown
## 变更摘要
## 测试
- [ ] 新增/修改测试覆盖关键路径
- [ ] `mvn test` 全绿
- [ ] `mvn jacoco:check` 全模块通过
- [ ] 前端 `npm test -- --coverage` ≥ 70%
- [ ] e2e smoke 通过

## 文档同步
- [ ] CLAUDE.md / README / wiki 已同步（如适用）
- [ ] 新增/变更 Controller 已加 `@Tag` + `@Operation`
- [ ] 新增 Service 已在 `wiki/services/` 建页（auto-gen 也行）
- [ ] ADR 已新增/更新（如涉及架构决策）

## Risk / Rollback
```

### 6.3 git pre-push hook 升级
现有 `pre-commit-wiki-sync` 仅在 commit 时拦截。扩展为 `pre-push`：
- 跑 `scripts/sync-wiki.sh --check`，若有 unstaged drift 则拒绝 push；
- 在 main 分支额外跑 `mvn jacoco:check` 本地预检，避免推空转 CI。

### 6.4 ADR 节奏
任何 P0/P1 改动必须在同 PR 内 `wiki/decisions.md` 加一条 ADR 段落（或新文件）。`code-reviewer` agent 的 checklist 里加这条。

## 7. 同步主分支与 PR 流程（v1.0 标准）

| 步骤 | 规则 |
|---|---|
| 分支命名 | `feat/<scope>-<short-desc>` / `fix/<scope>-<bug>` / `docs/<scope>` |
| commit 规范 | conventional commits（feat/fix/refactor/docs/test/chore），首行 ≤ 72 字，body 写 why |
| 必过 CI | backend.mvn-test + backend.jacoco-check（全 16 模块）+ frontend.test+coverage + frontend.tsc + e2e.playwright-smoke + spotbugs/checkstyle（去 continue-on-error） |
| 必过 review | code-reviewer agent + 1 个 human approval；security-sensitive 文件触发 security-reviewer |
| 合并方式 | **Squash merge to main**，PR title 即 commit msg |
| main 分支保护 | Require status checks / Require linear history / Require signed commits / Restrict who can push（仅 maintainer） |
| 同步 develop → main | 双周固定窗口；release 前必跑 e2e + 全模块 jacoco:check |
| Tag & release | `v1.0.0-rcX` → `v1.0.0`，CHANGELOG 自动从 conventional commits 生成 |

## 8. 改造方案

| 周 | 项 | 验收 |
|---|---|---|
| W2 | 写 e2e/smoke.spec.ts + spec-creation.spec.ts + multi-tenant.spec.ts；JaCoCo `-pl` 改全模块（带白名单）；vitest 加 coverage v8 + 阈值；新增 PR template + dependabot.yml | CI 三个 job 全绿；前端覆盖率报告可见 |
| W3 | web / context 模块覆盖率攻坚至 ≥ 80% | `mvn jacoco:check -pl web,context` 通过 |
| W4 | system / integration / admin / agent-config 覆盖率 ≥ 80% | 同上 |
| W5 | ops / gateway 覆盖率 ≥ 80% | 同上 |
| W6 | spec / quality 模块从 0 到 80%；引入 Spring Cloud Contract | contract jar 发布 |
| W7 | workflow / task 覆盖率 ≥ 80%；JaCoCo 白名单全部拆除 | `mvn jacoco:check`（无 `-pl`）通过 |
| W8 | PIT mutation 在 agent-engine 跑通，mutation score ≥ 60% | mutation 报告 artifact |
| W9 | doc-gardener 周扫 cron 上线；CLAUDE.md / README / wiki 三处 drift = 0 | doc-drift 报告全绿 |
| W10 | ADR 补齐近 2 周决策；wiki/decisions.md 加 status: superseded 标记 | 9 → 12+ ADR |

## 9. 给用户的关键问题

1. **CLAUDE.md 都把模块数写错（声明 12 实则 16）、admin "empty" 与代码矛盾，那么其他关键文档（README、AGENTS.md、wiki/architecture.md）默认在多大程度上还在骗你？是否能接受 W2 用 doc-gardener 跑一次全量 drift 扫描，结果对齐为基线？**
2. **CI 引用的 `e2e/smoke.spec.ts` 文件根本不存在，但 e2e job 一直挂在 workflow 里——你之前看到的 e2e 绿勾是真的吗？建议下个 PR 立刻补文件并把 CI 改成"missing spec → fail"。**
3. **JaCoCo 80% 门禁只覆盖 7/16 模块，剩下 9 个模块（含 web 33%）是裸奔的。是否同意 W2 立刻把所有模块纳入 `mvn jacoco:check`，未达标模块用临时 `<excludes>` 白名单逐周拆？**
4. **前端 100 个测试零覆盖率度量、`passWithNoTests: true` 直接放水。是否同意 W2 强制 v8 coverage + 70% 阈值，且 CI required？**
5. **`wiki/active-areas.md` 自动生成块全空，意味着"自维护知识库"的脚本已经悄悄坏了。要不要把 sync-wiki 脚本的修复列为 W2 必修？**
