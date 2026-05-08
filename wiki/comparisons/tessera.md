<!-- AUTO-GENERATED: manual-maintained wiki at 2026-05-08T15:00:00Z -->
---
title: SchemaPlexAI vs Tessera (horang-labs)
type: decision
source: github.com/horang-labs/tessera + 4-perspective roundtable debate
creation_date: 2026-05-08
update_date: 2026-05-08
tags: [comparison, tessera, ide-workspace, provider-adapter, composer, skills-dashboard]
confidence: high
---

# SchemaPlexAI vs Tessera

> 一句话:Tessera 是单人开发者多 CLI Agent 桌面工作台;我们是企业多租户协作平台。定位不同 ⇒ 不照搬,只取其交集中**对企业 Agent 平台真正有意义**的概念。

## TL;DR

经 10 项候选 × 4 视角(架构师 / 产品体验工程师 / 务实工程师 / 企业平台架构师)圆桌辩论,**P0 三件、P1 三件、共识不做四件**。总投入约 3-4 周,可叠加在现有 MAF 路线图(`wiki/comparisons/microsoft-agent-framework.md`)之上。

| 共识等级 | 改进点 | 投入 |
|---------|--------|------|
| P0 必做 | Skills Dashboard UI | 2-3 天 |
| P0 必做 | Composer 富功能(@ 引用 + 文件附件 + 图片) | 3-5 天 |
| P0 必做 | Session Timeline 增强 + ClickHouse 持久化 | 3-5 天 |
| P1 应做 | List 视图(补足 Kanban) | 1-2 天 |
| P1 应做 | External Agent SPI(单一 PoC) | 5-7 天 |
| P1 应做 | Task 级 Logical Branch 隔离 | 3-5 天 |
| ❌ 不做 | Per-Task 物理 Git Worktree | — |
| ❌ 不做 | 多 panel 拖拽并行 UI | — |
| ❌ 不做 | Web Speech 语音输入(优先级低) | — |
| ❌ 不做 | 完整 Claude/Codex/OpenCode provider 矩阵 | — |

---

## 1. 定位差异(避免盲目对标)

| 维度 | Tessera | SchemaPlexAI |
|------|---------|--------------|
| 用户 | 单人开发者 | 企业团队 / 多租户 |
| 部署 | 本地优先(Electron + npm CLI) | 服务端(Spring Cloud + 12 微服务) |
| Agent 来源 | 外部 CLI 包装(Claude Code / Codex / OpenCode) | 内置 LangChain4j + Tool / MCP / A2A |
| 数据 | SQLite 本地 + `~/.tessera/` | PostgreSQL + ClickHouse + Milvus + MinIO |
| 协作 | 单人多任务并行 | 多用户 RBAC + Spec/Quality Gate |
| 计费 | 免费开源 | 多租户 Cost Budget(ClickHouse 分析) |
| 协议归一化 | Claude `stream-json` / Codex `app-server` / OpenCode ACP JSON-RPC | LangChain4j 统一 + A2A protocol |
| 安全模型 | Provider-native(借用 CLI 自带审批) | 平台级 Sandbox + Guardrails + ApprovalService |

**关键洞察:** Tessera 是"workstation",我们是"platform"。它解决的是**开发者把多个 CLI 黑盒工作流搬进可视化界面**;我们解决的是**企业把 AI 研发协作纳入治理 + 计费 + 审计**。所以 80% 的 Tessera 特性不能直接照搬,必须经过"企业化转换"。

---

## 2. 候选特性清单(10 项)

| # | Tessera 特性 | 我们当前能力 | 差距 |
|---|-------------|-------------|------|
| 1 | Provider Adapter + 协议归一化层 | `ToolAdapter`(file/http)、`A2aClient` | 缺**外部 CLI Agent** adapter |
| 2 | Chat → Task 演进流(无缝 worktree) | `ChatMemory` + `AgentExecutor`(独立) | 缺无缝过渡 |
| 3 | List + Kanban 双视图 | `Hive/KanbanBoard` ✓ | 缺 List 视图 |
| 4 | Skills Dashboard(自动发现 + 浏览) | `SkillMarkdownParser` + `SkillRegistry`(后端) | 缺 UI Dashboard |
| 5 | Composer 富功能(@ 引用 / 图片 / 附件 / 语音) | 基础输入框 | 缺 @ 引用 + 多模态 |
| 6 | Session Timeline(输出/推理/工具/计划/diff/PR) | `SseViewer`(基础流) | 缺富时间线 |
| 7 | Per-Task Git Worktree 隔离 | 无 | 完全缺失 |
| 8 | 多 Agent 并行工作空间 + 拖拽 | `AgentList` 单页 | 无多 panel |
| 9 | Provider-native 控制(审批/沙箱) | `ApprovalService`(后端) | UI 待补 |
| 10 | OpenCode 模型桥接(本地/离网 LLM) | LangChain4j multi-provider | 无离网包装 |

---

## 3. 圆桌辩论(4 视角)

### 视角 A:架构师

> 关注点:模块边界、扩展点、与现有 SPI 协调

**强支持(2):**
- **#1 Provider Adapter for 外部 CLI** — 我们已有 `ToolAdapter` 和 `A2aClient`,加 `ExternalAgentAdapter` SPI 是自然演进。但**只做接口,不做具体 CLI 实现**。
- **#6 Session Timeline 抽象** — `SseViewer` 太薄;timeline 应该是"agent reasoning provenance"的统一抽象,服务于审计与调试两个场景。

**条件支持(1):**
- **#7 Worktree 隔离** — 反对**物理** worktree(我们是服务端,不是 IDE);**支持 logical branch 隔离**,放在 `schemaplexai-integration/git` 服务里。

**反对(1):**
- **#8 多 panel 拖拽** — 与现有 React Router 单页结构冲突,改造成本高、价值不明。

**关键担忧:** 协议归一化层会与已有 A2A protocol 冲突。**必须明确边界**:A2A 是平台**内** agent-to-agent;ExternalAgentAdapter 是平台**对外**包装第三方 CLI。两者不应混入同一抽象。

---

### 视角 B:产品体验工程师

> 关注点:用户能"看见 / 操作 / 信任"agent 的程度

**强支持(3):**
- **#5 Composer 富功能** — @ 引用文件 / 会话、粘贴图片、附件,直接提升 `AgentExecutor` 体验。语音输入可后置。
- **#4 Skills Dashboard UI** — 我们后端有 `SkillMarkdownParser` 但用户不可见,这是巨大的 UX 漏洞。**没有 UI 的 Skills 等于没有 Skills**。
- **#6 Session Timeline** — 用户能"看见"agent 的推理 / 工具 / 审批,**信任感与可调试性双提升**。这是 Tessera 最值得借鉴的产品决策。

**条件支持(2):**
- **#3 List + Kanban 双视图** — 我们已有 Kanban,加 List 是 1-2 天;但任务分类逻辑要先想好。
- **#9 Provider-native 审批 UI** — `ApprovalService` 后端就绪,前端要补 modal + 实时通知。

**反对(1):**
- **#8 多 panel 并行** — 我们的 `AgentExecutor` 已经支持单 agent 长会话深度协作,多 panel **破坏现有产品定位**。企业用户需要的是"**agent 间**协作",不是"用户切 panel"。

**关键洞察:** Tessera 的强项是把"agent 黑盒"打开给开发者看;我们的强项是"平台治理"。两者结合 ⇒ Timeline + Skills Dashboard + Composer 三件套是 ROI 最高的产品改进。

---

### 视角 C:务实工程师

> 关注点:ROI、可实施性、能马上做什么

**强支持(3):**
- **#4 Skills Dashboard** — 后端已齐备,前端就是"列表 + 详情 + 搜索",**~2-3 天**。
- **#5 Composer 富功能** — Ant Design 5 已有完整组件,@ 引用 + 粘贴附件 **~3-5 天**(图片/附件存储复用 MinIO)。
- **#6 Session Timeline 增强** — `SseViewer` 加结构化事件类型 + 时间线 UI **~3-5 天**(后端事件结构已经够用)。

**条件支持(2):**
- **#1 External Agent SPI(PoC)** — 不要一开始就做归一化层(YAGNI)。先做**单一 CLI**(推荐 Codex,因为 OAI app-server 协议最稳定)做 PoC,**~5-7 天**。归一化层等第二个 provider 接入时再抽。
- **#3 List 视图** — 1-2 天加完。

**反对(2):**
- **#7 物理 Worktree** — 服务端没必要;改用 logical branch + 自动清理,~3-5 天。
- **#8 多 panel UI** — 复杂度极高、价值不明,放 backlog。

**关键洞察:** Tessera 80% 的"杀手锏"对我们是**前端工程**(Skills Dashboard + Composer + Timeline),不是**架构改造**。这意味着可以**两个前端工程师 1-2 周**完成 P0 三件套,ROI 极高。

---

### 视角 D:企业平台架构师

> 关注点:多租户、合规、审计、与企业定位的契合度

**强支持(2):**
- **#4 Skills Dashboard + 多租户 RBAC** — Skills 必须按 tenant 可见性管理;开源 Tessera 是单人无 RBAC,**我们必须加权限层**。
- **#6 Session Timeline + 审计合规** — Tessera timeline 是本地 SQLite;**我们必须持久化到 ClickHouse 做 audit trail**(此项已在 MAF 路线图 P1)。

**条件支持(2):**
- **#5 Composer 文件上传** — **必须接 MinIO + 病毒扫描 + 多租户配额**,否则就是"SSRF 漏洞 2.0"。已有 MinIO,只需加 ClamAV 集成或第三方扫描 SaaS。
- **#9 Provider-native 审批 UI** — 与现有 `ApprovalService` 一致,但**审批操作必须留 audit log**(谁在何时批准了什么工具调用)。

**强烈反对(2):**
- **#1 全量 External CLI Agent provider 矩阵** — Claude Code / Codex 需要**本地账号 + 个人订阅**,与多租户后端模型冲突;企业不会希望员工把代码送进外部 CLI agent **缺乏审计**的会话。**只做 SPI,不主推具体 provider**;具体集成留给企业按需自建或开源社区扩展。
- **#7 物理 Worktree** — 服务端磁盘空间 / 清理机制 / 多租户隔离三大坑。**logical branch + ephemeral container** 才是企业方案。

**关键担忧:** Tessera 的核心优势(本地优先 + 单人多 CLI)与我们的核心优势(多租户 + 平台治理)**几乎正交**。直接照搬 1-2 个特性会引入大量"非企业级"的隐性假设(无 RBAC / 无审计 / 无配额),**改造成本可能高于重写**。

---

### 视角投票矩阵

| 候选 | 架构师 | 产品 | 务实 | 平台 | 共识 |
|------|-------|------|------|------|------|
| #1 External Agent SPI | ✅ | ➖ | 🟡(只 PoC) | 🟡(只 SPI) | **P1: 只做 SPI + 单一 PoC** |
| #2 Chat→Task 演进 | ➖ | ➖ | ➖ | ➖ | 推迟,等 Multi-Agent 一起设计 |
| #3 List + Kanban | ➖ | 🟡 | ✅ | ➖ | **P1: 直接做** |
| #4 Skills Dashboard | ✅ | ✅ | ✅ | ✅ | **P0: 必做(全员一致)** |
| #5 Composer 富功能 | ➖ | ✅ | ✅ | 🟡(加病毒扫描) | **P0: 必做** |
| #6 Session Timeline | ✅ | ✅ | ✅ | ✅ | **P0: 必做(全员一致)** |
| #7 Worktree | 🟡(改 logical) | ➖ | ❌(物理) | ❌(物理) | **P1: 只做 logical branch** |
| #8 多 panel UI | ❌ | ❌ | ❌ | ➖ | **❌ 不做** |
| #9 审批 UI | ➖ | 🟡 | ➖ | 🟡(加 audit) | 已在 MAF 路线图,不重复 |
| #10 离网 LLM | ➖ | ➖ | ➖ | ➖ | 已有 multi-provider,跳过 |

✅ 强支持 / 🟡 条件支持 / ❌ 反对 / ➖ 中立

---

## 4. 共识改进点(6 项)

### P0 必做(1-2 周, ~10 人天)

#### #4 Skills Dashboard UI(~2-3 天)
- **现状**:`schemaplexai-integration/skill/SkillMarkdownParser` + `SkillRegistry` 后端就绪;前端无入口
- **目标**:在 `schemaplexai-ui/src/pages/Platform/IntegrationCenter` 下加 Skills Tab
- **要素**:
  - 列表视图(name / version / tags / description / 收藏)
  - 详情面板(渲染 markdown body,展示元数据)
  - 搜索 + tag 过滤
  - 多租户可见性(`tenantId` 过滤)
  - "用此 Skill 创建新会话"快捷入口
- **依赖**:无;后端 API 已就绪

#### #5 Composer 富功能 — 第一阶段(~3-5 天)
- **现状**:`AgentExecutor` 是基础 textarea
- **目标**:升级到带 @ 引用 + 附件 + 图片粘贴的 Composer
- **要素**:
  - **@ 引用**:`@file:` / `@chat:` / `@skill:` / `@agent:`(autocomplete 弹窗)
  - **附件上传**:Drag-and-drop / 点击上传 → MinIO + 病毒扫描(ClamAV)+ tenant 配额校验
  - **图片粘贴**:监听 `paste` 事件,自动上传 + 内联预览
  - **附件渲染**:消息流中显示缩略图 + 下载链接
- **依赖**:MinIO ✓;**新增**:ClamAV sidecar(docker-compose 加 service)
- **风险**:不接病毒扫描就是 P0 安全漏洞 ⇒ **必须先做扫描**

#### #6 Session Timeline 增强(~3-5 天)
- **现状**:`SseViewer` 是字符流,无结构化展现
- **目标**:把 agent 执行过程渲染为时间线
- **要素**:
  - 后端:`SseEventType` 枚举(`THOUGHT` / `TOOL_CALL` / `TOOL_RESULT` / `APPROVAL_REQ` / `APPROVAL_RESP` / `PLAN` / `FILE_DIFF` / `OUTPUT` / `ERROR`)
  - 前端:每事件类型独立卡片样式(图标 + 折叠 + 复制 + 时间戳)
  - **审计持久化**:每事件落 ClickHouse `agent_timeline_events` 表(MAF 路线图协同)
  - **Replay**:从 ClickHouse 回放历史 session(企业审计场景刚需)
- **依赖**:ClickHouse ✓;`AgentExecutionController` 事件结构已存在,需扩展枚举
- **协同 MAF 路线图**:OpenTelemetry trace ID 注入每事件,实现"日志 + 时间线 + trace"三视图打通

---

### P1 应做(2-3 周, ~10-12 人天)

#### #3 List 视图(~1-2 天)
- 在 `Hive/KanbanBoard` 同级加 `Hive/TaskList`,共享 `TaskCard`
- 顶部加切换按钮(Kanban / List / Calendar 三选)
- 列表支持排序 / 过滤 / 多选批量操作

#### #1 External Agent SPI(PoC)(~5-7 天)
- **范围**:**只做 SPI + 一个 reference impl**(不做归一化矩阵)
- **位置**:`schemaplexai-agent-engine/src/main/java/.../external/`
- **接口**:
  ```
  ExternalAgentAdapter
    ├── start(ExternalAgentConfig) → SessionHandle
    ├── send(SessionHandle, Message) → Stream<AgentEvent>
    ├── interrupt(SessionHandle)
    └── close(SessionHandle)
  ```
- **Reference impl**:Codex(因为 OpenAI app-server 协议是 OpenAPI 标准,稳定性最高)
- **审计要求**:每次 `send` 必须落 audit log(tenant + user + cli-provider + prompt-hash)
- **不做**:Claude Code / OpenCode 具体实现 ⇒ 留给企业按需扩展或社区贡献
- **风险**:本地账号 ⇒ **必须明确文档说明"此功能仅适用于自托管企业版"**,公有云租户禁用

#### #7 Task 级 Logical Branch 隔离(~3-5 天)
- **不做**:物理 Git worktree(磁盘 / 清理 / 隔离三坑)
- **做**:`schemaplexai-integration/git` 增加 `TaskBranchManager`
  - 任务创建 ⇒ 自动从 `main` 创建 `task/<task-id>` 分支
  - Agent commit 默认到此分支
  - 任务完成 ⇒ 创建 PR,合并后**软删除分支**(retention 30 天)
- **优势**:无磁盘负担,与现有 GitIntegrationService 协同
- **限制**:Agent 跨任务文件操作需走 GitHub API,不能本地 fs(我们已经如此,无新限制)

---

### ❌ 不做(共识不采纳)

| # | 原因 |
|---|------|
| #1 全量 Claude/Codex/OpenCode provider 矩阵 | 本地账号 + 缺乏审计,与多租户后端冲突;只做 SPI |
| #2 Chat → Task 演进流 | 与未来 Multi-Agent Orchestration 一起设计,避免重做 |
| #7 物理 Worktree | 服务端磁盘 / 清理 / 隔离三坑;改 logical branch |
| #8 多 panel 拖拽 UI | 破坏现有"agent 间协作"产品定位;复杂度高 ROI 低 |
| #10 离网 LLM 桥接 | 已有 LangChain4j multi-provider;OpenCode bridge 是单人工具,不通用 |

---

## 5. 路线图叠加 MAF

不是替换 MAF 路线图,而是**叠加**。建议执行顺序:

| 周 | MAF 路线图(已规划) | 本次新增(Tessera 影响) | 总人天 |
|----|--------------------|----------------------|-------|
| W1 | OpenTelemetry(2d) + Tool-call budget(1d) + Milvus consistency(0.5d) | **Skills Dashboard UI**(2-3d) | ~6-7 |
| W2 | Progressive skill disclosure(1d) + Checkpoint hash(2d) | **Composer 第一阶段**(3-5d) + **List 视图**(1-2d) | ~7-10 |
| W3 | Middleware pipeline(start) | **Session Timeline 增强**(3-5d) + ClickHouse 审计表 | ~6-8 |
| W4-5 | Middleware pipeline(完成) + ApprovalMode | **Logical Branch 隔离**(3-5d) | ~8-10 |
| W6-7 | Provider-agnostic core(SPI) | **External Agent SPI PoC**(5-7d) | ~8-10 |
| W8+ | Multi-Agent: Concurrent / Handoff / Group Chat | (与 Tessera #2 Chat→Task 合并设计) | — |

**总投入估算:** Tessera 改进约 **3-4 周**(2 前端 + 1 后端工程师),MAF 路线图独立约 **6-8 周**;并行执行总周期 ~8 周。

---

## 6. 关键决策点

需要项目所有者确认的 3 个决策:

1. **External Agent SPI 是否做?**
   - 推荐:**做 SPI(W6-7),不做具体 provider 实现**
   - 触发条件:若有企业客户明确要求集成 Claude Code,做对应 adapter;否则 SPI 待用

2. **物理 Worktree vs Logical Branch?**
   - 推荐:**Logical Branch + GitHub PR**
   - 理由:服务端架构 / 多租户 / 已有 GitIntegrationService 协同

3. **Composer 文件附件:何时接病毒扫描?**
   - 推荐:**P0 同时上线**(若不上线,文件上传功能必须 disabled,否则 P0 安全漏洞)
   - 选项:ClamAV 自托管 / 第三方 SaaS(如 VirusTotal API,但有数据出境合规问题)

---

## 7. 总结洞察

| 我们能从 Tessera 学到的 | 我们不该从 Tessera 学的 |
|----------------------|----------------------|
| **打开 Agent 黑盒**:Timeline / Skills Dashboard / Composer 是用户**信任 + 调试**的关键 | **本地优先架构**:不适合多租户企业 |
| **协议归一化思路**:多 provider 抽象是好的设计模式(SPI 即可) | **完整 CLI 包装矩阵**:本地账号模型与多租户冲突 |
| **任务级隔离**:Worktree 思路对(隔离),实现错(物理) | **物理 worktree**:改 logical branch |
| **Provider-native 控制**:复用 CLI 自带审批是好思路(我们用 ApprovalService 替代) | **多 panel 拖拽**:破坏产品定位 |

**核心洞察:** Tessera 是好镜子,照出我们的"**前端 Agent 体验薄弱**"问题。后端我们已经很强(2,601 tests + 21/21 patterns),但**用户能看见的部分**(Skills / Timeline / Composer)还没追上。**P0 三件套就是补这个洞**。

---

## Backlinks

- [[architecture]] — 系统架构总览
- [[technical-debt]] — 包含 MAF 路线图,本文件协同
- [[comparisons/microsoft-agent-framework]] — 上一轮架构对比
- [[active-areas]] — 当前活跃工作区域
- [[plans-and-initiatives]] — 项目规划
