# SchemaPlexAI 文档中心

> 统一文档入口。所有项目文档按 SDD + TDD 流程管理，禁止 plugin 随意写入非标准目录。

---

## 文档地图

| 目录 | 内容 | 读者 |
|------|------|------|
| [`requirements/`](requirements/) | 产品需求文档（PRD） | PM、架构师 |
| [`specs/`](specs/) | 技术规格说明书（Spec） | 架构师、开发 |
| [`designs/`](designs/) | 架构设计文档（Design） | 架构师、开发 |
| [`plans/`](plans/) | 实施计划（Plan） | TL、开发 |
| [`decisions/`](decisions/) | 架构决策记录（ADR） | 全团队 |
| [`standards/`](standards/) | 开发规范与流程、审计与评审报告 | 全团队 |
| [`archive/`](archive/) | 已归档的旧版文档 | 参考查阅 |

## 统一评审入口

- [`standards/unified-review-v1.0.md`](standards/unified-review-v1.0.md) — 文档统一评审与归档报告（Specs + Plans + UI + 审计汇总）

---

## SDD + TDD 双轨流程

```
需求(requirements) → 规格(specs) → 设计(designs) → 计划(plans) → 编码+测试 → 评审
                                    ↑___________________________|
                                          TDD: RED → GREEN → REFACTOR
```

**规则**：
- 任何超过 50 行的代码变更必须有对应的 `specs/` 或 `designs/` 文档支撑
- 任何新模块必须有 `designs/` 设计评审通过后才能编码
- 所有代码必须通过 TDD 流程（见 [`standards/tdd-guide.md`](standards/tdd-guide.md)）

---

## 文件命名规范

```
YYYY-MM-DD[-vX.Y]-<topic>.md
```

- `YYYY-MM-DD`：文档创建或重大修订日期
- `vX.Y`：版本号（可选，用于基线文档）
- `topic`：短横线分隔的英文主题词

**示例**：
- `2026-04-29-v1.1-system-architecture.md`
- `2026-04-30-agent-runtime-task-board.md`

---

## Plugin 输出约束

**禁止**以下行为：
- 在 `docs/` 根目录直接创建文件
- 在 `docs/` 下创建 plugin 专属子目录（如 `docs/superpowers/`、`docs/ecc/`）
- 不经评审直接将 plugin 输出视为项目基线文档

**正确做法**：
- plugin 生成的 plan/spec 先输出到临时位置（如 `.claude/outputs/`）
- 经人工/团队评审后，按规范命名并移入对应的 `docs/` 子目录
- 旧版 plugin 输出归档到 `archive/`
