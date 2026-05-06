# SchemaPlexAI API 文档

## Knife4j OpenAPI 访问

启动 `schemaplexai-web` 后，访问 Swagger UI：

```
http://localhost:8082/doc.html
```

下拉菜单中可按服务分组（GroupedOpenApi）切换查看各模块的 API。OpenAPI JSON 位于 `/v3/api-docs`。

---

## 服务列表

| # | 服务 | 端口 | 基础路径 | 说明 |
|---|------|------|----------|------|
| 01 | web | 8082 | `/web/**`, `/sse/**`, `/ws/**` | Web 控制器、SSE 推送、WebSocket |
| 02 | system | 8081 | `/system/**`, `/auth/**` | 认证、用户、租户、角色、权限 |
| 03 | agent-config | 8083 | `/agent-config/**` | Agent 定义与配置 |
| 04 | agent-engine | 8084 | `/agent/**` | LLM 编排、工具执行（核心） |
| 05 | context | 8085 | `/context/**` | RAG、向量搜索、知识文档 |
| 06 | spec | 8086 | `/spec/**` | 规范文档、评审、版本 |
| 07 | workflow | 8087 | `/workflow/**` | Flowable BPMN 工作流、AI 节点 |
| 08 | integration | 8088 | `/integration/**` | GitHub/GitLab/MCP 集成 |
| 09 | ops | 8089 | `/ops/**` | 制品、成本分析、预算评估 |
| 10 | quality | 8090 | `/quality/**` | 质量门、安全检查、审计事件 |

---

## 通用模式

### 认证 — Bearer JWT

所有需要认证的接口需要在请求头中携带 JWT Token：

```
Authorization: Bearer <token>
```

Token 通过 `/auth/login` 接口获取。Knife4j UI 已内置全局 BearerAuth 安全方案，点击页面右上角 **Authorize** 按钮输入 Token 即可。

> 公开路径（网关白名单）：`/auth/login`、`/v3/api-docs/**`、`/swagger-ui/**`、`/doc.html`、`/webjars/**`

### 多租户 — X-Tenant-Id

多租户数据隔离通过请求头实现：

```
X-Tenant-Id: <tenant-id>
```

- 后端通过 `TenantContextHolder` + `TenantLineInterceptor` 自动注入 `tenant_id` 过滤
- 全局表（`sf_tenant`、`act_*` 系列）不参与租户隔离
- Knife4j UI 中每个分组都已添加 `X-Tenant-Id` 全局参数

### 响应格式 — Result\<T\>

所有接口统一返回 `com.schemaplexai.common.result.Result<T>`：

```json
{
  "code": 200,
  "message": "success",
  "data": { }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | Integer | 业务状态码，200 表示成功 |
| `message` | String | 提示信息 |
| `data` | T | 响应数据，错误时为 null |

### 分页响应

分页接口使用 MyBatis-Plus 的 `IPage<T>`，`data` 字段结构：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [],
    "total": 100,
    "size": 20,
    "current": 1
  }
}
```

---

## 通用错误码

错误码定义于 `schemaplexai-common/src/main/java/com/schemaplexai/common/result/ResultCode.java`：

### 通用错误

| 码 | 常量 | 说明 |
|----|------|------|
| 200 | SUCCESS | 成功 |
| 400 | PARAM_ERROR | 参数错误 |
| 401 | UNAUTHORIZED | 未认证 |
| 403 | FORBIDDEN | 无权限 |
| 404 | NOT_FOUND | 资源不存在 |
| 405 | METHOD_NOT_ALLOWED | 方法不允许 |
| 408 | REQUEST_TIMEOUT | 请求超时 |
| 500 | ERROR | 系统错误 |

### 租户

| 码 | 常量 | 说明 |
|----|------|------|
| 1001 | TENANT_NOT_FOUND | 租户不存在 |
| 1002 | TENANT_DISABLED | 租户已禁用 |

### 认证

| 码 | 常量 | 说明 |
|----|------|------|
| 2001 | TOKEN_EXPIRED | Token 已过期 |
| 2002 | TOKEN_INVALID | Token 无效 |
| 2003 | USER_NOT_FOUND | 用户不存在 |
| 2004 | PASSWORD_ERROR | 密码错误 |

### Agent

| 码 | 常量 | 说明 |
|----|------|------|
| 3001 | AGENT_NOT_FOUND | Agent 不存在 |
| 3002 | AGENT_EXECUTION_FAILED | Agent 执行失败 |
| 3003 | AGENT_RATE_LIMIT | 速率限制 |
| 3004 | TOKEN_BUDGET_EXCEEDED | Token 预算超限 |
| 3005 | LOOP_DETECTED | Agent 循环检测 |

### Workflow

| 码 | 常量 | 说明 |
|----|------|------|
| 4001 | WORKFLOW_NOT_FOUND | 工作流不存在 |
| 4002 | WORKFLOW_INSTANCE_NOT_FOUND | 工作流实例不存在 |

### Spec

| 码 | 常量 | 说明 |
|----|------|------|
| 5001 | SPEC_NOT_FOUND | 规范不存在 |

### Quality

| 码 | 常量 | 说明 |
|----|------|------|
| 6001 | QUALITY_GATE_BLOCKED | 质量门阻塞 |

### Integration

| 码 | 常量 | 说明 |
|----|------|------|
| 7001 | INTEGRATION_NOT_FOUND | 集成不存在 |
| 7002 | TOOL_EXECUTION_FAILED | 工具执行失败 |

### Context

| 码 | 常量 | 说明 |
|----|------|------|
| 8001 | CONTEXT_NOT_FOUND | 上下文不存在 |
| 8002 | KNOWLEDGE_DOC_NOT_FOUND | 知识文档不存在 |

### Sync

| 码 | 常量 | 说明 |
|----|------|------|
| 9001 | SYNC_CURSOR_ERROR | 同步游标错误 |

---

## Controller 端点索引

### Web（8082）

| Controller | 路径 | 说明 |
|------------|------|------|
| NotificationController | `/web/notification` | 通知管理（分页查询、已读标记） |
| SseController | `/sse` | SSE 事件订阅与推送 |

### System & Auth（8081）

| Controller | 路径 | 说明 |
|------------|------|------|
| AuthController | `/auth` | 登录认证 |
| UserController | `/system/user` | 用户管理 |
| TenantController | `/system/tenant` | 租户管理 |
| RoleController | `/system/role` | 角色管理 |
| PermissionController | `/system/permission` | 权限管理 |
| ConfigController | `/system/config` | 系统配置 |
| AiModelController | `/system/ai-model` | AI 模型管理 |
| ModelProviderController | `/system/model-provider` | 模型供应商管理 |

### Agent Config（8083）

| Controller | 路径 | 说明 |
|------------|------|------|
| AgentConfigController | `/agent-config` | Agent 配置 CRUD |
| PromptVersionController | `/agent-config/prompt-version` | Prompt 版本管理 |

### Agent Engine（8084）

| Controller | 路径 | 说明 |
|------------|------|------|
| AgentExecutionController | `/agent` | Agent 执行、任务管理 |

### Context（8085）

| Controller | 路径 | 说明 |
|------------|------|------|
| ContextController | `/context` | 上下文管理 |
| RagController | `/context/rag` | RAG 检索 |
| KnowledgeDocController | `/context/knowledge-doc` | 知识文档 |
| WorkspaceController | `/context/workspace` | 工作区管理 |

### Spec（8086）

| Controller | 路径 | 说明 |
|------------|------|------|
| SpecController | `/spec` | 规范文档 |
| SpecVersionController | `/spec/version` | 版本管理 |
| SpecReviewController | `/spec/review` | 评审 |
| SpecTemplateController | `/spec/template` | 模板 |
| SpecSteeringController | `/spec/steering` | 评审引导 |

### Workflow（8087）

| Controller | 路径 | 说明 |
|------------|------|------|
| WorkflowTemplateController | `/workflow/template` | 流程模板 |
| WorkflowInstanceController | `/workflow/instance` | 流程实例 |

### Integration（8088）

| Controller | 路径 | 说明 |
|------------|------|------|
| IntegrationController | `/integration` | 集成管理 |
| ApiGatewayController | `/integration/api-gateway` | API 网关配置 |
| McpServerController | `/integration/mcp-server` | MCP 服务器 |
| SkillController | `/integration/skill` | 技能管理 |

### Ops（8089）

| Controller | 路径 | 说明 |
|------------|------|------|
| ArtifactController | `/ops/artifact` | 制品管理 |
| CostController | `/ops/cost` | 成本分析 |
| BudgetController | `/ops/budget` | 预算管理 |
| EvaluationController | `/ops/evaluation` | 评估 |
| NotificationController | `/ops/notification` | 运维通知 |

### Quality（8090）

| Controller | 路径 | 说明 |
|------------|------|------|
| QualityGateController | `/quality/gate` | 质量门 |
| QualityIssueController | `/quality/issue` | 质量问题 |
| ReviewController | `/quality/review` | 代码评审 |
| SecurityPolicyController | `/quality/security-policy` | 安全策略 |
| AuditEventController | `/quality/audit` | 审计事件 |

---

## 技术实现

- **OpenAPI 实现**：SpringDoc OpenAPI（Spring Boot 3.2.5 对应 Jakarta 命名空间）
- **UI**：Knife4j 4.4.0
- **依赖**：`knife4j-openapi3-jakarta-spring-boot-starter`
- **配置类**：`com.schemaplexai.web.config.Knife4jConfig`
- **配置文件**：`schemaplexai-web/src/main/resources/application.yml`

### Maven 依赖

```xml
<dependency>
    <groupId>com.github.xiaoymin</groupId>
    <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
</dependency>
```

### application.yml 配置

```yaml
knife4j:
  enable: true
  setting:
    language: zh_cn
```
