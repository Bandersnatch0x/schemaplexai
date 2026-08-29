# RAG Pipeline Spec 合规核查报告

- **Spec**: `docs/specs/2026-04-30-v1.0-rag-pipeline.md`（注意：文件 front matter 为 `status: 草稿`，与委托描述"已批准"不一致，见 Open questions）
- **核查日期**: 2026-08-29
- **核查范围**: `schemaplexai-context`（主）、`schemaplexai-task`（MQ 消费 / 对账）、`schemaplexai-common`、`schemaplexai-dao`、`schemaplexai-model`、`schemaplexai-gateway`、`schemaplexai-ui/src/api`、`docker/postgres/init/03-init-schema-others.sql`、两模块 `application.yml` 与 `pom.xml`
- **方法**: 通读 spec → 提取 20 条可核查需求 → 文本搜索定位执行点 → 逐文件通读调用方/被调用方，走完正常/失败/降级路径 → 逐条裁决

## 裁决统计

| Verdict | 数量 | REQ |
|---|---|---|
| implemented | 5 | 04, 05, 06, 09, 10 |
| partial | 9 | 01, 03, 12, 13, 14, 15, 17, 18, 19 |
| contradicted | 5 | 02, 07, 08, 11, 16 |
| stronger-than-spec | 0 | （局部强于 spec 的行为并入各条注记与"反向差距"） |
| absent | 0 | （absent 级缺失均附着在 partial 条目的具体 ✗ 路径中，附 Searched 记录） |
| undecidable | 1 | 20 |

## 贯穿性事实（多条裁决共享的前提，均已独立验证）

- **F1 — 仓库自带配置下 RAG 能力全关**: `schemaplexai-context/src/main/resources/application.yml`（全文 42 行）不含任何 `milvus.*` / `minio.*` / `embedding.*` / `spring.rabbitmq.*` 键。`MilvusConfig.java:16`、`MilvusCollectionInitializer.java:28`、`MilvusSyncServiceImpl.java:40`、`RagSearchServiceImpl.java:25` 均 `@ConditionalOnProperty(name="milvus.enabled", havingValue="true", matchIfMissing=false)`；`MinioFileStorageService.java:30` 同理（`minio.enabled`）。缺省生效的是 `NoOpMilvusSyncServiceImpl.java:16`（matchIfMissing=true，仅打 warn）与 `DisabledFileStorageService.java:15-21`（upload 恒抛 "File storage is disabled"）。
- **F2 — context 服务从不注入租户上下文**: 全仓库 `src/main` 中 `TenantContextHolder.setTenantId` 共 11 处（agent-config×4、agent-engine×1、quality×1、task×1、web×1、workflow×3），**0 处在 schemaplexai-context**。`SchemaPlexaiContextApplication.java:7` 仅扫描 `com.schemaplexai.context` + `com.schemaplexai.dao`；`schemaplexai-common`/`schemaplexai-model` 的 main 中无任何 Filter/Interceptor（grep `OncePerRequestFilter|implements Filter|HandlerInterceptor` → 0 命中）。网关虽会注入 `X-User-Id`/`X-Tenant-Id` 头，但 context 服务内无人读取 → 该服务内 `TenantContextHolder.getTenantId()` 恒为 null。
- **F3 — `sf.milvus.sync` 异步链路是死端**: 唯一 `@RabbitListener` 在 `schemaplexai-task/.../mq/MilvusSyncConsumer.java:46`（队列 `sf.milvus.sync.queue`），其 handler 的唯一实现 `UnsupportedMilvusSyncRequestHandler.java:13-19` 恒抛 `BaseException`。context 模块 pom（`schemaplexai-context/pom.xml:7-24`）**无 spring-boot-starter-amqp 依赖**（grep `amqp|rabbit` → 0 命中），其 `mq/MilvusSyncConsumer.java:13-27` 无 `@RabbitListener` 注解、无任何调用方注入消息，为不可达死代码。

---

## REQ-01 — 文档上传：原始文件存入 MinIO

> "知识文档上传与存储（MinIO）" / 数据流图 "用户上传文档 → MinIO (原始文件)" — spec §1、§2

**Verdict:** partial · confidence: high

**What this demands:** 存在上传入口，接收文件、写入 MinIO 并返回对象地址，供后续提取/向量化使用。

**Where enforcement lives:**
- `FileUploadController.java:32-72` — `POST /context/files/upload`（网关 `GatewayConfig.java:24-25` 将 `/context/**` 路由到 context 服务）
- `MinioFileStorageService.java:65-87` — `putObject` 写入，对象名 `UUID-原文件名`（L67），返回 `endpoint/bucket/object`（L78）；bucket-per-tenant `sf-files-{tenantId}`（L33、L94-99）
- `FileUploadController.java:35-38` — 租户缺失即 400（fail-closed）

**Paths walked:**
- ✓ 正常路径：multipart → 空文件检查（L40-42）→ 50MB 限制（L44-46）→ 病毒扫描健康检查（L48-50）→ 扫描（L53）→ putObject → 返回 URL
- ✗ 租户路径：F2 下 `TenantContextHolder.getTenantId()` 恒 null → **该端点在仓库自带形态下恒返回 400 "Tenant ID missing"**
- ✗ 存储关闭路径：F1 下默认 Bean 为 `DisabledFileStorageService.java:19-21`，恒抛 500 "File storage is disabled"
- ✓ MinIO 异常路径：`MinioFileStorageService.java:83-86` 包装为 `BaseException`；凭证缺失 `@PostConstruct` fail-fast（L52-56）
- ✗ 与知识文档的衔接：上传与建档是两个端点（`/context/files/upload` 与 `POST /context/knowledge-docs`，后者收 JSON 体 `KnowledgeDocController.java:22-27`），由客户端手工传 `fileUrl` 衔接；spec §2 数据流中"上传→提取"是同一条流水线，实际为两步人工拼接

**Searched:** `minio` in context `application.yml` → 0 命中；`TenantContextHolder.setTenantId`（全仓库 src/main）→ 11 处，0 处在 context。

**How the verdict was reached:** MinIO 写入能力完整（非 absent）；但租户注入与 `minio.enabled` 两个前置在仓库形态下均不成立，端到端上传不可用且为 fail-closed（无泄漏）。方向未违背 spec，故非 contradicted。**Severity: High**（主链路入口在自带配置下不可用）。

---

## REQ-02 — sf_knowledge_doc 数据模型与状态枚举

> "**sf_knowledge_doc**: id BIGINT / tenant_id BIGINT / name VARCHAR / object_path VARCHAR / chunk_count INT / status VARCHAR PENDING / PROCESSING / INDEXED / FAILED / version_id BIGINT" — spec §3.1

**Verdict:** contradicted · confidence: high

**What this demands:** 表含规格字段集，且状态取值域为 {PENDING, PROCESSING, INDEXED, FAILED}。

**Where enforcement lives:**
- `docker/postgres/init/03-init-schema-others.sql:101-116` — 实际列：`id, tenant_id, title, file_name, file_url, file_size, status(DEFAULT 'ACTIVE'), doc_type, sync_status(DEFAULT 'PENDING')` + 审计列
- `SfKnowledgeDoc.java:11-20` — 实体：`title, fileName, fileUrl, fileSize, status, syncStatus, docType`
- `MilvusSyncServiceImpl.java:109` — 同步入口接受 `UPLOADED / PENDING / FAILED`
- `MilvusSyncServiceImpl.java:134-135`、`FailedStatusWriter.java:34-35` — 终态写入 `SYNCED` / `FAILED`

**Paths walked:**
- ✓ `tenant_id BIGINT NOT NULL` 存在（SQL L103），实体经 `BaseEntity.java:22` 提供 `tenantId`
- ✗ `chunk_count` 缺失：全模块无任何维护分块数的代码（`MilvusSyncServiceImpl` 只 log chunks.size()，L123，不落库）
- ✗ `version_id` 缺失：实体与表均无该列；`sf_knowledge_doc_version` 无任何写入方（见 REQ-03）
- ✗ `name`/`object_path` 被 `title`+`file_name`/`file_url` 替代（命名漂移，可容忍）
- ✗ 状态词表矛盾：`INDEXED` 与 `PROCESSING` 在 context+task 的 main 代码中 0 命中（grep 验证，`PROCESSING` 唯一命中在无关的 `MqIdempotencyInterceptor.java:70`）；实际使用 `UPLOADED`（代码）、`ACTIVE`（SQL 默认值）、`SYNCED`，且分裂为 `status`/`sync_status` 双字段（SQL L108、L110）

**Searched:** `INDEXED|PROCESSING`（context+task main）→ 0 处状态语义命中；`chunk_count|version_id`（context main、SQL）→ 0 命中。

**How the verdict was reached:** 这不是"以不同机制满足"：spec 明确列出状态枚举作为数据契约，代码词表与之无交集核心（仅 PENDING/FAILED 重合）且 spec 要求的两个字段（chunk_count、version_id）不存在。判 contradicted 而非 partial。**Severity: Medium**（数据契约漂移 + 版本/分块计数能力缺失，但无安全后果）。

---

## REQ-03 — sf_knowledge_doc_version 数据模型（版本管理）

> "**sf_knowledge_doc_version**: id / doc_id BIGINT / version INT / content_hash VARCHAR / chunk_count INT / created_at TIMESTAMP" — spec §3.1（§2 数据流图亦列出该表）

**Verdict:** partial · confidence: high

**What this demands:** 版本表存在且含规格字段，文档变更产生版本记录（数据流图将其置于写入侧）。

**Where enforcement lives:**
- `03-init-schema-others.sql:118-128` — 实际列：`id, tenant_id, doc_id, version INT, file_url, change_log, created_at, created_by, deleted`
- `SfKnowledgeDocVersion.java:11-17` — 实体：`docId, version(String!), fileUrl, changeLog`
- `SfKnowledgeDocVersionMapper.java` — 存在

**Paths walked:**
- ✓ 表存在；`doc_id`、`version INT`、`created_at` 与规格一致
- ✗ `content_hash` 缺失（表与实体均无）
- ✗ `chunk_count` 缺失
- ✗ 无任何写入/读取路径：`SfKnowledgeDocVersionMapper` 与 `KnowledgeDocVersion` 在 context main 中除自身定义外 0 引用（grep 验证）；`uploadAndVectorize`/`updateById` 均不产生版本记录
- ✗ 模块内部不一致：SQL `version INT`（L122）vs 实体 `String version`（L14）

**Searched:** `SfKnowledgeDocVersionMapper|KnowledgeDocVersion`（context src/main）→ 仅实体与 mapper 自身；`content_hash`（SQL、context main）→ 0 命中。

**How the verdict was reached:** 表骨架存在（非 absent），但规格字段缺 2/6 且整表零写入（版本管理功能整体不工作），故非 implemented；机制方向未反转，故非 contradicted。**Severity: Low**（当前无调用方受影响，属沉默缺失）。

---

## REQ-04 — Apache Tika 文本提取

> "文本提取（Apache Tika）" / 消费者步骤 "3. Tika 提取文本" — spec §1、§3.4

**Verdict:** implemented · confidence: high

**What this demands:** 从 MinIO 下载原始文件后用 Tika 解析为文本；解析失败可见、不静默吞掉。

**Where enforcement lives:**
- `MilvusSyncServiceImpl.java:245-254` — `new Tika().parseToString(is)`，异常包装为 `BaseException(INTERNAL_ERROR)` 向上抛
- `MilvusSyncServiceImpl.java:191-213` — MinIO `getObject` 下载，下载或提取失败统一包装并重抛
- `schemaplexai-context/pom.xml:14-15` — `tika-core` + `tika-parsers-standard-package` 依赖

**Paths walked:**
- ✓ 正常路径：extractText → resolveTenantBucket/resolveObjectName → getObject → Tika
- ✓ MinIO 未启用路径：`MilvusSyncServiceImpl.java:186-189` 显式抛错（fail-closed，不伪造空文本）
- ✗ 租户 bucket 往返缺陷：`resolveObjectName`（L216-232）只剥离默认桶前缀 `documents/`（L224-226），而 `FileUploadController` 存入的是 `endpoint/sf-files-{tenantId}/obj`（`MinioFileStorageService.java:78`）→ 对象名会携带桶前缀重复（`sf-files-{t}/obj` 作为 object name），`getObject` 将 NoSuchKey。测试未暴露：`MilvusSyncServiceImplTest.java:255-266` 对 `getObject(any())` 做无差别 mock。见 Open questions #2
- ✓ 提取失败路径：异常 → `doSync` catch（L139-144）→ `FailedStatusWriter.markFailed` + 重抛

**How the verdict was reached:** Tika 集成真实存在且被流水线使用，失败语义正确（非 partial）；租户桶往返缺陷属于"下载路径"的集成问题，记入上方 ✗ 与 REQ-13，不推翻"使用 Tika"本身。文档漂移注记：spec 将 Tika 置于消费者（MQ）内，实际位于同步服务中。

---

## REQ-05 — 分块参数：512 / overlap 50

> "CHUNK_SIZE = 512; CHUNK_OVERLAP = 50"（注释 "tokens"） — spec §3.2

**Verdict:** implemented · confidence: high

**What this demands:** 分块尺寸 512、重叠 50 的滑动窗口分块。

**Where enforcement lives:**
- `ChunkingConfig.java:11`（chunkSize=512）、`:16`（overlap=50）、`:21`（splitBySentence 默认 true）、`:50-52`（defaults()）
- `DocumentChunker.java:118-140` — 字符级滑动窗口 `step = chunkSize - overlap`（L120），与 spec 参考代码逐行同构
- `MilvusSyncServiceImpl.java:122` — 流水线使用 `ChunkingConfig.defaults()`

**Paths walked:**
- ✓ 短文本路径：≤512 单块返回（L32-39）
- ✓ 字符路径：与 spec §3.2 参考实现等价（含 `end == text.length()` 提前终止，L134-136）
- ✓ 句子路径（默认启用）：优先按句边界拼接，超尺寸回退（L52-113），重叠取上一块尾部文本（L90-93）
- ✓ null/空白输入 → 空列表（L24-26）

**How the verdict was reached:** 数值参数与参考算法一致。两点注记：(1) **文档漂移**——spec 注释写 "tokens" 但参考代码本身是字符实现（`text.length()`/`substring`），代码与参考实现同为字符级，按"代码以不同机制满足=implemented"处理；(2) 默认句子边界模式产生与参考代码不同的切分边界，属增强而非违背（重叠与尺寸约束保持）。**Severity: —（无分歧）**。

---

## REQ-06 — Embedding：1536 维 / OpenAI text-embedding-3-small

> "embedding: FLOAT_VECTOR(1536)  # OpenAI text-embedding-3-small" — spec §3.3（§1 "Embedding 生成"）

**Verdict:** implemented · confidence: high

**What this demands:** 向量化服务产出 1536 维向量，默认模型为 OpenAI text-embedding-3-small。

**Where enforcement lives:**
- `EmbeddingServiceImpl.java:47` — `EMBEDDING_DIMENSION = 1536`
- `EmbeddingServiceImpl.java:52` — `embedding.openai.model:text-embedding-3-small`
- `EmbeddingServiceImpl.java:117-128` — provider 分发（openai / ollama / mock）；`EmbeddingService.java` 接口 `embed` + `embedBatch`
- `MilvusSyncServiceImpl.java:125-128` — 流水线调用 `embedBatch`；`RagSearchServiceImpl.java:47` — 查询侧 `embed`

**Paths walked:**
- ✓ openai 路径：API key 缺失抛错（L145-148）、空响应/空数据抛错（L169-186）、HTTP 超时受限（connect 5s / read 30s，L81-97）
- ✓ mock 路径：确定性 1536 维（L130-140）；**stronger-than-spec**：prod profile 下 mock 启动即失败（L104-115）
- ✓ 失败传播：查询侧 embedding 失败显式重抛、不静默降级（`RagSearchServiceImpl.java:44-56`）
- ⚠ 注记：默认 `embedding.provider=mock`（L80）；openai 路径直接取响应向量长度、不校验 ==1536（L188），若换模型将在 Milvus 插入时失败（fail-closed，可接受）

**How the verdict was reached:** 维度与默认模型与规格一致；mock 默认是开发态配置且带生产守卫，判 implemented（附注记）而非 stronger-than-spec。**Severity: —（无分歧）**。

---

## REQ-07 — Milvus 集合名 `knowledge_chunks`

> "Collection: knowledge_chunks" — spec §3.3（§2 数据流图同）

**Verdict:** contradicted · confidence: high

**What this demands:** 集合名为 `knowledge_chunks`。

**Where enforcement lives:**
- `MilvusProperties.java:15` — 默认 `collectionName = "knowledge_doc_embedding"`
- `milvus/knowledge_doc_collection.json:2` — `"collectionName": "knowledge_doc_embedding"`
- `MilvusReconciliationService.java:28-29` — 对账侧默认同为 `knowledge_doc_embedding`（两处一致，无内部漂移）

**Searched:** `knowledge_chunks`（全仓库 src/main 与 resources）→ 0 命中。

**How the verdict was reached:** 名称是显式契约且代码内部一致地使用了另一个名字，判 contradicted（文档漂移，非机制等价问题——运维按规格建集合将连不上）。**Severity: Low**（代码内部自洽，改文档或改配置即可）。

---

## REQ-08 — Milvus 集合字段结构

> "id: INT64, primary_key, auto_id / content: VARCHAR(4096) / doc_id: INT64 / tenant_id: INT64 / embedding: FLOAT_VECTOR(1536) / version: INT" — spec §3.3

**Verdict:** contradicted · confidence: high

**What this demands:** 六字段、指定类型、id 自增主键、含 version 字段。

**Where enforcement lives:** `milvus/knowledge_doc_collection.json:4-40`（由 `MilvusCollectionInitializer.java:57-98` 在启动时建集合）

**Paths walked（逐字段对照）:**
- ✗ `id`：VarChar(64) 主键、`autoID(false)`（json L5-10；`MilvusCollectionInitializer.java:108-111`），插入端用 `UUID.randomUUID()`（`MilvusSyncServiceImpl.java:274`）—— spec 要求 INT64 auto_id
- ✗ `doc_id`：VarChar(64)（json L11-15；插入端 `docId.toString()`，L275）—— spec 要求 INT64
- ✗ `tenant_id`：VarChar(64)（json L30-35）—— spec 要求 INT64
- ✗ `content`：VarChar(65535)（json L20-24）—— spec 要求 4096（扩大，不破坏兼容）
- ✗ `version` 字段：**不存在**（json 无、插入行无、检索输出字段无）
- ✓ `embedding`：FLOAT_VECTOR(1536)（json L25-29）
- ➕ 额外字段：`chunk_index Int32`（json L16-19）、`created_at Int64`（L36-39）
- ⚠ 跨层类型链：PG `tenant_id BIGINT`（SQL L103）→ 实体 `String tenantId`（`BaseEntity.java:22`）→ Milvus VarChar —— 三层类型互不一致

**Searched:** `"version"` in `knowledge_doc_collection.json` → 0 命中；`row.put("version"`（context main）→ 0 命中。

**How the verdict was reached:** 语义要素（doc 关联、租户键、向量、正文）都在，但规格逐字列出的类型契约 4/6 不符、1 字段缺失。这超出"不同机制"范畴（类型影响运维与反查契约），判 contradicted。**Severity: Low**（功能自洽，反查可用字符串 doc_id 完成）。

---

## REQ-09 — 索引：IVF_FLAT / nlist=128 / COSINE

> "Index: embedding: IVF_FLAT, nlist=128, metric=COSINE" — spec §3.3

**Verdict:** implemented · confidence: high

**What this demands:** embedding 字段按该索引类型/参数建索引。

**Where enforcement lives:**
- `knowledge_doc_collection.json:41-50` — `IVF_FLAT` / `COSINE` / `nlist:128`
- `MilvusCollectionInitializer.java:132-159` — 按 JSON 动态建索引，`extraParams` 透传（L142-150）

**Paths walked:**
- ✓ 集合已存在则跳过创建（L43-46）——注意此时**不会**补建/校验索引（见 Open questions #4）
- ✓ 新建路径：先 createCollection 再 createIndex（L88-96）

**How the verdict was reached:** 三参数与规格逐字一致。**Severity: —（无分歧）**。

---

## REQ-10 — Partition Key = tenant_id

> "Partition Key: tenant_id" — spec §3.3

**Verdict:** implemented · confidence: high

**What this demands:** tenant_id 作为分区键，实现物理租户分区。

**Where enforcement lives:**
- `knowledge_doc_collection.json:31-34` — `tenant_id` 字段 `"isPartitionKey": true`
- `MilvusCollectionInitializer.java:80-86` — 检测到 partition key 字段后 `numPartitions(64)`；L121-123 设置 `isPartitionKey`

**Paths walked:** ✓ 新建集合路径带分区键；✓ 写入端每行都写 `tenant_id`（`MilvusSyncServiceImpl.java:279`，null 时兜底 "default"，L258）。

**How the verdict was reached:** 与规格一致；`numPartitions=64` 为规格未约定的实现细节（注记）。**Severity: —（无分歧）**。

---

## REQ-11 — 生产者：ingest 完成后经 `sf.milvus.sync` 异步触发同步

> "**生产者**: `RagServiceImpl.ingestDocument()` 完成后发送 MQ 消息" / 数据流 "异步: sf.milvus.sync 队列" — spec §3.4、§2

**Verdict:** contradicted · confidence: high

**What this demands:** 文档入库完成后，由生产者向 `sf.exchange` / `sf.milvus.sync` 发 MQ 消息，向量化异步执行。

**Where enforcement lives:**
- 实际触发点：`KnowledgeDocServiceImpl.java:47` — `uploadAndVectorize` 内**同步**调用 `milvusSyncService.syncToMilvus(doc.getId())`（同一事务内，L30+L47）
- `RagServiceImpl.java:22-41` — 只有 `retrieve`（关键词检索，L30-31 注释自陈 "Phase 1... Phase 2 (TODO): Vector-based retrieval"），无 `ingestDocument`
- 全仓库唯一 `sf.milvus.sync` 生产者：`MilvusReconciliationService.java:59-65`（对账任务，非上传路径；消息含 `idempotencyKey` L57）
- 常量存在：`CommonConstants.java:34`（`sf.exchange`）、`:42`（`RK_MILVUS_SYNC`）

**Paths walked:**
- ✗ MQ 生产者路径：context 模块无 AMQP 依赖（F3）→ 结构上不可能发消息
- ✓（偏差事实）同步路径可工作：上传 → 立即向量化 → 同一请求内完成或失败
- ✗ 同步调用的事务后果：`doSync` 失败时 `FailedStatusWriter`（REQUIRES_NEW）看不到外层未提交的文档行（`FailedStatusWriter.java:29-32` 走 "document not found" warn 分支）→ 外层回滚 → **文档行整体消失**，FAILED 状态无处附着（详见 REQ-19）

**Searched:** `ingestDocument|ingest`（全仓库 src/main）→ 0 命中（仅文档/注释引用）；`RabbitTemplate|convertAndSend`（schemaplexai-context）→ 0 命中；`spring-boot-starter-amqp`（context pom.xml）→ 0 命中。

**How the verdict was reached:** spec 明文规定"完成后发送 MQ 消息"的异步契约；代码结构上无此能力，改为同步调用。不是"不同机制满足同一性质"——异步解耦正是该需求的性质本身（上传延迟与向量化延迟解耦、失败经 MQ 重试），判 contradicted。**Severity: Medium**（功能经同步路径可达，但上传接口将承担全部向量化时延，且失败语义劣化，见 REQ-19）。

---

## REQ-12 — 消费者：`MilvusSyncConsumer` 监听 `sf.milvus.sync`，MANUAL ACK

> "@RabbitListener(queues = \"sf.milvus.sync\", ackMode = \"MANUAL\") public class MilvusSyncConsumer" — spec §3.4

**Verdict:** partial · confidence: high

**What this demands:** 存在绑定该路由键队列的消费者，手动确认，成功 ack / 失败 nack。

**Where enforcement lives:**
- `schemaplexai-task/.../mq/MilvusSyncConsumer.java:46-65` — `@RabbitListener(queues = "sf.milvus.sync.queue")`；成功 `channel.basicAck`（L55），失败 `basicNack(requeue=false)`（L59、L63）+ 失败日志落库（L86-92）
- 手动确认经全局配置满足：`schemaplexai-task/src/main/resources/application.yml:13-19` — `listener.simple.acknowledge-mode: manual`（+ concurrency 5-20 / prefetch 10 / default-requeue-rejected:false）——与 spec 的注解参数写法属机制等价（文档漂移注记）
- 队列/绑定：`schemaplexai-task/.../config/RabbitMqConfig.java:113-126`（队列 `sf.milvus.sync.queue` + DLX 参数，绑定 `RK_MILVUS_SYNC`）
- context 模块同名类 `context/mq/MilvusSyncConsumer.java:13-27`：无 `@RabbitListener`、无 AMQP 依赖（F3）→ 死代码

**Paths walked:**
- ✓ 消息到达 → 解析 `MilvusSyncMessage`（L52）→ 校验 `SYNC_DOC`（L67-84）
- ✗ 处理委托：`syncRequestHandler.handle` 唯一实现 `UnsupportedMilvusSyncRequestHandler.java:13-19` **恒抛异常** → 消费者永远走 nack 分支 → 消息进 DLX、写 `sf_message_fail_log`；`DeadLetterRetryService.java:41-68` 手工重试也只是重投同一死路
- ✓ 幂等：`MqIdempotencyInterceptor.java:32-64` AOP 包裹所有 `@RabbitListener`（Redis + `sf_idempotency_key` 双保险）——规格未要求，属增强

**Searched:** `@RabbitListener`（schemaplexai-context）→ 0 命中；`implements MilvusSyncRequestHandler`（全仓库）→ 仅 `UnsupportedMilvusSyncRequestHandler` 1 处。

**How the verdict was reached:** 监听 + 手动 ACK + DLQ + 幂等的"管道"齐备（非 absent），但管道里没有任何会成功的处理实现，需求"消费者执行同步"的一半不成立，故 partial 而非 implemented。**Severity: Medium**（ACK 机制本身正确；后果并入 REQ-13/REQ-14）。

---

## REQ-13 — 消费者执行步骤 1–7（查 PG → MinIO 下载 → Tika → 分块 → Embedding → 写 Milvus → PG 置 INDEXED）

> "// 1. 查询 PG 获取文档信息 // 2. 从 MinIO 下载文件 // 3. Tika 提取文本 // 4. 分块 // 5. 生成 Embedding // 6. 写入 Milvus // 7. 更新 PG 状态为 INDEXED" — spec §3.4

**Verdict:** partial · confidence: high

**What this demands:** 上述 7 步由消息消费路径完整执行，成功终态为 INDEXED。

**Where enforcement lives:** 7 步的真实实现在**同步服务** `MilvusSyncServiceImpl.doSync`（L117-145），而非消费者：
1. 查 PG — `syncToMilvus` L103-106 ✓
2. MinIO 下载 — `extractText` L191-206（✗ 租户桶往返缺陷，见 REQ-04）
3. Tika — L245-254 ✓
4. 分块 — L122 ✓
5. Embedding — L125-128（`embedBatch` 为串行循环，`EmbeddingServiceImpl.java:252-258`）✓
6. 写 Milvus — `insertChunksIntoMilvus` L256-292（单次批量 Insert，L284-291）✓
7. 终态 — 写 `SYNCED`（L134-135），**不是** `INDEXED`（该词全仓库 0 命中）✗

**Paths walked:**
- ✗ 消费路径（规格要求的路径）：`task/MilvusSyncConsumer.java:54` → `UnsupportedMilvusSyncRequestHandler` 恒抛 → 7 步一步都不会执行（F3）
- ✓ 同步替代路径：上述 1-6 步在 `milvus.enabled=true` 且 `minio.enabled=true` 时可用
- ✗ 部分写入路径：Milvus insert 成功后 `updateById`（L136）失败/进程崩溃 → 向量已入库而 PG 仍为 UPLOADED → 漂移；`doSync` 的 catch 不会回滚已写入的向量（Milvus 无事务）。补偿仅存在于 `reSyncDoc`（先删后写，L165-178），而它只被文档 update 触发（`KnowledgeDocServiceImpl.java:95`）
- ✗ 状态门槛路径：`syncToMilvus` 仅接受 `UPLOADED/PENDING/FAILED`（L109），其他状态静默 skip（L110-112）——与对账侧依赖的 `sync_status` 字段（`MilvusReconciliationMapper.java:17`）是两个字段，门槛判据不一致

**Searched:** 见 REQ-11、REQ-12 的搜索记录；`INDEXED`（context+task main）→ 0 命中。

**How the verdict was reached:** 步骤内容被实现（故非 absent），但承载主体与规格相反（同步服务而非消费者；消费路径死端），终态词不符，部分写入无补偿 → partial 并逐条列出 ✗。**Severity: High**（规格定义的异步执行平面整体不可用；同步替代平面在仓库配置下又被 F1/F2 封死）。

---

## REQ-14 — 每日 3:00 对账：比对 PG 与 Milvus 记录数

> "每日对账 | `MilvusPgReconciliationTask` 比对 PG 与 Milvus 记录数 | 每日凌晨 3:00"；关联 §5 "Milvus-PG 一致性 | 对账差异率 < 0.1%" — spec §3.5、§5

**Verdict:** partial · confidence: high

**What this demands:** 定时任务每日 3:00 运行，比对 PG 与 Milvus 的记录数差异并修复。

**Where enforcement lives:**
- `MilvusReconciliationJob.java:21-22` — `@Scheduled(cron = "0 0 3 * * ?")` ✓ 时间正确；`@SchedulerLock` 防多实例（stronger-than-spec）
- `MilvusReconciliationService.java:31-49` — 实际机制：扫 `sf_knowledge_doc` 中 `sync_status IN ('PENDING','FAILED')`（`MilvusReconciliationMapper.java:13-21`，含 `COALESCE` 兜底与 `deleted=0`）→ 逐条发 `SYNC_DOC` MQ 消息（L51-70）

**Paths walked:**
- ✓ 调度路径：3:00 触发、异常上抛不吞（L28-34，stronger）、批量上限 100（L18-19）
- ✗ "比对 PG 与 Milvus 记录数"：**从不查询 Milvus**——task 模块无 Milvus SDK（依赖清单无 `io.milvus`），无 count/统计调用；机制是"按 PG 单边状态重发同步请求"
- ✗ 修复闭环：重发的消息进入死端消费者（REQ-12/13）→ 永远失败 → `sync_status` 永不改变 → 下一日重复重发同一批文档（幂等键按 `milvus-sync-doc-{docId}` 固定，`MilvusReconciliationService.java:57`，而消费侧幂等切面在消息体级别判重——同 docId 重投会被幂等拦截直接吞掉，修复概率为 0）
- ✗ 漂移来源未覆盖：REQ-13 的"Milvus 写入成功后 PG 更新失败"场景中，`sync_status` 可能已被同事务回滚带回 `PENDING` 之前的状态或行已消失，对账侧无法感知 Milvus 侧多出的孤儿向量

**Searched:** `MilvusPgReconciliationTask`（全仓库）→ 0 命中（命名漂移，实际为 MilvusReconciliationJob）；`io.milvus`（schemaplexai-task/pom.xml）→ 0 命中；`getCollectionStats|query.*count`（task main）→ 0 命中。

**How the verdict was reached:** 调度骨架与时间契约满足（非 absent），但"比对记录数"的机制缺失、修复通道死端 → partial。**Severity: Medium**（数据一致性缺口按可达性评级：默认 `milvus.enabled=false` 下漂移不可达；启用后主要漂移窗口为 REQ-13 部分写入路径与 `updateById` 吞掉 reSync 失败的路径 `KnowledgeDocServiceImpl.java:94-98`，均为低频但真实可达，且无有效对账兜底）。

---

## REQ-15 — 元数据关联：Milvus 存 PG 主键（doc_id），支持反向查询

> "元数据关联 | Milvus 中存储 PG 主键（doc_id），支持反向查询" — spec §3.5（§4.2 步骤 3 "用 doc_id 反查 PG 获取完整元数据"）

**Verdict:** partial · confidence: high

**What this demands:** 向量行携带 PG doc_id，且存在自 Milvus 结果回查 PG 元数据的消费路径。

**Where enforcement lives:**
- 写入：`MilvusSyncServiceImpl.java:275`（`doc_id` 入行）
- 反向使用①：`deleteByDocId` 按 `doc_id` 过滤删除（L148-162）✓
- 反向使用②：`RagSearchServiceImpl.java:63` 检索输出字段含 `doc_id`，结果带回（L79）

**Paths walked:**
- ✓ doc_id 写入/删除/检索返回均存在（非 absent）
- ✗ "反查 PG 获取完整元数据"：`RagSearchServiceImpl.search` 拿到 `doc_id` 后**没有任何 `knowledgeDocMapper` 调用**（该类只注入 milvusClient/milvusProperties/embeddingService，L30-32）；返回的 `KnowledgeChunk`（entity/KnowledgeChunk.java:12-19）无 docName/元数据字段 → 规格响应中的 `docName`、`metadata` 无来源
- ✗ 该服务无任何 controller 调用方（见 REQ-16/17）

**Searched:** `knowledgeDocMapper|KnowledgeDocMapper`（RagSearchServiceImpl.java）→ 0 命中；`RagSearchService` 注入点（全仓库 src/main）→ 0 处（仅接口、实现与测试）。

**How the verdict was reached:** "存储关联"成立、"支持反向查询"只成立一半（可反查但未反查），故 partial。**Severity: Medium**（后果随 REQ-16/17：检索结果缺少规格承诺的元数据）。

---

## REQ-16 — 检索 API：`POST /context/rag/search` 请求/响应契约

> "POST /context/rag/search ... { query, topK, filters: { docType: [...] } } ... data: [{ docId, docName, content, score, metadata }]" — spec §4.1

**Verdict:** contradicted · confidence: high

**What this demands:** 该路径存在，接受 query/topK/filters，返回含 docId、docName、content、score、metadata 的对象数组。

**Where enforcement lives:**
- 实际暴露端点：`RagController.java:14` `/context/rag` + `:21-25` `POST /retrieve` → 返回 `Result<List<String>>`（"title | fileName" 拼接串，`RagServiceImpl.java:39`）
- 请求体仅 `query` + `topK`（`RagController.java:28-31`），**无 filters**
- 向量检索引擎 `RagSearchServiceImpl` 未挂任何控制器（F-搜索证据见下）
- 前端已按实际端点对接：`schemaplexai-ui/src/api/context.ts:35` 调用 `/context/rag/retrieve`

**Paths walked:**
- ✗ 路径契约：`/context/rag/search` 不存在（全仓库 0 命中）
- ✗ 响应契约：`List<String>` ≠ 规格对象数组；无 score（实际检索才有）、无 docName、无 metadata
- ✗ filters（docType）：全链路无支持——Milvus schema 无 docType 字段（json），`RagSearchServiceImpl.search` 签名无 filters 参数（`RagSearchService.java:9`），PG 侧 `docType` 列存在（SQL L109）但未接入任何检索
- ✓（偏差事实）存在一个可工作的关键词检索端点（`RagServiceImpl.java:33-40`，`selectList(null)` 全表扫描 + 内存过滤，租户隔离依赖 `TenantLineInterceptor`）

**Searched:** `rag/search|/context/rag/search`（全仓库，含 ui）→ 0 命中；`filters`（context main）→ 0 处检索语义命中；`docType`（context main）→ 仅实体/SQL 定义与关键词匹配（`RagServiceImpl.java:50-52`）。

**How the verdict was reached:** 规格逐字给出路径与报文形状，代码提供的是另一路径、另一语义（关键词 vs 向量）、另一返回类型的端点；向量引擎存在但未接线。"不同机制满足"不成立——端点契约是对外硬契约，判 contradicted。**Severity: High**（规格的核心读路径不存在；调用方若按规格集成将 404）。

---

## REQ-17 — 检索流程：query 向量化 → 带 tenant_id 的 ANN → 反查 PG → 排序返回

> "1. Embedding 服务将 query 转为向量 2. Milvus 执行 ANN 搜索（带 tenant_id 过滤） 3. 用 doc_id 反查 PG 获取完整元数据 4. 返回排序结果" — spec §4.2

**Verdict:** partial · confidence: high

**What this demands:** 四步流水线端到端成立并可被调用。

**Where enforcement lives:** `RagSearchServiceImpl.java:35-97`

**Paths walked:**
- ✓ 步骤 1：`embed(query)`，失败显式上抛不降级（L46-56）
- ✓ 步骤 2：`SearchReq` ANN 检索（L59-70），tenant 过滤表达式 `tenant_id == "..."`（L66-68），tenantId 格式白名单防注入（L28、L99-103）；一致性级别可配（默认 STRONG，`MilvusProperties.java:16`）
- ✗ 步骤 3：无 PG 反查（REQ-15）
- ✓ 步骤 4：Milvus 按距离有序返回，代码保序组装（L72-87）
- ✗ 可达性：该方法无任何调用方（控制器缺失，REQ-16）；且服务本身受 `milvus.enabled` 门控（L25），仓库配置下 Bean 不存在
- ✗ 租户边界隐患：`if (tenantId != null && !tenantId.isBlank())`（L66）——**tenantId 为空时不加过滤，返回全租户数据**；当前无调用方故不可达，但这是一段写好的可被误用的旁路（详见 REQ-18）

**Searched:** `RagSearchService`（全仓库 src/main 注入点）→ 0 命中。

**How the verdict was reached:** 四步中两步半成立且质量较高（防注入、失败不降级），但步骤 3 缺失、整体未暴露 → partial。**Severity: High**（检索能力对任何调用方不可达）。

---

## REQ-18 — 租户数据边界（贯穿：PG 行级 / MinIO 桶级 / Milvus 分区与过滤）

> "tenant_id（租户隔离）" / "Partition Key: tenant_id" / "Milvus 执行 ANN 搜索（带 tenant_id 过滤）" — spec §3.1、§3.3、§4.2

**Verdict:** partial · confidence: high

**What this demands:** 任一读取路径都不得跨越租户边界返回他人数据；写入必须携带租户键。

**Where enforcement lives:**
- PG：`TenantLineInterceptor.java:14-20`（MyBatis-Plus 全局 SQL 租户过滤；`sf_knowledge_doc` 不在 ignoreTable 名单，L28-33）
- MinIO：bucket-per-tenant `sf-files-{tenantId}`（`MinioFileStorageService.java:94-99`；同步下载侧同规则 `MilvusSyncServiceImpl.java:238-243`）
- Milvus：partition key（json L31-34）+ 检索过滤（`RagSearchServiceImpl.java:66-68`）+ 写入携带（`MilvusSyncServiceImpl.java:279`）
- 上传侧：强制租户非空（`FileUploadController.java:35-38`；`KnowledgeDocServiceImpl.java:38-41`）

**Paths walked:**
- ✓ PG 读：F2 下上下文为空 → `getTenantId()` 返回 `NullValue`（L16-18）→ SQL 条件 `tenant_id = NULL` 恒假 → 查询空结果（fail-closed，无泄漏）
- ✓ 上传/建档：租户缺失直接 400（fail-closed）
- ✗ **Milvus 检索旁路**：`RagSearchServiceImpl.java:66-68` tenantId 空则无过滤 → 跨租户返回。当前无调用方（不可达），但代码分支已写好；一旦未来接线时误传空值即为跨租户读
- ✗ 写入兜底值：`MilvusSyncServiceImpl.java:258` tenantId null 时写 `"default"` 分区——当前流程因上传侧 400 拦截而不可达，但属危险兜底
- ⚠ 前提性缺口：context 服务无租户注入器（F2）→ 当前形态是"全功能不可用"而非"边界失守"；边界机制的正确性依赖于尚不存在的接线

**Searched:** `setTenantId`（context src/main）→ 0 命中；`Filter|Interceptor`（common/model main）→ 0 文件命中。

**How the verdict was reached:** 三层边界机制都已编码（非 absent）；当前无可利用的跨租户读路径（全部失败模式均为 fail-closed），故不判 contradicted；但存在一处**潜在**旁路分支（检索空租户）与整体接线缺失，故 partial。**Severity: Critical（潜在）**——按"租户数据边界缺口为 Critical"的规则评级：`RagSearchServiceImpl.java:66-68` 是成文的旁路分支，当前不可达（无调用方）且其余路径 fail-closed，故标注为潜在；一旦该服务被接线且调用方传空租户即升级为活跃 Critical。

---

## REQ-19 — 失败语义：FAILED 可见 + 可恢复（重试/对账兜底）

> "status ... FAILED"（§3.1）+ 消费者 MANUAL ACK 暗含失败不丢（§3.4）+ 对账兜底（§3.5）；委托方点名的"失败重试"维度

**Verdict:** partial · confidence: high

**What this demands:** 同步失败必须留下 FAILED 终态可见，且存在使其恢复的通道。

**Where enforcement lives:**
- `FailedStatusWriter.java:24-38` — REQUIRES_NEW 独立事务写 `status=FAILED` + `sync_status=FAILED`（stronger-than-spec 的事务设计）
- `MilvusSyncServiceImpl.java:139-144` — catch 内先记 FAILED 再重抛
- 重试通道：DLX（`RabbitMqConfig.java:116-117`）→ `sf_message_fail_log`（`MilvusSyncConsumer(task).java:86-92`）→ `DeadLetterRetryService.java:41-68` 手工重投；每日对账重投（REQ-14）

**Paths walked:**
- ✗ 默认（同步）路径的 FAILED 可见性：`uploadAndVectorize`（`KnowledgeDocServiceImpl.java:30-48`）事务内同步调用 → 失败时 `FailedStatusWriter` 的新事务读不到未提交的行（`FailedStatusWriter.java:29-32` → "document not found"）→ 外层回滚 → **行消失，FAILED 无处可见**；用户只见 500，文档"蒸发"
- ✓ 已提交文档的再同步失败：`updateById → reSyncDoc` 失败时 `FailedStatusWriter` 能读到已提交行 → FAILED 落库 ✓；但该异常被 `KnowledgeDocServiceImpl.java:94-98` catch 后仅记日志（调用方收到成功）
- ✗ 恢复通道：FAILED 文档的恢复依赖（a）对账重投 → 死端消费者（REQ-12/13）；（b）DLQ 手工重试 → 同一死端；（c）`syncToMilvus` 接受 FAILED 状态重入（L109）但无任何调用方会对已存在文档触发它。**FAILED 文档实际不可恢复**
- ✓ 删除路径容错：Milvus 删除失败不阻塞 PG 删除（`KnowledgeDocServiceImpl.java:74-78`）

**Searched:** 依赖 REQ-12/14 的搜索；`reSync|syncToMilvus` 调用点（context main）→ 仅 `uploadAndVectorize`(L47)、`updateById`(L95)、`reSyncDoc` 内部。

**How the verdict was reached:** FAILED 写入机制存在且事务设计讲究（非 absent），但在主入口路径上不可见、且三条恢复通道全为死路 → partial。**Severity: Medium**（fail-closed 不泄漏；代价是失败即数据"蒸发"或永久 FAILED）。

---

## REQ-20 — 非功能指标（30s 可检索 / P99<100ms / Top5>85% / 差异率<0.1%）

> "文档上传 → 可检索延迟 < 30 秒 / 向量检索延迟 P99 < 100ms / 检索准确率（Top5）> 85% / Milvus-PG 一致性 对账差异率 < 0.1%" — spec §5

**Verdict:** undecidable · confidence: high（对"不可判"本身）

**What this demands:** 运行时性能/质量达标。

**Where enforcement lives:** 无运行时测量基础设施可引用（无基准测试、无评估集、无指标断言）。

**Paths walked:**
- ✗ "上传→可检索 < 30s"：同步路径下理论可达（单请求内完成），但 F1/F2 下端到端不可运行，无法测量
- ✗ "P99 < 100ms"：静态反证线索——默认一致性级别 STRONG（`MilvusProperties.java:16`）+ `embedBatch` 串行 HTTP（`EmbeddingServiceImpl.java:252-258`）使写入侧不可能快；检索侧单查询单向量，理论上可满足，但无数据
- ✗ "差异率 < 0.1%"：对账不比数、不修复（REQ-14）→ 无机制保障该指标，但当前也无实测漂移数据

**How the verdict was reached:** 静态核查只能给出风险线索，达标与否需要运行时测量，故 undecidable（列入 notChecked 语义）。**Severity: —（待运行时验证）**。

---

## 反向差距（代码中存在、规格未提及的重要行为）

1. **病毒扫描门禁**：`FileUploadController.java:48-53` ClamAV 扫描，扫描器不健康即 503 拒绝上传（fail-closed）；`ClamAvFileScanService` / `NoOpFileScanService` 双实现。
2. **文件大小限制**：单文件 50MB 硬限（`FileUploadController.java:29、44-46`）；**1GB 租户配额常量已声明但从未使用**（L30 `MAX_TOTAL_SIZE_PER_TENANT`，grep 全模块仅此 1 处 → 死代码）。无文件类型白名单。
3. **上传与建档分离**：`/context/files/upload` 与 `POST /context/knowledge-docs` 两步式，靠客户端传 `fileUrl` 衔接（规格画的是单条流水线）。
4. **双状态字段**：`status` + `sync_status` 并行（SQL L108、L110），消费门槛用前者（`MilvusSyncServiceImpl.java:109`）、对账用后者（`MilvusReconciliationMapper.java:17`），判据不一致。
5. **MQ 幂等切面**：`MqIdempotencyInterceptor.java:32-64` Redis + DB 双重判重，覆盖所有 @RabbitListener。
6. **DLQ + 失败日志 + 手工重试台**：`sf_message_fail_log`、`DeadLetterHandler`、`DeadLetterRetryService`。
7. **对账加固**：ShedLock 分布式锁 + 批量上限 100（`MilvusReconciliationJob.java:18-22`）。
8. **降级与守卫**：NoOp/Disabled 双降级实现；MinIO 凭证缺失启动即失败（`MilvusSyncServiceImpl.java:73-84`）；prod profile 禁用 mock embedding（`EmbeddingServiceImpl.java:104-115`）。
9. **更新即重向量化**：文档 update 触发 delete+re-insert 全量重建（`KnowledgeDocServiceImpl.java:92-99`），失败被吞；删除文档联动清理向量（L72-80）。
10. **Milvus schema 加宽**：`chunk_index`、`created_at` 额外字段；content 上限 65535。
11. **关键词检索过渡桩**：`RagServiceImpl.java:30-31` 自陈 Phase 1 关键词、Phase 2 向量 TODO；前端已对接该桩（`schemaplexai-ui/src/api/context.ts:35`）。
12. **内部类型不一致**：`SfKnowledgeDocVersion.version` 实体 String（L14）vs SQL INT（L122）。

## Open questions

1. **规格状态元数据**：文件 front matter `status: 草稿`，委托描述为"已批准"——以哪份为准？本报告按委托口径对"已批准"规格核查。
2. **`resolveObjectName` 是否为缺陷**：租户桶（`sf-files-{tenantId}`）下对象名会携带桶前缀（`MilvusSyncServiceImpl.java:216-232` 只剥 `documents/`），与 `MinioFileStorageService.java:78` 产出的 URL 往返不通；测试以无差别 mock 掩盖（`MilvusSyncServiceImplTest.java:255-266`）。是设计未定还是实现缺陷，需与作者确认。
3. **租户 ID 类型链**：PG `BIGINT` / 实体 `String` / Milvus `VarChar` 三层不一致，是有意的多态租户键还是漂移？
4. **集合已存在时不校验索引**：`MilvusCollectionInitializer.java:43-46` 存在即跳过，若既有集合缺索引不会补建——既有环境的索引状态未知。
5. **`sf_chat_message` 等其他域**与本规格无关，未纳入。

## notChecked（受手段限制未核查）

- REQ-20 四项指标均需运行时测量/评估数据集，静态核查不可判（见上文）。
- 检索准确率评估：仓库无评估语料与测试集。
- 实际部署形态（环境变量覆盖后的 `milvus.enabled` / `embedding.provider` 取值）未知，本报告按仓库自带配置形态裁决。

## 反驳复核

独立复核：对全部 severity ∈ {Critical, High} 且 verdict ∈ {contradicted, partial} 的 5 条（01/13/16/17/18）逐条尝试推翻。代码方向另查：`RagSearchService` 全仓库注入点与 `getBean`/反射旁路、全部 `@RabbitListener` 清单、`MilvusSyncRequestHandler` 全部实现、context 服务租户注入的可能来源（`schemaplexai-common` 的 4 个 AutoConfiguration 类、`TenantIdSpanProcessor`、web 模块 `TenantContextInterceptor` 是否可达）、网关是否存在路径改写、context resources 是否存在其他 profile 配置；规格方向确认 frontmatter `status: 草稿` 属实，contradicted 类裁定据此按"对草稿规格的违背"表述，证据不变。

| REQ | 原判 | 裁定 | 理由（一行） | 证据 |
|---|---|---|---|---|
| 01 | partial / High | 维持 | 推翻尝试失败：context 服务内无任何 `TenantContextHolder.setTenantId` 来源（common 自动配置仅异常处理/JWT 校验/遥测，`TenantIdSpanProcessor` 只读不写；context pom 不依赖含 `TenantContextInterceptor` 的 web 模块），resources 仅一份 application.yml 且无 `minio.*` 键 → 400/500 双门控成立 | `FileUploadController.java:35-38`；`TenantContextHolder.java:4-12` + 全仓库 setTenantId 清单（context 0 处）；`AutoConfiguration.imports`（4 类，无 Filter/Interceptor）；`TenantIdSpanProcessor.java:13-18`；`application.yml` 全文 42 行；`DisabledFileStorageService.java:15-21` |
| 13 | partial / High | 维持 | 消费者真实实现不在别处：全仓库唯一 `sf.milvus.sync.queue` 监听在 task 模块，其唯一 `MilvusSyncRequestHandler` 实现恒抛异常；context 无 AMQP 依赖、同名类无注解且零调用方；7 步仅存在于同步路径且终态写 `SYNCED` 非 `INDEXED` | `task/mq/MilvusSyncConsumer.java:46-65`；`UnsupportedMilvusSyncRequestHandler.java:13-19`（全仓库唯一实现）；`schemaplexai-context/pom.xml:7-24`（无 amqp）；`context/mq/MilvusSyncConsumer.java:13-27`；`MilvusSyncServiceImpl.java:109,134-135`；全仓库 `@RabbitListener` 枚举 |
| 16 | contradicted / High | 维持 | `/context/rag/search` 全仓库（含前端 ts/yml/json）零命中，网关 `/context/**` 纯路由无改写，web 模块无 rag/knowledge 代理；实际为 `/retrieve` + `List<String>`、无 filters——裁定按"对草稿规格（status: 草稿）的违背"表述，证据不变 | `RagController.java:14,21-31`；`rag/search` 全仓库检索 0 命中；`GatewayConfig.java:24-25`（无改写过滤器）；`schemaplexai-web` src/main 无 `rag\|knowledge` 命中；spec frontmatter L5 `status: 草稿` |
| 17 | partial / High | 维持 | "检索不可达"经受住推翻尝试：`RagSearchService` 在全仓库 src/main 注入点为 0（仅接口/实现/测试/文档），无 getBean 或反射旁路，Bean 受 `milvus.enabled` 门控而仓库配置无此键；步骤 3 反查确缺失（仅注入 3 依赖，无 Mapper） | `RagSearchServiceImpl.java:25,30-32,35-97`；`RagController.java:19`（只注入 `RagService`）；全仓库 `RagSearchService` 命中清单（main 中仅接口与实现）；`MilvusProperties.java:16` |
| 18 | partial / Critical（潜在） | 维持 | 旁路分支（空租户→无过滤→跨租户返回）真实存在，但"当前无调用方"经全仓库注入点检索成立，故维持"潜在"而非活跃；其余读路径均 fail-closed（PG 空租户 → `tenant_id = NULL` 恒假） | `RagSearchServiceImpl.java:66-68,99-103`；`TenantLineInterceptor.java:14-20`（NullValue 分支）；`TenantContextHolder.java:4-12`（context 服务无写入方）；`MilvusSyncServiceImpl.java:258`（"default" 兜底仅写入侧，上传端 400 拦截使其不可达） |
