# 深渊蜂巢 UI/UE 设计规范

> SchemaPlexAI 全局界面设计系统 + 四个标志性页面深度设计
> 风格：TeamCity 布局 × 驾驶舱沉浸感 × 蜂巢生物隐喻 × 黑镜未来科技

---

## 目录

1. [概述](#1-概述)
2. [设计原则](#2-设计原则)
3. [设计系统](#3-设计系统)
4. [布局框架](#4-布局框架)
5. [组件规范](#5-组件规范)
6. [标志性页面设计](#6-标志性页面设计)
7. [Ant Design 主题配置](#7-ant-design-主题配置)
8. [文生图提示词汇总](#8-文生图提示词汇总)
9. [交付物清单](#9-交付物清单)

---

## 1. 概述

### 1.1 背景

SchemaPlexAI 是一个多 Agent 协作平台，当前前端基于 React 18 + Ant Design 5 构建，采用经典侧边栏+内容区布局。随着平台功能扩展，需要一套统一的、具有品牌辨识度的设计系统，同时满足以下核心页面的高体验要求：

- **驾驶舱大屏**：全局 Agent 集群监控，需要信息密度高、视觉冲击力强
- **Agent 编排画布**：可视化 DAG 工作流编排，需要空间感强、交互直观
- **工作流执行监控**：甘特图+数据表格，需要信息结构清晰、可读性强
- **Agent 详情页**：实时监控+日志终端，需要功能明确、专业感强

### 1.2 设计目标

| 目标 | 说明 |
|------|------|
| 品牌辨识度 | 通过"蜂巢/蚁穴"生物隐喻建立独特的视觉语言，区别于传统中后台 |
| 信息效率 | 管理页面（监控、详情）保持高信息密度，不因风格牺牲效率 |
| 沉浸体验 | 核心页面（驾驶舱、编排画布）提供全屏沉浸感，最大化内容展示 |
| 技术可行 | 基于 Ant Design 5 覆写 token，不从零构建组件库，控制开发成本 |

### 1.3 参考来源

- **TeamCity**：三栏布局结构（项目树 + 画布 + 属性面板）、DAG 可视化模式
- **黑镜（Black Mirror）**：冷峻极简、无菌感界面、临床未来主义美学
- **参考图**：用户基于 Midjourney 提示词生成的 4 张高保真概念稿（`images/` 目录）

---

## 2. 设计原则

### 2.1 核心原则

**深渊背景（Abyss Background）**
页面以极深的蓝黑色为底，内容元素如荧光生物般浮现其上。无实体阴影，仅靠背景色差异和荧光晕光建立层次。

**蜂巢隐喻（Hive Metaphor）**
六边形作为核心视觉符号贯穿全系统：图标、网格纹理、节点形状、按钮样式。Agent 间的通信以"信息素轨迹"（虚线+流动光效）可视化。

**功能与美学自适应（Adaptive Aesthetics）**
同一套设计语言，两种空间策略：展示型页面采用沉浸式（无边框、隐藏导航），管理型页面采用渐进式（结构清晰、导航常驻）。

**荧光色编码（Bioluminescent Color Coding）**
所有状态、类型、优先级均通过荧光色区分：青=正常/通信，琥珀=执行/能量，红=异常/危险。

### 2.2 设计禁忌

- 不使用弥散阴影（`box-shadow` 仅用于荧光晕光）
- 不使用渐变背景（保持深渊的纯粹性）
- 不使用圆角过大的组件（最大 12px，保持冷峻感）
- 不使用纯黑色（`#000000`），保持蓝黑调的层次感

---

## 3. 设计系统

### 3.1 色彩系统

#### 背景层级

| 名称 | 色值 | 用途 |
|------|------|------|
| 深渊背景 | `#0a0e1a` | 页面底层背景 |
| 巢穴壁 | `#0d1117` | 侧边栏背景 |
| 蜂房卡片 | `#111827` | 卡片、面板、表格容器 |
| 菌丝分隔 | `#1e2a33` | 边框线、分隔线、表头底 |
| 巢穴 hover | `#1e3a5f` | hover 态背景、次要强调 |

#### 荧光色板

| 名称 | 色值 | 用途 | 透明度变体 |
|------|------|------|-----------|
| 信息素青 | `#00d4aa` | 正常、成功、通信、主品牌色 | `20`/`30`/`40`/`50`/`60` |
| 琥珀能量 | `#ff9f43` | 执行中、警告、能量、等待 | `20`/`30`/`40`/`50`/`60` |
| 危险红 | `#ff4757` | 异常、错误、危险、告警 | `20`/`30`/`40`/`50`/`60` |
| 信息灰 | `#64748b` | 次要文字、占位符、禁用态 | — |
| 主文字 | `#e2e8f0` | 标题、正文、主要数据 | — |

#### 状态色映射

| 状态 | 色值 | 图标 | 背景变体 |
|------|------|------|---------|
| 空闲/待命 | `#00d4aa` | ● 微弱呼吸灯 | `#00d4aa20` |
| 执行中 | `#ff9f43` | ● 金色脉冲 | `#ff9f4320` |
| 异常/失败 | `#ff4757` | ● 红色告警 | `#ff475720` |
| 等待/排队 | `#ff9f43` | ● 琥珀闪烁 | `#ff9f4320` |
| 已完成 | `#00d4aa` | ● 稳定青绿 | `#00d4aa20` |

### 3.2 字体系统

全部使用开源字体，SIL Open Font License，Google Fonts 免费提供。

| 层级 | 字体 | 字重 | 字号 | 行高 | 用途 |
|------|------|------|------|------|------|
| 标题 | Inter / Noto Sans SC | 600 | 18-32px | 1.2 | 页面标题、卡片标题 |
| 正文 | Inter / Noto Sans SC | 400 | 14-15px | 1.5 | 段落、描述、标签 |
| 数据 | JetBrains Mono | 500 | 12-28px | 1.4 | 数字、代码、时间戳、状态标识 |
| 标签 | Inter / Noto Sans SC | 500 | 10-12px | 1.2 | 全大写 + letter-spacing: 0.05em |

CSS 变量：

```css
:root {
  --font-sans: 'Inter', 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  --font-mono: 'JetBrains Mono', 'Fira Code', 'SF Mono', monospace;
}
```

Google Fonts 加载：

```html
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=JetBrains+Mono:wght@400;500&family=Noto+Sans+SC:wght@400;500;600&display=swap" rel="stylesheet">
```

### 3.3 间距系统

以 8px 为基准单位，采用 8 点网格：

| Token | 值 | 用途 |
|-------|-----|------|
| space-1 | 4px | 极细间距、图标与文字间隙 |
| space-2 | 8px | 紧凑间距、行内元素 |
| space-3 | 12px | 小卡片内边距、按钮组间隙 |
| space-4 | 16px | 标准内边距、表单字段间距 |
| space-5 | 20px | 中等面板内边距 |
| space-6 | 24px | 大卡片内边距、页面边距 |
| space-8 | 32px | 区块间距 |
| space-10 | 40px | 大区块间距 |
| space-12 | 48px | 页面级间距 |

### 3.4 圆角与阴影

| 层级 | 圆角 | 用途 |
|------|------|------|
| radius-sm | 4px | 数据单元、标签、进度条、小按钮 |
| radius-md | 8px | 卡片、面板、输入框、图标按钮 |
| radius-lg | 12px | 模态框、大面板、抽屉 |
| radius-full | 9999px | 头像、状态点、药丸导航 |

阴影策略：不使用实体弥散阴影。仅用荧光晕光：

```css
box-shadow: 0 0 12px {color}30;  /* 12px 模糊，30% 透明度 */
box-shadow: 0 0 0 1px {color}20, 0 0 12px {color}15;  /* focus 双层光晕 */
```

---

## 4. 布局框架

### 4.1 模式一：沉浸式（Immersive）

适用页面：驾驶舱大屏、Agent 编排画布

**结构：**

```
┌─────────────────────────────────────────────────────────────┐
│  ┌──┐                                                        │
│  │◉│  ← 左侧图标栏 (52px, 图标 only, hover 展开文字)          │
│  │◆│                                                        │
│  │▲│     ┌──────────────────────────────────────────┐       │
│  │●│     │  中央画布 (无边框全屏, subtle grid)       │       │
│  └──┘     │                                          │       │
│           │    ┌──────────────────────────────┐      │       │
│           │    │  悬浮状态条 (auto-fade)        │      │       │
│           │    └──────────────────────────────┘      │       │
│           │                                          │       │
│           │  ← 右侧面板 (320px, 默认隐藏, 选中滑出)   │       │
│           └──────────────────────────────────────────┘       │
└─────────────────────────────────────────────────────────────┘
```

**关键特征：**

- 左侧边栏：宽度 52px，仅显示图标。hover 时 tooltip 显示文字。选中态图标容器背景 `#00d4aa20` + 荧光边框
- 顶部状态条：悬浮于画布上方，auto-fade（鼠标远离时透明度降至 0.3）。内容：系统名 + Agent 统计 + 时间
- 中央画布：无边框，内容即背景。微妙的六边形网格纹理（80px 间距，2% 透明度）作为空间参考
- 右侧面板：宽度 320px，overlay 滑出（不挤压画布）。含头部/标签页/属性/操作区
- 底部工具栏（编排画布）：悬浮工具按钮组（添加/连线/删除/运行）

### 4.2 模式二：渐进式（Progressive）

适用页面：工作流执行监控、Agent 详情页

**结构：**

```
┌─────────────────────────────────────────────────────────────┐
│  SchemaPlexAI                    [租户 ▼] [🔔] [👤]        │  ← 顶部栏 (48px)
├────────┬────────────────────────────────────────────────────┤
│        │  页面标题                              [操作按钮]  │
│  Logo  ├────────────────────────────────────────────────────┤
│        │                                                    │
│  导航项 │  ┌────────────────────────────────────────────┐  │
│  ───── │  │  卡片容器 (表格/表单/时间线)                  │  │
│  导航项 │  │                                            │  │
│  ───── │  └────────────────────────────────────────────┘  │
│  导航项 │                                                    │
│        │                                                    │
│  用户区 │                                                    │
└────────┴────────────────────────────────────────────────────┘
```

**关键特征：**

- 左侧边栏：宽度 200px，图标+文字同时可见。分组标题全大写。选中态：左侧 3px 荧光竖线 + 背景 `#111827`
- 顶部栏：固定高度 48px，系统 Logo（左）+ 全局操作（右：租户切换、通知、用户）
- 内容区：结构化卡片容器包裹表格/表单/时间线。页面标题 + 操作按钮位于卡片上方
- 表格保留清晰行列分隔，hover 行背景变为 `#111827`

### 4.3 切换规则

| 页面 | 布局模式 |
|------|---------|
| 驾驶舱大屏 | 沉浸式 |
| Agent 编排画布 | 沉浸式 |
| 工作流执行监控 | 渐进式 |
| Agent 详情页 | 渐进式 |

切换时保持同一套色彩/字体/组件规范，仅布局框架和控件显隐策略变化。

---

## 5. 组件规范

基于 Ant Design 5，通过 ConfigProvider theme token + CSS 变量覆写视觉层。组件 DOM 结构和 API 保持不变。

### 5.1 卡片（Card）

覆写目标：统计指标卡片（参考图提取）

| 属性 | 值 |
|------|-----|
| background | `#111827` |
| border | `none` |
| border-radius | `8px` |
| padding | `16px` |
| 左侧色带 | 3px 宽，颜色对应状态（青/琥珀/红） |
| 底部微图 | 7-8 根细柱状图，高度不等，颜色渐变透明度 |

统计卡片结构：

```
┌──────────────────────────────┐
│ ▏Active Agents               │  ← 标签（全大写，#64748b）
│ ▏7,843                       │  ← 大数字（JetBrains Mono，24px+）
│ ▏↑ 12.5%                     │  ← 变化率（颜色编码）
│ ▏▁▃▅▇▆█▃                     │  ← 微柱状图（7-8 根）
└──────────────────────────────┘
```

### 5.2 按钮（Button）

| 类型 | 背景 | 边框 | 文字色 | 阴影 |
|------|------|------|--------|------|
| Primary | `#00d4aa` | `none` | `#0a0e1a` | `0 0 12px #00d4aa30` |
| Default | `#111827` | `1px solid #1e2a33` | `#e2e8f0` | `none` |
| Ghost | `transparent` | `1px solid #00d4aa` | `#00d4aa` | `0 0 8px #00d4aa15` |
| Danger | `#ff4757` | `none` | `#0a0e1a` | `0 0 12px #ff475730` |
| Link | `transparent` | `none` | `#00d4aa` | `none` |

圆角：操作按钮 6px，图标按钮 8px。

**六边形图标按钮**（Agent 类型选择器，参考图提取）：

- 尺寸：36×36px
- 默认态：背景 `#111827`，边框 `1px solid #1e2a33`
- 激活态：背景 `#00d4aa20`，边框 `1px solid #00d4aa`，阴影 `0 0 8px #00d4aa30`
- 图标：六边形 `⬡`（Unicode 2B21）

### 5.3 表格（Table）

| 元素 | 样式 |
|------|------|
| 表头背景 | `#0d1117` |
| 表头文字 | `#64748b`，全大写，letter-spacing: 0.05em，11px |
| 表头分隔 | `1px solid #1e2a33` |
| 行背景 | 默认 `transparent`，hover `#111827` |
| 行分隔 | `1px solid #1e2a3380`（半透明） |
| 行文字 | `#e2e8f0`，12px |
| 状态列 | 荧光圆点 `●` + 文字 |
| Tag | 透明底 + 荧光文字 + 圆角 4px + padding 2px 8px |

### 5.4 输入框（Input / TextArea / Select）

参照 x.com 极简输入美学，融合暗色主题。

| 属性 | 值 |
|------|-----|
| 背景 | `transparent`（与页面融合） |
| 边框 | `1px solid #1e2a33` |
| 圆角 | `8px` |
| 字体 | `var(--font-sans)`，15px，字重 400 |
| placeholder 色 | `#475569` |
| focus 边框 | `1px solid #00d4aa` |
| focus 阴影 | `0 0 0 1px #00d4aa20, 0 0 12px #00d4aa15` |
| error 边框 | `1px solid #ff4757` |
| error 阴影 | `0 0 0 1px #ff475720` |

**浮动标签（Floating Label）：**

- 未 focus/无值时：标签文字位于输入框内，颜色 `#475569`，15px
- focus/有值时：标签上浮至输入框顶部上方，颜色 `#00d4aa`，11px，letter-spacing: 0.05em
- 过渡动画：0.2s ease

### 5.5 药丸导航（Pill Navigation）

视图切换器，参考图提取。

```
┌─────────────────────────────────────┐
│  [ 拓扑视图 ]  列表视图   代码视图   │
└─────────────────────────────────────┘
       ↑
    激活项：背景 #00d4aa，文字 #0a0e1a，圆角 16px
    非激活：透明底，文字 #64748b
    容器：背景 #0d1117，边框 #1e2a33，圆角 20px，padding 3px
```

### 5.6 终端日志（Terminal Log）

基于 xterm.js 风格的日志流组件。

| 元素 | 样式 |
|------|------|
| 容器背景 | `#0a0e1a` |
| 容器边框 | `1px solid #1e2a33` |
| 字体 | JetBrains Mono，11px，行高 1.8 |
| 时间戳 | `#64748b`，格式 `[HH:MM:SS]` |
| INFO | `#00d4aa`，前缀 `[INFO]` |
| WARN | `#ff9f43`，前缀 `[WARN]` |
| ERROR | `#ff4757`，前缀 `[ERROR]` |
| DEBUG | `#64748b`，前缀 `[DEBUG]` |
| 正文 | `#e2e8f0` |
| 光标 | 底部 `#00d4aa` 闪烁方块 `▌` |

---

## 6. 标志性页面设计

### 6.1 B. 全局驾驶舱大屏 · 沉浸式

**页面定位**：系统级监控视图，用户登录后的首屏或点击"驾驶舱"后的全屏页面。

**布局结构：**

```
┌─────────────────────────────────────────────────────────────┐
│  [◉]                                                       │
│  [◆]     ┌─ SchemaPlexAI · ●12 Agents · ●3 Executing ─┐   │
│  [▲]     └─────────────────────────────────────────────┘   │
│  [●]                                                        │
│           ┌─────────────────────────────────────┐           │
│           │                                     │           │
│           │     ◉  ORCHESTRATOR                 │           │
│           │        click to expand swarm view   │           │
│           │                                     │           │
│           │    ◆        ◆        ◆        ◆    │           │
│           │   Agent    Agent    Agent    Agent  │           │
│           │                                     │           │
│           └─────────────────────────────────────┘           │
│                                                             │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐               │
│  │THROUGH │ │LATENCY │ │SUCCESS │ │TOKEN   │               │
│  │2,847   │ │124ms   │ │99.7%   │ │4.2M    │               │
│  └────────┘ └────────┘ └────────┘ └────────┘               │
└─────────────────────────────────────────────────────────────┘
```

**核心元素：**

- **中央蜂巢热力图**：力导向布局，中心为 Orchestrator（大圆形，荧光青），周围 Agent 节点按环形轨道排列
- **节点状态**：青=正常、琥珀=执行中、红=异常。节点带呼吸灯动画
- **信息素轨迹**：节点间虚线连线，带方向箭头，暗示通信链路
- **底部指标带**：4 个统计卡片（吞吐量/延迟/成功率/Token 消耗），左侧色带标识
- **顶部悬浮条**：系统名 + Agent 统计 + 当前时间，auto-fade

**交互：**

- 点击中心节点：展开/收缩 swarm 视图（节点从中心散开/聚合）
- 点击普通节点：右侧面板滑出，显示 Agent 实时详情
- hover 节点：显示 tooltip（Agent 名称 + 状态 + 最近任务）
- 底部卡片 click：下钻到对应详细报表

**动画：**

- 节点呼吸灯：opacity 0.6 → 1.0 → 0.6，周期 3s
- 信息素轨迹：虚线 offset 动画，模拟流动
- 页面加载：节点从中心依次向外扩散出现（stagger 0.1s）

---

### 6.2 A. Agent 编排画布 · 沉浸式

**页面定位**：可视化 DAG 工作流编排，类似 TeamCity Pipeline Editor。

**布局结构：**

```
┌─────────────────────────────────────────────────────────────┐
│  [S]                                                       │
│  [◉]     ┌─ 拓扑 │ 列表 │ 代码 ─┐                          │
│  [◆]     └─────────────────────┘                          │
│  [▲]                                                        │
│  [●]      ┌─────────────┐     ┌─────────────┐             │
│           │ ⬡ DATA_ING  │────→│ ⬡ PIPELINE  │             │
│           │   Worker    │     │ Orchestrator│             │
│           └─────────────┘     └──────┬──────┘             │
│                                      │                     │
│           ┌─────────────┐     ┌──────┴──────┐             │
│           │ ⬡ PREPROC   │←───→│ ⬡ VALIDATOR │             │
│           │ Executor    │     │ Guard       │             │
│           └─────────────┘     └─────────────┘             │
│                                                             │
│         [+] [⟷] [⌫] [▶]  ← 底部工具栏                     │
└─────────────────────────────────────────────────────────────┘
```

**核心元素：**

- **左侧 Agent 面板**：可拖拽的 Agent 类型列表（Worker/Executor/Guard/Orchestrator），拖拽到画布创建节点
- **中央 DAG 画布**：
  - 节点：六边形图标 + 圆角矩形卡片，含名称/类型/状态 Tag/进度条
  - 连线：虚线 6px dash + 3px gap + 箭头，支持贝塞尔曲线
  - 画布背景：40px 直线网格（2% 透明度）
- **右侧面板**：选中节点后滑出，含 Properties/Logs/Metrics 标签页
- **底部工具栏**：添加节点/创建连线/删除/运行工作流
- **顶部药丸导航**：拓扑视图 / 列表视图 / 代码视图（YAML/Kotlin DSL）

**交互：**

- 拖拽：从左侧面板拖拽 Agent 类型到画布创建节点
- 连线：点击工具栏"连线"按钮，依次点击两个节点建立依赖
- 选中：单击节点高亮（边框荧光 + 光晕），右侧面板滑出
- 拖拽移动：选中节点可拖拽调整位置
- 运行：点击底部"▶"，节点依次高亮执行，连线流动动画

**节点卡片结构：**

```
┌─────────────────────────────┐
│ [⬡] DATA_INGESTION          │  ← 六边形图标 + 名称
│     Worker · v2.1.0         │  ← 类型 + 版本
│     ┌──────┐                │
│     │ACTIVE│  2.3s          │  ← 状态 Tag + 执行时间
│     └──────┘                │
│     ████████░░░░ 65%        │  ← 进度条（执行中节点显示）
└─────────────────────────────┘
```

---

### 6.3 D. 工作流执行监控 · 渐进式

**页面定位**：工作流历史与实时监控，甘特图+表格混合视图。

**布局结构：**

```
┌─────────────────────────────────────────────────────────────┐
│  SchemaPlexAI                                          [👤] │
├────────┬────────────────────────────────────────────────────┤
│        │  工作流执行监控          [全部] [运行中] [已完成]   │
│  Logo  ├────────────────────────────────────────────────────┤
│  ───── │  ┌────────────────────────────────────────────┐   │
│  驾驶舱 │  │  WORKFLOW │ 00:00 │ 00:30 │ 01:00 │ ...   │   │
│  编排   │  │  data-pipe│ [ingest    ][process][validate]│   │
│  监控◀──│  │  model-tra│ [    training...             ]│   │
│  详情   │  │  sync-job │      [   failed   ]            │   │
│        │  └────────────────────────────────────────────┘   │
│        │                                                    │
│        │  ┌────────────────────────────────────────────┐   │
│        │  │  详细列表（表格）                            │   │
│        │  └────────────────────────────────────────────┘   │
└────────┴────────────────────────────────────────────────────┘
```

**核心元素：**

- **甘特图时间线**：
  - 行：工作流名称（左固定列）
  - 列：时间刻度（顶部）
  - 任务段：圆角矩形色块（高度 20-24px，圆角 4px），段内显示任务名
  - 颜色：青=成功完成段、琥珀=执行中段、红=失败段、紫=等待段
- **药丸过滤**：全部 / 运行中 / 已完成 / 失败（顶部）
- **详细表格**：甘特图下方，展示工作流列表（名称/状态/执行时间/操作）

**交互：**

- 点击任务段：下钻到该任务的 Agent 详情页
- hover 任务段：tooltip 显示任务详情（开始时间/持续时间/Agent）
- 点击工作流行标题：展开/折叠该行，显示子任务
- 时间轴拖拽：横向滚动查看历史/未来
- 当前时间指示：荧光竖线，带脉冲动画

---

### 6.4 C. Agent 详情/终端页 · 渐进式

**页面定位**：单个 Agent 的实时监控、日志流、性能曲线、配置管理。

**布局结构：**

```
┌─────────────────────────────────────────────────────────────┐
│  SchemaPlexAI                                          [👤] │
├────────┬────────────────────────────────────────────────────┤
│        │  [⬡] data-processor-alpha      [重启] [终止]      │
│  Logo  │       Worker · ID: #7A3F-9021                     │
│  ───── ├────────────────────────────────────────────────────┤
│  驾驶舱 │  实时监控 │ 执行日志 │ 性能曲线 │ 配置              │
│  编排   ├────────────────────────────────────────────────────┤
│  监控   │  ┌──────────┐  ┌────────────────────────────────┐│
│  详情◀──│  │ CPU      │  │ [14:30:01] INFO Agent init...  ││
│        │  │ 67.3%    │  │ [14:30:02] INFO Connected...   ││
│        │  │ ████████ │  │ [14:30:15] WARN Queue depth... ││
│        │  ├──────────┤  │ [14:30:22] INFO Task received  ││
│        │  │ MEMORY   │  │ ...                            ││
│        │  │ 2.1 GB   │  │ ▌  ← 闪烁光标                   ││
│        │  │ ████░░░░ │  └────────────────────────────────┘│
│        │  ├──────────┤                                    │
│        │  │ UPTIME   │                                    │
│        │  │ 14:32:08 │                                    │
│        │  └──────────┘                                    │
└────────┴────────────────────────────────────────────────────┘
```

**核心元素：**

- **Agent 身份卡**：六边形头像 + 名称 + ID + 类型 + 操作按钮（重启/终止）
- **Tab 切换**：实时监控 / 执行日志 / 性能曲线 / 配置
- **实时监控页**：
  - 左侧：指标卡片（CPU/Memory/Uptime），含进度条
  - 右侧：终端式日志流（xterm.js 风格）
- **执行日志页**：全宽终端，支持搜索/过滤级别/导出
- **性能曲线页**：折线图（CPU/内存/延迟随时间变化）
- **配置页**：表单编辑 Agent 参数（浮动标签输入框）

**交互：**

- 日志自动滚动到底部，可暂停自动滚动
- 点击日志行：展开显示完整日志详情（JSON 结构）
- 性能曲线：hover 显示时间点数值，可缩放时间范围
- 配置保存：实时生效，无需刷新页面

---

## 7. Ant Design 主题配置

### 7.1 ConfigProvider Theme Tokens

```typescript
// theme.ts
import type { ThemeConfig } from 'antd';

export const abyssHiveTheme: ThemeConfig = {
  token: {
    // 色彩
    colorBgBase: '#0a0e1a',
    colorBgContainer: '#111827',
    colorBgElevated: '#111827',
    colorBgLayout: '#0a0e1a',
    colorBorder: '#1e2a33',
    colorBorderSecondary: '#1e2a3380',
    colorPrimary: '#00d4aa',
    colorError: '#ff4757',
    colorWarning: '#ff9f43',
    colorSuccess: '#00d4aa',
    colorText: '#e2e8f0',
    colorTextSecondary: '#64748b',
    colorTextTertiary: '#475569',
    colorTextPlaceholder: '#475569',
    colorLink: '#00d4aa',
    colorLinkHover: '#00f5c4',
    colorLinkActive: '#00b894',

    // 字体
    fontFamily: "'Inter', 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif",
    fontFamilyCode: "'JetBrains Mono', 'Fira Code', monospace",
    fontSize: 14,
    fontSizeSM: 12,
    fontSizeLG: 16,
    fontSizeXL: 20,

    // 圆角
    borderRadius: 8,
    borderRadiusSM: 4,
    borderRadiusLG: 12,

    // 间距
    paddingXS: 8,
    paddingSM: 12,
    padding: 16,
    paddingMD: 20,
    paddingLG: 24,
    paddingXL: 32,

    // 控制组件
    controlHeight: 40,
    controlHeightSM: 32,
    controlHeightLG: 48,
  },
  components: {
    Card: {
      colorBgContainer: '#111827',
      borderRadius: 8,
      headerBg: 'transparent',
      headerFontSize: 16,
      actionsBg: 'transparent',
    },
    Button: {
      borderRadius: 6,
      borderRadiusSM: 4,
      primaryShadow: '0 0 12px #00d4aa30',
      dangerShadow: '0 0 12px #ff475730',
    },
    Table: {
      headerBg: '#0d1117',
      headerColor: '#64748b',
      headerSplitColor: '#1e2a33',
      rowHoverBg: '#111827',
      borderColor: '#1e2a3380',
      cellPaddingBlock: 12,
      cellPaddingInline: 16,
    },
    Input: {
      colorBgContainer: 'transparent',
      colorBorder: '#1e2a33',
      activeBorderColor: '#00d4aa',
      activeShadow: '0 0 0 1px #00d4aa20, 0 0 12px #00d4aa15',
      borderRadius: 8,
      colorTextPlaceholder: '#475569',
      errorActiveShadow: '0 0 0 1px #ff475720',
      hoverBorderColor: '#1e3a5f',
    },
    Select: {
      colorBgContainer: 'transparent',
      colorBorder: '#1e2a33',
      borderRadius: 8,
      optionSelectedBg: '#00d4aa20',
      optionActiveBg: '#1e3a5f',
    },
    Modal: {
      colorBgElevated: '#111827',
      borderRadius: 12,
      headerBg: 'transparent',
      contentBg: '#111827',
    },
    Drawer: {
      colorBgElevated: '#111827',
      borderRadius: 0,
    },
    Tag: {
      borderRadius: 4,
      defaultBg: 'transparent',
    },
    Tabs: {
      colorBgContainer: 'transparent',
      inkBarColor: '#00d4aa',
      itemActiveColor: '#00d4aa',
      itemHoverColor: '#00f5c4',
      itemColor: '#64748b',
    },
    Tooltip: {
      colorBgDefault: '#111827',
      colorTextLightSolid: '#e2e8f0',
    },
    Menu: {
      colorBgContainer: '#0d1117',
      colorItemBgSelected: '#111827',
      colorItemText: '#64748b',
      colorItemTextSelected: '#e2e8f0',
      colorItemTextHover: '#e2e8f0',
      colorActiveBarWidth: 3,
      colorActiveBarBorderSize: 0,
    },
    Switch: {
      colorPrimary: '#00d4aa',
      colorPrimaryHover: '#00f5c4',
    },
    Slider: {
      trackBg: '#00d4aa',
      trackHoverBg: '#00f5c4',
      handleColor: '#00d4aa',
    },
    Progress: {
      defaultColor: '#00d4aa',
      remainingColor: '#1e2a33',
    },
  },
};
```

### 7.2 全局 CSS 变量

```css
/* global.css */
:root {
  /* 背景 */
  --abyss-bg: #0a0e1a;
  --abyss-sidebar: #0d1117;
  --abyss-card: #111827;
  --abyss-border: #1e2a33;
  --abyss-hover: #1e3a5f;

  /* 荧光色 */
  --hive-cyan: #00d4aa;
  --hive-amber: #ff9f43;
  --hive-red: #ff4757;

  /* 文字 */
  --text-primary: #e2e8f0;
  --text-secondary: #64748b;
  --text-tertiary: #475569;

  /* 字体 */
  --font-sans: 'Inter', 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  --font-mono: 'JetBrains Mono', 'Fira Code', 'SF Mono', monospace;

  /* 间距 */
  --space-1: 4px;
  --space-2: 8px;
  --space-3: 12px;
  --space-4: 16px;
  --space-5: 20px;
  --space-6: 24px;
  --space-8: 32px;

  /* 圆角 */
  --radius-sm: 4px;
  --radius-md: 8px;
  --radius-lg: 12px;
  --radius-full: 9999px;

  /* 阴影 */
  --glow-cyan: 0 0 12px #00d4aa30;
  --glow-amber: 0 0 12px #ff9f4330;
  --glow-red: 0 0 12px #ff475730;
  --focus-cyan: 0 0 0 1px #00d4aa20, 0 0 12px #00d4aa15;
}

html, body, #root {
  background: var(--abyss-bg);
  color: var(--text-primary);
  font-family: var(--font-sans);
  height: 100%;
}

/* 隐藏滚动条但保留功能 */
::-webkit-scrollbar {
  width: 4px;
  height: 4px;
}
::-webkit-scrollbar-track {
  background: transparent;
}
::-webkit-scrollbar-thumb {
  background: var(--abyss-border);
  border-radius: var(--radius-full);
}
::-webkit-scrollbar-thumb:hover {
  background: var(--hive-cyan);
}

/* 选择文字颜色 */
::selection {
  background: #00d4aa40;
  color: var(--text-primary);
}
```

---

## 8. 文生图提示词汇总

### 8.1 驾驶舱大屏

**v1 - 概念方向：**

```
Dark futuristic command center dashboard, hexagonal honeycomb grid background faintly visible, central glowing orb node labeled 'ORCHESTRATOR' surrounded by smaller orbiting agent nodes in teal and amber, thin pheromone trail lines connecting nodes, bottom row of minimal dark data cards showing metrics, floating translucent header bar with system status, deep void black background #0a0e1a, bioluminescent accents, clean HUD interface, no borders, sterile clinical aesthetic inspired by Black Mirror, 8k, octane render, UI/UX design mockup --ar 16:9 --style raw
```

**v2 - 蜂巢热力图强调：**

```
Swarm intelligence visualization dashboard, hexagonal honeycomb heatmap showing agent cluster density, central large hexagon hub with radiating connections to smaller hexagon nodes, bioluminescent teal and amber glow on deep navy black background, force-directed graph layout, data metrics panels at bottom, minimalist sci-fi UI, Black Mirror inspired sterile interface, no chrome, glowing particle effects, 8k, UI design mockup --ar 16:9 --style raw
```

**v3 - 黑镜冷峻感：**

```
Black Mirror style system monitoring dashboard, dark clinical interface, circular orbital layout with central AI core and satellite agent nodes, thin glowing connection lines, bottom status bar with monospace data readouts, deep black background with subtle blue tint, no decorative elements, purely functional yet beautiful, sterile future tech aesthetic, bioluminescent cyan accents, 8k, UI/UX mockup --ar 16:9 --style raw
```

### 8.2 Agent 编排画布

**v1 - DAG 编辑器：**

```
Node-based visual workflow editor interface, dark void background #0a0e1a, rectangular agent nodes with hexagonal icons connected by dashed pheromone trail lines, top pill-shaped view switcher, bottom floating toolbar with minimal icons, translucent property panel sliding from right, subtle 40px grid lines, bioluminescent teal and amber accents, clean futuristic UI inspired by Black Mirror and TeamCity pipeline editor, 8k, UI/UX design mockup --ar 16:9 --style raw
```

**v2 - 蜂巢编排：**

```
Multi-agent orchestration canvas, dark futuristic interface, hexagon-shaped agent nodes arranged in DAG topology, glowing dashed connection lines with directional arrows, left draggable agent palette, right configuration panel, bottom execution toolbar, deep navy background, bioluminescent cyan and orange node borders, clinical minimal design, Black Mirror tech aesthetic, 8k, UI design mockup --ar 16:9 --style raw
```

### 8.3 工作流执行监控

**v1 - 甘特图：**

```
Dark admin dashboard with Gantt chart timeline, workflow rows with colored task segments in teal amber and red, left expanded navigation sidebar, top pill filter buttons, structured data table aesthetic, deep black background #0a0e1a, minimal borders, clean monospace data typography, clinical futuristic interface inspired by Black Mirror, 8k, UI/UX design mockup --ar 16:9 --style raw
```

**v2 - 时间线：**

```
Workflow execution monitoring dashboard, dark theme, horizontal timeline with colored bars showing task execution states, left navigation panel, top status filters, data table below timeline, deep navy background, bioluminescent color coding for task states, minimal clean interface, monospace font for timestamps, Black Mirror inspired sterile UI, 8k, UI design mockup --ar 16:9 --style raw
```

### 8.4 Agent 详情页

**v1 - 终端风格：**

```
Dark admin agent detail page with hexagonal avatar icon, left metrics cards showing CPU memory and uptime with progress bars, right terminal window with color-coded log stream in teal amber and red, tab navigation below header, structured sidebar navigation, deep black background #0a0e1a, clean monospace typography, clinical futuristic interface, 8k, UI/UX design mockup --ar 16:9 --style raw
```

**v2 - 监控面板：**

```
Agent monitoring dashboard, dark futuristic interface, left panel with real-time metrics gauges and sparkline charts, right terminal console with streaming logs, hexagonal agent icon at top, tabbed navigation for logs/metrics/config, deep navy background, bioluminescent cyan accents, clean minimal design, Black Mirror tech aesthetic, 8k, UI design mockup --ar 16:9 --style raw
```

---

## 9. 交付物清单

### 9.1 已交付

| # | 交付物 | 位置 | 状态 |
|---|--------|------|------|
| 1 | 设计系统基础（色彩/字体/间距/圆角/阴影） | 本文档 §3 | ✅ |
| 2 | 布局框架（沉浸式 + 渐进式） | 本文档 §4 | ✅ |
| 3 | 组件规范（Card/Button/Table/Input/Pill/Terminal） | 本文档 §5 | ✅ |
| 4 | 四个标志性页面设计 | 本文档 §6 | ✅ |
| 5 | Ant Design 5 主题配置代码 | 本文档 §7 | ✅ |
| 6 | Midjourney 提示词汇总（4页×3版=12组） | 本文档 §8 | ✅ |
| 7 | 参考图分析（基于 AI 生成图提取） | `images/` + 浏览器历史 | ✅ |

### 9.2 待实施（前端开发阶段）

| # | 任务 | 优先级 | 说明 |
|---|------|--------|------|
| 1 | 全局样式初始化 | P0 | 加载字体、设置 CSS 变量、覆写 AntD token |
| 2 | Layout 组件（沉浸式） | P0 | 侧边栏/画布/面板三栏结构 |
| 3 | Layout 组件（渐进式） | P0 | 侧边栏/顶部栏/内容区结构 |
| 4 | 驾驶舱页面 | P1 | 蜂巢热力图（ECharts/D3）+ 指标卡片 |
| 5 | 编排画布页面 | P1 | DAG 编辑器（ReactFlow/X6）+ 节点组件 |
| 6 | 工作流监控页面 | P1 | 甘特图（自定义或 gantt-schedule-timeline-calendar）+ 表格 |
| 7 | Agent 详情页面 | P1 | 指标卡片 + xterm.js 终端 + 性能图表 |
| 8 | 动画系统 | P2 | 呼吸灯、轨迹流动、节点扩散 |

### 9.3 技术栈推荐

| 功能 | 推荐库 | 理由 |
|------|--------|------|
| DAG 画布 | `@antv/x6` 或 `reactflow` | 国产/社区活跃，支持自定义节点 |
| 甘特图 | 自定义 ECharts 或 `gantt-task-react` | 灵活定制暗色主题 |
| 终端日志 | `xterm.js` | 业界标准，支持 WebSocket 流 |
| 性能图表 | `echarts` | 与 Ant Design 生态兼容好 |
| 蜂巢热力图 | `echarts` + `d3-force` | 力导向布局 + 自定义渲染 |
| 动画 | `framer-motion` | React 友好，支持编排 |

---

## 附录

### A. 命名对照表

| 中文名 | 英文名 | 用途 |
|--------|--------|------|
| 深渊蜂巢 | Abyss Hive | 设计系统代号 |
| 信息素青 | Pheromone Cyan | 主品牌色 |
| 琥珀能量 | Amber Energy | 执行/警告色 |
| 菌丝分隔 | Mycelium Border | 边框/分隔线色 |
| 蜂房卡片 | Hive Cell | 卡片背景色 |
| 巢穴壁 | Nest Wall | 侧边栏背景色 |

### B. 文件索引

- 设计文档：`docs/superpowers/specs/2026-05-01-abyss-hive-ui-design.md`
- 参考图：`images/all.png`、`images/Cell Detail.png`、`images/Task Hive.png`、`images/Task Timeline.png`
- 浏览器原型历史：`D:\code_space\frige\.superpowers\brainstorm\58499-1777568941\content\`

---

> 文档版本：v1.0
> 创建日期：2026-05-01
> 设计系统：深渊蜂巢 Abyss Hive
> 适用范围：SchemaPlexAI 全平台前端界面
