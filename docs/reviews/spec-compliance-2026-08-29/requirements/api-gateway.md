# Spec 合规核查 — api-gateway

- **Spec**: `docs/specs/2026-04-30-v1.0-api-gateway.md`（v1.0，已批准）
- **核查对象**: `schemaplexai-gateway/src/main/java`、`schemaplexai-gateway/src/main/resources/application.yml`；执行点旁证：`schemaplexai-common`（常量/Key 解析/启动校验）、`schemaplexai-system`（JWT 签发侧）
- **核查日期**: 2026-08-29
- **方法**: 逐字通读 spec → 提取 25 条规范性需求 → grep 定位执行点后通读实际代码 → 走查成功/失败路径（Token 缺失/过期/篡改、白名单、限流超限、Redis 异常、无租户）→ 过滤器 order 实际核对
- **裁决统计**: implemented 14 · partial 3 · contradicted 5 · absent 3 · undecidable 0

> 实际过滤器链（按 `getOrder()` 实测，数值越小越先执行）：
> `LoggingFilter`(Integer.MIN_VALUE, LoggingFilter.java:47) → `TracePropagationFilter`(MIN_VALUE+100, TracePropagationFilter.java:44) → `JwtAuthFilter`(-100, JwtAuthFilter.java:145) → `TenantResolveFilter`(-90, TenantResolveFilter.java:50) → `RateLimitFilter`(-50, RateLimitFilter.java:119) → 路由。

---

## REQ-01 — 路由前缀 → 目标服务映射（13 条）

> "| `/auth/**` | system | 8081 | … | `/task/**` | task | 8091 |" — spec §3 路由规则表

**Verdict:** implemented · confidence: high
**What this demands:** spec 表中 13 个前缀均存在路由，且转发到表中对应的目标服务。
**Where enforcement lives:** `schemaplexai-gateway/src/main/java/com/schemaplexai/gateway/config/GatewayConfig.java:13-38`（Java DSL，`lb://schemaplexai-<service>`）。application.yml:10-13 注明路由仅在 Java DSL 中定义，无 YAML 路由重复。
**Paths walked:**
- ✓ `/auth/**`、`/system/**` → `lb://schemaplexai-system`（GatewayConfig.java:14-15）
- ✓ `/web/**` → web（L16-17）；`/agent-config/**` → agent-config（L18-19）
- ✓ `/agents/**`、`/agent-engine/**` → agent-engine（L20-21）
- ✓ `/context/**`(L24)、`/spec/**`(L26)、`/workflow/**`(L22)、`/integration/**`(L30)、`/ops/**`(L34)、`/quality/**`(L28)、`/task/**`(L32)
- ✗ 端口与实例可达性 → 见 REQ-02
**How the verdict was reached:** 13 个 spec 前缀全部命中且服务名一一对应，故非 partial。代码另有 spec 未提及的 `/sse/**`、`/ws/**`(L16)、`/agent/**`(L20)、`/admin/**`(L36-37) 四个前缀 → 记入反向差距，不影响本条。无 StripPrefix（grep `StripPrefix|filters|rewrite` → 0 命中），路径原样转发，与下游 `@RequestMapping("/auth")`(AuthController.java:19)、`/system/tenants`(TenantController.java:16) 一致。

## REQ-02 — 路由端到端可达（目标端口 8081–8091）

> "| Gateway 前缀 | 目标服务 | 目标端口 |" — spec §3

**Verdict:** partial · confidence: medium
**What this demands:** 请求经路由后实际到达表中端口上的目标服务实例。
**Where enforcement lives:** 端口侧：各服务 `application.yml` 的 `server.port` 与 spec 完全一致（system 8081 … task 8091，逐一核对 11 个模块）。解析侧：**缺失** —— 路由 URI 为 `lb://` 方案（GatewayConfig.java:15 等），`spring-cloud-starter-gateway:4.1.2` 传递依赖包含 `spring-cloud-starter-loadbalancer`（已核对本地仓库 POM），但 LoadBalancer 需要服务实例来源。
**Paths walked:**
- ✓ 前缀 → 服务名映射（REQ-01）
- ✓ 服务自身监听 spec 端口（各模块 application.yml `server.port`）
- ✗ 服务名 → 实例（host:port）解析：仓库内无任何发现机制 → 运行期 LoadBalancer 解析为空实例列表 → 所有路由请求预期 503
**Searched:**
- `loadbalancer`（全仓库 pom/yml）→ 0 命中（loadbalancer 仅经 starter 传递引入）
- `eureka|nacos|consul`（全仓库 pom/yml）→ 0 命中
- `discovery`（gateway src/main）→ 0 命中；全仓库 yml → 仅 elasticsearch `discovery.type`(docker-compose.yml:143) 与 RabbitMQ listener `simple:`（agent-engine/task，非服务发现）
- `spring.cloud.discovery.client.simple.instances` → 0 命中
- docker-compose.yml → 仅基础设施容器，无网关/应用服务定义
**How the verdict was reached:** 非 implemented：仓库自身运行方式（agents.md 启动顺序为本机 `mvn spring-boot:run`）下无实例来源，`lb://` 名称解析必然失败。非 contradicted：映射与端口本身正确，缺的是解析链路一环；且不排除仓库外部署环境（如 K8s discovery）补齐 → 列入 Open questions。严重度 High（功能性：网关整体不可用，非安全缺口）。

## REQ-03 — 过滤器链顺序：RateLimit → JwtAuth → TenantResolve → Log → 路由

> "Gateway 内部过滤器链: 1. RateLimitFilter … 2. JwtAuthFilter … 3. TenantResolveFilter … 4. LogFilter … 5. Route to Service" — spec §2

**Verdict:** contradicted · confidence: high
**What this demands:** 限流最先执行（在任何 JWT 计算之前），访问日志最后（路由前）。
**Where enforcement lives:** 实测顺序（见文首）：Logging(MIN_VALUE) → TracePropagation(MIN_VALUE+100) → JwtAuth(-100) → TenantResolve(-90) → **RateLimit(-50, 最后)**。证据：LoggingFilter.java:47、TracePropagationFilter.java:44、JwtAuthFilter.java:145、TenantResolveFilter.java:50、RateLimitFilter.java:119。
**Paths walked:**
- ✗ 匿名洪水：无 Token 的请求在 JwtAuthFilter(-100) 处以 401 终止（JwtAuthFilter.java:79-81），**永远到不了** RateLimitFilter(-50) → 匿名流量完全不受限流保护，且每请求都先做 HMAC 解析
- ✗ `/auth/**`：JWT 白名单直通（JwtAuthFilter.java:68-76）且限流白名单同样豁免（application.yml:52-57）→ 登录暴破在网关层零限流
- ✓ 已认证请求：JwtAuth 注入 `X-Tenant-Id` 后 RateLimit 才能取到租户维度 Key（RateLimitFilter.java:87）——现顺序是租户级限流可用的前提（工程权衡，见下）
**How the verdict was reached:** 顺序与 spec §2 编号明确相反（RateLimit 由第 1 变最后、Log 由第 4 变第 1），非文档措辞歧义。注意：spec 自身顺序下 RateLimitFilter 只能依赖客户端自报的 `X-Tenant-Id` 做租户限流（可伪造），代码顺序修复了该缺陷但未回写 spec。严重度 **High**（网关为信任边界，从严）：规格强制的限流保护在"未认证流量"这一最需要限流的路径类上完全不生效——匿名洪泛不受限且每请求都先做 HMAC 解析（CPU 成本），叠加反向差距 #3（`/auth/**` 免限流）后登录口暴破零节流；未达 Critical 仅因后端仍被 JwtAuth 401 保护、无越权后果。

## REQ-04 — 限流 Key 格式 `rate_limit:{tenant_id}:{client_ip}:{path}`

> "Key 格式: `rate_limit:{tenant_id}:{client_ip}:{path}`" — spec §4.1

**Verdict:** contradicted · confidence: high
**What this demands:** 限流计数按 租户+客户端IP+路径 三维粒度分桶。
**Where enforcement lives:** RateLimitFilter.java:86-95 → `TenantRedisKeyResolver.rateLimit()`（TenantRedisKeyResolver.java:115-117）：租户请求 Key = `sf:{tenantId}:ratelimit:tenant:{windowKey}`；无租户回退 `sf:global:ratelimit:ip:{ip}:{windowKey}`（TenantRedisKeyResolver.java:120-122）。
**Paths walked:**
- ✗ 租户请求：Key 不含 client_ip 与 path → 同租户所有用户所有路径共享 100/min 桶（比 spec 更粗粒度、单租户更严）
- ✓ 无租户请求：按 IP 维度回退（spec 未定义此路径）
**Searched:** `rate_limit:`（全仓库 java/yml）→ 0 命中（spec 中的字面前缀无实现）
**How the verdict was reached:** 非 implemented-via-other-mechanism：Key 维度是 spec 明示的"策略"内容，且行为可观测地不同（多用户大租户会更早被整体限流；单用户单路径刷接口在 spec 粒度下更早触顶）。注：`TenantRedisKeyResolver` 类注释（L9-10）说明统一前缀是为防跨租户 Key 碰撞——工程上更优，属文档漂移候选。严重度 Low。

## REQ-05 — 窗口 60s、默认阈值 100 请求/分钟（租户级）

> "窗口: 60 秒 / 默认阈值: 100 请求/分钟（租户级）" — spec §4.1

**Verdict:** implemented · confidence: high
**What this demands:** 每租户每 60 秒窗口最多 100 请求，超出即拒。
**Where enforcement lives:** application.yml:48-51（`default-limit: 100`、`window-size: 60`）；默认值兜底 RateLimitProperties.java:27-33；判定 `count > getDefaultLimit()` → 429（RateLimitFilter.java:69-74）；窗口 TTL `expire(key, 60s)`（L63-66）。
**Paths walked:**
- ✓ 第 1 个请求：INCR=1 → 设置 60s 过期（L63-65）
- ✓ 第 101 个请求：`101 > 100` → 429（L70-71）
- ✓ 配置缺省时代码默认亦为 100/60（RateLimitProperties.java:30/33）
**How the verdict was reached:** 数值与租户级语义均可逐行对上；Key 粒度偏差归 REQ-04、算法归 REQ-06 单独裁决，不折损本条。

## REQ-06 — 滑动窗口算法

> "**职责**: 基于 Redis 的滑动窗口限流" — spec §4.1（§1 亦称"基于 Redis 的滑动窗口限流"）

**Verdict:** contradicted · confidence: high
**What this demands:** 计数随时间连续滑动，任意 60s 区间内不超过阈值。
**Where enforcement lives:** RateLimitFilter.java:56-57：`windowKey = System.currentTimeMillis()/1000/windowSeconds`（代码注释自证 "window-aligned time slot"）+ INCR/EXPIRE —— 教科书式**固定窗口**计数器。
**Paths walked:**
- ✗ 窗口边界突刺：第 N 窗口末尾 100 请求 + 第 N+1 窗口开头 100 请求 → 任意跨界 60s 区间内可达 200 请求，违反滑动窗口语义
- ✓ 单窗口内限额正确
**How the verdict was reached:** 非 partial：固定窗口与滑动窗口是不同算法而非不完整实现；无 ZSET/时间戳序列等滑窗结构（gateway 内 `opsForValue().increment` 是唯一计数操作，RateLimitFilter.java:60-61）。agents.md 同样写"滑动窗口"，属文档连锁漂移。严重度 Low（2 倍边界突刺，无越权）。

## REQ-07 — Redis 异常 Fail-Closed

> "异常处理: **Fail-Closed**（Redis 异常时拒绝请求）" — spec §4.1

**Verdict:** implemented · confidence: high
**What this demands:** 限流依赖故障时宁可拒绝也不放行。
**Where enforcement lives:** RateLimitFilter.java:75-78：`onErrorResume` → log.error → `rateLimitExceeded()`（429 拒绝）。
**Paths walked:**
- ✓ Redis 连接异常/超时：Mono 错误信号 → 拒绝（L75-78）
- ✓ 响应序列化再失败：仍返回 429 硬编码兜底体（L109-113），不放行
- ✗（注记）`rate-limit.enabled=false` 或命中限流白名单时整个过滤器跳过（L42-52）——属开关/豁免设计，非 Redis 异常路径，不折损本条
**How the verdict was reached:** 异常路径明确走拒绝分支且无任何 `chain.filter` 逃逸；以 429 而非 503 拒绝属 spec 未规定的细节。

## REQ-08 — 超限返回 429 Too Many Requests

> "// 超限返回 429 Too Many Requests" — spec §4.1 代码块

**Verdict:** implemented · confidence: high
**What this demands:** 超过阈值的请求收到 HTTP 429。
**Where enforcement lives:** RateLimitFilter.java:97-98：`setStatusCode(HttpStatus.TOO_MANY_REQUESTS)`；JSON 体 `{code:429,message,timestamp}`（L100-104）。
**Paths walked:** ✓ count > limit → 429 + JSON（L70-71, L97-108）。
**How the verdict was reached:** 状态码与响应体逐行可证。

## REQ-09 — JWT 白名单 = {/auth/login, /auth/register, /doc.html, /v3/api-docs/**}

> "1. 排除白名单路径（`/auth/login`, `/auth/register`, `/doc.html`, `/v3/api-docs/**`）" — spec §4.2

**Verdict:** contradicted · confidence: high
**What this demands:** 免鉴权路径**恰为**这 4 项；其余一律验 Token。
**Where enforcement lives:** JwtAuthFilter.java:54-61 白名单实际为 6 项：`/auth/**`、**`/system/tenants/**`**、`/v3/api-docs/**`、`/swagger-ui/**`、`/webjars/**`、`/doc.html`（AntPathMatcher 匹配，L114-116）。
**Paths walked:**
- ✗ `/system/tenants/**`（spec 完全未列）：匿名请求穿过网关直达 system 服务。当前被下游 `SecurityConfig` 的 `anyRequest().authenticated()` 挡为 401（schemaplexai-system/.../security/SecurityConfig.java:20-23，且 system 无任何 header 认证 filter：grep `addFilter|PreAuthenticated|OncePerRequestFilter` → 仅 SecurityFilterChain 声明本身），**但网关信任边界已破**——防线整体转移到下游单点配置
- ✗ `/auth/**` 宽于 {login, register}：`/auth/logout`、`/auth/change-password` 也免鉴权；又因白名单分支强制剥离 `X-User-Id`（JwtAuthFilter.java:69-75），这两个需要身份的端点经网关调用时永远拿不到身份（change-password 直接 401，AuthController.java:59-62；logout 收到 null userId，AuthController.java:49-51）→ 白名单过宽同时造成功能性破坏
- ✓ `/doc.html`、`/v3/api-docs/**` 覆盖 spec 项；`/swagger-ui/**`、`/webjars/**` 为文档配套超集（低危）
- ✓ 缓解（stronger-than-spec 行为）：白名单路径注入头被剥离（L69-75），攻击者无法借白名单伪造 `X-User-Id`/`X-Tenant-Id`
- ✗ 旁证：spec 列的 `/auth/register` 在 system 侧根本无端点（grep `register` AuthController.java → 0 命中）——spec 与实现双向漂移
**Searched:** `"/auth/login"`、`"/auth/register"`（gateway src/main）→ 0 命中（无精确路径白名单）；`system/tenants`（gateway）→ JwtAuthFilter.java:56 1 命中。
**How the verdict was reached:** 非 partial：不是"少实现了某项"，而是免鉴权面**扩大**（尤其 `/system/tenants/**` 属业务数据路径），与"恰为 4 项"直接冲突。网关为信任边界，从严定 High（当前靠下游 Spring Security 兜底，一次下游配置改动即暴露）。

## REQ-10 — 从 Authorization Header 提取 Token

> "2. 从 `Authorization` Header 提取 Token" — spec §4.2

**Verdict:** implemented · confidence: high
**What this demands:** 仅接受标准 `Authorization: Bearer <token>`；缺失/畸形按未认证处理。
**Where enforcement lives:** JwtAuthFilter.java:118-124（`resolveToken`：取 `Authorization` 首值，校验 `Bearer ` 前缀后截取）；缺失 → 401（L79-81）。
**Paths walked:**
- ✓ 规范 Bearer → 提取成功
- ✓ 无 Header / 空值 / 无 `Bearer ` 前缀 → null → 401（L79-81, L120-123）
- ✓ 不存在 URL 参数取 Token 的旁路（文件内无 query 读取）
**How the verdict was reached:** 提取与拒绝路径齐备，无替代来源。

## REQ-11 — 验证 Token 签名与过期时间

> "3. 验证 Token 签名、过期时间" — spec §4.2

**Verdict:** implemented · confidence: high
**What this demands:** 篡改（签名不符）与过期 Token 均被拒。
**Where enforcement lives:** JwtAuthFilter.java:84-85：`Jwts.parser().verifyWith(key).build().parseSignedClaims(token)`（jjwt 0.12：verifyWith 同时校验签名与 exp）。
**Paths walked:**
- ✓ 过期 → `ExpiredJwtException` → 401 "token expired"（L105-107）
- ✓ 篡改/错密钥/畸形 → 通用 `Exception` → 401 "token invalid"（L108-111）
- ✓ 密钥来源与签发侧一致（同 `jwt.secret`；签发侧 JwtTokenProvider.java:54, 92-98 同为 HMAC-SHA）
**How the verdict was reached:** 两类失败路径显式分支且都终止请求；无 `validateToken` 吞异常放行的模式。

## REQ-12 — 解析 userId、tenantId

> "4. 解析 `userId`, `tenantId`" — spec §4.2

**Verdict:** implemented · confidence: high
**What this demands:** 从合法 Token 中取出用户与租户标识，与签发侧字段约定一致。
**Where enforcement lives:** JwtAuthFilter.java:87-88：`userId = claims.getSubject()`、`tenantId = claims.get("tenantId", String.class)`。签发侧对应：JwtTokenProvider.java:58-59（`subject(userId)`、`claim("tenantId", tenantId)`）——字段严格互配。
**Paths walked:**
- ✓ 登录签发的 Token：login 强制非空 tenantId（AuthService.java:51-53）→ 两字段恒可解析
- ✓ tenantId 缺失容忍：`StringUtils.hasText` 判空后跳过注入（JwtAuthFilter.java:98-100），不抛错（后果由 REQ-19 承接）
**How the verdict was reached:** 双侧字段逐行核对一致；无字段名漂移。

## REQ-13 — 注入下游 Header：X-User-Id / X-Tenant-Id / Authorization 透传

> "5. **注入下游 Header**: `X-User-Id`: 用户 ID / `X-Tenant-Id`: 租户 ID / `Authorization`: Bearer Token（透传）" — spec §4.2

**Verdict:** implemented · confidence: high
**What this demands:** 认证成功后下游收到三个头，且值来自已验证的 Token。
**Where enforcement lives:** JwtAuthFilter.java:90-102：先删除入站 `X-Tenant-Id`/`X-User-Id`（L91-94，防伪造，stronger-than-spec），再注入 `Authorization: Bearer <token>`(L95)、`X-User-Id`(L96)、`X-Tenant-Id`（有值时，L98-100）。头名常量：CommonConstants.java:8/12/15-16。
**Paths walked:**
- ✓ 正常 Token → 三头齐备
- ✓ 客户端预置伪造 `X-User-Id` → 先删后注，下游只见 Token 派生值（L91-94）
- ✗（注记）Token 无 tenantId claim → `X-Tenant-Id` 缺失下发（L98 条件注入）→ 由 REQ-19 裁决
- ✓ 下游消费验证：system `PermissionAspect` 读 `X-User-Id`（PermissionAspect.java:63-65，缺失即拒）
**How the verdict was reached:** 注入点、常量、下游消费三方对齐；条件注入是 REQ-19 的问题域而非本条缺口。

## REQ-14 — 单次构建 ServerHttpRequest

> "必须单次构建 `ServerHttpRequest`（避免多次 mutate 导致 Header 丢失）" — spec §4.2 关键约束

**Verdict:** implemented · confidence: high
**What this demands:** JwtAuthFilter 内对请求只做一次 mutate/build。
**Where enforcement lives:** 认证分支：单一 builder 链 `request.mutate()...build()` 一次完成（JwtAuthFilter.java:90-102，唯一 `build()` 在 L102）；白名单分支亦单次（L69-75）。
**Paths walked:** ✓ 白名单分支 1 次 mutate；✓ 认证分支 1 次 mutate；✗ 无第二次 mutate（文件内 `mutate()` 仅 L69、L90 两处，分属互斥分支）。
**How the verdict was reached:** 逐分支计数核实。（跨过滤器看，TenantResolveFilter/TracePropagationFilter 各自再 mutate 一次，但 spec 约束限定于 JwtAuthFilter 内部单次构建，不构成违反。）

## REQ-15 — Token 过期返回 401，格式为 JSON

> "Token 过期返回 401，格式为 JSON" — spec §4.2 关键约束

**Verdict:** implemented · confidence: high
**What this demands:** 过期路径响应 401 且 body 为 JSON（非纯文本/空体）。
**Where enforcement lives:** JwtAuthFilter.java:105-107 → `unauthorized()`：L127 状态 401、L128 `Content-Type: application/json`、L129-137 ObjectMapper 序列化 `{code:401, message:"token expired", timestamp}`（message 出自 ResultCode.java:23）。
**Paths walked:** ✓ 过期 → 401 JSON；✓ 缺失/篡改同走 401 JSON（L80, L110）；✓ 序列化极端失败 → `setComplete()` 空体 401（L138-140，仍是 401）。
**How the verdict was reached:** 状态码、Content-Type、序列化方式三点齐备。spec §6 示例文案 "Unauthorized: token expired" 与实际 "token expired" 仅措辞差异，§6 约束的是结构而非文案。

## REQ-16 — TenantResolveFilter 在 JwtAuthFilter 之后

> "**优先级**: 在 JwtAuthFilter 之后" — spec §4.3

**Verdict:** implemented · confidence: high
**What this demands:** 租户解析执行时 JWT 已完成校验与注入。
**Where enforcement lives:** JwtAuthFilter order -100（JwtAuthFilter.java:145） < TenantResolveFilter order -90（TenantResolveFilter.java:50）→ 后者后执行。
**Paths walked:** ✓ 认证请求到达 TenantResolveFilter 时 `X-Tenant-Id` 已由 JwtAuthFilter 注入（可在 L21 读到）。
**How the verdict was reached:** 数值序即执行序（Spring Cloud Gateway GlobalFilter 语义）。注：agents.md 写 TenantResolveFilter order -200（先于 JWT）与代码不符——agents.md 漂移，spec 与代码一致。

## REQ-17 — X-Tenant-Id 存在时验证租户存在性（缓存查询）

> "if (X-Tenant-Id Header 存在): 验证租户存在性（缓存查询） 传递至下游" — spec §4.3

**Verdict:** absent · confidence: high
**What this demands:** 网关对声明的租户做存在性（隐含含有效性/启用态）校验，不存在则拒绝。
**Where enforcement lives:** 无。TenantResolveFilter.java:34-42 仅做格式告警（长度>128 只 `log.warn` 仍放行，L35-37）后原样透传。
**Paths walked:**
- ✗ 携带不存在租户 ID 的合法 Token → 网关放行至下游
- ✗ 已禁用租户（ResultCode.TENANT_DISABLED, ResultCode.java:20）→ 网关无从拦截
**Searched:**
- `exist|TENANT_NOT_FOUND|sf_tenant|tenantService|tenantCache`（gateway src/main）→ 0 命中
- `Redis|redisTemplate`（gateway filter 包）→ 仅 RateLimitFilter.java:33 限流用途，无租户查询
- gateway pom.xml → 无 dao/model 依赖（pom.xml:13-54），网关物理上无法查库，也未见任何租户缓存客户端
**How the verdict was reached:** 非 partial：连降级形态的校验（如 Redis 缓存查询）都不存在，格式告警不拦截不构成"验证存在性"。严重度 Medium：伪造/已注销租户流量抵达下游，租户禁用无法在边缘生效；越权读数还需下游 TenantLineInterceptor 同时失效，故未到 Critical。

## REQ-18 — Token 中包含 tenantId 时自动注入

> "else if (Token 中包含 tenantId): 自动注入" — spec §4.3

**Verdict:** implemented · confidence: high
**What this demands:** Header 未带租户但 Token 含 tenantId 时，下游仍收到 `X-Tenant-Id`。
**Where enforcement lives:** 实际由 **JwtAuthFilter** 完成（JwtAuthFilter.java:98-100：claim 有值即注入 Header），先于 TenantResolveFilter 执行 → 到达后者时 Header 已存在，走 L21→L34-42 透传分支。机制与 spec 描述的"TenantResolveFilter 注入"不同但净效果一致（文档漂移注记）。
**Paths walked:**
- ✓ Token 含 tenantId、Header 未带 → JwtAuth 注入 → 下游收到
- ✗（死代码注记）TenantResolveFilter.java:25 读 `exchange.getAttribute(CONTEXT_TENANT_ID)` 作为"Token 回退"——全仓库该 attribute 仅 TenantResolveFilter.java:41 自己写入（grep `CONTEXT_TENANT_ID` → 3 处：常量定义 CommonConstants.java:9、读 L25、写 L41），JwtAuthFilter 从不写入 → 此回退分支永远取 null，不可达
**How the verdict was reached:** 按任务准则"不同机制满足需求 = implemented"。死代码不改变外部行为，但应在文档或代码中消解（Open questions）。

## REQ-19 — 无租户信息时返回 400 Bad Request

> "else: 返回 400 Bad Request（缺少租户信息）" — spec §4.3

**Verdict:** contradicted · confidence: high
**What this demands:** Header 与 Token 均无租户 → 网关在边缘拒绝（400），不放行至下游。
**Where enforcement lives:** 反向证据：TenantResolveFilter.java:28-31 注释明言 "No tenant ID anywhere — downstream services will validate/block as needed" 且仅 `log.debug`；L45 `return chain.filter(exchange)` 无租户直接放行。文件内无任何 400/`HttpStatus.BAD_REQUEST` 构造。
**Paths walked:**
- ✗ 合法 Token 无 tenantId claim + 无 Header → 放行，下游收不到 `X-Tenant-Id`
- ✓（缓解）现有签发路径恒嵌入非空 tenantId（AuthService.java:51-53 登录强校验；refresh 同样携带 AuthService.java:65-67）→ 该失败路径今天需异常签发才触发
- ✗（spec 内在冲突注记）白名单路径（如 `/auth/login`）经 JwtAuthFilter 剥头后必然无租户——若严格按 spec 返回 400，登录将永久 400。spec 该条与 §4.2 白名单叠加后自相矛盾，代码的放行是对矛盾的单方面消解
**Searched:** `BAD_REQUEST|400`（gateway src/main/java）→ 0 命中（仅 429/401 构造存在）。
**How the verdict was reached:** 非 absent：不是漏写，而是注释明示的**反向决策**（依赖下游校验），与 spec 指令直接冲突。严重度 Medium：边缘拒绝这道防线缺失，但当前签发侧恒有租户 + 下游部分端点自校验（TenantPolicyController.java:64-67 对缺头返回 PARAM_ERROR）+ DAO TenantLineInterceptor 兜底。

## REQ-20 — 日志内容：请求 method/path/query/userId/tenantId/timestamp；响应 status/duration_ms

> "- 请求: method, path, query, userId, tenantId, timestamp / - 响应: status, duration_ms" — spec §4.4

**Verdict:** partial · confidence: high
**What this demands:** 访问日志包含身份维度（userId/tenantId），可支撑按租户/用户审计。
**Where enforcement lives:** LoggingFilter.java:28-35（method/path/query + 日志时间戳 + clientIp）；L37-42（status + duration ms）。
**Paths walked:**
- ✓ method/path/query/timestamp/status/duration 全记录
- ✗ userId：全文件无引用（结构性原因：LoggingFilter order = MIN_VALUE，先于 JwtAuthFilter 执行，请求期身份未知；且响应期 `doFinally` 也未回读任何身份 attribute）
- ✗ tenantId：同上，全文件无引用
**Searched:** `userId|tenantId|X-User-Id|X-Tenant-Id`（LoggingFilter.java）→ 0 命中。
**How the verdict was reached:** 六项记录四项，身份两项系统性缺失 → partial 而非 implemented；也非 contradicted（无相反行为）。严重度 Low（审计溯源缺口）。

## REQ-21 — 敏感信息脱敏：password、token、secret

> "- 敏感信息脱敏: password, token, secret" — spec §4.4

**Verdict:** absent · confidence: high
**What this demands:** 日志输出前对敏感字段做遮蔽。
**Where enforcement lives:** 无。LoggingFilter.java:35 将 query 字符串**原样**打印（`{}?{}`）——若 query 携带 `?token=...`/`?password=...` 将全量落日志。
**Paths walked:**
- ✗ `GET /x?token=eyJ...` → token 明文进日志（L35）
- ✓（面收窄）不记录 Header 与 Body，Authorization 头不落日志
- ✗ 关联风险：agents.md 记载前端已知缺陷"JWT Token 被放入 URL 参数"——与本缺口叠加即形成实际凭据落盘链路
**Searched:**
- `mask|redact|sanitiz|desensit`（gateway src/main，忽略大小写）→ 0 命中
- `PiiRedactor`（common 中存在，PiiRedactor.java，用于 OTel span 导出）→ gateway 无引用（grep gateway src → 0 命中）
**How the verdict was reached:** 非 partial：无任何脱敏代码，仅靠"少记字段"被动收窄。结合前端 token-in-URL 现状定 Medium。

## REQ-22 — CORS 配置（CORS_ORIGINS 环境变量、方法集、headers*、credentials）

> "allowedOrigins: \"${CORS_ORIGINS:http://localhost:3000}\" / allowedMethods: GET, POST, PUT, DELETE, OPTIONS / allowedHeaders: \"*\" / allowCredentials: true" — spec §5.1

**Verdict:** partial · confidence: high
**What this demands:** 全局 CORS 存在；源可经 `CORS_ORIGINS` 环境变量外部化，默认 localhost:3000；方法集为列出的 5 种。
**Where enforcement lives:** application.yml:14-29。
**Paths walked:**
- ✓ 全局 CORS 存在，`allowedHeaders: "*"`(L27-28)、`allowCredentials: true`(L29)
- ✗ 源硬编码列表 `[http://localhost:5173, http://localhost:3000]`（L17-19），**无** `${CORS_ORIGINS}` 占位 → 生产改源需改文件/用 Spring 外部化配置覆盖，且多出 5173（Vite 默认端口，工程合理但 spec 未列）
- ✗ 方法多出 `PATCH`（L26），超集
**Searched:** `CORS_ORIGINS`（全仓库 yml/java/properties）→ 0 命中。
**How the verdict was reached:** 核心防护在且更宽松项（origins/methods 超集）均为本地开发源，非任意放开（无 `*` origin + credentials 的危险组合）→ partial 而非 contradicted。严重度 Low（配置外部化漂移）。

## REQ-23 — JWT Secret：@PostConstruct 校验 ≥32、来自 JWT_SECRET、禁止硬编码默认值

> "if (secret == null || secret.length() < 32) { throw new IllegalStateException…} / Secret 必须从环境变量 `JWT_SECRET` 读取 / **禁止**在配置文件中硬编码默认值" — spec §5.2

**Verdict:** implemented · confidence: high
**What this demands:** 弱/缺失密钥必须阻断启动；配置无默认值兜底。
**Where enforcement lives:**
- JwtAuthFilter.java:45-50：`@PostConstruct` 校验 UTF-8 字节数 ≥32（字节 ≤ 字符数，判定等价或更严），否则 `IllegalStateException` 阻断启动
- application.yml:44-45：`jwt.secret: ${JWT_SECRET}` —— 无冒号默认值
- 双保险：common 自动装配 `JwtSecretStartupValidator`（ApplicationRunner，JwtSecretStartupValidator.java:36-53，同样 ≥32 字节 fail-fast）
**Paths walked:** ✓ 未设 JWT_SECRET → 占位符无默认 → 属性缺失/空 → 两级校验任一抛异常，进程退出；✓ 31 字节密钥 → 阻断；✓ 32+ 字节 → 通过。
**Searched:**（硬编码排查）`${JWT_SECRET:`（gateway resources）→ 0 命中（无默认值语法）。
**How the verdict was reached:** 三项子约束逐条落地且有冗余防线。

## REQ-24 — 错误响应统一 JSON {code,message,timestamp}，必须 ObjectMapper 序列化

> "Gateway 层错误统一返回 JSON: {\"code\": 401, …} / **禁止**使用 String.format 拼接 JSON，必须使用 `ObjectMapper` 序列化。" — spec §6

**Verdict:** implemented · confidence: high
**What this demands:** 网关自产错误体结构统一为三字段，且经 ObjectMapper 生成。
**Where enforcement lives:** 401：JwtAuthFilter.java:129-137（Map{code,message,timestamp} → `objectMapper.writeValueAsBytes`）；429：RateLimitFilter.java:100-108（同构）。
**Paths walked:**
- ✓ 401（缺失/过期/篡改三入口同一出口 unauthorized()）
- ✓ 429（超限与 Redis 异常同出口）
- ✗（注记）RateLimitFilter.java:111 存在字符串字面量兜底 `"{\"code\":429,...}"` —— 仅在 ObjectMapper 对 `Map.of` 序列化抛异常时触达（实际不可达路径），且为常量拼装而非 String.format 插值，无注入面
**Searched:** `String.format`（gateway src/main/java）→ 0 命中。
**How the verdict was reached:** 主路径全部 ObjectMapper；字面量兜底是防御性死角而非拼接实践，注记不降级。

## REQ-25 — 授权：RBAC 基础检查（网关层）

> "- **授权**: 基于角色的访问控制（RBAC）基础检查" — spec §1 概述

**Verdict:** absent · confidence: medium
**What this demands:** 网关在转发前做某种基于角色的访问判定（spec 未给细则，§4 无对应过滤器规格）。
**Where enforcement lives:** 网关内无。RBAC 全部位于 system 服务：`RbacService.java`、`PermissionEvaluator.java`、`PermissionAspect.java`（基于 `@RequirePermission` 注解 + 网关注入的 `X-User-Id`，PermissionAspect.java:63-73）。
**Paths walked:** ✗ 网关对任意已认证请求不做角色判断，直接路由。
**Searched:** `role|rbac|permission`（gateway src/main/java，忽略大小写）→ 0 命中。
**How the verdict was reached:** confidence 降为 medium 的原因：spec 仅在概述提及、无细则章节，"基础检查"边界不明——若解读为"网关注入身份、下游执行 RBAC"则现状可辩护。按字面（网关承担该职责）裁 absent，严重度 Low（授权在下游有实现，非无防护）。

---

## notChecked

| 项 | spec 位置 | 原因 |
|----|----------|------|
| 性能指标：转发 P99<5ms / JWT P99<2ms / Redis P99<3ms / QPS>5000 | §7 | 运行时特性，静态核查不可判；仓库无基准测试（gateway 测试目录仅功能单测） |
| "Token 刷新" 作为网关职责 | §1 | 网关自身无刷新逻辑；`/auth/refresh` 由 system 提供（AuthController.java:40-44）并经 `/auth/**` 路由+白名单可达。spec 未说明网关是"实现"还是"转发"该能力，无法单边裁决 |
| CORS 运行时实际生效行为 | §5.1 | 已按配置静态核查（REQ-22）；预检响应的运行时验证未执行 |

## 反向差距（网关存在、spec 未提及）

1. **TracePropagationFilter**：W3C `traceparent` 生成/透传（TracePropagationFilter.java:14-45，order MIN_VALUE+100）；配套 OTLP→Jaeger 上报配置（application.yml:64-68）。spec §2 过滤器链共 4 个，实际 5 个。
2. **额外路由**：`/sse/**`、`/ws/**` → web（GatewayConfig.java:16）；`/agent/**` → agent-engine（L20）；`/admin/**` → `lb://schemaplexai-admin`（L36-37）——admin 为占位模块无应用（agents.md 明载），该路由指向不存在的服务。
3. **限流白名单与总开关**（application.yml:48-57，RateLimitFilter.java:42-52）：`/auth/**` 等免限流 + `rate-limit.enabled` 可整体关停。与 REQ-03 顺序问题叠加 → **登录/匿名流量在网关层完全无限流**（spec 设计意图是限流最先、全量覆盖）。
4. **入站身份头防伪剥离**（JwtAuthFilter.java:69-75, 91-94）：白名单与认证路径均先删客户端自带 `X-User-Id`/`X-Tenant-Id` —— stronger-than-spec 的正向行为。
5. **IP 维度限流回退**（RateLimitFilter.java:91-94）：无租户请求按来源 IP 计数。
6. **可疑租户 ID 长度告警**（TenantResolveFilter.java:35-37）：仅告警不拦截。
7. **LoggingFilter traceId**（LoggingFilter.java:21-25）：自生成 16 位 traceId 注入 exchange attribute（与 TracePropagationFilter 的 traceparent 双轨并存，互不引用）。

## Open questions

1. REQ-02 的 503 结论基于"仓库内零发现源"（grep 证据齐备）；若部署侧存在仓库外的服务发现（K8s Service DNS + spring-cloud-kubernetes 等），结论需复核——但仓库内（含 docker-compose、agents.md 启动说明）无任何此类基座。
2. 下游 `TenantLineInterceptor` 在 `X-Tenant-Id` 缺失时的具体行为（报错 or 无过滤查询）未核查（超出网关 spec 范围），直接决定 REQ-19 的最坏后果层级。
3. system 服务 `SecurityConfig.anyRequest().authenticated()`（SecurityConfig.java:22）且无任何认证 filter —— 按此代码，即便携带网关注入头的合法请求也应被 system 拒绝，REQ-09 的"下游兜底"缓解与 system 自身可用性互为矛盾，需要 system 侧核查确认。
4. TenantResolveFilter.java:25 的 attribute 回退死代码（REQ-18）：应删除或让 JwtAuthFilter 写入 `CONTEXT_TENANT_ID` attribute，二者择一以消除误导。
5. JwtAuthFilter.java:96：若 Token 无 subject（当前签发侧不会产生），`header(HEADER_USER_ID, (String) null)` 的行为未走查——现实不可达，列此备查。

## 反驳复核

> 独立复核员对全部 severity ∈ {Critical, High} 且 verdict ∈ {contradicted, partial, absent} 的条目（共 3 条）逐条重读代码与规格、沿两个方向尝试推翻，均未发现可推翻的证据，全部维持。

| REQ | 原判 | 裁定 | 理由(一行) | 证据 |
|-----|------|------|-----------|------|
| REQ-02 | partial · High | 维持 | 无静态实例配置、无测试替身（网关无 src/test）、无仓库内配置载体，`lb://` 解析为空 → 503 机制经独立复证成立 | 网关唯一配置文件 application.yml（无 profile/bootstrap，通读无发现源）；根 pom 仅 `spring-cloud-contract`（L60、L261-268），无 eureka/nacos/consul/k8s 依赖；本地 .m2 复证 `spring-cloud-starter-gateway-4.1.2.pom` 传递引入 `spring-cloud-starter-loadbalancer:4.1.2`（有解析器、无实例来源）；docker-compose 12 项服务全为基础设施容器；`src/test` 目录不存在 |
| REQ-03 | contradicted · High | 维持 | 五个过滤器 order 逐一复读无误，JwtAuth 无 Token 时 401 短路且不调用 chain.filter，匿名流量到不了排最后的限流器；无其他前置限流机制；顺序与 spec §2 编号链明确相反 | order 复读：LoggingFilter.java:46-48（HIGHEST_PRECEDENCE）、TracePropagationFilter.java:43-45（+100）、JwtAuthFilter.java:143-146（-100）、TenantResolveFilter.java:48-51（-90）、RateLimitFilter.java:117-120（-50）；短路点 JwtAuthFilter.java:78-81；过滤器全集仅 5 个类（filter 包清单），路由无 `.filters(`、无 RequestRateLimiter；spec §2 编号 + §4.3"在 JwtAuthFilter 之后"为规范性顺序 |
| REQ-09 | contradicted · High | 维持 | 白名单为硬编码 6 项（不可被配置覆盖）且免鉴权面扩大至业务路径 `/system/tenants/**`，与"恰为 4 项"直接冲突；下游兜底与功能性破坏两条旁证独立复证成立 | JwtAuthFilter.java:54-61（`private final List` 内联初始化，无 @Value/@ConfigurationProperties 覆盖入口）；system SecurityConfig.java:20-22 `anyRequest().authenticated()` 且 permitAll 清单不含 `/system/tenants/**`，system 模块 grep 认证 filter（addFilter/OncePerRequestFilter/PreAuthenticated）0 命中 → 匿名穿透确被下游 401 挡下（唯一防线）；AuthController.java:59-62 change-password 缺 `X-User-Id` 返回 401、L49-51 logout 取 null userId（白名单分支剥头，JwtAuthFilter.java:69-75） |
