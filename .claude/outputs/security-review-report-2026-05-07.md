# SchemaPlexAI 安全审查报告

- **审查日期**: 2026-05-07
- **审查范围**: 全项目安全态势评估（Gateway, Agent Engine, Context, UI, 配置）
- **审查方式**: 静态代码分析 + 配置审查

---

## 总体评估

| 维度 | 状态 | 备注 |
|------|------|------|
| 硬编码密钥 | **通过** | 所有 application.yml 使用 `${}` 环境变量，无硬编码密码 |
| SQL 注入 | **通过** | MyBatis-Plus 参数化查询，无拼接 SQL |
| 认证/授权 | **已修复** | JwtAuthFilter 已修复租户冒充漏洞（C-3） |
| 全局异常处理 | **已修复** | GlobalExceptionHandler 已添加（C-4） |
| XSS 防护 | **MEDIUM** | 1 处 `dangerouslySetInnerHTML` 需关注 |
| Milvus 注入 | **已修复** | tenantId 格式校验已添加（M-11） |

---

## CRITICAL — 无

所有 Critical 级安全问题已在之前的审查中修复：
- **C-2** ✅ FailedStatusWriter REQUIRES_NEW 子事务
- **C-3** ✅ JwtAuthFilter 租户头剥离
- **C-5** ✅ AgentStateMachine FAILED 递归防护

---

## HIGH

### S-001: 前端 XSS 风险 — dangerouslySetInnerHTML
- **位置**: `schemaplexai-ui/src/pages/Login/index.tsx:742`
- **描述**: `<span dangerouslySetInnerHTML={{ __html: line.msg }} />` 用于终端动画效果
- **风险**: `line.msg` 当前由组件内部生成（静态 HTML 字符串），短期无直接注入路径。但如果未来改为从 API 获取消息或接受用户输入，将产生 XSS 漏洞
- **建议**: 使用 DOMPurify 清理，或改为 React JSX 渲染（将 HTML 标签转为 React 组件）
- **优先级**: 合并前评估

### S-002: Milvus filter 表达式注入（已修复）
- **位置**: `schemaplexai-context/.../RagSearchServiceImpl.java:60-62`
- **描述**: tenantId 拼接到 Milvus filter 表达式
- **状态**: **已修复** — 添加了 `TENANT_ID_PATTERN` 正则校验 + 双引号转义
- **验证**: `Pattern.compile("^[a-zA-Z0-9\\-]+$")` 仅允许字母数字和短横线

---

## MEDIUM

### S-003: JWT Secret 对称密钥
- **位置**: `schemaplexai-gateway/.../JwtAuthFilter.java:38-46`
- **描述**: 使用 HMAC-SHA 对称密钥（`jwt.secret`），网关泄漏密钥可导致全栈 token 伪造
- **建议**: 长期迁移到 RS256/ES256 非对称密钥，短期确保 secret ≥ 32 字节（已校验）
- **优先级**: 合并后跟进

### S-004: 白名单路径范围过宽
- **位置**: `schemaplexai-gateway/.../JwtAuthFilter.java:50-57`
- **描述**: `/system/tenants/**` 整个目录树无鉴权，包括管理接口
- **建议**: 收窄到 `/system/tenants/register` 或 `/system/tenants/lookup-by-domain`
- **优先级**: 合并后跟进

### S-005: CSRF 保护状态
- **位置**: Spring Security 配置
- **描述**: 项目使用 JWT token 认证（无 cookie-based session），CSRF 风险较低。但需确认 Spring Security 默认 CSRF 配置是否显式禁用
- **建议**: 显式配置 `.csrf(csrf -> csrf.disable())` 并文档说明原因
- **优先级**: 合并后跟进

### S-006: RateLimitFilter IP 识别
- **位置**: `schemaplexai-gateway/.../RateLimitFilter.java:64-69`
- **描述**: 反向代理后所有请求共享代理 IP 的限流桶
- **建议**: 配置 `forwarded-headers-strategy: framework`，优先读 `X-Forwarded-For`
- **优先级**: 合并后跟进

---

## LOW

### S-007: 日志中敏感信息
- **位置**: 多处 `log.info/warn`
- **描述**: 当前日志不打印密码/Token，但 JWT token 前缀可能出现在 debug 日志中
- **建议**: 确保生产日志级别不低于 INFO

### S-008: ObjectMapper 每次新建
- **位置**: `JwtAuthFilter.java:131`
- **描述**: `new ObjectMapper()` 在每次 401 响应时新建，浪费资源
- **建议**: 注入共享 ObjectMapper Bean

---

## 已修复问题验证

| 编号 | 问题 | 状态 | 验证方式 |
|------|------|------|----------|
| C-2 | MilvusSync 事务回滚 | ✅ | FailedStatusWriter + REQUIRES_NEW |
| C-3 | 租户头冒充 | ✅ | headers() 先 remove 再 add |
| C-4 | 无全局异常处理 | ✅ | GlobalExceptionHandler @RestControllerAdvice |
| C-5 | 状态机递归 | ✅ | `newState != FAILED` 守卫 |
| M-11 | Milvus filter 注入 | ✅ | TENANT_ID_PATTERN 校验 |

---

## 修复优先级建议

1. **立即**: 无（所有 Critical 已修复）
2. **合并前**: S-001 (XSS 评估)
3. **合并后 Sprint 1**: S-003, S-004, S-005
4. **合并后 Sprint 2**: S-006, S-007, S-008

---

## 总结

项目安全态势良好。所有 Critical 级问题已修复，硬编码密钥检查通过，SQL 注入防护到位，多租户隔离已加强。主要剩余关注点是前端 XSS（低风险但需评估）和 JWT 对称密钥的长期演进。
