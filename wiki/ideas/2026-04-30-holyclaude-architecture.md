---
title: HolyClaude 架构调研
status: researching
project: holyclaude
source: https://github.com/CoderLuii/HolyClaude
creation_date: 2026-04-30
---

# HolyClaude 架构调研

> AI 编程工作站：Claude Code + Web UI + 7 个 AI CLI + Headless Browser + 50+ 工具。

## 一句话描述

基于 Docker 的"厚容器"AI 开发环境，将多个 AI CLI、Headless Browser 和开发工具打包为持久化工作站。

## 核心架构

```
[Docker Start]
    → entrypoint.sh (root)
        → UID/GID Remapping
        → File Pre-creation
        → First-boot check → bootstrap.sh (一次性)
        → exec /init (s6-overlay 作为 PID 1)
            → 监管 CloudCLI (:3001)
            → 监管 Xvfb (:99)
            → Claude Code CLI via CloudCLI
            → Chromium (headless) via Xvfb
```

## 关键设计决策

### 1. s6-overlay 进程监管

- 替代 supervisord，专为 Docker 设计的 init 系统
- 自动重启崩溃服务、信号转发、僵尸进程回收、优雅关闭
- 同时监管 Web UI 和虚拟显示

### 2. 多 CLI 共存

| CLI | 命令 | 认证方式 |
|-----|------|----------|
| Claude Code | `claude` | CloudCLI OAuth / API Key |
| Gemini CLI | `gemini` | `GEMINI_API_KEY` |
| OpenAI Codex | `codex` | `OPENAI_API_KEY` |
| Cursor | `cursor` | `CURSOR_API_KEY` |
| TaskMaster AI | `task-master` | 复用 Provider Key |
| Junie | `junie` | JetBrains 订阅 |
| OpenCode | `opencode` | 多 Provider TUI |

### 3. Headless Browser 栈

- Chromium + Xvfb (1920x1080 @ :99) + Playwright
- Xvfb 提供虚拟显示，即使 headless 模式也需要显示上下文
- Docker 需要 `shm_size: 2g`、`SYS_ADMIN`、`seccomp=unconfined`

### 4. 权限自动映射

- 每次启动时 `entrypoint.sh` 将容器内用户的 UID/GID 重新映射为主机用户的 `PUID`/`PGID`
- 解决 bind mount 的 root 拥有文件问题

### 5. 持久化策略

- `./data/claude:/home/claude/.claude` — 凭证、设置、记忆
- `./workspace:/workspace` — 开发目录

## 可借鉴设计

1. **容器化 Agent 运行环境**：我们的 Agent 执行目前无沙箱隔离，可引入 Docker 化的执行环境
2. **进程监管**：长时 Agent 任务需要可靠的进程监管（s6-overlay 或类似方案）
3. **多 Provider 统一接入**：当前只支持 LangChain4j 的 Provider，可设计多 CLI/多 Provider 的统一接入层
4. **Headless Browser 作为工具**：Agent 需要网页自动化能力时，提供预配置的 Headless Browser
5. **权限映射**：如果提供容器化执行，需解决文件所有权映射问题

## 参考链接

- [GitHub - CoderLuii/HolyClaude](https://github.com/CoderLuii/HolyClaude)
- [HolyClaude architecture.md](https://github.com/CoderLuii/HolyClaude/blob/master/docs/architecture.md)
