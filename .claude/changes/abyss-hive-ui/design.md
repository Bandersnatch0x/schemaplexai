---
change_id: abyss-hive-ui
status: approved
created_at: 2026-05-01
---

# Design: Abyss Hive UI/UE

> 引用正式设计文档：`docs/superpowers/specs/2026-05-01-abyss-hive-ui-design.md`

## 设计原则

1. **深渊背景** — 极深蓝黑色底，内容如荧光生物浮现
2. **蜂巢隐喻** — 六边形贯穿全系统，Agent 通信以"信息素轨迹"可视化
3. **自适应美学** — 沉浸式（无边框）vs 渐进式（结构化导航）
4. **荧光色编码** — 青=正常，琥珀=执行，红=异常

## 色彩系统

| 名称 | Hex | 用途 |
|------|-----|------|
| 深渊背景 | `#0a0e1a` | 页面底层 |
| 巢穴壁 | `#0d1117` | 侧边栏 |
| 蜂房卡片 | `#111827` | 卡片/面板 |
| 菌丝分隔 | `#1e2a33` | 边框线 |
| 信息素青 | `#00d4aa` | 主品牌色 |
| 琥珀能量 | `#ff9f43` | 执行/警告 |
| 危险红 | `#ff4757` | 异常/错误 |
| 信息灰 | `#64748b` | 次要文字 |
| 主文字 | `#e2e8f0` | 标题/正文 |

## 字体栈

- 英文：Inter (SIL OFL)
- 中文：Noto Sans SC (SIL OFL)
- 数据：JetBrains Mono (SIL OFL)

## 布局框架

### 沉浸式（Immersive）
- 52px 图标栏（左侧）
- 悬浮状态条（顶部中央）
- 40px  subtle grid 背景
- 用于：驾驶舱、编排画布

### 渐进式（Progressive）
- 200px 展开导航（左侧）
- 48px 顶部栏
- 选中态 3px 竖线
- 用于：工作流监控、Agent 详情

## 组件清单

| 组件 | 描述 |
|------|------|
| HexIcon | 六边形图标，支持 size/color 变体 |
| StatCard | 左侧 3px 色带 + 大数字 + 微柱状图 |
| PillNav | 药丸形视图切换器 |
| TerminalLog | 颜色分级日志流 + 自动滚动 + 闪烁光标 |

## 标志性页面

| 页面 | 布局 | 核心特征 |
|------|------|----------|
| Cockpit | 沉浸式 | 中央 Orchestrator + 轨道 Agent 节点 + 底部统计 |
| AgentCanvas | 沉浸式 | DAG 节点 + 药丸导航 + 底部工具栏 |
| WorkflowMonitor | 渐进式 | 甘特图 + 药丸过滤 + 详细表格 |
| AgentDetail | 渐进式 | 身份卡 + Tab 切换 + 指标卡片 + 终端日志 |

## Ant Design 主题覆写

通过 `ConfigProvider` 一次性覆盖 token + components，不从零构建组件库。

关键 token：
- `colorBgBase`: `#0a0e1a`
- `colorBgContainer`: `#111827`
- `colorPrimary`: `#00d4aa`
- `colorError`: `#ff4757`
- `colorWarning`: `#ff9f43`
- `borderRadius`: 8
