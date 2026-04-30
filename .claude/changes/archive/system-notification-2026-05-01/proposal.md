---
change_id: system-notification
status: proposed
created_at: 2026-05-01
author: wangbinyu
---

# Proposal: System Notification Module

## 一句话描述

为用户构建系统通知中心，支持接收、查看和标记已读系统消息。

## 背景与动机

- **问题**: 用户目前无法接收系统级消息（如任务完成提醒、工作流审批通知、系统维护公告），关键事件容易被遗漏。
- **影响**: 降低用户参与度和操作效率，增加支持成本。
- **触发**: 平台基础功能缺口，需在核心框架稳定后补齐。

## 目标

- [ ] 用户可以分页查询自己的通知列表（含未读/已读筛选）
- [ ] 用户可以将单条或全部通知标记为已读
- [ ] 系统可以通过 Service API 向指定用户发送通知
- [ ] 多租户隔离：用户只能看到本租户内发给自己的通知

## 范围

### In Scope（做什么）

- Notification 实体、数据库表、Mapper、Service、Controller
- REST API：查询列表、标记已读、全部已读
- 前端 TypeScript 类型定义
- 基础单元测试和集成测试

### Out of Scope（不做什么）

- 实时推送（WebSocket/SSE）— 未来增强
- 邮件/短信渠道 — 未来增强
- 通知模板管理 — 未来增强
- 管理员发通知的 UI — 未来增强

## 影响面评估

| 模块/服务 | 影响类型 | 说明 |
|-----------|---------|------|
| schemaplexai-model | 新增 | Notification 实体 |
| schemaplexai-dao | 新增 | NotificationMapper |
| schemaplexai-web | 新增 | NotificationController |
| sf_notification | 新增表 | 通知数据表 |

## 风险初判

| 风险 | 概率 | 影响 | 缓解思路 |
|------|------|------|---------|
| 大量通知导致查询慢 | 中 | 中 | 索引 + 分页 + 定期归档 |
| 并发标记已读冲突 | 低 | 低 | 乐观锁 / 幂等设计 |

## 相关文档

- wiki: `wiki/active-areas.md`
