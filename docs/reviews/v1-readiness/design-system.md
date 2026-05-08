---
title: DesignSystemLead 设计系统重构方案
agent: DesignSystemLead
date: 2026-05-08
domain: design-system
---

> 评审目的：把 SchemaPlexAI 的"设计系统"从 4-5/10 抬到 v1 上线门槛 7/10，并指明走向 10/10 的路径。
> 评审范围：`schemaplexai-ui/src/styles/*`、`src/theme/index.ts`、`src/components/*`、`src/pages/*`、AntD 5 ConfigProvider、Storybook/Figma/可访问性。

---

## 1. 0-10 评分表

| 子维度 | 当前分 | 10 分定义 | 证据 |
|---|---|---|---|
| Token 系统 | **3** | TS / JSON / CSS 三态导出，Style Dictionary 单源，命名空间分层（global / alias / component） | `variables.css` 仅 23 个原子变量；`theme/index.ts` 是 AntD 形态的硬编码副本，与 CSS 变量未同源；颜色字面量散落在 `theme/index.ts`（如 `#00f5c4`、`#1e3a5f`）未提升为 token |
| 组件复用 | **5** | Storybook ≥ 30，原子-分子-有机体-模板四层；每个组件有 stories + a11y + visual-regression | 已有 8 个业务组件目录（`Hive/`、`Composer/`、`ChatMemory/`、`SseViewer/`、`Layout/`、`LanguageSwitcher/`、`TenantSelector/`、`ErrorBoundary/`），但都是有机体级，缺原子层；无 Storybook 依赖 |
| 主题/暗模式 | **2** | 亮 / 暗 双主题 + 高对比 + 色弱预设，CSS `prefers-color-scheme` 自动切换 | `main.tsx` 仅注入 `abyssHiveTheme` 一套；CSS 变量未做 `[data-theme]` 切片 |
| 文档化 | **0** | Storybook + Figma library + Spec 三件套；每个原子有 do/don't、props 表、a11y 备注 | 零文档；无 Storybook、无 Figma、无 `docs/design/` 目录 |
| 可访问性 | **2** | WCAG 2.2 AA：对比度≥4.5、焦点可见、键盘可达、ARIA 完整、动效可关 | 未审计；`#64748b on #0a0e1a` 对比度仅 ~4.2（接近边界）；`focus-cyan` token 仅做 1px 边框，键盘焦点不显著 |
| 设计语言独特性 | **4** | 一句话能说出 brand metaphor，配色/排版/动效形成可识别系统 | 名字 "Abyss Hive" 已存在，但 metaphor 未落实——既无"深渊"暗示（缺纵深感、无环境光梯度），也无"蜂巢"暗示（除 `Hive/HexIcon` 外，六边形语言未形成系统） |
| **加权总分** | **4.5 / 10** | — | — |

---

## 2. 现有 Abyss Hive 解构

从 `variables.css` + `theme/index.ts` 提炼当前 token 集合：

**色板（背景层级）**
- `--abyss-bg #0a0e1a`：深渊底色（HSL 222° 36% 7%），偏蓝紫
- `--abyss-sidebar #0d1117`：侧栏底色，比 bg 略亮 1 阶
- `--abyss-card #111827`：卡片底色，第 3 级深度
- `--abyss-border #1e2a33`：分割线
- `--abyss-hover #1e3a5f`：悬浮态，唯一一个偏冷蓝的中间色

**色板（生物发光强调）**
- `--hive-cyan #00d4aa`：主色，青绿，模拟蜂蜡发光
- `--hive-amber #ff9f43`：警告，琥珀色（蜂蜜）
- `--hive-red #ff4757`：错误，珊瑚红

**文字**：3 级灰阶 `#e2e8f0 / #64748b / #475569`

**排版**：Inter（西文）+ Noto Sans SC（中文）；JetBrains Mono（代码/数据）

**间距**：4-base scale（4/8/12/16/20/24/32/40/48），缺 64/80/96 以上的大间距

**圆角**：sm 4 / md 8 / lg 12 / full —— 缺 xl(16)、2xl(24)

**Glow（已经是该系统的差异化资产）**：cyan/amber/red 三色 12px 软发光，`focus-cyan` 是焦点环

**致命缺口**：
1. 无亮色 token；无 `[data-theme]` 切片
2. 无字号 / 行高 / 字重 token（仅 AntD 默认）
3. 无动效 token（duration、easing）
4. 无层级 z-index token
5. 无组件级 alias token（如 `--ds-button-primary-bg`）

---

## 3. 市场调研：5 个标杆产品的设计语言

| 产品 | 设计语言关键词 | Brand Metaphor | 我们能学什么 |
|---|---|---|---|
| **Linear** | 极简、键盘优先、低饱和、单色调 | "issue tracker as a magazine"——内容即排版 | 极简灰阶 + 单一强调色的纪律性；命令面板（⌘K）作为系统主轴；微动效（150ms ease-out）的克制 |
| **Vercel** | 黑白极简、字体即品牌、Geist mono | "deploy as a moment"——一次发布是一次仪式 | 把开发者工具当成排版作品；用 mono font 做品牌锚点；构建出"开发即美学"的高级感 |
| **Cursor** | IDE 暗色、零装饰、文本密度极高 | "AI as a pair programmer"——AI 隐入编辑器 | AI 不抢戏：流式 token 的低调呈现；diff/patch 的内联视觉；上下文 chip 的自然嵌入 |
| **v0 / Lovable** | 紫蓝渐变、活泼、prompt-first | "prompt as a sketch"——一句话即原型 | 输入框作为页面主角；生成中状态的存在感（骨架屏 + glow）；component picker 的轻量化 |
| **Devin** | 终端审美、绿黑、log-centric | "engineer in a box"——AI 即工程师 | 终端美学的工业感；agent 计划/思考的可见性；任务进度作为一等公民 |

**对 SchemaPlexAI 的启示**：
- 我们是 Linear（issue/spec 管理）+ Cursor（AI 协作）+ Devin（agent 执行）三者交集的企业级产品。
- 当前 "Abyss Hive" 是装饰性命名，未抽象成系统语言。需要把"深渊（深度、纵深、企业级）"+"蜂巢（多 agent 协作、cell 单元）"两个比喻落到具体视觉规则。

---

## 4. SchemaPlexAI 独特定位提案（3 选 1）

### 提案 A：「Hive of Specs」——蜂巢即规约

- **视觉锚点**：六边形（hex）作为信息单元的几何语言。Spec 卡片、Agent tile、任务格子都用 hex 网格密铺；列表/看板皆是 hex grid 的不同投影。
- **配色逻辑**：保留青绿主色（蜂蜡发光）+ 琥珀（蜂蜜，活跃态）+ 深渊蓝黑（蜂巢内部）。每个 hex 边框有 1px cyan glow，hover 时整个 hex 浮起 4px 并加 amber 内辉光。
- **动效/微互动**：状态切换以"蜂群涌动"动效——多个 hex 以 60ms 错峰 fade-in；agent 工作时 hex 中心点有脉冲呼吸（2s ease-in-out）。
- **排版规则**：Inter 14/22 为正文；JetBrains Mono 13/20 为 spec 编号、agent ID、token 流。标题用 Inter 28/36 600，字距 -0.02em。
- **亮色转译**：底色 `#fafbfc`，hex 边框 `#0a8470`（深青绿），glow 改为 `0 0 8px #00d4aa20` 的弱化残影。蜂蜡白 `#fff8e1` 替代琥珀。

### 提案 B：「Abyss Console」——深渊控制台

- **视觉锚点**：纵深渐变 + 微光网格。所有页面有 `linear-gradient(180deg, #0a0e1a, #060912)` 的 200vh 长背景，营造"沉入深渊"感；网格背景以 32px 间距、`#1e2a3308` 透明度铺设。
- **配色逻辑**：cyan 是潜艇仪表盘的发光读数；amber 是警示灯；red 是紧急停机。文字层有"水压"感——越远越暗。
- **动效**：滚动时背景视差（0.3x 速度），页面进入有"下潜" 200ms ease-out 的 8px 上移。
- **排版**：Mono 字体使用更激进——所有数字（cost、tokens、duration）都强制 mono；标题混排 mono 数字 + sans 文字。
- **亮色转译**：放弃。Abyss Console 是暗色 only 的产品 metaphor；亮色违背"深渊"叙事。给用户的回答是：**锁暗色**。

### 提案 C：「Reactor Core」——研发核反应堆

- **视觉锚点**：以"圆形/同心环"作为核心几何。Agent 执行像反应堆运转：中心是任务 core，外环是工具调用，最外层是观察者。Cockpit 页是反应堆全景图。
- **配色逻辑**：cyan = 等离子发光，amber = 控制棒预警，red = 临界报警。hex 退化为辅助元素，主舞台是同心圆 + 辐射线。
- **动效**：执行中的 agent 有 conic-gradient 旋转（4s linear infinite），完成时 360° 闪光收束。
- **风险**：圆形语言与 AntD/X6 的方形组件文化冲突，开发成本高。

**推荐**：**提案 A「Hive of Specs」**。理由：(1) 与现有命名 Abyss Hive 一致，沉没成本最低；(2) hex 几何已在 `Hive/HexIcon` 落地，可放大；(3) 蜂巢-cell 的隐喻天然契合 spec / agent / task 三个核心实体；(4) 亮色模式可转译，不绑死暗色。

---

## 5. v1 设计系统重构方案（DSv1）

### 5.1 Token 三态化

引入 **Style Dictionary** 作为单源，输出 3 态：

```
ds-tokens/
  src/
    global/        # 原子 token：颜色面板、字号、间距、圆角
      color.json
      typography.json
      space.json
      radius.json
      motion.json
      elevation.json
    alias/         # 语义 token：bg/fg/border/accent/danger/...
      light.json
      dark.json
    component/     # 组件 token：button/card/input/...
      button.json
      card.json
      input.json
  build/
    web/css/variables.css       # CSS 变量（覆盖现有 variables.css）
    web/ts/tokens.ts            # TS 类型 + 常量
    web/json/tokens.json        # Figma Tokens Studio 同步
    figma/abyss-hive.tokens.json
```

**命名空间约定**：
- `--ds-color-{role}-{state}`：`--ds-color-bg-default`、`--ds-color-accent-primary`、`--ds-color-fg-muted`
- `--ds-space-{n}`：1-12 共 12 档，新增 16/20/24 三档
- `--ds-radius-{size}`：sm/md/lg/xl/2xl/full
- `--ds-motion-duration-{xs|sm|md|lg}`：80/150/240/400ms
- `--ds-motion-easing-{enter|exit|emphasize}`：cubic-bezier 三套
- `--ds-elevation-{0..5}`：0=flat，1=card，2=dropdown，3=modal，4=drawer，5=overlay
- `--ds-z-{tier}`：base/dropdown/sticky/modal/popover/toast/tooltip

### 5.2 组件分层（原子-分子-有机体-模板-页面）

| 层 | 数量 | 例子 | 现状 |
|---|---|---|---|
| **原子** | 12 | Button、Input、Tag、Icon、Avatar、Badge、Spinner、Divider、Link、Text、HexCell、CodeInline | **几乎全空**——除 `HexIcon` 外都直接用 AntD 原始组件 |
| **分子** | 10 | FormField、SearchBox、CardHeader、StatusPill、Toast、CommandItem、KbdHint、CopyButton、TokenBadge、CostChip | 0 |
| **有机体** | 14 | Composer、SseViewer、ChatMemory、Hive/KanbanBoard、Hive/TerminalLog、Hive/StatCard、AgentTile、SpecCard、TaskTimeline、AgentCanvas（X6）、ToolCallTrace、CostBreakdown、QualityGateBar、TenantSelector | 8 已存在，需补 6 个 |
| **模板** | 6 | DashboardTemplate、DetailTemplate、CanvasTemplate、ListTemplate、AuthTemplate、EmptyTemplate | 0 |
| **页面** | 21 | Cockpit、AgentList、AgentDetail、AgentExecutor、AgentCanvas、WorkflowMonitor、Quality、Tasks、Platform、Projects、Login、NotFound 等 | 21 已落地 |

### 5.3 30 个 Storybook 入库组件清单

| # | 组件 | 层 | Stories 数 |
|---|---|---|---|
| 1 | Button | 原子 | 6（primary/secondary/ghost/danger/loading/icon） |
| 2 | Input | 原子 | 5（default/with-icon/error/disabled/textarea） |
| 3 | Tag | 原子 | 4 |
| 4 | Icon | 原子 | 1（all icons grid） |
| 5 | Avatar | 原子 | 3 |
| 6 | Badge | 原子 | 3 |
| 7 | Spinner | 原子 | 2 |
| 8 | Divider | 原子 | 2 |
| 9 | Link | 原子 | 3 |
| 10 | Text | 原子 | 4（heading/body/caption/code） |
| 11 | HexCell | 原子 | 5（idle/active/success/warning/error） |
| 12 | CodeInline | 原子 | 2 |
| 13 | FormField | 分子 | 4 |
| 14 | SearchBox | 分子 | 3 |
| 15 | StatusPill | 分子 | 6 |
| 16 | Toast | 分子 | 4 |
| 17 | CommandItem | 分子 | 3 |
| 18 | TokenBadge | 分子 | 3 |
| 19 | CostChip | 分子 | 3 |
| 20 | Composer | 有机体 | 4 |
| 21 | SseViewer | 有机体 | 3 |
| 22 | ChatMemory | 有机体 | 3 |
| 23 | KanbanBoard | 有机体 | 3 |
| 24 | TerminalLog | 有机体 | 3 |
| 25 | StatCard | 有机体 | 4 |
| 26 | AgentTile | 有机体 | 5 |
| 27 | SpecCard | 有机体 | 4 |
| 28 | ToolCallTrace | 有机体 | 4 |
| 29 | CostBreakdown | 有机体 | 3 |
| 30 | QualityGateBar | 有机体 | 4 |

合计 30 组件 / ~110 stories，达到 10 分基线。

### 5.4 亮色主题转译（Abyss Hive Light）

| 暗色 token | 暗值 | 亮值 | 备注 |
|---|---|---|---|
| `--ds-color-bg-default` | `#0a0e1a` | `#fafbfc` | 蜂蜡白 |
| `--ds-color-bg-elevated` | `#111827` | `#ffffff` | 卡片纯白 |
| `--ds-color-bg-sidebar` | `#0d1117` | `#f5f7fa` | 侧栏 |
| `--ds-color-border-default` | `#1e2a33` | `#e2e8ec` | 边框 |
| `--ds-color-fg-default` | `#e2e8f0` | `#0a0e1a` | 反相 |
| `--ds-color-fg-muted` | `#64748b` | `#5a6a78` | 微调以满足对比度 |
| `--ds-color-accent-primary` | `#00d4aa` | `#0a8470` | **暗化**避免荧光感 |
| `--ds-color-accent-warn` | `#ff9f43` | `#cc7a1f` | 暗化 |
| `--ds-color-accent-danger` | `#ff4757` | `#c8333f` | 暗化 |
| `--ds-glow-primary` | `0 0 12px #00d4aa30` | `0 0 8px #0a847014` | 弱化 60% |

切换机制：`document.documentElement.dataset.theme = 'light'|'dark'`；CSS 用 `:root[data-theme="light"]` 覆盖；遵循 `prefers-color-scheme` 默认；用户偏好持久化到 `localStorage` + Zustand `themeStore`。

---

## 6. 产品 Mockup 概念（文字描述）

### Mockup A — AgentExecutor 亮色 / Hive of Specs 视觉

页面整体在 `#fafbfc` 蜂蜡白底上展开。顶部 64px 高 sticky header，左侧一个 32px 的 hex logo（`HexCell` 原子，filled `#0a8470`），右侧是 Composer 输入区——一个圆角 12px、白底、1px `#e2e8ec` 描边、focus 时 `0 0 0 1px #0a847020 + 0 0 8px #0a847014` 的微辉光。中央主舞台是一个 hex grid，每个 hex 是一个 AgentTile，边长 96px，密铺成 3 列 5 行的蜂巢。当前激活的 agent 所在 hex 顶部出现 4px amber 顶冠（`#cc7a1f`），且 hex 内部 cyan 进度环以 conic-gradient 顺时针填充。下方 ToolCallTrace 是一个 Mono 字体的可折叠列表，每个工具调用以 `[12:34:01.234] tool.call(args)` 格式呈现，hover 时整行底色变 `#f5f7fa`。右侧 320px 抽屉是 ChatMemory，按时间倒序，每条 memory 卡片左侧有一个 4px 宽的 cyan/amber/red 状态条。整体节奏克制，仅在 agent 状态切换时由 hex 顶冠的颜色脉冲（amber→cyan，240ms ease-out）暗示进度。

### Mockup B — SpecCenter 卡片 + 蜂巢网格布局（暗色）

进入 `/spec` 后是全屏 `#0a0e1a` 深渊底，Cockpit-style header 显示当前租户（`TenantSelector`）+ 全局搜索（⌘K）。主区域是一个 hex grid 看板——每个 hex 边长 140px，包含一个 SpecCard：顶部 12px 高度区是 `[SPEC-2026-001]` mono 编号（`#00d4aa` cyan），中部 48px 是 spec 标题（Inter 16/22 600 `#e2e8f0`），底部 24px 是状态行（StatusPill + 负责人 Avatar + 评论数）。Hex 之间间距 8px，hover 时单个 hex 上浮 4px 并出现 `0 0 12px #00d4aa30` 的青绿辉光，整个网格其他 hex 同时降低 60% 不透明度形成"聚光灯"。点击 hex 时它向中心放大并旋转 30°，其余 hex 像蜂群一样向外 fade-out，进入 SpecDetail 模板。空状态呈现一个孤立的 dashed 描边 hex，中心 36px 的 `+` 图标，文字 "Add the first cell to your hive"。

### Mockup C — WorkflowMonitor 实时流图 + glow 微光

WorkflowMonitor 页面以 X6 画布为主体，铺满视口减去顶栏。画布底色是 `#0a0e1a` + 32px 间距的网格点（`#1e2a3340`）。每个工作流节点用一个圆角 12px 的矩形，但节点的状态指示器是一个 16px 的 hex marker（`HexCell`）贴在右上角。节点之间的连线在 idle 态是 1px `#1e2a33` 直线，运行态变成 2px cyan 并叠加一条 4s linear infinite 的 dash 流光（`stroke-dashoffset` 动画），完成态 fade 回 1px。当前正在执行的节点整个外框有 `0 0 16px #00d4aa40` 的呼吸辉光（2s ease-in-out infinite）；失败节点是 red glow + 抖动 1 次（80ms × 3）。右下角浮窗是 SseViewer，紧贴画布边缘，半透明 `#11182799` 背板 + backdrop-blur 8px，mono 字体的 token 流以每行 60ms 错峰滑入。顶部 toolbar 是一个 pill nav（`PillNav`），4 个 view（Graph / Timeline / Logs / Cost），active 项是一个 cyan 实心 pill，inactive 是透明 outline。

---

## 7. 关键发现（≥ 5 条带证据）

1. **Token 系统是单层的，未做 alias/component 分层**。`variables.css` 仅 23 个 raw token，且 `theme/index.ts` 中存在大量未提升为 token 的字面量（`#00f5c4`、`#0d1117`、`#1e3a5f`、`#1e2a3380`），导致两个文件易漂移。
2. **暗色硬绑**。`main.tsx` 单独注入 `abyssHiveTheme`；`global.css` 的 `html, body` 直接写死 `--abyss-bg`；切亮色需要两个文件改动 + 全部 25+ AntD component override 重写。
3. **无 Storybook 依赖**。`package.json` devDependencies 中没有 `@storybook/*`，意味着零组件文档体系，开发新人 onboarding 只能读源码。
4. **可访问性未审计**。`#64748b` (text-secondary) 在 `#0a0e1a` 上 WCAG AA 对比度计算约 4.2:1，**低于** AA 正文要求 4.5:1。`focus-cyan` 是 1px 边框 + 软辉光，键盘焦点视觉强度不足，色盲用户可能无法识别。
5. **业务组件已成型但缺原子层**。8 个组件目录全部是有机体级（Composer、SseViewer、Hive/* 等），按钮/输入/标签等原子直接调用 AntD 原始组件，导致设计语言无法在原子层统一施加（例如 hex 几何只在 `HexIcon` 一个地方，未扩散）。
6. **品牌 metaphor 名义存在但未落地**。"Abyss Hive" 之于产品几乎等于命名口号；"Hive" 仅在 `components/Hive/HexIcon` 一处见到六边形元素；"Abyss" 在视觉上无任何纵深/水压/微光环境暗示。
7. **动效/字体/层级 token 完全缺失**。无 duration/easing/z-index token，导致每个组件自己写 `transition: all .2s`、各种 z-index magic number，未来做 motion-reduce 适配几乎不可能。

---

## 8. 改造方案

| 优先级 | 项 | 周次 | 验收 KPI |
|---|---|---|---|
| **P0** | 引入 Style Dictionary，建立 3 层 token（global/alias/component），合并现有 `variables.css` + `theme/index.ts` 为单源 | W1 | TS/JSON/CSS 三态产物 + 0 字面量 in `theme/index.ts` |
| **P0** | 落地亮/暗主题切换，`[data-theme]` 切片 + Zustand `themeStore` + localStorage 持久化 | W1-W2 | 亮色 Cockpit + AgentExecutor 通过视觉验收 |
| **P0** | 锁定 brand metaphor "Hive of Specs"，输出 1 页设计宪章（`docs/design/brand.md`） | W1 | 评审会议通过，写入 wiki |
| **P1** | 接入 Storybook 8 + a11y addon + viewport addon，落地原子层 12 个组件 + 36 stories | W2-W3 | Storybook 启动，原子层 100% 覆盖 |
| **P1** | 补齐分子层 10 个 + 有机体层缺口 6 个，达到 30 组件目标 | W3-W4 | Storybook 30/30 通过 visual regression |
| **P1** | WCAG 2.2 AA 全量审计：调整 `--ds-color-fg-muted` 至 ≥4.5:1；强化焦点环（2px 实线 + glow） | W2 | axe-core 0 violation；键盘 tab 全程焦点可见 |
| **P2** | Figma Library 同步，Token Studio 插件接 `tokens.json` | W4-W5 | Figma 与代码 token 1:1，新组件先 Figma 后代码 |
| **P2** | 动效 token 系统化（duration/easing），`prefers-reduced-motion` 适配 | W3 | 全站动效可关，无硬编码 transition |
| **P2** | 21 页面按 6 个模板归一，发布 `templates/` 目录 | W5-W6 | 21 页面 100% 走模板 |
| **P3** | 高对比模式（Contrast+）+ 色弱预设（protanopia/deuteranopia） | W6+ | 三种主题切换 |

时间估算：1 名 senior 设计工程师 + 0.5 名设计师，6 周到 7-8/10。

---

## 9. 给用户的关键问题

> **Q1（必答）**：「Abyss Hive」在亮色模式下的核心 metaphor 是什么？
> - 选项 A：保留 "Hive of Specs" 隐喻——蜂巢/蜂蜡白底 + 暗化青绿，转译可行。
> - 选项 B：放弃亮色——把"Abyss"作为不可让渡的暗色叙事，产品锁暗色 only（参考 Linear 早期、Cursor、Devin）。
> - 选项 C：双品牌——暗色叫 "Abyss Hive"、亮色叫 "Daylight Hive"，分别强化深渊感 / 工作台感。
> **如不在 W1 拍板，直接锁暗色 only，把"亮色"从 v1 KPI 中移除。**
>
> **Q2**：Storybook 是否纳入 v1 must-have？若否，文档化分降至 3 分，整体设计系统封顶 6.5/10。
>
> **Q3**：Figma library 是否需要在 v1 内交付？还是 v1.1？这关系到是否引入专职设计师。
>
> **Q4**：WCAG 合规级别——AA 还是 AAA？AAA 在暗色 + glow 风格下需牺牲品牌发光强度，需取舍。
