---
change_id: agent-engine-core-completion
status: proposed
created_at: 2026-05-04
author: Claude
---

# Proposal: Agent Engine Core Completion — 消除 Stub 实现并补全状态处理器

## 一句话描述

将 `schemaplexai-agent-engine` 模块中 4 个优先级的 Stub/缺失实现补全为生产级代码，涵盖 ToolRegistry 工具注册体系、剩余状态处理器（PAUSED/RETRYING/GATE_BLOCKED）、Prometheus 指标管道和租户环境安全配置。

## 背景与动机

- **问题**: `project-progress.md`（2026-05-01）记录了 agent-engine 模块在上一次迭代（Cursor Evaluation-First）后遗留的 4 个优先级任务。当前 `ToolCallingStateHandler` 使用启发式 `parseToolCalls()` 和 `executeToolStub()` 存根；`PausedStateHandler` 仅记录日志不持久化快照；`RETRYING` 状态无处理器；`GATE_BLOCKED` 缺乏重试/通知机制；指标采集链路未接入 Prometheus；租户环境安全检查使用临时 `tenantId` 字符串。
- **影响**: 不解决则 agent-engine 无法在生产环境安全执行工具调用、无法支持暂停/恢复工作流、无法自动重试、无法监控工具调用质量、无法按环境差异化安全策略。
- **触发**: `project-progress.md` 中的 4 个 Next Steps（Priority 1-4），零阻塞。

## 目标

- [ ] **P1: ToolRegistry** — 实现工具注册/发现机制，替换 `parseToolCalls()` 启发式解析为结构化 OpenAI/Anthropic 格式解析，替换 `executeToolStub()` 为真实工具适配器
- [ ] **P2: 剩余状态处理器** — 实现 `RetryingStateHandler`（基于 `ToolErrorCategory.retryable` 自动重试）、完善 `PausedStateHandler`（持久化快照 + 等待外部 Resume 信号）、完善 `GateBlockedStateHandler`（通知 + 重试倒计时）、集成 `AgentLoopDetectionService` 到状态机
- [ ] **P3: Evaluation Metrics Pipeline** — 暴露 Prometheus 指标端点（Keep Rate / Latency P99 / Blocked Rate / Error Rate by category），Grafana dashboard 骨架
- [ ] **P4: Tenant Environment Config** — 创建 `TenantEnvironmentConfig` 实体，环境-租户映射，多环境安全策略（dev/staging/prod）

## 范围

### In Scope

- [x] `ToolRegistry` + `ToolAdapter` 接口体系 + 至少 2 个内置适配器（FileRead、HttpCall）
- [x] 结构化工具调用解析：OpenAI `tool_calls` JSON + Anthropic `tool_use` XML
- [x] `RetryingStateHandler` 状态处理器
- [x] `PausedStateHandler` 完善（快照持久化 + Resume API）
- [x] `GateBlockedStateHandler` 完善（AdmissionResult 反馈 + 可配置重试）
- [x] `AgentLoopDetectionService` 集成到 `ThinkingStateHandler` 和 `ToolCallingStateHandler`
- [x] Prometheus `MeterRegistry` 指标导出 + `/actuator/prometheus` 端点
- [x] `TenantEnvironmentConfig` 实体 + Mapper + 安全策略加载
- [x] 所有新增代码的单元测试（TDD）

### Out of Scope

- 前端 UI（`schemaplexai-ui`）变更
- 其他服务模块（system, workflow, quality 等）的修改
- 数据库 Migration 脚本（仅定义实体，由 DBA 执行）
- Grafana Dashboard JSON 导入（仅提供 JSON 骨架文件）
- Milvus/ClickHouse/Redis 数据面变更
- `OBSERVATION` 状态处理器（当前代码中也缺失）

## 影响面评估

| 模块/服务 | 影响类型 | 说明 |
|-----------|---------|------|
| `schemaplexai-agent-engine` | 核心变更 | 新增 ~8 个 Java 文件，修改 ~6 个现有文件 |
| `schemaplexai-model` | 新增实体 | `TenantEnvironmentConfig` 实体类 |
| `schemaplexai-dao` | 新增 Mapper | `TenantEnvironmentConfigMapper` |
| `schemaplexai-common` | 无变更 | — |
| `schemaplexai-web` | 无变更 | — |

## 风险初判

| 风险 | 概率 | 影响 | 缓解思路 |
|------|------|------|---------|
| `ToolRegistry` 接口设计与现有 `ToolSandbox` 重叠 | 中 | 中 | 明确职责边界：ToolSandbox 负责沙箱执行安全，ToolRegistry 负责工具注册/发现/解析 |
| `RetryingStateHandler` 重试风暴 | 低 | 高 | 指数退避 + 最大重试次数硬限制 + 熔断器 |
| `AgentLoopDetectionService` 内存泄漏（ConcurrentHashMap 无界增长） | 中 | 中 | 添加 `clearRecords()` 在 COMPLETED/FAILED/CANCELLED 时调用，可选 TTL 驱逐 |
| Prometheus 指标时间序列基数爆炸（按 toolName + errorCategory 分维） | 低 | 低 | 仅保留 Top-N toolName 标签，其余归入 "other" |
| `TenantEnvironmentConfig` 缓存失效 | 低 | 中 | 使用 Caffeine Cache + 5min TTL + 手动刷新 API |

## 相关文档

- 项目记忆: `C:\Users\amsterdam\.claude\projects\D--code-space-frige\memory\project-progress.md`
- CLAUDE.md: `D:\code_space\frige\CLAUDE.md`
- 已归档: `.claude/outputs/archive-cursor-evaluation-first-2026-05-01.md`
- 工具 Wiki: `wiki/tool/`（已有3页）
- 关联服务: agent-engine (8084), agent-config (8083)
