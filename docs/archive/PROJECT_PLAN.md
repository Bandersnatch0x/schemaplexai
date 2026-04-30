# SchemaPlexAI — AI 研发协作平台 项目计划书

> **版本**：v1.0  
> **日期**：2026-04-29  
> **目标**：基于产品架构、系统架构、Agent 执行引擎及数据架构，制定可落地的研发路线图。

---

## 一、项目概述

| 项目属性 | 说明 |
|----------|------|
| **项目名称** | SchemaPlexAI（AI 研发协作平台） |
| **产品定位** | 面向企业级 AI 研发的协作平台，覆盖需求定义(Spec) → 工作流编排 → Agent 智能执行 → 质量门禁 → 成本分析的全生命周期 |
| **技术基座** | Spring Boot 3 + Java 21 + PostgreSQL + Redis + RabbitMQ |
| **目标用户** | 研发团队、AI 工程师、技术管理者 |
| **核心差异化** | 完整的 Agentic Loop 执行引擎、多租户全链路隔离、内置质量与安全加护、成本可控可分析 |

### 1.1 核心目标（OKR）

- **O1**：上线可稳定运行的 Agent 执行引擎，支持 Solo Agent 与 Team Agent（Lead+Sub-Agent）编排
- **O2**：建立完整的 Spec 规范中心，实现需求→设计→Tasks→评审的闭环
- **O3**：实现全链路多租户隔离，支持租户级成本核算与权限管控
- **O4**：构建可插拔的工具生态（内置工具 / Skill / MCP / API Gateway）

---

## 二、技术架构总览

### 2.1 工程模块结构

```
schemaplexai/
├── schemaplexai-web              # 接入层 / Web 层
│   ├── controller/               # REST API 控制器
│   ├── security/                 # JWT + Spring Security
│   ├── interceptor/              # TenantInterceptor / JwtAuthenticationFilter
│   └── websocket/                # WebSocket / SSE 事件流
├── schemaplexai-service          # 业务服务层
│   ├── spec/                     # Spec 规范中心
│   ├── agent/                    # Agent 管理中心 + 执行引擎
│   ├── workflow/                 # AI 工作流中心
│   ├── context/                  # 上下文与知识中心
│   ├── quality/                  # 质量与安全加护
│   ├── integration/              # 集成与工具生态
│   ├── notification/             # 通知运营
│   └── cost/                     # 成本与预算分析
├── schemaplexai-task             # 异步/调度层
│   ├── scheduling/               # 定时任务（成本/审批/恢复）
│   └── mq/                       # RabbitMQ Consumer
├── schemaplexai-dao              # 数据访问层
│   ├── mapper/                   # MyBatis-Plus Mapper
│   └── tenant/                   # TenantLine 租户隔离
├── schemaplexai-model            # 模型层
│   ├── entity/                   # 数据库实体
│   ├── dto/                      # 数据传输对象
│   ├── vo/                       # 视图对象
│   └── converter/                # 类型转换器
└── schemaplexai-common           # 公共组件
    ├── enums/                    # 枚举定义
    ├── exception/                # 异常体系
    ├── utils/                    # 工具类
    └── result/                   # 统一返回体
```

### 2.2 核心技术栈

| 层级 | 技术选型 | 版本建议 |
|------|----------|----------|
| **基础框架** | Spring Boot | 3.2.x |
| **JDK** | Java | 21 LTS |
| **安全** | Spring Security + JWT | 6.x |
| **ORM** | MyBatis-Plus | 3.5.x |
| **数据库** | PostgreSQL | 16.x |
| **缓存** | Redis | 7.x |
| **消息队列** | RabbitMQ | 3.12.x |
| **工作流引擎** | Flowable | 7.x |
| **对象存储** | MinIO | latest |
| **向量数据库** | Milvus | 2.3.x |
| **分析数仓** | ClickHouse | 24.x |
| **AI 框架** | LangChain4j / LangGraph4j | latest |
| **API 文档** | Knife4j (OpenAPI) | 4.x |
| **文档解析** | Apache Tika | 2.x |

---

## 三、模块拆解与依赖关系

### 3.1 模块依赖矩阵

```
                    ┌─────────────────────────────────────────────────────────────┐
                    │                     schemaplexai-web                         │
                    └───────────────────────┬─────────────────────────────────────┘
                                            │
        ┌───────────────────┬───────────────┼───────────────┬───────────────────┐
        │                   │               │               │                   │
┌───────▼───────┐  ┌────────▼───────┐ ┌────▼─────┐  ┌──────▼──────┐  ┌────────▼────────┐
│   spec 模块   │  │  agent 模块    │ │ workflow │  │  context    │  │  quality 模块   │
│               │  │                │ │  模块    │  │  模块       │  │                 │
└───────┬───────┘  └────────┬───────┘ └────┬─────┘  └──────┬──────┘  └────────┬────────┘
        │                   │               │               │                   │
        └───────────────────┴───────────────┼───────────────┴───────────────────┘
                                            │
                    ┌───────────────────────▼───────────────────────┐
                    │           schemaplexai-service (聚合层)        │
                    └───────────────────────┬───────────────────────┘
                                            │
        ┌───────────────────┬───────────────┼───────────────┬───────────────────┐
        │                   │               │               │                   │
┌───────▼───────┐  ┌────────▼───────┐ ┌────▼─────┐  ┌──────▼──────┐  ┌────────▼────────┐
│     dao       │  │     task       │ │  model   │  │   common    │  │  integration    │
│   (数据访问)   │  │   (异步调度)    │ │  (模型)  │  │   (公共)    │  │    (集成)       │
└───────────────┘  └────────────────┘ └──────────┘  └─────────────┘  └─────────────────┘
```

### 3.2 关键模块优先级

| 优先级 | 模块 | 理由 |
|--------|------|------|
| **P0** | 系统治理 + 多租户 | 所有模块的基础依赖，必须先有用户/租户/权限 |
| **P0** | Agent 执行引擎 | 平台核心价值，最复杂且风险最高，需尽早验证 |
| **P1** | Spec 规范中心 | 上游输入，Agent 执行的"意图与验收标准"来源 |
| **P1** | 上下文与知识中心 | Agent 执行的关键依赖（System Prompt 注入） |
| **P1** | 集成与工具生态 | Agent 执行的外部能力依赖（Git/Jenkins/MCP） |
| **P2** | AI 工作流中心 | 建立在 Agent 和 Spec 之上的编排层 |
| **P2** | 质量与安全加护 | 生产级必须，但可在核心链路稳定后增强 |
| **P2** | 交付与运营 | 成本/通知/制品管理，商业化必备 |

---

## 四、研发路线图（Roadmap）

### Phase 0：基础设施与脚手架（第 1-2 周）

**目标**：搭建可运行的工程骨架，建立 CI/CD 与开发规范。

| 任务 | 交付物 | 负责人 | 工时 |
|------|--------|--------|------|
| 初始化 Maven/Gradle 多模块工程 | 可编译的工程骨架 | 架构组 | 2d |
| 配置 Spring Boot 3 + Java 21 环境 | 基础运行环境 | 架构组 | 1d |
| 集成 Spring Security + JWT 认证 | 认证过滤器 + Token 生成/校验 | 架构组 | 2d |
| 设计并实现统一返回体 / 全局异常处理 | `Result<T>` + 异常枚举 | 架构组 | 1d |
| 搭建 PostgreSQL + Redis + RabbitMQ 本地开发环境 | `docker-compose.yml` | 架构组 | 1d |
| 集成 MyBatis-Plus + TenantLine 租户隔离 | 基类 Mapper + 拦截器 | 架构组 | 2d |
| 配置 Knife4j OpenAPI 文档 | 可访问的 API 文档页 | 架构组 | 0.5d |
| 建立 Git 分支规范 + CI 流水线 | `.github/workflows/ci.yml` | DevOps | 1d |
| **里程碑** | **工程可编译、数据库可连接、API 文档可访问** | | **~10d** |

---

### Phase 1：系统治理核心（第 3-4 周）

**目标**：建立多租户体系、用户权限体系、AI 模型管理。

| 任务 | 交付物 | 依赖 | 工时 |
|------|--------|------|------|
| 租户管理 CRUD | `sf_tenant` 表 + API | Phase 0 | 2d |
| 用户/角色/权限 RBAC | `sf_user` / `sf_role` / `sf_permission` + 菜单树 | Phase 0 | 3d |
| JWT 中嵌入租户信息 + `TenantInterceptor` | 多租户请求隔离 | Phase 0 | 1d |
| AI 模型/模型组管理 | `sf_ai_model` / `sf_model_group` + 增删改查 | Phase 0 | 2d |
| 模型路由与降级策略配置 | `AIModelRouter` 基础版（路由链 + 简单降级） | Phase 0 | 2d |
| 字典/国际化/系统配置 | `sf_dict` / `sf_i18n` / `sf_config` | Phase 0 | 1d |
| 审批中心基础框架 | `sf_approval` 表 + 审批流状态机 | Phase 0 | 2d |
| **里程碑** | **可创建租户 → 创建用户 → 分配角色 → 登录 → 看到菜单** | | **~13d** |

---

### Phase 2：Agent 执行引擎 MVP（第 5-8 周）

**目标**：实现最核心的 Agentic Loop，支持 Solo Agent 执行、工具调用、SSE 事件流。

| 任务 | 交付物 | 依赖 | 工时 |
|------|--------|------|------|
| Agent 配置模型设计 | `sf_agent` / `sf_agent_config` 表结构 | Phase 1 | 2d |
| Agent CRUD API | Agent 创建/编辑/删除/查询 | Phase 1 | 2d |
| LangChain4j 集成与封装 | `AgentModelInvoker`（统一模型调用入口） | Phase 1 | 3d |
| 四层 System Prompt 注入（ContextInjector） | Spec + 团队上下文 + 知识 + 历史记忆注入 | Phase 1 | 3d |
| Redis L1 ChatMemory 实现 | `CompositeChatMemoryStore` Redis 部分 | Phase 0 | 2d |
| PostgreSQL L2 ChatMemory 实现 | `sf_chat_message` 持久化 | Phase 0 | 2d |
| Agentic Loop 主循环（runAgenticLoop） | 基础轮次循环 + 工具调用解析 | ContextInjector | 4d |
| 内置工具注册与执行框架 | `AgentToolSessionFactory` + 工具发现机制 | Phase 1 | 3d |
| 工具调用并行读/串行写（AgentLoopToolHandler） | 读工具并行执行 + 写工具串行执行 | Agentic Loop | 2d |
| SSE 执行事件流（AgentController） | POST `/agents/{id}/execute` + SSE 推送 | Phase 0 | 2d |
| RabbitMQ `sf.agent.execute` 队列消费 | `AgentExecuteDispatcher` | Phase 0 | 1d |
| 执行记录持久化 | `sf_agent_execution` / `sf_agent_execution_log` | Phase 0 | 2d |
| **里程碑** | **可通过 API 创建 Agent → 配置 Prompt → 执行 → 看到 SSE 事件流 → 工具被调用 → 结果返回** | | **~28d** |

---

### Phase 3：上下文与知识中心（第 9-10 周）

**目标**：构建上下文库、RAG 流水线、知识文档管理。

| 任务 | 交付物 | 依赖 | 工时 |
|------|--------|------|------|
| 工作空间/分支/Git工作区模型 | `sf_workspace` / `sf_context` / `sf_context_item` | Phase 0 | 2d |
| 上下文快照与关系管理 | `sf_context_snapshot` + 版本对比 | 工作空间 | 2d |
| 知识文档上传与管理 | `sf_knowledge_doc` + MinIO 存储 | Phase 0 | 2d |
| Apache Tika 文档解析集成 | PDF/Word/Markdown 文本提取 | 知识文档 | 1d |
| 文本分块策略实现 | 固定长度 / 语义分块 | Tika | 2d |
| Embedding 服务封装 | OpenAI 兼容 / 本地 Embedding 模型 | Phase 0 | 2d |
| Milvus 向量存储集成 | Collection 设计 + 向量写入/检索 | Embedding | 2d |
| RAG 检索 API（tenant/context/document/item 过滤） | 带元数据过滤的向量检索 | Milvus | 2d |
| **里程碑** | **可上传文档 → 自动解析分块向量化 → Agent 执行时自动检索相关知识注入 Prompt** | | **~15d** |

---

### Phase 4：Spec 规范中心（第 11-12 周）

**目标**：建立需求→设计→Tasks 文档体系，支持版本、模板、评审。

| 任务 | 交付物 | 依赖 | 工时 |
|------|--------|------|------|
| Spec 文档模型设计 | `sf_spec` / `sf_spec_document` / `sf_spec_version` | Phase 0 | 2d |
| Spec 文档 CRUD + 版本管理 | 创建/编辑/发布/回滚/对比 | Phase 0 | 3d |
| 模板管理 | `sf_spec_template` + 从模板创建 Spec | Spec CRUD | 2d |
| Tasks 拆解与追踪 | 需求 → 设计 → Tasks 层级结构 | Spec CRUD | 2d |
| 评审/审批/变更追踪工作流 | 审批状态机 + 变更历史 | Phase 1 审批中心 | 2d |
| Steering 文档支持 | 方向性指导文档类型 | Spec CRUD | 1d |
| Spec 偏离检测（基础版） | 静态规则检测 Spec 与执行的偏离 | Agent 执行 | 2d |
| **里程碑** | **可在平台定义 Spec → 绑定到 Agent → Agent 执行时读取 Spec 作为意图标准** | | **~14d** |

---

### Phase 5：质量与安全加护（第 13-14 周）

**目标**：建立质量门禁、安全策略、输出合规检查。

| 任务 | 交付物 | 依赖 | 工时 |
|------|--------|------|------|
| 质量门禁规则配置 | `sf_quality_gate` + 规则包管理 | Phase 2 | 2d |
| AgentLoopQualityChecker 集成 | artifact / grounding / sanitize 检查 | Agentic Loop | 3d |
| 交叉评审机制 | `sf_review_record` + 评审分配算法 | Phase 4 Spec | 2d |
| 意图缺陷检测 | 基于 Spec 的执行结果合规判断 | Spec 偏离检测 | 2d |
| 安全策略与审计事件 | `sf_security_policy` / `sf_audit_event` | Phase 1 | 2d |
| 输出合规检查（SecurityRuntimeGuardService） | 敏感信息/违规内容拦截 | Agentic Loop | 2d |
| 渐进式信任（ToolApprovalAmendmentService） | 工具调用次数阈值 → 自动审批 | 工具框架 | 2d |
| **里程碑** | **Agent 执行时自动通过质量门禁 → 不符合则暂停/告警/记录问题** | | **~15d** |

---

### Phase 6：AI 工作流中心（第 15-17 周）

**目标**：支持工作流模板、节点编排、Flowable 运行时。

| 任务 | 交付物 | 依赖 | 工时 |
|------|--------|------|------|
| Flowable 引擎集成 | `sf_workflow_template` / `sf_workflow_instance` | Phase 0 | 2d |
| 工作流模板设计器 | 节点类型定义 + 可视化 JSON 存储 | Flowable | 2d |
| WorkflowNodeEngine 节点执行框架 | 触发/文档/Agent/审批/质量/通知/制品节点 | Phase 2 Agent | 3d |
| 工作流实例触发器 | 定时/事件/手动触发 + `sf.workflow.trigger` 队列 | RabbitMQ | 2d |
| 节点执行状态追踪 | `sf_workflow_node_execution` + 状态流转 | WorkflowNodeEngine | 2d |
| 工作流与 Agent 的集成节点 | Agent 节点调用 `AgentExecutionEngine` | Phase 2 | 2d |
| 人工审批节点集成 | 与 Phase 1 审批中心对接 | Phase 1 | 1d |
| **里程碑** | **可创建工作流模板 → 编排节点 → 触发执行 → 全流程状态可视** | | **~14d** |

---

### Phase 7：集成与工具生态（第 18-19 周）

**目标**：实现外部系统集成、MCP Server、插件市场。

| 任务 | 交付物 | 依赖 | 工时 |
|------|--------|------|------|
| GitHub/GitLab 集成 | OAuth + Webhook + Repo 操作 API | Phase 0 | 3d |
| Jenkins/CI-CD 集成 | 构建触发 + 状态回调 | Phase 0 | 2d |
| MCP Server 管理 | `sf_mcp_server` + MCP 协议客户端 | Phase 2 | 3d |
| 内置工具扩展（Git Worktree 多用户隔离） | 多租户安全的 Git 工作区 | Git 集成 | 2d |
| Skill 注册与管理 | `sf_skill` + 动态加载机制 | Phase 2 | 2d |
| API Gateway 管理 | 外部 API 注册/调用/限流 | Phase 0 | 2d |
| 插件市场框架（基础版） | 插件上传/安装/启停接口 | Phase 0 | 2d |
| **里程碑** | **Agent 可调用的工具覆盖 Git/Jenkins/MCP/自定义 API/Skill** | | **~16d** |

---

### Phase 8：交付与运营（第 20-21 周）

**目标**：成本分析、通知体系、制品管理、评测体系。

| 任务 | 交付物 | 依赖 | 工时 |
|------|--------|------|------|
| 制品/版本/交付记录 | `sf_artifact` / `sf_delivery_record` + MinIO | Phase 0 | 2d |
| 站内信/邮件/IM 通知通道 | `sf_notification` + 多渠道适配器 | RabbitMQ | 2d |
| 预算与 Token 成本采集 | 执行时 Token 计数 + 成本计算 | Agent 执行 | 2d |
| ClickHouse 成本库设计 | `sf_cost_record` / `sf_agent_metric` | Phase 0 | 2d |
| ClickHouseCostSyncService | 增量游标同步 PG → ClickHouse | ClickHouse | 2d |
| 日成本聚合视图（mv_daily_cost） | ClickHouse Materialized View | 成本库 | 1d |
| 成本报表 API | 租户级成本明细 + 趋势分析 | 同步服务 | 2d |
| 评测数据集/任务/结果 | `sf_eval_dataset` / `sf_eval_task` | Phase 2 | 2d |
| **里程碑** | **可查看租户成本报表 → 收到审批/执行通知 → 下载交付制品** | | **~15d** |

---

### Phase 9：高级特性与优化（第 22-24 周）

**目标**：Team Agent 编排、记忆体系深化、影子审核、系统优化。

| 任务 | 交付物 | 依赖 | 工时 |
|------|--------|------|------|
| Team Agent 模型设计 | `sf_team_agent` / Lead + Sub-Agent 关系 | Phase 2 | 2d |
| LangGraph4j 编排集成 | Team Agent 状态图编排 | Team Agent | 3d |
| `sf.agent.team.context` 团队上下文共享 | Redis 团队上下文广播 | RabbitMQ | 2d |
| AgentChatMemoryCompactor | 上下文压缩策略（摘要/滑动窗口） | ChatMemory | 2d |
| AgentLoopDetectionService | 哈希循环 + 工具序列循环检测 | Agentic Loop | 2d |
| AgentLoopCompletionHandler 三级收敛 | 自然停止 / 工具收敛 / 强制截断 | Agentic Loop | 2d |
| AgentLoopShadowReviewService | 异步影子审核队列 + 结果反馈 | Agentic Loop | 2d |
| AgentMemoryExtractionService（Phase1） | 执行完成时即时记忆提取 | ChatMemory | 2d |
| AgentMemoryConsolidationService（Phase2） | 定时任务记忆固化 | 提取服务 | 2d |
| AgentTraceService 分布式追踪 | TraceID 传递 + 链路日志 | Agent 执行 | 2d |
| 执行准入三维限流 | 租户 + Agent + 模型级 Semaphore | Phase 1 | 2d |
| **里程碑** | **支持 Team Agent 多角色协作 → 循环检测 → 记忆自动提取固化 → 全链路可追踪** | | **~23d** |

---

### Phase 10：系统测试与上线准备（第 25-26 周）

**目标**：集成测试、性能压测、安全审计、文档完善。

| 任务 | 交付物 | 工时 |
|------|--------|------|
| 核心链路集成测试（Spec → Workflow → Agent → Tool → Quality） | 测试用例 + 报告 | 3d |
| Agent 执行引擎压力测试（并发/限流/降级） | 压测报告 + 优化项 | 3d |
| 多租户隔离安全审计 | 审计报告 | 2d |
| RAG 检索质量评估 | 准确率/召回率测试 | 2d |
| 成本核算准确性验证 | 对账报告 | 1d |
| 部署文档 + 运维手册 | `DEPLOYMENT.md` / `OPERATIONS.md` | 2d |
| 用户操作手册 | `USER_GUIDE.md` | 2d |
| **里程碑** | **系统具备生产上线条件** | **~15d** |

---

## 五、总体时间线

```
周次:  01  02  03  04  05  06  07  08  09  10  11  12  13  14
      ├─P0─基础设施─┤├─P1─系统治理─┤├──P2──Agent MVP──┤├P3-上下文┤
      
周次:  15  16  17  18  19  20  21  22  23  24  25  26
      ├──P4─Spec规范─┤├─P5─质量安全─┤├P6-工作流┤├P7-集成生态┤
      
周次:  20  21  22  23  24  25  26
                  ├─P8─交付运营─┤├───P9─高级特性───┤├P10-测试上线┤
```

**总计工期**：约 **26 周（~6 个月）**

---

## 六、团队配置建议

| 角色 | 人数 | 职责 | 参与阶段 |
|------|------|------|----------|
| **架构师/技术负责人** | 1 | 技术决策、架构把控、核心代码评审 | 全程 |
| **后端工程师（基础设施）** | 2 | 系统治理、多租户、安全、工作流 | P0-P1, P5-P6 |
| **后端工程师（Agent 引擎）** | 2 | Agent 执行引擎、LangChain4j、工具生态 | P2, P7, P9 |
| **后端工程师（数据/RAG）** | 1 | 上下文中心、RAG 流水线、向量检索 | P3, P4 |
| **后端工程师（集成/运营）** | 1 | 外部集成、成本分析、通知、制品 | P7-P8 |
| **DevOps 工程师** | 1 | CI/CD、K8s 部署、监控告警、中间件运维 | P0, P10 |
| **测试工程师** | 1 | 测试用例、自动化测试、性能压测 | P5, P10 |
| **产品经理** | 1 | 需求细化、原型设计、验收标准 | 全程 |
| **前端工程师** | 2 | Web 界面、可视化编排、工作台 | P1 起持续 |

**建议团队规模**：后端 6 人 + 前端 2 人 + DevOps 1 人 + 测试 1 人 + 产品 1 人 ≈ **11 人**

---

## 七、关键风险与应对

| 风险 | 影响 | 概率 | 应对策略 |
|------|------|------|----------|
| **LangChain4j/LangGraph4j 成熟度不足** | Agent 引擎核心功能受阻 | 中 | 预留 1 周缓冲；准备直接调用 LLM API 的 fallback 方案 |
| **LLM 模型响应不稳定/降级逻辑复杂** | 执行成功率低 | 高 | P1 阶段优先完成模型路由与降级；集成多个供应商 |
| **多租户隔离漏洞** | 数据安全问题 | 中 | P1 完成严格单元测试 + 安全审计；DAO 层强制 TenantLine |
| **RAG 检索质量不达标** | Agent 回答质量差 | 中 | P3 预留调优时间；建立评测数据集持续优化分块/Embedding 策略 |
| **Team Agent 编排复杂度高** | P9 延期 | 高 | P2 先确保 Solo Agent 稳定；Team Agent 采用迭代方式 |
| **性能瓶颈（SSE 并发/向量检索）** | 用户体验差 | 中 | P10 进行专项压测；Redis 缓存热点数据；Milvus 集群化 |
| **ClickHouse 同步延迟** | 成本数据不准 | 低 | 设计增量游标 + 对账机制；预留补同步能力 |

---

## 八、技术决策记录（ADR）

| 编号 | 决策 | 理由 | 替代方案 |
|------|------|------|----------|
| ADR-001 | Java 21 而非 Java 17 | 虚拟线程提升并发处理能力，Agent 执行有大量 I/O | Java 17 LTS |
| ADR-002 | PostgreSQL 主库 + ClickHouse 分析库 | OLTP 与分析负载分离，成本数据量大数据结构简单 | 单 PG + 分区表 |
| ADR-003 | Redis L1 + PG L2 双层记忆 | 平衡查询性能与持久化可靠性 | 全 Redis / 全 PG |
| ADR-004 | RabbitMQ 而非 Kafka | 消息量级适中，RabbitMQ 路由灵活，运维简单 | Apache Kafka |
| ADR-005 | Milvus 而非 pgvector | 大规模向量检索性能更优，与 PG 解耦 | pgvector / Weaviate |
| ADR-006 | Flowable 自研 WorkflowNodeEngine 双轨 | Flowable 处理审批等标准 BPMN，自研引擎处理 AI 专属节点 | 纯自研 / 纯 Flowable |

---

## 九、验收标准（Definition of Done）

每个 Phase 需满足：

1. **代码**：通过 Code Review，单元测试覆盖率 ≥ 60%（核心模块 ≥ 80%）
2. **接口**：API 文档（Knife4j）完整，包含请求/响应示例
3. **数据**：数据库迁移脚本（Flyway/Liquibase）可重复执行
4. **测试**：核心链路有集成测试用例，无 P0/P1 级别 Bug
5. **部署**：可通过 `docker-compose` 一键启动完整环境
6. **文档**：模块设计文档更新至 `docs/` 目录

---

## 十、下一步行动（Next Steps）

1. **本周内**：评审并确认本项目计划，确定团队人员到位时间
2. **第 1 周**：启动 Phase 0，搭建工程脚手架，配置开发环境
3. **同步启动**：前端技术选型与 UI 设计规范制定
4. **基础设施准备**：申请云资源（PG/Redis/RabbitMQ/MinIO/Milvus/ClickHouse 测试环境）
5. **模型供应商对接**：确认 OpenAI / Anthropic / Gemini / 本地模型 的 API 接入方式

---

*本计划基于 SchemaPlexAI 架构设计文档制定，将在每个 Phase 结束时进行回顾（Retrospective）并根据实际情况调整。*
