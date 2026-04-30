---
topic: api-gateway
stage: spec
version: v1.0
status: 已批准
supersedes: ""
---

# API Gateway 技术规格

> **主题**: `api-gateway`
> **阶段**: `spec`
> **版本**: v1.0
> **状态**: 已批准
> **日期**: 2026-04-15
> **范围**: `schemaplexai-gateway` 服务

---

## 1. 概述

API Gateway 是所有流量的统一入口，承担以下职责：

- **路由**: 根据路径前缀将请求转发至对应后端服务
- **认证**: JWT 验证、Token 刷新
- **授权**: 基于角色的访问控制（RBAC）基础检查
- **多租户**: 从 Header 提取 `X-Tenant-Id` 并传递
- **限流**: 基于 Redis 的滑动窗口限流
- **日志**: 请求/响应日志记录
- **CORS**: 跨域配置

## 2. 架构视图

```
Client → [Gateway:8080] → [Service]

Gateway 内部过滤器链:
  1. RateLimitFilter      ← 限流（Redis 计数器）
  2. JwtAuthFilter        ← JWT 验证 + 用户信息注入
  3. TenantResolveFilter  ← 租户解析
  4. LogFilter            ← 访问日志
  5. Route to Service     ← 负载均衡
```

## 3. 路由规则

| Gateway 前缀 | 目标服务 | 目标端口 | 说明 |
|-------------|---------|---------|------|
| `/auth/**` | system | 8081 | 登录/注册/刷新 |
| `/system/**` | system | 8081 | 系统治理 |
| `/web/**` | web | 8082 | REST API / SSE |
| `/agent-config/**` | agent-config | 8083 | Agent 配置 |
| `/agents/**` | agent-engine | 8084 | Agent 执行 |
| `/agent-engine/**` | agent-engine | 8084 | Agent 引擎内部 |
| `/context/**` | context | 8085 | 上下文/RAG |
| `/spec/**` | spec | 8086 | Spec 管理 |
| `/workflow/**` | workflow | 8087 | 工作流 |
| `/integration/**` | integration | 8088 | 集成 |
| `/ops/**` | ops | 8089 | 运营 |
| `/quality/**` | quality | 8090 | 质量 |
| `/task/**` | task | 8091 | 异步任务 |

## 4. 过滤器规格

### 4.1 RateLimitFilter

**职责**: 基于 Redis 的滑动窗口限流

**策略**:

- Key 格式: `rate_limit:{tenant_id}:{client_ip}:{path}`
- 窗口: 60 秒
- 默认阈值: 100 请求/分钟（租户级）
- 异常处理: **Fail-Closed**（Redis 异常时拒绝请求）

```java
public class RateLimitFilter implements GlobalFilter {
    // Redis 计数器
    // 超限返回 429 Too Many Requests
    // Redis 异常 → 拒绝请求（安全优先）
}
```

### 4.2 JwtAuthFilter

**职责**: JWT 验证与用户信息注入

**流程**:

1. 排除白名单路径（`/auth/login`, `/auth/register`, `/doc.html`, `/v3/api-docs/**`）
2. 从 `Authorization` Header 提取 Token
3. 验证 Token 签名、过期时间
4. 解析 `userId`, `tenantId`
5. **注入下游 Header**:
   - `X-User-Id`: 用户 ID
   - `X-Tenant-Id`: 租户 ID
   - `Authorization`: Bearer Token（透传）

**关键约束**:

- 必须单次构建 `ServerHttpRequest`（避免多次 mutate 导致 Header 丢失）
- Token 过期返回 401，格式为 JSON

### 4.3 TenantResolveFilter

**职责**: 确保每个请求都携带租户 ID

**优先级**: 在 JwtAuthFilter 之后

**逻辑**:

```
if (X-Tenant-Id Header 存在):
    验证租户存在性（缓存查询）
    传递至下游
else if (Token 中包含 tenantId):
    自动注入
else:
    返回 400 Bad Request（缺少租户信息）
```

### 4.4 LogFilter

**职责**: 请求/响应日志

**记录内容**:

- 请求: method, path, query, userId, tenantId, timestamp
- 响应: status, duration_ms
- 敏感信息脱敏: password, token, secret

## 5. 安全配置

### 5.1 CORS 配置

```yaml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOrigins: "${CORS_ORIGINS:http://localhost:3000}"
            allowedMethods: GET, POST, PUT, DELETE, OPTIONS
            allowedHeaders: "*"
            allowCredentials: true
```

### 5.2 JWT 验证

```java
@PostConstruct
public void validateSecret() {
    if (secret == null || secret.length() < 32) {
        throw new IllegalStateException("JWT_SECRET must be at least 32 characters");
    }
}
```

- Secret 必须从环境变量 `JWT_SECRET` 读取
- **禁止**在配置文件中硬编码默认值

## 6. API 响应格式

Gateway 层错误统一返回 JSON:

```json
{
  "code": 401,
  "message": "Unauthorized: token expired",
  "timestamp": 1714500000000
}
```

**禁止**使用 String.format 拼接 JSON，必须使用 `ObjectMapper` 序列化。

## 7. 性能指标

| 指标 | 目标 |
|------|------|
| Gateway 转发延迟 | P99 < 5ms |
| JWT 验证耗时 | P99 < 2ms |
| Redis 限流查询 | P99 < 3ms |
| 单实例 QPS | > 5000 |

## 8. 相关文档

- `docs/decisions/ADR-001-service-decomposition.md`
- `docs/plans/unified-dev-plan.md`（Tasks 4, 17, 19）
- `docs/standards/security.md`
