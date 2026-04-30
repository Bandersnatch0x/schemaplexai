---
topic: fix-workflow
stage: standard
version: v1.0
status: 已批准
supersedes: ""
---

# Bug 修复工作流程

> **主题**: `fix-workflow`
> **阶段**: `standard`
> **版本**: v1.0
> **状态**: 已批准
> **日期**: 2026-04-30

---

## 1. 核心原则

1. **先理解，后修复**: 不急于改代码，先复现、定位根因
2. **最小化变更**: 只修复问题本身，不借机重构无关代码
3. **测试先行**: 先写复现测试，确认失败后再修复
4. **根因分析**: 修复后回答"为什么会发生"和"如何防止再次发生"

## 2. 流程概览

```
接收 Bug → 复现与定位 → Overview 当前实现 → 编写 Plan → 评审 Plan
                                                        ↓
                                              开发修复 + 测试 → Code Review → 合并
                                                        ↓
                                              根因分析 + 知识沉淀
```

---

## 3. 阶段详解

### Phase 1: 接收 Bug

**来源**: 用户反馈、监控告警、Code Review、自动化测试

**必须记录的信息**:

```yaml
bug_id: BUG-YYYY-MM-DD-NNN
severity: P0(阻断) / P1(高) / P2(中) / P3(低)
reporter: 报告人
environment: 环境（开发/测试/生产）
module: 所属模块
version: 版本号
title: 一句话描述
steps_to_reproduce: 复现步骤
expected: 期望行为
actual: 实际行为
logs: 相关日志
screenshots: 截图（如有）
```

**Severity 定义**:

| 级别 | 定义 | 响应时间 | 修复时间 |
|------|------|----------|----------|
| **P0** | 系统不可用 / 数据丢失 / 安全漏洞 | 立即 | 4h 内 |
| **P1** | 核心功能不可用 / 严重性能问题 | 2h 内 | 24h 内 |
| **P2** | 非核心功能异常 / 用户体验差 | 1 工作日 | 1 周内 |
| **P3** | 样式问题 / 文案错误 / 优化建议 | 排期处理 | 下次迭代 |

---

### Phase 2: 复现与定位

**执行人**: 开发工程师

**步骤**:

1. **复现**
   - 按报告步骤在本地/测试环境复现
   - 如果无法复现，标记为 `needs-more-info` 并联系报告人
   - 复现后记录：最小复现条件、复现频率（100% / 偶发）

2. **定位根因**
   - 从错误日志出发，向上追溯调用链
   - 使用调试器逐步执行
   - 检查最近的变更（`git log --since="1 week ago" -- 相关文件`）
   - 区分：代码缺陷 / 配置错误 / 环境问题 / 依赖变更

3. **影响范围评估**
   - 该 Bug 影响哪些功能？
   - 是否有数据需要修复？
   - 是否有其他模块有类似问题？（同类漏洞扫描）

**产出**: 根因分析摘要（写入 Bug 记录）

---

### Phase 3: Overview 当前实现

**目标**: 快速了解问题所在代码的上下文

**操作清单**:

1. **代码定位**
   ```bash
   # 找到相关代码
   grep -rn "错误关键词" --include="*.java" schemaplexai-模块/src/
   grep -rn "错误关键词" --include="*.tsx" schemaplexai-ui/src/
   ```

2. **上下文理解**
   - 该代码是何时引入的？（`git blame`）
   - 最近的修改是否引入了回归？（`git log -p -- 文件`）
   - 该模块的测试覆盖率如何？

3. **修复策略选择**
   | 场景 | 策略 |
   |------|------|
   | 简单逻辑错误（空指针、边界条件） | 直接修复 |
   | 设计缺陷 | 评估是否引入技术债务，必要时创建重构任务 |
   | 第三方依赖 Bug | 升级依赖 / 绕过 / 提交 Issue |
   | 环境问题 | 不修改代码，更新配置或文档 |

---

### Phase 4: 编写 Plan

**触发条件**: 根因已定位，修复策略已确定

**轻量级 Plan**（Bug 修复用）:

```markdown
## Fix Plan: BUG-2026-04-30-001

### 根因
xxx 方法在 yyy 条件下未做空值检查，导致 NPE。

### 修复方案
在 xxx 方法入口处增加 `Objects.requireNonNull(param, "...")`。

### 影响范围
仅影响 schemaplexai-xxx 模块的 yyy 功能。

### 测试计划
- [ ] 单元测试：复现 NPE 的测试用例
- [ ] 回归测试：确保正常流程不受影响

### 回退方案
回滚本次提交。

### 预计工时
2h
```

**何时需要完整 Plan**:
- 修复涉及 > 3 个文件
- 需要修改数据库 Schema
- 需要跨服务协调
- P0/P1 级别 Bug

---

### Phase 5: 评审 Plan

**评审人**: 模块 Owner 或技术负责人

**评审重点**:
- [ ] 根因分析是否正确？（避免治标不治本）
- [ ] 修复方案是否最小化？
- [ ] 是否引入了新的风险？
- [ ] 测试计划是否充分？
- [ ] 是否需要 hotfix 分支？（生产环境 P0/P1）

**时间盒**: P0/P1 在 1 小时内完成评审，P2/P3 在 1 工作日内

---

### Phase 6: 开发修复

**分支策略**:

```bash
# P0/P1 生产修复
git checkout -b hotfix/BUG-NNN-main

# P2/P3 常规修复
git checkout -b fix/BUG-NNN-develop
```

**开发规范**:

1. **测试先行**（强制）
   ```java
   @Test
   void shouldNotThrowNpeWhenParamIsNull() {
       // 先写这个测试，确认失败（RED）
       assertThrows(NullPointerException.class, () -> service.process(null));
   }
   ```

2. **最小化变更**
   - 只修改必要的代码行
   - 不重构无关代码
   - 不修改代码风格

3. **提交规范**
   ```
   fix: 修复 xxx 功能空指针异常

   - 在 process() 方法入口增加空值检查
   - 补充单元测试覆盖边界条件

   Fixes BUG-2026-04-30-001
   ```

---

### Phase 7: Code Review

**评审检查单**（Bug 修复专用）:

- [ ] **根因验证**: Reviewer 是否认同根因分析？
- [ ] **修复正确性**: 修复是否确实解决了问题？
- [ ] **无副作用**: 修复是否引入了新的 Bug？
- [ ] **测试充分性**:
  - [ ] 是否有复现该 Bug 的测试？
  - [ ] 是否有回归测试？
  - [ ] 边界条件是否覆盖？
- [ ] **文档同步**: 是否需要更新用户文档 / API 文档 / 变更日志？

**特殊要求**:
- P0/P1 修复必须 **2 人 Review**（模块 Owner + 技术负责人）
- 安全相关 Bug 必须经过 **security-reviewer** agent 审核

---

### Phase 8: 合并与部署

**Hotfix 流程**（P0/P1 生产）:

```
hotfix/BUG-NNN-main → main → 打 tag → 部署生产
         ↓
    cherry-pick 到 develop
```

**常规修复流程**（P2/P3）:

```
fix/BUG-NNN-develop → develop → 随下次发布部署
```

---

### Phase 9: 根因分析与知识沉淀

**修复后必须完成**:

1. **5 Whys 分析**
   ```
   Q: 为什么会发生 NPE?
   A: 未做空值检查。
   Q: 为什么未做空值检查?
   A: 代码评审时未覆盖该分支。
   Q: 为什么评审未覆盖?
   A: 该方法的单元测试缺失。
   Q: 为什么测试缺失?
   A: 开发时未遵循 TDD。
   → 改进措施: 加强 TDD 执行检查
   ```

2. **知识沉淀**
   - 如果是常见错误模式，更新 `docs/standards/common-pitfalls.md`
   - 如果是框架使用问题，更新开发指南
   - 如果是配置问题，更新运维手册

3. **同类漏洞扫描**
   ```bash
   # 扫描代码库中同类问题
   grep -rn "\.get.*null" --include="*.java" . | grep -v "requireNonNull\|Optional"
   ```

---

## 4. 流程对比

| 维度 | Feature 流程 | Fix 流程 |
|------|-------------|----------|
| 起点 | 需求/PRD | Bug 报告 |
| Spec | 必须编写 | 不需要（根因分析替代） |
| Plan | 完整 Plan | 轻量级 Plan |
| 测试 | TDD（先写测试） | 测试先行（复现测试） |
| Code Review | 1 人 | P0/P1 需 2 人 |
| 分支 | feature/xxx | fix/xxx 或 hotfix/xxx |
| 根因分析 | 可选 | 强制 |
| 知识沉淀 | 文档更新 | 必须（5 Whys） |

## 5. 相关文档

- `docs/standards/feature-workflow.md` — 功能开发流程
- `docs/standards/review-checklists.md` — 评审检查单
- `docs/standards/tdd-guide.md` — TDD 指南
