---
change_id: abyss-hive-ui
status: approved
created_at: 2026-05-01
updated_at: 2026-05-01
author: wangbinyu
reviewer: ""
---

# Spec: Abyss Hive UI/UE Design System

> 本 Spec 引用正式设计文档 `docs/superpowers/specs/2026-05-01-abyss-hive-ui-design.md`

## 1. 概述

### 1.1 问题陈述

SchemaPlexAI 前端需要一套统一的设计系统，融合 TeamCity 布局、驾驶舱沉浸感、蜂巢生物隐喻和黑镜未来科技美学，覆盖全局样式 + 4 个标志性页面。

### 1.2 范围

- 全局设计系统（色彩、字体、间距、圆角、阴影）
- Ant Design 5 ConfigProvider 主题覆写
- CSS 变量系统
- 两种布局框架（沉浸式/渐进式）
- 4 个基础组件（HexIcon、StatCard、PillNav、TerminalLog）
- 4 个标志性页面（驾驶舱、编排画布、工作流监控、Agent 详情）
- 路由集成
- Vitest 测试覆盖

### 1.3 非目标

- 从零构建组件库（使用 AntD token 覆写）
- 移动端响应式适配
- 自定义图标字体
- 动画框架（仅 CSS transition）
- ECharts/x6 深度集成（页面占位，Phase 2 接入）

## 2. 架构视图

### 2.1 组件关系

```
main.tsx
  ├── ConfigProvider (abyssHiveTheme)
  ├── Google Fonts (Inter + Noto Sans SC + JetBrains Mono)
  └── App (Router)
        ├── /cockpit → Cockpit → ImmersiveLayout
        ├── /canvas → AgentCanvas → ImmersiveLayout
        ├── /workflows → WorkflowMonitor → ProgressiveLayout
        └── /agents → AgentDetail → ProgressiveLayout

Hive Components (shared):
  ├── HexIcon
  ├── StatCard
  ├── PillNav
  └── TerminalLog

Layout Components:
  ├── ImmersiveLayout (icon sidebar + floating header)
  └── ProgressiveLayout (expanded sidebar + top header)
```

### 2.2 数据流

纯展示型页面，无复杂数据流。Mock 数据内嵌于页面组件中，后续通过 API 替换。

## 3. 接口规格

无新增 API。页面使用现有前端路由体系。

## 4. 数据模型

无数据库变更。纯前端实现。

## 5. 状态机

不适用。

## 6. 异常场景

| 场景 | 预期行为 |
|------|----------|
| Google Fonts 加载失败 | 回退到系统字体栈（PingFang SC / Microsoft YaHei / sans-serif） |
| 路由 404 | 保持现有 NotFound 页面行为 |

## 7. 非功能需求

### 7.1 性能
- 首屏加载：新增 CSS 变量 + 主题配置 < 5KB，无显著影响
- 字体加载：使用 `display=swap`，避免 FOIT

### 7.2 安全
- 无用户输入处理（纯展示）
- 无外部 API 调用

### 7.3 兼容性
- 不破坏现有 API
- 不破坏现有页面（新增路由，默认路由改为 `/cockpit`）
- 需要前端配合：无（纯前端实现）

## 8. 风险与回退

| 风险 | 影响 | 缓解 | 回退方案 |
|------|------|------|----------|
| AntD token 覆写不完全 | 部分组件显示为默认浅色主题 | 逐步添加 component-level token | 回退到默认 AntD 深色主题 |
| 字体加载慢 | FOUT/FOIT | font-display: swap | 使用系统字体回退 |
| 沉浸式布局与现有页面冲突 | 路由嵌套问题 | 独立布局组件，不修改现有 Layout | 保留现有 Layout 作为默认 |

## 9. 相关文档

- **Design Spec**: `docs/superpowers/specs/2026-05-01-abyss-hive-ui-design.md`
- **Implementation Plan**: `docs/superpowers/plans/2026-05-01-abyss-hive-ui.md`
- **Wiki**: `wiki/frontend/abyss-hive-design.md`
