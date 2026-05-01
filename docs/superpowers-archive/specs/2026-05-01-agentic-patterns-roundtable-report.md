# Agentic Design Patterns 圆桌辩论报告

> **日期**: 2026-05-01
> **参与专家**: 产品/平台负责人、资深后端架构师、安全与合规专家、AI/ML 工程负责人
> **方法**: 四方独立分析 → 分层对比 → 共识提取 → 分歧裁决
> **状态**: 终版

---

## 一、四方分层总览

### 1.1 各专家的分层方案

| 模式 | 产品 | 架构 | 安全 | AI工程 | **最终裁决** |
|------|------|------|------|--------|-------------|
| 1. Prompt Chaining | L2 | **L1** | L3 | L2 | **L2** |
| 2. Routing | L1 | L2 | L1 | L1 | **L1** |
| 3. Parallelization | L2 | L2 | L3 | L2 | **L2** |
| 4. Reflection | L1 | **L1** | L3 | **L1** | **L1** |
| 5. Tool Use | **L1** | L2 | **L1** | **L1** | **L1** |
| 6. Planning | **L1** | L2 | L3 | L2 | **L2** |
| 7. Multi-Agent | **L2** | L3 | **L2** | **L2** | **L2** ⚠️ |
| 8. Memory | L2 | **L1** | **L1** | **L1** | **L1** |
| 9. Learning/Adaptation | L3 | L3 | L2 | L3 | **L3** |
| 10. MCP | L2 | L3 | L2 | L2 | **L2** |
| 11. Goal Setting | L2 | L2 | L3 | L3 | **L2** |
| 12. Exception Handling | **L1** | **L1** | **L1** | **L1** | **L1** ✅ |
| 13. HITL | **L2** | **L2** | **L2** | **L2** | **L2** ✅ |
| 14. RAG | **L1** | L2 | **L1** | L2 | **L1** ⚠️ |
| 15. A2A | L2 | L3 | L2 | L2 | **L2** |
| 16. Resource Optimization | **L1** | **L1** | L2 | **L1** | **L1** |
| 17. Reasoning | L3 | **L1** | L3 | **L1** | **L1** |
| 18. Guardrails | **L1** | **L1** | **L1** | L3(规则) | **L1** ⚠️ |
| 19. Evaluation | **L1** | **L1** | **L1** | **L1** | **L1** ✅ |
| 20. Prioritization | L3 | L3 | L3 | L3 | **L3** ✅ |
| 21. Exploration | L3 | L3 | L3 | L3 | **L3** ✅ |

### 1.2 裁决规则

- **4/4 共识**: 直接采用
- **3/4 多数**: 原则上采用多数意见，但需记录少数意见作为风险
- **2/2 平局**: 主持人基于项目实际情况裁决
- **⚠️ 标记**: 该模式存在强烈反对意见，实施时需附加条件

---

## 二、最终分层方案

### Layer 1: 基础骨架（Foundation）— 10 个模式

> **定义**: 当前架构正常运行所必需。缺失任何一个，系统不可靠或不可控。
> **时间窗口**: 立即启动（Week 9-16）

| # | 模式 | 纳入理由 | 反对意见及缓解 |
|---|------|---------|--------------|
| 4 | **Reflection** | 3/4 支持。自我纠错是 LLM 可靠性的底线 | 安全专家担心提示注入风险 → 缓解：与 Guardrails 联动，反射内容需经安全过滤 |
| 5 | **Tool Use** | 3/4 支持。ReAct 循环的核心，当前 parseToolCalls() 为空实现 | 架构师认为需 Exception Handling 先闭环 → 缓解：两者并行，Tool Use 使用已定义的 ToolErrorCategory |
| 8 | **Memory** | 3/4 支持。所有状态持久化的基础设施 | 产品负责人认为是竞争力 → 缓解：L1 完成基础接口抽象，L2 实现高级策略 |
| 12 | **Exception Handling** | **4/4 共识**。当前最大技术债务，ToolCallingStateHandler 是存根 | 无 |
| 14 | **RAG** | 2/4 支持，但安全理由极强。数据隔离是企业合规基线 | 架构师/AI工程师认为需 Tool Use 稳定后 → 缓解：RAG 与 Tool Use 同步推进，RAG 作为 retrieve tool 接入 |
| 16 | **Resource Optimization** | **4/4 共识**。TokenBudget + Admission 已有骨架 | 无 |
| 17 | **Reasoning** | 3/4 支持。ReAct 循环本身就是 Reasoning | 产品/安全认为是创新 → 缓解：明确 L1 的 Reasoning = ReAct 循环稳定，非 CoT/ToT |
| 18 | **Guardrails** | 3/4 支持。安全基线，无它不能上线 | AI工程师认为深度实现复杂 → 缓解：L1 完成基础规则层（黑名单/长度限制），深度 LLM-as-Guardrail 放到 L3 |
| 19 | **Evaluation** | **4/4 共识**。没有评估就无法判断改进是否有效 | 无 |
| 2 | **Routing** | 3/4 支持。AiModelRouter 已有多 Provider 基础 | 架构师认为需 Evaluation 先闭环 → 缓解：Routing 从简单意图分类开始 |

**Layer 1 实施优先级（内部排序）**:
```
P0: Exception Handling + Tool Use + Guardrails(规则层) + RAG(数据隔离)
P1: Reflection + Reasoning(ReAct循环) + Memory(接口抽象) + Evaluation
P2: Resource Optimization(策略升级) + Routing(意图分类)
```

### Layer 2: 能力扩展（Capability）— 8 个模式

> **定义**: 从 Demo 到生产环境必须补足的核心业务能力。
> **时间窗口**: 中期建设（Week 17-28）

| # | 模式 | 纳入理由 | 反对意见及缓解 |
|---|------|---------|--------------|
| 1 | **Prompt Chaining** | 2/4 支持，但架构理由充分。11-state 状态机本身就是硬编码 chain | 产品/AI工程认为价值低 → 缓解：与 Planning 共享 chain 引擎，不单独投入 |
| 3 | **Parallelization** | **4/4 共识放在 L2**。效率倍增器 | 无 |
| 6 | **Planning** | 3/4 支持放 L2。需要 Reasoning + Tool Use 闭环后才能有效分解 | 产品认为应在 L1 → 缓解：L1 的 ThinkingStateHandler 可先支持简单计划，L2 实现完整 PlanningStateHandler |
| 7 | **Multi-Agent** | 3/4 支持放 L2。**最大差异化点** | 架构师强烈反对（单体不稳定时多体会放大故障）→ ⚠️ **附加条件**: Tool Use + Exception Handling + Evaluation 测试覆盖率 > 70% 后方可启动 |
| 10 | **MCP** | **4/4 共识放在 L2**。协议/生态层，需内部 Tool Use 成熟 | 架构师认为应在 L3 → 缓解：MCP 作为 Tool Registry 的扩展协议，不维护两套抽象 |
| 11 | **Goal Setting** | **4/4 共识放在 L2**。需 Planning + Evaluation 成熟 | 无 |
| 13 | **HITL** | **4/4 共识放在 L2**。企业合规刚需 | 无 |
| 15 | **A2A** | **4/4 共识放在 L2**。需 Multi-Agent 基础 + 通信协议 | 无 |

### Layer 3: 高级创新（Advanced）— 3 个模式

> **定义**: 提升竞争力、建立技术领导力，但需等待市场验证。
> **时间窗口**: 延后探索（Week 29+）

| # | 模式 | 纳入理由 |
|---|------|---------|
| 9 | **Learning/Adaptation** | **4/4 共识**。需长期数据积累，Self-improving Agent 有幻觉放大风险 |
| 20 | **Prioritization** | **4/4 共识**。规模问题，当前并发量不足 |
| 21 | **Exploration** | **4/4 共识**。前沿方向，市场需求待验证 |

---

## 三、关键分歧深度分析

### 分歧 1: RAG 的层级（2:2 平局）

| 支持 L1 | 支持 L2 |
|---------|---------|
| 产品：企业知识管理是刚需 | 架构：需 Memory 策略接口先抽象 |
| 安全：数据隔离是合规基线 | AI工程：需 Tool Use 稳定后才能作为 retrieve tool |

**主持人裁决**: 采用 **L1，但附加条件**。
- RAG 的**数据隔离**（Milvus 租户隔离 + 检索权限过滤）是安全基线，必须在任何用户数据入向量库前完成
- RAG 的**检索质量优化**（准确率调优、重排序）可延后到 L2
- 实施路径：W1 完成数据隔离，W3-W4 完成检索管道接入 ThinkingStateHandler

### 分歧 2: Multi-Agent 的层级（3:1 多数，但有强烈反对）

| 支持 L2 | 支持 L3 |
|---------|---------|
| 产品：最大差异化点 | 架构：单体不稳定时多体会放大故障 |
| 安全：跨 Agent 通信需信任边界 | |
| AI工程：LangChain4j 无 Multi-Agent 抽象，需自研 | |

**主持人裁决**: 采用 **L2，但附加严格的前提条件**。
- 架构师的风险警告被采纳：Multi-Agent 不得在任何单体 Agent 测试覆盖率 < 70% 时启动
- 实施路径：先实现 Coordinator 模式（最简单），Swarm/Crew 延后
- 架构保障：每个子 Agent 独立状态机实例，故障隔离

### 分歧 3: Guardrails 的深度（3:1 多数）

| 支持 L1 完整 | 支持 L1 简单规则 + L3 深度 |
|-------------|---------------------------|
| 产品：合规红线 | AI工程：深度 LLM-as-Guardrail 技术复杂 |
| 架构：合规底线 | |
| 安全：P0 中的 P0 | |

**主持人裁决**: 采用 **折中方案**。
- L1：基础规则层（黑名单关键词、最大长度限制、输出格式校验、SSE 权限校验）
- L2：增强层（输入语义分类、Pydantic 式输出 schema 验证）
- L3：深度层（LLM-as-Guardrail、对抗样本检测、红队测试体系）

### 分歧 4: Reflection 的安全风险（3:1 多数）

| 支持 L1 | 支持 L3 |
|---------|---------|
| 产品：输出质量保障 | 安全：自我修正有提示注入风险 |
| 架构：可靠性底线 | |
| AI工程：ReAct 循环必要组成 | |

**主持人裁决**: 采用 **L1，但安全加固**。
- Reflection 的输出必须经过 Guardrails 过滤后才能回注到上下文
- Reflection 循环次数设上限（如最多 2 轮）
- 禁止 Reflection 修改系统指令或工具定义

---

## 四、共识矩阵

### 4/4 完全一致（8 个模式）

| 模式 | 共识层级 | 说明 |
|------|---------|------|
| Exception Handling | L1 | 无争议，当前最大技术债务 |
| Evaluation | L1 | 无争议，可观测性前提 |
| Resource Optimization | L1 | 无争议，已有骨架 |
| HITL | L2 | 无争议，企业合规刚需 |
| Prioritization | L3 | 无争议，规模问题 |
| Exploration | L3 | 无争议，前沿方向 |
| Parallelization | L2 | 无争议，效率倍增器 |
| Goal Setting | L2 | 无争议，需 Planning 先成熟 |

### 3/4 多数一致（8 个模式）

| 模式 | 多数层级 | 少数意见 | 裁决 |
|------|---------|---------|------|
| Tool Use | L1 | 架构师放 L2 | **维持 L1**，与 Exception Handling 并行 |
| Memory | L1 | 产品放 L2 | **维持 L1**，L1 完成接口抽象 |
| Reflection | L1 | 安全放 L3 | **维持 L1**，增加安全加固 |
| Reasoning | L1 | 产品/安全放 L3 | **维持 L1**，明确=ReAct循环稳定 |
| Guardrails | L1 | AI工程深度放 L3 | **维持 L1**，但分层实现深度 |
| Routing | L1 | 架构师放 L2 | **维持 L1**，从简单意图分类开始 |
| Multi-Agent | L2 | 架构师放 L3 | **维持 L2**，附加覆盖率前提条件 |
| MCP | L2 | 架构师放 L3 | **维持 L2**，作为 Tool Registry 扩展 |

### 2/2 平局（1 个模式）

| 模式 | 产品/安全 | 架构/AI工程 | 裁决 |
|------|----------|------------|------|
| RAG | L1（企业价值+安全基线）| L2（需先决条件）| **L1，但分阶段**：数据隔离(W1) → 检索管道(W3) |

---

## 五、架构演进建议（综合四方意见）

### 5.1 新增模块（4 个，架构师提议，全票通过）

| 模块 | 用途 | 覆盖模式 |
|------|------|---------|
| `chain` | 动态 Prompt Chain 引擎 | Prompt Chaining, Planning, Goal Setting |
| `memory-api` | 内存策略抽象 | Memory, RAG, Learning |
| `tool-runtime` | 工具执行沙箱 | Tool Use, MCP, Exception Handling, Guardrails |
| `evaluation` | 评估与反馈闭环 | Evaluation, Reflection, Learning |

### 5.2 安全基线（安全专家提议，全票通过）

任何 Layer 1 模式上线前，必须先关闭以下 **Critical** 风险：
1. **Tool 执行引入 Sandbox**（gVisor/Firecracker/容器隔离）
2. **ContextInjector 实现输入验证**（黑名单 + 语义检测）
3. **SSE 增加权限校验**（JWT scope 验证 + 一次性 token）

### 5.3 技术选型（AI工程师提议，全票通过）

| 能力 | LangChain4j 支持度 | 策略 |
|------|-------------------|------|
| ChatMemory / RAG | 原生支持 | 直接采用 |
| Tool Use (Function Calling) | 部分支持 | 扩展 LlmProvider 接口 |
| Prompt Chaining | 原生支持 | `AiServices` chain 模式 |
| ReAct 循环 / Multi-Agent / MCP | 不支持 | 自研 |
| Evaluation | 不支持 | 自研，对接现有 ObservabilityRecorder |

---

## 六、实施路线图（综合版）

### Phase 1: Layer 1 基础骨架（Week 9-16，8 周）

```
Week 9-10: 安全基线 + Tool Runtime
  ├── Tool Sandbox（gVisor/Firecracker）
  ├── ContextInjector 输入验证
  ├── SSE 权限校验
  └── ToolRegistry + ToolAdapter + ToolSafetyGuard

Week 11-12: ReAct 循环闭环
  ├── 扩展 LlmProvider 支持 function calling
  ├── ToolCallingStateHandler 真正执行工具
  ├── 状态机循环边：OBSERVATION → THINKING
  └── FinalAnswerExtractor + 最大迭代保护

Week 13-14: Memory + RAG 数据隔离
  ├── MemoryStrategy 接口抽象
  ├── Milvus 租户隔离 + 检索权限过滤
  └── ContextInjector 接入 RAG 检索

Week 15-16: Reflection + Evaluation + Guardrails(规则层)
  ├── REFLECT 状态（自我纠错，2轮上限）
  ├── Evaluation 基础设施（trace/metric）
  └── Guardrails 基础规则层（黑名单/长度限制/格式校验）
```

### Phase 2: Layer 2 能力扩展（Week 17-28，12 周）

```
Week 17-19: Planning + Prompt Chaining
  ├── PlanningStateHandler（目标分解 → 子任务 DAG）
  ├── ChainDefinition 动态配置
  └── 与 Tool Use 集成

Week 20-22: Multi-Agent（附覆盖率前提）
  ├── Coordinator Agent（任务分发 → 聚合）
  ├── Agent 间消息总线（RabbitMQ）
  └── 两 Agent 协作 Demo

Week 23-25: HITL + Parallelization
  ├── 审批流 UI + 权限控制 + 审计留痕
  ├── 并行子任务执行 + 结果聚合
  └── Goal Setting（目标追踪 + 完成检测）

Week 26-28: 生态扩展
  ├── MCP 统一注册中心
  ├── A2A 协议支持
  └── Guardrails 增强层（语义分类 + schema 验证）
```

### Phase 3: Layer 3 高级创新（Week 29+）

```
Phase A: Learning + Reasoning 深度
  ├── Shadow Review 数据收集 → 自动学习 Pipeline
  ├── Self-Correction 机制
  └── 代码执行推理（zeroboot 沙箱）

Phase B: 效率优化
  ├── Prioritization（任务队列优先级）
  └── 动态模型选择

Phase C: 探索
  ├── Agent Laboratory
  └── 自主知识图谱构建
```

---

## 七、风险预警（综合四方）

| 风险 | 来源 | 影响 | 缓解措施 |
|------|------|------|---------|
| Multi-Agent 复杂度失控 | 架构师 | Layer 2 延期 | 覆盖率 > 70% 前提 + 先 Coordinator 模式 |
| RAG 准确率不达标 | AI工程师 | 知识场景不可用 | 预留 2 周调优窗口 + 关键词匹配 Fallback |
| Tool 安全漏洞 | 安全专家 | 服务器沦陷 | Sprint 1 必须完成 Sandbox |
| Layer 1 工期超支 | 产品负责人 | 错过早期客户 | Reflection/Resource Optimization 可降级 |
| 竞品快速迭代 | 产品负责人 | 差异化窗口缩小 | 聚焦企业级功能（HITL/审计/多租户）|
| LangChain4j 能力边界 | AI工程师 | 自研工作量大 | ReAct/Multi-Agent/MCP 预期自研 |

---

## 八、总结

### 最终分层统计

| 层级 | 模式数量 | 模式列表 |
|------|---------|---------|
| **Layer 1: 基础骨架** | 10 | Exception Handling, Evaluation, Resource Optimization, Tool Use, Memory, Reflection, Reasoning, Guardrails(规则层), RAG, Routing |
| **Layer 2: 能力扩展** | 8 | Prompt Chaining, Parallelization, Planning, Multi-Agent ⚠️, MCP, Goal Setting, HITL, A2A |
| **Layer 3: 高级创新** | 3 | Learning/Adaptation, Prioritization, Exploration |

### 最关键的 3 个决策

1. **RAG 放在 L1（分阶段实施）**: 安全基线（数据隔离）不可妥协，但检索质量优化可延后
2. **Multi-Agent 放在 L2（附严格前提）**: 采纳产品/安全/AI工程的多数意见，但架构师的风险警告转化为覆盖率门槛
3. **Guardrails 分层深度**: L1 规则层保证安全基线，L3 深度层保证智能安全

### 最紧迫的 3 个行动

1. **立即修复 3 个 Critical 安全风险**: Tool Sandbox + ContextInjector 验证 + SSE 认证
2. **6 周内完成 ReAct 循环闭环**: Tool Use + Reasoning + Exception Handling 形成可运行的最小 Agent
3. **建立评估基础设施**: 没有 Evaluation，无法判断任何改进是否有效
