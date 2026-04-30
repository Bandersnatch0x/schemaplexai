# SchemaPlexAI — 修订版项目计划书

> **版本**：v1.1（基于架构评审修订）  
> **日期**：2026-04-29  
> **目标**：根据架构评审结果，调整工程结构、开发阶段与工期估算

---

## 一、修订概要

本次修订基于架构评审报告 v1.0 的 15 项建议，主要调整：

| 修订类别 | 核心变更 | 工期影响 |
|----------|----------|----------|
| **系统架构** | 增加 API Gateway；按域拆分 Service 为 9 个独立服务；增加可观测性层 | +0.5 周 |
| **Agent 引擎** | `runAgenticLoop` 重构为显式状态机；增加中断/恢复、Token 预算、影子审核反馈闭环 | +1 周 |
| **数据架构** | `sf_chat_message` 分区归档；ClickHouse 游标持久化；Milvus-PG 一致性保障 | +0.5 周 |
| **运维保障** | MinIO Lifecycle；RabbitMQ 幂等；Redis 防护；数据备份策略 | 纳入现有工期 |
| **总计** | | **+2 周（26 周 → ~28 周）** |

> **建议总工期**：**30 周（~7 个月）**，含 2 周缓冲。

---

## 二、修订版工程模块结构

```
schemaplexai/
├── schemaplexai-gateway              # 【新增】API Gateway（统一入口）
│   ├── filter/                       # JWT鉴权 / 租户解析 / 限流 / 日志
│   └── config/
├── schemaplexai-web                  # Web 接入层（职责精简为编排层）
│   ├── controller/                   # REST API 控制器
│   ├── sse/                          # SSE 事件推送
│   ├── websocket/                    # WebSocket 处理器
│   └── config/
├── schemaplexai-system               # 【拆分】系统治理服务
│   ├── tenant/ / user/ / role/ / permission/ / menu/
│   ├── ai-model/                     # 【新增】模型供应商管理
│   └── config/
├── schemaplexai-spec                 # 【拆分】Spec 规范中心
│   ├── spec/ / document/ / version/ / template/ / review/
│   └── steering/                     # 【明确】Steering 是 Spec 子类型
├── schemaplexai-agent-config         # 【拆分】Agent 配置中心
│   ├── solo-agent/ / team-agent/ / model-binding/ / tool-registry/
│   ├── hook/ / execution-mode/
│   └── shadow-config/                # 【新增】影子审核反馈配置
├── schemaplexai-agent-engine         # 【拆分】Agent 执行引擎
│   ├── orchestrator/                 # Solo/Team 编排
│   ├── engine/                       # 【修订】状态机驱动的 Agentic Loop
│   │   ├── AgentStateMachine.java
│   │   └── state/                    # Thinking / ToolCalling / Paused / Completed
│   ├── context/                      # 四层 SystemPrompt 注入
│   ├── memory/                       # 双层记忆 + 压缩策略
│   ├── model/                        # 【新增】LLM Provider 抽象层
│   ├── loop/                         # 循环检测 / 收敛判断 / 质量检查
│   ├── admission/                    # 【修订】四维限流 + Token 预算
│   ├── lifecycle/                    # 【新增】中断/恢复/取消 + 上下文快照
│   ├── shadow/                       # 【修订】影子审核 + 反馈闭环
│   ├── security/                     # 输出合规 / 敏感字段遮蔽
│   └── trace/                        # 分布式追踪
├── schemaplexai-workflow             # 【拆分】AI 工作流中心
│   ├── template/ / instance/ / trigger/
│   ├── node/                         # 【明确】WorkflowNodeEngine（AI节点）
│   └── flowable/                     # 【明确】Flowable 边界（BPMN状态机）
├── schemaplexai-context              # 【拆分】上下文与知识中心
│   ├── workspace/ / context/ / snapshot/
│   ├── knowledge/                    # 文档 / 分块 / Embedding
│   └── rag/                          # 【新增】Milvus 同步消费者
├── schemaplexai-quality              # 【拆分】质量与安全加护
│   ├── gate/ / deviation/ / review/ / security/ / audit/
├── schemaplexai-integration          # 【拆分】集成与工具生态
│   ├── git/ / jenkins/ / mcp/ / skill/ / plugin/
├── schemaplexai-ops                  # 【拆分】交付与运营
│   ├── artifact/ / notification/ / cost/ / budget/ / evaluation/
├── schemaplexai-task                 # 异步/调度层（增强）
│   ├── scheduling/                   # 【新增】归档任务 / 对账任务
│   └── mq/                           # 【新增】Milvus 同步 / 幂等消费者
├── schemaplexai-dao                  # 数据访问层
│   ├── mapper/ / tenant/
├── schemaplexai-model                # 模型层
│   └── entity/ / dto/ / vo/ / converter/
└── schemaplexai-common               # 公共组件
    └── enums/ / exception/ / utils/ / result/
```

### 服务间通信规范

```yaml
同步调用（查询类）:
  protocol: HTTP/REST
  client: OpenFeign + Spring Cloud LoadBalancer
  timeout: 5s
  circuit_breaker: enabled

异步调用（事件驱动）:
  protocol: AMQP (RabbitMQ)
  exchange_type: topic
  delivery_mode: persistent
  ack_mode: manual
  dead_letter: enabled
  
数据一致性:
  pattern: eventual_consistency
  compensation: saga_pattern
  outbox: enabled
```

---

## 三、修订版研发路线图

### Phase 0：基础设施与脚手架（第 1-3 周）【修订：+0.5 周】

**目标**：搭建可运行的工程骨架，含 Gateway、可观测性、CI/CD。

| 任务 | 交付物 | 工时 | 备注 |
|------|--------|------|------|
| 初始化 Maven 多模块工程（含 13 个模块） | 可编译工程骨架 | 2d | 含 gateway + 9 个业务服务 |
| 配置 Spring Boot 3 + Java 21 | 基础运行环境 | 1d | |
| **【新增】搭建 API Gateway** | `schemaplexai-gateway` 可运行 | 2d | JWT/租户/限流/路由/日志 |
| **【新增】搭建可观测性环境** | Prometheus + Grafana + ELK + Jaeger | 2d | Docker Compose 一键启动 |
| 集成 Spring Security + JWT | 认证过滤器 | 1d | 移至 Gateway 层 |
| 统一返回体 / 全局异常 | `Result<T>` | 1d | |
| 搭建 PG + Redis + RabbitMQ + MinIO + Milvus + CK + ES | `docker-compose.yml` | 1.5d | **新增 ES** |
| 集成 MyBatis-Plus + TenantLine | 基类 Mapper + 拦截器 | 2d | |
| 配置 Knife4j OpenAPI | API 文档页 | 0.5d | |
| 建立 Git 分支规范 + CI 流水线 | `.github/workflows/ci.yml` | 1d | |
| **里程碑** | **Gateway → Web → Service → DB 全链路通** | **~14d** | |

---

### Phase 1：系统治理核心（第 4-5 周）

| 任务 | 交付物 | 工时 | 备注 |
|------|--------|------|------|
| 租户 / 用户 / 角色 / 权限 / 菜单 | CRUD + API | 3d | |
| **【新增】模型供应商管理** | `sf_model_provider` + CRUD | 1.5d | 供应商增删改查 |
| AI 模型 / 模型组管理 | `sf_ai_model` / `sf_model_group` | 1.5d | |
| 模型路由与降级（含供应商冷却期） | `AIModelRouter` | 2d | **新增供应商维度** |
| 字典 / 国际化 / 系统配置 | `sf_dict` / `sf_i18n` / `sf_config` | 1d | |
| 审批中心基础框架 | `sf_approval` + 状态机 | 2d | |
| **里程碑** | **可创建租户 → 配置供应商 → 创建模型 → 登录** | **~11d** | |

---

### Phase 2：Agent 执行引擎 MVP（第 6-10 周）【修订：+1 周】

**目标**：状态机驱动的 Agentic Loop，支持中断/恢复、Token 预算、SSE。

| 任务 | 交付物 | 工时 | 备注 |
|------|--------|------|------|
| **【新增】LLM Provider 抽象层** | `LlmProvider` + `OpenAiProvider` + `AnthropicProvider` | 2d | **关键预研** |
| **【新增】LangChain4j 技术预研验证** | Tool Calling / Streaming / Memory 可行性报告 | 2d | **阻塞项** |
| Agent 配置模型 + CRUD | `sf_agent` / `sf_agent_config` | 2d | |
| **【修订】AgentStateMachine 状态机框架** | `AgentExecutionState` + `AgentStateHandler` | 3d | **核心重构** |
| 四层 System Prompt 注入 | `ContextInjector` | 2d | |
| Redis L1 + PG L2 双层记忆 | `CompositeChatMemoryStore` | 2d | |
| **【新增】记忆压缩策略** | `CompressionStrategy` + 触发器 | 2d | |
| **【新增】Token 预算管理** | `TokenBudget` + 预估/裁剪 | 2d | |
| Agentic Loop 状态机实现 | `ThinkingStateHandler` / `ToolCallingStateHandler` | 4d | |
| 内置工具注册框架 | `AgentToolSessionFactory` | 2d | |
| 工具并行读/串行写 | `AgentLoopToolHandler` | 2d | |
| SSE 执行事件流 | POST `/agents/{id}/execute` + SSE | 2d | **新增 PAUSED 事件** |
| **【新增】执行中断/恢复/取消** | `AgentExecutionLifecycleService` + 快照 | 3d | |
| **【新增】执行快照持久化** | `sf_agent_execution_snapshot` | 1d | |
| RabbitMQ `sf.agent.execute` 消费 | `AgentExecuteDispatcher` | 1d | |
| 执行记录持久化 | `sf_agent_execution` / `sf_agent_execution_log` | 2d | |
| **【修订】四维限流（+供应商）** | `ExecutionAdmissionService` | 1d | |
| **【修订】影子审核反馈闭环** | `FeedbackAction` + `AgentShadowConfig` | 2d | |
| **里程碑** | **API 创建 Agent → 执行 → SSE 流 → 暂停 → 恢复 → 完成** | **~35d** | |

---

### Phase 3：上下文与知识中心（第 11-12 周）【修订：+0.5 周】

| 任务 | 交付物 | 工时 | 备注 |
|------|--------|------|------|
| 工作空间/分支/Git工作区 | `sf_workspace*` | 2d | |
| 上下文快照与关系 | `sf_context_snapshot` | 2d | |
| **【新增】知识文档版本管理** | `sf_knowledge_doc_version` | 1d | |
| 知识文档上传 + MinIO | `sf_knowledge_doc` | 2d | |
| Apache Tika 集成 | 文本提取 | 1d | |
| 文本分块 + Embedding | 分块策略 + Embedding 服务 | 2d | |
| Milvus 向量存储 | Collection 设计 + 写入/检索 | 2d | |
| **【新增】PG→Milvus 同步消费者** | `MilvusSyncConsumer` + `sf.milvus.sync` 队列 | 2d | |
| **【新增】定时对账任务** | `MilvusPgReconciliationTask` | 1.5d | 每日凌晨执行 |
| RAG 检索 API | 带元数据过滤的向量检索 | 2d | |
| **里程碑** | **上传文档 → 向量化 → Agent 自动检索 → Milvus-PG 一致** | **~17.5d** | |

---

### Phase 4：Spec 规范中心（第 13-14 周）

| 任务 | 交付物 | 工时 | 备注 |
|------|--------|------|------|
| Spec 文档模型 | `sf_spec*` / `sf_spec_document` | 2d | |
| Spec CRUD + 版本管理 | 发布/回滚/对比 | 3d | |
| **【明确】Steering 文档** | `sf_spec_steering`（Spec 子类型） | 1d | |
| 模板管理 | `sf_spec_template` | 2d | |
| Tasks 拆解与追踪 | 层级结构 | 2d | |
| 评审/审批/变更追踪 | 状态机 + 历史 | 2d | |
| Spec 偏离检测（基础版） | 静态规则检测 | 2d | |
| **里程碑** | **定义 Spec → 绑定 Agent → 执行时读取作为意图标准** | **~14d** | |

---

### Phase 5：质量与安全加护（第 15-16 周）

| 任务 | 交付物 | 工时 | 备注 |
|------|--------|------|------|
| 质量门禁规则配置 | `sf_quality_gate` | 2d | |
| AgentLoopQualityChecker 集成 | artifact / grounding / sanitize | 3d | |
| 交叉评审机制 | `sf_review_record` | 2d | |
| 意图缺陷检测 | 基于 Spec 的合规判断 | 2d | |
| 安全策略与审计事件 | `sf_security_policy` / `sf_audit_event` | 2d | |
| 输出合规检查 | `SecurityRuntimeGuardService` | 2d | |
| 渐进式信任 | `ToolApprovalAmendmentService` | 2d | |
| **里程碑** | **执行自动过门禁 → 不符暂停/告警/记录** | **~15d** | |

---

### Phase 6：AI 工作流中心（第 17-19 周）

| 任务 | 交付物 | 工时 | 备注 |
|------|--------|------|------|
| Flowable 引擎集成 | `sf_workflow*` / Flowable 表 | 2d | |
| **【明确】Flowable 边界** | BPMN 状态机 vs AI 节点执行器 | 1d | 文档化边界 |
| 工作流模板设计器 | 节点类型定义 + JSON 存储 | 2d | |
| WorkflowNodeEngine 框架 | AI 节点执行器 | 3d | |
| 节点执行状态追踪 | `sf_workflow_node_execution` | 2d | |
| 工作流-Agent 集成节点 | 调用 `AgentExecutionEngine` | 2d | |
| 人工审批节点 | 与审批中心对接 | 1d | |
| **里程碑** | **创建模板 → 编排节点 → 触发执行 → 状态可视** | **~15d** | |

---

### Phase 7：集成与工具生态（第 20-21 周）

| 任务 | 交付物 | 工时 | 备注 |
|------|--------|------|------|
| GitHub/GitLab 集成 | OAuth + Webhook + Repo 操作 | 3d | |
| Jenkins/CI-CD 集成 | 构建触发 + 状态回调 | 2d | |
| MCP Server 管理 | `sf_mcp_server` + MCP 客户端 | 3d | |
| 内置工具扩展（Git Worktree） | 多租户安全 Git 工作区 | 2d | |
| Skill 注册与管理 | `sf_skill` + 动态加载 | 2d | |
| API Gateway 管理 | 外部 API 注册/调用/限流 | 2d | |
| 插件市场框架（基础版） | 上传/安装/启停 | 2d | |
| **里程碑** | **Agent 可调用 Git/Jenkins/MCP/API/Skill** | **~16d** | |

---

### Phase 8：交付与运营（第 22-23 周）

| 任务 | 交付物 | 工时 | 备注 |
|------|--------|------|------|
| 制品/版本/交付记录 | `sf_artifact*` + MinIO | 2d | |
| 站内信/邮件/IM 通知 | `sf_notification*` + 多渠道 | 2d | |
| 预算与 Token 成本采集 | Token 计数 + 成本计算 | 2d | |
| ClickHouse 成本库 | `sf_cost_record` / `sf_agent_metric` | 2d | |
| **【修订】ClickHouse 同步游标持久化** | `sf_sync_cursor` / `sf_sync_batch_log` | 2d | |
| **【新增】MQ 幂等性保障** | `sf_idempotency_key` + 拦截器 | 2d | |
| ClickHouseCostSyncService | 增量游标同步 + 批次校验 | 2d | |
| 日成本聚合视图 | `mv_daily_cost` | 1d | |
| 成本报表 API | 租户级明细 + 趋势 | 2d | |
| 评测数据集/任务/结果 | `sf_eval_dataset` / `sf_eval_task` | 2d | |
| **里程碑** | **成本报表 → 通知 → 制品下载** | **~19d** | |

---

### Phase 9：高级特性与优化（第 24-26 周）

| 任务 | 交付物 | 工时 | 备注 |
|------|--------|------|------|
| **【前置】LangGraph4j 可行性确认** | Team Agent 编排评估报告 | 2d | **阻塞项，不通过则跳过 Team Agent** |
| Team Agent 模型设计 | `sf_team_agent` / Lead + Sub-Agent | 2d | |
| LangGraph4j 编排（若通过） | Team Agent 状态图 | 3d | |
| 团队上下文共享 | `sf.agent.team.context` + Redis | 2d | |
| AgentChatMemoryCompactor | 上下文压缩策略 | 2d | |
| AgentLoopDetectionService | 哈希 + 工具序列循环检测 | 2d | |
| AgentLoopCompletionHandler 三级收敛 | 自然停止 / 工具收敛 / 强制截断 | 2d | |
| AgentLoopShadowReviewService | 异步影子审核队列 | 2d | |
| AgentMemoryExtractionService（Phase1） | 即时记忆提取 | 2d | |
| AgentMemoryConsolidationService（Phase2） | 定时记忆固化 | 2d | |
| **【新增】ChatMessage 归档任务** | `ChatMessageArchiveJob` | 1d | 每日归档 30 天前数据 |
| **【新增】OpenTelemetry 追踪集成** | TraceID 全链路传递 | 2d | |
| 执行准入四维限流 | 租户 + Agent + 模型 + 供应商 | 1d | |
| **里程碑** | **Team Agent 协作 → 循环检测 → 记忆固化 → 全链路追踪** | **~25d** | |

---

### Phase 10：系统测试与上线准备（第 27-28 周）

| 任务 | 交付物 | 工时 | 备注 |
|------|--------|------|------|
| 核心链路集成测试 | 测试用例 + 报告 | 3d | |
| **【新增】跨服务调用测试** | OpenFeign + Saga 补偿测试 | 2d | |
| Agent 引擎压力测试 | 并发/限流/降级压测 | 3d | |
| **【新增】Redis OOM 压测** | 长对话内存增长测试 | 1d | |
| **【新增】Milvus 并发检索压测** | 多租户并发向量检索 | 1d | |
| 多租户隔离安全审计 | 审计报告 | 2d | |
| **【新增】幂等性测试** | MQ 重复消费测试 | 1d | |
| RAG 检索质量评估 | 准确率/召回率测试 | 2d | |
| 成本核算准确性验证 | 对账报告 | 1d | |
| 部署文档 + 运维手册 | `DEPLOYMENT.md` / `OPERATIONS.md` | 2d | |
| 用户操作手册 | `USER_GUIDE.md` | 2d | |
| **里程碑** | **系统具备生产上线条件** | **~20d** | |

---

## 四、总体时间线（修订版）

```
周次:  01  02  03  04  05  06  07  08  09  10  11  12  13  14
      ├──P0──基础设施──┤├─P1─系统治理─┤├────P2──Agent MVP────┤├P3-上下文┤
                                                        ↑+1周
周次:  15  16  17  18  19  20  21  22  23  24  25  26  27  28
      ├──P4─Spec规范─┤├─P5─质量安全─┤├P6-工作流─┤├P7-集成生态┤
      
周次:  22  23  24  25  26  27  28  29  30
                  ├─P8─交付运营─┤├───P9─高级特性───┤├─P10-测试上线─┤
                                                      ↑缓冲
```

**总计工期**：约 **28 周（~7 个月）**  
**建议预留缓冲**：**+2 周** → **总控 30 周**

---

## 五、团队配置（修订）

| 角色 | 人数 | 职责 | 参与阶段 |
|------|------|------|----------|
| **架构师/技术负责人** | 1 | 技术决策、架构把控、Code Review | 全程 |
| **后端工程师（基础设施）** | 2 | Gateway、系统治理、可观测性、工作流 | P0-P1, P5-P6 |
| **后端工程师（Agent 引擎）** | 2 | 状态机、LangChain4j 预研、工具生态 | P2, P7, P9 |
| **后端工程师（数据/RAG）** | 1 | 上下文中心、Milvus 同步、向量检索 | P3, P4 |
| **后端工程师（集成/运营）** | 1 | 外部集成、成本分析、ClickHouse 同步 | P7-P8 |
| **DevOps 工程师** | 1 | CI/CD、K8s、监控告警、中间件运维 | P0, P10 |
| **测试工程师** | 1 | 集成测试、压测、安全审计、自动化 | P5, P10 |
| **产品经理** | 1 | 需求细化、原型设计、验收标准 | 全程 |
| **前端工程师** | 2 | Web 界面、可视化编排、工作台 | P1 起持续 |

**团队规模**：**11 人**（不变）

---

## 六、关键风险（修订版）

| 优先级 | 风险项 | 影响 | 缓解措施 |
|--------|--------|------|----------|
| **P0** | LangChain4j Tool Calling/Streaming 不满足需求 | Phase 2 阻塞 | **Phase 0 并行预研**；封装 Provider 抽象层；准备直接调用 SDK 的 fallback |
| **P0** | LangGraph4j Team Agent 不成熟 | Phase 9 大幅缩减 | 预研决定取舍；如不过则 Team Agent 降级为 "多个 Solo Agent 顺序调用" |
| **P0** | AgentStateMachine 状态转换 Bug | 核心链路不稳定 | 每个状态独立单元测试；状态转换矩阵 100% 覆盖；混沌测试 |
| **P1** | Token 预算预估不准 | 过早截断或超支 | 预留 20% 缓冲；实际消耗校准预估模型；超限时先压缩再终止 |
| **P1** | Milvus-PG 数据不一致 | RAG 检索到已删文档 | 消息队列同步 + 每日对账 + Milvus metadata 存 PG 主键 |
| **P1** | 服务拆分后网络延迟增加 | 跨服务调用慢 | OpenFeign 连接池优化；本地缓存（Caffeine）；非必要不拆分查询链路 |
| **P2** | ClickHouse 同步延迟 | 成本数据实时性差 | 游标持久化 + 批次校验 + 补同步机制；接受分钟级延迟 |

---

## 七、下一步行动（修订后）

1. **本周内**：评审确认修订版架构设计 + 项目计划
2. **第 1 周**：启动 Phase 0，**优先搭建 Gateway + 可观测性环境**
3. **同步启动（关键）**：
   - **LangChain4j 技术预研**（2 周内输出可行性报告，阻塞 Phase 2）
   - **LangGraph4j Team Agent 预研**（2 周内输出评估报告，影响 Phase 9）
4. **第 2 周**：完成 `AgentStateMachine` 原型验证（状态转换 + 暂停恢复）
5. **基础设施**：申请 ES 集群 + MinIO Lifecycle 配置权限 + 云资源预算确认

---

*本修订版项目计划基于 DESIGN_REVISED.md 架构修订内容制定。所有调整均以"降低核心链路风险、提升可观测性、保障数据一致性"为原则。*
