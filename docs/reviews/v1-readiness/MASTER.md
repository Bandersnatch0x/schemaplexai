---
title: SchemaPlexAI v1 上线就绪评审 — MASTER 报告
moderator: PlanModerator
date: 2026-05-08
version: Day-0 Baseline
verdict: NOT-READY-FOR-RELEASE
---

# SchemaPlexAI v1 上线就绪评审 — Master 报告

> **Day-0 Verdict**：v1 当前状态 **NOT-READY-FOR-RELEASE**。综合得分约 **4.0/10**，7 位专家全票认定存在 release-blocker。本报告是 ralph-loop 持续工作流的 Day-0 基线，距 v1 GA 至少 12 周硬骨头。

## 0. Changelog

- 2026-05-08：Day-0 初始基线建立，7 专家首次评审完成。后续每次 ralph-loop 重 loop 触发新一行。
- 2026-05-08 [Δ-decision]：用户决策 #1 锁定 → launch 叙事 = 候选 ④ **「企业/团队 AI 编排」**（A+C 组合：单 Agent 内 21 patterns + Workflow BPMN 流程编排，多 Agent 协作明确 Reduce 至 v1.1）。联动调整：21 Agentic Patterns 由 Maintain → Expand（升为 launch 卖点）；Workflow BPMN 前端由 Reduce → Selective Expand（轻量版：导入 XML + react-flow 只读渲染）；brand metaphor 候选锁 **Studio Lab**；W7-W9 魔法时刻由 Drag-Drop RAG 改为 **Drag-Drop BPMN AI 节点编排**。冲突 #2 视觉 vs 叙事 仲裁同步更新（视觉走 Studio Lab）。
- 2026-05-08 [Δ-decision]：用户决策 #2+#3+#4 全锁 —— Notification v1 仅 in-app（ADR-013）；Cost v1 走 PG 短链路（ADR-012）；JWT 轮转 SLA = **90 天**（ADR-011，W4 落地 Vault 集成）。全部 4 决策已清，W1 启动阻塞归零。

## 1. 八维度评分矩阵（综合）

| # | 维度 | 当前分 | 10 分定义 | Gap | 攻坚周次 | 责任专家 |
|---|------|--------|----------|-----|---------|---------|
| 1 | 产品叙事 | 4.4 | launch tweet 一句话 + 4 模式 backlog 全公开 + 赛道清晰 | 5.6 | W1, W12 | ProductStrategist |
| 2 | 架构清晰度 | 4.2 | C4-L2 图 + 16 模块 ADR 全 + 17 隐藏假设清单 + 失败补偿 ADR | 5.8 | W1-W6 | ArchitectureAuditor |
| 3 | DX / TTHW | 2.3 | TTHW ≤ 2 分钟，5 秒首屏 SSE，1-click demo | 7.7 | W7-W9 | DXArchitect |
| 4 | 设计系统 | 4.5 | Storybook ≥ 30，亮/暗双主题，Style Dictionary，AA | 5.5 | W5-W6 | DesignSystemLead |
| 5 | 安全 | 4.25 | 零 P0/P1，SBOM，Trivy/Dep-Check/Gitleaks 三轨绿 | 5.75 | W1-W2 | SecurityAuditor |
| 6 | 调试/根因 | 5.0 | 每阻塞含假设链 + 数据流 + 验证 + 修复 PR | 5.0 | W1-W2 | DebugMaster |
| 7 | 测试覆盖 | 4.75 | JaCoCo 全 16 模块 ≥ 80%，前端 line ≥ 70%，e2e smoke 绿 | 5.25 | W3-W6 | TestDocSentinel |
| 8 | 文档同步 | 4.0 | doc-gardener drift = 0，9 drift 全修，Knife4j 100% 维持 | 6.0 | W2-W11 | TestDocSentinel |
| **加权综合** |   | **4.0** | 全 8 维 ≥ 9 | **6.0** |   |   |

> 加权方法：等权平均 (4.4+4.2+2.3+4.5+4.25+5.0+4.75+4.0)/8 = **4.18 → 取保守值 4.0**。详细子维度见 [scoring-matrix.md](./scoring-matrix.md)。

## 2. Top 5 Critical Insights

### 2.1 隐藏的十星级产品 — 赛道转向（ProductStrategist）

ProductStrategist 在打 Cursor/Devin 红海中识别出 SchemaPlexAI 真正的护城河是 **多租户 + Cost Analytics + Quality Gates + 4 维准入控制**——这些是 Cursor/Devin/v0/Replit Agent **全部都没有** 的资产。继续对标 IDE 类产品会让 launch 叙事永远写不出来；正确赛道是 **Backstage / Port.io / Cortex.io / GitLab Duo Enterprise / SourceGraph Cody Enterprise**（开发者门户类）。一旦换赛道，TTHW 落后 20-100x 立刻从灾难变行业常态——B 端门户类产品 TTHW 普遍 30 分钟级是行业基线。

**Launch tweet 候选**：
- ① "Cursor for code, but for the entire R&D pipeline"——直接借 Cursor 心智但会被打脸
- ② **"开发者门户里的 AI 副驾"** ⭐ 推荐——避开红海、利用最强资产
- ③ "AI 研发的 SOC2 控制面"——合规赛道但销售周期 6-12 个月

### 2.2 Security 不可发布（6 P0 + 4 P1）

SecurityAuditor 以 confidence ≥ 8/10 + 双证据原则识别出 **6 个 P0**：
1. `docker-compose.yml` 4 处明文密码 + Grafana admin/admin（10/10）
2. 13 个 application.yml 弱默认 `${DB_PASSWORD:sf_password}`（9/10）
3. ES/Milvus/etcd/CH/Jaeger 全部无认证暴露宿主机端口（9/10）
4. Gateway CORS `allowedOrigins:"*"` + `allowCredentials:true` 同时存在（8/10）
5. CI 完全无安全扫描，零 Trivy/Dep-Check/Gitleaks job（10/10）
6. `jdk21.zip`（20MB）入库 + `.gitignore` 缺 `*.pem/*.key/secrets/`（10/10）

**结论**：安全态势 **不可发布 v1**。W1-W2 必须清零所有 P0+P1，否则 CFO/CISO 在采购评估第一轮就会把产品筛掉。

### 2.3 修复成本被高估 3-4 倍（DebugMaster）

DebugMaster 用"调查到根因"的方法把 4 个明面阻塞的预估工作量从 8-15 人日砍到 1.5 人日：

| 阻塞 | 原估 | 实估 | 节省关键 |
|------|------|------|---------|
| AgentExecution SSE 不发数 | 1-2 天 | **0.1 人日** | 仅缺 1 行 `eventBus.register(executionId, emitter)`，基础设施 100% 就位 |
| NotificationConsumer 三桩 | 4-7 天 | **0.3 人日** | v1 砍到仅 in-app（NotificationServiceImpl 已完整实现）；email/sms 抛 DLQ 留 v1.1 |
| CostService 三零 | 5-10 天 | **0.5 人日** | v1 走 PG 短链路（publisher → MQ → consumer → `sf_cost_record_pg`），ClickHouse 推 v1.1 |
| smoke.spec.ts 缺失 | 2 天 | **0.5 人日** | MSW mock 路线，避免 CI 启 12 服务 |

**Day-1 即可关单 4 个明面阻塞，合计 ≤ 1.5 人日**。

### 2.4 CI 是幽灵（TestDocSentinel + Security 双证）

3 条独立证据汇聚到同一结论——CI 长期裸奔：
1. `ci.yml:152` 引用 `e2e/smoke.spec.ts`，**文件根本不存在**，e2e job 自首次 push 起 100% 失败或被默认跳过；
2. `ci.yml:41` JaCoCo `-pl` 仅含 7/16 模块，web/agent-config/integration/ops/admin/spec/quality/workflow/task **9 模块零门禁**；
3. 前端 `vitest.config.ts` **未配 coverage provider**、`passWithNoTests:true` 直接放水，CI `npm test --run` 不带 `--coverage`，100 个测试零度量。

**任何"CI 绿"的发布信号当前都是假绿**。W2 必须修这三处，否则后续覆盖率攻坚全是无根之木。

### 2.5 多租户隔离只在 SQL 层（Architecture）

`TenantLineInterceptor` 仅覆盖 **SQL row-level**，但下面 4 层全部裸奔：

| 层 | 现状 | 风险 |
|---|------|------|
| RabbitMQ | 单一 vhost `/`、单一 exchange、队列名无 tenant 前缀 | 租户 A 消息可被租户 B 消费者抢消 |
| Milvus | 全租户共用 collection，仅靠业务层 filter（M-11 已修但纵深防御缺） | filter 注入或权限误判 → 跨租户向量泄漏 |
| MinIO | 单 bucket | 路径遍历或签名 URL 泄漏 → 跨租户文件可读 |
| Redis | 部分 key 有 tenant 前缀，限流 / 缓存键不强一致 | 跨租户限流穿透、缓存中毒 |

"企业级多租户"是 launch tweet 必含词，**这 4 层不打通就不能写这句**——否则 SOC2 / ISO 27001 审计当场打脸。

## 3. 4 模式范围决策（产品级综合）

合并 ProductStrategist / DXArchitect / DesignSystemLead 三家的 4 模式建议。

### Expand（v1 必做，7 项）

| 项 | 来源 | 验收 KPI |
|---|------|---------|
| 首页 Hero + 30 秒 demo gif/video（AgentExecutor SSE 升首页） | Product+DX | landing 页 autoplay demo ≤ 30s + "Try sample agent" 一键按钮在 sample tenant 下无需登录可触达 |
| Cost Dashboard 升级为首页第二屏（CFO-friendly 卖点） | Product | `/ops/cost` 真实 PG 数据 + 按 tenant/agent/model 三维度切片 |
| Quickstart 5 分钟可达（`schemaplexai up` 脚本 + .env.example + seed） | Product+DX | 新机器从 git clone 到看到 Agent 跑通 ≤ 5 分钟 |
| 修 C-3 + M-11 两个租户隔离 bug + MQ/Milvus/MinIO 多租户化 | Product+Arch | 跨租户 fuzz 测试 0 命中 |
| Style Dictionary 三态 token + 亮/暗主题切换 + brand metaphor 落地（Studio Lab） | Design | Cockpit + AgentExecutor 亮色视觉验收通过；brand metaphor 锁 Studio Lab |
| Codespaces / devcontainer 模板 + MockLlmProvider | DX | "Open in Codespaces" 按钮 5 分钟内可点 demo |
| **21 Agentic Patterns 升为 launch 卖点 ⭐**（落 ④ A+C 决策） | Product+DX | 首页文案改"21 种 Agent 编排模式（ReAct / Reflection / RAG / Tool Use / Planning…）"+ hero 列出 5 大类标签 + 文档站给每模式 1 段 use-case |

### Selective Expand（v1 二选一，3 项）

| 项 | 取舍 | 推荐 |
|---|------|------|
| Storybook 亮色 launch 截图集 vs 全量到 8/10（4 周 vs 3 天） | Design | **截图集**，全量推 v1.1 |
| 3 份 Spec 改 outcome 用户故事 vs 8 份全改 | Product | **3 份**（agent-execution/cost-analytics/quality-gate） |
| BPMN 流程编排前端（导入 XML + react-flow 只读渲染）vs 全量拖拽编辑器 | Product+DX | **导入 + 只读渲染**（W3-W4 1.5 周）；全量拖拽推 v1.1 |

### Maintain Scope（保持，4 项）

| 项 | 理由 |
|---|------|
| 现有 16 个 Maven 模块结构 | 已稳定，v1 期窗内不重构；admin 空着也不动 |
| Knife4j API Docs（最近 commit e134899 已 100% 覆盖） | 是企业评估加分项不是 launch 卖点 |
| 2,601 后端测试 + 100 前端测试基础体量 | 维持，不为 launch 加测、也不砍 |
| 9 个现有 ADR 文档 | 后续追补 ADR-010~014，不改既有 |

### Reduce / 延 v1.1（必砍，7 项）

| 项 | 来源 | 理由 |
|---|------|------|
| Multi-Agent 编排（Sequential/Concurrent/Handoff/Group Chat） | Product | MAF 对比表自陈 LARGE，3-4 周；v1 ④ A+C 锁定为 "Single-Agent + Workflow"，多 Agent 留 v1.1 |
| Notification email/sms 通道 | Debug+Product | v1 仅 in-app（0.3 人日），email/sms 抛 DLQ 留 v1.1 |
| Cost ClickHouse 长链路 | Debug | v1 走 PG 短链路（0.5 人日），ClickHouse 推 v1.1 |
| Quality 4 页面（QualityCenter/Issues/Gates/SecurityAudit） | DX | 后端无实现，前端有页面误导用户，v1 全砍 |
| Tasks 三页合一（TaskBoard 主 + Drawer） | DX | 路由 3 → 1 |
| schemaplexai-admin 空模块从 root pom 暂 comment-out | DX | v1 用 SQL+CLI 做租户管理 |
| MCP Tool Discovery 完整版（保留 stub 返 501） | Product | 避免做一半 |

## 4. 跨域冲突仲裁

7 份报告间识别出 **5 处显著冲突**，逐条仲裁：

### 冲突 1：Cost 走 PG 短链路 vs ClickHouse 长链路

- **DebugMaster**：v1 PG 短链路（0.5 人日），ClickHouse 推 v1.1。
- **Architecture**：ClickHouse 是 ADR-005 决策，应启用而非延后。
- **TestDocSentinel**：ops 模块覆盖率攻坚需要 ClickHouse Testcontainer。

**仲裁**：**采纳 DebugMaster 的 PG 短链路**。理由：(a) v1 当前 ops/application.yml `clickhouse.enabled:false`，启用 + 接线 + 测试 ≥ 2 人日，且 ClickHouse 容器无认证（Security P0），风险叠加；(b) PG 路线 0.5 人日即可让 Cost Dashboard 出真实数据，是 Expand-2 的前置；(c) 写 ADR-012 `Cost-PG-Short-Path-for-v1`，明确 v1.1 升级路径；(d) ClickHouse Testcontainer 推到 W5（ops 攻坚周）。

### 冲突 2：launch 叙事赛道（开发者门户 vs IDE 副驾）

- **ProductStrategist**：转向 Backstage/Port/Cortex 赛道。
- **DesignSystemLead**：仍按 Cursor/Linear/Vercel 视觉语言设计（"Hive of Specs"）。
- **DXArchitect**：以 Cursor/Replit/v0/Devin 为 TTHW 对标基线（即 IDE 类）。

**仲裁**：**叙事用门户、视觉借鉴 IDE 暗色美学**。理由：(a) 视觉品牌（Linear/Vercel/Cursor）与产品定位（门户/Backstage）是两个独立维度——Backstage 本身视觉很难看，正是给 SchemaPlexAI 留出的差异化空间；(b) DXArchitect 的 TTHW 基线必须重新校准为门户类（30 分钟级），否则 W7-W9 DX 攻坚目标不切实际；(c) 由 ProductStrategist 在 W1 拍板后，DXArchitect 重写 §6 的 Codespaces SLA。

### 冲突 3：admin 模块状态（empty vs 已有 6 Service）

- **CLAUDE.md** 与 **ProductStrategist** ："admin 是空模块"
- **TestDocSentinel** + **wiki/gaps.md**：admin 已有 6 个 Service（AuditLog/PlatformHealth/RoleAdmin/SystemConfig/TenantAdmin/UserAdmin）

**仲裁**：**TestDocSentinel 数据为准**。CLAUDE.md drift 列入 W2 必修；ProductStrategist 的 "Reduce: admin 延 v1.1" 改写为 "admin 已有 6 Service 但无前端，v1 后端保留 + 前端延 v1.1"；DXArchitect 的 "comment-out admin from root pom" 撤销。

### 冲突 4：Notification 通道范围

- **DebugMaster**：v1 仅 in-app（email/sms 进 DLQ）。
- **Architecture**：NotificationConsumer 应接 SendGrid/SES + Twilio，2-3 周。
- **ProductStrategist**：未直接表态但建议 v1 砍非核心。

**仲裁**：**采纳 DebugMaster**。Architecture 的方案是 v1.1 蓝图，写入 ADR-013 `Notification-InApp-Only-for-v1`，明确 email/sms 进 DLQ 而非静默吞掉（修正 NotificationConsumer 当前 `return true` 的问题）。

### 冲突 5：Storybook 是否 v1 must-have

- **DesignSystemLead**：v1 must-have，30 组件 + 110 stories。
- **DXArchitect**：W3-W4 引入 Storybook（与 DS 一致）。
- **ProductStrategist**：Selective Expand，仅做 launch 用截图集。

**仲裁**：**采纳 ProductStrategist + DesignSystemLead 折中**：v1 内交付 12 个原子层 Storybook + 36 stories（W3 完成），launch 截图集独立交付（W6）；30 组件全量推 v1.1。

## 5. v1 阻塞清单（合并 + 优先级）

按 **严重性 → 周次 → 人日** 排序的 22 项阻塞：

| # | 阻塞 | 来源 | 严重性 | 修复人日 | 周次 | 责任 |
|---|------|------|--------|---------|------|------|
| 1 | docker-compose 4 处明文密码 + Grafana admin/admin | Security | P0 | 0.5 | W1 | SecAuditor |
| 2 | 13 个 application.yml 弱默认 sf_password | Security | P0 | 0.3 | W1 | SecAuditor |
| 3 | ES/Milvus/etcd/CH/Jaeger 容器无认证暴露 host 端口 | Security | P0 | 1.0 | W1 | SecAuditor |
| 4 | Gateway CORS `*` + `allowCredentials:true` | Security | P0 | 0.3 | W1 | SecAuditor |
| 5 | CI 零安全门禁（无 Trivy/DepCheck/Gitleaks） | Security+TestDoc | P0 | 1.0 | W2 | SecAuditor+TestDoc |
| 6 | jdk21.zip 入库 + .gitignore 缺 secrets/ | Security | P0 | 0.3 | W1 | SecAuditor |
| 7 | CostService 三零值（PG 短链路） | Debug | P1 | 0.5 | W2 | DebugMaster |
| 8 | AgentExecution SSE 缺 1 行 register | Debug | P1 | 0.1 | W1 | DebugMaster |
| 9 | NotificationConsumer 三桩 → 仅 in-app + email/sms 入 DLQ | Debug+Product | P1 | 0.3 | W2 | DebugMaster |
| 10 | CI e2e/smoke.spec.ts 文件不存在 | TestDoc | P1 | 0.5 | W1 | TestDoc |
| 11 | 9 模块 JaCoCo 未强制（web/agent-config/integration/ops/admin/spec/quality/workflow/task） | TestDoc | P1 | 0.5 | W2 | TestDoc |
| 12 | 前端 vitest 无 coverage 度量 + passWithNoTests:true | TestDoc | P1 | 0.3 | W2 | TestDoc |
| 13 | mutation 端点缺 @Valid / @PreAuthorize 矩阵 | Security | P1 | 1.5 | W1-W2 | SecAuditor |
| 14 | JWT 白名单 `/system/tenants/**` 过宽 | Security | P1 | 0.3 | W1 | SecAuditor |
| 15 | JWT HS256 无轮转 + kid/JWKS 缺 | Security | P1 | 1.0 | W3 | SecAuditor |
| 16 | langchain4j 0.31.0 陈旧 + 无 SBOM | Security | P1 | 0.5 | W3 | SecAuditor |
| 17 | HttpCallAdapter SSRF allowlist 缺失 | Debug+Sec | P1 | 0.5 | W2 | SecAuditor |
| 18 | RabbitMQ 多租户隔离（vhost/exchange/queue 命名 sf_{tenant}_*） | Architecture | P1 | 2.0 | W3 | ArchAuditor |
| 19 | Milvus collection 多租户化 + MinIO bucket 多租户化 | Architecture | P1 | 2.0 | W3-W4 | ArchAuditor |
| 20 | RabbitMQ 缺 DLX 声明 + publisher confirm | Architecture | P1 | 1.0 | W2 | ArchAuditor |
| 21 | 9 处文档 drift（CLAUDE.md "12 模块"/admin empty/active-areas 全空 等） | TestDoc | P2 | 1.0 | W2 | TestDoc |
| 22 | WorkflowTrigger/QualityEvent/MilvusSync stub + SpecReviewNotification 不发通知 | Debug | P2 | 2.0 | W4 | DebugMaster |

**P0 合计 6 项 / 3.4 人日；P1 合计 12 项 / 9.6 人日；P2 合计 4 项 / 4.0 人日；总计 22 项 / 17 人日**。

## 6. 12-week v1 GA 路线图

合并 7 份报告的周次建议，每周给可验收 KPI。

### Week 1 — Critical Security + Debug Quick Wins（P0 全清）
- 关 P0 6 个安全阻塞（明文密码 / 弱默认 / 容器暴露 / CORS / jdk21.zip / CI 安全门禁）
- SSE 缺 1 行 + JWT 白名单收窄 + mutation @Valid 第一批
- **KPI**：CI 全绿，gitleaks 历史扫绿，SSE 集成测试通过，nmap 扫宿主无未授权端口

### Week 2 — CI Hardening + 文档 drift + Notification 砍简
- CI 加 Trivy/Dep-Check/Gitleaks 三轨 + JaCoCo 全 16 模块强制（带 excludes 白名单）+ smoke.spec.ts + spec-creation.spec.ts + multi-tenant.spec.ts
- vitest 加 v8 coverage + 70% 阈值
- Notification 砍到仅 in-app（ADR-013）
- CostService 走 PG 短链路（ADR-012）
- 9 处 drift 修复（CLAUDE.md / active-areas.md / Knife4j memory / 等）
- HttpCallAdapter SSRF allowlist + RabbitMQ DLX 声明
- **KPI**：P0=0，JaCoCo CI required 全模块（含白名单），文档 drift=0，前端 coverage 报告可见

### Week 3-4 — 覆盖率攻坚 + 多租户深化 + BPMN 前端轻量版
- W3：web/context 模块覆盖率 ≥ 80%；MQ vhost+queue 多租户化；JWT 轮转 ADR-011 + RS256 POC
- W3：**BPMN XML 导入 API + react-flow 只读流程图渲染**（落 ④ A+C 决策；前端 1.5 周）
- W4：system/integration/admin/agent-config 模块 ≥ 80%；Milvus collection + MinIO bucket 多租户化
- W4：JWT 轮转 ADR-011 落地（90 天 SLA；Vault KV 集成 + JWKS endpoint 设计 + RS256 切）；JWT_SECRET fail-fast 启动校验
- WorkflowTrigger/QualityEvent/MilvusSync stub 实现（视 ProductStrategist 决策可砍）
- **KPI**：8 个低覆盖模块 ≥ 70%；多租户隔离 4 层全打通；ArchUnit 规则上线；BPMN 导入 demo 可演示；JWT 90 天轮转可演示（Token 过期 → 刷新 → 新 Key）

### Week 5-6 — 覆盖率收尾 + 设计系统启动
- W5：ops/gateway 模块 ≥ 80%；Style Dictionary 三态 token + 亮/暗切换
- W6：spec/quality 从 0 到 80%；Storybook 12 原子组件 + 36 stories；Spring Cloud Contract 引入
- **KPI**：JaCoCo 全模块 ≥ 80%；Storybook 公开预览；Brand Metaphor "Hive of Specs" 写入 wiki

### Week 7-9 — DX 魔法时刻
- W7：workflow/task 覆盖率 ≥ 80%；JaCoCo 白名单全部拆除；`schemaplexai up` 一键脚本（bash + ps1）
- W8：seed 数据 + 3 个 Demo Agent + MockLlmProvider；PIT mutation 在 agent-engine 跑通（≥ 60% mutation score）
- W9：Codespaces / devcontainer 模板上线；5 秒首屏 SSE 魔法时刻可触达；**Drag-Drop BPMN AI 节点编排魔法时刻**（react-flow 编辑器 + AI 节点库；落 ④ A+C 决策）
- **KPI**：TTHW ≤ 15 分钟（本地）/ ≤ 5 分钟（Codespaces）；首屏 SSE ≤ 5 秒；demo gif 可拍

### Week 10-11 — RC + Beta + Triage
- W10：v1-RC tag、外部 dogfood 邀请 10 人；doc-gardener 周扫 cron 上线；ADR 补齐到 12+
- W11：Quality 4 页面下架 / Tasks 三页合一；bug bash + 9 处 drift = 0
- **KPI**：dogfood 10 人，bug bash close ≥ 80%

### Week 12 — GA
- launch tweet 上线（候选 ④ A+C 锁定 — 「企业/团队 AI 编排」）；首页 Hero + 30s demo video
- ralph-loop 切日间监控（每 PR 轻 loop + 每日 03:17 重 loop）
- 8 维度评分全 ≥ 9
- **KPI**：8 维全 ≥ 9，TTHW ≤ 2 分钟（Codespaces 路径），ralph-loop Δ 连续 3 天 = 0

## 7. ralph-loop 持续工作流

- **重 loop**：每天 03:17 触发，重跑全 8 专家 + MASTER 合并，产物落 `.claude/plans/v1-readiness/loop-<date>.md`，对比上一轮的 Δ（评分变化 / 阻塞新增 / 阻塞关单）
- **轻 loop**：每个 PR 触发，仅跑 SecurityAuditor + DebugMaster + TestDocSentinel 三专家，5 分钟内反馈
- **终止条件**：8 维全 ≥ 9 且 Δ 连续 3 天 = 0 自动停止
- **失败保护**：重 loop 任何一专家连续 3 天评分回退，自动升级到主持人发 ADR 草案（DebugMaster 铁律）

## 8. 用户决策状态（0 待回 / 4 已锁）

> **W1 启动阻塞归零。** 全部 4 个决策已于 2026-05-08 锁定。以下仅为归档记录，不再等待。

1. ✅ **决策 #1（赛道叙事）**：候选 ④「企业/团队 AI 编排」A+C 组合 —— 单 Agent 21 patterns + Workflow BPMN 前端轻量版；brand metaphor → Studio Lab。
2. ✅ **决策 #2（Notification 通道）**：v1 仅 in-app，email/sms 抛 DLQ 留 v1.1 → ADR-013。
3. ✅ **决策 #3（Cost 路线）**：v1 走 PG 短链路，ClickHouse 推 v1.1 → ADR-012 + ADR-005 现状段改写。
4. ✅ **决策 #4（JWT 轮转 SLA）**：90 天 → ADR-011，W4 落地 Vault 集成 + JWKS endpoint。

## 9. v1 GA 验收（GA 当天必须 5 项全绿）

1. 8 维度全 ≥ 9/10；
2. 22 项阻塞全 close（P0/P1/P2 全部）；
3. TTHW ≤ 2 分钟（Codespaces 路径）/ ≤ 15 分钟（本地路径）；
4. ralph-loop Δ 连续 3 天 = 0；
5. launch tweet 一句话叙事被 7 专家全票通过；
6. （加分）SBOM artifact 公开 + Trivy 三轨 12 周连续绿。

## 10. 附录：7 份域报告链接

- [Product Strategy](./product-strategy.md) — ProductStrategist 4.4/10
- [Architecture](./architecture.md) — ArchitectureAuditor 4.2/10
- [DX Evaluation](./dx-evaluation.md) + [DX Questions (45)](./dx-questions.md) — DXArchitect 2.3/10
- [Design System](./design-system.md) — DesignSystemLead 4.5/10
- [Security](./security.md) — SecurityAuditor 4.25/10
- [Debugging](./debugging.md) — DebugMaster 5.0/10
- [Test & Docs](./test-and-docs.md) — TestDocSentinel 4.75/10
- [Scoring Matrix](./scoring-matrix.md) — 8 维度子维度雷达 + 评分快照
