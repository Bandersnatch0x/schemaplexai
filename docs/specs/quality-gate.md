---
topic: quality-gate
stage: spec
version: v1.0
status: 草稿
supersedes: ""
---

# 质量门禁技术规格

> **主题**: `quality-gate`
> **阶段**: `spec`
> **版本**: v1.0
> **状态**: 草稿
> **日期**: 2026-04-30
> **范围**: `schemaplexai-quality` 服务

---

## 1. 概述

质量门禁（Quality Gate）在 Agent 执行的关键节点自动检查输出质量，确保 AI 生成内容符合规范、安全、可靠。

**检查时机**:
- Agent 执行完成后（后置检查）
- 工具调用结果返回后（中间检查）
- 工作流节点完成后（节点级检查）

**处理策略**:
- `PASS`: 继续执行
- `WARN`: 记录告警，继续执行
- `BLOCK`: 暂停执行，等待人工确认
- `FAIL`: 终止执行，标记失败

## 2. 架构视图

```
┌─────────────────────────────────────────────┐
│           QualityOrchestrator                │
│  ┌───────────────────────────────────────┐  │
│  │ 规则注册表 (Map<String, QualityRule>)  │  │
│  └───────────────────────────────────────┘  │
└──────────────┬──────────────────────────────┘
               │
    ┌──────────┼──────────┬──────────┬──────────┐
    │          │          │          │          │
    ▼          ▼          ▼          ▼          ▼
┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐
│ Spec   │ │Security│ │Ground- │ │Output  │ │Artifact│
│Compliance│ │ Scan   │ │  ing   │ │Format  │ │ Check  │
└────────┘ └────────┘ └────────┘ └────────┘ └────────┘
```

## 3. 规则类型规格

### 3.1 规则接口

```java
public interface QualityRule {
    String getRuleName();
    QualityCheckResult check(QualityContext context);
}

public class QualityContext {
    private String agentOutput;      // Agent 输出内容
    private String specContent;      // 关联 Spec 内容
    private Map<String, Object> metadata;  // 执行元数据
}
```

### 3.2 Spec 合规规则

**规则名**: `SPEC_COMPLIANCE`

**职责**: 检查 Agent 输出是否满足关联 Spec 的要求

**实现策略**（分级）:

| 级别 | 策略 | 说明 |
|------|------|------|
| L1 | 关键词匹配 | 检查输出是否包含 Spec 中的关键要求词 |
| L2 | 结构化匹配 | 检查输出格式是否符合 Spec 规定的模板 |
| L3 | LLM 评估 | 调用 LLM 判断输出是否满足 Spec（成本高，精度高） |

**评分标准**:

```java
if (complianceScore >= 0.8) return PASS;
if (complianceScore >= 0.5) return WARN;
return BLOCK;
```

### 3.3 安全扫描规则

**规则名**: `SECURITY_SCAN`

**职责**: 检测输出中的安全隐患

**检测项**:

| 检测项 | 模式 | 严重等级 |
|--------|------|----------|
| 硬编码密码 | `password\s*=\s*["'][^"']+["']` | BLOCK |
| 密钥泄露 | `api[_-]?key\s*=\s*["'][^"']+["']` | BLOCK |
| 敏感信息 | 手机号、身份证号、银行卡号正则 | BLOCK |
| SQL 注入痕迹 | `'; DROP\s+TABLE` 等 | FAIL |
| XSS 载荷 | `<script>`、事件处理器 | FAIL |
| PII 泄露 | 邮箱、地址模式 | WARN |

### 3.4 Grounding 验证规则

**规则名**: `GROUNDING_CHECK`

**职责**: 验证输出中的事实性声明是否基于提供的知识库

**实现策略**:

1. 从输出中提取事实性声明（使用 LLM 或 NER）
2. 在 RAG 知识库中检索相关文档
3. 对比声明与知识库内容的一致性
4. 标记无法验证的声明

### 3.5 输出格式规则

**规则名**: `OUTPUT_FORMAT`

**职责**: 验证输出格式是否符合预期

**检测项**:

| 检测项 | 说明 |
|--------|------|
| JSON 有效性 | 如果要求 JSON 输出，验证是否可解析 |
| Schema 合规 | 验证 JSON 是否符合指定 Schema |
| 长度限制 | 输出是否超过最大长度 |
| 编码检查 | 是否包含非法字符 |

### 3.6 制品检查规则

**规则名**: `ARTIFACT_CHECK`

**职责**: 验证生成的代码/文档/配置文件的质量

**检测项**:

| 检测项 | 说明 |
|--------|------|
| 语法检查 | 代码是否可编译/可解析 |
| 依赖检查 | 引用的包/模块是否存在 |
| 规范检查 | 是否符合项目编码规范 |

## 4. 门禁配置

### 4.1 质量门禁配置表

**sf_quality_gate**:

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| tenant_id | BIGINT | 租户隔离 |
| name | VARCHAR | 门禁名称 |
| rule_name | VARCHAR | 规则名 |
| severity | VARCHAR | PASS / WARN / BLOCK / FAIL |
| enabled | BOOLEAN | 是否启用 |
| config | JSONB | 规则特定配置 |

### 4.2 默认门禁策略

```yaml
默认策略:
  - rule: SECURITY_SCAN
    severity: BLOCK
    enabled: true
  - rule: SPEC_COMPLIANCE
    severity: WARN
    enabled: true
  - rule: OUTPUT_FORMAT
    severity: WARN
    enabled: true
```

## 5. 质量报告

**QualityReport**:

```java
public class QualityReport {
    private Long executionId;
    private boolean allPassed;
    private List<QualityCheckResult> results;
    private LocalDateTime checkedAt;
}
```

**报告内容**:

| 字段 | 说明 |
|------|------|
| executionId | 关联执行 ID |
| ruleName | 规则名称 |
| passed | 是否通过 |
| severity | 严重等级 |
| message | 详细说明 |
| suggestion | 改进建议 |

## 6. 与 Agent 执行引擎集成

```
Agent 执行流程:
  THINKING → TOOL_CALLING → OBSERVATION
                                     ↓
                              ┌─────────────┐
                              │ QualityGate │
                              │   Check     │
                              └──────┬──────┘
                                     │
                    ┌────────────────┼────────────────┐
                    │                │                │
                    ▼                ▼                ▼
                 PASS            WARN/BLOCK        FAIL
                    │                │                │
                    ▼                ▼                ▼
              继续执行        记录问题/告警      终止执行
                              人工确认后继续
```

## 7. API 接口

```http
GET    /quality/gates              # 获取门禁配置列表
POST   /quality/gates              # 创建门禁规则
PUT    /quality/gates/{id}         # 更新门禁规则
DELETE /quality/gates/{id}         # 删除门禁规则
POST   /quality/check              # 手动触发质量检查
GET    /quality/reports/{executionId}  # 获取质量报告
```

## 8. 非功能需求

| 指标 | 目标 |
|------|------|
| 检查延迟 | P99 < 500ms（不含 LLM 评估） |
| 并发检查 | 支持 50+ 并发 |
| 误报率 | < 5% |
| 漏报率 | < 1% |

## 9. 相关文档

- `docs/plans/project-plan.md`（Phase 5）
- `docs/plans/unified-dev-plan.md`（Task 39）
- `docs/specs/agent-execution-engine.md`
