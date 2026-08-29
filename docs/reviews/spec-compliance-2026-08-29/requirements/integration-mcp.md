# Spec-to-Code 合规核查 — Integration 层 & MCP 工具发现

- 核查日期: 2026-08-29(本轮为独立重查,覆盖并替换旧稿)
- 权威 spec:
  - `docs/specs/2026-04-30-v1.0-integration-layer.md`(下称 **SPEC-INT**;注意文件头 `status: 草稿`,与任务陈述"已批准"不一致,见 Open questions)
  - `docs/specs/2026-05-07-v1.0-mcp-tool-discovery.md`(下称 **SPEC-MCP**;文件头 `status: completed` —— 重点验证该宣称)
- 核查对象: `schemaplexai-integration/src/main/java`;执行点延伸至 `schemaplexai-agent-engine/src/main/java/.../tool/mcp`(MCP 发现与消费侧)、`schemaplexai-common`、`schemaplexai-model`、`docker/postgres/init/*.sql`、Flyway 迁移、各 `application.yml`
- 方法: 逐字读两份 spec → 提取 28 条可单独核查需求 → 文本搜索定位 → 读实际代码 → 走正常/失败/超时/降级路径 → 裁决

## 裁决总览

| Verdict | 数量 | REQ |
|---|---|---|
| implemented | 11 | 06, 11, 13, 17, 20, 21, 22, 23, 24, 25, 28 |
| partial | 10 | 01, 02, 07, 09, 10, 12, 16, 18, 19, 27 |
| contradicted | 4 | 04, 05, 08, 14 |
| absent | 3 | 03, 15(子项), 26 |
| stronger-than-spec | 0(以注记形式出现于 REQ-09、REQ-11) |
| undecidable | 0 |

严重度(仅分歧项): Critical ×1(REQ-04)· High ×6(REQ-03, 05, 12, 14, 16, 27)· Medium ×6(REQ-01, 02, 08, 15, 18, 19)· Low ×3(REQ-07, 10, 13)

**SPEC-MCP `status: completed` 宣称核查结论: 不成立。** 编排层(并行/隔离/白名单/守卫/调度/命名)与测试确实完成,但真正的协议级工具发现 `McpClient.listTools()` 是返回空列表的桩(见 REQ-27),且发现服务依赖的 `status = 1` 查询与交付的 DB 模式不兼容(见 REQ-14/REQ-19),生产环境下任何工具都无法被发现。

---

## REQ-01 — 工具适配器接口契约(来源: SPEC-INT)

> "public interface ToolAdapter { String getToolType(); void validateParams(Map<String, Object> params); ToolExecutionResult execute(Long tenantId, Map<String, Object> params); boolean isReadOnly(); }" — SPEC-INT §3.1

**Verdict:** partial · confidence: high
**What this demands:** 统一工具适配器需具备 4 项能力:类型标识、参数校验、带租户上下文的执行、只读标记。
**Where enforcement lives:** `schemaplexai-integration/src/main/java/com/schemaplexai/integration/tool/ToolExecutor.java:5-10` —— 实际接口只有 `getToolName()` 与 `String execute(Map<String,Object>)`。
**Paths walked:**
- ✓ 类型标识:`getToolName()` 对应 `getToolType()`(ToolExecutor.java:7;实现见 LocalToolExecutor.java:13、McpToolExecutor.java:28)
- ✗ 参数校验:接口无 `validateParams`;ToolExecutionService 仅做 JSON 反序列化,失败时退化为 `Map.of("raw", ...)`(ToolExecutionService.java:54-64),无任何字段/类型级校验
- ✗ 租户上下文:`execute` 不带 `tenantId`;整个 `schemaplexai-integration/src/main` 无任何 `tenantId` 字样(仅继承自 BaseEntity 的字段),工具执行路径对租户无感知
- ✗ 只读标记:无 `isReadOnly`;无返回值结构体(返回裸 String)
**How the verdict was reached:** 统一入口+注册表这一"机制"存在(故非 absent),接口以不同形态满足"类型标识+执行"两项(可记文档漂移),但 4 项契约能力缺 3 项,故非 implemented;代码并未做出与契约相反的断言,故非 contradicted。

---

## REQ-02 — ToolExecutionService:注册表 + 30 秒超时 + JSON Schema 校验(来源: SPEC-INT)

> "@Service public class ToolExecutionService { // 注册表: Map<String, ToolAdapter> // 超时: 30 秒 // 参数校验: JSON Schema 验证 }" — SPEC-INT §3.2

**Verdict:** partial · confidence: high
**What this demands:** 三项并列性质:执行器注册表、30 秒执行超时、JSON Schema 参数校验。
**Where enforcement lives:** `service/ToolExecutionService.java:25-52`
**Paths walked:**
- ✓ 注册表:`@PostConstruct` 将 `List<ToolExecutor>` 收集为 `Map<String, ToolExecutor>`(ToolExecutionService.java:27-31);未知工具抛 `INTEGRATION_NOT_FOUND`(L37-40)
- ✗ 30 秒超时:类中无任何超时控制;直接同步调用 `executor.execute(parameters)`(L44),时长完全取决于各执行器内部(而各执行器也大多无超时,见 REQ-16)
- ✗ JSON Schema 校验:`parseParameters` 只做 `objectMapper.readValue` 通用解析,解析失败吞异常并回退 `Map.of("raw", parametersJson)`(L58-63)——与 Schema 验证相反,非法输入被静默接受
- ✓ 失败路径:执行异常统一包装为 `TOOL_EXECUTION_FAILED`(L47-51)
**How the verdict was reached:** 三缺一超时、二缺 Schema 校验;注册表成立。机制存在故非 absent;未做与规格相反的声明故非 contradicted。

---

## REQ-03 — Git OAuth 流程:code 换 token + 加密落库(来源: SPEC-INT)

> "用户授权 → 回调到 /integration/git/callback ↓ 交换 code 获取 access_token ↓ 加密存储 token(AES-256-GCM)↓ 返回成功,显示已连接仓库" — SPEC-INT §4.1

**Verdict:** absent · confidence: high
**What this demands:** OAuth 回调端点、授权码交换、加密持久化、连接状态回显,四步成链。
**Searched:**
- 模式 `integration/git|git/callback|git/connect`(全仓 *.java)→ 0 命中(无任何路由映射)
- 模式 `disconnect|REGISTERED|AVAILABLE|CONNECTED|tenantId`(integration main 源码)→ 0 命中
- 直接读 `service/GitIntegrationService.java:213-222`:`handleOAuthCallback` 仅做参数非空校验 + `log.info("OAuth callback acknowledged...")`,注释自承 "Phase 1: Store authorization code, exchange for access token via provider API / Phase 2: Persist token securely" 均未做
**Paths walked:** ✗ 无回调端点 ✗ 无 code→token 交换 ✗ 无加密存储 ✗ 无已连接仓库回显
**How the verdict was reached:** 唯一相关代码是一个打日志的占位方法,四个子步骤全部不存在,故 absent 而非 partial;方法存在但语义为空,不构成 contradicted。

---

## REQ-04 — 外部凭据以 AES-256-GCM 加密存储(来源: SPEC-INT)

> "Token 必须加密存储"(§4.1 安全约束);"加密存储 token(AES-256-GCM)"(§4.1);"Token 加密 | AES-256-GCM"(§7);"config | JSONB | 配置(加密存储敏感信息)"(§5.1)

**Verdict:** contradicted · confidence: high · **severity: Critical**
**What this demands:** 集成层持有的外部凭据(Git access token、Jenkins apiToken、集成 config 中的敏感项)必须以 AES-256-GCM 级加密落存储。
**Where enforcement lives:** 无;相反证据如下:
- `service/GitIntegrationService.java:56` —— `repo.put("accessToken", accessToken != null ? accessToken : "")`,明文存入内存 `repoStore`(ConcurrentHashMap);`cloneRepository` 进一步把明文 token 拼进 URL(L108 `injectToken(cloneUrl, accessToken)` → L321-329 `https://<token>@...`),失败时异常消息可能携带该 URL
- `entity/SfIntegration.java:15` —— `configJson` 为普通 String,`IntegrationServiceImpl.save`(L33-37)仅做名称/类型非空校验,写入前无任何加解密
- `docker/postgres/init/03-init-schema-others.sql:219` —— `config_json TEXT`,无加密语义
**Searched:** 模式 `AES|GCM|encrypt|Encrypt|decrypt|Decrypt`(*.java/sql/yml,全仓)→ 全部命中位于 `schemaplexai-agent-engine/.../memory/TenantKeyService.java` 与 `CompositeChatMemoryStore`(聊天记忆加密,属另一领域);`schemaplexai-integration` 模块 0 命中
**Paths walked:** ✗ OAuth 存储路径(不存在,见 REQ-03)✗ 仓库注册路径(明文)✗ sf_integration 保存路径(明文)✗ Jenkins 凭据仅在方法参数间传递、从不存储(`JenkinsIntegrationService.java:39` 接收、L179-186 组 Basic 头后即丢弃)
**How the verdict was reached:** 规格明文要求加密保护级别,代码以明文实现凭据驻留,属保护级别不符 → 按任务分级为 Critical;代码提供了"存储"但与规格保护级别相悖,故 contradicted 而非 absent。

---

## REQ-05 — Git 操作按租户隔离(来源: SPEC-INT)

> "每个租户的 Git 操作隔离" — SPEC-INT §4.1 安全约束

**Verdict:** contradicted · confidence: high · **severity: High**
**What this demands:** 租户 A 不能看见/操作租户 B 的仓库、分支、webhook 记录。
**Where enforcement lives:** `service/GitIntegrationService.java:37-39` —— `repoStore`/`webhookStore` 为进程级全局 ConcurrentHashMap,key 为 `repoId`/`webhookId`,记录体中无 `tenantId` 字段;`registerRepository`(L43-62)不接收租户参数。
**Paths walked:**
- ✗ 仓库注册/列举:`listRepositories()`(L75-83)返回全部租户数据
- ✗ webhook 存储:`handleWebhook`(L224-259)记录不含租户维度;`listWebhookEvents`(L261-279)无租户过滤
- ✗ 工具执行路径:`ToolExecutor.execute(Map)` 无租户参数(见 REQ-01)
- △ DB 实体路径(`SfIntegration` 等)依赖 `schemaplexai-dao` 的 `TenantLineInterceptor`(经 `SchemaPlexaiIntegrationApplication.java:7` scanBasePackages 引入),但 Git 域完全未落库,该保护不覆盖内存存储
**How the verdict was reached:** 规格强制隔离,代码以全局共享存储实现且无任何租户键,行为与规格相反 → contradicted(需要"特定状态"——本实现下任何部署形态都无法满足,故非 partial)。

---

## REQ-06 — Jenkins triggerBuild 可用(来源: SPEC-INT)

> "触发构建 | 调用 Jenkins API 触发 Job | 已实现" — SPEC-INT §4.2

**Verdict:** implemented · confidence: high
**What this demands:** 能向 Jenkins 发起构建触发并处理失败。
**Where enforcement lives:** `service/JenkinsIntegrationService.java:39-69`
**Paths walked:**
- ✓ 正常路径:组 `buildWithParameters` URL、form-urlencoded 参数体、Basic 认证头,`restTemplate.postForObject`(L46-63)
- ✓ 参数校验:URL/任务名缺失抛 `PARAM_ERROR`(L42-44)
- ✓ 失败路径:任何异常包装为 `TOOL_EXECUTION_FAILED`(L64-68)
- ✗ 超时:依赖无超时配置的 RestTemplate(见 REQ-16)——该缺口归 REQ-16 记
**How the verdict was reached:** 规格状态表宣称"已实现",代码功能与之相符;超时缺失属横切需求另案处理,不推翻本条。

---

## REQ-07 — 回调/通知:webhook 接收与构建回调的下游联动(来源: SPEC-INT)

> "Webhook | 接收 Push/MR 事件"(§4.1);`handleBuildCallback`/构建结果处理语境(§4.2)

**Verdict:** partial · confidence: high · **severity: Low**
**What this demands:** 接收外部事件并驱动下游动作(工作流/通知)。
**Where enforcement lives:** `GitIntegrationService.java:224-259`(handleWebhook)、`JenkinsIntegrationService.java:71-87`(handleBuildCallback)
**Paths walked:**
- ✓ Git webhook 接收:解析 GitHub/GitLab 事件类型、仓库、分支、commit(提取器 L354-384),仓库缺失拒绝(§4.1 安全语义的弱形式)
- ✓ Jenkins 回调接收:写入 `buildCache`
- ✗ 下游联动:`processWebhookEvent`(L392-396)与 `processBuildResult`(L192-196)均只有 `// TODO: integrate with workflow engine...` 日志占位;未发 `sf.notification`/`sf.workflow.trigger` 消息
- ✗ 暴露:`/integration/git/**`、`/integration/jenkins/**` 无任何 Controller(见 REQ-15),回调实际无 HTTP 入口
- ✗ 持久性:两处均为内存存储,重启即失
**How the verdict was reached:** "接收+解析"成立,"通知/触发下游"完全为空 → partial;因规格自身状态表也把 Git webhook 标为"未实现",后果有限,评 Low。

---

## REQ-08 — MCP 生命周期状态机(来源: SPEC-INT)

> "MCP 生命周期: REGISTERED → CONNECTED → AVAILABLE → DISCONNECTED";状态表逐一定义四态 — SPEC-INT §4.3

**Verdict:** contradicted · confidence: high · **severity: Medium**
**What this demands:** MCP Server 实体带四态生命周期并可迁移。
**Searched:** 模式 `REGISTERED|AVAILABLE|CONNECTED|disconnect`(integration main)→ 0 命中
**Where enforcement lives:**
- `entity/SfMcpServer.java:21` —— `private Integer status;`(0/1 布尔语义,见 `McpToolExecutor.java:41` 的 `!= 1` 判断)
- `docker/postgres/init/03-init-schema-others.sql:242` —— `status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE'`(既非四态枚举,也与实体 Integer 不兼容,见 REQ-14)
- 无连接/断开迁移操作:`McpServerController.java:24-73` 只有 CRUD + discover + invoke
**Paths walked:** ✗ 注册后进入 REGISTERED ✗ 连接迁移 ✗ 工具列表就绪迁移到 AVAILABLE ✗ 断连迁移
**How the verdict was reached:** 代码用二值 Integer、DB 用 'ACTIVE' 字符串,两者都不是四态机且互相矛盾;规格的状态语义在任一层都不存在 → contradicted(而非 absent,因为存在一套相异的替代状态模型)。

---

## REQ-09 — MCP 管理功能面:注册/连接测试/工具列表/调用/断开(来源: SPEC-INT)

> 功能表:注册(部分实现)、连接测试(已实现)、`listTools()`(未实现)、`callTool()`(已实现)、断开(未实现)— SPEC-INT §4.3

**Verdict:** partial · confidence: high · **severity: Medium**
**Where enforcement lives:** `service/impl/McpServerServiceImpl.java`、`controller/McpServerController.java`、`tool/McpToolExecutor.java`
**Paths walked:**
- ✓ 注册:`McpServerController.create`(McpServerController.java:24-29)→ `ServiceImpl.save`;注:未调用 `validateEndpoint`(McpServerServiceImpl.java:59-66 定义了但注册路径不引用,仅测试引用)
- ✓ 连接测试:`healthCheck`(McpServerServiceImpl.java:38-56),GET `{endpoint}/health`,连接拒绝/其他异常均返回 false(降级成立)
- ✓ 工具列表:`discoverTools`(L68-121)POST `{endpoint}/discover`,body 为 `tools/list` JSON-RPC;已超出规格状态表的"未实现"(文档漂移,应回填规格)
- ✓ 调用:`invokeTool`(L123-182)POST `{endpoint}/invoke`;失败/降级路径返回 `"Error: ..."` 字符串而非抛异常(降级成立);`McpToolExecutor.execute`(tool/McpToolExecutor.java:33-56)另提供按 `serverId+method` 的调用,校验 `status==1`
- ✗ 断开连接:无端点、无服务方法;`McpClientManager.close`(在 agent-engine)仅供客户端池淘汰,非管理面操作
- ✗ 超时:discover/invoke 仅设 `connectTimeout(Duration.ofSeconds(10))`(L81、L152),未设请求级 `.timeout()`,慢响应可无限挂起(记入 REQ-16/17)
**How the verdict was reached:** 5 项功能 4 有 1 无,且注册未走端点校验 → partial。

---

## REQ-10 — Skill 数据模型(来源: SPEC-INT)

> "public class SfSkill { Long id; String name; String description; String promptTemplate; List<String> toolTypes; Map<String, Object> config; }" — SPEC-INT §4.4

**Verdict:** partial · confidence: high · **severity: Low**
**What this demands:** Skill = Prompt 模板 + 工具集绑定 + 配置。
**Where enforcement lives:** `entity/SfSkill.java:11-18` —— 实际字段 `name / code / description / content / status`;`content` 为 Markdown(带 YAML frontmatter,由 `skill/SkillMarkdownParser.java:14-34` 解析 name/version/description/tags)。
**Paths walked:**
- ✓ name/description 对应成立
- △ promptTemplate:可由 markdown 正文承载(机制不同,文档漂移)
- ✗ `toolTypes`(绑定工具类型):无任何结构化承载,frontmatter 只有 tags
- ✗ `config`:无
- ✓(超出规格)版本化:`SkillServiceImpl.createVersion`(SkillServiceImpl.java:54-81)按 `code@vN` 建版本;执行经 `executeSkill`(L109-140)路由到 `local` 工具
**How the verdict was reached:** "可复用能力包"目标以 Markdown 机制部分满足,规格点名的两个结构化字段缺失 → partial;因属机制漂移而非对抗,记 Low 并建议修规格。

---

## REQ-11 — Skill API(来源: SPEC-INT)

> "POST /integration/skills # 创建 Skill;GET /integration/skills # 列表" — SPEC-INT §6

**Verdict:** implemented · confidence: high
**Where enforcement lives:** `controller/SkillController.java:17`(`@RequestMapping("/integration/skills")`),create L24-29、list L62-66;另有 PUT/DELETE、`GET /{id}`(摘要)、`GET /{id}/content`(按需加载)。
**Paths walked:** ✓ 创建(带实体校验 `SkillServiceImpl.validateSkillEntity` L41-51)✓ 列表(返回不含 content 的 `SkillSummary`,超出规格的渐进披露设计,文档漂移注记)
**How the verdict was reached:** 规格两条端点均存在且语义一致;额外端点不构成违规。

---

## REQ-12 — API Gateway 管理:注册/调用代理/响应缓存/熔断降级(来源: SPEC-INT)

> 职责表:API 注册、调用代理(统一限流、日志)、响应缓存、熔断降级 — SPEC-INT §4.5

**Verdict:** partial · confidence: high · **severity: High**
**Where enforcement lives:** `service/ApiGatewayServiceImpl.java`、`controller/ApiGatewayController.java`、`entity/SfApiGatewayConfig.java`
**Paths walked:**
- △ 注册:`ApiGatewayController.create`(L22-27)+ `validateGateway`(ApiGatewayServiceImpl.java:44-57)存在,但持久化表 `sf_api_gateway_config` 不存在(见下),`save()` 运行时必失败
- ✗ 调用代理:`/integration/api-gateways/{id}/invoke` 无实现,`ApiGatewayService` 接口无 invoke 方法
- ✗ 响应缓存:无任何缓存逻辑
- ✗ 熔断降级:无熔断器;`healthCheck`(L120-136)仅返回布尔
- △ 限流:`rateLimit` 字段可存可改(`updateRateLimit` L106-117),但无处执行
**Searched:** 模式 `sf_api_gateway_config`(*.sql,全仓)→ 0 命中(`docker/postgres/init/*.sql` 与所有迁移均无该表);模式 `CircuitBreaker|Resilience|Retry|fallback`(integration main)→ 0 命中
**How the verdict was reached:** 4 项职责仅"注册"有代码形态且因缺表无法落库;核心"调用代理"完全缺失 → partial 偏重,评 High。

---

## REQ-13 — sf_integration 数据模型(来源: SPEC-INT)

> 字段表:id / tenant_id / type(GIT/JENKINS/MCP/API)/ name / config JSONB(加密存储敏感信息)/ status — SPEC-INT §5.1

**Verdict:** implemented · confidence: high · **severity: Low**(文档漂移注记)
**Where enforcement lives:** `docker/postgres/init/03-init-schema-others.sql:213-223`、`entity/SfIntegration.java:10-17`
**Paths walked:** ✓ id/tenant_id/name/type/status 齐全;△ `config` 实际为 `config_json TEXT`(非 JSONB);△ type 取值注释为 `GITHUB / GITLAB / JENKINS / MCP / SKILL / API`(规格为 GIT / JENKINS / MCP / API);✗ "加密存储敏感信息"不成立(已归 REQ-04 Critical,不在本条重复计罚)
**How the verdict was reached:** 列结构按不同机制满足字段集 → implemented + 漂移注记;加密子句的违反已由 REQ-04 承载。

---

## REQ-14 — sf_mcp_server 数据模型与模式一致性(来源: SPEC-INT + SPEC-MCP)

> "status | VARCHAR | REGISTERED / CONNECTED / DISCONNECTED;tools | JSONB | 可用工具列表(缓存)" — SPEC-INT §5.2;"Query approved MCP servers (`status = 1`) from the database" — SPEC-MCP Requirements

**Verdict:** contradicted · confidence: high · **severity: High**
**What this demands:** 一张自洽的 `sf_mcp_server`:状态语义一致、工具缓存列存在、实体映射与列类型兼容。
**Where enforcement lives:**
- `docker/postgres/init/03-init-schema-others.sql:238-248`:`status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE'`;**无 `tools` 列**
- `entity/SfMcpServer.java:21`:`Integer status` —— 与 VARCHAR 列直接冲突:任何命中行的 `selectById/selectList` 会因类型转换失败;且 `status = 1`(整型参数比 varchar 列,见 `McpToolDiscoveryService.java:153-158` 与 `McpServerRegistry.java:72-77`)在 PostgreSQL 上产生 `character varying = integer` 比较,无法匹配默认值 'ACTIVE'
- 迁移 `schemaplexai-agent-engine/src/main/resources/db/migration/V2026_05_03__extend_mcp_server.sql:5-10` 补齐 `command/args/env_vars/server_public_key/protocol_version/tool_whitelist` 六列(与实体其余字段对齐),但**没有任何迁移**改 `status` 的类型或默认值,也没有添加 `tools` 缓存列
**Searched:** 模式 `tool_whitelist|server_public_key|protocol_version|env_vars|sf_api_gateway_config`(*.sql)→ 仅命中上述迁移;模式 `ALTER.*status`(迁移目录)→ 0 命中(仅新表的 SMALLINT status)
**Paths walked:** ✗ 状态语义(三套:规格四态字符串 / 实体 Integer / DB 'ACTIVE')✗ tools 缓存列 ✗ 实体-列类型兼容 ✗ 默认数据可满足 `status = 1`
**How the verdict was reached:** 三层状态定义互斥且同表共存,直接后果是 SPEC-MCP 的入口查询(批准服务器)在交付模式下不可满足、实体映射可运行期崩溃 → contradicted;这比"缺某列"更重,故评 High 并作为 REQ-19 partial 的根因。

---

## REQ-15 — §6 API 面:Git / Jenkins / MCP 路径(来源: SPEC-INT)

> "POST /integration/git/connect;GET /integration/git/repos;...;POST /integration/jenkins/builds;GET /integration/jenkins/builds/{id};POST /integration/mcp/servers;POST .../connect;POST .../disconnect;GET .../tools" — SPEC-INT §6

**Verdict:** absent(对 Git 4 条与 Jenkins 2 条)/ contradicted(对 MCP 4 条的路径形态)· 合并裁决 **absent** · confidence: high · **severity: Medium**
**Searched:** 模式 `integration/git|integration/jenkins|git/callback|git/connect`(全仓 *.java)→ 0 命中;通读全部 4 个 Controller(`Integration/McpServer/ApiGateway/SkillController`)确认无 Git / Jenkins 路由
**Where enforcement lives:**
- Git、Jenkins:无任何 Controller;`GitIntegrationService`/`JenkinsIntegrationService` 仅为内部 Service,无 HTTP 暴露
- MCP:`McpServerController.java:17` 实际前缀为 `/integration/mcp-servers`(规格为 `/integration/mcp/servers`);`connect`/`disconnect` 端点缺失;`GET .../tools` 变为 `POST /{id}/discover`(L60-64),另增 `POST /{id}/invoke`(L66-73)
**Paths walked:** ✗ Git connect/repos/files 全链 ✗ Jenkins builds 触发/查询 ✗ MCP connect ✗ MCP disconnect △ MCP 注册(存在但路径不同)△ 工具列表(动词与路径均不同)
**How the verdict was reached:** 规格 §6 是接口契约;Git/Jenkins 六条端点整体不存在(虽然 §4.1/§4.2 状态表自认多项"未实现",端点契约仍被违反,故合并记 absent+Medium 而非 High);MCP 四条为路径漂移+缺失,一并记入。

---

## REQ-16 — 外部调用 30 秒超时 + 降级(来源: SPEC-INT §7 + agents.md 项目规范)

> "外部 API 超时 | 30s" — SPEC-INT §7;agents.md:"所有外部调用(LLM、Git、Jenkins 等)必须加超时和降级"

**Verdict:** partial · confidence: high · **severity: High**
**Where enforcement lives(缺口侧):**
- `config/IntegrationConfig.java:10-13` —— `new RestTemplate()`,未设 `ClientHttpRequestFactory`,**无连接/读取超时**;该 Bean 被 GitHub/GitLab/Jenkins/MCP-health/API-GW-health 全部外部 HTTP 路径共享
- `tool/GitHubToolExecutor.java:67-68、90-91`、`tool/GitLabToolExecutor.java`(同构)、`service/impl/McpServerServiceImpl.java:46`(healthCheck)、`service/ApiGatewayServiceImpl.java:126`、`service/GitIntegrationService.java:406、417` —— 均经上述无超时 RestTemplate
- `service/GitIntegrationService.java:331-352` —— `executeGitCommandInDir` 用 `ProcessBuilder` + `process.waitFor()` **无限等待**,无 `waitFor(timeout, unit)`;git 进程挂起即线程永久阻塞
**Where enforcement lives(成立侧):**
- `service/impl/McpServerServiceImpl.java:81、152` —— MCP discover/invoke 有 `connectTimeout(10s)`(连接超时成立;请求级 `.timeout()` 缺失)
- 降级:Jenkins 不可达映射为 `REQUEST_TIMEOUT` 异常(JenkinsIntegrationService.java:120-122、164-166);MCP invoke/discover 返回 `"Error: ..."`/空列表(McpServerServiceImpl.java:117-120、178-181);healthCheck 返回 false;GitHub/GitLab 执行器把异常包装为 `TOOL_EXECUTION_FAILED`(有错误归一,无回退值)
**Paths walked:** ✓ 连接超时仅 MCP 一路 ✗ 30s 全局上限 ✗ RestTemplate 各路的读超时 ✗ git CLI 超时 △ 降级(部分路径有,无熔断)
**How the verdict was reached:** "超时"仅在 MCP 连接一段成立,"降级"零散成立;规格 30s 上限与项目规范的全覆盖要求均不满足 → partial,High(任一外部端点慢响应可拖死调用线程)。

---

## REQ-17 — MCP 连接超时 10 秒(来源: SPEC-INT)

> "MCP 连接超时 | 10s" — SPEC-INT §7

**Verdict:** implemented · confidence: high
**Where enforcement lives:** `service/impl/McpServerServiceImpl.java:80-82、151-153` —— `HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))`,discover 与 invoke 两路一致;失败路径:`ResourceAccessException`/一般异常分别记日志并返回空列表/`"Error: ..."`(L117-120、178-181)
**Paths walked:** ✓ 连接建立超 10s 中断 ✓ 异常不穿透为 500 裸栈(降级到错误字符串)
**How the verdict was reached:** 数值与语义精确匹配"连接超时";请求级读超时缺失已在 REQ-16 记账,不在本条重复。

---

## REQ-18 — 熔断降级(来源: SPEC-INT §4.5,横切)

> "熔断降级 | 外部 API 不可用时熔断" — SPEC-INT §4.5

**Verdict:** partial · confidence: high · **severity: Medium**
**Searched:** 模式 `CircuitBreaker|Resilience4j|Retry|fallback|Hystrix`(integration main + pom)→ 0 命中;`pom.xml` 无 resilience 依赖
**Where enforcement lives:** 仅存在弱形态:连续失败不会被记忆,下次调用仍会原样重发(如 `McpServerServiceImpl.healthCheck` 每次重新发起请求);模型冷却(`sf:model:cooldown`)在 system/agent 域,不覆盖集成层
**Paths walked:** ✗ 熔断打开 ✗ 半开探测 ✗ 快速失败回退 △ 单次失败降级(见 REQ-16 成立侧)
**How the verdict was reached:** 有"降级"无"熔断",故 partial 而非 absent。

---

## REQ-19 — 发现服务:查询批准服务器(来源: SPEC-MCP)

> "Query approved MCP servers (`status = 1`) from the database" — SPEC-MCP Requirements

**Verdict:** partial · confidence: high · **severity: Medium**(根因在 REQ-14,本条不重复记 High)
**Where enforcement lives:** `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/tool/mcp/McpToolDiscoveryService.java:153-158` —— `selectList(new LambdaQueryWrapper<SfMcpServer>().eq(SfMcpServer::getStatus, 1))`
**Paths walked:** ✓ 代码语义与规格逐字一致 ✗ 运行时语义:列类型冲突(见 REQ-14),PostgreSQL 上该查询要么抛 `operator does not exist: character varying = integer`,要么(即便强转)永远匹配不到默认值 'ACTIVE' 的行
**How the verdict was reached:** 代码层满足、数据层被模式阻断,需要"特定状态"(迁移 status 列为整型并回填 1)才能生效 → partial。

---

## REQ-20 — 并行发现:固定线程池 4、守护线程(来源: SPEC-MCP)

> "Discover tools from each server in parallel using a fixed thread pool (size 4)";"CompletableFuture.runAsync(...) with a daemon thread pool" — SPEC-MCP Requirements/Design

**Verdict:** implemented · confidence: high
**Where enforcement lives:** `McpToolDiscoveryService.java:40`(DISCOVERY_POOL_SIZE = 4)、L47-53(`Executors.newFixedThreadPool(4, ...)`,线程工厂 `setDaemon(true)`)、L73-74(`runAsync(..., discoveryExecutor)`)
**Paths walked:** ✓ 每服务器一个 future ✓ 池大小 4 ✓ 守护线程 ✓ 空服务器列表提前返回并记 debug(L63-67)
**How the verdict was reached:** 与 Design 节逐条对应,无偏差。

---

## REQ-21 — 每服务器故障隔离(来源: SPEC-MCP)

> "Isolate failures per-server so one unreachable server does not block others";".exceptionally(...) per-server error isolation" — SPEC-MCP

**Verdict:** implemented · confidence: high
**Where enforcement lives:** `McpToolDiscoveryService.java:75-79`(每 future 独立 `.exceptionally` 记 warn 返回 null)、L96-139(`discoverForServer` 内层再包 try/catch,双保险)、L83(`allOf(...).join()` 汇总)
**Paths walked:** ✓ 单服务器抛异常不中断其他(测试 `McpToolDiscoveryTest.shouldIsolateFailuresPerServer` L142-161 以 mock 一侧抛 `boom` 验证)✓ 汇总等待不因单点失败抛出
**How the verdict was reached:** 隔离在编排层与执行层各有一道,规格语义完全成立。

---

## REQ-22 — Guardrails 校验工具描述(来源: SPEC-MCP)

> "Apply guardrails validation to each tool description before registration";"GuardrailsEngine.validateInput(tool.description()) must return success = true";"Failed tools are logged and skipped" — SPEC-MCP

**Verdict:** implemented · confidence: high
**Where enforcement lives:** `McpToolDiscoveryService.java:114-119` —— 先 `guardrailsEngine.validateInput(tool.description())`,`!success()` 则记 warn(含工具名/服务器/错误原因)并 `continue`;引擎实现 `GuardrailsEngine.java:26-34`(短路聚合),规则由 `GuardrailsConfig.java:26-29` 注入(黑名单+长度)
**Paths walked:** ✓ 校验先于注册(顺序正确,在 exists 检查之前)✓ 失败记日志+跳过 ✓ 测试覆盖(`shouldSkipToolsFailingGuardrails` McpToolDiscoveryTest.java:82-103)
**How the verdict was reached:** 与 Guardrails 小节逐字对应。

---

## REQ-23 — 白名单语义(来源: SPEC-MCP)

> "Respect server tool whitelist (null/empty means all tools allowed)" — SPEC-MCP Requirements

**Verdict:** implemented · confidence: high
**Where enforcement lives:** `McpToolDiscoveryService.java:160-166` —— `whitelist == null || isEmpty → true`,否则 `contains(toolName)`;实体字段 `SfMcpServer.java:40-42`(JacksonTypeHandler JSONB 数组,迁移列见 REQ-14)
**Paths walked:** ✓ null 放行 ✓ 空列表放行 ✓ 命中放行 ✓ 未命中跳过并记 debug(L108-112)✓ 测试 `shouldSkipToolsNotInWhitelist`(McpToolDiscoveryTest.java:105-125)
**How the verdict was reached:** 三态语义与规格一致;消费侧 `McpServerRegistry.isToolAllowed`(McpServerRegistry.java:42-53)同语义,一致性成立。

---

## REQ-24 — 跳过已注册工具(来源: SPEC-MCP)

> "Skip already-registered tools" — SPEC-MCP Requirements

**Verdict:** implemented · confidence: high
**Where enforcement lives:** `McpToolDiscoveryService.java:121-125` —— `buildQualifiedName` 后 `toolRegistry.exists(qualifiedName)` 为真则跳过并记 debug;注册表实现 `InMemoryToolRegistry.java:70-73`(ConcurrentHashMap containsKey),且 `register` 对重复名抛 `IllegalArgumentException`(L25-28)——exists 前置检查避免了该异常
**Paths walked:** ✓ 已注册跳过(测试 `shouldNotReregisterExistingTools` McpToolDiscoveryTest.java:174-190)△ 边角:多服务器并发场景下 exists→register 之间存在竞态窗口(两服务器同名工具可并发通过 exists 检查),因 register 抛异常会被 `.exceptionally` 吞掉并记 warn——不违反规格但值得注记
**How the verdict was reached:** 规格语义成立;竞态记入 Open questions。

---

## REQ-25 — 周期调度:可配置间隔、默认 60 秒(来源: SPEC-MCP)

> "Run periodically via Spring @Scheduled with configurable interval (default 60s)" — SPEC-MCP Requirements

**Verdict:** implemented · confidence: high
**Where enforcement lives:** `McpToolDiscoveryService.java:61` —— `@Scheduled(fixedDelayString = "${mcp.discovery.interval:60000}")`;调度使能 `SchemaPlexaiAgentEngineApplication.java:13`(`@EnableScheduling`);额外开关 `@ConditionalOnProperty(prefix = "mcp.discovery", name = "enabled", havingValue = "true", matchIfMissing = true)`(L35)
**Paths walked:** ✓ 默认 60000ms ✓ 属性可覆盖 ✓ 禁用开关有测试(`McpToolDiscoveryConditionalTest.java:21-33`)✓ `syncAll` 同步完成后再入下一次延迟(与 fixedDelay 语义自洽)
**How the verdict was reached:** 与规格逐字一致,且附加的禁用开关属增强不构成偏差。

---

## REQ-26 — 限定名格式与双 API(来源: SPEC-MCP)

> "Qualified Name Format: `mcp:<serverId>:<toolName>`";"`syncAll()` ... `discoverForServer(SfMcpServer server)` Returns `List<ToolDefinition>` ... without registering" — SPEC-MCP Design/API

**Verdict:** implemented · confidence: high
**Where enforcement lives:** `McpToolDiscoveryService.java:168-170`(`"mcp:" + server.getId() + ":" + toolName`)、L62-85(syncAll:取批准服务器→并行→注册)、L96-140(discoverForServer:只返回不注册,注释明示 "does NOT register")
**Paths walked:** ✓ 格式与示例 `mcp:1:read_file` 一致(测试断言 `containsExactly("mcp:1:read_file", "mcp:1:write_file")` McpToolDiscoveryTest.java:78-79)✓ discoverForServer 无注册副作用(测试 L199-230 未 verify register)✓ 依赖注入与规格 Dependencies 节一致(L42-45:McpServerMapper / McpClientManager / ToolRegistry / GuardrailsEngine)
**How the verdict was reached:** Design/API/Dependencies 三节全部逐条成立。

---

## REQ-27 — 真实工具发现:McpClient 协议层(来源: SPEC-MCP)

> "Periodic background service that discovers tools from configured MCP servers";"Discover tools from each server" — SPEC-MCP Overview/Requirements

**Verdict:** partial · confidence: high · **severity: High**
**What this demands:** 服务真的能从 MCP 服务器取得工具清单。
**Where enforcement lives:** `schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/tool/mcp/McpClient.java:54-56`:
```java
public List<McpTool> listTools() {
    return List.of();   // "Stub implementation — returns empty list. Will be replaced with actual MCP protocol call in a later phase."
}
```
同文件 L11-14 类注释:"The actual MCP protocol implementation will be added in a later phase";构造函数无条件 `connected = true`(L27),即不存在真实连接建立,也无从谈起 10s 连接超时。
**Paths walked:**
- ✓ 编排链完整(见 REQ-20..26)
- ✗ 生产路径结果:`clientManager.create` 返回桩客户端 → `listTools()` 恒空 → `discoverForServer` 恒返回空 → 永不注册任何工具
- ✓ 测试路径:`McpToolDiscoveryTest` 全程 mock `McpClient`(L43-44),因此"测试绿"与"功能真"脱钩
**How the verdict was reached:** 规格宣称的服务行为(发现并注册工具)在无 mock 的环境中产出恒为空;但规格 Dependencies 节只要求 "McpClientManager — create/get cached McpClient",未逐字约束协议实现,且桩有明确注释,故裁 partial 而非 contradicted。SPEC-MCP 头部 `status: completed` 与本条事实不符——**该宣称不成立**,这是本次核查的核心结论。

---

## REQ-28 — 测试体系(来源: SPEC-MCP)

> "Unit tests with JUnit 5 + Mockito;Mock all external dependencies;Test: happy path, guardrails failure, whitelist filtering, disconnected client, per-server failure isolation, no servers, existing tool skip" — SPEC-MCP Testing

**Verdict:** implemented · confidence: high
**Where enforcement lives:** `schemaplexai-agent-engine/src/test/java/com/schemaplexai/agent/engine/tool/mcp/McpToolDiscoveryTest.java`(7 个 syncAll 用例 + 2 个 discoverForServer 用例)、`McpToolDiscoveryConditionalTest.java`(条件装配 2 用例)、`McpClientManagerTest.java`、`McpToolAdapterTest.java`
**Paths walked:** ✓ happy path(L59-80)✓ guardrails 失败(L82-103)✓ 白名单(L105-125)✓ 断连客户端(L127-139)✓ 故障隔离(L141-161)✓ 无服务器(L163-172)✓ 已注册跳过(L174-190)✓ 全 mock(JUnit 5 `@ExtendWith(MockitoExtension.class)`)
**How the verdict was reached:** 规格点名的 7 类用例逐一对应存在。注:测试全绿不能证明 REQ-27(桩被 mock 掉),两者分别裁决。

---

## notChecked

1. **工具调用延迟 P99 < 5s**(SPEC-INT §7):纯运行期指标,静态核查无法裁决,仓库内亦无基准/压测数据。
2. **Git Worktree 多租户隔离工作区**(SPEC-INT §4.1 功能表):规格自认"未实现",且与 `TaskBranchManager`(反向差距 1)部分重叠,未单独成条。
3. **Gitea 平台支持**(SPEC-INT §4.1 "支持平台: GitHub, GitLab, Gitea"):代码仅见 github/gitlab 分支(`GitIntegrationService.java:293-297、311-315`),Gitea 无实现——并入 REQ-15 记,未单独核查深度。
4. **`sf_api_gateway_config` 是否有计划中的建表脚本**在仓库外(如运维手工):全仓搜索无果,按仓库现状裁决。

## 反向差距(代码有、规格未提)

1. **`git/TaskBranchManager.java:25-78`**:任务级分支命名(`task/<taskId>`)+ 30 天软删除注册表,规格未提及。
2. **Skill 版本化与 Markdown 体系**:`SkillServiceImpl.createVersion/listVersions`(L54-92)、`SkillMarkdownParser`、`GET /{id}/content` 渐进披露——超出 SPEC-INT §4.4 的静态模型。
3. **双 MCP 栈并存**:集成模块的 HTTP `POST /discover`、`/invoke`(自造 JSON-RPC 变体,McpServerServiceImpl.java:77、138-143)与 agent-engine 的 `McpClientManager` Caffeine 连接池(上限 32、10 分钟空闲淘汰、`@PreDestroy` 全关,McpClientManager.java:31-44、105-108)互不相通,规格均未描述协议形态。
4. **消费侧断裂**:`McpToolAdapter.execute`(McpToolAdapter.java:38-68)把 `mcp:<serverId>:...` 中的 serverId 传给 `McpServerRegistry.queryServer` 按 **endpoint** 列查询(McpServerRegistry.java:72-77),且 `clientManager.create(ref.serverId())` 把 id 当 endpoint 建客户端——限定名语义(数字 DB id)与注册表查询语义(endpoint 字符串)不一致,运行时必然 "MCP server not allowed";末段还无条件返回 "MCP protocol call not implemented"(L66-67)。
5. **双注册表不互通**:发现服务注册进 `com.schemaplexai.agent.engine.tool.ToolRegistry`(InMemoryToolRegistry,定义型),而执行链 `ToolCallingStateHandler.java:209` 经 `com.schemaplexai.agent.engine.tool.registry.ToolRegistry`(适配器型)按精确名解析——已发现的 `mcp:1:xxx` 工具永远解析不到适配器,注册即死档。
6. **ApiGatewayServiceImpl 内存路由管理**(`upsertRoute/listRoutes/deleteRoute`,L60-103)与 **IntegrationServiceImpl** 的 webhook 注册表 + `aggregateHealthStatus`(L57-139):规格 §4.5 未涵盖。
7. **全内存态存储**:Git 仓库/webhook、Jenkins 构建缓存、API 网关路由、集成 webhook 均为进程内存,重启即失,规格未声明该持久性级别。
8. **`McpServerController.create` 直接绑定实体**(`@RequestBody SfMcpServer`,McpServerController.java:26):调用方可注入 `id/tenantId/status` 等字段,规格未涉及;建议引入请求 DTO。

## Open questions

1. SPEC-INT 文件头 `status: 草稿` 与任务陈述"已批准"不一致;本报告按任务指示视其为权威规格,若实际未批准,REQ-03/04/05 的严重度语境需重议。
2. `schemaplexai-integration/src/main/resources/application.yml` 无 `spring.datasource` 配置,而模块依赖 `schemaplexai-dao` 并装配 4 个 MyBatis Mapper——生产数据源从何而来(环境变量注入?共享配置?)无法静态确认;若确无数据源,所有 DB 路径(含 MCP 批准服务器查询)不可用。
3. `exists → register` 并发竞态(REQ-24 注):两个服务器同名工具同时通过 exists 检查时,后注册者抛 `IllegalArgumentException` 被 `.exceptionally` 吞掉——行为安全但会丢一条 warn 之外的审计线索,是否需要幂等注册待产品定夺。
4. Flyway 迁移 `V2026_05_03__extend_mcp_server.sql` 位于 **agent-engine** 资源目录,由 agent-engine 启动时对共享库执行;integration 服务自身无迁移机制——跨服务模式是否被团队接受,文档未载。

## 反驳复核

独立复核人于 2026-08-29 对上述全部 7 条 severity ∈ {Critical, High} 且 verdict ∈ {contradicted, partial, absent} 的分歧条目,从代码方向(另寻被遗漏的执行点)与规格方向(重读原文、核对口径)逐条尝试推翻。结论:0 条推翻,7 条维持。

| REQ | 原判 | 裁定 | 理由(一行) | 证据 |
|---|---|---|---|---|
| REQ-03 | absent · High | 维持 | OAuth 四步链(回调端点/换 token/加密落库/回显)全仓皆无,`handleOAuthCallback` 仅为打日志占位,亦无独立交换端点存在于任何其他模块 | 全仓 *.java 搜 `git/callback|git/connect|authorization_code` 仅命中该占位方法与测试;`GitIntegrationService.java:213-222` 注释自承 Phase 1/Phase 2 未做;4 个 Controller 均无 git 路由 |
| REQ-04 | contradicted · Critical | 维持 | 全仓唯一 AES-256-GCM 实现位于 agent-engine 聊天记忆域(另一领域,未被集成层调用),集成链路从内存 repoStore 到 sf_integration.config_json 全程明文,无存储层加密封装 | 全仓 `encrypt|Cipher|AES` 命中仅 `TenantKeyService`/`CompositeChatMemoryStore`(+其测试);`GitIntegrationService.java:56` 明文存 token、L321-329 拼入 clone URL;`IntegrationServiceImpl.save`(L33-37)无加解密;`03-init-schema-others.sql:218` `config_json TEXT`;dao/common 模块无加密拦截器或 TypeHandler |
| REQ-05 | contradicted · High | 维持 | `repoStore`/`webhookStore` 为进程级全局且记录体无租户键,模块 main 源码 `tenantId` 0 命中,DAO 租户拦截器仅覆盖 SQL 不覆盖内存存储,无任何部署形态可满足隔离 | `GitIntegrationService.java:37-39`(全局 Map)、L43-62(registerRepository 无租户参数)、L75-83(list 返回全部);grep `tenantId` 于 `schemaplexai-integration/src/main` → 0 命中;`schemaplexai-dao` 仅有 `TenantLineInterceptor`(DB 层) |
| REQ-12 | partial · High | 维持 | 全仓所有 *.sql(含各模块 Flyway 迁移与测试 schema)均无 `sf_api_gateway_config` 建表,注册路径运行时必失败;调用代理/响应缓存/熔断降级皆无实现 | 全仓 *.sql 搜 `api_gateway` → 0 命中;`ApiGatewayServiceImpl` 仅 CRUD/route 内存管理/healthCheck,无 invoke/cache/熔断;`ApiGatewayController.java` 无 `/{id}/invoke`;`schemaplexai-integration/pom.xml` 无 resilience 依赖 |
| REQ-14 | contradicted · High | 维持 | 三条查询路径(发现/注册表/执行器)全部按整型 1 语义消费 status,与库中 `VARCHAR(32) DEFAULT 'ACTIVE'` 互斥,执行侧数据源为 PostgreSQL,`varchar = integer` 必然报错;无迁移修复类型、无 tools 缓存列,无兼容的替代查询路径 | `03-init-schema-others.sql:238-248`(status VARCHAR 'ACTIVE'、无 tools 列);`McpToolDiscoveryService.java:153-158` `.eq(getStatus,1)`、`McpServerRegistry.java:34,72-77`、`McpToolExecutor.java:41` 均整型 1;`V2026_05_03__extend_mcp_server.sql` 仅加 6 列;`schemaplexai-agent-engine/src/main/resources/application.yml` 数据源 `jdbc:postgresql` |
| REQ-16 | partial · High | 维持 | 共享 `RestTemplate` 为裸构造(无连接/读超时)、git CLI `process.waitFor()` 无限等待,且更外层无超时兜底:gateway 配置唯一 `timeout: 5s` 属 Redis,integration yml 无任何 timeout 项 | `IntegrationConfig.java:10-13` `new RestTemplate()` 无 factory;`GitIntegrationService.java:347` 无超时 `waitFor()`;`McpServerServiceImpl.java:81,152` 仅 connectTimeout 10s、无请求级 `.timeout()`;gateway/integration application.yml 无 HTTP 出站超时配置 |
| REQ-27 | partial · High | 维持(裁定表述微调) | `McpClient.listTools()` 为恒空桩、全 main 代码无第二实现、测试全程 mock;但 SPEC-MCP `status: completed` 宜按"规格自定范围(编排服务+全 mock 单测)完成"的口径读,不能读作生产协议级发现已可用——原判"宣称不成立"应限定为生产口径 | `McpClient.java:54-56` 桩注释、L27 无条件 `connected=true`;主代码 `listTools` 仅见于该桩与发现服务;`McpToolDiscoveryTest.java:42-43` `@Mock McpClient`;SPEC-MCP Testing 节自定 "Mock all external dependencies",正文从未约束 McpClient 协议实现 |

复核口径说明:
1. 代码方向的五个假设执行点均被排除——凭据加密不在别处(全仓搜索仅命中聊天记忆域)、OAuth 交换无独立端点、租户键无存储层封装、超时配置无更外层、status 整型语义无兼容查询路径。
2. 规格方向确认原引用无误读;SPEC-INT §4.1 状态表虽自认 Git 多项"未实现",但 §6 端点契约与 §4.1 安全约束("必须加密存储""每个租户的 Git 操作隔离")为规范性要求,REQ-03/04/05 的分歧性质不受影响(严重度语境另见原报告 Open question 1:SPEC-INT 文件头仍为"草稿")。
3. 唯一表述调整在 REQ-27:SPEC-MCP 的 `status: completed` 在其自定的 mock 测试验收口径下可以成立(编排与测试确实完成),故原报告"宣称不成立"宜精确表述为"生产环境端到端工具发现不成立",而非全称否定;REQ-27 的 partial/High 裁定本身不变。
