---
topic: spec-management
stage: spec
version: v1.0
status: 草稿
supersedes: ""
---

# Spec 管理技术规格

> **主题**: `spec-management`
> **阶段**: `spec`
> **版本**: v1.0
> **状态**: 草稿
> **日期**: 2026-04-30
> **范围**: `schemaplexai-spec` 服务

---

## 1. 概述

Spec 管理模块负责：

- **Spec 文档**: 结构化需求文档的创建、编辑、版本管理
- **Steering 文档**: AI 行为约束文档（Spec 子类型）
- **模板管理**: 可复用的 Spec 模板
- **版本控制**: 发布、回滚、对比
- **评审工作流**: 审批链、评审意见、变更追踪

## 2. 文档类型

| 类型 | 说明 | 用途 |
|------|------|------|
| `SPEC` | 产品需求规格 | 定义功能需求 |
| `STEERING` | AI 行为约束 | 约束 Agent 行为、输出格式、安全规则 |
| `TEMPLATE` | 文档模板 | 快速创建标准化文档 |

## 3. 版本管理

### 3.1 版本生命周期

```
DRAFT → IN_REVIEW → APPROVED → PUBLISHED → DEPRECATED
```

| 状态 | 说明 | 可编辑 |
|------|------|--------|
| `DRAFT` | 草稿 | 是 |
| `IN_REVIEW` | 评审中 | 否 |
| `APPROVED` | 已批准 | 否 |
| `PUBLISHED` | 已发布 | 否 |
| `DEPRECATED` | 已废弃 | 否 |

### 3.2 版本操作

| 操作 | 说明 | 权限 |
|------|------|------|
| `createVersion` | 基于当前版本创建新版本 | 编辑者 |
| `publish` | 发布版本，更新 Spec 当前版本 | 审批者 |
| `rollback` | 回滚到指定版本 | 管理员 |
| `diff` | 对比两个版本差异 | 读者 |

### 3.3 Diff 算法

**已实现**: `SpecDiffUtil.java` 使用 LCS（最长公共子序列）算法

**Diff 结果**:

```java
public class SpecDiffResult {
    private Long specId;
    private Long versionA;
    private Long versionB;
    private List<DiffHunk> hunks;  // 差异块
}

public class DiffHunk {
    private int oldStart;
    private int oldLength;
    private int newStart;
    private int newLength;
    private List<DiffLine> lines;
}
```

## 4. 评审工作流

### 4.1 评审流程

```
提交评审
    ↓
┌─────────────────────────────────────────┐
│           评审人分配                     │
│  (可按角色/指定人员/轮询分配)             │
└─────────────────────┬───────────────────┘
                      ↓
              ┌───────────────┐
              │  评审人审核    │
              └───────┬───────┘
                      │
        ┌─────────────┼─────────────┐
        ▼             ▼             ▼
    APPROVED    CHANGES_REQUESTED   REJECTED
        │             │               │
        ▼             ▼               ▼
    通过发布      返回修改          结束流程
```

### 4.2 评审意见

**sf_spec_review**:

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| spec_id | BIGINT | 关联 Spec |
| version_id | BIGINT | 关联版本 |
| reviewer_id | BIGINT | 评审人 |
| decision | VARCHAR | APPROVED / REJECTED / CHANGES_REQUESTED |
| comments | TEXT | 评审意见 |
| line_numbers | VARCHAR | 关联行号（如 "10-15,20"） |
| status | VARCHAR | 待处理 / 已解决 |

### 4.3 变更追踪

**sf_spec_change**:

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| spec_id | BIGINT | 关联 Spec |
| version_id | BIGINT | 关联版本 |
| change_type | VARCHAR | ADD / MODIFY / DELETE |
| field_name | VARCHAR | 变更字段 |
| old_value | TEXT | 旧值 |
| new_value | TEXT | 新值 |
| changed_by | BIGINT | 变更人 |
| changed_at | TIMESTAMP | 变更时间 |

## 5. Steering 文档

**Steering** 是 Spec 的子类型，专门用于约束 Agent 行为。

**内容结构**:

```markdown
# Steering: 代码审查助手

## 角色定义
你是一个专业的代码审查助手...

## 输出格式
- 必须以 JSON 格式输出
- 包含 severity、file、line、message 字段

## 安全约束
- 禁止建议不安全的函数调用
- 禁止泄露敏感信息

## 示例
输入: ...
输出: ...
```

**与 Agent 绑定**:

```java
// Agent 配置时选择关联的 Steering 文档
SfAgentConfig config = new SfAgentConfig();
config.setSteeringId(steeringId);
// 执行时，Steering 内容作为 System Prompt 的一部分注入
```

## 6. API 接口

### 6.1 Spec 管理

```http
POST   /spec/specs                  # 创建 Spec
GET    /spec/specs                  # 列表
GET    /spec/specs/{id}             # 详情
PUT    /spec/specs/{id}             # 更新
DELETE /spec/specs/{id}             # 删除
POST   /spec/specs/{id}/publish     # 发布
POST   /spec/specs/{id}/rollback    # 回滚
```

### 6.2 版本管理

```http
POST   /spec/versions               # 创建新版本
GET    /spec/versions?specId={id}   # 版本列表
GET    /spec/versions/{id}          # 版本详情
GET    /spec/versions/diff?a={id}&b={id}  # 版本对比
```

### 6.3 评审

```http
POST   /spec/reviews                # 提交评审
GET    /spec/reviews?specId={id}    # 评审列表
PUT    /spec/reviews/{id}/resolve   # 解决评审意见
```

### 6.4 Steering

```http
POST   /spec/steerings              # 创建 Steering
GET    /spec/steerings              # 列表
PUT    /spec/steerings/{id}         # 更新
```

## 7. 非功能需求

| 指标 | 目标 |
|------|------|
| Diff 计算延迟 | < 1s（文档 < 10MB） |
| 版本列表查询 | P99 < 200ms |
| 并发编辑 | 乐观锁（version 字段） |

## 8. 相关文档

- `docs/plans/project-plan.md`（Phase 4）
- `docs/plans/unified-dev-plan.md`（Task 37）
