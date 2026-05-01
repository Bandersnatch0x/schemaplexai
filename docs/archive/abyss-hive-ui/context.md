---
change_id: abyss-hive-ui
status: proposed
created_at: 2026-05-01
---

# Task Context

## Change Goal

在 SchemaPlexAI 前端项目中实施"深渊蜂巢"设计系统，包括：
1. 全局 Ant Design 5 暗色主题覆写
2. CSS 变量系统
3. 两种布局框架（沉浸式/渐进式）
4. 4 个可复用基础组件（HexIcon、StatCard、PillNav、TerminalLog）
5. 4 个标志性页面（驾驶舱、编排画布、工作流监控、Agent 详情）
6. 路由集成
7. Vitest 测试覆盖

## Related Modules

- `schemaplexai-ui/` — 前端项目根目录
- `schemaplexai-ui/src/main.tsx` — 入口文件（需注入 ConfigProvider + 字体）
- `schemaplexai-ui/src/router/index.tsx` — 路由配置（需添加 4 条新路由）
- `schemaplexai-ui/src/index.css` — 全局样式（需重构）

## Key Files

### 新创建
- `schemaplexai-ui/src/theme/index.ts` — AntD theme tokens
- `schemaplexai-ui/src/styles/variables.css` — CSS 自定义属性
- `schemaplexai-ui/src/styles/global.css` — 全局覆写
- `schemaplexai-ui/src/components/Hive/HexIcon.tsx` — 六边形图标
- `schemaplexai-ui/src/components/Hive/StatCard.tsx` — 统计卡片
- `schemaplexai-ui/src/components/Hive/PillNav.tsx` — 药丸导航
- `schemaplexai-ui/src/components/Hive/TerminalLog.tsx` — 终端日志
- `schemaplexai-ui/src/components/Hive/index.ts` — 统一导出
- `schemaplexai-ui/src/components/Layout/ImmersiveLayout.tsx` — 沉浸式布局
- `schemaplexai-ui/src/components/Layout/ProgressiveLayout.tsx` — 渐进式布局
- `schemaplexai-ui/src/components/Layout/index.ts` — 统一导出
- `schemaplexai-ui/src/pages/Cockpit/index.tsx` — 驾驶舱页面
- `schemaplexai-ui/src/pages/AgentCanvas/index.tsx` — 编排画布页面
- `schemaplexai-ui/src/pages/WorkflowMonitor/index.tsx` — 工作流监控页面
- `schemaplexai-ui/src/pages/AgentDetail/index.tsx` — Agent 详情页面

### 修改
- `schemaplexai-ui/package.json` — 添加依赖和测试脚本
- `schemaplexai-ui/vite.config.ts` — 添加 vitest 配置
- `schemaplexai-ui/src/main.tsx` — 注入 ConfigProvider + 字体加载
- `schemaplexai-ui/src/index.css` — 重构为导入 variables.css + global.css
- `schemaplexai-ui/src/router/index.tsx` — 添加新路由，默认改为 `/cockpit`

## Decision Log

| # | 决策 | 原因 |
|---|------|------|
| 1 | 使用 `#0a0e1a` 而非纯黑 | 从 AI 概念稿中提取，蓝黑更有层次感和科技感 |
| 2 | AntD ConfigProvider 覆写而非自定义组件库 | 控制开发成本，利用 AntD 成熟组件 |
| 3 | 字体使用开源 SIL OFL 协议（Inter + Noto Sans SC + JetBrains Mono） | 用户要求字体必须开源 |
| 4 | 输入框参照 x.com 风格（transparent + 浮动标签 + 8px 圆角） | 用户明确要求 |
| 5 | 场景自适应布局（沉浸式 vs 渐进式） | 兼顾展示冲击力和管理效率 |

## Open Questions

1. 是否需要在现有页面（Dashboard、AgentManager 等）上应用新主题？—— 计划暂不修改，它们继续使用 ProgressiveLayout
2. ECharts 性能曲线和 x6 DAG 编辑器何时接入？—— Phase 2，当前页面占位
3. 是否需要 dark mode 切换？—— 不需要，深渊主题是唯一模式
