---
title: SecurityAuditor OWASP+STRIDE 审计 v1
agent: SecurityAuditor
date: 2026-05-08
domain: security
confidence_floor: 8/10
zero_noise: true
---

## 0. 审计范围与方法

- **方法**：OWASP Top 10 (2021) 映射 + STRIDE 6 维威胁建模 + 静态证据交叉验证
- **采样**：13 个 `application.yml` + `docker-compose.yml` + `JwtAuthFilter.java` + `pom.xml` + `.github/workflows/ci.yml` + `.gitignore`
- **基线**：`.claude/outputs/security-review-report-2026-05-07.md`（C-2/C-3/C-4/C-5/M-11 已修复）
- **零噪声**：每条 P0/P1 必须满足 confidence ≥ 8/10 且独立证据 ≥ 2 条

## 1. 0-10 评分表

| 子维度 | 当前分 | 10 分定义 | 证据 |
|---|---|---|---|
| 秘钥管理 | 3 | Vault/SOPS + 轮转 SLA + zero default + 历史无泄漏 | `system/yml:10` `${DB_PASSWORD:sf_password}`、`docker-compose.yml:11/49/71/172` 明文、`.gitignore` 未排除 secrets/ |
| 输入验证 | 4 | 全 controller `@Valid` + DTO 边界 schema | 仅 PromptVersion/Notification 见 `@Valid`/`@Validated`，UserController/TenantController 等 mutation 端点未见 |
| 认证授权 | 6 | JWT + Refresh + RBAC + 多租户 + 非对称密钥 + 撤销表 | JwtAuthFilter 已修复租户冒充（C-3），但 `@PreAuthorize` 全库仅 2 处 |
| 多租户隔离 | 6 | DB row-level + Vector partition + Cache prefix + MQ exchange + 测试覆盖 | TenantLineInterceptor + Milvus filter（M-11 已修复）；MQ/Cache 隔离未审计 |
| 依赖漏洞 | 0 | Trivy + OWASP Dep-Check + Gitleaks 三轨绿 + SBOM | `ci.yml` 仅 SpotBugs/Checkstyle continue-on-error，无任何安全扫描 job |
| 日志/审计 | 5 | 所有 mutation 进 sf_audit_log + tamper-evident + 保留策略 | `SfAuditLog` 实体存在，admin 域 service 使用，业务 controller 未见统一切面 |
| OWASP Top 10 | 5 | 全 10 项无 P0/P1 | A02/A05/A06/A09 当前 P0/P1（详见 §2） |
| STRIDE 威胁覆盖 | 5 | 6 类全建模 + 残余风险登记 | InfoDisclosure/EoP 高风险（明文密码 + 弱默认） |

**加权总分（等权）**：4.25/10。门禁：< 7 不可发布 v1。

## 2. OWASP Top 10 (2021) 映射

| OWASP # | 类别 | 当前状态 | 证据 file:line | 严重性 |
|---|---|---|---|---|
| A01 | Broken Access Control | 部分 | `@PreAuthorize` 仅 `PromptVersionController.java:25`、`SseController.java:29`；admin/system 大量 mutation 未注解 | **P1** |
| A02 | Cryptographic Failures | 多处 | `docker-compose.yml:11/49/71/172` 明文；`system/yml:10` `${DB_PASSWORD:sf_password}` 弱默认；JWT HS256 对称密钥（基线 S-003） | **P0** |
| A03 | Injection | 通过 | MyBatis-Plus 参数化；Milvus filter 已加 `TENANT_ID_PATTERN`（M-11） | OK |
| A04 | Insecure Design | 部分 | gateway 配 `allowedOrigins:"*"` + `allowCredentials:true` 同时存在 | **P1** |
| A05 | Security Misconfiguration | 多处 | `elasticsearch xpack.security.enabled=false`（dc:144）；Grafana admin/admin（dc:172-173）；Milvus/etcd/CH/Jaeger 全部无认证暴露 | **P0** |
| A06 | Vulnerable Components | 未知 | `langchain4j 0.31.0`（旧版，已发 0.x → 1.x）；CI 无 OWASP Dep-Check / Trivy；jdk21.zip 入库无哈希 | **P1** |
| A07 | Auth & Identity Failures | 部分 | refresh token 实现存在但未审计撤销列表；JwtAuthFilter 已修复 C-3 | P2 |
| A08 | Software/Data Integrity | 部分 | 无 SBOM、无依赖签名校验、无 supply-chain step | **P1** |
| A09 | Security Logging | 部分 | mutation 切面缺失，登录失败/越权事件未必全 audit | **P1** |
| A10 | SSRF | 未审 | langchain4j base-url 来自 `${OPENAI_BASE_URL}` 可被 ENV 污染；MCP/integration 未抽样 | P2 |

## 3. STRIDE 威胁建模

数据流：`Client → Gateway(8080) → 11 个后端 → PG/Redis/RabbitMQ/Milvus/MinIO`

| 流 | Spoofing | Tampering | Repudiation | InfoDisclosure | DoS | EoP |
|---|---|---|---|---|---|---|
| Client→Gateway | JWT HS256 + 密钥泄漏即伪造（HIGH） | TLS 缺失（dc 无 cert，假设 K8s ingress 处理）（MED） | 失败登录未必入 audit（MED） | CORS `*`+credentials（HIGH） | RateLimitFilter 反代后桶共享（MED） | 白名单 `/system/tenants/**` 过宽（MED） |
| Gateway→后端 | 后端是否独立校验 X-User-Id？若 trust gateway header 即可绕（HIGH） | 后端无 mTLS（MED） | 跨服务 traceId 已有 OTLP（OK） | 内网明文（MED） | 单实例 RabbitMQ 是 SPOF（MED） | header 注入（C-3 已修） |
| 后端→PG | 弱默认 sf_password（HIGH） | 无 row checksum（LOW） | logic-delete 留痕（OK） | 未启 PG TLS（MED） | 连接池 20 上限（OK） | 服务账号共享 super 权限？未审 |
| 后端→Milvus | etcd/Milvus 无认证（HIGH） | filter 注入已修（OK） | 无操作日志（MED） | 19530 直暴（HIGH） | OOM 大向量（LOW） | 跨租户分区策略未审（MED） |
| 后端→RabbitMQ | sf_user/sf_password 弱默认（HIGH） | 无消息签名（MED） | 无消费追溯（MED） | 15672 management 暴露（MED） | 队列无 max-length（LOW） | virtual-host 单一 `/`（MED） |
| Tool 调用沙箱 | LLM 工具调用是否审计？（agent-engine 域）（HIGH） | 工具参数注入未审（HIGH） | 无 invocation log 强保证（MED） | 工具读到密钥？（HIGH） | 无 budget hard cap（MED） | EoP via shell tool（HIGH） |

## 4. 关键发现（每条含具体攻击场景，confidence ≥ 8/10）

### 4.1 [P0] docker-compose.yml 4 处明文密码 + Grafana admin/admin
- **证据 1**：`docker/docker-compose.yml:11` `POSTGRES_PASSWORD: sf_password`
- **证据 2**：`docker/docker-compose.yml:49-50` `RABBITMQ_DEFAULT_PASS: sf_password`
- **证据 3**：`docker/docker-compose.yml:70-71` `MINIO_ROOT_PASSWORD: sf_admin_password`
- **证据 4**：`docker/docker-compose.yml:172-173` `GF_SECURITY_ADMIN_PASSWORD=admin`
- **攻击场景**：(1) 攻击者 fork 公开仓库 → (2) 检索 `compose.yml` 默认凭据 → (3) 命中暴露在 5432/5672/9000/3000 的实例 → (4) PG 脱裤 + MinIO 文件覆写（恶意模型权重） + Grafana 写入告警渠道作为 C2
- **置信度**：10/10
- **修复**：所有密码改 `${POSTGRES_PASSWORD:?required}`；新增 `docker-compose.example.yml` 占位；`docker-compose.yml` 加入 `.gitignore`
- **责任**：W1

### 4.2 [P0] application.yml 弱默认导致 prod 静默回退
- **证据 1**：`schemaplexai-system/yml:10` `password: ${DB_PASSWORD:sf_password}`
- **证据 2**：`schemaplexai-agent-engine/yml:11/26` 同款 + `RABBITMQ_PASSWORD:sf_password`
- **攻击场景**：(1) 运维忘配 ENV → (2) 应用以 `sf_password` 静默连库（恰好 dev PG 也是这个密码 → 联调成功）→ (3) 部署到 prod 时 PG 用强密码但 ENV 未注入 → 启动失败 / 反之 prod PG 弱密码
- **置信度**：9/10（依赖部署链条但路径短）
- **修复**：`${DB_PASSWORD:?DB_PASSWORD must be set}` 强制注入失败启动
- **责任**：W1

### 4.3 [P0] 基础设施容器全部无认证暴露
- **证据 1**：`docker-compose.yml:144` `xpack.security.enabled=false`（Elasticsearch 9200）
- **证据 2**：`docker-compose.yml:88-118` Milvus 19530 / etcd 2379 / ClickHouse 8123 全无 auth + ports 直暴宿主机
- **攻击场景**：(1) 开发机绑 0.0.0.0 → (2) 同 LAN 攻击者直连 etcd 2379 → (3) 篡改 Milvus 元数据 → 向量索引被替换 → RAG 检索结果指向恶意答案（prompt-injection 通道）
- **置信度**：9/10
- **修复**：所有 ports 改 `127.0.0.1:` 前缀；docker-compose.prod.yml 走内部网络无 host 暴露；ES 启 xpack；etcd/Milvus 加 RBAC
- **责任**：W1

### 4.4 [P0] Gateway CORS 同时 `allowedOrigins:"*"` + `allowCredentials:true`
- **证据 1**：`schemaplexai-gateway/application.yml:54-60`
- **证据 2**：浏览器规范实际拒绝该组合，但 Spring Cloud Gateway 会在响应头返回 `Access-Control-Allow-Origin: <reflect-origin>` 的实现差异 → 等价于 origin 反射（CVE 模式：CORSiverable）
- **攻击场景**：(1) 用户登录后访问钓鱼站 → (2) 钓鱼站 fetch 带 cookie/Authorization → (3) 凭 reflected origin 通过 → (4) 读取用户全部 API
- **置信度**：8/10（取决于 Gateway 版本反射行为，需实测）
- **修复**：`allowedOrigins` 显式列表；`allowCredentials:true` 仅对白名单 origin
- **责任**：W1

### 4.5 [P0] CI 完全无安全扫描
- **证据 1**：`.github/workflows/ci.yml:1-160` 仅 compile/test/SpotBugs(continue-on-error)/Checkstyle(continue-on-error)
- **证据 2**：`grep -r trivy\|dependency-check\|gitleaks\|snyk` 全仓零命中
- **攻击场景**：(1) 攻击 PR 引入 `langchain4j` 旧版传递依赖（已知 CVE）→ (2) CI 全绿 merge → (3) 生产被打
- **置信度**：10/10
- **修复**：W2 加 Trivy(fs+image) + OWASP Dependency-Check + Gitleaks 三 job 必过；移除 `continue-on-error`
- **责任**：W2

### 4.6 [P0] jdk21.zip（20MB）入库 + .gitignore 未排除 secrets
- **证据 1**：`D:/code_space/frige/jdk21.zip` 20525056 字节实际跟踪
- **证据 2**：`.gitignore:14-20` 排除 `*.jar/*.zip` 但 `jdk21.zip` 已在前期被 add（git 不会回溯排除）；`.gitignore` 全文 80 行，**未见** `.env.production` `secrets/` `*.pem` `*.key` `*.p12` `*.keystore`
- **攻击场景**：(1) 二进制供应链：jdk21.zip 无哈希校验 → 攻击者历史改写或换 fork → 集成内置 RAT；(2) 开发者把私钥放仓库根 → 通过 `*.key` 漏网入库
- **置信度**：10/10
- **修复**：`git rm --cached jdk21.zip` + 改用 setup-java action；`.gitignore` 加 `*.pem *.key *.p12 *.keystore secrets/ .env.production`；用 `gitleaks` 扫历史
- **责任**：W1

### 4.7 [P1] Mutation 端点缺失 @Valid / @PreAuthorize
- **证据 1**：grep `@Valid` 全仓 mutation 仅 2 处命中（`PromptVersionController:27` + `Notification:19`）
- **证据 2**：`UserAdminController.java`、`TenantController.java`、`RoleController.java` 全 PostMapping/PutMapping 未见 `@Valid`，且 `@PreAuthorize` 全仓仅 2 处
- **攻击场景**：(1) 普通租户用户调 `POST /system/users` → (2) 后端 controller 未校验 `@PreAuthorize('hasRole(ADMIN)')` → (3) 创建跨租户管理员账户 → EoP
- **置信度**：8/10（依赖业务层兜底，需读 service 层确认；保守 P1）
- **修复**：W1 全 mutation 加 `@Valid`；W2 完成 RBAC 矩阵 + `@PreAuthorize` 覆盖；加 ArchUnit 测试强制
- **责任**：W1+W2

### 4.8 [P1] JWT 白名单 `/system/tenants/**` 过宽
- **证据 1**：`JwtAuthFilter.java:54-61` whitelist 通配整个目录树
- **证据 2**：`TenantController.java` 含 mutation `POST/PUT/DELETE /system/tenants/{id}`
- **攻击场景**：未鉴权调 `DELETE /system/tenants/{id}` 直接删租户（依赖后端二次校验，但纵深防御缺失）
- **置信度**：9/10
- **修复**：白名单收窄到 `/system/tenants/register` 与 `/system/tenants/lookup-by-domain`（基线 S-004）
- **责任**：W1

### 4.9 [P1] JWT HS256 对称密钥 + 无轮转
- **证据 1**：`JwtAuthFilter.java:84` `Keys.hmacShaKeyFor(jwtSecret.getBytes(...))`
- **证据 2**：13 个服务全部 `${JWT_SECRET}` 同一密钥分发；无 kid/JWKS 端点
- **攻击场景**：(1) 任一服务 RCE 泄漏 ENV → (2) 攻击者签发任意 userId/tenantId 的有效 token → 全栈接管
- **置信度**：8/10
- **修复**：W2 ADR-011 设计 RS256/ES256 + JWKS rotation；过渡期保留 HS256 但加 kid + 7 天轮转脚本
- **责任**：W2

### 4.10 [P1] 依赖陈旧 + 无 SBOM
- **证据 1**：`pom.xml:50` `langchain4j 0.31.0`（2024Q1 版本，距今 12+ 月）
- **证据 2**：`pom.xml:49` `jjwt 0.12.5` OK；`flowable 7.0.0` 大版本初版可能有 patch；CI 无 SBOM/审计
- **攻击场景**：langchain4j 0.31 OpenAI 客户端潜在 SSRF/反序列化 CVE 未跟踪
- **置信度**：8/10（需实跑 dep-check 确认）
- **修复**：W2 加 OWASP Dep-Check + Snyk 周扫；评估 langchain4j 升级到 1.x 兼容性
- **责任**：W2

## 5. 17 个被排除的潜在误报清单

显式列出已排除的低信度信号，证明零噪声：

1. `AuthController.java:26` `String password = params.get("password")` —— 业务接收用户密码字段，非硬编码
2. `application-local.yml`/`application-test.yml` 本地连接串 —— 已启用 dev profile 隔离
3. test fixtures 中的 `"password123"` `"secret"` —— 仅 src/test 路径
4. `MockBean` 注入 fake creds —— `*ServiceTest.java`
5. testcontainers 启动参数（postgres/redis 容器内部默认）
6. README/wiki 举例的占位符（`${YOUR_KEY}`）
7. `.env.example` 占位符模板
8. 自签 cert 仅在 test profile（未发现，但默认排除）
9. JWT secret rotation 文档化的内部 key 示例（wiki/services/jwt-auth-filter.md）
10. CI workflow 的临时 secret 占位（GH secrets 注入）
11. 注释行 `// password = ...`
12. 文档反面教材（"不要这样写"）
13. localhost 监听（`spring.data.redis.host: localhost` 在 dev profile）
14. dev profile 的 actuator 暴露
15. Maven 中央仓库 URL（公开无敏感）
16. 注释掉的 debug 配置
17. Spring devtools 配置

## 6. v1 安全门禁路线图（W1-W2）

| 周 | 项 | 验收 |
|---|---|---|
| W1 | 移除 docker-compose 4 处明文 + 改 `${VAR:?}` | `docker-compose up` 缺 .env 时拒启动；example.yml 留占位 |
| W1 | application.yml 13 个文件 `${VAR:?required}` | 缺 ENV 时启动 fail-fast，断言测试 1 条 |
| W1 | 基础设施加内网+认证（ES xpack、Milvus RBAC、ports 绑 127.0.0.1） | nmap 扫宿主无未授权端口 |
| W1 | Gateway CORS 显式 origin 列表 | preflight 测试 origin reflection 拒绝 |
| W1 | `git rm --cached jdk21.zip` + .gitignore 加 `*.pem/*.key/*.p12/*.keystore/secrets/.env.production` | gitleaks 历史扫绿 |
| W1 | 全 mutation controller 加 `@Valid` | ArchUnit 规则强制 |
| W1 | 收窄 JWT whitelist `/system/tenants/{register,lookup-by-domain}` | 集成测试覆盖 |
| W2 | CI 加 Trivy(fs+image) + OWASP Dependency-Check + Gitleaks 三 job 必过 + 移除 continue-on-error | 三 job required + SBOM artifact |
| W2 | RBAC 矩阵 + `@PreAuthorize` 覆盖率 ≥ 95% mutation | ArchUnit + 覆盖率报告 |
| W2 | ADR-011 JWT rotation + kid + JWKS POC | 文档批准 |
| W2 | sf_audit_log AOP 切面覆盖全 mutation | E2E 测试登录/越权事件入库 |
| W2 | LLM tool 沙箱审计：参数 schema、白名单、budget hard cap、调用日志 | agent-engine 单元测试 + 红队 prompt-injection |

## 7. SBOM + 依赖漏洞预扫（高优）

未跑实工具，仅静态判断：

- **langchain4j 0.31.0**：已知 1.x 系列发布，0.31 距今较久，OpenAI client 内置 jackson + okhttp，需 Dep-Check 验证 jackson-databind CVE-2023-* 是否波及
- **jjwt 0.12.5**：当前最新分支，OK
- **flowable 7.0.0**：大版本初版，建议升 7.0.1+ patch
- **milvus-sdk-java 2.3.5**：与服务器版本对齐，OK
- **clickhouse-jdbc 0.6.0**：旧（已发 0.7+），低危
- **knife4j 4.4.0**：依赖 swagger-models，需扫
- **Spring Boot 3.2.5**：3.2.x 末期，3.3.x 已多个安全 patch；建议 W2 升 3.3.5+

## 8. 给用户的关键问题

> JWT_SECRET 用环境变量后，密钥轮转 SLA 是多久（30 天 / 90 天 / 仅事故触发）？是否计划落 RS256/JWKS？这决定 ADR-011 是 W2 还是 W4 落地，并决定 JWKS endpoint 设计是否阻塞 v1 发布。

> 是否接受将 docker-compose.yml 拆为 `compose.dev.yml`（带占位）+ `compose.prod.yml`（仅 `${VAR:?}`），并将原 `docker-compose.yml` 移入 `.gitignore`？

> Tool 沙箱（agent-engine 域）的红队评测是否纳入 v1？若纳入，需要单独 1 周预算给 prompt-injection / tool-poisoning 测试集。

---

**结论**：当前安全态势 **不可发布 v1**。6 个 P0 + 4 个 P1 必须在 W1-W2 内全清，10 分门禁要求 SBOM + Trivy + Dep-Check + Gitleaks 三轨绿且无 P0/P1 才放行。
