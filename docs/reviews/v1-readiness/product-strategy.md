---
title: ProductStrategist v1 上线战略评审
agent: ProductStrategist
date: 2026-05-08
domain: product-strategy
---

> 本报告聚焦"用户为什么今天要打开 SchemaPlexAI 而不是 Cursor / Devin / v0 / Replit Agent"这一终极问题。范围决策（Expand / Selective Expand / Maintain Scope / Reduce）以"v1 GA 当天能否写出一句让 AI 工程负责人转发的 launch tweet"为收敛准则。

## 1. 0-10 评分表

| 子维度 | 当前分 | 10 分定义 | 证据 / Gap |
|---|---|---|---|
| 产品定位清晰度 | 5 | 一句话能讲清独特价值，且这句话不能套用到任意"AI Agent 平台" | `README.md:1-3` 描述"Spec→Workflow→Agent→质量门禁→成本分析"——是流程图，不是定位语；`CLAUDE.md` 顶部仍是相同表述。MAF 对比页 (`wiki/comparisons/microsoft-agent-framework.md:18`) 自承"我们是 platform，他们是 SDK"，但没回答"为什么是 SchemaPlexAI 这个 platform"。 |
| Launch 叙事 | 3 | 有 demo 视频脚本 + 30 秒"魔法时刻" + 可复制的 quickstart | 16 个模块要 12 条 `mvn spring-boot:run` 才能启动 (`README.md:69-82`)；无 `.env.example`、无 seed 数据、无 demo tenant；TTHW 45-90 分钟（v0 30 秒）—— 没有可拍摄的"魔法时刻"。 |
| 与竞品差异化 | 6 | 单一卖点能在 5 秒内让对方点头："哦，Cursor/Devin 确实做不到这个" | 真实差异化集中在 wiki/comparisons:33-39 列的 7 项（多租户 + 质量门禁 + 成本分析 + ClickHouse + 4 维准入控制 + Tika 摄取 + Shadow A/B），但 v1 UI 把这 7 项藏在二级菜单里，首屏仍是"Agent Executor 聊天框"——和 ChatGPT 同质化。 |
| 商业模式 | 4 | 已有定价页 / 试用门 / 计费 SKU 清单 / 第一批意向客户 | `schemaplexai-ops` 有 `BudgetServiceImpl`/`CostAnalyticsServiceImpl` 雏形（code-review-report 776 行附录列为"未审查"），但前端无 pricing 页、无 self-serve 注册、无 license/seat 概念；`schemaplexai-admin` 模块为空 (`technical-debt.md:34`)。 |
| 用户旅程闭环 | 4 | 新用户 5 分钟内能完成 1 次"上传 spec → 看到产物"循环，无需读文档 | 8 份 v1 spec (`docs/specs/2026-04-30-v1.0-*.md`) 覆盖 engine/gateway/cost/integration/quality/rag/spec/workflow，**但没有一份 spec 是"用户故事"**——全部是技术 spec；21 UI 页无 onboarding 引导、无 sample dataset，新用户登录后看到的是空 console。 |

**综合分：4.4 / 10**——技术骨架 7 分水准，但产品故事还停在"我们做了一个平台"，没回答"用户为什么今天就要用"。

## 2. 关键发现（带证据）

1. **Launch 叙事缺位是 v1 最大风险，超过任何技术 critical**。`code-review-report-2026-05-07.md` 的 6 条 Critical 都是后端 bug，但即便明天全修完，没有"用户 30 秒能复现的魔法时刻"，v1 上线日仍只有内部团队转发。证据：`README.md:46-89` 启动一次完整环境需 12 条命令 + 9 个 docker 容器。

2. **CLAUDE.md 元数据 drift 暴露了产品自我认知失焦**。task 描述里指出"CLAUDE.md 写 12 模块实际 16"——这不是文档 bug，是**核心叙事不稳定**。当作者自己都数不清模块数时，外部用户不可能记住产品定位。

3. **v1 spec 全部是后端技术 spec，缺一份用户故事 spec**。`docs/specs/` 8 份 v1.0 spec 全部以模块命名（agent-engine/api-gateway/cost-analytics/integration/quality-gate/rag-pipeline/spec-management/workflow-engine），无 `2026-04-30-v1.0-first-user-journey.md` 之类。

4. **AgentExecutor SSE 是当前最接近"魔法时刻"的资产但被埋藏**。`schemaplexai-ui/src/pages/AgentExecutor/index.tsx` 是 git 已修改文件，意味着团队还在打磨它，这是 v1 该当头牌的素材；但 21 页 UI 把它放在普通菜单项里，没有"首页大 hero + 一键 demo"的呈现。

5. **MAF 对比表自陈最大 gap = 多 Agent 编排，但这恰是 v1 不该补的**。`wiki/comparisons/microsoft-agent-framework.md:24-29` 列 6 项 gap，其中 Multi-Agent Orchestration 标 HIGH/Large——`technical-debt.md:56-62` 排到 Phase 3（3-4 周）。如果 v1 等多 Agent 才发，会错过窗口期；应该把 v1 重新定义为"Single-Agent 但带企业级控制面"。

6. **设计系统 4-5/10 + 无 Storybook + 无亮色，会让 launch 截图不出彩**。这一点已被预先验证；补充证据：v0/Cursor/Devin 的 launch 截图全部是亮色 + 大字 + 强烈渐变，SchemaPlexAI 的 Abyss Hive 暗黑系即便完成度 10/10，也需要为 launch 准备一套**可截图的亮色快照**。

7. **`schemaplexai-admin` 空模块 = 商业化基础设施缺位**。`technical-debt.md:34` 列为 P1 placeholder。没有 admin 后台意味着无法做"试用申请审批 / 配额调整 / license key 颁发"——v1 就只能私部署，无法 self-serve。

8. **8 份 v1 spec 中 7 份证明"我们做了什么"，0 份证明"用户得到了什么"**。`SPEC-REVIEW-v1.0.md` 文件存在但 8 份子 spec 标题全是模块名而非 outcome，对外发布时找不到一句可用的产品价值主张。

9. **Cost Analytics 是被低估的 v1 真王牌**。`schemaplexai-ops` 已有 ClickHouse + BudgetService + CostAnalyticsService 雏形（code-review-report 附录第 770 行），且 4 维准入控制（rate/concurrency/token/cost）是 MAF / Cursor / Devin 都没有的——这是**唯一一个能让 CFO 在采购会议上说"我们需要这个"的功能**。但 v1 UI 没有 cost dashboard 首屏。

10. **TTHW 20-100x 落后不是 bug 是定位错位**。Cursor 2 分钟、v0 30 秒是**单机/单文件场景**；SchemaPlexAI 启动需要 PG+CH+Redis+RabbitMQ+MinIO+Milvus+ES+Prom+Grafana 9 个容器，**这不是产品缺陷，是企业级定位的必然代价**。错误是把它和这些 IDE 直接对标。正解：换赛道——对标 Backstage / Port / Cortex（开发者门户类），TTHW 30 分钟在该赛道是行业常态。

## 3. 用户痛点 / 隐藏假设公开化

> 这一节的 5 条假设全部是"皇帝的新衣"——团队心知肚明但 v1 没人写出来的事。

1. **假设 A：「我们要打 Cursor / Devin」——事实上我们打不过，且不应该打**。Cursor 是 IDE 内嵌、Devin 是 SaaS 黑箱、v0 是前端单文件。我们是企业级多租户平台。继续对标会让 launch 叙事永远写不出来。**正确比较组：Backstage、Port.io、Cortex.io、SourceGraph Cody Enterprise、GitLab Duo Enterprise**。

2. **假设 B：「16 个模块 + 21 个页面 = v1 完成度高」——事实上是反向信号**。竞品 v1 通常 1 个产品页 + 3 个核心 workflow。模块数与产品力不成正比，且让 launch 叙事失焦。

3. **假设 C：「等多 Agent 就绪再发 v1」——事实上 multi-Agent 是 v2 的事**。MAF 对比表自陈这是 LARGE effort（3-4 周），且 OpenAI Swarm / CrewAI / AutoGen 已经把 multi-Agent 卷成红海。v1 应该以"single-Agent 但带 cost / quality / tenant 三重控制"为锚定。

4. **假设 D：「企业客户会读 spec / 看 Knife4j 文档」——事实上他们看 Loom 视频和 30 秒 demo gif**。当前 README 没有 demo gif、没有视频链接、没有 Try-it-now 按钮。

5. **假设 E：「我们的多租户隔离是壁垒」——事实上代码审查发现 C-3 (JwtAuthFilter 租户头注入) + M-11 (Milvus filter 注入) 还有 bug**。在这两个修完之前，不能把"企业级多租户"写进 launch tweet——会被安全审计客户当场打脸。

## 4. 改造方案 — 4 模式范围决策

### Expand（v1 必做，4 项）

| 项 | 为什么是 Expand | 验收 KPI |
|---|---|---|
| **首页 Hero + 30 秒 demo gif/video** | 当前最接近"魔法时刻"的 AgentExecutor SSE 流必须从二级菜单升到首页大 hero + 自动播放 demo。没有这个，launch 当天没人转发。 | Landing page + autoplay demo（≤ 30s）+ "Try sample agent" 一键按钮在 sample tenant 下无需登录可触达；首屏 Lighthouse > 85 |
| **Cost Dashboard 升级为首页第二屏** | 这是唯一的 CFO-friendly 卖点。ClickHouse + 4 维准入已就位，前端补 1 个仪表板就能成为差异化首发素材。 | `/ops/cost` 页有真实 ClickHouse 数据 + 按 tenant/agent/model 三维度切片 + 成本预算告警 UI；并在首页放一张静态截图 |
| **Quickstart 5 分钟可达**（`.env.example` + `docker-compose up` + seed tenant + sample agent + sample spec） | 当前启动需 12 条 mvn 命令——没人会跑完。需要 `make demo` 一键起。 | 新机器从 `git clone` 到看到 Agent 跑通 ≤ 5 分钟，且无需手动配置任何 secret |
| **修 C-3 + M-11 两个租户隔离 bug** | "企业级多租户"是 v1 launch tweet 必含词。这两个 bug 修不掉就不能写这句。 | 安全测试用例：tenant A 用 tenant B 的 X-Tenant-Id header 访问任何接口都返回 403；Milvus filter 注入 fuzz 测试通过 |

### Selective Expand（二选一，2 项）

| 项 | 为什么是 Selective | 验收 KPI |
|---|---|---|
| **Storybook 亮色快照（仅为 launch 截图）** OR **设计系统全量到 8/10** | launch 需要亮色截图，但全量到 8/10 是 4 周工作量；只做 launch 用截图集是 3 天。**建议选前者**。 | 12 张关键页面亮色快照导出 PNG，能拼成 marketing 长图 |
| **3 份 Spec 改写为用户故事** OR **8 份全部改写** | 8 份 spec 全部改是仪式性工作；选 3 份对外（agent-execution、cost-analytics、quality-gate）改成 outcome-driven 用户故事即可。 | 3 份 spec 顶部加 "Job-to-be-done" 段落 + 1 句产品价值主张 |

### Maintain Scope（保持不动，4 项）

| 项 | 为什么 Maintain | 验收 KPI |
|---|---|---|
| **现有 16 个 maven 模块结构** | 已稳定，不要在 v1 期窗内重构。`schemaplexai-admin` 空着也不动——v1.1 再补。 | 不引入新模块，不合并旧模块 |
| **21 Agentic Patterns 完整度** | 已完成是事实，但在 launch 叙事里只字不提"21 个模式"——这是工程话语不是产品话语。 | 保留代码，launch 文案中改写为"覆盖 ReAct / Reflection / Planning / Tool Use / RAG 五大执行范式" |
| **Knife4j API docs** | 是企业客户做技术评估时的加分项，但不是 launch 卖点。保持现状。 | 8082/doc.html 可访问且全 controller 已贴 @Operation 注解（最近一次 commit e134899 已完成） |
| **2,601 后端测试 + 100 前端测试** | 是技术 hygiene 不是产品故事。不要为 launch 加更多测试，也不要砍。 | CI 绿，coverage ≥ 80% 维持 |

### Reduce / 延 v1.1（必砍/延后，6 项）

| 项 | 为什么 Reduce | 验收 KPI |
|---|---|---|
| **Multi-Agent 编排（Sequential/Concurrent/Handoff/Group Chat）** | MAF 对比表自陈 LARGE effort，3-4 周。v1 不发，重新定位为"Single-Agent enterprise"。 | v1 release notes 显式标注 "Multi-agent: planned for v1.1"，删除前端任何"团队 / 协作 Agent"占位入口 |
| **MCP Tool Discovery 完整版** | `2026-05-07-v1.0-mcp-tool-discovery.md` 是后写的；v1 仅需保留 stub，避免做一半。 | controller stub 返回 501 Not Implemented，明示 v1.1 |
| **Workflow BPMN 可视化编辑器** | Flowable 7 后端就绪，但前端可视化编辑成本极高。v1 只暴露"导入 BPMN XML"。 | 移除任何 workflow drag-drop UI 入口；保留后端 + JSON/XML 导入 API |
| **schemaplexai-admin 全量后台** | 空模块 (technical-debt P1) 不在 v1 补。v1 用 SQL + 命令行做租户管理足矣。 | README 显式说明"v1 admin via CLI / SQL only" |
| **Long-term vector memory** | technical-debt P1，v1 仅 Redis L1 + PG L2 已足够 demo。 | 不做向量长记忆，相关 UI/API 全部隐藏 |
| **Reflection / Goal Setting / Exploration / Learning 高阶模式** | 已有代码骨架但产品上无人能用。v1 隐藏入口，避免曝露半成品。 | UI 入口加 feature flag，默认关闭 |

## 5. 给用户的关键问题

> **v1 GA 当天的 launch tweet 一句话写什么？这决定 Reduce 取舍。**
>
> 我准备了 3 个候选叙事（见第 6 节）。你（产品负责人 / CEO）必须在 launch 前 2 周内勾选其中 1 个，因为这一句话决定：
> - 首页 hero 的视觉与文案；
> - 哪些功能必须到 10/10（Expand），哪些可以停在 6/10（Maintain）；
> - Reduce 名单上 6 项中是否还要再砍。
>
> 如果你不确定，我倾向候选 ②"开发者门户里的 AI 副驾"，因为它最能利用现有资产 + 避开 Cursor/Devin 红海。

补充建议（次要问题）：

1. **是否同意把竞品参考组从 Cursor/Devin 切换到 Backstage/Port/Cortex**？这一步改动会让"TTHW 落后 20-100x"从灾难变成行业常态。

2. **v1 launch 客户是谁**？10 个内部 dogfood？50 个 design partner？1000 公开注册？这决定 schemaplexai-admin 是否真的能延到 v1.1。

3. **Pricing 模型是 seat-based 还是 token-based**？后者匹配现有 cost-analytics 资产，前者更易销售。建议 v1 公开"per-tenant 试用免费 + token-metered 付费"双轨。

## 6. 「十星级产品」候选叙事

### 候选 ① "Cursor for Enterprise AI Engineering"
**一句话**：SchemaPlexAI 是给企业 AI 团队的多租户 Cursor——同样的"对话即代码"，但带成本预算、质量门禁、租户隔离。
**为什么是 10 星**：直接借 Cursor 心智，转换成本低；CFO 能听懂"成本预算"、安全官能听懂"租户隔离"。
**为什么可能只是 7 星**：会被直接拿来对标 Cursor，TTHW 落后 20-100x 立刻被打脸；且 Cursor for Business 已存在。

### 候选 ② "开发者门户里的 AI 副驾"（推荐）
**一句话**：SchemaPlexAI 是 Backstage/Port 之上的 AI 协作层——Spec 即工单、Agent 即工程师、ClickHouse 即工时表，让 AI 研发产出可被 CFO/CTO/安全官同时读懂。
**为什么是 10 星**：
- 避开 Cursor/Devin 红海，进入 Backstage/Port/Cortex 蓝海；
- 利用现有最强资产（多租户 + cost analytics + quality gates）；
- 对企业买家而言"门户 + AI"是 2026 增长最快的预算线；
- 10/10 验证：CTO 可以一句话向 CFO 解释为什么要花这个钱（"AI 工程的 Datadog"）。

**为什么可能只是 7 星**：要求团队接受"我们不是 IDE"的认知重构；landing page 与现有 README 几乎要全改。

### 候选 ③ "AI 研发的 SOC2 控制面"
**一句话**：所有 AI agent 都跑在 SchemaPlexAI 上——你才能告诉审计师"谁在什么 tenant 用什么模型花了多少钱、产出过什么代码、是否被人审核过"。
**为什么是 10 星**：合规是 2026 企业 AI 第一卡点；当前 cost analytics + quality gate + tenant 隔离 + audit trail 资产几乎为该叙事量身打造；MAF/Cursor/Devin 都不打这个赛道。
**为什么可能只是 7 星**：销售周期长（合规采购 6-12 个月），launch 当天难有快速反馈；要求 v1 必须出 SOC2-ready 的审计日志（当前 OpenTelemetry 还在 Phase 1 待办）。

---

**最终建议**：选候选 ②，按本文 4 节 4 模式范围决策执行，预计 v1 launch 窗口 4 周内可达。若选①需重做差异化，若选③需追加 OpenTelemetry + 审计日志至 Expand。
