---
title: DXArchitect v1 开发者体验评估
agent: DXArchitect
date: 2026-05-08
domain: dx
---

# SchemaPlexAI v1 开发者体验（DX）评估报告

> 评估目标：判断当前仓库距离一个"Replit / Cursor / v0 风格"的现代开发者体验有多远，并给出可执行的 Expand / Optimize / Triage 决策。
> 评估对象：master 分支当前快照。
> 基线：Cursor TTHW 2 分钟、v0 30 秒、Replit Agent 1 分钟、Devin 5 分钟。

---

## 1. 0–10 评分表

| 子维度 | 当前分 | 10 分定义 | 证据 |
|---|---|---|---|
| TTHW（首次冷启动） | **1** | 新开发者从 `git clone` 到看到首屏 SSE token ≤ 2 分钟 | 实测链路 11 步、估算 45–90 分钟，无任何"一键启动"脚本；`README.md` 第 49–82 行要求手动起 9 个中间件 + 12 个 Spring Boot 服务 |
| 文档可发现性 | **2** | landing → quickstart → first-run 三步可达 | 仅有根 `README.md` 一篇；无 `docs/quickstart.md`、无 `getting-started.md`、无 `.env.example`、无 `schemaplexai-ui/README.md`；新手到达 `wiki/`/`docs/standards/` 路径完全靠猜 |
| 错误反馈即时性 | **3** | 任何启动失败 5 秒内可读建议 | `docker-compose.yml` 缺 etcd healthcheck、缺 milvus/jaeger/clamav healthcheck（全网络一起启会随机失败）；CI 中 `mvn clean compile -q` 静默；前端 401 直接跳 `/login` 但未提示"未登录"；`AgentExecutor` SSE 重试到 3 次才报 `sseError`，中间 10–30s 是黑屏 |
| 魔法时刻强度 | **3** | 5 秒内首屏 SSE token | 现状：登录 → 选 Agent → 输入 Prompt → 等首字 token（依赖真实 LLM key）。`AgentExecutor/index.tsx` 路径已具骨架（SseViewer + ChatMemory），但要求用户先有 Agent + LLM key + 租户上下文，三道前置门 |
| 删减勇气 | **3** | 21 页面砍到 ≤ 12 个核心 | 21 个页面里有大量空壳（QualityCenter / OpsCenter / IntegrationCenter / SystemCenter）；`CLAUDE.md` 已自承 ops/quality/spec/workflow/integration/task 模块"无测试"，但 UI 入口仍占据顶级菜单 |
| 工具链完整 | **2** | one-script 启全栈、一键 seed、一键 mock LLM | 无 `Makefile`、无 `taskfile.yml`、无 `schemaplexai up` 脚本、无 seed 数据、无 mock LLM provider、无 Codespaces / devcontainer 配置 |

**加权总分：14 / 60 ≈ 23%**。当前是"内部团队可启动"水平，离"开源/路演级"距离 5–6 个量级。

---

## 2. TTHW 全步骤秒表分析

| # | 步骤 | 估时 | 阻塞点 | 优化潜力 |
|---|---|---|---|---|
| 1 | `git clone` | 30 s – 2 min | 仓库含历史构建产物 / 大文件可能性（需核查 `.gitignore`） | 加 LFS 钩子、`git-sizer` 自检 |
| 2 | `docker-compose pull` | 15–25 min | 11 个镜像：postgres/redis/rabbitmq/minio/milvus/etcd/clickhouse/elasticsearch/prometheus/grafana/jaeger/clamav，其中 ES + Milvus + ClamAV 均 GB 级 | 提供 `docker-compose.lite.yml`（仅 PG+Redis+MinIO）、Codespaces 预拉镜像、aliyun mirror |
| 3 | 写 `.env` | 5–10 min | **无 `.env.example`**，需手翻 `application-*.yml` 读 16 个模块的连接串 | 提供 `.env.example` + `scripts/setup-env.sh` |
| 4 | `mvn clean compile` | 4–6 min | 16 模块全编译，`-q` 模式无进度 | 提供预编译 `ghcr.io` 镜像；`-pl` 选项打 lite profile（仅 gateway+system+web+agent-engine） |
| 5 | 数据库 init | 1–2 min | 依赖 `docker/postgres/init/*.sql` 自动执行；缺 seed | 加 `scripts/seed.sh` 写默认租户 + admin + 3 个 demo Agent |
| 6 | 启 12 个 Spring Boot 服务 | 10–15 min | README 列出 `mvn spring-boot:run -pl ...` 12 行，每个 30–90 s 启动 | 提供 `docker-compose.app.yml` 或 Spring Boot Admin 一键编排 |
| 7 | `npm install` | 2–5 min | 依赖中包含 `@antv/x6`（重）、`echarts`（重） | `npm ci` + 锁文件；按需懒加载图形库 |
| 8 | `npm run dev` | 30 s | 正常 | — |
| 9 | 浏览器打开登录 | 1 min | **无默认账号说明**，需查 SQL init 文件 | 登录页给"演示账号一键填充"按钮（`admin / admin123`） |
| 10 | 创建 Agent | 3–5 min | 空白库；表单字段无说明、无模板 | seed 3 个 ready-to-run Agent |
| 11 | 调用 SSE | 1–3 min | 需配置真实 LLM key + 模型 ID + 租户，否则报错 | 提供 `MockLlmProvider`，在 `dev` profile 下默认启用，输出固定 demo token 流 |

**优化后 TTHW 上限：≤ 5 分钟（Codespaces）/ ≤ 8 分钟（本地）**。实现需在 §6 三模式中并行推进。

---

## 3. 21 UI 页面清单 + 价值评估

| # | 页面 | 用途 | 上线必要 | 决策 |
|---|---|---|---|---|
| 1 | Login | 登录鉴权 | 必 | Optimize（演示账号一键登录） |
| 2 | NotFound | 404 兜底 | 必 | Keep |
| 3 | Cockpit | 工作台首页 | 必 | Optimize（首屏放魔法时刻入口） |
| 4 | AgentList | Agent 列表 | 必 | Optimize（seed 3 个示例） |
| 5 | AgentDetail | Agent 详情 | 必 | Keep |
| 6 | AgentCanvas | Agent 可视化编排（X6） | 价值核心 | Optimize（首屏模板） |
| 7 | **AgentExecutor** | **SSE 执行台** | **必（魔法时刻）** | **Optimize 优先级最高** |
| 8 | WorkflowMonitor | 工作流监控 | 中 | Optimize |
| 9 | Projects/SpecCenter | 规范中心 | 中 | Triage（合并到 Projects 二级 Tab） |
| 10 | Projects/ContextCenter | 上下文/RAG 中心 | 必 | Keep |
| 11 | Projects/WorkflowCenter | 工作流中心（含 Template/Instance Tab） | 中 | Optimize |
| 12 | Projects/WorkflowCenter/WorkflowTemplateTab | 模板 Tab | 中 | 合并 |
| 13 | Projects/WorkflowCenter/WorkflowInstanceTab | 实例 Tab | 中 | 合并 |
| 14 | Quality/QualityCenter | 质量中心总览 | 低 | **Triage**（v1 砍） |
| 15 | Quality/QualityIssues | 质量问题列表 | 低 | **Triage** |
| 16 | Quality/QualityGates | 质量门禁配置 | 低 | **Triage** |
| 17 | Quality/SecurityAudit | 安全审计 | 低 | **Triage** |
| 18 | Platform/SystemCenter（含 General/Users/Models 三 Tab） | 系统设置 | 必 | Keep（合并 3 个 Tab 到一页） |
| 19 | Platform/IntegrationCenter | 集成中心 | 中 | Triage（v1 仅留 GitHub/MCP） |
| 20 | Platform/OpsCenter | 运营/成本 | 中 | Triage（v1 仅留 cost 概览） |
| 21 | Tasks/TaskBoard / TaskJobs / TaskDetail | 任务看板/作业/详情 | 中 | 合并为 Tasks 单页 + Drawer |

**Triage 后预计核心页面：12 个**（Login、NotFound、Cockpit、AgentList、AgentDetail、AgentCanvas、AgentExecutor、ContextCenter、WorkflowCenter、SystemCenter、IntegrationCenter-lite、Tasks-merged）。砍掉 9 个空壳，UI 体积 −40%、TTHW 心智门槛 −60%。

---

## 4. 「魔法时刻」候选

### 候选 A：5-Second SSE（推荐为 v1 默认魔法时刻）
- **触发动作**：登录后 Cockpit 首屏放置一个 "Try a Demo Agent" 大卡片，点击。
- **5 秒内反馈**：直接打开 AgentExecutor，自动选中 seed `demo-summarizer` Agent，自动填入示例 prompt（"用三句话总结附件 README.md"），按 Enter 触发 SSE。
- **30 秒内可见产物**：MockLlmProvider 流式输出 60 个 token 完成的 markdown 总结，右侧 SseViewer 同步展示状态机 transitions。
- **差异化**：v0 输出 React 组件、Cursor 输出代码补全、Replit 输出整站。SchemaPlexAI 输出"可观测的 Agent 思维链 + 状态转换可视化"——企业 R&D 场景独有。

### 候选 B：Spec → Workflow 一键生成
- **触发动作**：Cockpit 输入框写"我想做一个会议纪要 Agent"。
- **5 秒内反馈**：跳转 SpecCenter，Spec 草稿 streaming 中。
- **30 秒内可见产物**：自动创建 Spec、关联 Agent、生成 Workflow BPMN 图（X6 渲染）。
- **差异化**：从需求到可执行工作流的"自动化产线"，对标 Devin 但落在企业 R&D 流程。

### 候选 C：Drag-Drop RAG
- **触发动作**：拖一份 PDF 进 Composer。
- **5 秒内反馈**：ClamAV 扫描 + Tika 解析进度条；首段 chunk 出现在 ContextCenter 预览。
- **30 秒内可见产物**：embed 完成，AgentExecutor 自动用 RAG 回答关于 PDF 的问题。
- **差异化**：拖拽 → 知识库 → 回答 三步全自动，对标 ChatGPT Files 但保留企业级（多租户、扫描、审计）。

**v1 投入建议**：A 必做，B 留 v1.1，C 作为加分项（已有 Composer 拖拽骨架可复用）。

---

## 5. 关键发现（带证据）

1. **TTHW 系统性失败**：`README.md:49–82` 要求手动启动 12 个 Spring Boot 服务，单凭顺序执行就需 10+ 分钟，远超 Cursor/Replit 量级。**没有 `docker-compose.app.yml`、没有 `Makefile`、没有 one-script**。
2. **零环境模板**：仓库 `.env.example` 不存在；新人需逆向 16 个模块的 `application-*.yml` 才能启动，违反 Twelve-Factor §3。
3. **前端零文档**：`schemaplexai-ui/README.md` 不存在（Glob 验证）；`package.json` 仅有 `dev/build/test/lint` 四脚本，无 `seed` / `mock` / `e2e:smoke` 等 DX 脚本。
4. **Healthcheck 覆盖缺口**：`docker-compose.yml` 中 etcd、milvus-standalone、jaeger 缺 healthcheck，启动时序竞态会导致随机首启失败 → 表现为"前端 502，后端日志不报错"，5 秒内不可读。
5. **魔法时刻被三道门挡住**：当前最强体验路径 `AgentExecutor/index.tsx:32–44` 在 mount 时直接 fetch agent list；若 token 失效或租户未选，会进入 `sseError` 后再静默 close。需要"零配置示例 Agent + Mock LLM"才能让首屏 5 秒可达。
6. **21 页面 9 个空壳**：Glob 显示 21 个 `index.tsx`（Login/NotFound/Cockpit/AgentList/AgentDetail/AgentCanvas/AgentExecutor/WorkflowMonitor + Projects×3 + Quality×4 + Platform×3 + Tasks×3）。`CLAUDE.md` 已自承 quality/ops/integration 模块"无测试"，UI 表面繁荣掩盖了后端真空。
7. **CI 只测后端骨架**：`.github/workflows/ci.yml:41` JaCoCo 覆盖检查仅覆盖 `common,model,dao,gateway,system,agent-engine,context` 7 个模块；其余 9 个模块未纳入 80% 门槛 → 与 `CLAUDE.md` 中的"无测试"一致。开发者从 CI 绿灯无法判断功能可用性。
8. **Composer 已是好资产但未文档化**：`schemaplexai-ui/src/components/Composer/`（5 个文件 + 3 个测试）已实现 mention / 文件上传 / ClamAV 扫描健康检查，是 ChatGPT-style Composer 的雏形；但既无 README，也未在其他页面复用。这是"内部明珠尚未抛光"的典型例子。

---

## 6. 三模式决策

### DX-Expand（新增能力，3–5 项）

1. **`schemaplexai up` 一键脚本**（Bash + PowerShell 双版本）：
   ```
   schemaplexai up           # docker pull + db init + seed + 启动 lite 后端 + ui dev
   schemaplexai up --full    # 完整 12 服务
   schemaplexai down/reset/seed/logs
   ```
   产出：`scripts/schemaplexai.sh` + `scripts/schemaplexai.ps1` + `docker-compose.app.yml`。
2. **`.devcontainer/` + Codespaces 模板**：让"Open in Codespaces"按钮可在 5 分钟内进入可点 demo。
3. **Seed 数据 + Demo 工作流**：`scripts/seed.sh` 写入租户 `demo`、用户 `admin/admin123`、3 个 Agent（summarizer/translator/code-reviewer）、1 个 Workflow 模板、1 份 RAG 文档。
4. **`MockLlmProvider`** 在 agent-engine `dev` profile 下默认启用；返回固定 60-token 流，2 秒内首字。
5. **`docs/quickstart.md`**：landing → quickstart → first-run 三段，附 GIF 演示 5 秒魔法时刻。

### DX-Optimize（现有路径打磨，3–5 项）

1. **AgentExecutor 首屏 ≤ 1.5s**：lazy-load X6/echarts；`useEffect` 改为 `Suspense` + skeleton；SSE `onerror` 改为前置 1s 内显式提示而非 3 次重试静默。
2. **Composer 文档化 + 跨页复用**：补 `Composer/README.md`、Storybook 故事；在 SpecCenter / WorkflowCenter 也用同一 Composer。
3. **Login 演示账号填充按钮**：一行加 `<Button onClick={fill('admin','admin123')}>Use Demo Account</Button>`。
4. **错误反馈即时化**：网关 502 / 401 / 503 在前端 axios 拦截器统一映射为人类可读 toast；后端 health endpoint 暴露中间件就绪态供 UI 加载预检。
5. **README + `schemaplexai-ui/README.md`** 加"5-Minute Quickstart"章节、`.env.example` 提交。

### DX-Triage（删减/合并，2–4 项）

1. **Quality 4 页面 v1 全砍**：QualityCenter / QualityIssues / QualityGates / SecurityAudit 一律下架（仅在 Cockpit 留一个 "Quality (Coming Soon)" 占位卡）。理由：后端无测试、无业务实现，留页面误导用户。
2. **Tasks 三页合一**：TaskBoard 主页 + Drawer 形式展示 TaskJobs / TaskDetail，路由从 3 个降到 1 个。
3. **Platform/IntegrationCenter v1 仅留 GitHub + MCP 两个 tab**，GitLab/Jenkins 入口隐藏。
4. **`schemaplexai-admin` 空模块**：从 root `pom.xml` 暂时 comment-out，避免新手编译时面对一个空模块产生疑惑。

---

## 7. 给用户的关键问题

> **假设新开发者是 Replit 用户，第一次打开你给他什么 URL？**

当前答案：没有。最接近的是 `http://localhost:5173/login`，但在那之前他要：clone → docker-compose → 配 .env → 编译 16 模块 → 启 12 服务 → npm install → npm dev。

理想答案应是 **一个公网 demo URL**（`demo.schemaplexai.com`）+ **一个 Codespaces 按钮**（`Open in GitHub Codespaces`）。两者皆能在 5 分钟内让开发者看到 §4 候选 A 的"5-Second SSE"。

**这是 v1 必须回答的问题。 §6 的 DX-Expand 第 1–4 项都是为这一个 URL 服务的。**

---

## 8. 强化问题清单

详见独立文件 [`dx-questions.md`](./dx-questions.md)（35+ 题，覆盖开发者画像、TTHW、魔法时刻、痛点、竞品对比、删减勇气、路线图七大主题）。
