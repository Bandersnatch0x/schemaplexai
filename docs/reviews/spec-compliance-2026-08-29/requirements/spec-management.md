# Spec 合规核查报告 — spec-management

- **核查对象**: `schemaplexai-spec` 模块（含 gateway / model / dao / common / docker SQL 执行点；并扩展核查了 `schemaplexai-workflow` 中与 Spec 评审相关的 BPMN 流程）
- **权威 spec**: `docs/specs/2026-04-30-v1.0-spec-management.md`（任务声明 status 已批准；文件 frontmatter 实际标注 `status: 草稿`，见 L5——本身即一处元数据漂移）
- **核查日期**: 2026-08-29（独立复核版：本报告所有关键断言均经逐文件实读与 grep 复核）
- **方法**: 逐字通读 spec → 提取 21 条规范性需求 → grep 定位 + 逐文件读实际代码 → 走通全部路径（含驳回/回滚等分支）后裁决
- **前置事实修正**:
  1. CHANGELOG.md:113 称 "Some modules (ops, quality, spec, workflow) have no test coverage yet"，实际 `schemaplexai-spec/src/test/` 下有 12 个测试文件（6 controller + 5 service + 1 util + 1 application）——该 CHANGELOG 条目已过时（文档错代码对，Low）。测试内容（如 SpecReviewServiceImplTest:104-116）被用作"实现意图"证据。
  2. 与 AGENTS.md "多数 application.yml 指向 MySQL" 的说明不同，`schemaplexai-spec/src/main/resources/application.yml:7-12` 已正确配置 PostgreSQL（`jdbc:postgresql://...`），与 docker-compose 一致。

## 裁决统计

| Verdict | 数量 | REQ |
|---|---|---|
| implemented | 2 | 08, 18 |
| partial | 7 | 01, 04, 05, 11, 15, 16, 17 |
| contradicted | 4 | 02, 10, 13, 19 |
| absent | 7 | 03, 06, 07, 09, 12, 14, 21 |
| undecidable | 1 | 20 |
| stronger-than-spec | 0 | — |

---

## REQ-01 — 文档类型枚举 SPEC / STEERING / TEMPLATE

> "| `SPEC` | 产品需求规格 | 定义功能需求 | / | `STEERING` | AI 行为约束 | 约束 Agent 行为、输出格式、安全规则 | / | `TEMPLATE` | 文档模板 | 快速创建标准化文档 |" — spec-management.md §2

**Verdict:** partial · confidence: high

**What this demands:** 系统文档类型体系应为 SPEC / STEERING / TEMPLATE 三类，且类型值受此枚举约束。

**Where enforcement lives:**
- `SfSpec.java:14` — `private String type;` 自由字符串，无枚举、无校验注解。
- `docker/postgres/init/01-init-schema.sql:131` — `type VARCHAR(32) NOT NULL, -- REQUIREMENT / DESIGN / TASK / STEERING`：DB 注释给出的是**另一套**类型集（无 SPEC、无 TEMPLATE，多出 REQUIREMENT/DESIGN/TASK）。
- TEMPLATE 未作为 sf_spec.type 存在，而是独立表 `sf_spec_template`（01-init-schema.sql:166-176）+ `SfSpecTemplate.java` + 完整 CRUD（SpecTemplateController.java）。
- `SpecServiceImpl.validateSpecType():158-162` — 仅校验非空，不校验取值。

**Paths walked:**
- ✓ 创建 Spec 时 type 传任意字符串（如 "FOO"）→ `SpecController.create():28-31` → `save()` 直接落库，无拦截。
- ✓ `createFromTemplate` 路径 → `SpecServiceImpl:120-138`，type 仅非空校验。
- ✓ TEMPLATE 能力路径 → 由独立表/独立 API 承载（机制不同但能力存在）。

**Searched:**
- `SpecType|enum.*Type`（schemaplexai-spec + schemaplexai-model/src/main）→ 0 类型枚举命中（仅命中 `validateSpecType` 方法名：SpecServiceImpl:123,158、SpecTemplateServiceImpl:36,123）。
- `SPEC|STEERING|TEMPLATE` 作为类型常量 → 仅 SQL 注释 01-init-schema.sql:131 出现 STEERING。

**How the verdict was reached:** TEMPLATE 以独立表机制实现（按规则记文档漂移而非 contradicted）；STEERING 在 DB 注释中存在但实际由独立表 sf_spec_steering 承载（见 REQ-13）；类型集不一致且完全无枚举约束——能力部分成立、约束缺失，故 partial 而非 implemented；因代码没有主动实现一套与 spec 对抗的强制类型集（只是不约束），不判 contradicted。

**Severity:** Medium（类型自由字符串 + 双方类型清单不一致，下游按类型分流的逻辑无从建立）。

---

## REQ-02 — 版本生命周期状态机 DRAFT → IN_REVIEW → APPROVED → PUBLISHED → DEPRECATED

> "DRAFT → IN_REVIEW → APPROVED → PUBLISHED → DEPRECATED" — spec-management.md §3.1

**Verdict:** contradicted · confidence: high

**What this demands:** Spec 状态取值限于这 5 个枚举，且状态迁移沿此单向链条推进。

**Where enforcement lives:** 不存在集中状态机。全模块所有状态写入点（`setStatus(` 全量实读，8 处）：
- `SpecServiceImpl.java:42` — `spec.setStatus("published")`（小写）
- `SpecServiceImpl.java:83` — `spec.setStatus("archived")`（spec 状态集外）
- `SpecServiceImpl.java:132` / `SpecTemplateServiceImpl.java:57` — `spec.setStatus("draft")`（小写）
- `SpecVersionServiceImpl.java:74` — `spec.setStatus("ACTIVE")`（spec 状态集外）
- `SpecReviewServiceImpl.java:49` — `spec.setStatus("CHANGES_REQUESTED")`（spec §3.1 中这是评审决策值，不是生命周期状态）
- `SpecReviewServiceImpl.java:52` — `spec.setStatus("APPROVED")`
- DB 默认值 `'DRAFT'`（大写，01-init-schema.sql:132）

**Paths walked:**
- ✓ 创建 → 状态 "draft"（小写）与 DB 默认 'DRAFT'（大写）并存。
- ✓ 提交评审路径：**不存在** IN_REVIEW 写入点。全仓 `IN_REVIEW`（*.java）命中仅 `schemaplexai-quality/.../ReviewServiceImpl.java:24,75,82,154`（质量域 `STATUS_IN_REVIEW`，与本域无关），spec 模块 0 命中。
- ✓ publish 路径 → "published"；再次 publish 同一 Spec → 仍成功（无前置状态校验，SpecServiceImpl:35-74 全程不读 status）。
- ✓ 废弃路径：DEPRECATED **不存在**；替代品是 "archived"（SpecController:70-74 → SpecServiceImpl:77-88）。
- ✓ 任意状态 → 任意状态：`getStatus()` 在 `schemaplexai-spec/src/main` 中 **0 处调用**（复核证实），状态是只写不读的字段，任何迁移都不会被拒绝。
- ✓ 旁证：workflow 模块存在 `spec-review-approval.bpmn20.xml`（Flowable 流程），但其 delegate 只操作 process 变量与 MQ 通知（SpecReviewInitDelegate.java:22-65、SpecReviewNotificationDelegate.java:30-90），**不写回** sf_spec 状态；且 schemaplexai-spec 对 `workflow|amqp|flowable|rabbit` grep 0 命中，流程未被 spec 域触发（详见 REQ-09/10）。

**Searched:**
- `IN_REVIEW|DEPRECATED|PUBLISHED`（全仓 *.java）→ spec 模块 main 代码 0 命中；命中集中于 quality 域与 agent-engine/execution 事件（他域）。
- `getStatus\(\)|editable|可编辑`（schemaplexai-spec/src/main）→ 0 命中。

**How the verdict was reached:** 不是 absent——代码确实实现了一套状态模型，但它使用不同的状态集（draft/published/archived/ACTIVE/APPROVED/CHANGES_REQUESTED）、大小写不一致、且无任何迁移约束，与 spec 声明的 5 态单向链条正面冲突；也不属于"不同机制满足同一性质"，因为 IN_REVIEW/DEPRECATED 两个状态语义整体消失、且非法迁移不被阻止。

**Severity:** High（状态值混乱直接破坏所有依赖状态的下游查询与门禁；同一 DB 列内混存两套大小写值）。

---

## REQ-03 — 非 DRAFT 状态不可编辑

> "| `DRAFT` | 草稿 | 是 | / | `IN_REVIEW` | 评审中 | 否 | / | `APPROVED` | 已批准 | 否 | / | `PUBLISHED` | 已发布 | 否 | / | `DEPRECATED` | 已废弃 | 否 |" — spec-management.md §3.1（"可编辑"列）

**Verdict:** absent · confidence: high

**What this demands:** 对处于 IN_REVIEW/APPROVED/PUBLISHED/DEPRECATED 状态的 Spec，更新操作必须被拒绝。

**Where enforcement lives:** 无。`SpecController.update():34-38` → `spec.setId(id); specService.updateById(spec)` 直通 MyBatis-Plus，更新前不加载原记录、不读 status。Service 层无任何 update 钩子。

**Paths walked:**
- ✗ PUT /spec/specs/{id} 更新已 publish 的 Spec → 成功（无守卫，被违反的路径真实可达）。
- ✗ PUT /spec/versions/{id}（SpecVersionController:31-35）同样直通 updateById，已发布版本快照可被改写——比 Spec 本体可变更更严重（版本快照本应不可变）。

**Searched:**
- `getStatus\(\)`（schemaplexai-spec/src/main）→ 0 命中（状态从未被读取，编辑守卫不可能存在）。
- `editable|可编辑|checkStatus|assertDraft`（schemaplexai-spec）→ 0 命中。

**How the verdict was reached:** 守卫能力整体缺失（不是某条分支漏掉），按"整个能力缺失如实记 absent"处理；后果上等价于持续违反 spec 不变量。

**Severity:** High（已批准/已发布文档可被任意改写且不留痕——评审与发布的公信力失效；叠加 REQ-12 变更追踪缺失，篡改不可审计）。

---

## REQ-04 — createVersion：基于当前版本创建新版本（权限：编辑者）

> "| `createVersion` | 基于当前版本创建新版本 | 编辑者 |" — spec-management.md §3.2

**Verdict:** partial · confidence: high

**What this demands:** 提供从当前版本派生新版本快照的操作。

**Where enforcement lives:**
- `SpecVersionServiceImpl.createVersion():53-79` — 校验 spec 存在 → 插入 `SfSpecVersion` → **同时改写** `spec.content = content`、`spec.status = "ACTIVE"`（:72-75）。不读取任何"当前版本"做基线复制，content 完全由调用方提供。
- 入口：`SpecVersionController:67-74`（路径为 POST /spec/versions/publish，命名与语义错位）及裸 CRUD POST /spec/versions（:23-28，直接 `save()` 实体）。
- 另一条创建版本路径：`SpecServiceImpl.publishSpec():65-70`（自动编号，从 spec.content 取内容）。

**Paths walked:**
- ✓ 正常路径：specId 有效 + 数字版本号 → 版本插入成功。
- ✗ 非数字版本号路径：`SfSpecVersion.version` 为 `String`（SfSpecVersion.java:14），DB 列为 `version INT NOT NULL`（01-init-schema.sql:158）→ 传 "v2.0" 时 INSERT 在 PostgreSQL 抛 `invalid input syntax for type integer`，运行时 500。接口签名承诺 String，实际只接受数字——实体/DDL 类型错位。
- ✗ 权限路径：无编辑者校验（见 REQ-07）。
- ✓ specId 不存在 → `BaseException(SPEC_NOT_FOUND)`（SpecVersionServiceImpl:61-63）。

**Searched:**（partial 必填）
- `createVersion`（全仓 *.java）→ 3 命中：SpecVersionService 接口、SpecVersionServiceImpl.java:53、SpecVersionController.java:73；SpecServiceImpl 无该方法（其用 insert 直写）。

**How the verdict was reached:** 核心能力存在故非 absent；非数字版本路径必然失败 + 顺带改写 Spec 本体状态为 spec 外状态 "ACTIVE" + "基于当前版本"的基线复制语义缺失（内容全靠调用方传入）+ 无权限位，缺陷集中在具体路径上，故 partial。

**Severity:** Medium（类型错位是真实运行时故障点；副作用改写 content/status 未在 spec 中授权）。

---

## REQ-05 — publish：发布版本并更新 Spec 当前版本（权限：审批者）

> "| `publish` | 发布版本，更新 Spec 当前版本 | 审批者 |" — spec-management.md §3.2

**Verdict:** partial · confidence: high

**What this demands:** 发布动作生成版本并把 Spec 的"当前版本"指针推进。

**Where enforcement lives:** `SpecServiceImpl.publishSpec():35-74` — 置 status="published" → 按租户查最新版本（`orderByDesc(SfSpecVersion::getVersion)`，DB 列为 INT 故数值序正确）→ `parseInt(latest.getVersion()) + 1` 计算下一版本号（:55-63）→ 插入快照（:65-70）。入口 `SpecController:64-68`（POST /spec/specs/{id}/publish，与 §6.1 一致）。

**Paths walked:**
- ✓ 首次发布 → 版本 "1"，快照内容取自 spec.content。
- ✗ "更新 Spec 当前版本"：`sf_spec.version INT NOT NULL DEFAULT 1`（01-init-schema.sql:134）是 DB 中的当前版本指针，但 `SfSpec.java:11-17` **未映射该字段**（实体仅 title/type/status/content），publishSpec 从不更新它——当前版本指针永远停在 1。
- ✗ 状态前置校验路径：从 DRAFT（未经评审）直接 publish → 成功；spec §3.1/§4.1 隐含 APPROVED 才可发布，无门禁（getStatus() 0 调用）。
- ✓ parseInt 失败回退路径（:59-61）→ 回退为 1；由于 DB 列 INT，历史数据不可能非数字，该分支实际不可达（防御性冗余）。
- ✗ 审批者权限：无（见 REQ-07）。

**Searched:**
- `setVersion`（SfSpec / SpecServiceImpl）→ SfSpec 无 version 属性，sf_spec.version 无任何 Java 写入点。

**How the verdict was reached:** "发布版本"半边成立（快照生成✓），"更新 Spec 当前版本"半边缺失（指针字段未映射、未更新），恰为 partial 的定义；未判 contradicted 因为没有代码把指针推向错误值——它只是不动。

**Severity:** Medium（当前版本指针失真；未过评审可直接发布削弱工作流约束——后者的根因记在 REQ-02/03）。

---

## REQ-06 — rollback：回滚到指定版本（权限：管理员）

> "| `rollback` | 回滚到指定版本 | 管理员 |" — spec-management.md §3.2；及 "POST   /spec/specs/{id}/rollback    # 回滚" — §6.1

**Verdict:** absent · confidence: high

**What this demands:** 存在把 Spec 内容/当前版本恢复到历史版本的操作端点与实现。

**Where enforcement lives:** 无。`SpecService.java` 接口共 5 个方法（publishSpec / archiveSpec / getLatestVersion / compareVersions / createFromTemplate，实读确认），无 rollback；`SpecController` 在 spec 声明 rollback 的位置提供的是 `POST /{id}/archive`（:70-74，语义完全不同——归档≠回滚）。

**Paths walked:**
- ✗ POST /spec/specs/{id}/rollback → 无该路由，404。
- ✗ 任何等效机制（用旧版本 content 覆盖 spec）→ 无任何服务方法读取历史版本回写 Spec。
- ✗ 前端也无调用方：`schemaplexai-ui/src/api/spec.ts:14-32` 仅封装 5 个 CRUD 端点，无 rollback 消费方。

**Searched:**（absent 必填）
- `rollback|Rollback`（schemaplexai-spec 全模块）→ 5 命中，全部为 `@Transactional(rollbackFor = Exception.class)` 注解（5 个 ServiceImpl 各 1 处，逐行复核），无一处业务回滚。
- `revert|rollbackVersion|rollbackTo|restoreVersion`（全仓 *.java，排除 test）→ 0 命中。
- `rollback`（schemaplexai-ui/src）→ 0 命中。

**How the verdict was reached:** 多种命名惯例全部落空 + 接口清单实读确认无对应方法 + spec 声明的 URL 位置被 archive 占用，能力整体缺失。

**Severity:** High（版本控制三大操作之一缺失；发布错误内容后无系统内恢复手段，只能人工改库）。

---

## REQ-07 — 版本操作权限矩阵（编辑者 / 审批者 / 管理员 / 读者）

> "| 操作 | 说明 | 权限 | ... createVersion ... 编辑者 ... publish ... 审批者 ... rollback ... 管理员 ... diff ... 读者" — spec-management.md §3.2

**Verdict:** absent · confidence: high

**What this demands:** 四类版本操作按角色差异化授权。

**Where enforcement lives:** 无。spec 模块 5 个 Controller、5 个 ServiceImpl 中无任何授权注解或角色判断。链路上游：Gateway `JwtAuthFilter` 仅做认证（校验 JWT 签名/过期 + 白名单放行，实读 JwtAuthFilter.java:52-62 确认 whiteList 无 /spec/**，filter 主体无角色判断），合法 JWT 即放行 /spec/**（GatewayConfig.java:26-27 路由 `lb://schemaplexai-spec`）。

**Paths walked:**
- ✗ 携带任意合法 JWT 的用户调 publish / createVersion / diff / archive / delete → 全部成功，无角色差异。

**Searched:**（absent 必填）
- `PreAuthorize`（全仓 *.java）→ 4 命中：`agent-config PromptVersionController.java:10,25`（`@PreAuthorize("hasAuthority('agent:config:write')")`）、`web SseController.java:12,59`（`@PreAuthorize("hasAuthority('sse:admin:send')")`）——**spec 模块 0 命中**（对照证明：项目具备该机制且他处在用，spec 模块未接入）。另有独立机制 `schemaplexai-system/.../security/PermissionEvaluator.java`（含配套测试）存在，亦未被 spec 模块引用。
- `编辑者|审批者|管理员|hasRole|hasAuthority`（schemaplexai-spec）→ 0 命中。

**How the verdict was reached:** 授权能力在本模块整体缺失；同工程其他模块已示范 `@PreAuthorize` 用法，排除"框架不支持"解释。

**Severity:** High（任何认证用户可发布/删除任何租户内 Spec——结合 REQ-03 无编辑锁，治理文档完全不设防）。

---

## REQ-08 — Diff 采用 LCS 算法及结果结构

> "**已实现**: `SpecDiffUtil.java` 使用 LCS（最长公共子序列）算法" — spec-management.md §3.3

**Verdict:** implemented · confidence: high

**What this demands:** SpecDiffUtil 以 LCS 计算行级差异，结果含 specId/两版本标识/hunks（每 hunk 含新旧起始行、长度、行列表）。

**Where enforcement lives:**
- `SpecDiffUtil.java:20-32` — `computeLcs` 标准 LCS DP 矩阵；`:34-50` backtrack 产出 ADDED/REMOVED/UNCHANGED 行；`:52-98` 聚合为 hunks（oldStart/oldLines/newStart/newLines）。
- `SpecDiffResult.java:12-18` — specId + versionAId + versionBId + hunks。
- `DiffHunk.java:12-27` — oldStart/oldLines/newStart/newLines + 内嵌 `LineChange`（type/content）。
- `SpecVersionServiceImpl.diff():30-50` — 校验两版本存在（:40-42）且属同一 Spec（:44-46）。
- 有测试：`schemaplexai-spec/src/test/.../util/SpecDiffUtilTest.java`。

**Paths walked:**
- ✓ 正常 diff → hunks 正确分组（`SpecVersionController:62-65` GET /spec/versions/diff 暴露）。
- ✓ null 内容 → `SpecDiffUtil:11-12` 空数组兜底。
- ✓ 版本不存在 → SPEC_NOT_FOUND（SpecVersionServiceImpl:41，错误码定义于 ResultCode.java:41）。
- ✓ 跨 Spec 对比 → PARAM_ERROR "Versions belong to different specs"（:45）。

**Searched:** 不适用（implemented）。

**How the verdict was reached:** 算法、防御路径、结果结构均在。文档漂移注记：spec §3.3 的字段名 `versionA/versionB` 实为 `versionAId/versionBId`（SpecDiffResult.java:15-16）、`oldLength/newLength` 实为 `oldLines/newLines`（DiffHunk.java:15,17）、`DiffLine` 实为内嵌类 `LineChange`（DiffHunk.java:23-26）、查询参数名 `a/b` 实为 `versionAId/versionBId`（SpecVersionController:63）——同机制不同命名，按规则记 implemented + 文档漂移。

**Severity:** Low（仅需修文档对齐字段名）。

---

## REQ-09 — 评审人分配（按角色 / 指定人员 / 轮询）

> "│           评审人分配                     │ │  (可按角色/指定人员/轮询分配)             │" — spec-management.md §4.1

**Verdict:** absent · confidence: high

**What this demands:** 提交评审后系统按三种策略之一分配评审人。

**Where enforcement lives:** 无。`SpecReviewServiceImpl.submitReview():25-58` 的 `reviewerId` 是**调用方直接传入**的参数（SpecReviewController:62-67 @RequestParam），不存在分配逻辑——评审人自报家门，甚至不校验该用户存在或具备评审角色。

**Paths walked:**
- ✗ 按角色分配 → 无。
- ✗ 轮询分配 → 无。
- ~ 指定人员：调用方传 reviewerId 勉强可视为"指定"，但 spec 语境是提交者/系统指定评审人后由评审人审核，而这里提交评审与给出决策是**同一次调用**（status 参数即决策），根本没有"待评审"中间态。
- ✓ 旁路核查（新发现）：`schemaplexai-workflow` 存在 Flowable 流程 `spec-review-approval.bpmn20.xml`，其中 `primaryReviewTask` 的 assignee 为 `${reviewerId != null ? reviewerId : 'reviewer-group'}`（:46-47）、`escalatedReviewTask` 有 `candidateGroups="senior-reviewers"`（:69-70）——形式上含"指定人员/按角色"元素。但该流程**不构成执行点**：① schemaplexai-spec 对 `workflow|amqp|flowable|rabbit` grep 0 命中，提交评审不触发它；② 全仓除 BPMN 文件自身与 WorkflowDeployService.java:126 的 Javadoc 示例外，无任何代码启动 `specReviewApproval`（通用入口 WorkflowBpmnController:37 存在但无 spec 域调用方）；③ 流程 delegate（SpecReviewInitDelegate.java:22-65、HumanTaskAssignmentDelegate.java:22-86）只写 process 变量与日志，不落库 sf_spec_review；④ HumanTaskAssignmentDelegate 无任何轮询/角色解析算法，纯日志记录。

**Searched:**（absent 必填）
- `assign|PENDING|round.?robin|轮询`（schemaplexai-spec/src/main，忽略大小写）→ 1 命中：application.yml:27 `id-type: assign_id`（无关）。
- `specReviewApproval|spec-review-approval`（全仓 *.java/*.ts/*.yml/*.xml）→ 3 命中，全部在 workflow 模块内部（BPMN:5,7 + Javadoc 示例 WorkflowDeployService:126），无 spec 域触发方。
- DB 默认 `status VARCHAR(32) NOT NULL DEFAULT 'PENDING'`（01-init-schema.sql:195）存在，但 Java 代码无任何 PENDING 读写（submitReview 强制要求调用方传 status，:32-34）。

**How the verdict was reached:** 分配能力三种策略在可达路径上全无；workflow 模块的 BPMN 是未接线的平行制品（无触发、无落库、无算法），不满足"以不同机制实现同一性质"的条件；DB 留了 PENDING 默认值但代码从未使用，进一步证明工作流未实现而非换机制实现。故 absent 而非 partial。

**Severity:** Medium（评审流程退化为"任何人可代表任何 reviewerId 一步定结论"，无独立性保证）。

---

## REQ-10 — 评审三分支语义：APPROVED→通过发布 / CHANGES_REQUESTED→返回修改 / REJECTED→结束流程

> "APPROVED    CHANGES_REQUESTED   REJECTED │ ... ▼ 通过发布      返回修改          结束流程" — spec-management.md §4.1

**Verdict:** contradicted · confidence: high

**What this demands:** 三种评审决策产生三种**不同**的后续状态；REJECTED 是终态（结束流程），与 CHANGES_REQUESTED（回到修改）语义必须区分。

**Where enforcement lives:** `SpecReviewServiceImpl.java:47-54`：

```java
if ("REJECTED".equalsIgnoreCase(status) || "CHANGES_REQUESTED".equalsIgnoreCase(status)) {
    spec.setStatus("CHANGES_REQUESTED");
} else if ("APPROVED".equalsIgnoreCase(status)) {
    spec.setStatus("APPROVED");
}
```

**Paths walked:**
- ✓ APPROVED → spec.status="APPROVED"（与 §3.1 状态吻合，之后可（无门禁地）publish）。
- ✗ REJECTED → spec.status="CHANGES_REQUESTED"——**驳回被折叠为返回修改**，无终止/关闭语义；`SpecReviewServiceImplTest.java:104-116`（断言位于 :115 `assertThat(spec.getStatus()).isEqualTo("CHANGES_REQUESTED")`）固化了这一行为，证明是有意实现而非笔误。
- ✗ 决策值无白名单：status 传 "BANANA" → review 照常入库（:40-45），spec.status 不变——第四条未定义分支静默通过。
- ✗ 评审对象无状态前置：对 draft/archived/published 任何状态的 Spec 均可提交评审。
- ✓ 旁路核查（新发现）：workflow 模块的 `spec-review-approval.bpmn20.xml` 恰好建模了正确的三分支——`APPROVE`→notifyApprovalTask→approvedEndEvent（:106-108,134）、`REJECT`→notifyRejectionTask→**rejectedEndEvent 终态**（:109-111,135）、`REVISE`→reviseTask→回到 primaryReviewTask（:112-114,119）——与 spec §4.1 语义一致。但该流程与 /spec/reviews API 完全未接线（证据见 REQ-09 ①-④），不改变可达路径上的矛盾裁决；它只证明正确语义在本仓库曾以流程图形式存在。

**Searched:** 不适用（contradicted，执行点已直接引用；旁路流程的核查记录见 REQ-09）。

**How the verdict was reached:** 代码在被违反路径上主动写入与 spec 相反的语义（REJECTED ≠ 结束，而是等同 CHANGES_REQUESTED），且有测试固化——是明确的 contradicted，非 partial；未接线的 BPMN 正确语义不构成可达执行点。

**Severity:** High（"驳回"这一治理终局在系统中不存在，被驳回的 Spec 与被要求修改的 Spec 不可区分，可无限重提）。

---

## REQ-11 — sf_spec_review 数据模型（version_id / decision / line_numbers / 待处理-已解决状态）

> "| version_id | BIGINT | 关联版本 | ... | decision | VARCHAR | APPROVED / REJECTED / CHANGES_REQUESTED | ... | line_numbers | VARCHAR | 关联行号（如 \"10-15,20\"） | | status | VARCHAR | 待处理 / 已解决 |" — spec-management.md §4.2

**Verdict:** partial · confidence: high

**What this demands:** 评审记录锚定到具体版本与行号，决策（decision）与处理状态（status）是两个独立字段。

**Where enforcement lives:**
- 存在的列：`01-init-schema.sql:190-200` — id/tenant_id/spec_id/reviewer_id/status（DEFAULT 'PENDING'）/comment(+审计列)。
- 实体：`SfSpecReview.java:13-16` — specId/reviewerId/status/comment。
- 缺失：`version_id`、`decision`、`line_numbers` 三列在 DDL 与实体中均不存在（实读 01-init-schema.sql:190-200 与实体全文件确认）；代码把决策值（APPROVED/REJECTED/CHANGES_REQUESTED）写进 `status`（SpecReviewServiceImpl:43），导致 spec 定义的"待处理/已解决"处理状态无处安放。

**Paths walked:**
- ✓ 评审记录可创建、可查询（SpecReviewController CRUD 全通，:22-58）。
- ✗ 按版本追溯评审 → 无 version_id，不可能。
- ✗ 行级评审意见 → 无 line_numbers，不可能。
- ✗ decision 与 status 分离 → 一列两用，互斥。

**Searched:**（partial 必填）
- `line_numbers|lineNumbers`（全仓 *.java/*.sql/*.yml）→ 0 命中。
- `version_id`（sf_spec_review 上下文，01-init-schema.sql:190-200 实读）→ 0 列。

**How the verdict was reached:** 表和主干字段存在（spec_id/reviewer_id/comments→comment/status），锚定与状态分离字段缺失——按"partial 写明缺失路径"处理；status 一列两用是 REQ-10 折叠语义的存储层根因。

**Severity:** Medium（评审证据链断裂：意见无法对应到被评审的版本与行；"已解决"闭环无法记录）。

---

## REQ-12 — 变更追踪 sf_spec_change（ADD/MODIFY/DELETE 字段级审计）

> "**sf_spec_change**: | 字段 | 类型 | 说明 | ... | change_type | VARCHAR | ADD / MODIFY / DELETE | | field_name | VARCHAR | 变更字段 | | old_value | TEXT | 旧值 | | new_value | TEXT | 新值 | ..." — spec-management.md §4.3

**Verdict:** absent · confidence: high

**What this demands:** 每次 Spec 变更产生字段级审计记录（谁、何时、改了哪个字段、旧值新值）。

**Where enforcement lives:** 无——无表、无实体、无 Mapper、无写入点。

**Paths walked:**
- ✗ 更新 Spec → `SpecController.update():34-38` 直通 updateById，无任何变更记录旁路。
- ✗ 发布/归档/评审改状态 → 同样零记录。

**Searched:**（absent 必填）
- `sf_spec_change|SpecChange|change_type`（全仓）→ 7 命中全部在 docs/（specs/plans/designs/decisions/archive），**0 代码 / 0 SQL 命中**；sf_spec 域仅 6 张表（01-init-schema.sql:127-200 实读：sf_spec / sf_spec_document / sf_spec_version / sf_spec_template / sf_spec_steering / sf_spec_review），无 change 表。
- `field_name|old_value`（docker/**/*.sql）→ 0 命中。

**How the verdict was reached:** 三个层（DDL/实体/服务）全部落空，能力整体缺失。change_log 字段（sf_spec_version.change_log，01-init-schema.sql:161）只是版本级自由文本备注，不构成字段级审计的替代机制。

**Severity:** High（叠加 REQ-03 可任意编辑 + REQ-07 无权限：变更既不受限也不可追溯，企业治理场景的核心审计能力缺位）。

---

## REQ-13 — Steering 是 Spec 的子类型（Markdown 行为约束文档）

> "**Steering** 是 Spec 的子类型，专门用于约束 Agent 行为。" — spec-management.md §5

**Verdict:** contradicted · confidence: high

**What this demands:** Steering 作为一种 Spec（type=STEERING 的文档），内容为 Markdown（角色定义/输出格式/安全约束/示例）。

**Where enforcement lives:**
- 实际实现是**独立实体**：`SfSpecSteering.java:11-17` — `specId / direction / constraints / acceptanceCriteria` 三段结构化文本，**从属于某个 Spec**（spec_id NOT NULL，01-init-schema.sql:178-188），不是 Spec 的子类型，而是 Spec 的附属规则记录。
- 佐证矛盾：DB 注释 `sf_spec.type -- REQUIREMENT / DESIGN / TASK / STEERING`（01-init-schema.sql:131）保留了"STEERING 是 spec 类型"的痕迹，但 `/spec/steerings` 全部 API（SpecSteeringController.java）只操作 SfSpecSteering 表，二者不相通。
- 语义走向不同：`applySteering():59-88` 把 direction/constraints/criteria 以 HTML 注释**追加进 Spec 内容**；`evaluateSteeringRules():30-56` 用 `content.contains()` 做子串匹配判定——这是"对 Spec 文本做规则标注"，与"约束 Agent 行为的文档"是两个概念。

**Paths walked:**
- ✓ 创建/查询/校验 Steering → 针对独立表，可用（SpecSteeringController:25-88）。
- ✗ 创建 type=STEERING 的 Spec 并让 /spec/steerings 端点感知它 → 两套数据互不相通（/spec/steerings 按 specId 查 SfSpecSteering，从不查 SfSpec.type）。

**Searched:** `SfSpecSteering` 全部使用点（模块内）→ 仅 SpecSteeringController / SpecSteeringService(Impl) / Mapper，无与 SfSpec.type 的关联逻辑。

**How the verdict was reached:** 不是"不同机制满足同一性质"：spec 要求的性质是"Steering 文档承载 Agent 行为约束"，实现物是"Spec 内容的规则附注"，性质本身变了；且数据建模方向（子类型 vs 附属子表）相反，故 contradicted 而非 implemented-with-drift。

**Severity:** Medium（概念错位导致 §5 的全部下游设计——文档结构、与 Agent 绑定——失去载体；单看本条不直接造成运行故障）。

---

## REQ-14 — Steering 与 Agent 绑定并注入 System Prompt

> "// Agent 配置时选择关联的 Steering 文档 SfAgentConfig config = new SfAgentConfig(); config.setSteeringId(steeringId); // 执行时，Steering 内容作为 System Prompt 的一部分注入" — spec-management.md §5

**Verdict:** absent · confidence: high

**What this demands:** SfAgentConfig 持有 steeringId；Agent 执行时将 Steering 内容并入 System Prompt。

**Where enforcement lives:** 无。`schemaplexai-agent-config/.../entity/SfAgentConfig.java` 实读：字段为 agentId / maxRounds / maxTools / maxInputTokens / maxOutputTokens / systemPrompt / modelId / temperature / executionMode——**无 steeringId**。

**Paths walked:**
- ✗ Agent 配置关联 Steering → 无字段、无 API。
- ✗ 执行期注入 → agent-engine 无引用（见下搜索）。

**Searched:**（absent 必填）
- `steeringId|SteeringId`（全仓 *.java）→ 命中全部在 schemaplexai-spec 模块内部（validateSteeringConfig 等），agent-config / agent-engine / model 0 命中。
- `steering`（schemaplexai-agent-engine，忽略大小写）→ 0 命中。
- `SfAgentConfig`（实体实读）→ 无相关字段。

**How the verdict was reached:** 绑定字段与注入逻辑两端都不存在，跨模块能力整体缺失。spec 引文以代码示例形式写出（`config.setSteeringId(...)`），属强制性接口承诺而非示意。

**Severity:** High（Steering 的既定用途——约束 Agent 行为——完全无法达成，§5 能力名存实亡）。

---

## REQ-15 — Spec 管理 API 契约（§6.1）

> "POST /spec/specs # 创建 Spec / GET /spec/specs # 列表 / GET /spec/specs/{id} # 详情 / PUT /spec/specs/{id} # 更新 / DELETE /spec/specs/{id} # 删除 / POST /spec/specs/{id}/publish # 发布 / POST /spec/specs/{id}/rollback # 回滚" — spec-management.md §6.1

**Verdict:** partial · confidence: high

**What this demands:** 7 个端点按字面路径可用（网关前缀 /spec/** 已由 GatewayConfig.java:26-27 路由到本服务 ✓）。

**Where enforcement lives / Paths walked:**（SpecController.java）
- ✓ POST /spec/specs（:27-31）
- ✗ GET /spec/specs → 实为 GET /spec/specs/page（:57-62）；字面路径 404。列表能力存在（文档漂移），但无按 type/status 过滤。
- ✓ GET /spec/specs/{id}（:47-54）
- ✓ PUT /spec/specs/{id}（:34-38）
- ✓ DELETE /spec/specs/{id}（:41-44，逻辑删除：BaseEntity @TableLogic + application.yml `logic-delete-value: 1`）
- ✓ POST /spec/specs/{id}/publish（:65-68）
- ✗ POST /spec/specs/{id}/rollback → 不存在（见 REQ-06），同位置为 /archive（:70-74）。

**Searched:** SpecController.java 全文件实读（共 9 端点）；前端消费方核查：`schemaplexai-ui/src/api/spec.ts:14-32` 仅调用 page / {id} / POST / PUT / DELETE 五端点，未调用 rollback（无前端 404 消费方，但 rollback 缺失不因无人调用而减轻）。

**How the verdict was reached:** 7 条契约中 5 条字面成立、1 条改道（列表→/page，能力在）、1 条整体缺失（rollback），故 partial。

**Severity:** Medium（rollback 缺失为主要扣分项，其余为 Low 级文档漂移）。

---

## REQ-16 — 版本管理 API 契约（§6.2）

> "POST /spec/versions # 创建新版本 / GET /spec/versions?specId={id} # 版本列表 / GET /spec/versions/{id} # 版本详情 / GET /spec/versions/diff?a={id}&b={id} # 版本对比" — spec-management.md §6.2

**Verdict:** partial · confidence: high

**Where enforcement lives / Paths walked:**（SpecVersionController.java）
- ✓ POST /spec/versions（:23-28，裸实体保存；另有语义化 POST /spec/versions/publish :67-74）
- ✗ GET /spec/versions?specId={id} → 实为 GET /spec/versions/page（:53-59），且 `PageParam` 仅含 current/size（PageParam.java 实读，仅两个字段 +14 行），**不支持 specId**——分页返回全部版本混排，"按 Spec 查版本列表"这一明示能力缺失（仅 SpecController /{id}/latest-version :77-81 与 /{id}/compare :84-89 提供受限替代）。
- ✓ GET /spec/versions/{id}（:44-51）
- ✓ GET /spec/versions/diff → 存在（:62-65），参数名为 `versionAId/versionBId` 而非 `a/b`（文档漂移）。

**Searched:**（partial 必填）
- `specId`（SpecVersionController.java 实读）→ 仅 /publish 端点使用（:69）；page 端点 0 使用。

**How the verdict was reached:** 4 条中 3 条能力成立（1 条参数名漂移），"按 specId 过滤版本列表"路径缺失，故 partial。

**Severity:** Medium（版本列表不可按 Spec 过滤，前端只能全量拉取后自筛；兼有数据量增长后的性能问题，见 REQ-20 索引缺失）。

---

## REQ-17 — 评审 API 契约（§6.3）

> "POST /spec/reviews # 提交评审 / GET /spec/reviews?specId={id} # 评审列表 / PUT /spec/reviews/{id}/resolve # 解决评审意见" — spec-management.md §6.3

**Verdict:** partial · confidence: high

**Where enforcement lives / Paths walked:**（SpecReviewController.java）
- ✓ POST /spec/reviews（:22-27 裸创建；语义化端点为 POST /spec/reviews/submit :60-67）
- ✗ GET /spec/reviews?specId={id} → 实为 /page（:52-58）且 PageParam 无 specId 字段——按 Spec 查评审不可行。
- ✗ PUT /spec/reviews/{id}/resolve → 不存在。通用 PUT /{id}（:29-34）可改任意字段，但"解决"语义（待处理→已解决）因 status 列被决策值占用（REQ-11）而无法表达。

**Searched:**（partial 必填）
- `resolve|Resolve`（schemaplexai-spec/src/main）→ 0 命中。
- `specId`（SpecReviewController.java 实读）→ 仅 /submit 使用（:62）。

**How the verdict was reached:** 3 条中 1 条成立、1 条过滤能力缺失、1 条端点及其语义均缺失，故 partial（resolve 单看是 absent，并入本契约条目并已附搜索记录）。

**Severity:** Medium（评审意见无闭环状态，列表不可按 Spec 定位）。

---

## REQ-18 — Steering API 契约（§6.4）

> "POST /spec/steerings # 创建 Steering / GET /spec/steerings # 列表 / PUT /spec/steerings/{id} # 更新" — spec-management.md §6.4

**Verdict:** implemented · confidence: high

**Where enforcement lives / Paths walked:**（SpecSteeringController.java）
- ✓ POST /spec/steerings（:25-30）
- ✓ 列表 → GET /spec/steerings/page（:55-61，路径漂移同前，能力在）
- ✓ PUT /spec/steerings/{id}（:32-37）

**Searched:** 不适用。

**How the verdict was reached:** 三端点能力齐备，仅列表路径 /page 漂移，按"不同机制满足=implemented+注记"处理。注意：端点背后的 Steering **语义**偏差已在 REQ-13/14 单独裁决，此处仅评 API 面。

**Severity:** Low（修文档）。

---

## REQ-19 — 非功能：Diff 计算延迟 < 1s（文档 < 10MB）

> "| Diff 计算延迟 | < 1s（文档 < 10MB） |" — spec-management.md §7

**Verdict:** contradicted · confidence: medium

**What this demands:** 10MB 以内文档的 diff 在 1 秒内完成。

**Where enforcement lives:** `SpecDiffUtil.computeLcs():20-32` — `int[][] dp = new int[a.length + 1][b.length + 1]`，O(N×M) 时间与**内存**；`diff():10-17` 与 `SpecVersionServiceImpl.diff():30-50` 均无文档大小防护。

**Paths walked:**
- ✓ 小文档 → 毫秒级（SpecDiffUtilTest 覆盖功能正确性，无性能断言）。
- ✗ 10MB 边界：按 50 字节/行估算 ≈ 20 万行，dp 矩阵达 10^10 量级 int 单元 → 必然 OOM，遑论 1s。即便 1KB/行（1 万行）矩阵也达数百 MB/次调用。

**Searched:** `maxSize|MAX_.*SIZE|length\(\)`（SpecDiffUtil.java / SpecVersionServiceImpl.java 实读全文）→ 无任何大小上限守卫。

**How the verdict was reached:** 无法运行基准（confidence 降为 medium），但内存复杂度是静态可证的：在 spec 允诺的输入域（<10MB）内存在必然失败的子域，且无守卫将其拒之门外——这不是"未验证"，而是结构性不满足，故 contradicted 而非 undecidable。

**Severity:** Medium（当前数据量下不触发；一旦出现大文档即 OOM 拖垮整个 8086 服务）。

---

## REQ-20 — 非功能：版本列表查询 P99 < 200ms

> "| 版本列表查询 | P99 < 200ms |" — spec-management.md §7

**Verdict:** undecidable · confidence: low

**What this demands:** 版本列表接口 P99 延迟 < 200ms。

**Where enforcement lives:** 无性能测试、无基准、无 SLO 监控告警配置指向该指标。

**Paths walked:**
- ✗ 无法在静态核查中测得 P99。
- 风险线索（非结论）：`01-init-schema.sql:203-206` 仅建 4 个索引（idx_user_tenant / idx_role_tenant / idx_spec_tenant / idx_model_tenant），**sf_spec_version 无任何二级索引**（spec_id/tenant_id 均无）；publishSpec/getLatestVersion 按 spec_id+tenant_id 过滤并排序（SpecServiceImpl:47-53）在数据增长后将全表扫。

**Searched:**
- `P99|percentile|benchmark|jmh`（schemaplexai-spec）→ 0 命中。
- `CREATE INDEX`（01-init-schema.sql 实读 :203-206）→ 4 条，无 sf_spec_version。

**How the verdict was reached:** 指标本质需运行时测量，代码层既无违反的确证也无满足的证据，如实记 undecidable；索引缺失作为风险注记而非裁决依据。

**Severity:** Low（建议：补 `sf_spec_version(spec_id)` 索引 + 建立性能基线后复核）。

---

## REQ-21 — 非功能：并发编辑乐观锁（version 字段）

> "| 并发编辑 | 乐观锁（version 字段） |" — spec-management.md §7

**Verdict:** absent · confidence: high

**What this demands:** Spec 更新使用 version 字段做乐观并发控制，过期写入被拒绝。

**Where enforcement lives:** 仅 DB 有列：`sf_spec.version INT NOT NULL DEFAULT 1`（01-init-schema.sql:134）。三处必要件全缺：
1. `SfSpec.java:11-17` — 实体未映射 version 字段；
2. 全仓 `@Version` 注解 → 仅 `schemaplexai-system/.../entity/TenantPolicy.java:26` 一处（他域）；
3. `MyBatisPlusConfig.java:17-23`（spec 模块）— 仅注册 TenantLine + Pagination 拦截器（:20-21），**无 OptimisticLockerInnerInterceptor**（对照：agent-engine 的 MyBatisPlusConfig.java:4,14 注册了它——项目会用该机制，spec 模块没接）。

**Paths walked:**
- ✗ 并发 PUT /spec/specs/{id} → 两个写入都成功，last-write-wins，静默丢失先写内容。

**Searched:**（absent 必填）
- `@Version|OptimisticLocker`（全仓 *.java）→ 3 命中：TenantPolicy.java:26、agent-engine MyBatisPlusConfig.java:4（import）,:14（注册）——spec 模块 0 命中。
- `version`（SfSpec.java 实读）→ 无该属性。

**How the verdict was reached:** DB 预留了列但 ORM 层三件套（字段映射/@Version/拦截器）全部缺失，乐观锁在运行时完全不生效——能力缺失记 absent；未记 partial 因为"只有列没有机制"不构成任何一条生效路径。

**Severity:** High（协作编辑场景必然发生静默覆盖；且因 REQ-12 无变更追踪，丢失不可发现不可恢复）。

---

## notChecked

- 无强制项遗漏（21 条已覆盖全部规范性条款）。以下为非强制信息性内容，未列 REQ：
  - §8 相关文档引用：`docs/plans/project-plan.md`、`docs/plans/unified-dev-plan.md` 在仓库中的实际文件名为 `docs/plans/2026-04-29-v1.0-project-plan.md`、`docs/plans/2026-04-30-v1.0-unified-dev-plan.md`（文件名漂移，Low，修文档）。
  - spec frontmatter `status: 草稿`（L5）与任务声明"已批准"不一致（元数据漂移，Low）。

## 反向差距（模块存在但 spec 未提及的重要行为）

1. **归档能力**：POST /spec/specs/{id}/archive + "archived" 状态（SpecController:70-74, SpecServiceImpl:77-88）——spec 生命周期无此状态。
2. **最新版本查询**：GET /spec/specs/{id}/latest-version（SpecController:76-81）。
3. **重复的版本对比端点**：GET /spec/specs/{id}/compare 按版本号串返回实体列表（SpecController:83-89），与 /spec/versions/diff 能力重叠但返回形态不同（实体列表 vs hunks）。
4. **模板 API 全集**：/spec/templates CRUD + /{id}/apply + /default + /by-category + /{id}/clone（SpecTemplateController 全文件）——spec §2 提及 TEMPLATE 类型但从未定义模板 API。
5. **Steering 扩展端点**：/spec/steerings 的 evaluate、apply、active、/{id}/validate 四个端点及其"子串匹配+HTML 注释注入"语义（SpecSteeringController:63-87, SpecSteeringServiceImpl:30-119）——spec 完全未提及。
6. **死代码**：sf_spec_document 表（01-init-schema.sql:142-152）+ SfSpecDocument 实体 + SfSpecDocumentMapper——全仓 grep `SfSpecDocument` 仅 3 处命中（实体自身 + mapper 两处），无任何服务/控制器引用。
7. **POST /spec/versions/publish 的副作用**：名为 publish 实为 createVersion，且会覆盖 spec.content 并把状态置为 spec 外值 "ACTIVE"（SpecVersionController:67-74 → SpecVersionServiceImpl:72-75）。
8. **双保险租户隔离**：TenantLineInnerInterceptor 全局生效（MyBatisPlusConfig:20）之外，服务层再手工拼 tenantId 条件（SpecServiceImpl:46-51 等）——行为正确但冗余，spec 未提。
9. **审计字段填充缺失**：全仓无 MetaObjectHandler 实现（`grep -l MetaObjectHandler` → 0 命中），BaseEntity 的 tenantId/createdAt/updatedAt/createdBy/updatedBy `@TableField(fill=...)` 永不自动填充；依赖 TenantLineInterceptor 注入租户、其余审计字段恒为 null。
10. **未接线的 Flowable 评审流程**（本次复核新发现）：`schemaplexai-workflow/src/main/resources/processes/spec-review-approval.bpmn20.xml` 定义了完整的 Spec 评审审批流程（风险分级自动通过 / 主审 / 升级评审 / 修订回路 / 驳回终态），配套 SpecReviewInitDelegate、SpecReviewNotificationDelegate（含 MQ 通知发布）、HumanTaskAssignmentDelegate——但无任何代码从 spec 域触发它，也不写回 sf_spec / sf_spec_review。它是 spec §4.1 的一个更完整的平行实现雏形，当前为孤立资产。

## Open questions

1. sf_spec.type 的权威类型集到底是 §2 的 SPEC/STEERING/TEMPLATE 还是 DDL 注释的 REQUIREMENT/DESIGN/TASK/STEERING？两处均无代码强制，需产品定夺后择一修正（REQ-01）。
2. "版本列表查询 P99 < 200ms" 需运行基准才能裁决（REQ-20）；建议先补 `sf_spec_version(spec_id)` 索引再测。
3. CHANGELOG.md:113 "spec 模块无测试覆盖" 与实际 12 个测试文件矛盾——CHANGELOG 需更新（文档错、代码对，Low）。
4. workflow 模块的 spec-review-approval BPMN 是否为 §4.1 的目标载体？若是，需补齐 spec 模块→流程触发→状态写回链路；若否，应明确废弃（反向差距 #10）。

## 反驳复核

> 独立复核员（非原作者）对 8 条 severity=High 且 verdict ∈ {contradicted, partial, absent} 的条目逐条尝试推翻。代码方向：重读被引代码行，排查网关/基类/角色拦截器/全局乐观锁拦截器/别名端点/其他 SQL 初始化脚本/agent-config 模块等替代执行点；规格方向：重读对应段落确认原引用是否误读或非强制。结论：8 条全部维持，0 条推翻。

| REQ | 原判 | 裁定 | 理由（一行） | 证据 |
|---|---|---|---|---|
| REQ-02 | contradicted · High | 维持 | 状态校验不在网关（网关仅认证）、不在基类/拦截器；01 脚本对 sf_spec.status 无 CHECK/触发器；workflow 的 setStatus 全写本域实体不写回 sf_spec；IN_REVIEW 仅存在于 quality 域整型常量，代码状态集与 §3.1 五态链冲突成立 | 01-init-schema.sql:127-140 无约束实读；JwtAuthFilter.java:63-112（仅认证）；WorkflowInstanceServiceImpl.java:57-92；quality/ReviewServiceImpl.java:24；spec §3.1 为规范性生命周期定义 |
| REQ-03 | absent · High | 维持 | 编辑守卫不在任何层：spec 模块 main 中 getStatus() 0 调用、@Aspect/HandlerInterceptor/ControllerAdvice 0 命中，网关不读状态，DB 无触发器，两处 update 均直通 updateById | SpecController.java:34-38、SpecVersionController.java:31-35 实读；grep getStatus()=0；grep @Aspect\|@Around\|HandlerInterceptor\|ControllerAdvice（spec main）=0；spec §3.1"可编辑"列为强制矩阵 |
| REQ-06 | absent · High | 维持 | 无别名端点：全仓 main 中 rollback/revert/restore 仅命中 spec 模块 5 处 @Transactional(rollbackFor) 与 context 域 restoreFromSnapshot（上下文快照，与 Spec 版本无关）；网关纯路径路由无改写，/archive 语义≠回滚 | grep rollback\|revert\|restore（schemaplexai-spec）=5×rollbackFor；GatewayConfig.java:26-27；SpecService 接口实读无 rollback；spec §3.2+§6.1 明示 POST /spec/specs/{id}/rollback |
| REQ-07 | absent · High | 维持 | 权限不在角色拦截器：网关全模块 0 处 role/permission/authority 逻辑，@EnableMethodSecurity 仅 web/agent-config 启用，spec 模块 pom 无 spring-security 依赖，system 的 PermissionEvaluator 未被 spec 依赖或引用 | grep PreAuthorize\|EnableMethodSecurity 全仓（仅 agent-config/web 命中）；schemaplexai-spec/pom.xml 依赖清单实读；grep role\|permission\|authority（gateway main）=0；spec §3.2"权限"列为强制要求 |
| REQ-10 | contradicted · High | 维持 | REJECTED 折叠为 CHANGES_REQUESTED 经实读确认且被测试固化；BPMN 虽建模正确三分支但完全未接线（spec 模块 pom 无 amqp 依赖、无触发方，workflow 不写回 sf_spec），不构成可达执行点 | SpecReviewServiceImpl.java:48-54 实读；SpecReviewServiceImplTest.java:104-117（:115 断言固化）；schemaplexai-spec/pom.xml 无 amqp；spec §4.1 三分支语义为流程规范 |
| REQ-12 | absent · High | 维持 | 变更追踪表不在其他初始化脚本：docker 全部脚本（01/02/03/04/009+ClickHouse）CREATE TABLE 枚举无 sf_spec_change；quality 域 sf_audit_event 为通用审计，spec 模块零引用且无 MQ 生产依赖 | CREATE TABLE 全量枚举（01/02/03/04/009）；grep spec_change\|change_type\|field_name\|old_value（*.sql）=0；schemaplexai-spec/pom.xml 无 amqp；spec §4.3 强制定义该表 |
| REQ-14 | absent · High | 维持 | Steering 绑定不在 agent-config：SfAgentConfig 实体与 sf_agent_config DDL 均无 steeringId/steering_id，8 个外围模块 main 代码 steering 命中 0；唯一反驳点（§5 代码示例或为示意伪码）不成立——即使从宽解读，绑定与 System Prompt 注入两端能力仍均不存在 | SfAgentConfig.java:11-32 实读；02-init-schema-agent.sql:21-37 实读；grep steering（全仓 main）仅 schemaplexai-spec 命中；spec §2/§5 明示 Steering 用途为约束 Agent 行为 |
| REQ-21 | absent · High | 维持 | 乐观锁无全局拦截器兜底：OptimisticLockerInnerInterceptor 仅 agent-engine 注册，spec 模块 MyBatisPlusConfig 仅 TenantLine+Pagination，SfSpec/BaseEntity 无 version 字段与 @Version，MyBatis-Plus 无属性开关且 application.yml 无相关配置 | MyBatisPlusConfig.java:17-23（spec）实读；SfSpec.java:11-17、BaseEntity.java:14-39 实读；agent-engine/MyBatisPlusConfig.java:14；spec application.yml:19-30；spec §7 强制 |
