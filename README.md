# SchemaPlexAI — AI 研发协作平台

> 一个面向企业级 AI 研发的协作平台，覆盖需求定义(Spec) → 工作流编排 → Agent 智能执行 → 质量门禁 → 成本分析的全生命周期。

## 技术栈

| 层级 | 技术 |
|------|------|
| 基础框架 | Spring Boot 3.2 + Java 21 |
| 网关 | Spring Cloud Gateway |
| ORM | MyBatis-Plus 3.5 |
| 数据库 | PostgreSQL 16 + ClickHouse 24 |
| 缓存 | Redis 7 + Caffeine |
| 消息队列 | RabbitMQ 3.12 |
| 工作流 | Flowable 7 |
| 对象存储 | MinIO |
| 向量库 | Milvus 2.3 |
| AI 框架 | LangChain4j |
| 前端 | React 18 + TypeScript + Vite + Ant Design 5 |
| 可观测性 | Prometheus + Grafana + ELK + Jaeger |

## 工程结构

```
schemaplexai/
├── schemaplexai-gateway          # API 网关（统一入口 / 鉴权 / 限流）
├── schemaplexai-web              # Web 接入层（Controller / SSE / WebSocket）
├── schemaplexai-system           # 系统治理（租户 / 用户 / 角色 / 权限 / 模型）
├── schemaplexai-spec             # Spec 规范中心
├── schemaplexai-agent-config     # Agent 配置中心
├── schemaplexai-agent-engine     # Agent 执行引擎（核心）
├── schemaplexai-workflow         # AI 工作流中心
├── schemaplexai-context          # 上下文与知识中心（含 RAG）
├── schemaplexai-quality          # 质量与安全加护
├── schemaplexai-integration      # 集成与工具生态
├── schemaplexai-ops              # 交付与运营（成本 / 通知 / 制品）
├── schemaplexai-task             # 异步/调度层（MQ 消费者 / 定时任务）
├── schemaplexai-dao              # 数据访问层（MyBatis-Plus + 租户隔离）
├── schemaplexai-model            # 模型层（Entity / DTO / VO）
├── schemaplexai-common           # 公共组件
├── schemaplexai-admin            # 管理后台聚合
└── schemaplexai-ui               # 前端工程（React + TypeScript）
```

## 快速启动

### 1. 启动基础设施

```bash
cd docker
docker-compose up -d
```

将启动：PostgreSQL、Redis、RabbitMQ、MinIO、Milvus、ClickHouse、Elasticsearch、Prometheus、Grafana

### 2. 初始化数据库

```bash
# 数据库初始化脚本会自动执行
# docker/postgres/init/*.sql
```

### 3. 启动后端服务

```bash
# 编译整个项目
mvn clean compile

# 依次启动各服务（或使用 IDE 并行启动）
mvn spring-boot:run -pl schemaplexai-gateway
mvn spring-boot:run -pl schemaplexai-system
mvn spring-boot:run -pl schemaplexai-web
mvn spring-boot:run -pl schemaplexai-agent-config
mvn spring-boot:run -pl schemaplexai-agent-engine
mvn spring-boot:run -pl schemaplexai-context
mvn spring-boot:run -pl schemaplexai-spec
mvn spring-boot:run -pl schemaplexai-workflow
mvn spring-boot:run -pl schemaplexai-integration
mvn spring-boot:run -pl schemaplexai-ops
mvn spring-boot:run -pl schemaplexai-quality
mvn spring-boot:run -pl schemaplexai-task
```

### 4. 启动前端

```bash
cd schemaplexai-ui
npm install
npm run dev
```

### 5. 访问

| 服务 | 地址 |
|------|------|
| API Gateway | http://localhost:8080 |
| API 文档（Knife4j） | http://localhost:8082/doc.html |
| RabbitMQ 管理台 | http://localhost:15672 |
| Grafana | http://localhost:3000 |
| Prometheus | http://localhost:9090 |
| MinIO 控制台 | http://localhost:9001 |

## 核心特性

- **多租户全链路隔离**：从 Gateway 到 DAO 层的完整租户隔离
- **状态机驱动的 Agent 执行引擎**：显式状态转换，支持中断/恢复/取消
- **Token 预算管理**：执行级 Token 管控，防止成本失控
- **双层记忆体系**：Redis L1 + PostgreSQL L2，支持压缩与归档
- **RAG 知识增强**：Tika 解析 → 分块 → Embedding → Milvus 检索
- **影子审核反馈闭环**：异步审核 → 反馈动作 → 优化下次执行
- **可观测性**：Metrics + Logs + Traces 三位一体

## 开发规范

- 所有 Entity 继承 `BaseEntity`
- 所有 Mapper 继承 `BaseMapperX`
- REST API 统一返回 `Result<T>`
- 异常统一使用 `BaseException`
- 常量使用 `CommonConstants`

## 文档

- [架构设计文档](docs/design/DESIGN_REVISED.md)
- [项目计划](PROJECT_PLAN_REVISED.md)

## License

MIT
