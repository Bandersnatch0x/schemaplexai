# Spec 合规核查：Frontend UI/UE Alignment (v1.0)

- **规格文档**：`docs/specs/2026-05-07-v1.0-ui-alignment.md`（status: approved, version 1.0）
- **核查目标**：`schemaplexai-ui/src/`
- **核查日期**：2026-08-29
- **方法**：逐条提取规范性需求 → 文本搜索定位 → 通读执行点代码与关联接口层 → 走通加载/失败/空态/权限路径 → 裁决。质量门禁通过实际运行 `tsc --noEmit`、`vitest run`、`npm run build` 验证。

## 裁决总览

| Verdict | 数量 | REQ |
|---|---|---|
| implemented | 24 | 01–06, 09–17, 19, 21, 22, 24, 25, 29, 30 |
| partial | 4 | 07, 08, 18, 23 |
| absent | 1 | 20 |
| contradicted | 4 | 26, 27, 28 |
| stronger-than-spec | 0 | — |
| undecidable | 0 | — |

---

## REQ-01 — 双布局模式与尺寸（Immersive 52px 图标栏 + 浮动头 / Progressive 200px 侧栏 + 48px 固定头）

> "Split routes into two layout modes: ImmersiveLayout — /cockpit, /canvas — Sidebar 52px icon-only, Header Floating overlay; ProgressiveLayout — /agents/*, /workflows, /specs, /contexts, /quality, /integrations, /ops, /notifications, /settings — Sidebar 200px with labels, Header 48px fixed" — spec §1

**Verdict:** implemented · confidence: high

**What this demands:** 两种布局组件存在；沉浸路由使用 52px 纯图标侧栏与浮动覆盖式头部；渐进路由使用 200px 带标签侧栏与 48px 固定头部；规格列出的路径均可达且落入对应布局。

**Where enforcement lives:**
- `src/router/index.tsx:47-54`（/cockpit → ImmersiveLayout）、`:56-63`（/agents/canvas → ImmersiveLayout）
- `src/components/Layout/Layout.css:96`（.layout-icon-sidebar width 52px）、`:180-196`（.layout-floating-header position:absolute 浮动覆盖）、`:226`（.layout-progressive-header height 48px）、`:271`（.layout-progressive-sidebar width 200px）
- `src/components/Layout/ImmersiveLayout.tsx:36-65`（图标栏，标签仅作 hover tooltip :55-57）、`:69-78`（浮动头部）
- `src/components/Layout/ProgressiveLayout.tsx:79-87`（48px 固定头部）、`:90-95`（200px 侧栏，DomainNav 带标签）

**Paths walked:**
- ✓ /cockpit、/agents/canvas 渲染 ImmersiveLayout（52px + 浮动头）
- ✓ /agents、/projects、/quality、/platform、/tasks 渲染 ProgressiveLayout（200px + 48px 头）
- ✓ 规格列出的旧路径 /workflows /specs /contexts /integrations /ops /settings /notifications /canvas 全部保留为 legacy redirect（router:145-152），落地页均在 ProgressiveLayout/ImmersiveLayout 内

**How the verdict was reached:** 布局尺寸与结构逐像素对得上规格表。文档漂移注记：规格写 `/canvas`，代码将画布迁至 `/agents/canvas` 并保留 `/canvas` 重定向（router:152）；规格按平铺路径列举，代码重组为领域嵌套路由（/projects、/platform、/quality 子页、/tasks 为规格未提及的新域）。旧路径全部可达，机制等价 → implemented 而非 contradicted。

---

## REQ-02 — `/` 与 `/dashboard` 重定向到 `/cockpit`

> "`/` and `/dashboard` redirect to `/cockpit`" — spec §1

**Verdict:** implemented · confidence: high

**What this demands:** 两条入口路径均 302 式跳转到 /cockpit。

**Where enforcement lives:** `src/router/index.tsx:42`（/ → Navigate /cockpit replace）、`:43`（/dashboard → Navigate /cockpit replace）；目标路由存在（:47）。

**Paths walked:** ✓ `/` ✓ `/dashboard`，均带 `replace`（不污染历史栈）。

**How the verdict was reached:** 与规格逐字一致，无歧义。

---

## REQ-03 — 所有受保护路由包裹 `<RequireAuth>`，校验 localStorage 中的 `schemaplexai_token`

> "All authenticated routes wrapped in `<RequireAuth>` (checks `schemaplexai_token` in localStorage)" — spec §1

**Verdict:** implemented · confidence: high

**What this demands:** 每个渲染业务页面的路由都被 RequireAuth 包裹；无令牌时跳转登录；令牌键名为 `schemaplexai_token`。

**Where enforcement lives:** `src/router/index.tsx:32-38`（RequireAuth：`localStorage.getItem('schemaplexai_token')`，空则 `<Navigate to="/login" replace />`）；包裹点：/cockpit :48-52、/agents/canvas :58-61、/agents :69-71、/projects :85-87、/quality :101-103、/platform :117-119、/tasks :133-135。键名与 `src/utils/token.ts:1` 的 `TOKEN_KEY='schemaplexai_token'` 一致。

**Paths walked:**
- ✓ 无令牌 → 跳转 /login
- ✓ 有令牌 → 渲染子路由
- ✓ 未包裹项仅 /login（:41）、纯重定向（:42-43、:145-152）、`*`（:154）——与规格" `*` 渲染 NotFound（未要求鉴权）"一致

**How the verdict was reached:** 无遗漏的受保护路由。附注（不构成违规）：RequireAuth 直接读裸键，未做 `utils/token.ts:19-22` 中的过期校验——过期但未清除的令牌可通过路由守卫，仅在 axios 401 流程中被拒（见反向差距 #8）。

**令牌传递方式专项核查（已知历史待办）**：令牌不再进入 URL。axios 走 `Authorization: Bearer` 头（`src/api/request.ts:16-19`）；SSE `EventSource` 用 `withCredentials: true` 且不带令牌参数（:100-104）；Searched `token=|\?token|searchParams.*token` → src 内 0 命中。令牌仍存 localStorage（`utils/token.ts:6-13` SECURITY NOTE 已声明 XSS 风险与 Cookie 化路线）——规格仅要求从 localStorage 校验，故合规。

---

## REQ-04 — `*` 路由渲染 `<NotFound />`

> "`*` route renders `<NotFound />`" — spec §1

**Verdict:** implemented · confidence: high

**What this demands:** 兜底路由存在且渲染 404 页面。

**Where enforcement lives:** `src/router/index.tsx:154`（`{ path: '*', element: <NotFound /> }`）；`src/pages/NotFound/index.tsx:6-19`（antd Result status="404" + 返回首页按钮，NotFound.css 提供样式）。

**Paths walked:** ✓ 任意未匹配路径渲染 NotFound（懒加载经 App.tsx:19 Suspense）。

**How the verdict was reached:** 逐字满足。

---

## REQ-05 — Cockpit 从 Hive 导入 HexIcon/StatCard，移除内联定义

> "Remove inline HexIcon/StatCard definitions (80+ lines); Import `HexIcon`, `StatCard` from `@/components/Hive`" — spec §2

**Verdict:** implemented · confidence: high

**What this demands:** 页面内不得再有本地 HexIcon/StatCard 定义；二者来自 Hive 组件库。

**Where enforcement lives:** `src/pages/Cockpit/index.tsx:4`（`import { HexIcon, StatCard } from '@/components/Hive'`）；使用点 :112-115（HexIcon）、:122-124（StatCard）；导出源 `src/components/Hive/index.ts:1,3`。文件内无任何本地组件定义（通读全文件 136 行确认）。

**Paths walked:** ✓ 正常渲染 ✓ loading 态（值显示 '—'，:46 等）。

**How the verdict was reached:** 导入与使用均在位，内联定义不存在。

---

## REQ-06 — Cockpit 内联样式全部提取至 Cockpit.css

> "Extract all inline styles to `Cockpit.css` (157 lines)" — spec §2

**Verdict:** implemented · confidence: high

**What this demands:** 页面样式集中于 Cockpit.css，组件内无 style 对象。

**Where enforcement lives:** `src/pages/Cockpit/index.tsx:5`（import './Cockpit.css'）；`src/pages/Cockpit/Cockpit.css` 实际 **157 行**（`wc -l` 验证，与规格数字一致）；通读 index.tsx 全文，`style={{` 出现 0 次。

**Paths walked:** ✓ 连接/断开态样式（.status-connected/.status-disconnected，CSS:27-32）✓ 断开通知样式（.cockpit-disconnect-notice，CSS:38-52）。

**How the verdict was reached:** 行数、职责、零内联三点全部吻合。

---

## REQ-07 — Cockpit 保持 `getAgentStats()` API 集成与 30s 轮询

> "Maintain API integration with `getAgentStats()` and 30s polling" — spec §2

**Verdict:** partial · confidence: high

**What this demands:** 页面通过真实 API 调用获取统计数据，并以 30 秒周期刷新。

**Where enforcement lives:**
- 轮询：`src/pages/Cockpit/index.tsx:25-29` —— `setInterval(fetchStats, 30000)` + 卸载时 `clearInterval` ✓
- 调用：`:34` `await getAgentStats()` ✓
- **缺口**：`src/api/agent-engine.ts:14-22` —— 函数注释 `/** Stub — no backend endpoint exists yet. Returns zeroed stats. */`，实现为 `return Promise.resolve({ totalAgents: 0, totalExecutions: 0, totalTokens: 0, pendingApprovals: 0 })`，**不发起任何 HTTP 请求**。

**Paths walked:**
- ✓ 轮询启动/清理路径
- ✗ 真实 API 请求路径不存在——统计永远为 0，与后端状态无关

**Searched:** `getAgentStats` → 唯一定义 `api/agent-engine.ts:15`（stub）；`src/api` 内 `Promise.resolve|Stub` → 仅此一处。

**How the verdict was reached:** 调用结构、函数名、轮询周期与规格逐字一致，但 "API integration" 的实质（真实请求）缺失，故非 implemented；代码未做出与规格相反的行为，故非 contradicted。**severity: High** —— 规格强制的数据集成在所有可达状态下都未真实生效（页面恒显 0）。

---

## REQ-08 — Cockpit API 不可达时显示离线通知

> "Add offline disconnection notice when API unreachable" — spec §2

**Verdict:** partial · confidence: high

**What this demands:** 当 API 请求失败/不可达时，页面展示断开提示。

**Where enforcement lives:** `src/pages/Cockpit/index.tsx:37-38`（catch → `setApiStatus('disconnected')`）、`:85-89`（disconnected 时渲染 `.cockpit-disconnect-notice`，文案 `cockpit.backendUnavailable` 在 en/zh locale 均存在）、`:75-77`（状态栏离线指示）；样式 `Cockpit.css:38-52`。

**Paths walked:**
- ✗ **失败路径当前不可达**：`getAgentStats()` 是永不 reject 的 `Promise.resolve`（agent-engine.ts:16），catch 分支为死代码，离线通知在当前构建中不可能出现
- ✓ 若 getAgentStats 接入真实端点，失败 → 通知的链路完整可用

**How the verdict was reached:** UI 机制完整但触发条件被 stub 短路——特定状态（API 不可达）在当前实现下无法出现。非 absent（代码完整存在），非 implemented（无任何可达路径能展示）。**severity: High**。

---

## REQ-09 — AgentCanvas 三视图提取为顶层独立组件

> "Extract `TopologyView`, `ListView`, `CodeView` as independent top-level components (avoid re-creation on each render)" — spec §3

**Verdict:** implemented · confidence: high

**What this demands:** 三个视图组件定义在模块顶层，不在主组件渲染函数内重建。

**Where enforcement lives:** `src/pages/AgentCanvas/index.tsx:154`（TopologyView）、`:273`（ListView）、`:295`（CodeView）均为模块级函数定义；主组件 `AgentCanvas` 自 :307 开始，仅通过 props 调用（:460-470）。

**Paths walked:** ✓ topology/list/code 三态切换（:459-470）均渲染同一份组件定义。

**How the verdict was reached:** 定义位置与规格意图（避免每次渲染重建）完全一致。

---

## REQ-10 — 连线端点查找改用 `nodeMap.get` O(1)

> "Replace `nodes.find((n) => n.id === conn.from)` with `nodeMap.get(conn.from)` for O(1) lookup" — spec §3

**Verdict:** implemented · confidence: high

**What this demands:** 连线渲染通过 Map 查找节点，不做线性扫描。

**Where enforcement lives:** `src/pages/AgentCanvas/index.tsx:317-321`（useMemo 构建 `Map<string, CanvasNode>`）、`:186-187`（`nodeMap.get(conn.from)` / `nodeMap.get(conn.to)`）、`:324`（选中节点同样走 nodeMap）。

**Paths walked:** ✓ 连线两端存在 → 渲染贝塞尔路径；✓ 端点缺失 → `return null` 守卫（:188）。

**Searched:** `nodes.find` in AgentCanvas → 0 命中。

**How the verdict was reached:** 旧模式无残留，新模式覆盖全部查找点。

---

## REQ-11 — 节点 ID 改用 `crypto.randomUUID()`

> "Replace `Date.now()` with `crypto.randomUUID()` for collision-safe node ID generation" — spec §3

**Verdict:** implemented · confidence: high

**What this demands:** 新增节点使用防碰撞的随机 UUID。

**Where enforcement lives:** `src/pages/AgentCanvas/index.tsx:330` —— `const id = \`n${crypto.randomUUID()}\``（handleAddNode 内）。

**Paths walked:** ✓ 四类节点（agent/condition/start/end）新增均走同一 ID 生成（:414-432 四个工具栏按钮 → handleAddNode）。

**Searched:** `Date.now()` in `src/pages/AgentCanvas/` → 0 命中。

**How the verdict was reached:** 替换完成且无旧模式残留。

---

## REQ-12 — ListView Table 空态文案

> "Add `locale={{ emptyText: t('common.noData') }}` to ListView Table" — spec §3

**Verdict:** implemented · confidence: high

**What this demands:** 列表视图的 antd Table 在无数据时显示国际化空态。

**Where enforcement lives:** `src/pages/AgentCanvas/index.tsx:284`（`locale={{ emptyText: t('common.noData') }}`）；i18n 键存在（`i18n/locales/en.json:14` "No data"、`zh.json:14` "暂无数据"）。

**Paths walked:** ✓ 空态可达：工具栏 "Clear Canvas"（:440-446 → handleClear :347-350 置空 nodes）后切换 List 视图即显示 emptyText。

**How the verdict was reached:** 属性与键名逐字一致，空态路径实际可达。

---

## REQ-13 — `ListViewProps.listColumns` 使用 `ColumnsType<CanvasNode>`

> "Fix `ListViewProps.listColumns` type using `ColumnsType<CanvasNode>` from antd" — spec §3

**Verdict:** implemented · confidence: high

**What this demands:** listColumns 具备 antd 官方列类型而非 any/手写类型。

**Where enforcement lives:** `src/pages/AgentCanvas/index.tsx:20`（`import type { ColumnsType } from 'antd/es/table'`）、`:268-271`（`interface ListViewProps { nodes: CanvasNode[]; listColumns: ColumnsType<CanvasNode> }`）。

**Paths walked:** ✓ `npx tsc --noEmit` 对该文件零报错（全项目仅 2 处测试文件报错，见 REQ-26）。

**How the verdict was reached:** 类型声明逐字匹配规格。

---

## REQ-14 — AgentDetail 身份卡 + 四 Tab 结构

> "Refactored into identity card + tabbed layout: Identity Card: Avatar, name, ID, version, status badge, LLM model badge; Tabs: Metrics / Logs / Charts (placeholder) / Config; Metrics Tab: Execution metrics (success rate, latency, runs) + resource usage (tokens, cost, cache hit); Logs Tab: TerminalLog component from Hive; Config Tab: JSON dump of agent configuration" — spec §4

**Verdict:** implemented · confidence: high

**What this demands:** 页面由身份卡与四个语义明确的 Tab 组成，各 Tab 内容如规格所列。

**Where enforcement lives:** `src/pages/AgentDetail/index.tsx`
- 身份卡：avatar :100-102、name :104-106、ID/version/status :108-113（status 经 statusMap :89-93 国际化）、LLM badge :117-119
- Tabs 定义 :53-58（metrics/logs/charts/config），条件渲染 :142-145
- MetricsTab :151-169：执行指标（successRate/avgLatency/totalRuns，:61-63）+ 资源（tokens/cost/cacheHit，:67-69）
- LogsTab :217-219 → TerminalLog（Hive 导入 :6，渲染 :212）
- ChartsTab :221-227 占位（规格 Out of Scope 明示）
- ConfigTab :229-255：`JSON.stringify` 配置 dump :236-250

**Paths walked:**
- ✓ loading：Spin 包裹 :96
- ✓ 失败：catch → `message.error` :81-83
- ✓ agent 为 null：ConfigTab 显示 `noAgentSelected` :251，name 回退 :105

**How the verdict was reached:** 规格枚举的结构性条目全部在场。注记（不改裁决）：指标数值（99.2% 等）、LOGS、version `2.3.1`（:108）、LLM 回退 `GPT-4`（:118）为硬编码占位——规格 §4 仅要求结构与 `getAgentDetail` 集成，未要求指标取自 API；列入反向差距。

---

## REQ-15 — AgentDetail 通过 `getAgentDetail(id)` 集成 API

> "API integration via `getAgentDetail(id)`" — spec §4

**Verdict:** implemented · confidence: high

**What this demands:** 页面按路由参数发起真实详情请求并消费结果。

**Where enforcement lives:** `src/pages/AgentDetail/index.tsx:5`（导入）、`:72-74`（useEffect [id] 触发）、`:76-87`（fetchAgent）；`src/api/agent-config.ts:29-31` —— `request.get<Agent>(\`/agent-config/agents/${id}\`)` 为真实 HTTP 调用。

**Paths walked:** ✓ 成功 → setAgent ✓ 失败 → message.error ✓ id 变化重新拉取（依赖数组含 id）。

**How the verdict was reached:** 端到端链路（路由参数 → API → 状态 → 渲染）完整。

---

## REQ-16 — WorkflowMonitor 移除 mock，动态时间线 + 09:00-12:00 回退

> "Remove all mock data (`MOCK_RUNS`, hardcoded `HOURS` array); Dynamic timeline generation via `buildTimelineMeta(runs)`: Computes start/end hours from actual API data; Generates hour labels dynamically; Falls back to 09:00-12:00 when no data" — spec §5

**Verdict:** implemented · confidence: high

**What this demands:** 时间轴完全由 API 数据驱动；无数据时回退固定窗口。

**Where enforcement lives:** `src/pages/WorkflowMonitor/index.tsx:49-64`（buildTimelineMeta：从 runs 的 startHour/endHour 求 min/max :53-56，循环生成小时标签 :59-62）；空数据回退 `{ start: 9, duration: 3, hours: ['09:00','10:00','11:00','12:00'] }` :50-51；useMemo 缓存 :104。数据来源 `mapInstanceToRun` :18-41（createdAt/updatedAt → startHour/duration）。

**Paths walked:** ✓ 有数据 → 动态轴 ✓ 无数据 → 09:00-12:00 回退 ✓ 跨小时边界（Math.floor/Math.ceil :55-56）。

**Searched:** `MOCK_RUNS` → 0 命中；`HOURS`（硬编码数组）→ 0 命中。

**How the verdict was reached:** 旧模式无残留，回退值与规格逐字一致。

---

## REQ-17 — Gantt 条基于 `timelineMeta.start/duration` 定位

> "Gantt-style bar positioning based on `timelineMeta.start` / `timelineMeta.duration`" — spec §5

**Verdict:** implemented · confidence: high

**What this demands:** 条形位置/宽度由时间线元数据百分比计算。

**Where enforcement lives:** `src/pages/WorkflowMonitor/index.tsx:204-207` —— `left: ((run.startHour - timelineMeta.start) / timelineMeta.duration) * 100}%`、`width: (run.duration / timelineMeta.duration) * 100}%`。

**Paths walked:** ✓ 常规条 ✓ 过滤后为空 → 空态块 :213-217（无条渲染）。

**How the verdict was reached:** 公式与规格指定的两个基准量一致。

---

## REQ-18 — PillNav 过滤标签（all/running/completed/failed）带状态计数

> "PillNav filter tabs (all/running/completed/failed) with status counts" — spec §5

**Verdict:** partial · confidence: high

**What this demands:** 四个过滤标签存在，且标签上展示对应状态的数量。

**Where enforcement lives:** `src/pages/WorkflowMonitor/index.tsx:106-111`（FILTERS 四项）、`:161-171`（按钮渲染，**仅显示 label**，:168）；计数 `statusCount` :116-121 已计算，但渲染在独立的统计徽章行 :147-150（StatBadge），不在标签上。过滤逻辑本身有效（:113-114）。

**Paths walked:** ✓ 四种过滤切换 ✓ 计数正确 ✗ 标签本体无计数。

**How the verdict was reached:** 标签与过滤完整，"with status counts" 的挂载位置与规格不符（计数在同页他处呈现）→ partial。**severity: Low** —— 纯呈现位置差异，信息未丢失。另注记：此处为自绘 pill 按钮，未复用 Hive `PillNav` 组件（Hive/index.ts:5 存在该组件）。

---

## REQ-19 — WorkflowMonitor API 集成与离线通知

> "API integration via `getWorkflowList()` + `getWorkflowInstances()`; Offline notice when API unreachable" — spec §5

**Verdict:** implemented · confidence: high

**What this demands:** 两个真实 API 调用；失败时展示离线通知。

**Where enforcement lives:** `src/pages/WorkflowMonitor/index.tsx:80-83`（Promise.all 并发两请求）、`:95-98`（catch → message.error + `setApiStatus('disconnected')`）、`:139-143`（离线通知）；`src/api/workflow.ts:14-16`（GET /workflow/templates/page）、`:48-50`（GET /workflow/instances/page）均为真实 HTTP。页面可达性：经 `src/pages/Projects/WorkflowCenter/WorkflowInstanceTab.tsx:1,4` 嵌入 /projects/workflows 实例标签。

**Paths walked:** ✓ 成功 → 时间线+表格 ✓ 失败 → 离线通知 ✓ 实例为空 → runs=[] → 空态 + 时间线回退 ✓ loading 指示 :138。

**How the verdict was reached:** 与 REQ-07/08 不同，此处接口层是真实请求，失败路径真实可达。

---

## REQ-20 — Dashboard 图表聚合与 Table 空态

> "Replace hardcoded 7-day chart data with `useMemo` aggregation from API records; Extract date keys from `records[].createdAt`, count per day, sort chronologically; Add `locale={{ emptyText }}` to Table" — spec §6

**Verdict:** absent · confidence: high

**What this demands:** Dashboard 页面存在，含基于 `records[].createdAt` 的按日聚合图表与带空态的 Table。

**Paths walked:** ✗ 页面不存在，无任何承载路径。

**Searched:**
- `src/pages` 文件树（find 全量列举）→ 无 `Dashboard/` 目录、无 `Dashboard` 命名的任何组件文件
- 模式 `Dashboard|dashboard` in `src` → 仅 3 类命中：`router/index.tsx:43`（/dashboard 重定向条目）、`i18n/locales/en.json:100-101` 与 `zh.json:100`（残留 i18n 命名空间，无组件消费）
- 模式 `pages/Dashboard`（import 路径）→ 0 命中

**How the verdict was reached:** 需求载体（页面文件）整体缺失，聚合逻辑与 Table 无任何等价实现 → absent（非 contradicted：无代码做出相反行为）。重要背景：**规格 §1 自身要求 `/dashboard` 重定向到 `/cockpit`，与 §6 互相矛盾**；代码选择了 §1。severity: **Low**（文档漂移，无用户可见破坏）。建议修订规格删除 §6。

---

## REQ-21 — 全局 ErrorBoundary 组件

> "Class component with `getDerivedStateFromError` + `componentDidCatch`; Abyss Hive themed error page: icon, title, description, error trace in monospace block; Actions: \"Reload Page\" (primary) + \"Go Home\" (ghost)" — spec §7

**Verdict:** implemented · confidence: high

**What this demands:** 类组件具备两个生命周期钩子；错误页含图标/标题/描述/等宽字体错误块与两个指定动作按钮。

**Where enforcement lives:** `src/components/ErrorBoundary/index.tsx:13`（class 组件）、`:19-21`（getDerivedStateFromError）、`:23-25`（componentDidCatch）、`:40-44`（图标 ⚠ / 标题 / 描述）、`:45-47`（`<pre class="error-boundary-trace">{error.message}`）、`:49-54`（Reload Page + Go Home）；`ErrorBoundary.css:7`（abyss 背景）、`:42`（trace `font-family: var(--font-mono)`）、`:66-70`（--primary 样式）、`:77-81`（--ghost 样式）。动作实现：`window.location.reload()` :28、`window.location.href='/'` :32。

**Paths walked:** ✓ hasError → 错误页 ✓ 正常 → children 直通 :61 ✓ error 为 undefined → trace 块被 :45 守卫跳过。

**How the verdict was reached:** 逐条比对全部在场。注记：文案为硬编码英文（未走 i18n），规格未要求。

---

## REQ-22 — ErrorBoundary 包裹 `<App>`

> "Wrapped around entire `<App>` in `App.tsx`" — spec §7

**Verdict:** implemented · confidence: high

**What this demands:** 应用根部被 ErrorBoundary 包裹。

**Where enforcement lives:** `src/App.tsx:4`（导入）、`:18`（`<ErrorBoundary>` 开）、`:30`（闭）——包裹 Suspense + Routes 全树。

**Paths walked:** ✓ 任意子树抛错 → 边界捕获渲染错误页。

**How the verdict was reached:** 包裹位置为最外层，符合 "entire `<App>`"。

---

## REQ-23 — 八页面 Table 空态 locale

> "All Ant Design `<Table>` components across pages receive `locale={{ emptyText: t('common.noData') }}`: AgentManager, ContextCenter, Dashboard, IntegrationCenter, OpsCenter, QualityCenter, SpecCenter, WorkflowCenter" — spec §8

**Verdict:** partial · confidence: high

**What this demands:** 列名页面的所有 antd Table 均带统一空态文案。

**Where enforcement lives:** 逐页核查（Searched 模式 `<Table|locale={`，src/pages 全量）：
- ✓ **AgentManager**（已更名 AgentList）：`AgentList/index.tsx:154`（`agent-mgr-*` 类名保留原命名痕迹）
- ✓ **ContextCenter**：`Projects/ContextCenter/index.tsx:89`
- ✗ **Dashboard**：页面不存在（见 REQ-20）
- ✓ **IntegrationCenter**：`Platform/IntegrationCenter/index.tsx:87` + `SkillsTab.tsx:197`
- ✓ **OpsCenter**：`Platform/OpsCenter/index.tsx:76`
- ✗ **QualityCenter**（按名）：`Quality/QualityCenter/index.tsx:4-15` 已退化为仅含 `Card`+`Empty` 的桩页（无 Table），且**未注册于路由**；质量域实际由 `QualityGates/index.tsx:50` ✓ 与 `QualityIssues/index.tsx:52` ✓ 承载（均有 locale）
- ✓ **SpecCenter**：`Projects/SpecCenter/index.tsx:88`
- ✓ **WorkflowCenter**：`WorkflowTemplateTab.tsx:85`（InstanceTab 嵌入 WorkflowMonitor，其"最近运行"为原生 `<table>`，不属 antd Table 约束范围）

额外合规（规格未列举）：AgentCanvas:284、SystemModelsTab:50、SystemUsersTab:33、TaskJobs:83、TaskDetail:87。

**Paths walked:** ✓ 6/8 列举页面空态生效 ✗ Dashboard、QualityCenter 按名缺失（前者被重定向取代，后者被质量子页取代）。

**How the verdict was reached:** 空态机制本身全面生效（现存所有 antd Table 均带 locale），但规格点名的 8 页中 2 页以描述形态不存在 → partial。**severity: Medium**。

---

## REQ-24 — 测试修复三项（§9）

> "`src/api/__tests__/request.test.ts`: Remove unused `MockInstance` import; `src/stores/__tests__/userStore.test.ts`: Add missing `vi` import; `src/components/__tests__/TenantSelector.test.tsx`: Remove unused `tenants` variable declarations from 2 test cases" — spec §9

**Verdict:** implemented · confidence: high

**What this demands:** 三处具体测试代码修正生效。

**Where enforcement lives:**
- `MockInstance`：Searched 全 src → **0 命中**；`request.test.ts:1` 导入列表干净（`describe, it, expect, vi, beforeEach, afterEach`）
- `userStore.test.ts:1`：`import { describe, it, expect, vi, beforeEach } from 'vitest'` —— vi 已导入
- `TenantSelector.test.tsx`：`tenants` 出现点（:27 初始状态、:43-50 测试数据声明与消费、:72）全部被使用，无未使用声明残留

**Paths walked:** ✓ 三个文件当前均无规格所述缺陷。

**How the verdict was reached:** 逐项验证缺陷已消除。注记：request.test.ts 现存一个**不同的**失败（刷新令牌契约过期），见 REQ-27，与 §9 所列修复无关。

---

## REQ-25 — App.tsx 清理（§10）

> "Wrap app with `<ErrorBoundary>`; Extract `<AppFallback>` component (centered Spin with Abyss background); Remove hardcoded Chinese \"加载中...\" text; Create `App.css` for fallback styling" — spec §10

**Verdict:** implemented · confidence: high

**What this demands:** 四项清理全部落地。

**Where enforcement lives:** `src/App.tsx:18`（ErrorBoundary 包裹）、`:8-14`（AppFallback：`.app-suspense` 容器 + `<Spin size="large" />`）；`src/App.css:1-7`（flex 居中 + `background: var(--abyss-bg)`）；Searched `加载中` → 组件源码 0 命中，仅 `i18n/locales/zh.json:3` 作为合法翻译资源存在。

**Paths walked:** ✓ 懒加载期间显示居中 Spin + Abyss 背景。

**How the verdict was reached:** 四项逐字满足；中文文案进入 i18n 资源是正确归宿而非违规。

---

## REQ-26 — 质量门：TypeScript 零错误 + 构建成功

> "TypeScript (`tsc --noEmit`) Zero errors; Build (`vite build`) 31.64s success" — spec Quality Gates

**Verdict:** contradicted · confidence: high

**What this demands:** 类型检查零错误，生产构建成功。

**Where enforcement lives（实测证据）:**
- `npx tsc --noEmit` 实际输出 2 处错误：
  - `src/components/Composer/__tests__/Composer.test.tsx(5,1): error TS6133: 'ComposerValue' is declared but its value is never read`
  - `src/components/Composer/__tests__/useFileUpload.test.ts(2,27): error TS6133: 'waitFor' is declared but its value is never read`
- `package.json` build 脚本为 `"tsc && vite build"`；实测 `npm run build`：**tsc 阶段报错退出，vite build 未执行**

**Paths walked:** ✗ 类型门 ✗ 构建门（同一根因）。

**How the verdict was reached:** 实测结果与门禁声明直接相反。根因：`tsconfig.json:16` `noUnusedLocals: true` + `:24` `include: ["src"]` 覆盖测试文件。**severity: High** —— CI 前端 job 的 `npm run build` 无 `|| true` 保护，当前提交将直接失败。修复成本极低（删除两处未用导入）。

---

## REQ-27 — 质量门：测试全部通过

> "Tests (`vitest run`) 91/91 pass" — spec Quality Gates

**Verdict:** contradicted · confidence: high

**What this demands:** 测试套件全绿。

**Where enforcement lives（实测证据）:** `npx vitest run` → **Test Files 1 failed | 21 passed (22)；Tests 1 failed | 145 passed (146)**。失败点：`src/api/__tests__/request.test.ts:178-185` —— 测试断言刷新请求为 `POST /auth/refresh` 空 body `{}` + 请求头 `X-Refresh-Token`；实现为 body `{ refreshToken }`（`api/request.ts:56`）并读取 `data.data.accessToken`（:57）。后端交叉验证：`schemaplexai-system/.../AuthController.java:42-43` 从请求体读 `refreshToken` 参数，`AuthService` 返回 `accessToken` 键 —— **代码与后端契约一致，是测试断言过期**。规格 "91/91" 数字亦过期（现为 146 个测试）。

**Paths walked:** ✗ 套件含 1 个失败用例。

**How the verdict was reached:** 实测与门禁声明相反 → contradicted。**severity: Medium** —— 生产行为正确（与后端一致），但红套件违反门禁、误导后续开发，应更新测试断言而非回改代码。

---

## REQ-28 — 质量门：无 MOCK_*/DEMO_*/SAMPLE_*

> "No MOCK_*/DEMO_*/SAMPLE_* Verified" — spec Quality Gates

**Verdict:** contradicted · confidence: high

**What this demands:** 代码库不存在以 MOCK_/DEMO_/SAMPLE_ 命名的常量。

**Where enforcement lives（证据）:** `src/components/Composer/useMentions.ts:4` —— `const MOCK_CANDIDATES: MentionCandidate[] = [...]`，**生产代码**，且于 :52 被消费（提及建议硬编码、未接 API）。其余命中均为测试夹具：`Hive/KanbanBoard.test.tsx:12`、`Hive/TaskCard.test.tsx:6`、`Hive/TaskList.test.tsx:13`。

**Paths walked:** ✗ 生产路径存在 MOCK_ 常量。

**Searched:** `MOCK_|DEMO_|SAMPLE_` in src → 命中如上，无遗漏文件。

**How the verdict was reached:** 门禁宣称 Verified 与生产代码中的 `MOCK_CANDIDATES` 直接冲突 → contradicted。**severity: Medium**。测试夹具是否算违规规格未定义（Open question）。

---

## REQ-29 — 质量门：生产代码无 console.log

> "No console.log in production Verified" — spec Quality Gates

**Verdict:** implemented · confidence: high

**What this demands:** src 内无 console.log。

**Where enforcement lives:** Searched `console\.log` in src → **0 命中**。`ErrorBoundary/index.tsx:24` 使用 `console.error`（错误边界日志，不属 console.log）。

**Paths walked:** ✓ 全量文本搜索覆盖。

**How the verdict was reached:** 零命中即满足；console.error 为有意保留的错误通道。

---

## REQ-30 — 质量门：页面代码无 `any`

> "No `any` types in pages Verified" — spec Quality Gates

**Verdict:** implemented · confidence: high

**What this demands:** `src/pages` 内无 any 类型。

**Where enforcement lives:** Searched `: any|as any|<any>` in `src/pages` → **0 命中**；`tsc --noEmit` 对 pages 目录零报错（严格模式 + strict:true，tsconfig.json:15）。

**Paths walked:** ✓ 全量模式搜索 + 编译器双重验证。

**How the verdict was reached:** 搜索与编译器证据一致。

---

## notChecked

| 项 | 原因 |
|---|---|
| 质量门历史数字（91/91 测试数、31.64s 构建时长、"19 files, +572 / -468" 范围） | 历史快照数字，非持续性规范要求；现状数字已变化（146 测试），其合规性由 REQ-26/27 的实质裁决覆盖 |
| ESLint 配置 | 规格 Out of Scope 明示（"pre-existing project config issue"） |
| Login 页装饰性遥测计数器（`agents: 247`） | 规格 Out of Scope 明示；现状仍存在（`Login/index.tsx:139`） |
| AgentDetail ChartsTab 功能 | 规格 Out of Scope 明示（"feature gap placeholder"）；占位在 `AgentDetail/index.tsx:221-227` |

## 反向差距（代码存在、规格未提及）

1. **ImmersiveLayout 浮动头硬编码遥测**："12 Agents / 3 Executing"（`ImmersiveLayout.tsx:73-77`）——与规格 Out-of-Scope 的 Login 计数器同类，但未被规格提及。
2. **Cockpit 硬编码装饰数据**：'+3 this week'（:48）、change 12/-5（:54,:60）、'2 urgent'（:66）、'Last sync: 2s'（:81）。
3. **AgentCanvas 演示数据与空保存**：`INITIAL_NODES`/`INITIAL_CONNECTIONS`（:42-60）、`EXECUTION_LOGS`（:62-68）；Save 按钮仅 toast 不落库（:352-354）。
4. **AgentDetail 占位数值**：METRICS/RESOURCES/LOGS 硬编码（:13-70）、version '2.3.1'（:108）、LLM 回退 'GPT-4'（:118）。
5. **QualityCenter 桩页**：无路由注册、仅含 Empty 组件（`Quality/QualityCenter/index.tsx:4-15`），description 复用 `fetchError` 文案键，语义错位。
6. **规格路由表之外的新域**：/tasks/*、/platform/system、/quality/gates|issues|security、/agents/executor（router:97-142）。
7. **request.ts 硬编码中文错误串**：'请求失败'（:46）、'请求错误: '（:84）、'网络错误，请检查网络连接'（:87），未走 i18n；:36-42 存在无操作的 XSS 响应头注释块。
8. **路由守卫跳过令牌过期校验**：`router/index.tsx:33` 读裸 localStorage 键，而 `utils/token.ts:19-22` 的 getToken 会按过期时间清除——过期令牌可过路由守卫（随后由 401 流程兜底）。
9. **Hive `PillNav` 组件未被复用**：组件存在（`Hive/index.ts:5`），但 AgentDetail（:127-139）与 WorkflowMonitor（:161-171）各自手写 pill 按钮。
10. **ErrorBoundary 文案硬编码英文**，未国际化。
11. **残留 dashboard i18n 命名空间**（`en.json:100`、`zh.json:100`），无组件消费。

## Open questions

1. `MOCK_*` 门禁是否豁免测试夹具？当前 3 个测试文件含 MOCK_ 常量（REQ-28）。
2. WorkflowMonitor 是否应恢复独立路由，还是永久作为 WorkflowCenter 实例标签的内嵌视图？
3. 规格文档是否应修订：删除 §6（Dashboard 已不存在）、更新 §1 路由表（领域重组）、将 `getAgentStats` stub 现状如实记录（REQ-07/08/20）？

---

## 反驳复核

独立复核员对上述裁决中 severity ∈ {Critical, High} 且 verdict ∈ {contradicted, partial, absent} 的 3 条分歧逐条尝试推翻（代码实测 + 规格口径重读）。复核日期：2026-08-29。

| REQ | 原判 | 裁定 | 理由(一行) | 证据 |
|---|---|---|---|---|
| REQ-07 | partial · High | 维持 | `getAgentStats()` 仍是永不发请求的 `Promise.resolve` 零值桩，规格 Overview「replace all mock/placeholder data with real API calls」与 §2「Maintain API integration」口径要求真实集成，结构在而实质缺 | `src/api/agent-engine.ts:14-22` 仍带 `/** Stub — no backend endpoint exists yet. */` 注释且实现为 `Promise.resolve({totalAgents:0,...})`；全 src 唯一调用点 `Cockpit/index.tsx:3,34`；无任何替代真实实现 |
| REQ-08 | partial · High | 维持 | 桩永不 reject，catch→disconnected→离线通知链路在当前构建中为不可达代码，「API 不可达时显示通知」无可达触发路径 | `Cockpit/index.tsx:37-38`（catch）与 `:85-89`（通知渲染）完整存在，但上游 `agent-engine.ts:16` 恒为 fulfilled Promise；失败路径无其他入口 |
| REQ-26 | contradicted · High | 维持 | 复核员实测 `npx tsc --noEmit` 复现与原分析逐字相同的 2 处 TS6133 错误（exit code 2），build 脚本 `tsc && vite build` 必然在 tsc 阶段失败，无旁路配置 | 实测输出：`Composer.test.tsx(5,1) TS6133 'ComposerValue'`、`useFileUpload.test.ts(2,27) TS6133 'waitFor'`；grep 确认两导入在各自文件中仅出现于 import 行（确属未用）；`package.json:8` build=`tsc && vite build`；`tsconfig.json:16` noUnusedLocals + `:24` include:["src"] 覆盖测试，`tsconfig.node.json` 仅含 vite.config.ts（无豁免）；`.github/workflows/ci.yml:197-200` frontend job 直接运行 `npx tsc --noEmit` 且无失败保护 |

**复核备注（不改裁定）**：原分析称「CI 前端 job 的 `npm run build` 将失败」，实际当前 `ci.yml` frontend job 未运行 `npm run build`，而是以独立的 `npx tsc --noEmit` 步骤（无 `\|\| true` 保护）直接暴露同一根因——CI 失败结论不受影响，失败路径比原描述更直接，REQ-26 的 contradicted 裁定只会被强化。另注意到裁决总览表「contradicted \| 4」与正文 3 条（REQ-26/27/28）计数不符，属原文笔误，不影响本次复核范围内的条目。

**复核结论：3/3 维持，0 推翻。**
