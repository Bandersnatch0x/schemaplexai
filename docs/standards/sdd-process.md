# SDD 流程规范（Specification-Driven Development）

> 所有代码实现必须以 Spec 为契约。无 Spec，不编码。

---

## 1. SDD 阶段定义

```
需求(requirements) → 规格(specs) → 设计(designs) → 计划(plans) → 编码 → 评审
```

| 阶段 | 产出 | 评审人 | 门禁 |
|------|------|--------|------|
| **需求** | PRD | PM + 架构师 | 需求明确、边界清晰 |
| **规格** | Spec | 架构师 + 开发 | 接口契约、数据模型、异常场景覆盖 |
| **设计** | Design | 架构师 + TL | 模块边界、依赖关系、性能/安全评估 |
| **计划** | Plan | TL + 开发 | 任务分解 <= 3 天/任务、有验收标准 |
| **编码** | Code + Test | 开发 | TDD 通过、CI 通过 |
| **评审** | Review | 至少 1 人 | Code Review + Spec 一致性检查 |

---

## 2. 各阶段文档规范

### 2.1 需求（Requirements）

- **位置**：`docs/requirements/`
- **命名**：`YYYY-MM-DD-<feature>-prd.md`
- **必填内容**：
  - 用户故事（As a ... I want ... So that ...）
  - 验收标准（Given/When/Then）
  - 非功能需求（性能、安全、兼容性）

### 2.2 规格（Specs）

- **位置**：`docs/specs/`
- **命名**：`YYYY-MM-DD-<topic>-spec.md`
- **必填内容**：
  - 接口定义（URL / Method / Request / Response / Error Code）
  - 数据模型（Entity / DTO / VO，含字段类型/约束）
  - 状态机（如有）
  - 异常场景清单

### 2.3 设计（Designs）

- **位置**：`docs/designs/`
- **命名**：`YYYY-MM-DD[-vX.Y]-<topic>-design.md`
- **必填内容**：
  - 架构图（C4 Model 或等效）
  - 模块边界与接口
  - 数据流图
  - 部署/运维考虑

### 2.4 计划（Plans）

- **位置**：`docs/plans/`
- **命名**：`YYYY-MM-DD-<topic>-plan.md`
- **必填内容**：
  - 任务分解（Task 级别）
  - 工期估算（理想人天 + 缓冲）
  - 依赖关系与阻塞项
  - 验收标准（每个 Task）

---

## 3. 触发 SDD 的条件

**必须走完整 SDD 流程**：
- 新增模块/服务
- 新增数据表（>= 3 张）
- 变更公共接口（影响 >= 2 个消费者）
- 涉及安全、权限、资费的变更

**可简化 SDD（仅 Design + Plan）**：
- 单一模块内的功能增强（< 200 行）
- Bug 修复（已定位根因）
- 纯前端 UI 调整

**无需 SDD**：
- 文案修改
- 配置变更
- 单测补充（已有 Spec 覆盖）

---

## 4. Plugin 生成内容的处理

Claude Code plugins（superpowers、ccg 等）生成的 plan/spec 属于**草案**，**不等于项目基线**：

1. Plugin 输出到临时位置（推荐 `.claude/outputs/` 或 plugin 自己的缓存目录）
2. 人工评审后，按本规范命名并移入对应 `docs/` 子目录
3. 未经评审的 plugin 输出不得作为编码依据
4. 旧版 plugin 输出归档到 `docs/archive/`
