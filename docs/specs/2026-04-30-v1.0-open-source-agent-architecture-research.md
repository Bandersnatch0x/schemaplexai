
---
title: 开源AI Agent架构调研与设计借鉴
type: spec
source: wiki/ideas/2026-04-30-*-architecture.md
creation_date: 2026-04-30
update_date: 2026-05-01
status: draft
tags: [spec, research, agent-architecture, design-reference]
confidence: high
---

# 开源AI Agent架构调研与设计借鉴

> 调研6个主流开源AI Agent项目（open-agents、deer-flow、langfuse、holyclaude、aionui、zeroboot），通过三视角分析（架构/工程/产品），提炼SchemaPlexAI可直接借鉴的设计。

---

## 一、调研背景与目标

SchemaPlexAI 覆盖 Spec定义 → Workflow编排 → Agent执行 → Quality gating → Cost分析 全生命周期。当前脚手架已完成，但 Agent 执行引擎、可观测性、质量评估等核心模块仍为 stub 状态。

本次调研目标：从业界主流开源AI Agent项目中，提取可落地到 SchemaPlexAI 的架构模式、数据模型和设计决策。

---

## 二、五项目概览

| 项目 | 来源 | 定位 | 核心架构模式 | 技术栈 | 与SchemaPlexAI关联度 |
|------|------|------|-------------|--------|---------------------|
| **langfuse** | langfuse/langfuse | LLM可观测性平台 | Trace/Span/Generation层级模型 + OLTP/OLAP分离 | Next.js + PostgreSQL + ClickHouse + Redis | **极高** — 可观测性、Prompt管理、评估体系 |
| **deer-flow** | bytedance/deer-flow | 超级Agent Harness | Lead Agent + 子Agent + 中间件链 | LangGraph + Python + Docker/K8s | **高** — Agent执行引擎架构演进 |
| **open-agents** | vercel-labs/open-agents | 云Agent模板 | Agent/Sandbox三层分离 + 持久化Workflow | Next.js + Vercel Workflow SDK | **高** — 执行隔离、Skill生态 |
| **aionui** | iOfficeAI/AionUi | 多Agent桌面编排 | Channel子系统 + 扩展Manifest | Electron + React + SQLite | **中** — MCP管理、消息总线、扩展系统 |
| **holyclaude** | CoderLuii/HolyClaude | AI编程工作站 | 厚容器 + 进程监管 + 多CLI共存 | Docker + s6-overlay + Playwright | **中** — 容器化执行、Headless Browser |
| **zeroboot** | zerobootdev/zeroboot | 亚毫秒VM沙箱 | CoW内存分叉 + KVM硬件隔离 + REST API | Rust + Firecracker + KVM | **极高** — 沙箱执行隔离、并行批量执行、透明性能指标 |

---

## 三、三视角圆桌辩论核心结论

_以下结论综合架构师视角（系统边界/技术选型/扩展性）、工程师视角（实现成本/技术栈匹配/集成难度）、产品视角（用户价值/功能优先级/竞争差异）三向交叉辩论得出。_

### 辩题1：Langfuse 数据模型是否应全量引入？

**架构师**：Trace/Span/Generation 三级模型是行业事实标准，与 OpenTelemetry 原生兼容。SchemaPlexAI 已有 PostgreSQL + ClickHouse，数据模型可直接嵌入 `agent-engine` + `ops` 服务。

**工程师**：纯数据模型引入零技术栈冲突。Java 侧定义 POJO 层级结构，ClickHouse 存观测明细，PostgreSQL 存元数据。与现有 Jaeger 互补而非替代——Jaeger 管微服务链路，Langfuse 模型管 Agent 执行链路。

**产品经理**：可观测性是企业用户采用 AI Agent 平台的"入场券"。没有 Trace 追踪、成本归因、Prompt 版本的 Agent 平台无法进入生产环境。这是典型的 Must-have 功能。

**结论**：**Phase 1 立即引入。** 定义 `ObservabilityTrace`、`ObservabilitySpan`、`ObservabilityGeneration` 实体，ops 服务负责 ClickHouse 写入查询，web 服务做 Trace 可视化。

---

### 辩题2：DeerFlow 的 Agent 架构是否适合 Spring Cloud 微服务？

**架构师**：中间件链和 Lead/Sub-Agent 分层是优秀的设计模式，但 DeerFlow 基于 Python/LangGraph。Spring Cloud 微服务需要在 LangChain4j 基础上封装适配层，不能直接移植。

**工程师**：中间件链适合用 Spring 的 `HandlerInterceptor` 或自研 `AgentMiddleware` 接口实现。Lead/Sub-Agent 分层涉及服务间通信协议设计——当前 REST 同步调用需改用 RabbitMQ 异步编排或引入 Temporal。实现成本属于中等（2-3人月）。

**产品经理**：长时程Agent任务和复杂任务拆解是企业R&D场景的核心差异化能力。但当前 SchemaPlexAI 的 Agent 引擎仍是 stub，建议先用简单中间件链跑通基本流程，Lead/Sub-Agent 分层留到引擎稳定后再引入。

**结论**：**Phase 2 引入中间件链（精简为5-7层），Phase 3 引入 Lead/Sub-Agent 分层。** 先不改服务拆分，在 `agent-engine` 内部通过中间件管道实现可插拔扩展。

---

### 辩题3：沙箱隔离方案选型 — zeroboot CoW VM vs Docker

**架构师**：Agent 逻辑与执行环境分离是 open-agents 最核心的架构决策，长期看必须。但具体方案有代际差异——Docker 容器方案（~500ms 冷启动、~20MB RSS、namespace 软隔离）vs zeroboot CoW Fork（~0.8ms、~265KB RSS、VT-x 硬件隔离）。zeroboot 的 REST API 化设计天然适配 Spring Cloud 微服务，无需引入容器编排层。

**工程师**：zeroboot 单 Rust 二进制部署，仅需 KVM 支持（云服务器标配），无 Docker daemon、镜像仓库依赖。Java 侧集成成本约 1-2 人周（HTTP Client + JSON），远低于 Docker 方案的 3-4 人周（镜像构建、资源配额、网络策略）。不足在于目前仅支持 Python/Node 模板，但恰好覆盖 Agent 代码执行的核心场景。Headless Browser 等需完整 Linux 环境的场景仍需 Docker 互补。

**产品经理**：zeroboot 将沙箱隔离从"昂贵的安全税"变成"几乎免费的基础能力"。0.8ms 延迟用户无感知，265KB 内存意味着单机万级并发。这对多租户 SaaS 的成本模型是革命性的。

**结论**：**Phase 3 采用 zeroboot CoW VM 作为主沙箱方案（代码执行/Shell命令），Docker 降级为 Headless Browser 等完整环境场景的互补方案。** 在 `integration` 服务增加 `ZerobootClient` 工具，zeroboot 二进制部署到沙箱节点，`agent-engine` 通过工具接口 → REST API 调度隔离执行。

---

### 辩题4：统一 MCP 管理和扩展系统优先级？

**架构师**：AionUi 的分层 Channel 子系统和统一 MCP 管理设计优秀，但 Electron 桌面定位与 SchemaPlexAI 的 Web 平台有偏差。Message bus 概念可引入，Channel/IM 集成暂不需要。

**工程师**：MCP 统一管理是配置层改造，不涉及核心引擎，实现成本低（1-2人周）。统一消息格式是接口层设计，成本极低。

**产品经理**：统一 MCP 管理解决多 Agent 重复配置的摩擦，对多 Agent 场景有直接价值。扩展市场是平台生态化的长期方向，当前优先级不高。

**结论**：**Phase 2 引入统一 MCP 管理 + 统一消息格式，扩展 Manifest 留到 Phase 3。**

---

### 辩题5：HolyClaude 的厚容器模式是否适用？

**架构师/工程师/产品经理一致**：**不适用。** 厚容器（7 个 CLI + 浏览器 + 开发工具打包到一个镜像）与 Spring Cloud 微服务架构根本冲突。唯一可借鉴的是 Headless Browser 作为 Agent 工具的能力，作为可选工具集成到 `integration` 服务。

---

### 辩题6：zeroboot 的风险与边界对策

**架构师**：zeroboot 需要 KVM 支持，对部署节点有硬性要求（裸金属或支持嵌套虚拟化的云实例）。建议在 `integration` 服务中设计 `SandboxClient` 接口，zeroboot 作为默认实现，Docker 作为 fallback——当 KVM 不可用时自动降级。

**工程师**：zeroboot 目前仅支持 Python/Node 模板，Agent 执行 Bash/Go/Java 等代码片段时需回退到 Docker。可在 `ZerobootClient` 中按 language 参数路由——Python/Node 走 zeroboot，其余走 Docker。zeroboot 社区尚小（2260 stars，12 issues），需关注长期维护风险。

**产品经理**：zeroboot 的 KVM 依赖并不影响核心价值——主流云厂商（AWS EC2 bare metal、GCP sole-tenant、阿里云 ECS 神龙）均支持 KVM。对不支持 KVM 的环境，Docker fallback 提供了平滑的体验降级，不会造成功能不可用。

**结论**：**zeroboot 作为首选沙箱，Docker 作为 fallback。** `integration` 服务抽象 `SandboxClient` 接口，根据 `language` 参数和 KVM 可用性自动路由。此设计将 zeroboot 的 KVM 依赖从"硬性约束"降为"性能优化项"。

---

## 四、设计借鉴路线图

### Phase 1（即日—2周）：核心可观测性与配置规范

**目标**：让 Agent 执行从黑盒变透明，建立 Prompt 管理基线。

| # | 设计项 | 来源 | 实施路径 | 影响服务 |
|---|--------|------|----------|----------|
| 1 | **Trace/Span/Generation 数据模型** | langfuse | 定义实体类 `ObservabilityTrace`/`Span`/`Generation`，agent-engine 每次执行写入层级追踪数据 | agent-engine, ops, agent-config |
| 2 | **Prompt 版本管理** | langfuse | agent-config 增加 Prompt 版本表 `sf_prompt_version`，支持 label（production/staging）+ SDK 运行时获取 | agent-config |
| 3 | **Skill 定义规范（Markdown+YAML）** | deer-flow | 定义 `SKILL.md` 规范文档，agent-config 支持 Markdown 格式的 Skill 注册 | agent-config |
| 4 | **统一消息格式** | aionui | 在 web 服务定义 `UnifiedMessage` 接口，统一 SSE/WS/REST 的消息语义 | web, integration |

### Phase 2（2周—2个月）：Agent 引擎增强

**目标**：将 agent-engine 从同步单体执演进为可插拔中间件管道，统一 MCP 管理。

| # | 设计项 | 来源 | 实施路径 | 影响服务 |
|---|--------|------|----------|----------|
| 5 | **可插拔中间件链** | deer-flow | 设计 `AgentMiddleware` 接口，首批实现：记忆、循环检测、工具错误处理、人类介入点，共 4-5 层 | agent-engine, context |
| 6 | **统一 MCP 注册中心** | aionui | integration 服务增加 MCP 统一管理，"一次配置、多 Agent 引用" | integration, agent-config |
| 7 | **渐进式 Skill 加载** | deer-flow | agent-engine 支持按需加载 Skill 配置，避免全量上下文注入 | agent-engine |
| 8 | **LLM-as-a-Judge 评估** | langfuse | quality 服务引入基于 LLM 的自动化评估流水线 | quality |
| 9 | **会话隔离（复合键）** | aionui | context 服务使用 `tenantId:userId:conversationId` 复合键隔离会话上下文 | context |

### Phase 3（2—6个月）：执行隔离与分层编排

**目标**：执行环境沙箱化，Agent 引擎支持 Lead/Sub-Agent 分层编排。

| # | 设计项 | 来源 | 实施路径 | 影响服务 |
|---|--------|------|----------|----------|
| 10 | **Agent/沙箱执行分离** | zeroboot | agent-engine 编排决策，代码/命令通过 REST API 委托到 zeroboot CoW VM 沙箱（~0.8ms fork, 265KB RSS, 硬件隔离）；Docker 仅作为 Headless Browser 场景的互补 | agent-engine, integration |
| 11 | **Lead/Sub-Agent 分层编排** | deer-flow | 将 agent-engine 内部拆分为 orchestration 层 + worker 层，RabbitMQ 做任务分发 | agent-engine, task |
| 12 | **扩展 Manifest 系统** | aionui | 定义 `schemaplexai-extension.json` 规范，支持 ACP/MCP/Skill 扩展 | integration, agent-config |
| 13 | **Headless Browser 工具** | holyclaude | integration 服务增加 Playwright/Chromium 预配置的容器化 Browser 工具 | integration |

---

## 五、被排除的设计

| 设计 | 来源 | 排除原因 |
|------|------|----------|
| 厚容器多 CLI 共存 | holyclaude | 与微服务架构根本冲突，攻击面和镜像体积不可控 |
| Electron 桌面框架 | aionui | SchemaPlexAI 定位 Web 平台，非桌面应用 |
| IM 平台 Channel 插件 | aionui | 当前阶段 IM 集成非核心场景，已有独立方案 |
| s6-overlay 进程监管 | holyclaude | K8s 原生健康检查和生命周期已覆盖 |
| Vercel Workflow SDK | open-agents | 平台绑定，需自研 | 
| 完整 11 层中间件链 | deer-flow | 当前 stub 阶段过度设计，精简为 4-5 层够用 |
| Docker 容器沙箱（代码执行场景） | open-agents | zeroboot CoW VM 在启动速度（0.8ms vs 500ms）、内存（265KB vs 20MB+）、隔离级别（硬件 vs namespace）全面优于 Docker；Docker 仅保留给 Headless Browser 等需完整 Linux 环境的场景 |

---

## 六、风险评估

| 风险 | 等级 | 缓解措施 |
|------|------|----------|
| 状态化执行 vs 无状态微服务冲突 | 中 | 状态外置到 Redis/PostgreSQL，服务保持无状态 |
| LangGraph 概念在 Java 中无直接对应 | 中 | 只借鉴架构模式，LangChain4j 基础封装适配层 |
| 中间件链性能损耗 | 低 | 精简为 4-5 层，支持按场景选择性加载，异步执行非阻塞中间件 |
| ClickHouse 双写一致性问题 | 低 | RabbitMQ 异步同步，容忍秒级延迟 |
| 扩展系统供应链攻击 | 中 | 沙箱中运行扩展代码，Manifest 签名验证，核心扩展点白名单 |
| zeroboot KVM 依赖限制部署节点 | 中 | 抽象 `SandboxClient` 接口，不支持 KVM 时自动降级到 Docker fallback |
| zeroboot 仅支持 Python/Node 运行时 | 低 | 按 language 参数路由：Python/Node → zeroboot，其余 → Docker |
| zeroboot 社区规模小 | 低 | 关注版本更新和 issue 响应速度，保留 Docker 互补路径 |

---

## 七、下一步行动

1. **评审本 spec 完成（2026-04-30）** — 已识别并修复 5 项问题（辩题3/6去重、标题修正、风险评估补充、辩题4工期澄清、本节更新）
2. **Phase 1 计划已就绪** — `docs/plans/2026-04-30-phase1-observability-foundation.md`（10 个 Task，TDD），待选择执行方式（Subagent-Driven / Inline）
3. **调研结果已归档** — 6 个 ideas 文件在 `wiki/ideas/`，wiki index/log/active-areas 已同步更新
4. **Phase 2/3 计划待创建** — 中间件链、MCP 注册中心、沙箱隔离、Lead/Sub-Agent 分层各自需要独立实施计划

## 参考链接

- [GitHub - vercel-labs/open-agents](https://github.com/vercel-labs/open-agents)
- [GitHub - bytedance/deer-flow](https://github.com/bytedance/deer-flow)
- [GitHub - langfuse/langfuse](https://github.com/langfuse/langfuse)
- [GitHub - CoderLuii/HolyClaude](https://github.com/CoderLuii/HolyClaude)
- [GitHub - iOfficeAI/AionUi](https://github.com/iOfficeAI/AionUi)
- [GitHub - zerobootdev/zeroboot](https://github.com/zerobootdev/zeroboot)
- [[2026-04-30-open-agents-architecture]] — open-agents 详细调研
- [[2026-04-30-deer-flow-architecture]] — deer-flow 详细调研
- [[2026-04-30-langfuse-architecture]] — langfuse 详细调研
- [[2026-04-30-holyclaude-architecture]] — holyclaude 详细调研
- [[2026-04-30-aionui-architecture]] — aionui 详细调研
- [[2026-04-30-zeroboot-architecture]] — zeroboot 详细调研
