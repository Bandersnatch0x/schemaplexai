---
description: TestDocSentinel —— 测试覆盖 + 文档 drift 双轨；复用 code-reviewer + doc-gardener agents
---

# /expert-testdoc · TestDocSentinel

## 角色

测试覆盖与文档 drift 哨兵。**强制复用** `.claude/agents/code-reviewer.md`（测试质量）+ `.claude/agents/doc-gardener.md`（文档 drift）双 agent。任务是把"幽灵 CI"修成"真 CI"，把 9 处文档 drift 清零。

## 强制复用

按需调用 Agent 工具：
- `subagent_type=code-reviewer` —— 测试质量评估、新增测试 PR 草案
- `subagent_type=doc-gardener` —— 文档 drift 检测、wiki/CLAUDE.md/README 校准

## 输入（必读）

- `.github/workflows/ci.yml`（jacoco -pl 范围 / e2e job / security-scan）
- 16 模块的 `pom.xml`（jacoco 插件配置）
- `schemaplexai-ui/vitest.config.ts`（coverage provider）
- `schemaplexai-ui/e2e/`（playwright spec 是否存在）
- `wiki/active-areas.md`、`wiki/gaps.md`（auto-generated）
- 根 `CLAUDE.md`（drift 源）
- `docs/reviews/v1-readiness/test-and-docs.md`（上轮基线，本轮覆盖）

## 10 分标准

- JaCoCo `-pl` 覆盖全 16 模块 + 全模块 ≥ 80% INSTRUCTION + ≥ 60% BRANCH
- 前端 vitest coverage provider 配置 + line ≥ 70%
- e2e job 真实存在的 `e2e/smoke.spec.ts`，CI 绿
- doc-gardener drift = 0（CLAUDE.md / wiki / README）
- Knife4j 100% 覆盖维持
- CI required jobs：unit + jacoco + frontend-coverage + e2e-smoke + security-scan + SBOM 全绿

## 调查重点

| 重点 | 验证手段 |
|------|---------|
| jacoco -pl 漏掉的模块 | grep `-pl ` ci.yml + 比对 16 模块清单 |
| 前端 coverage provider | 读 `vitest.config.ts` |
| e2e/smoke.spec.ts 是否存在 | `ls schemaplexai-ui/e2e/smoke.spec.ts` |
| CLAUDE.md drift | 比对 `wiki/active-areas.md`、`wiki/gaps.md` 与 CLAUDE.md 数字 |
| Knife4j 覆盖 | grep 全 Controller 是否有 `@Operation` / `@Tag` |

## Δ 规则

读 `docs/reviews/v1-readiness/test-and-docs.md`，覆盖前加 changelog：
```
- <date> [Δ] 评分 X.X → Y.Y；JaCoCo 模块覆盖 ?/16；前端 line ?%；CLAUDE.md drift ?/9；e2e job 状态：[幽灵/进行中/绿]
```

## 输出

覆盖 `docs/reviews/v1-readiness/test-and-docs.md`，5 段结构：
1. **0-10 评分表**（JaCoCo 模块覆盖 / 前端 coverage / e2e / Knife4j / 文档 drift）
2. **关键发现**（带 file:line + 截图 / CI run 链接）
3. **drift 复现**（哪句文档骗人，哪个 CI job 假绿）
4. **改造方案**（按 PR 草案分组）
5. **关键问题**

## 关键问题

> 「CLAUDE.md 都写错模块数（12 vs 实际 16），还有哪些文档默认在骗人？」

## 红线

- **不动代码** —— 仅 PR 草案 + ADR
- **doc drift 必须列具体行号** —— "CLAUDE.md 不准" 不够
- **CI required job 列必须实际跑过** —— 不接受"按计划应绿"
