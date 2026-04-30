---
topic: rag-pipeline
stage: spec
version: v1.0
status: 草稿
supersedes: ""
---

# RAG Pipeline 技术规格

> **主题**: `rag-pipeline`
> **阶段**: `spec`
> **版本**: v1.0
> **状态**: 草稿
> **日期**: 2026-04-30
> **范围**: `schemaplexai-context` 服务中的知识文档管理与检索

---

## 1. 概述

RAG（Retrieval-Augmented Generation）Pipeline 负责：

- 知识文档上传与存储（MinIO）
- 文本提取（Apache Tika）
- 文本分块与 Embedding 生成
- 向量存储（Milvus）
- 带元数据过滤的相似度检索
- PG→Milvus 同步一致性保障

## 2. 数据流

```
用户上传文档
    │
    ▼
┌──────────────┐
│  MinIO       │────┐
│  (原始文件)   │    │
└──────────────┘    │
    │               │
    ▼               │
┌──────────────┐    │
│ Apache Tika  │    │
│ (文本提取)   │    │
└──────────────┘    │
    │               │
    ▼               │
┌──────────────┐    │
│ 文本分块      │    │
│ (512 tokens  │    │
│  overlap 50) │    │
└──────────────┘    │
    │               │
    ▼               │
┌──────────────┐    │
│ Embedding    │    │
│  Service     │    │
└──────────────┘    │
    │               │
    ▼               ▼
┌──────────────────────────┐
│        PostgreSQL        │
│  sf_knowledge_doc        │
│  sf_knowledge_doc_version│
└──────────────────────────┘
    │
    │ 异步: sf.milvus.sync 队列
    ▼
┌──────────────────────────┐
│        Milvus 2.3        │
│   Collection: knowledge  │
│   _chunks                │
└──────────────────────────┘
```

## 3. 核心组件

### 3.1 文档模型

**sf_knowledge_doc**:

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| tenant_id | BIGINT | 租户隔离 |
| name | VARCHAR | 文件名 |
| object_path | VARCHAR | MinIO 对象路径 |
| chunk_count | INT | 分块数量 |
| status | VARCHAR | PENDING / PROCESSING / INDEXED / FAILED |
| version_id | BIGINT | 当前版本 |

**sf_knowledge_doc_version**:

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| doc_id | BIGINT | 关联文档 |
| version | INT | 版本号 |
| content_hash | VARCHAR | 内容哈希 |
| chunk_count | INT | 分块数 |
| created_at | TIMESTAMP | 创建时间 |

### 3.2 分块策略

```java
public class TextChunker {
    private static final int CHUNK_SIZE = 512;     // tokens
    private static final int CHUNK_OVERLAP = 50;   // tokens

    public List<String> chunk(String text) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + CHUNK_SIZE, text.length());
            chunks.add(text.substring(start, end));
            start += CHUNK_SIZE - CHUNK_OVERLAP;
        }
        return chunks;
    }
}
```

### 3.3 Milvus Collection 设计

```python
Collection: knowledge_chunks

Fields:
  - id: INT64, primary_key, auto_id
  - content: VARCHAR(4096)
  - doc_id: INT64
  - tenant_id: INT64
  - embedding: FLOAT_VECTOR(1536)  # OpenAI text-embedding-3-small
  - version: INT

Index:
  - embedding: IVF_FLAT, nlist=128, metric=COSINE

Partition Key: tenant_id
```

### 3.4 PG→Milvus 同步

**生产者**: `RagServiceImpl.ingestDocument()` 完成后发送 MQ 消息
**消费者**: `MilvusSyncConsumer`

```java
@RabbitListener(queues = "sf.milvus.sync", ackMode = "MANUAL")
public class MilvusSyncConsumer {
    // 1. 查询 PG 获取文档信息
    // 2. 从 MinIO 下载文件
    // 3. Tika 提取文本
    // 4. 分块
    // 5. 生成 Embedding
    // 6. 写入 Milvus
    // 7. 更新 PG 状态为 INDEXED
}
```

### 3.5 一致性保障

| 机制 | 说明 | 频率 |
|------|------|------|
| MQ 同步 | 文档变更后异步同步 | 实时 |
| 每日对账 | `MilvusPgReconciliationTask` 比对 PG 与 Milvus 记录数 | 每日凌晨 3:00 |
| 元数据关联 | Milvus 中存储 PG 主键（doc_id），支持反向查询 | — |

## 4. 检索 API

### 4.1 向量检索

```http
POST /context/rag/search
Content-Type: application/json

{
  "query": "如何优化 Spring Boot 启动速度？",
  "topK": 5,
  "filters": {
    "docType": ["technical_doc", "best_practice"]
  }
}

Response:
{
  "code": 200,
  "data": [
    {
      "docId": 101,
      "docName": "Spring Boot 性能优化指南",
      "content": "...",
      "score": 0.92,
      "metadata": { "docType": "technical_doc" }
    }
  ]
}
```

### 4.2 检索流程

1. Embedding 服务将 query 转为向量
2. Milvus 执行 ANN 搜索（带 tenant_id 过滤）
3. 用 doc_id 反查 PG 获取完整元数据
4. 返回排序结果

## 5. 非功能需求

| 指标 | 目标 |
|------|------|
| 文档上传 → 可检索延迟 | < 30 秒 |
| 向量检索延迟 | P99 < 100ms |
| 检索准确率（Top5） | > 85% |
| Milvus-PG 一致性 | 对账差异率 < 0.1% |

## 6. 相关文档

- `docs/decisions/ADR-004-database-middleware-selection.md`
- `docs/plans/project-plan.md`（Phase 3）
- `docs/plans/unified-dev-plan.md`（Tasks 35-36）
