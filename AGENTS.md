# SchemaPlexAI — Agent 开发指南

## 项目概述

SchemaPlexAI 是一个面向企业级 AI 研发的协作平台，覆盖需求定义(Spec) → 工作流编排 → Agent 智能执行 → 质量门禁 → 成本分析的全生命周期。

本项目采用多模块 Maven 工程结构，后端基于 Spring Boot 3.2 + Java 21，前端基于 React 18 + TypeScript + Vite。各业务域按微服务拆分，通过 API Gateway 统一接入，使用 RabbitMQ 进行跨服务异步通信。

**当前状态说明**：项目已完成基础脚手架搭建（Gateway 路由、认证鉴权、全局过滤器、公共基类、数据库初始化脚本、前端框架），各业务模块已大量填充；`schemaplexai-admin` 模块已实现（6 个管理服务 + 111 个测试，截至 2026-08-30 实测口径，详见下文）。全部 16 个 Maven 模块均已建立测试套件。Spec 合规修复批（来源：`docs/reviews/spec-compliance-2026-08-29/REPORT.md`）正在进行中，逐票完成状态以 `.scratch/spec-compliance-fix/issues/REGISTRY.md` 台账为准；本文档中的数字类内容均为**截至 2026-08-30 的实测/台账口径**，修复批推进后请以最新运行产物为准。

---

## 技术基座

| 层级 | 技术 |
|------|------|
| 基础框架 | Spring Boot 3.2.5 + Java 21 |
| 网关 | Spring Cloud Gateway 4.1.2 |
| ORM | MyBatis-Plus 3.5.5 |
| 数据库 | PostgreSQL 16 (OLTP) + ClickHouse 24 (分析) |
| 缓存 | Redis 7 + Caffeine 3.1.8 |
| 消息队列 | RabbitMQ 3.12 |
| 工作流引擎 | Flowable 7.0.0 |
| 对象存储 | MinIO 8.5.7 |
| 向量库 | Milvus 2.3.5 |
| AI 框架 | LangChain4j 0.31.0 |
| 文档解析 | Apache Tika 2.9.1 |
| 前端 | React 18.3 + TypeScript 5.5 + Vite + Ant Design 5 |
| 前端状态管理 | Zustand 4.5.4 |
| 前端路由 | React Router DOM 6.26 |
| API 文档 | Knife4j 4.4.0 (OpenAPI 3) |
| 可观测性 | Micrometer + Prometheus + Grafana + ELK + Jaeger |

---

## 工程模块

```
schemaplexai/
├── schemaplexai-gateway          # API 网关（端口 8080）— 路由 / JWT 校验 / 租户解析 / 限流
├── schemaplexai-web              # Web 接入层（端口 8082）— Controller / SSE / WebSocket / Knife4j 文档
├── schemaplexai-system           # 系统治理（端口 8081）— 租户 / 用户 / 角色 / 权限 / 模型管理
├── schemaplexai-spec             # Spec 规范中心（端口 8086）
├── schemaplexai-agent-config     # Agent 配置中心（端口 8083）
├── schemaplexai-agent-engine     # Agent 执行引擎（端口 8084）— 核心，对接 LLM
├── schemaplexai-workflow         # AI 工作流中心（端口 8087）— Flowable 驱动
├── schemaplexai-context          # 上下文与知识中心（端口 8085）— RAG / Milvus / MinIO / Tika
├── schemaplexai-quality          # 质量与安全加护（端口 8090）
├── schemaplexai-integration      # 集成与工具生态（端口 8088）
├── schemaplexai-ops              # 交付与运营（端口 8089）— ClickHouse 分析 / 定时任务
├── schemaplexai-task             # 异步/调度层（端口 8091）— MQ 消费 / 定时任务
├── schemaplexai-dao              # 数据访问层（MyBatis-Plus + 租户隔离）
├── schemaplexai-model            # 模型层（Entity / DTO / VO / 枚举）
├── schemaplexai-common           # 公共组件（异常 / 常量 / 结果封装 / 工具类）
├── schemaplexai-admin            # 管理后台聚合（端口 8092）— 租户/用户/角色/审计日志/系统配置/平台健康管理
└── schemaplexai-ui               # 前端工程（端口 3000）
```

### 模块依赖关系

- `schemaplexai-common`：无内部依赖，被所有模块引用
- `schemaplexai-model`：依赖 `common`
- `schemaplexai-dao`：依赖 `model`、`common`
- 所有**业务服务**（gateway 除外）均依赖 `common`、`model`、`dao`
- `gateway` 不依赖 `dao` 和 `model`，仅依赖 `common` 及部分 Web/Security 组件

---

## 开发规范

### 包名规范

| 模块 | 根包名 |
|------|--------|
| 网关 | `com.schemaplexai.gateway` |
| Web | `com.schemaplexai.web` |
| 系统治理 | `com.schemaplexai.system` |
| Agent 配置 | `com.schemaplexai.agent.config` |
| Agent 引擎 | `com.schemaplexai.agent.engine` |
| 工作流 | `com.schemaplexai.workflow` |
| 上下文 | `com.schemaplexai.context` |
| Spec | `com.schemaplexai.spec` |
| 质量 | `com.schemaplexai.quality` |
| 集成 | `com.schemaplexai.integration` |
| 运营 | `com.schemaplexai.ops` |
| 任务调度 | `com.schemaplexai.task` |

### 代码分层规范

1. **Entity**：必须继承 `BaseEntity`，使用 `@TableName` 注解；字段 `tenant_id` 必须存在
2. **Mapper**：必须继承 `BaseMapperX<T>`（位于 `schemaplexai-dao`）
3. **Service**：接口继承 `IService<T>`，实现类继承 `ServiceImpl<Mapper, T>`
4. **Controller**：返回统一使用 `Result<T>`，使用 `@RequiredArgsConstructor` 注入依赖
5. **异常**：统一使用 `BaseException`
6. **常量**：使用 `CommonConstants`
7. **租户隔离**：DAO 层已配置 `TenantLineInterceptor`，自动过滤租户数据

### API 规范

- RESTful 风格
- 统一返回体：`{"code": 200, "message": "success", "data": {}, "timestamp": 1714368000000}`
- 分页请求参数：`current`（默认 1）、`size`（默认 10）
- 分页返回体：`{"records": [], "total": 0, "current": 1, "size": 10, "pages": 0}`

### 关键公共类位置

| 类 | 所在模块 | 说明 |
|----|---------|------|
| `BaseEntity` | `schemaplexai-model` | 含 `id`(ASSIGN_ID)、`tenantId`、`createdAt`、`updatedAt`、`createdBy`、`updatedBy`、`deleted`(逻辑删除) |
| `BaseMapperX<T>` | `schemaplexai-dao` | 继承 MyBatis-Plus `BaseMapper<T>` 的占位基类 |
| `BaseException` | `schemaplexai-common` | 带错误码的运行时异常 |
| `CommonConstants` | `schemaplexai-common` | 租户头 `X-Tenant-Id`、JWT/Redis Key 前缀、MQ Routing Key、默认 Agent 限制 |
| `Result<T>` | `schemaplexai-common` | 统一 REST 响应封装 |
| `ResultCode` | `schemaplexai-common` | 错误码枚举（HTTP 码 + 业务码 1001–9001） |

---

## 构建与运行

### 后端构建

```bash
# 编译整个项目（不执行测试）
mvn clean compile -DskipTests

# 打包
mvn clean package -DskipTests

# 启动单个服务（示例）
mvn spring-boot:run -pl schemaplexai-gateway
mvn spring-boot:run -pl schemaplexai-system
```

### 前端构建

```bash
cd schemaplexai-ui
npm install
npm run dev      # 开发服务器（端口 3000）
npm run build    # 生产构建（输出到 dist/）
npm run lint     # ESLint 检查
```

前端 Vite 配置已代理 `/api` 到 `http://localhost:8080`（可通过环境变量 `VITE_API_BASE_URL` 覆盖）。

### 启动顺序

1. **基础设施**：`cd docker && docker-compose up -d`
   - 启动 PostgreSQL、Redis、RabbitMQ、MinIO、Milvus、ClickHouse、Elasticsearch、Prometheus、Grafana
2. **后端服务**：
   - 先启动 `schemaplexai-system`（其他服务依赖认证）
   - 再启动 `schemaplexai-gateway`
   - 其余服务可并行启动
3. **前端**：`cd schemaplexai-ui && npm run dev`

### 访问地址

| 服务 | 地址 |
|------|------|
| API Gateway | http://localhost:8080 |
| API 文档（Knife4j） | http://localhost:8082/doc.html |
| RabbitMQ 管理台 | http://localhost:15672 |
| Grafana | http://localhost:3000 |
| Prometheus | http://localhost:9090 |
| MinIO 控制台 | http://localhost:9001 |
| Jaeger UI | http://localhost:16686 |

---

## 测试策略

### 当前状态（截至 2026-08-30 实测/台账口径）

**全部 16 个 Maven 模块均已建立测试套件**，`pom.xml` 已引入 `spring-boot-starter-test`（16 模块）与 Testcontainers 依赖。以下为各模块实测测试数（聚合自各模块 `target/surefire-reports`，本 worktree 运行时间 2026-08-30 00:09–01:26；统计日期 2026-08-30）：

| 模块 | 实测测试数 | 失败/错误 | 跳过 |
|------|-----------|----------|------|
| schemaplexai-agent-engine | 1,874 | 0 / 0 | 4（Docker 容器沙箱类） |
| schemaplexai-integration | 300 | 0 / 0 | 0 |
| schemaplexai-quality | 279 | 0 / 0 | 0 |
| schemaplexai-ops | 269 | 0 / 0 | 0 |
| schemaplexai-system | 174 | 0 / 0 | 0 |
| schemaplexai-spec | 150 | 0 / 0 | 0 |
| schemaplexai-web | 147 | 0 / 0 | 0 |
| schemaplexai-common | 129 | 0 / 0 | 0 |
| schemaplexai-admin | 111 | 0 / 0 | 0 |
| schemaplexai-model | 27 | 0 / 0 | 0 |
| schemaplexai-dao | 20 | 0 / 0 | 0 |
| schemaplexai-task | 15 | 0 / 0 | 0 |
| schemaplexai-gateway | 86 | 0 / 0 | 0 |
| **小计（13 模块，2026-08-30 运行）** | **3,581** | **0 / 0** | **4** |

- `schemaplexai-agent-config`（103）、`schemaplexai-context`（242）、`schemaplexai-workflow`（210）：本次运行未覆盖（本 worktree 无 2026-08-30 运行产物），最近一次记录运行（2026-07-02）均为全绿；按台账口径计入后 16 模块合计约 **4,136**。
- 前端：`schemaplexai-ui` 已配置 Vitest + React Testing Library（22 个 `*.test.*` 文件、146 个单元用例，静态计数口径）与 Playwright（`e2e/` 下 3 个 spec）；`vitest.config.ts` 仍保留 `passWithNoTests: true`。
- 修复批在途，测试数随并行工作持续变化；引用时请以最新 `target/surefire-reports` 为准。

### CI 中的测试环节

GitHub Actions 工作流 `.github/workflows/ci.yml` 的 `backend` job 执行 `mvn test`（全模块）并上传 Surefire 测试报告与 JaCoCo 覆盖率报告；`frontend` job 执行 `npm run lint`、`npm test -- --run`、`npx tsc --noEmit` 与 Playwright e2e smoke（`e2e/smoke.spec.ts`）。

### 建议补充的测试体系

- **后端单元测试**：JUnit 5 + Mockito + AssertJ（`spring-boot-starter-test` 已就位，16 模块均有测试）
- **后端集成测试**：`@SpringBootTest` + Testcontainers（PostgreSQL、Redis、RabbitMQ）——Testcontainers 依赖已声明，集成覆盖面待扩展
- **前端单元测试**：Vitest + React Testing Library（已就位）
- **前端 E2E**：Playwright（已就位，`e2e/` 下 smoke/screenshots/figma-regression 三个 spec）

---

## CI/CD

仓库根目录 `.github/workflows/ci.yml` 定义了以下任务：

| Job | 说明 |
|-----|------|
| `security-trivy` | Trivy 漏洞扫描（repo 模式，SARIF 上传） |
| `security-dependency-check` | OWASP Dependency-Check 依赖漏洞扫描 |
| `security-gitleaks` | Gitleaks 密钥泄露扫描 |
| `backend` | 依赖三条安全轨 → JDK 21 → `mvn compile` → `mvn test`（全模块）→ `mvn jacoco:check`（全 16 模块）→ SpotBugs / Checkstyle → 安全敏感文件变更检测 → 上传测试/覆盖率报告 + JaCoCo badge |
| `frontend` | 依赖三条安全轨 → Node → `npm ci` → lint / vitest / `tsc --noEmit` → Playwright e2e smoke |

**注意**：`spotbugs`、`checkstyle` 与 `dependency-check` 使用 `continue-on-error: true` 允许失败；`jacoco:check` 覆盖全部 16 模块且为硬门禁；Trivy 无 fail 条件，安全扫描非硬阻断。

---

## 基础设施与部署

### Docker Compose（`docker/docker-compose.yml`）

定义了 12 个服务，运行在自定义桥接网络 `sf-network`（另声明内部网络 `sf-internal`）；所有宿主机端口均绑定 `127.0.0.1`：

| 服务 | 镜像 | 端口 | 说明 |
|------|------|------|------|
| postgres | postgres:16-alpine | 5432 | 主 OLTP 库 |
| redis | redis:7-alpine | 6379 | 缓存 / 会话 / 限流 |
| rabbitmq | rabbitmq:3.12-management-alpine | 5672, 15672 | 消息队列 |
| minio | minio/minio:latest | 9000, 9001 | 对象存储 |
| milvus-standalone | milvusdb/milvus:v2.3.5 | 19530, 9091 | 向量库 |
| etcd | quay.io/coreos/etcd:v3.5.5 | 2379 | Milvus 依赖 |
| clickhouse | clickhouse/clickhouse-server:24.3 | 8123, **9004**→容器 9000 | 分析仓库 |
| elasticsearch | elasticsearch:8.12.0 | 9200 | 日志检索 |
| prometheus | prom/prometheus:v2.50.0 | 9090 | 指标采集 |
| grafana | grafana/grafana:10.3.0 | 3000 | 可视化 |
| jaeger | jaegertracing/all-in-one:1.52 | 16686, 4317, 4318 | 分布式追踪 |
| clamav | clamav/clamav:1.3 | 3310 | 病毒扫描 |

**现状说明（截至 2026-08-30）**：
- **端口冲突已解决**：ClickHouse Native 协议改映射为 `127.0.0.1:9004:9000`，不再与 MinIO API（9000）冲突
- **服务名拼写已修正**：Grafana 服务名为 `grafana`（容器名 `sf-grafana`）
- **应用配置与 Docker 已对齐**：各服务 `application.yml` 统一为 **PostgreSQL**（`jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/schemaplexai`），用户名/密码经 `${DB_USERNAME:?required}` / `${DB_PASSWORD:?required}` 等环境变量注入；RabbitMQ 凭据同样为必填环境变量（`RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD`）。本地启动前需提供这些环境变量（如 `POSTGRES_PASSWORD`、`RABBITMQ_DEFAULT_PASS`、`MINIO_ROOT_PASSWORD`、`GRAFANA_ADMIN_PASSWORD` 均为 `:?required`）
- **健康检查与重启策略**：12 个服务中 7 个配置了 `healthcheck`（postgres / redis / rabbitmq / minio / clickhouse / grafana / clamav），全部服务配置 `restart: unless-stopped`；milvus / etcd / elasticsearch / prometheus / jaeger 暂无健康检查

### 数据库初始化

初始化脚本位于 `docker/postgres/init/`，按序号执行：

| 脚本 | 领域 |
|------|------|
| `01-init-schema.sql` | 核心治理 + Spec（`sf_tenant`、`sf_user`、`sf_role`、`sf_spec`…） |
| `02-init-schema-agent.sql` | Agent + 对话（`sf_agent`、`sf_chat_message` 16 个 Hash 分区、`sf_agent_memory`…） |
| `03-init-schema-others.sql` | 工作流、上下文、质量、集成、运营（`sf_workflow_template`、`sf_knowledge_doc`、`sf_quality_gate`…） |
| `009_observability.sql` | 可观测性（`sf_observability_trace` 等） |

设计特点：启用 UUID 扩展；统一软删除（`deleted` INT，逻辑删除值 = 1）；`sf_chat_message` 按 `conversation_id` 做 16 个 Hash 分区；存在归档表做冷数据存储。

### Prometheus 抓取配置

`docker/prometheus/prometheus.yml` 配置了所有 Java 服务（端口 8080–8091）的 `/actuator/prometheus` 端点抓取。

### Kubernetes / Helm

**暂无。** 项目中未提供 K8s、Helm 或 Terraform 配置文件。

---

## 安全考量

### 认证与鉴权

- **JWT**：使用 `jjwt` 0.12.5，HS256 签名；Token 内嵌 `userId`、`tenantId`、`username`；有效期 24h，刷新期 7d
- **Gateway 层 JWT 校验**：`JwtAuthFilter`（GlobalFilter，order -100）在网关统一校验 Token，并将 `X-User-Id`、`X-Tenant-Id` 注入下游请求
- **白名单**：`/auth/**`、Swagger/Knife4j 路径免鉴权
- **Spring Security**：`schemaplexai-system` 中配置，禁用 CSRF / formLogin / httpBasic，`/auth/**` 放行，其余需认证

### 租户隔离

- **Gateway**：`TenantResolveFilter`（order -200）从请求头解析 `X-Tenant-Id`
- **DAO 层**：`TenantLineInterceptor` 自动为 SQL 追加租户过滤条件
- **Redis Key**：限流、上下文、记忆等 Key 均包含 `tenantId`

### 限流

- **Gateway**：`RateLimitFilter`（order -50）基于 Redis 实现滑动窗口限流，默认 100 请求/分钟（按 `tenantId` 或 IP），超限时返回 429

### 敏感配置

- 密码、Token、密钥等通过环境变量注入（如 `${JWT_SECRET}`）
- **禁止在代码中硬录数据库密码或 API Key**

### 前端已知安全缺陷（来自最近一次代码评审）

- **JWT Token 被放入 URL 参数**：存在安全风险，应改为 Cookie 或临时 Ticket 方案
- **SSE 连接缺少卸载清理与重连机制**：可能导致内存泄漏和状态异常
- **缺少 CSP 策略**：`index.html` 建议补充 `Content-Security-Policy`

---

## 消息队列 Topic

Exchange 统一为 `sf.exchange`：

| Routing Key | 说明 |
|-------------|------|
| `sf.agent.execute` | Agent 执行 |
| `sf.agent.exec.event` | 执行事件广播 |
| `sf.agent.team.context` | 团队上下文共享 |
| `sf.workflow.trigger` | 工作流触发 |
| `sf.notification` | 通知 |
| `sf.cost` | 成本同步 |
| `sf.quality` | 质量事件 |
| `sf.milvus.sync` | Milvus 同步 |
| `sf.agent.config.shadow` | 影子配置更新 |

### 消费者配置差异

截至 2026-08-30 台账口径，两个消费模块均为 RabbitMQ **手动 ACK**（`acknowledge-mode: manual`），差异在并发与预取参数：

- `schemaplexai-agent-engine`：并发 2–10（未单独配置 prefetch / `default-requeue-rejected`）
- `schemaplexai-task`：并发 5–20，prefetch 10，`default-requeue-rejected: false`

---

## Redis Key 规范

| 用途 | Key 格式 |
|------|---------|
| 对话记忆 L1 | `sf:memory:chat:{conversationId}` |
| 团队上下文 | `sf:context:team:{teamId}` |
| MQ 幂等 | `sf:idempotency:{messageId}` |
| 限流计数 | `sf:rate:{tenantId}:{key}` |
| 执行快照（暂停） | `sf:execution:paused:{executionId}` |
| 模型冷却期 | `sf:model:cooldown:{modelId}` |

---

## 关键设计决策

1. **Java 21**：使用虚拟线程提升并发处理能力
2. **多租户**：从 Gateway 到 DAO 全链路租户隔离
3. **状态机**：Agent 执行引擎采用显式状态机驱动，支持中断/恢复/取消
4. **Token 预算**：执行级 Token 管控，防止成本失控
5. **双层记忆**：Redis L1 + PostgreSQL L2，支持压缩与归档
6. **OLTP/OLAP 分离**：PG 主库 + ClickHouse 分析库
7. **服务拆分**：按业务域拆分为 9 个独立业务服务 + 网关/接入层/任务/公共层
8. **LangChain4j**：统一对接 OpenAI 及其他 LLM Provider
9. **RAG 流水线**：Tika 文档解析 → 分块 → Embedding → Milvus 向量检索

---

## 可观测性

| 维度 | 技术栈 | 端口 |
|------|--------|------|
| Metrics | Micrometer + Prometheus | 9090 |
| Logs | ELK Stack（Elasticsearch） | 9200 |
| Traces | OpenTelemetry + Jaeger | — |
| Dashboard | Grafana | 3000 |

各 Java 服务均暴露 `/actuator/prometheus` 端点。

---

## 已知问题与注意事项

1. **循环依赖**：不要在 `pom.xml` 中引入循环依赖
2. **跨服务调用**：优先使用 MQ 异步，避免同步调用链路过长
3. **外部 API 超时**：所有外部调用（LLM、Git、Jenkins 等）必须加超时和降级
4. **配置一致性**：各服务 `application.yml` 已统一为 PostgreSQL + 环境变量注入（`${VAR:?required}`），与 `docker-compose.yml` 对齐；本地启动前需提供必填环境变量
5. **测试与覆盖率**：16 个后端模块均已有测试（截至 2026-08-30 实测口径合计约 4,136）；覆盖率门禁（JaCoCo 指令 80% + 分支 60%）下 `schemaplexai-model`、`schemaplexai-dao` 的最近记录值（台账口径：2026-07-02 产物 model 21.0%、2026-05-06 基线 dao 44.6%）仍低于门禁，覆盖率攻坚进行中。新增核心业务逻辑时必须同步补充测试
6. **代码风格**：仓库中未配置 `checkstyle.xml`、Spotless 或 `.editorconfig`，CI 中的 `checkstyle` 与 `spotbugs` 允许失败（`continue-on-error: true`）；建议补充统一的格式化规则
7. **schemaplexai-admin**：已实现（`SchemaPlexaiAdminApplication` + `application.yml`，端口 8092），含租户/用户/角色/审计日志/系统配置/平台健康 6 个管理服务与 111 个测试（截至 2026-08-30 实测口径）
8. **修复批在途**：Spec 合规修复批（2026-08-29 复核报告驱动）正在进行，逐票状态以 `.scratch/spec-compliance-fix/issues/REGISTRY.md` 为准；本 worktree 有并行工作者，提交须显式路径暂存、绝不夹带 `wiki/`
