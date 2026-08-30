# Spec 合规核查 — System Notification Module

- **规格**: `docs/specs/2026-05-01-v1.0-notification.md`（status: 已批准，v1.0）
- **核查日期**: 2026-08-29
- **核查范围**: 通知能力分散实现的全部位置，实际定位如下：
  - `schemaplexai-web`：`NotificationController`、`NotificationService(Impl)`、`NotificationVO`（对外 REST 端点实现位置）
  - `schemaplexai-model`：`entity/notification/Notification.java`
  - `schemaplexai-dao`：`mapper/notification/NotificationMapper.java`、`config/TenantLineInterceptor.java`（仅 Handler，无插件注册）
  - `docker/postgres/init/04-notification.sql` 与 `docker/postgres/init/03-init-schema-others.sql`（**两份互相冲突的 sf_notification DDL**）
  - `schemaplexai-task`：`mq/NotificationConsumer.java`、`mq/dto/NotificationMessage.java`、`config/RabbitMqConfig.java`（MQ 投递链路，超出本规格范围 → 反向差距）
  - `schemaplexai-ops`：平行的 `NotificationController/Service/ServiceImpl/Mapper/entity SfNotification`（超出本规格范围 → 反向差距）
  - `schemaplexai-workflow`：`SpecReviewNotificationDelegate.java`（MQ 生产方，超出本规格范围）
  - `schemaplexai-ui`：`src/types/notification.ts`、`src/api/notification.ts`
  - 测试：web（controller/service 单元测试）、dao（`NotificationMapperTest` @SpringBootTest 集成测试）、model、task

> 说明：任务提示中列出的候选需求（站内消息/邮件/回调三通道、模板引擎、发送重试）在**本规格 §1.3 中被明确列为非目标**（"实时推送（WebSocket/SSE）"、"邮件/短信渠道"、"通知模板管理"、"管理员发通知的 UI"）。因此它们不作为 REQ 核查，仅在"非目标遵守情况"与"反向差距"中核对。

**裁决计数**：implemented 9 · partial 3 · contradicted 1 · absent 3 · stronger-than-spec 0 · undecidable 0 · notChecked 2

---

## REQ-01 — Entity / 表 / Mapper / Service / Controller 全链路组件存在

> "Notification 实体、数据库表、Mapper、Service、Controller" — 规格 §1.2 范围

**Verdict:** implemented · confidence: high

**What this demands:** 五个工件齐备且分层符合项目规范（Entity 继承 BaseEntity、Mapper 继承 BaseMapperX、Controller 返回 Result）。

**Where enforcement lives:**
- Entity：`schemaplexai-model/src/main/java/com/schemaplexai/model/entity/notification/Notification.java:10-24`（`@TableName("sf_notification")`，继承 `BaseEntity`）
- 表：`docker/postgres/init/04-notification.sql:4-17`（表结构与规格一致，但部署冲突见 REQ-11）
- Mapper：`schemaplexai-dao/src/main/java/com/schemaplexai/dao/mapper/notification/NotificationMapper.java:10-18`（继承 `BaseMapperX<Notification>`）
- Service：`schemaplexai-web/src/main/java/com/schemaplexai/web/service/notification/NotificationService.java:7-16` + `NotificationServiceImpl.java:20-72`
- Controller：`schemaplexai-web/src/main/java/com/schemaplexai/web/controller/notification/NotificationController.java:19-52`（`@RequestMapping("/web/notification")`，继承 `BaseController`，返回 `Result<T>`）

**Paths walked:** ✓ 五个工件均存在且相互引用一致（Controller → Service → Mapper → Entity/表）

**How the verdict was reached:** 组件关系图 §2.1（Controller→Service→Mapper→sf_notification）与代码拓扑逐一对应，无缺件。

---

## REQ-02 — GET /web/notification/page 端点契约

> "`GET /web/notification/page` 查询当前用户的通知列表（分页）。" — 规格 §3.1

**Verdict:** implemented · confidence: high

**What this demands:** GET 方法、路径 `/web/notification/page`、按当前用户过滤、分页返回。

**Where enforcement lives:** `NotificationController.java:27-35`（`@GetMapping("/page")`）→ `NotificationServiceImpl.pageQuery` `NotificationServiceImpl.java:25-45`（`wrapper.eq(Notification::getUserId, userId)`、`orderByDesc(createdAt)`、`notificationMapper.selectPage`）

**Paths walked:**
- ✓ 正常分页查询路径（含按 `createdAt` 倒序）
- ✓ 用户身份取自 `X-User-Id` 头（网关注入），缺失 → 401（`NotificationController.java:54-57`）
- ✗ 实际运行时分页行为不成立（无 Pagination 插件，见 REQ-13）——本条仅核端点契约存在性

**How the verdict was reached:** 方法、路径、语义与规格一致；分页运行时缺陷单列为 REQ-13，不重复计入本条。

---

## REQ-03 — 查询参数与默认值（page=1, size=20, read 可选）

> "参数 | 类型 | 必填 | 说明 — page | int | 否 | 页码，默认 1 — size | int | 否 | 每页大小，默认 20 — read | boolean | 否 | 筛选已读/未读，不传则全部" — 规格 §3.1

**Verdict:** implemented · confidence: high

**What this demands:** 三参数均可选；默认值 1/20；`read` 不传时不加已读过滤。

**Where enforcement lives:** `NotificationController.java:30-32`（`@RequestParam(defaultValue = "1") Integer page`、`defaultValue = "20") Integer size`、`required = false) Boolean read`）；`NotificationServiceImpl.java:28-30`（`if (read != null) wrapper.eq(Notification::getRead, read)`）

**Paths walked:** ✓ read=null → 不过滤；✓ read=true/false → 等值过滤；✓ 默认值注入

**How the verdict was reached:** 默认值与三态过滤逻辑与规格字面一致。参数合法性校验缺失见 REQ-12。

---

## REQ-04 — 分页响应契约（Result 包裹 records/total/size/current/pages；record 含 id/title/content/type/read/createdAt）

> "Response (200): { \"code\": 200, \"data\": { \"records\": [{ \"id\": 1, \"title\": ..., \"content\": ..., \"type\": \"TASK\", \"read\": false, \"createdAt\": ... }], \"total\": 100, \"size\": 20, \"current\": 1, \"pages\": 5 }, \"message\": \"success\" }" — 规格 §3.1

**Verdict:** implemented · confidence: high（附字段漂移注记）

**What this demands:** 统一返回体 + 分页字段齐全 + 记录字段集合与规格一致。

**Where enforcement lives:**
- 信封：`schemaplexai-common/src/main/java/com/schemaplexai/common/result/Result.java:14-17`（code/message/data/timestamp），`SUCCESS(200, "success")` `ResultCode.java:8`
- 分页对象：`NotificationServiceImpl.java:40-44` 返回 MyBatis-Plus `Page<NotificationVO>`（序列化含 records/total/size/current/pages）
- 记录字段：`schemaplexai-web/src/main/java/com/schemaplexai/web/vo/notification/NotificationVO.java:10-20`（id/title/content/type/read/createdAt，与规格 §4.2 VO 逐字段一致）

**Paths walked:** ✓ 成功响应组装路径（convertToVO `NotificationServiceImpl.java:67-71`，BeanUtils 同名字段拷贝）

**How the verdict was reached:** 规格要求的字段全部存在。漂移注记（不影响裁决）：MP `Page` 的 Jackson 序列化会附带 `orders`/`searchCount`/`optimizeCountSql` 等额外字段，规格示例未列出但属于增量字段；`read` 字段 Lombok 生成 `getRead()`，JSON 键为 `read`，与规格一致。

---

## REQ-05 — PUT /web/notification/{id}/read 标记单条已读

> "`PUT /web/notification/{id}/read` 将单条通知标记为已读。Response (200): { \"code\": 200, \"data\": true ... }" — 规格 §3.1

**Verdict:** implemented · confidence: high

**What this demands:** 路径与方法正确；更新 `read=TRUE`；返回布尔 true。

**Where enforcement lives:** `NotificationController.java:38-44` → `NotificationServiceImpl.markAsRead` `NotificationServiceImpl.java:48-54` → `NotificationMapper.markAsRead` `NotificationMapper.java:12-14`（`UPDATE sf_notification SET read = TRUE, updated_at = NOW() WHERE id = #{id} AND user_id = #{userId} AND deleted = 0`）

**Paths walked:**
- ✓ 命中（affected=1）→ 返回 `Result<Boolean>(data=true)`
- ✓ 未命中（affected=0）→ 404（见 REQ-07）
- ✓ 软删除行（`deleted=0` 条件）不参与

**How the verdict was reached:** SQL 语义、返回类型与规格一致；幂等（重复标记仍返回 true，规格未禁止）。

---

## REQ-06 — PUT /web/notification/read-all 全部已读并返回数量

> "`PUT /web/notification/read-all` 将当前用户所有未读通知标记为已读。Response (200): { ... \"data\": 5 ... }" — 规格 §3.1

**Verdict:** implemented · confidence: high

**What this demands:** 仅更新当前用户的未读行；返回值 = 受影响行数。

**Where enforcement lives:** `NotificationController.java:47-52` → `NotificationServiceImpl.markAllAsRead` `NotificationServiceImpl.java:57-59` → `NotificationMapper.markAllAsRead` `NotificationMapper.java:16-18`（`WHERE user_id = #{userId} AND read = FALSE AND deleted = 0`）

**Paths walked:** ✓ 有未读 → 返回计数；✓ 无未读 → 返回 0；✓ 不影响其他用户（user_id 条件）

**How the verdict was reached:** 返回 `Result<Integer>`，`data` 即受影响行数，与规格示例 `data: 5` 语义一致。

---

## REQ-07 — 标记不存在/无权访问的通知返回 404

> "404 | 通知不存在 | 标记已读时 ID 不存在或无权访问" — 规格 §3.1 错误码表；"标记不存在的通知 | id = 999999 | 返回 404" — 规格 §6

**Verdict:** implemented · confidence: high

**What this demands:** 单条标记已读在 ID 不存在或不属于当前用户时返回错误码 404。

**Where enforcement lives:** `NotificationServiceImpl.java:49-52`（`affected == 0 → throw new BaseException(ResultCode.NOT_FOUND.getCode(), "通知不存在或无权访问")`）；`NOT_FOUND(404, "not found")` `ResultCode.java:14`；`GlobalExceptionHandler.handleBase` `schemaplexai-common/src/main/java/com/schemaplexai/common/exception/GlobalExceptionHandler.java:36-40`（`Result.error(e.getCode(), e.getMessage())`）

**Paths walked:** ✓ id 不存在 → 0 行 → 404；✓ 属于他人 → 0 行 → 404；✓ 软删除行 → 0 行 → 404

**How the verdict was reached:** "不存在"与"无权访问"合并为同一 404 分支，与规格错误码表语义完全一致；单元测试覆盖该分支（`NotificationServiceTest.java:49-55`）。

---

## REQ-08 — 用户级隔离：标记其他用户的通知返回 404（避免信息泄露）

> "标记其他用户的通知 | 当前用户A操作用户B的通知 | 返回 404（避免信息泄露）" — 规格 §6；"权限控制: 只能操作自己的通知" — §7.2

**Verdict:** implemented · confidence: high

**What this demands:** 任何读/改操作均以当前用户为边界，不得确认他人通知的存在性。

**Where enforcement lives:**
- 单条：`NotificationMapper.java:12-14`（`AND user_id = #{userId}`），0 行 → 404，不区分"不存在"与"非你所有"
- 批量：`NotificationMapper.java:16-18`（`WHERE user_id = #{userId}`）
- 列表：`NotificationServiceImpl.java:27`（`wrapper.eq(Notification::getUserId, userId)`）

**Paths walked:** ✓ markAsRead 跨用户 → 404；✓ read-all 只影响本人；✓ page 只返回本人

**How the verdict was reached:** 三个端点全部以 user_id 为强制边界，且错误响应不泄露存在性。集成测试 `NotificationMapperTest.java:54-87` 覆盖跨用户 0 行场景。注记：强制点在 SQL 的 user_id 等值而非拦截器，这是与规格 §6 所述机制（依赖租户拦截器）不同但更强的直接控制。

---

## REQ-09 — 租户隔离由 TenantLineInterceptor 自动注入 SQL 过滤

> "`TenantLineInterceptor` 自动注入 `tenant_id` 过滤" — 规格 §2.2 数据流第 3 步；"标记其他租户的通知 | ... | TenantLineInterceptor 过滤，返回 404" — §6；"租户隔离由 TenantLineInterceptor 保证" — §7.2

**Verdict:** partial · confidence: high

**What this demands:** 承载 `/web/notification/**` 的运行时（schemaplexai-web 应用）中，MyBatis 插件自动为通知相关 SQL 追加 `tenant_id` 条件。

**Where enforcement lives:** （应为而缺失）`schemaplexai-web` 无 `MybatisPlusInterceptor` 注册；现存最接近的工件是 `schemaplexai-dao/src/main/java/com/schemaplexai/dao/config/TenantLineInterceptor.java:11-33`（仅 `TenantLineHandler` 实现，`@Component`）

**Paths walked:**
- ✗ web 运行时：`SchemaPlexaiWebApplication.java:11` `scanBasePackages = {"com.schemaplexai.web"}` → dao 包中的 `TenantLineInterceptor` Bean 不会被实例化；且整个 web 模块无任何 `MybatisPlusInterceptor` Bean（见 Searched）→ `markAsRead`/`markAllAsRead`/`selectPage` 的最终 SQL 均无 `tenant_id` 条件
- ✓ 同租户跨用户：由 user_id 等值兜底（REQ-08），当前不泄露
- ✓ 测试环境：`schemaplexai-dao/src/test/java/com/schemaplexai/dao/config/MyBatisPlusTestConfig.java:18-23` 注册了租户+分页插件——**仅测试上下文**，证明机制本身可用但未进入 web 生产装配
- ✗ 跨租户场景按规格应返回 404：实际仍返回 404，但由 user_id 等值产生（机制漂移），且一旦未来出现不以 user_id 收敛的通知查询/管理端点即失去兜底

**Searched:**
- `MybatisPlusInterceptor|TenantLineInnerInterceptor`（全仓 src/main/java）→ 6 个模块命中（agent-engine、agent-config、context、workflow、spec、system），**schemaplexai-web 与 schemaplexai-task 0 命中**
- `ls schemaplexai-web/.../config/` → 仅 `Knife4jConfig.java`、`WebMvcConfig.java`、`WebSocketConfig.java`
- `TenantContextHolder`（web main）→ 仅 `TenantContextInterceptor.java:27` 设置上下文，无任何 SQL 侧消费方

**How the verdict was reached:** 判为 partial 而非 contradicted：规格承诺的可观测行为（跨租户标记 → 404）在所有现存端点上碰巧仍成立（user_id 全局唯一且全部操作以 user_id 为界）。判为 partial 而非 absent：租户拦截器工件存在且在 6 个其他模块注册，唯独承载本功能的 web 模块未注册。这不是"不同机制满足需求"（implemented 的漂移豁免仅适用于需求本体；§7.2 把"由 TenantLineInterceptor 保证"写成了强制控制项），该控制项在可达路径上未生效。严重度评级见摘要。

---

## REQ-10 — 数据库异常返回 500

> "500 | 系统错误 | 数据库异常" — 规格 §3.1 错误码表

**Verdict:** implemented · confidence: high

**What this demands:** 非业务异常被兜底转换为 code=500 的统一返回体，且不泄露内部信息。

**Where enforcement lives:** `GlobalExceptionHandler.java:79-83`（`@ExceptionHandler(Exception.class)` → `Result.error(ResultCode.ERROR.getCode(), "Internal error")`）；`ERROR(500, "system error")` `ResultCode.java:9`

**Paths walked:** ✓ DB 异常（如 SQL 语法/连接失败）→ catch-all → code=500，消息固定为 "Internal error"

**How the verdict was reached:** 任何未被 `BaseException` 分支捕获的运行时异常均落入该兜底，满足规格的 500 契约。

---

## REQ-11 — sf_notification 表按规格结构落库（新增表迁移）

> "| sf_notification | 新增 | 通知表，含租户、用户、标题、内容、类型、已读状态 |" — 规格 §4.1；"[x] 需要数据库迁移（新增表）" — §7.3

**Verdict:** contradicted · confidence: high · **severity: Critical**

**What this demands:** 标准部署路径（docker-compose 初始化）完成后，`sf_notification` 具备规格结构（含 `read BOOLEAN`），且通知 SQL 可执行。

**Where enforcement lives:**
- 规格一致的 DDL：`docker/postgres/init/04-notification.sql:4-21`（`read BOOLEAN NOT NULL DEFAULT FALSE`、`type VARCHAR(32) DEFAULT 'SYSTEM'`、tenant/user/read 三组索引）
- **冲突的先行 DDL**：`docker/postgres/init/03-init-schema-others.sql:281-292` —— 同名表 `CREATE TABLE sf_notification`，列为 `status VARCHAR(32) DEFAULT 'UNREAD'`，**无 `read` 列**；`03-init-schema-others.sql:409` 索引 `(user_id, status)`
- 挂载：`docker/docker-compose.yml:16`（`./postgres/init:/docker-entrypoint-initdb.d`）

**Paths walked:**
- ✗ 全新初始化：initdb 按字典序执行 01→02→03→04；03 先行创建 `sf_notification`（status 版），04 的裸 `CREATE TABLE sf_notification`（无 `IF NOT EXISTS`、无 `DROP`）必然报 `relation already exists`；官方 postgres 镜像以 `ON_ERROR_STOP=1` 执行 init 脚本 → 04 中途中止（三个索引均未建），且初始化失败可导致容器启动失败
- ✗ 结果库结构 = 03 版（无 `read` 列）：`NotificationMapper.java:12` 与 `:16` 的 `SET read = TRUE`、`NotificationServiceImpl.java:29` 的 `getRead` 过滤、实体字段 `read` 全部对应不存在的列 → 三个端点在真实库上均以 500 失败
- ✓ 测试库：`schemaplexai-dao/src/test/resources/schema.sql:1-14` 单独定义了与实体一致的 `read BOOLEAN` 版（`tenant_id VARCHAR(64)`），因此集成测试通过——**测试库结构与生产初始化脚本脱节**，掩盖了本冲突

**Searched:**
- `sf_notification`（全仓 `**/*.sql`）→ 仅 3 处定义：`03-...:281`、`04-...:4`、`dao/src/test/resources/schema.sql:1`；不存在 `DROP TABLE sf_notification`、不存在第四份协调脚本

**How the verdict was reached:** 判 contradicted 而非 partial：04 脚本内容与规格一致，但标准部署路径上该脚本**不可能生效**，生效的是与规格冲突的结构（`status` 替代 `read`），使整个模块的数据层需求（§4.1/§4.2/§5 的 read 状态）在真实库上全面不成立。判 Critical：规格强制的数据模型控制在唯一标准部署路径上未生效，且直接导致 REQ-02/05/06 的运行时失败。修复方向：删除 03 中的重复定义，或 04 改为幂等迁移（`DROP/IF NOT EXISTS` + 结构升级），并统一测试 schema。

---

## REQ-12 — page/size 输入校验与分页上限 100

> "[x] 输入验证: page/size 参数校验" — 规格 §7.2；"分页上限: 100 条/页" — §7.1

**Verdict:** absent · confidence: high · severity: Medium

**What this demands:** page/size 带约束校验；单页不得超过 100 条。

**Where enforcement lives:** （缺失）`NotificationController.java:21` 有类级 `@Validated`，但参数上无任何约束注解；`@RequestParam(defaultValue = "20") Integer size` 接受任意整数

**Paths walked:**
- ✗ `size=10000` → 原样传入 `new Page<>(page, size)`（`NotificationServiceImpl.java:33`），无截断
- ✗ `page=0`/负数、`size=0`/负数 → 无校验直达 MyBatis-Plus
- ✗ 上限 100 无任何实现痕迹

**Searched:**
- `@Min|@Max|@Positive|@Size(|@NotNull`（`schemaplexai-web/src/main/**`）→ **0 命中**
- `NotificationController.java` 全文 64 行逐行核读 → 无手动范围裁剪

**How the verdict was reached:** 判 absent 而非 partial：不存在任何形式的校验/钳制（无注解、无手工判断、无配置项）。`@Validated` 虽在但无约束可触发，不构成部分实现。后果：配合 REQ-13（无分页插件），任意 `size` 都等价于全表（按 user_id）扫描，NFR §7.1（QPS 500 / P99 100ms）失去基础。

---

## REQ-13 — 分页机制真实生效（LIMIT/计数）

> "分页请求参数…分页返回体：records/total/current/size/pages"（项目统一 API 规范，规格 §3.1 响应示例依赖之）；规格 §3.1 要求 `total: 100, pages: 5` 的语义

**Verdict:** absent · confidence: high · severity: High

**What this demands:** `/page` 端点实际执行受 `size` 约束的分页查询并给出正确 `total`/`pages`。

**Where enforcement lives:** （缺失）MyBatis-Plus 的 `selectPage` 依赖 `PaginationInnerInterceptor` 才会生成 count 查询与 `LIMIT`；web 运行时未注册任何此类插件

**Paths walked:**
- ✗ 运行时路径：`NotificationServiceImpl.java:33-34` `selectPage(pageParam, wrapper)` 在无分页插件时退化为不带 LIMIT 的全量查询：`records` = 全部命中行、`total` 恒 0、`pages` 恒 0 → 响应契约 §3.1 的 `total`/`pages` 语义在所有请求上不成立
- ✓ 测试路径：`MyBatisPlusTestConfig.java:21` 为 dao 测试注册了 `PaginationInnerInterceptor`——再次仅测试上下文

**Searched:**
- `Pagination|selectPage`（`schemaplexai-web/**`）→ main 中仅 `NotificationServiceImpl.java:34` 与 `EngineExecutionQueryPort.java:40` 两处调用，**无插件注册**
- `MybatisPlusInterceptor`（web main）→ 0 命中（同 REQ-09 Searched）

**How the verdict was reached:** 判 absent 而非 partial：分页能力在运行时装配中完全不存在（不是参数传错或实现不全）。这也解释了为何 REQ-12 的上限缺失没有造成"单页超 100"而是更糟的"单页无界"。严重度 High：每个列表请求都以错误契约响应（total=0）且结果集无界。

---

## REQ-14 — 数据模型：Entity 与 VO 字段定义

> "```java @TableName(\"sf_notification\") public class Notification extends BaseEntity { private Long tenantId; private Long userId; private String title; private String content; private String type; // SYSTEM, TASK, WORKFLOW private Boolean read; // false = 未读 }``` …NotificationVO { id, title, content, type, read, createdAt }" — 规格 §4.2

**Verdict:** implemented · confidence: high（文档漂移注记）

**What this demands:** 实体具备租户/用户/标题/内容/类型/已读字段；VO 六字段。

**Where enforcement lives:**
- Entity：`Notification.java:11-23`（userId/title/content/type/read 显式声明；`@TableName("sf_notification")`）；`tenantId` 继承自 `BaseEntity.java:21-22`
- VO：`NotificationVO.java:10-20` 六字段与规格逐一对应

**Paths walked:** ✓ Entity→表列映射（underscore↔camel）；✓ Entity→VO 拷贝（`NotificationServiceImpl.java:67-71` 同名字段）

**How the verdict was reached:** 字段集合完整 → implemented。两处漂移注记（不改裁决，建议修文档或修代码后更新规格）：
1. 规格声明 `private Long tenantId;`，实现继承 `BaseEntity.tenantId` 且类型为 **String**（`BaseEntity.java:21-22`），DDL 列为 `BIGINT`（`04-notification.sql:7`）——功能等价但类型漂移；
2. `type` 取值 SYSTEM/TASK/WORKFLOW 无任何强制（DDL `VARCHAR(32) DEFAULT 'SYSTEM'`、实体无枚举），且 MQ 消费路径实际写入集合外的 `"IN_APP"`（`NotificationConsumer.java:116`）。

---

## REQ-15 — 状态模型：仅 read = false/true 两态

> "无复杂状态机。通知只有 `read = false / true` 两种状态。" — 规格 §5

**Verdict:** implemented · confidence: high

**What this demands:** 已读状态为布尔两态；写入路径默认未读。

**Where enforcement lives:** DDL `read BOOLEAN NOT NULL DEFAULT FALSE`（`04-notification.sql:11`）；实体 `Boolean read`（`Notification.java:23`）；写入路径 `NotificationServiceImpl.java:63`（`setRead(false)`）、`NotificationConsumer.java:117`（`setRead(false)`）；标记路径仅置 `TRUE`（`NotificationMapper.java:12,16`）

**Paths walked:** ✓ 新建=未读；✓ 单条/全部标记=置 TRUE；✓ 无反向（取消已读）路径

**How the verdict was reached:** 规格模块内所有路径均符合两态模型。注意：`schemaplexai-ops` 平行实现使用 `Integer status` 0/1 三值化语义（`ops/NotificationServiceImpl.java:24-25`），与本规格冲突——但属规格外实现，计入反向差距而非本条裁决。

---

## REQ-16 — 前端 TypeScript 类型定义

> "前端 TypeScript 类型定义" — 规格 §1.2 范围；"[x] 需要前端配合（新增类型和 API 调用）" — §7.3

**Verdict:** implemented · confidence: high

**What this demands:** 前端存在与契约一致的通知类型与 API 调用封装。

**Where enforcement lives:**
- `schemaplexai-ui/src/types/notification.ts:1-18`：`NotificationType = 'SYSTEM' | 'TASK' | 'WORKFLOW'`；`Notification`（id/title/content/type/read/createdAt）；`NotificationPageResult`（records/total/size/current/pages）——与 §3.1/§4.2 逐字段一致
- `schemaplexai-ui/src/api/notification.ts:10-20`：`GET /web/notification/page`、`PUT /web/notification/${id}/read`、`PUT /web/notification/read-all` 三调用齐全

**Paths walked:** ✓ 三个端点调用路径与后端路由逐一对应

**How the verdict was reached:** 类型与调用均存在且与契约一致。前端是否渲染消息中心页面不在本规格范围（§1.2 仅要求类型定义）。

---

## REQ-17 — 基础单元测试和集成测试

> "基础单元测试和集成测试" — 规格 §1.2 范围

**Verdict:** partial · confidence: high · severity: Low

**What this demands:** 通知模块具备基础级别的单元测试与集成测试。

**Where enforcement lives:**
- 单元（Mockito）：`schemaplexai-web/src/test/java/com/schemaplexai/web/controller/NotificationControllerTest.java:27-47`、`.../service/notification/NotificationServiceTest.java:39-64`（含 404 分支）、`.../NotificationServiceImplTest.java:35-77`、`schemaplexai-model/src/test/java/com/schemaplexai/model/entity/notification/NotificationTest.java:11-48`、`schemaplexai-web/src/test/java/com/schemaplexai/web/controller/WebControllerTest.java:105-125`
- 集成（@SpringBootTest + 真实 SQL）：`schemaplexai-dao/src/test/java/com/schemaplexai/dao/mapper/notification/NotificationMapperTest.java:15-130`（TestApplication + schema.sql + 租户上下文，覆盖 insert/单条/全部已读/跨用户）

**Paths walked:**
- ✓ Service/Mapper/Entity 单元与集成路径有测试
- ✗ Controller 层仅有直接方法调用的 Mockito 测试，**无 @WebMvcTest/MockMvc HTTP 级测试**（契约、头部解析、异常→HTTP 映射未覆盖）
- ✗ 集成测试使用的 `schema.sql`（`read BOOLEAN`）与生产初始化脚本实际生效结构（03 版 `status`）不一致 → 集成测试无法发现 REQ-11 级别的部署缺陷

**Searched:**
- `Notification`（`schemaplexai-web/src/test/**`）→ 4 个文件，无 `MockMvc`/`@WebMvcTest` 形态
- `@SpringBootTest`（notification 相关）→ 仅 dao `NotificationMapperTest.java:15`

**How the verdict was reached:** "基础单元测试"足额；"集成测试"仅达 DAO 层且基于与生产脱节的 schema。字面要求勉强满足，但按核查深度判 partial（缺 HTTP 层、测试 schema 失真）。若按最宽松读法可视为 implemented——分歧点已在上方明示。

---

## REQ-18 — 非目标遵守：不引入实时推送/邮件短信/模板/管理员发送 UI

> "- 实时推送（WebSocket/SSE） - 邮件/短信渠道 - 通知模板管理 - 管理员发通知的 UI" — 规格 §1.3

**Verdict:** absent · confidence: high（对非目标而言为**合规**裁决，用 absent 表示"规格未要求之物在规格承载模块中不存在"）· 反向越界见反向差距

**What this demands:** v1 通知模块本体不包含被排除的能力。

**Where enforcement lives:** web 模块三个端点无任何邮件/短信/推送/模板逻辑（`NotificationController.java`、`NotificationServiceImpl.java` 全文）

**Paths walked:**
- ✓ web 模块内无邮件/短信/模板/推送代码
- ✗（越界，计入反向差距）`NotificationMessage.java:22-26` 携带 `templateCode`/`templateParams`/`webhookUrl`/`webhookHeaders` 字段且无人消费；`schemaplexai-ops/NotificationController.java:58-65` 暴露管理侧 `POST /ops/notifications/send`（无 UI，仅 API）

**How the verdict was reached:** 规格承载模块（web）严格遵守非目标；越界能力位于规格外模块（task/ops），按反向差距处理，不判 contradicted。

---

# notChecked

| 项 | 规格出处 | 原因 |
|----|---------|------|
| QPS 500 / P99 100ms 性能目标 | §7.1 | 无任何负载测试或基准设施，静态核查无法裁决 |
| §7.3 "不破坏现有 API（全新模块）"的完整回归 | §7.3 | 需全量 API 回归；本模块路径全新（`/web/notification/**`）未发现路由冲突，仅作初步确认 |

# 反向差距（实现存在、规格未提及）

1. **MQ 投递链路（整条管线规格未提及）**：`NotificationConsumer.java`（task 模块，`@RabbitListener("sf.notification.queue")`）+ `RabbitMqConfig.java:68-81`（队列/DLX 绑定）+ `NotificationMessage.java`。行为：仅投递 `in-app` 通道，其余通道 nack→DLQ；幂等去重（`InboxDeduplicationService`）。与 `docs/decisions/ADR-013-notification-v1-channel-reduction.md` 对应，但该 ADR 声称改动位于 `schemaplexai-ops`（ADR §影响），实际位于 `schemaplexai-task` —— 文档漂移。
2. **MQ 生产方**：`SpecReviewNotificationDelegate.java:103-142`（workflow 模块）向 `sf.notification` routing key 发布 in-app 通知。
3. **疑似运行时死链（Open question）**：`NotificationConsumer.java:31` `@ConditionalOnBean(InboxDeduplicationService.class)`；`SchemaPlexaiTaskApplication.java:15` 仅扫描 `com.schemaplexai.task` 且无 `@Import`，quality 模块的 `@Service InboxDeduplicationService` 与 `SfProcessedEventMapper` 均未进入 task 上下文 → 该消费者在默认装配下**可能根本不注册**，`sf.notification.queue` 消息无人消费。置信度 medium，需运行时验证。
4. **模板/回调管道死字段**：`NotificationMessage.java:22-26` 的 templateCode/templateParams/webhook* 无任何消费代码（规格非目标"通知模板管理"的代码残留）。
5. **ops 模块平行实现**：`schemaplexai-ops` 的 `NotificationController/ServiceImpl` 提供 CRUD+send+批量已读，使用 `Integer status`(0/1) 状态模型（`ops/NotificationServiceImpl.java:24-25`）——既不匹配 03 版（`status VARCHAR 'UNREAD'`）也不匹配 04 版（`read BOOLEAN`）DDL，两套生产结构下该组端点均无法工作。
6. **web 内部 `sendNotification` 无调用方**：`NotificationServiceImpl.java:62-65` 不设置 tenantId（`@TableField(fill=INSERT)` 且全仓**不存在** `MetaObjectHandler`，搜索 `MetaObjectHandler` → 0 命中）→ 若被调用将因 `tenant_id NOT NULL` 违反而失败。
7. **`type` 集合外值**：消费者写入 `"IN_APP"`（`NotificationConsumer.java:116`），超出规格 SYSTEM/TASK/WORKFLOW。
8. **配置缺口（Open question）**：`schemaplexai-web/src/main/resources/application.yml`（全 29 行）**无任何 `spring.datasource` 配置**，通知端点所在服务在默认打包形态下无数据库连接（需外部注入环境变量方可运行）。

# Open questions

1. task 模块 `@ConditionalOnBean` 装配缺口（反向差距 3）需启动验证。
2. web 服务数据源是否由部署环境注入（反向差距 8）。
3. 04 与 03 的 DDL 冲突是否存在未入库的手工协调步骤（部署文档未声明）。
4. `TenantContextInterceptor.java:17` 允许非数字租户号（`^[a-zA-Z0-9_-]+$`）而 `tenant_id` 列为 `BIGINT`，字符串租户号将导致 SQL 类型错误（与 03/04 冲突相关联的次生问题）。

## 反驳复核

> 独立复核员（非原分析作者）对 severity ∈ {Critical, High} 且 verdict ∈ {contradicted, partial, absent} 的 2 条分歧条目逐条尝试推翻。复核方法：重读被引用代码行与两份 DDL、核对 docker-compose 挂载与官方 postgres 镜像的 ON_ERROR_STOP 行为、全仓检索插件注册与自动装配文件、核对规格条款强制性。结论：2 条均无法推翻，维持原判。

| REQ | 原判 | 裁定 | 理由(一行) | 证据 |
|-----|------|------|-----------|------|
| REQ-11 | contradicted / Critical | 维持 | initdb 按字典序执行（009→01→02→03→04），03:281-292 先行创建无 `read` 列的 `status` 版 `sf_notification`，04:4 为裸 `CREATE TABLE`（无 IF NOT EXISTS / DROP），官方 postgres 镜像以 `psql -v ON_ERROR_STOP=1` 执行 init 脚本（compose 未覆写 entrypoint）→ 04 必报 `relation already exists` 而中止（3 个索引未建），初始化失败可致容器退出；全仓无 `DROP TABLE sf_notification`、无协调脚本，规格 §4.1（含已读状态的新增表）与 §7.3"[x] 需要数据库迁移"均为强制条款，标准部署路径上规格结构不可能落库 | `docker/postgres/init/03-init-schema-others.sql:281-292,409`；`docker/postgres/init/04-notification.sql:4-21`；`docker/docker-compose.yml:16`（`./postgres/init:/docker-entrypoint-initdb.d`，postgres 服务无 command/entrypoint 覆写）；全仓 `**/*.sql` grep `sf_notification` 仅 3 处定义（第三处为测试专用 `schemaplexai-dao/src/test/resources/schema.sql:1`）；规格 §4.1 / §5 / §7.3 |
| REQ-13 | absent / High | 维持 | 分页拦截器不在任何共享装配中：`MybatisPlusInterceptor`/`PaginationInnerInterceptor` 全仓仅出现于 6 个业务服务模块各自包内的 `@Configuration`，`schemaplexai-common/model/dao` 零注册，唯一的 `AutoConfiguration.imports` 仅列异常/JWT/可观测 4 项；web 虽依赖 `schemaplexai-agent-config`（其 `MyBatisPlusConfig` 在类路径上），但 `scanBasePackages={"com.schemaplexai.web"}` 排除该包、`@Import` 仅 3 个服务实现类且全仓无传递 `@Import` 链、`@MapperScan` 只注册 Mapper 接口 → web 运行时无分页插件，`selectPage` 退化为无 LIMIT 全量查询、`total`/`pages` 恒 0，违反规格 §3.1 强制响应契约 | `SchemaPlexaiWebApplication.java:11-21`；`schemaplexai-common/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports:1-4`；`schemaplexai-agent-config/.../config/MyBatisPlusConfig.java:11-24`（在类路径但扫描范围外）；`NotificationServiceImpl.java:33-34`；全仓 grep `MybatisPlusInterceptor` 仅 6 模块命中（web/dao/common/model 0 命中）；规格 §3.1 响应示例（`total: 100, pages: 5`） |
