---
title: AionUi 架构调研
status: researching
project: aionui
source: https://github.com/iOfficeAI/AionUi
creation_date: 2026-04-30
---

# AionUi 架构调研

> 免费开源多 Agent 桌面应用，Electron + React，统一接入 12+ AI Agent。

## 一句话描述

本地优先的 Agent 桌面编排层，通过分层 Channel 子系统桥接 AI Agent 与即时通讯平台，支持统一 MCP 管理和动态插件扩展。

## 核心架构

| 层级 | 技术 | 职责 |
|------|------|------|
| 桌面框架 | Electron | Main + Renderer 进程 |
| UI | React + UnoCSS | 界面渲染 |
| 本地数据库 | SQLite | 会话和配置持久化 |
| 构建工具 | Vite | 打包 |

## 关键设计决策

### 1. 分层 Channel 子系统（6 层）

```
Gateway Layer      → PluginManager, ActionExecutor
Core Layer         → ChannelManager, SessionManager, PairingService
Agent Layer        → ChannelMessageService, ChannelEventBus
Plugins Layer      → TelegramPlugin, LarkPlugin, ...
Adapters Layer     → 平台 API ↔ 统一格式转换
Actions Layer      → UI 按钮、斜杠命令
```

- **统一消息格式**：`IUnifiedIncomingMessage` / `IUnifiedOutgoingMessage`
- **会话隔离**：复合键 `userId:chatId` 维护独立上下文
- **双向广播**：`ChannelEventBus` 同步 AI 响应到桌面 UI 和外部 IM 平台
- **安全配对**：6 位随机配对码，桌面 UI 确认后才创建用户记录

### 2. 多 Agent 统一编排

- 自动检测已安装 CLI Agent（Claude Code、Codex、Qwen Code、Goose CLI 等）
- 统一 MCP 管理：配置一次，所有 Agent 自动同步
- 每个 Agent 独立配置、上下文、会话记忆

### 3. 统一扩展系统（RFC-001）

- **单一 Manifest**：`aion-extension.json` 声明能力
- **零源码修改**：从文件系统路径、环境变量或市场加载
- 覆盖：ACP 适配器、MCP Server、Preset Agent、Skill、Channel 插件、WebUI 路由、主题
- **企业特性**：集中分发、环境变量注入、路由隔离、默认认证

### 4. 扩展路线图

| 版本 | 特性 |
|------|------|
| v1.0 | 核心框架 + ACP、MCP、Agent、Skill |
| v1.1 | Channel 插件动态加载 |
| v1.2 | WebUI 扩展点（API/WS/中间件/静态文件） |
| v2.0 | CSS 主题 + 扩展市场 UI |
| v2.1 | 热重载（无需重启） |
| v3.0 | 沙箱隔离（VM2/Worker Threads）+ 基于能力的权限 |

## 可借鉴设计

1. **统一消息总线**：我们的 SSE/WebSocket 层可引入统一消息格式，隔离平台细节
2. **Channel 即插件**：IM/通讯平台接入应设计为可插拔的 Channel 插件
3. **统一 MCP 管理**：当前每个 Agent 可能有独立的 MCP 配置，可设计统一 MCP 注册中心
4. **扩展 Manifest**：插件/扩展系统使用声明式 Manifest，降低接入门槛
5. **会话隔离设计**：复合键 `userId:chatId` 的会话隔离模式可参考
6. **热重载扩展**：未来插件市场需要热重载能力

## 参考链接

- [GitHub - iOfficeAI/AionUi](https://github.com/iOfficeAI/AionUi)
- [AionUi Wiki](https://github.com/iOfficeAI/AionUi/wiki)
- [RFC-001 Unified Extension System](https://github.com/iOfficeAI/AionUi/issues/954)
