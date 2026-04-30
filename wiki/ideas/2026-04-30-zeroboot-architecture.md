---
title: zeroboot 架构调研 — Sub-millisecond CoW VM Sandbox
status: researching
project: zeroboot
source: https://github.com/zerobootdev/zeroboot
creation_date: 2026-04-30
---

# zeroboot 架构调研

> Rust 构建的亚毫秒级 VM 沙箱，基于 Firecracker MicroVM + CoW (Copy-on-Write) 内存分叉，为 AI Agent 提供硬件级隔离的代码执行环境。

## 一句话描述

通过 Firecracker 模板快照 + `mmap(MAP_PRIVATE)` CoW 分叉，在 **~0.8ms** 内创建一个 256MB 但仅占 ~265KB RSS 的硬件隔离 VM 沙箱。

## 核心架构

```
                    ┌─────────────────────────────────────────┐
                    │          API Server (axum/tokio)        │
                    │  auth · rate-limit · metrics · batch    │
                    └──────────────┬──────────────────────────┘
                                   │
                    ┌──────────────▼──────────────────────────┐
                    │          Fork Engine (kvm.rs)           │
                    │  1. KVM create_vm + create_irq_chip     │
                    │  2. Restore IOAPIC redirect table       │
                    │  3. mmap(MAP_PRIVATE) snapshot memory   │
                    │  4. Restore CPU state (sregs→XCRS→...)  │
                    │  5. Serial I/O via 16550 UART emulation │
                    └──────────────┬──────────────────────────┘
                                   │
               ┌───────────────────┼───────────────────┐
               ▼                   ▼                   ▼
         ┌──────────┐       ┌──────────┐       ┌──────────┐
         │  Fork A  │       │  Fork B  │       │  Fork C  │
         │  256MB   │       │  256MB   │       │  256MB   │
         │  (CoW)   │       │  (CoW)   │       │  (CoW)   │
         └──────────┘       └──────────┘       └──────────┘
         Actual RSS:         Actual RSS:         Actual RSS:
           ~265KB              ~265KB              ~265KB
```

## 技术栈

| 层级 | 技术 | 职责 |
|------|------|------|
| API 层 | Rust axum + tokio | HTTP API、认证、限流、指标 |
| 分叉引擎 | Rust + KVM ioctl | VM 创建、CoW 内存映射、CPU 状态恢复 |
| VM 模板 | Firecracker MicroVM | 一次性启动预装 Python/numpy/pandas 的 VM |
| Guest | C (init.c) | 轻量级 init，通过串口读取命令并执行 |
| SDK | Python (零依赖)、TypeScript (fetch) | 客户端封装 |
| 指标 | Prometheus | fork 时间直方图、请求计数、错误率 |

## 关键闪光点

### 1. CoW 快照分叉 — 亚毫秒沙箱创建

**工作流程：**

**阶段 1 — 模板创建（一次性，~15s）：**
Firecracker 启动 VM → 预装 Python + numpy + pandas → 预导入所有模块 → 快照完整内存 + CPU 状态 → 产出 `snapshot/mem` + `snapshot/vmstate`

**阶段 2 — 分叉（每次，~0.8ms）：**
1. `KVM_CREATE_VM` + `KVM_CREATE_IRQCHIP` 创建新 VM
2. `mmap(MAP_PRIVATE)` 映射快照内存 → 内核自动 CoW：读共享快照，写触发 page fault 分配私有页
3. 按精确顺序恢复 CPU 状态：`sregs → XCRS → XSAVE → regs → LAPIC → MSRs → MP_STATE`
4. 通过 16550 UART 模拟串口与 Guest 通信

**结果：** 每个 fork 的 RSS 仅 ~265KB（而非 256MB），1000+ 并发无压力。

### 2. 硬件级隔离（不是容器）

- **VT-x / AMD-V 强制隔离** — 每个 fork 是独立 KVM VM，CoW 确保写时拷贝
- 在 benchmark 中验证：向 Fork A 写入 secret，Fork B 读同一偏移 → 读不到，证明隔离有效
- 非 namespace/cgroup 软隔离，是 CPU 硬件强制

### 3. Agent-Native API 设计

```json
// 单次执行
POST /v1/exec
{"code": "print(1+1)", "language": "python", "timeout_seconds": 30}

// 响应带 fork_time_ms、exec_time_ms、total_time_ms
{"id": "...", "stdout": "2\n", "stderr": "", "exit_code": 0,
 "fork_time_ms": 0.75, "exec_time_ms": 7.2, "total_time_ms": 8.0}

// 批量并行
POST /v1/exec/batch
{"executions": [
  {"code": "print(1)", "language": "python"},
  {"code": "console.log(2)", "language": "node"}
]}
```

API 设计极简——Agent 不需要管理 VM 生命周期，提交代码 → 拿结果。`fork_time_ms` / `exec_time_ms` 透明暴露性能。

### 4. 多运行时模板

可同时加载多个语言模板：
```bash
zeroboot serve python:./templates/python,node:./templates/node
```
每个模板独立预装运行时和库，fork 时按语言选择。

### 5. Agent 客户端示例（demo/agent.py）

使用 Claude SDK + zeroboot Python SDK 的 Agent：
- 两个工具：`run_python`（单次）和 `run_parallel`（并行5个方案）
- 系统提示强调"优先用并行比较多种方案"
- Rich TUI 实时展示 fork/exec 时间和结果表
- 零依赖 Python SDK (`pip install zeroboot`)

### 6. 内置性能基准

`zeroboot bench` 命令运行6个阶段的基准测试：
- Phase 1: 10,000 次纯 mmap CoW（P50 ~0.1ms）
- Phase 2: 1,000 次完整 fork（P50 ~0.8ms）
- Phase 3: 100 次 fork+exec echo（端到端延迟）
- Phase 4: 并发测试（10/100/1000）
- Phase 5: 内存隔离验证
- 输出对比表 vs E2B(~150ms) / microsandbox(~200ms) / Daytona(~27ms)

### 7. Rust 单二进制部署

单个 Rust 二进制 + Firecracker kernel/rootfs，systemd service 一行启动。无 Docker daemon、无 K8s、无容器编排依赖。

## 与其他沙箱方案的对比

| 指标 | zeroboot | E2B | Daytona | Docker |
|------|----------|-----|---------|--------|
| 启动延迟 P50 | **~0.8ms** | ~150ms | ~27ms | ~500ms |
| 内存/沙箱 | **~265KB** | ~128MB | ~50MB | ~20MB |
| 隔离级别 | **硬件 (VT-x)** | VM | 容器 | 容器 |
| 最大并发 | **1000+** | ~100 | ~1000 | ~100 |
| 技术栈 | Rust + KVM | Go + Firecracker | Go + containerd | Go |
| 部署复杂度 | 单二进制 | 云服务/自建 | 云服务/自建 | Docker daemon |

## 对 SchemaPlexAI 的可借鉴设计

### 直接可借鉴

1. **Agent 执行沙箱化方案** — zeroboot 的 REST API 模式最适合作为 `integration` 服务的沙箱后端：`agent-engine` 通过工具接口调用 `integration` → `integration` 调用 zeroboot API → 返回结果
2. **并行执行模式** — `/v1/exec/batch` 的多沙箱并行执行，对应 Agent 需要"同时尝试多种方案"的场景
3. **透明性能指标** — 每次执行返回 `fork_time_ms` + `exec_time_ms`，可直接接入 `ops` 的 ClickHouse 成本分析
4. **模板预装载** — Python 预装 numpy/pandas 的思路，我们的沙箱可以预装常用工具链

### 需要适配

5. **Java 生态集成** — zeroboot 是 REST API，Java 侧只需 HTTP 客户端封装，无需 Rust 改造。可以在 `integration` 服务增加 `ZerobootClient` 工具
6. **安全策略** — zeroboot 自带 API key 认证 + 速率限制（100 req/s），可直接作为安全边界

### 不适合

7. **不能替代所有沙箱场景** — zeroboot 适合代码片段执行，不适合完整的容器化开发环境（如 HolyClaude 的 headless browser）。两者互补
8. **不是时序/工作流引擎** — zeroboot 管理 VM 生命周期，不管理 Agent 工作流。需要 `agent-engine` + Flowable/Temporal 做编排

## 新增依赖评估

| 依赖 | 类型 | 影响 |
|------|------|------|
| zeroboot 二进制 | 新基础设施 | 需要部署到沙箱节点（需 KVM 支持） |
| Firecracker kernel/rootfs | 新基础设施 | 随 zeroboot 分发 |
| Python SDK | 零依赖 pip 包 | 仅 demo Agent 需要，Java 侧不需要 |
| API key 管理 | 配置 | 新增 `ZEROBOOT_API_KEY` 环境变量 |

## 总结

zeroboot 是当前业界最快的 VM 沙箱方案——用 CoW 分叉替代冷启动，把沙箱创建从 ~200ms 级别拉到 ~0.8ms。对 SchemaPlexAI 的价值在于：**提供了一个极简的、REST API 化的、硬件隔离的代码执行后端**，可以直接挂到 `integration` 服务的工具链上，解决 Agent 执行不可信代码的安全隔离问题。

## 参考链接

- [GitHub - zerobootdev/zeroboot](https://github.com/zerobootdev/zeroboot)
- [zeroboot.dev](https://zeroboot.dev)
