---
topic: claude-code-harness-research
stage: plan
version: v1.0
status: 草稿
supersedes: ""
---

# 技术预研报告：《御舆：解码 Agent Harness》对 SchemaPlexAI 的借鉴价值

> **报告日期**: 2026-04-30
> **研究对象**: lintsinghua/claude-code-book（3031 stars, 683 forks, 42 万字）
> **目标项目**: SchemaPlexAI（AI R&D 协作平台）

---

## 一、研究对象概览

### 1.1 书籍定位

《御舆：解码 Agent Harness》是林清华（lintsinghua）所著的一本深度解析 Claude Code 工程架构的专著。"舆"取自《考工记》"一器而工聚焉者，车为多"，喻指 Agent Harness 是承载智能体运行的运行时框架——LLM 是大脑，Harness 是骨架、肌肉、神经与免疫系统。

**核心命题**: Agent = Model + Harness。2026 年行业关键转移：竞争差异化重心已从 Model 转移到 Harness。LangChain Terminal Bench 2.0 实证：不换模型，只改 Harness，准确率 52.8% -> 66.5%。

### 1.2 内容结构

全书 15 章 + 4 附录，分四大部分：

| 部分 | 章节 | 主题 |
|:---|:---|:---|
| Part 1 基础篇 | Ch01-04 | 新范式、对话循环、工具系统、权限管线 |
| Part 2 核心系统篇 | Ch05-08 | 配置、记忆、上下文、钩子系统 |
| Part 3 高级模式篇 | Ch09-12 | Fork/Coordinator 模式、技能系统、MCP 集成 |
| Part 4 工程实践篇 | Ch13-15 | 流式架构、Plan 模式、构建自己的 Harness |

### 1.3 技术基础

基于对 Claude Code 公开文档和产品行为的架构分析（非泄露源码），Claude Code 本身是一个约 51 万行 TypeScript 的工业级 Agent 系统：
- 1900+ 源文件
- 40+ 内置工具，50+ 斜杠命令
- 552 个 .tsx 文件（React + Ink 终端 UI）
- Bash AST 解析器 2,679 行
- Agent Loop 核心 ~1,700 行

---

## 二、五大关键架构洞察

### 洞察 1：Async Generator 驱动的对话循环 —— Agent 的心跳机制

**核心设计**: `while(true)` 异步生成器主循环，yield 中间事件实现流式输出。

```typescript
// 简化逻辑
while (true) {
    const { messages, toolUseContext, ... } = state;
    // 压缩管道 -> 构建系统提示 -> 调用 LLM API（流式）
    // 收集 tool_use 块 -> 错误恢复（7 个 continue 站点）
    // 工具执行 -> Stop Hook -> 更新状态 -> continue
}
```

**关键创新**:
- **七种 Continue 路径**: 消息扣留、模型降级、上下文崩塌恢复、权限拒绝、工具失败、用户中断、正常继续
- **单一 State 对象**: 伪不可变语义，每次迭代重新赋值，便于调试和状态回溯
- **依赖注入**: `QueryDeps` 接口隔离外部依赖（LLM Client、文件系统、权限系统），极大提升可测试性

**对 SchemaPlexAI 的启示**: `schemaplexai-agent-engine` 的 Agent 执行引擎应采用类似的**状态机 + 异步生成器**架构，而非简单的同步调用链。当前基于 LangChain4j 的实现可以引入 `AsyncGenerator` 模式来支持 SSE 流式输出和中间状态暴露。

---

### 洞察 2：五层权限管线与纵深防御 —— 安全架构的标杆

**六层纵深防御模型**:

```
+-----------------------------------------+
|  L1: CLAUDE.md（指导性约束）~95% 遵守率   |
+-----------------------------------------+
|  L2: Permission Rules（声明性约束）       |
+-----------------------------------------+
|  L3: Hooks（可编程约束）                  |
+-----------------------------------------+
|  L4: YOLO Classifier（AI 约束）          |
|     两阶段：快速80ms + 深度400ms         |
+-----------------------------------------+
|  L5: Sandbox（系统级约束）                |
+-----------------------------------------+
|  L6: Hardcoded Denials（不可覆盖约束）    |
+-----------------------------------------+
```

**五级权限模式谱系**:
`plan -> default -> acceptEdits -> auto -> dontAsk -> bypassPermissions`

**关键数据**: 用户约批准 93% 的权限提示，"每次都弹窗"导致用户疲劳，因此系统通过多层机制减少人工审批依赖。

**对 SchemaPlexAI 的启示**: SchemaPlexAI 作为企业级平台，权限模型远比 Claude Code 复杂（多租户、角色、RBAC）。但**渐进式信任**和**纵深防御**的思想可直接借鉴：
- 在 `schemaplexai-gateway` 的 JWT + 租户隔离之上，增加**操作级权限门控**
- 对 Agent 执行的**危险操作**（如代码生成后的自动部署）引入分级确认机制
- 利用 Flowable 工作流引擎实现**审批流程的灵活配置**

---

### 洞察 3：四级渐进式上下文压缩 —— 有限资源的最优管理

**七层压缩流水线**（按优先级触发）:

```
applyToolResultBudget -> snipCompact -> microcompact -> contextCollapse -> autoCompact -> summarize
```

| 层级 | 名称 | 功能 | 触发时机 |
|:---|:---|:---|:---|
| 1 | HISTORY_SNIP | 剪除重复系统消息 | 早期 |
| 2 | applyToolResultBudget | 工具结果预算截断 | 每次工具执行后 |
| 3 | snipCompact | 截断过长的单个消息 | 上下文压力上升 |
| 4 | microcompact | 微压缩近期工具结果 | 接近阈值 |
| 5 | contextCollapse | 语义上下文折叠 | 严重压力 |
| 6 | autoCompact | 自动压缩旧消息 | 配置启用时 |
| 7 | summarize | 汇总整个对话 | 最后手段 |

**关键参数**:
- **92% 阈值**: 预留 8% 缓冲区（约 16K tokens）
- **反向遍历**: Token 统计从后往前，O(n) -> O(k) 优化
- **13000 tokens 缓冲**: 连续失败 3 次后停止，防止 API 浪费

**对 SchemaPlexAI 的启示**: SchemaPlexAI 的 `context` 服务已集成 Milvus 向量数据库用于 RAG。上下文压缩机制可以：
- 在 `schemaplexai-agent-engine` 中实现**Token 预算管理**，防止 LLM 调用超出预算
- 利用向量检索实现**智能上下文选择**（仅加载相关记忆），而非简单截断
- 结合 ClickHouse 的**成本分析**能力，建立 Token 使用与成本的实时关联

---

### 洞察 4：Fork 模式与 Coordinator 模式 —— 多 Agent 协作的双轨方案

**Fork 模式**（父子关系，临时派生）:
```
主 Agent --Fork--> 子 Agent（隔离上下文，试错探索）
   +<-------------- 结论回传，"用完即毁"
```

- **缓存共享**: Fork 子 Agent 以近乎零成本共享父进程的 Prompt Cache
- **两种模式**: `context: 'inline'`（共享历史）/ `context: 'fork'`（完全隔离）
- **应用场景**: Dream 记忆整合、投机执行、危险操作隔离

**Coordinator 模式**（中心调度，持久协作）:
```
Coordinator Agent（AI 项目经理）
    +-- 派活 -> Worker 1（研究）
    +-- 派活 -> Worker 2（实现）
    +-- 派活 -> Worker 3（验证）
```

- **核心 Prompt**: 370 行自然语言 = 完整项目管理手册
- **关键原则**: "只编排不执行"、"理解力不外包给下属"
- **Worker 工具过滤**: Simple 模式仅给基础工具

**对 SchemaPlexAI 的启示**: SchemaPlexAI 的 `workflow` 服务已集成 Flowable BPMN，天然支持**Coordinator 模式**：
- 将 BPMN 流程节点映射为不同 Worker Agent 的任务分配
- 利用 Flowable 的**会签/或签**机制实现多 Agent 结果的聚合
- Fork 模式可用于**沙箱测试**（如 Agent 生成的代码在隔离环境中验证后再合并）

---

### 洞察 5：六层配置优先级链 —— "Agent DNA" 的设计

**配置合并规则**（后覆盖前）:

```
默认值 -> 全局设置 -> 项目设置 -> 本地设置 -> 命令行参数 -> 环境变量 -> 会话覆盖
```

**关键设计**:
- **安全边界**: `settings.json` 始终不可写（硬编码拒绝）
- **双层功能门控**: 编译时（死代码消除）+ 运行时（GrowthBook Flag）
- **恢复时不还原权限**: 每次重启必须重新授权

**对 SchemaPlexAI 的启示**: SchemaPlexAI 的多租户环境需要**分层配置**：
- 全局平台配置（`application.yml`）-> 租户级配置（数据库）-> 项目级配置 -> 用户级配置
- 利用 Spring Cloud Config 或 Nacos 实现**配置中心化管理**
- 对 AI 模型参数（temperature、maxTokens 等）引入**租户级覆盖**机制

---

## 三、可直接借鉴的"闪光点"

### 闪光点 1：StreamingToolExecutor —— 流式中途执行

**传统模式**: 接收完整响应 -> 解析 -> 执行工具
**Claude Code 模式**: 响应仍在传输时就开始执行工具

**技术实现**:
- `toolOrchestration.ts` 将工具调用分为**并发批次**和**串行批次**
- 工具声明 `isConcurrencySafe()` — 读操作并行，写操作互斥
- `maxResultSizeChars` 预算控制，超大结果截断并持久化到临时文件

**SchemaPlexAI 落地建议**:
- 在 `schemaplexai-agent-engine` 的 LLM 调用层引入**流式响应解析**
- 一旦检测到 `tool_use` 块，立即触发工具执行，无需等待完整响应
- 利用 RabbitMQ 的**异步消息**机制解耦工具执行与响应收集

---

### 闪光点 2：Auto Dream —— 后台记忆巩固机制

**模拟人类 REM 睡眠**，定期整理记忆：
- **触发**: >=24h 且 >=5 个新 Session
- **四阶段**: 定向 -> 收集 -> 整合 -> 减值/修剪
- **技术**: Forked Agent 后台运行，锁文件互斥，15 秒预算限制

**关键原则**: "只保存代码外信息" — 可从源码推导的信息不进入长期记忆。

**SchemaPlexAI 落地建议**:
- 在 `schemaplexai-context` 服务中实现**定时任务**（Spring Scheduler / XXL-Job）
- 利用 Milvus 的**向量聚类**能力自动整理知识库
- 结合 `schemaplexai-ops` 的 ClickHouse 成本数据，生成**项目级经验记忆**

---

### 闪光点 3：Hooks 系统 —— 事件驱动的扩展机制

**26 个生命周期事件 x 5 种 Hook 类型**:

| Hook 类型 | 说明 |
|:---|:---|
| PreToolUse | 工具执行前拦截 |
| PostToolUse | 工具执行后处理 |
| Notification | 状态变更通知 |
| Stop | 会话终止处理 |
| SubagentStop | 子 Agent 终止 |

**SchemaPlexAI 落地建议**:
- 在 `schemaplexai-web` 的 SSE/WebSocket 层引入**事件总线**（Spring Event / RabbitMQ）
- 允许用户通过**插件机制**注册自定义 Hook（如代码审查 Hook、安全扫描 Hook）
- 与 `schemaplexai-quality` 的**质量门禁**集成，在 PostToolUse 阶段自动触发检查

---

### 闪光点 4：MCP Bridge 系统 —— 双向协议集成

**七种传输协议**: stdio / http / sse / sse-ide / ws-ide / http-streamable / sdk

**双向数据流**:
- **出站**: 对话消息、工具调用、状态变更
- **入站**: 权限响应（最高优先级）、控制请求、用户消息

**SchemaPlexAI 落地建议**:
- `schemaplexai-integration` 服务已实现 GitHub/GitLab/Jenkins 集成，可扩展支持 **MCP 协议**
- 作为 MCP Host，消费外部工具（如代码审查工具、测试工具）
- 作为 MCP Server，向外部 IDE（VS Code / JetBrains）暴露 SchemaPlexAI 的 Agent 能力

---

### 闪光点 5：Plan 模式 —— "先想后做"的结构化工作流

**核心哲学**: 分离"只读探索"和"可写执行"两个阶段。

**三层恢复策略**:
- 计划文件本地持久化
- 中断后从计划文件恢复
- 远程触发执行

**SchemaPlexAI 落地建议**:
- 在 `schemaplexai-spec` 服务的**规格文档评审**流程中引入 Plan 模式
- Agent 先生成**执行计划**（只读），经人工确认后再执行（可写）
- 利用 Flowable 的**用户任务节点**实现计划审批流程

---

## 四、Adoption 建议与优先级

| 优先级 | 借鉴点 | 目标模块 | 工作量 | 预期收益 |
|:---|:---|:---|:---|:---|
| **P0** | StreamingToolExecutor 流式执行 | `agent-engine` | 中 | 大幅降低 LLM 调用延迟 |
| **P0** | 渐进式权限管线 | `gateway` + `system` | 中 | 提升企业安全合规性 |
| **P1** | Fork 模式（子 Agent 隔离） | `agent-engine` + `workflow` | 高 | 支持复杂任务的安全试错 |
| **P1** | Coordinator 模式（Flowable 集成） | `workflow` | 中 | 成熟的多 Agent 编排能力 |
| **P1** | 四级上下文压缩 | `agent-engine` + `context` | 中 | 降低 Token 成本 30%+ |
| **P2** | Auto Dream 记忆巩固 | `context` + `ops` | 高 | 长期知识积累与复用 |
| **P2** | Hooks 插件系统 | `web` + 全局 | 高 | 平台可扩展性大幅提升 |
| **P2** | MCP Bridge 双向集成 | `integration` | 高 | 生态互通能力 |
| **P3** | Plan 模式工作流 | `spec` + `workflow` | 低 | 提升 Agent 执行可控性 |
| **P3** | 六层配置优先级链 | `system` | 低 | 配置管理灵活性 |

---

## 五、关键风险与注意事项

1. **技术栈差异**: Claude Code 基于 TypeScript/Bun，SchemaPlexAI 基于 Java/Spring Boot。部分模式（如 Async Generator）需要语言级适配。
2. **企业级复杂度**: Claude Code 是单用户 CLI 工具，SchemaPlexAI 是多租户 SaaS 平台。权限、隔离、审计要求更高。
3. **开源合规**: 本书基于公开文档分析，未引用泄露源码。SchemaPlexAI 的借鉴应遵循同样的**公开信息**原则。
4. **Token 成本控制**: 流式执行和上下文压缩需要精细的 Token 预算管理，否则可能适得其反。

---

## 六、参考资源

- [《御舆：解码 Agent Harness》GitHub 仓库](https://github.com/lintsinghua/claude-code-book)
- [Claude Code 核心架构深度解析](https://juejin.cn/post/7627436724852457507)
- [Harness Engineering 第21章 设计模式与架构决策](https://juejin.cn/post/7628880606123425834)
- [Claude Code 源码逆向工程与系统性分析](https://qingkeai.online/archives/Claude%20Code)
- [Claude Code 记忆系统深度解析](https://post.smzdm.com/p/aqr3rl07)
- [Claude Code MCP 协议集成全拆解](https://cloud.tencent.com/developer/article/2653446)

---

**报告总结**：《御舆：解码 Agent Harness》系统性地解构了工业级 Agent 系统的工程化方法。对 SchemaPlexAI 而言，最具 immediate value 的是**流式工具执行**、**渐进式权限管线**和**Fork/Coordinator 多 Agent 模式**。这些模式与 SchemaPlexAI 现有的 Spring Boot + Flowable + LangChain4j 技术栈高度兼容，可在 2-3 个迭代周期内落地。
