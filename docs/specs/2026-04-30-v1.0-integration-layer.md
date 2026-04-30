---
topic: integration-layer
stage: spec
version: v1.0
status: 草稿
supersedes: ""
---

# 集成层技术规格

> **主题**: `integration-layer`
> **阶段**: `spec`
> **版本**: v1.0
> **状态**: 草稿
> **日期**: 2026-04-30
> **范围**: `schemaplexai-integration` 服务

---

## 1. 概述

集成层负责连接外部系统，为 Agent 提供工具调用能力。支持：

- **Git 集成**: GitHub/GitLab 代码仓库操作
- **CI/CD 集成**: Jenkins 构建触发
- **MCP 服务**: Model Context Protocol 服务器管理
- **Skill 注册**: 可复用技能包管理
- **API Gateway**: 外部 API 注册与调用

## 2. 架构视图

```
┌─────────────────────────────────────────────┐
│              ToolExecutionService            │
│         (统一工具调用入口)                    │
└──────────────┬──────────────────────────────┘
               │
    ┌──────────┼──────────┬──────────┐
    │          │          │          │
    ▼          ▼          ▼          ▼
┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐
│ Local  │ │  MCP   │ │  Git   │ │ Jenkins│
│ Tools  │ │ Tools  │ │ Tools  │ │ Tools  │
└────────┘ └────────┘ └────────┘ └────────┘
```

## 3. 工具执行框架

### 3.1 工具适配器接口

```java
public interface ToolAdapter {
    String getToolType();
    void validateParams(Map<String, Object> params);
    ToolExecutionResult execute(Long tenantId, Map<String, Object> params);
    boolean isReadOnly();
}
```

### 3.2 工具执行服务

```java
@Service
public class ToolExecutionService {
    // 注册表: Map<String, ToolAdapter>
    // 超时: 30 秒
    // 参数校验: JSON Schema 验证
}
```

## 4. 集成类型规格

### 4.1 Git 集成

**支持平台**: GitHub, GitLab, Gitea

**功能**:

| 功能 | 说明 | 状态 |
|------|------|------|
| OAuth 授权 | 用户授权接入 Git 平台 | 未实现 |
| 仓库列表 | 获取用户/组织的仓库列表 | 未实现 |
| 文件操作 | 读取/创建/更新/删除文件 | 未实现 |
| PR 管理 | 创建/查看/合并 Pull Request | 未实现 |
| Webhook | 接收 Push/MR 事件 | 未实现 |
| Git Worktree | 多租户安全隔离的 Git 工作区 | 未实现 |

**OAuth 流程**:

```
用户点击"连接 GitHub"
    ↓
重定向到 GitHub OAuth 授权页
    ↓
用户授权 → 回调到 /integration/git/callback
    ↓
交换 code 获取 access_token
    ↓
加密存储 token（AES-256-GCM）
    ↓
返回成功，显示已连接仓库
```

**安全约束**:

- Token 必须加密存储
- 每个租户的 Git 操作隔离
- 禁止访问仓库以外的资源

### 4.2 Jenkins 集成

**状态**: 部分实现（`triggerBuild` 已可用）

**功能**:

| 功能 | 说明 | 状态 |
|------|------|------|
| 触发构建 | 调用 Jenkins API 触发 Job | 已实现 |
| 构建状态查询 | 查询构建进度和结果 | 未实现 |
| 构建日志获取 | 获取实时构建日志 | 未实现 |
| Job 配置管理 | 创建/更新 Jenkins Job | 未实现 |

### 4.3 MCP 服务管理

**状态**: 部分实现（McpToolExecutor 可用，但生命周期管理缺失）

**MCP 生命周期**:

```
REGISTERED → CONNECTED → AVAILABLE → DISCONNECTED
```

| 状态 | 说明 |
|------|------|
| `REGISTERED` | 已注册，未连接 |
| `CONNECTED` | 已建立连接 |
| `AVAILABLE` | 已获取工具列表，可调用 |
| `DISCONNECTED` | 连接断开 |

**功能**:

| 功能 | 说明 | 状态 |
|------|------|------|
| 注册 MCP Server | 添加 MCP 服务端点 | 部分实现 |
| 连接测试 | 验证 MCP Server 可用性 | 已实现 |
| 获取工具列表 | `listTools()` | 未实现 |
| 调用 MCP 工具 | `callTool()` | 已实现 |
| 断开连接 | 关闭 MCP 连接 | 未实现 |

### 4.4 Skill 注册

**Skill**: 可复用的 Agent 能力包，包含 Prompt 模板 + 工具集 + 配置

**数据模型**:

```java
public class SfSkill {
    private Long id;
    private String name;
    private String description;
    private String promptTemplate;
    private List<String> toolTypes;  // 绑定的工具类型
    private Map<String, Object> config;
}
```

### 4.5 API Gateway 管理

**职责**: 管理外部 API 的注册、调用、限流

**功能**:

| 功能 | 说明 |
|------|------|
| API 注册 | 注册外部 API（URL、方法、参数、认证方式） |
| 调用代理 | 通过平台代理调用外部 API（统一限流、日志） |
| 响应缓存 | 缓存外部 API 响应 |
| 熔断降级 | 外部 API 不可用时熔断 |

## 5. 数据模型

### 5.1 sf_integration

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| tenant_id | BIGINT | 租户隔离 |
| type | VARCHAR | GIT / JENKINS / MCP / API |
| name | VARCHAR | 集成名称 |
| config | JSONB | 配置（加密存储敏感信息） |
| status | VARCHAR | 状态 |

### 5.2 sf_mcp_server

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| tenant_id | BIGINT | 租户隔离 |
| name | VARCHAR | 名称 |
| endpoint | VARCHAR | MCP 服务端点 |
| status | VARCHAR | REGISTERED / CONNECTED / DISCONNECTED |
| tools | JSONB | 可用工具列表（缓存） |

## 6. API 接口

```http
# Git 集成
POST   /integration/git/connect          # OAuth 连接
GET    /integration/git/repos            # 获取仓库列表
GET    /integration/git/repos/{id}/files # 获取文件内容
POST   /integration/git/repos/{id}/files # 创建/更新文件

# Jenkins 集成
POST   /integration/jenkins/builds       # 触发构建
GET    /integration/jenkins/builds/{id}  # 查询构建状态

# MCP 管理
POST   /integration/mcp/servers          # 注册 MCP Server
POST   /integration/mcp/servers/{id}/connect    # 连接
POST   /integration/mcp/servers/{id}/disconnect # 断开
GET    /integration/mcp/servers/{id}/tools      # 获取工具列表

# Skill 管理
POST   /integration/skills               # 创建 Skill
GET    /integration/skills               # 列表

# API Gateway
POST   /integration/api-gateways         # 注册 API
POST   /integration/api-gateways/{id}/invoke   # 调用 API
```

## 7. 非功能需求

| 指标 | 目标 |
|------|------|
| 工具调用延迟 | P99 < 5s |
| 外部 API 超时 | 30s |
| MCP 连接超时 | 10s |
| Token 加密 | AES-256-GCM |

## 8. 相关文档

- `docs/plans/project-plan.md`（Phase 7）
- `docs/plans/unified-dev-plan.md`（Tasks 40-41）
