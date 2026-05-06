<!-- AUTO-GENERATED: sync-wiki.sh at 2026-05-01T18:07:18Z -->

---
title: Active Development Areas
type: index
source: auto-generated
creation_date: 2026-05-02
update_date: 2026-05-06
tags: [active, development]
confidence: high
---

# Active Development Areas

> Auto-generated from docs/ status=进行中/评审中 entries.

## Active Specs


## Active Plans


## Active Changes (.claude/changes/)

- **project-gaps-completion**: 已完成 — ClickHouse schema, Workflow Node Executors (7), Milvus collection + RAG pipeline, DocumentChunker + EmbeddingService
- **v1-final-delivery**: 已完成 — 覆盖率基线、集成测试、前端测试、部署指南全部交付
- **agent-engine-core-completion**: 已交付 (Judge 4.00/5.0)，等待归档至 docs/archive/
- **v1-release-readiness**: 已交付，527/527 测试通过，等待归档
- **v1-test-fixes-and-coverage**: 已交付，所有 21 个预存测试失败修复，等待归档
- **core-ai-engine-design**: 已完成 — L2持久化、SSE事件流、getLatestSnapshot实现
  - **Known Limitation**: `ExecutionEventBus` SSE emitters are stored in a local in-memory `ConcurrentHashMap`. This is single-node only; horizontal scaling requires sticky sessions or a Redis pub/sub bridge before production multi-node deployment.
- **wiki-gaps-completion**: 已完成 — 6个wiki服务页面已创建

## v1.0 Release Status

- **Tests**: 527/527 passing (100%) + 16 new tests (DocumentChunker, EmbeddingService, NodeExecutorRegistry)
- **Build**: All 17 modules compile
- **CI/CD**: GitHub Actions + Jenkins configured
- **API Docs**: Knife4j + docs/API.md
- **JaCoCo**: Plugin 0.8.12 added (待运行 mvn verify 生成基线)
- **Pending**: JaCoCo coverage baseline generation
