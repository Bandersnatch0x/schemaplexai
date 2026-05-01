---
change_id: abyss-hive-ui
status: planned
created_at: 2026-05-01
---

# Tasks: Abyss Hive UI Implementation

> 完整实施计划见 `docs/superpowers/plans/2026-05-01-abyss-hive-ui.md`
> 共 17 个任务，分 5 个阶段

## 阶段 1：基础设施（Task 1-5）

- [ ] **Task 1**: 安装依赖（vitest, @testing-library/react, jsdom, @antv/x6, echarts）
- [ ] **Task 2**: 配置 Vitest（vite.config.ts + jsdom + coverage）
- [ ] **Task 3**: 创建 AntD 主题配置（`src/theme/index.ts`）
- [ ] **Task 4**: 创建 CSS 变量文件（`src/styles/variables.css` + `global.css`）
- [ ] **Task 5**: 更新入口文件（`main.tsx` ConfigProvider + 字体加载，重构 `index.css`）

## 阶段 2：基础组件（Task 6-9）

- [ ] **Task 6**: HexIcon 组件 + 测试
- [ ] **Task 7**: StatCard 组件 + 测试
- [ ] **Task 8**: PillNav 组件 + 测试
- [ ] **Task 9**: TerminalLog 组件 + 测试 + 统一导出

## 阶段 3：布局框架（Task 10-11）

- [ ] **Task 10**: ImmersiveLayout 组件 + 测试
- [ ] **Task 11**: ProgressiveLayout 组件 + 测试 + 统一导出

## 阶段 4：标志性页面（Task 12-15）

- [ ] **Task 12**: Cockpit 页面（驾驶舱大屏）
- [ ] **Task 13**: AgentCanvas 页面（编排画布）
- [ ] **Task 14**: WorkflowMonitor 页面（工作流监控）
- [ ] **Task 15**: AgentDetail 页面（Agent 详情）

## 阶段 5：路由集成与验证（Task 16-17）

- [ ] **Task 16**: 更新路由配置（4 条新路由，默认路由改为 `/cockpit`）
- [ ] **Task 17**: 全量测试 + dev server 验证 + 最终 commit

## 执行状态

| 任务 | 状态 | 负责人 | 备注 |
|------|------|--------|------|
| Task 1 | pending | | |
| Task 2 | pending | | |
| Task 3 | pending | | |
| Task 4 | pending | | |
| Task 5 | pending | | |
| Task 6 | pending | | |
| Task 7 | pending | | |
| Task 8 | pending | | |
| Task 9 | pending | | |
| Task 10 | pending | | |
| Task 11 | pending | | |
| Task 12 | pending | | |
| Task 13 | pending | | |
| Task 14 | pending | | |
| Task 15 | pending | | |
| Task 16 | pending | | |
| Task 17 | pending | | |
