---
topic: service-decomposition
stage: decision
version: v1.0
status: 已批准
supersedes: ""
---

# ADR-001: 微服务拆分与模块边界决策

> **日期**: 2026-04-15
> **决策人**: 架构评审委员会
> **状态**: 已批准

---

## 背景

SchemaPlexAI 最初采用单体 Spring Boot 应用架构。随着功能域增长（Agent 引擎、Spec 管理、工作流、RAG、集成等），单体应用出现以下问题：

- **部署耦合**：任何小改动都需要全量部署
- **技术栈锁定**：所有模块必须使用相同版本的 Spring Boot / Java
- **故障扩散**：Agent 引擎高负载影响系统治理接口可用性
- **团队并行度低**：多团队同时修改同一仓库，冲突频繁

## 决策

将单体应用拆分为 **9 个独立业务服务 + 1 个 Gateway + 1 个 Web 接入层**，共 13 个 Maven 模块。

### 服务边界

| 服务 | 职责 | 拆分理由 |
|------|------|----------|
| `schemaplexai-gateway` | 统一入口、JWT 鉴权、租户解析、限流、路由 | 横切关注点，必须与业务解耦 |
| `schemaplexai-web` | REST API、SSE、WebSocket、Knife4j | BFF 模式，聚合下游服务 |
| `schemaplexai-system` | 租户/用户/角色/权限/AI 模型 | 系统治理域，访问频率高，需独立扩缩容 |
| `schemaplexai-agent-config` | Agent 定义/配置/执行记录 | 配置域与执行域分离，配置变更不重启引擎 |
| `schemaplexai-agent-engine` | Agent 执行引擎、LLM 编排、Token 预算 | 核心计算密集型服务，独立扩缩容 |
| `schemaplexai-context` | RAG、知识文档、向量检索 | IO 密集型（MinIO/Milvus），与计算型分离 |
| `schemaplexai-spec` | Spec 文档/模板/评审/变更追踪 | 文档域，读写比高，可独立缓存策略 |
| `schemaplexai-workflow` | Flowable BPMN、AI 节点引擎 | 状态机长流程，需独立持久化策略 |
| `schemaplexai-quality` | 质量门禁/偏离检测/安全审计 | 安全域，独立审计策略 |
| `schemaplexai-integration` | GitHub/GitLab/Jenkins/MCP/Skill | 外部集成域，网络依赖多，故障隔离 |
| `schemaplexai-ops` | 制品/通知/成本分析 | 运营域，OLAP 查询为主 |
| `schemaplexai-task` | MQ 消费者/定时任务 | 异步任务聚合，统一调度 |

### 通信模式

```yaml
同步调用（查询类）:
  protocol: HTTP/REST
  client: OpenFeign + Spring Cloud LoadBalancer
  timeout: 5s

异步调用（事件驱动）:
  protocol: AMQP (RabbitMQ)
  exchange_type: topic
  ack_mode: manual
  dead_letter: enabled

数据一致性:
  pattern: eventual_consistency
  compensation: saga_pattern
  outbox: enabled
```

## 替代方案

| 方案 | 评估 | 结论 |
|------|------|------|
| 保持单体 | 部署简单，但无法解决团队并行度和故障隔离 | 拒绝 |
| 拆分为 5 个服务（粗粒度） | 减少运维复杂度，但 Agent 引擎与配置未分离 | 拒绝 |
| 拆分为 15+ 个服务（细粒度） | 过度拆分，增加网络延迟和运维负担 | 拒绝 |
| **9 个业务域服务（本方案）** | 按限界上下文拆分，平衡独立性与复杂度 | **采纳** |

## 影响

- **正面**：故障隔离、独立扩缩容、团队并行开发、技术演进自由
- **负面**：网络延迟增加（OpenFeign 优化后平均 +3-5ms）、分布式事务复杂度、运维成本增加
- **缓解**：非必要不跨服务查询；本地缓存（Caffeine）热点数据；Saga 补偿处理最终一致性

## 相关文档

- `docs/designs/system-architecture.md` v1.1
- `docs/plans/project-plan.md` v1.1
