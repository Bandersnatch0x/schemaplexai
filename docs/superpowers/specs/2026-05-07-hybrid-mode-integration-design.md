---
title: 混合模式集成设计 — Skill/Role + Sub-agent + Compaction + MCP + Sandbox
topic: hybrid-mode-integration
stage: design
version: 1.0
date: 2026-05-07
status: draft
authors: [product, ai-engineering, architecture, security]
---

# SchemaPlexAI 混合模式集成设计

## 1. 背景与目标

### 1.1 问题

SchemaPlexAI 当前是一个"工程师工具"：11-state 状态机驱动 Agent 执行，ToolRegistry 管理工具调用，CompositeChatMemoryStore 管理对话历史。但缺少三个关键能力：

1. **人机协作界面** — 无法让非工程师定义 Agent 行为（Skill/Role）
2. **多 Agent 协作** — 单 Agent 单轮问答，无法处理复杂工作流
3. **生态接入** — MCP 存根，无法接入外部工具生态

### 1.2 方案选择

经四方专家（产品、AI工程、架构、安全）圆桌辩论，采用**混合模式（方案 B）**：从 Flue、LangGraph、Spring AI、Claude Code 中择优集成，而非全盘移植任一框架。

### 1.3 设计原则

1. **零侵入现有状态机** — 所有新能力作为扩展，不改 11-state 状态机
2. **渐进增强** — 新增实现而非重构接口
3. **安全前置** — P0 安全项必须在任何代码合并前完成
4. **行业对齐** — Skill/Role Markdown 驱动是 Sim Studio + Flue 的行业共识

---

## 2. 分层方案

| 层级 | 维度 | 优先级 | 时间窗口 |
|------|------|--------|---------|
| **Layer 1** | Skill + Role 体系 | L1（4/4 共识） | Week 10-11 |
| **Layer 1** | Task / Sub-agent | L1（2/2 平局+裁决） | Week 15-16 |
| **Layer 1** | MCP 集成 | L1（3/4 多数） | Week 13-14 |
| **Layer 2** | Compaction 自动触发 | L2（4/4 共识） | Week 12 |
| **Layer 2** | Session 持久化升级 | L2（3/4 多数） | Week 17+ |
| **Layer 2** | Sandbox 抽象增强 | L2（2/2 平局+裁决） | Week 17+ |

### 安全前置条件（P0，阻塞所有实施）

| # | 前置条件 | 验收标准 |
|---|---------|---------|
| 1 | SkillLoader 沙箱化解析 | Markdown 在隔离线程解析，frontmatter name≤64/description≤500，禁止 HTML |
| 2 | CompositeChatMemoryStore 加密 | AES-256-GCM 按租户加密，DB/Redis 中 LlmMessage.content 为密文 |
| 3 | LocalProcessSandbox 环境变量清理 | exec() 前移除 PASSWORD/SECRET/TOKEN/KEY 环境变量 |

### 安全前置条件（P1，阻塞 Task/Sub-agent + MCP）

| # | 前置条件 | 验收标准 |
|---|---------|---------|
| 4 | 全局子 Agent 配额 + Guardrails 传播 | Redis 计数器阈值 16，子 Agent 继承父 Guardrails |
| 5 | MCP 服务器白名单 + 工具描述校验 | 未授权服务器拒绝，恶意 description 被过滤 |

---

## 3. Design Section 1: Skill + Role 体系

### 3.1 目标

引入 Markdown 驱动的 Skill/Role 体系，让非工程师可通过编辑 `.md` 文件定义 Agent 行为。

**行业验证**: Sim Studio 和 Flue 独立采用了相同的 `.agents/skills/<name>/SKILL.md` 模式，证明这是行业 emergent best practice。

### 3.2 数据模型

```sql
-- Skill 定义
CREATE TABLE sf_agent_skill (
    id           BIGSERIAL PRIMARY KEY,
    tenant_id    BIGINT NOT NULL,
    name         VARCHAR(64) NOT NULL,        -- frontmatter name
    description  VARCHAR(500),                -- frontmatter description
    content      TEXT NOT NULL,               -- Markdown body (instructions)
    version      INTEGER NOT NULL DEFAULT 1,
    status       SMALLINT NOT NULL DEFAULT 1, -- 0=draft, 1=active, 2=archived
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, name)
);

-- Skill 版本快照（运行时锁定）
CREATE TABLE sf_agent_skill_version (
    id           BIGSERIAL PRIMARY KEY,
    skill_id     BIGINT NOT NULL REFERENCES sf_agent_skill(id),
    tenant_id    BIGINT NOT NULL,
    version      INTEGER NOT NULL,
    content      TEXT NOT NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (skill_id, version)
);

-- Role 定义（system prompt overlay）
CREATE TABLE sf_agent_role (
    id           BIGSERIAL PRIMARY KEY,
    tenant_id    BIGINT NOT NULL,
    name         VARCHAR(64) NOT NULL,
    description  VARCHAR(500),
    overlay      TEXT NOT NULL,               -- system prompt 后缀
    status       SMALLINT NOT NULL DEFAULT 1,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, name)
);
```

### 3.3 版本保留策略

`sf_agent_skill_version` 表需设置保留上限，防止无限增长：

- 每个 Skill 最多保留 **50 个版本**
- 超出时删除最旧版本（按 `created_at` ASC）
- 执行时锁定的版本（被 `SfAgentExecution` 引用的 `skill_version_id`）永不删除
- 定期清理任务：删除无引用且超过 90 天的版本

### 3.4 核心接口

```java
public interface SkillRegistry {
    /** 加载最新版本 Skill instructions */
    String resolve(String skillName, Long tenantId);

    /** 加载指定版本（执行时锁定） */
    String resolveVersion(String skillName, Long versionId, Long tenantId);
}

public interface RoleRegistry {
    /** 加载 Role overlay */
    String resolve(String roleName, Long tenantId);
}
```

**实现**: Caffeine 内存缓存 + DB，`sf_agent_skill_version` 表记录每次变更快照。

### 3.5 注入点

**前提**: `SfAgentExecution` 需新增 `skillName`（VARCHAR 64）和 `roleName`（VARCHAR 64）字段，可为 null。

在 `ThinkingStateHandler.buildPrompt()` 中注入，改动不超过 20 行：

```java
private String buildPrompt(List<LlmMessage> messages, SfAgentExecution execution) {
    StringBuilder sb = new StringBuilder();
    // 注入 Skill instructions（system message 前缀）
    if (execution.getSkillName() != null) {
        String skillInstructions = skillRegistry.resolve(execution.getSkillName(), execution.getTenantId());
        if (skillInstructions != null) {
            sb.append("system: ").append(skillInstructions).append("\n");
        }
    }
    // 注入 Role overlay（system message 后缀）
    if (execution.getRoleName() != null) {
        String roleOverlay = roleRegistry.resolve(execution.getRoleName(), execution.getTenantId());
        if (roleOverlay != null) {
            sb.append("system: ").append(roleOverlay).append("\n");
        }
    }
    // 原有 message 拼接
    for (LlmMessage msg : messages) {
        sb.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
    }
    return sb.toString();
}
```

### 3.6 安全措施

- **P0**: SkillLoader 沙箱化解析 — Markdown 解析在隔离线程，frontmatter 字段长度限制，禁止 HTML 标签
- **P1**: SemanticGuardrail — 语义级注入检测（规则引擎检测行为覆盖意图）
- **P2**: Skill 签名验证 — HMAC-SHA256，运行时验证完整性

### 3.7 数据流

```
UI Monaco Editor
  → POST /api/agent-config/skills (保存 sf_agent_skill + sf_agent_skill_version)
  → SkillRegistry 缓存失效/刷新
  → AgentExecutionEngine.startExecution() 读取 skillName/roleName
  → ThinkingStateHandler.buildPrompt() 注入 instructions + overlay
  → LLM 调用
```

---

## 4. Design Section 2: Task / Sub-agent

### 4.1 目标

支持 Agent 动态分发子任务，从单轮问答升级为多 Agent 协作。

### 4.2 方案选择: Tool 内置化（非状态机扩展）

**决策**: 将 `session.task()` 实现为 `TaskToolAdapter` 注册到 `ToolRegistry`，复用 `ToolCallingStateHandler` 的 loop detection、safety guard、retry 路径。

**理由**: 不改 11-state 状态机，不新增 DELEGATING/AWAITING_SUB 状态，最小化架构冲击。

### 4.3 核心实现

```java
@Component
public class TaskToolAdapter implements ToolAdapter {

    private final SubAgentExecutionService subAgentService;
    private final SubAgentQuotaService quotaService;

    @Override
    public String getToolName() { return "task"; }

    @Override
    public ToolResult execute(ToolCall toolCall, ExecutionContext ctx) {
        String prompt = toolCall.parameters().get("prompt").toString();
        String role = toolCall.parameters().getOrDefault("role", "default").toString();

        // 配额检查（前置，快速失败）
        quotaService.checkAndIncrement(ctx.executionId());

        try {
            SubAgentRequest request = SubAgentRequest.builder()
                .parentExecutionId(ctx.executionId())
                .prompt(prompt)
                .role(role)
                .workspaceRoot(ctx.workspaceRoot())    // 共享 sandbox（同层协作）
                .inheritedGuardrails(ctx.guardrails())  // P1: Guardrails 传播
                .maxDepth(getRemainingDepth(ctx))
                .build();

            SubAgentResult result = subAgentService.execute(request);
            return ToolResult.success(result.output());
        } catch (SubAgentQuotaExceededException e) {
            return ToolResult.error("Sub-agent quota exceeded: " + e.getMessage());
        } catch (SubAgentExecutionException e) {
            return ToolResult.error("Sub-agent failed: " + e.getMessage());
        } finally {
            quotaService.decrement(ctx.executionId());
        }
    }
}
```

**注意**: `ExecutionContext` 需新增 `guardrails()` 方法返回当前 Guardrails 配置。

### 4.4 配额模型（澄清）

配额为**直接子 Agent 数**限制，非递归总计：

| 维度 | 键 | 阈值 | 说明 |
|------|-----|------|------|
| 父级配额 | `sf:subagent:count:{parentExecutionId}` | 16 | 单个父 Agent 的直接子 Agent 数 |
| 租户级配额 | `sf:subagent:tenant:{tenantId}` | 可配置（默认 64） | 租户全局并发子 Agent 数 |
| 深度限制 | execution metadata `depth` | 4 | 递归深度硬上限 |

- 父级配额防止单 Agent 横向爆炸
- 租户级配额防止多 Agent 协同耗尽资源
- 深度限制防止纵向递归

### 4.5 四层安全防护

| 层级 | 措施 | 实施点 |
|------|------|--------|
| L1 | 父级 + 租户级子 Agent 配额 | Redis 原子计数器 |
| L1 | Guardrails 传播链 | 子 Agent 继承父配置 + 额外限制 |
| L1 | 子 Agent 独立沙箱 | `SandboxProvider.scope()` 隔离 |
| L1 | ancestryChain 防绕 | execution metadata 传递父 ID 列表 |

### 4.6 关键约束

- 子 Agent 独立 `conversationId` + 独立 `SandboxSession`（通过 `scope()` 创建）
- `workspaceRoot` 仅在父子需要协作编辑同一目录时共享（需显式传入，非默认行为）
- TokenBudget 父子成本汇总到 ClickHouse 追踪
- 子任务失败复用现有 RETRYING/FAILED 路径
- **不引入 LangGraph Subgraph**（预定义工作流用 Flowable BPMN）

---

## 5. Design Section 3: Session 持久化

### 5.1 目标

补齐 Agent 级沙箱状态持久化，支持暂停/恢复。

### 5.2 方案选择: 不引入 LangGraph checkpointing

**决策**: 仅实现 Agent 级沙箱状态持久化（文件系统快照），不引入 LangGraph 的 reducer-driven checkpointing / time travel。

**理由**:
- 现有 PAUSED/RESUMING 状态已满足暂停恢复需求
- Time travel 企业价值存疑
- 与 Flowable 7 BPMN 概念重叠

### 5.3 实现

```java
public class AgentSessionPersistence {

    /** 暂停时快照沙箱状态 */
    public void persistSession(SfAgentExecution execution, SandboxSession sandbox) {
        Path snapshotDir = snapshotRoot.resolve(execution.getExecutionId());
        Files.createDirectories(snapshotDir);
        // 复制 workspace 文件到快照目录
        copyDirectory(sandbox.workspaceRoot(), snapshotDir.resolve("workspace"));
        // 保存 execution metadata
        saveMetadata(execution, snapshotDir.resolve("metadata.json"));
    }

    /** 恢复时还原沙箱状态 */
    public void restoreSession(SfAgentExecution execution, SandboxSession sandbox) {
        Path snapshotDir = snapshotRoot.resolve(execution.getExecutionId());
        if (Files.exists(snapshotDir)) {
            copyDirectory(snapshotDir.resolve("workspace"), sandbox.workspaceRoot());
        }
    }
}
```

---

## 6. Design Section 4: Compaction 自动触发

### 6.1 目标

自动管理上下文窗口，防止 token 超限导致执行失败。

### 6.2 方案: 四层递进策略链

参考 Claude Code 的 7 种策略 × 3 层架构，SchemaPlexAI 设计 4 层递进系统：

```
Layer 0: ToolResultCompactionStrategy（零成本，请求前清理）
  ↓ 仍超限
Layer 1: SlidingWindowCompactionStrategy（裁剪旧消息）
  ↓ 仍超限
Layer 2: SummarizationCompactionStrategy（LLM 摘要 + post-compact 恢复）
  ↓ 摘要本身超限（PTL）
Layer 3: PTL Retry（丢弃最旧消息组，重试 Layer 2）
  ↓ 所有策略失败
GATE_BLOCKED
```

### 6.3 Token 估算

复用现有 `TokenEstimator.estimate(String)`（char/4 启发式），新增 `List<LlmMessage>` 重载：

```java
public static long estimate(List<LlmMessage> messages) {
    return messages.stream().mapToLong(m -> estimate(m.getContent())).sum();
}
```

**已知局限**: char/4 对 CJK 文本偏低（实际约 char/1.5），后续可替换为 tiktoken-jni。

### 6.4 Layer 0: Tool Result 清理

**参考**: Claude Code 的 `apiMicrocompact.ts` — 在发送给 LLM 前清理旧的 tool results。

```java
public class ToolResultCompactionStrategy implements CompactionStrategy {

    private static final Set<String> CLEARABLE_TOOLS = Set.of(
        "file_read", "shell_exec", "web_search", "web_fetch", "grep"
    );
    private static final long MAX_INPUT_TOKENS = 120_000;
    private static final int KEEP_RECENT = 3;

    @Override
    public CompactionResult compact(String conversationId, List<LlmMessage> messages, TokenBudget budget) {
        long currentTokens = TokenEstimator.estimate(messages);
        if (currentTokens <= MAX_INPUT_TOKENS) return CompactionResult.noOp();

        List<LlmMessage> compacted = clearOldToolResults(messages, KEEP_RECENT);
        return CompactionResult.success(compacted, "tool_result_cleanup");
    }
}
```

**特点**: 请求发送前执行，不触发 LLM 调用，零成本。

### 6.5 Layer 1: SlidingWindow（保留现有设计）

裁剪超出窗口的旧消息，保留 system message + 最近 N 条。

### 6.6 Layer 2: Summarization + Post-compact 恢复

**参考**: Claude Code 的 `compactConversation()` — 摘要后重新注入关键上下文。

```java
public class SummarizationCompactionStrategy implements CompactionStrategy {

    private static final int MAX_FILES_TO_RESTORE = 5;
    private static final long FILE_TOKEN_BUDGET = 50_000;
    private static final long TOKENS_PER_SKILL = 5_000;
    private static final long SKILL_TOKEN_BUDGET = 25_000;

    @Override
    public CompactionResult compact(String conversationId, List<LlmMessage> messages, TokenBudget budget) {
        // 摘要前 PII 扫描（安全要求）
        messages = piiRedactor.redact(messages);

        String summary = generateSummary(messages);  // 独立 LLM 调用

        // Post-compact 恢复
        List<LlmMessage> restored = new ArrayList<>();
        restored.add(LlmMessage.system(summary));
        restored.addAll(recentFileContext(conversationId, MAX_FILES_TO_RESTORE, FILE_TOKEN_BUDGET));
        restored.addAll(activeSkillContext(conversationId, TOKENS_PER_SKILL, SKILL_TOKEN_BUDGET));

        return CompactionResult.success(restored, "summarization_with_restoration");
    }
}
```

**Post-compact 恢复策略**（参考 Claude Code）:

| 恢复项 | SchemaPlexAI 等价物 | Token 预算 |
|--------|-------------------|-----------|
| 最近访问的文件 | SandboxSession 最近 read 的文件 | 50K, 最多 5 个 |
| Plan 文件 | SubTaskPlan JSON | 不限 |
| 已调用的 Skill | SkillRegistry 加载的 instructions | 5K/skill, 总计 25K |

### 6.7 Layer 3: PTL Retry

**参考**: Claude Code 的 `truncateHeadForPTLRetry()` — 摘要请求本身也超限时，丢弃最旧消息组重试。

```java
@Component
public class AutoCompactionService {

    private static final int MAX_PTL_RETRIES = 3;

    public CompactionResult compactIfNeeded(String conversationId, TokenBudget budget) {
        List<LlmMessage> messages = chatMemoryStore.loadMessages(conversationId);

        // 快速路径：未超限直接返回，避免进入策略链
        long currentTokens = TokenEstimator.estimate(messages);
        if (currentTokens <= budget.remainingInput()) {
            return CompactionResult.noOp();
        }

        // Layer 0: Tool result 清理（零成本）
        CompactionResult l0 = toolResultStrategy.compact(conversationId, messages, budget);
        if (l0.success()) messages = l0.messages();

        if (TokenEstimator.estimate(messages) <= budget.remainingInput()) {
            chatMemoryStore.replaceMessages(conversationId, messages);
            return l0;
        }

        // Layer 1: SlidingWindow
        CompactionResult l1 = slidingWindowStrategy.compact(conversationId, messages, budget);
        if (l1.success()) messages = l1.messages();

        if (TokenEstimator.estimate(messages) <= budget.remainingInput()) {
            chatMemoryStore.replaceMessages(conversationId, messages);
            return l1;
        }

        // Layer 2: Summarization（带 PTL retry）
        for (int retry = 0; retry < MAX_PTL_RETRIES; retry++) {
            try {
                CompactionResult l2 = summarizationStrategy.compact(conversationId, messages, budget);
                chatMemoryStore.replaceMessages(conversationId, l2.messages());
                return l2;
            } catch (PromptTooLongException e) {
                messages = truncateHead(messages, 0.25);  // 丢弃最旧 25%
            }
        }

        return CompactionResult.failed("All compaction strategies exhausted");
    }
}
```

**注意**: `CompositeChatMemoryStore` 需新增 `replaceMessages(String conversationId, List<LlmMessage> messages)` 方法。

### 6.8 安全措施

- **Compaction 前 PII 扫描**: 摘要生成前对历史消息运行 `PiiRedactor`
- **摘要后 Guardrails 校验**: 生成的摘要必须通过 `GuardrailsEngine.validateOutput()`
- **Compaction 审计日志**: 记录触发原因、原始 token 数、压缩后 token 数

### 6.9 与 Claude Code 的差异

| 方面 | Claude Code | SchemaPlexAI |
|------|------------|--------------|
| Snip Compact | 删除整条旧消息 | 合并到 Layer 1 |
| Session Memory | 后台持续提取到磁盘 | 暂不引入（无 forked agent） |
| Context Collapse | 细粒度 span 折叠 | 暂不引入（需要 feature gate 架构） |
| Reactive Compact | 413 错误后触发 | 暂不引入（GATE_BLOCKED 替代） |
| Cache Sharing | Forked agent 共享 prompt cache | 不适用（Java 侧） |
| 触发方式 | 用户命令 + auto-compact | Token budget 超限自动触发 |

---

## 7. Design Section 5: MCP 集成

### 7.1 目标

接入 MCP 工具生态，让 Agent 能调用外部 MCP 服务器提供的工具。

### 7.2 架构

```
ToolCallingStateHandler
  → ToolRegistry.resolve("mcp:github:search_repos")
    → McpToolAdapter
      → McpClientManager
        → Spring AI MCP Client (stdio / SSE)
          → External MCP Server
```

### 7.3 核心接口

**McpToolAdapter** — 桥接 MCP 到 ToolAdapter:

```java
@Component
public class McpToolAdapter implements ToolAdapter {

    private final McpClientManager clientManager;
    private final McpServerRegistry serverRegistry;

    @Override
    public ToolResult execute(ToolCall call, ExecutionContext ctx) {
        McpToolRef ref = McpToolRef.parse(call.toolName());  // "mcp:<serverId>:<toolName>"
        serverRegistry.requireApproved(ref.serverId());       // 白名单检查

        try {
            McpSchema.CallToolResult result = clientManager
                    .getClient(ref.serverId())
                    .callTool(ref.toolName(), call.parameters());
            return ToolResult.success(serializeContent(result.content()));
        } catch (McpServerNotFoundException e) {
            return ToolResult.error("MCP server unavailable: " + ref.serverId());
        } catch (McpToolExecutionException e) {
            return ToolResult.error("MCP tool failed: " + e.getMessage());
        }
    }
}
```

**McpToolDiscoveryService** — 并行同步工具定义（每服务器独立错误隔离）:

```java
@Component
public class McpToolDiscoveryService {

    private final ExecutorService discoveryExecutor = Executors.newFixedThreadPool(4);

    @Scheduled(fixedDelayString = "${mcp.discovery.interval:60000}")
    public void syncTools() {
        List<McpServerConfig> servers = serverRegistry.listApproved();
        List<CompletableFuture<Void>> futures = servers.stream()
            .map(server -> CompletableFuture.runAsync(
                () -> syncServerTools(server), discoveryExecutor)
                .exceptionally(ex -> {
                    log.warn("Failed to sync MCP server {}: {}", server.serverId(), ex.getMessage());
                    return null;  // 单服务器失败不影响其他
                }))
            .toList();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    private void syncServerTools(McpServerConfig server) {
        List<McpSchema.Tool> remoteTools = clientManager.getClient(server.serverId()).listTools();
        for (McpSchema.Tool tool : remoteTools) {
            String qualifiedName = "mcp:" + server.serverId() + ":" + tool.name();
            if (!guardrails.validateInput(tool.description()).success()) continue;
            toolRegistry.registerDynamic(buildDefinition(qualifiedName, tool), mcpAdapter);
        }
    }
}
```

**McpClientManager** — 连接池限制（防连接泄漏）:

```java
@Component
public class McpClientManager {
    // Caffeine 缓存限制并发连接数
    private final Cache<String, McpClient> clients = Caffeine.newBuilder()
        .maximumSize(32)                    // 最多 32 个并发 MCP 连接
        .expireAfterAccess(Duration.ofMinutes(10))
        .removalListener((key, client, cause) -> {
            if (client != null) client.close();
        })
        .build();

    public McpClient getClient(String serverId) {
        return clients.get(serverId, this::createClient);
    }
}
```

**McpServerRegistry** — 白名单机制:

```java
@Component
public class McpServerRegistry {
    public void requireApproved(String serverId) {
        SfMcpServer server = mcpServerMapper.selectById(serverId);
        if (server == null || server.getStatus() != 1) {
            throw new SecurityException("MCP server not approved: " + serverId);
        }
    }
}
```

### 7.4 Entity 扩展

```java
@TableName("sf_mcp_server")
public class SfMcpServer extends BaseEntity {
    private String name;
    private String endpoint;          // "stdio://..." or "sse://..."
    private String transport;         // "stdio" | "sse"
    private Integer status;           // 0=pending, 1=approved, 2=blocked
    private String command;           // stdio: command to spawn
    private String args;              // stdio: JSON array
    private String envVars;           // JSON map (encrypted)
    private String serverPublicKey;   // identity fingerprint
    private Integer protocolVersion;
    private String toolWhitelist;     // JSON array (null=all)
}
```

### 7.5 安全措施

| 威胁 | 缓解 | 优先级 |
|------|------|--------|
| 恶意 MCP 服务器 | 白名单（McpServerRegistry） | P1 |
| 工具描述注入 | GuardrailsEngine.validateInput() | P1 |
| MCP 服务器网络访问 | 容器 `--network none` | P1 |
| 凭证泄露 | envVars AES-256-GCM 加密 | P1 |
| 供应链攻击 | SBOM 扫描 + 公钥指纹 | P2 |

### 7.6 关键决策

| 决策 | 选择 | 理由 |
|------|------|------|
| 传输协议 | stdio + SSE | Spring AI MCP 两者都支持 |
| 命名空间 | `mcp:<serverId>:<toolName>` | 避免与内部 tool 冲突 |
| 发现机制 | 定时轮询 60s | 简单可靠，事件驱动作为 Phase 2 |
| 主客户端 | Spring AI MCP Boot Starters | Java 生态最成熟 |
| 备用客户端 | 手写 MCP 客户端 | 防 Spring AI 版本锁定 |

---

## 8. Design Section 6: Sandbox 抽象增强

### 8.1 目标

增强沙箱隔离能力，引入容器级沙箱作为生产默认选项。

### 8.2 方案选择: 渐进增强（不重构接口）

**决策**: 保留现有 `SandboxProvider` + `SandboxSession` 接口，新增 `ContainerSandboxProvider` 实现。

**理由**: 重构接口影响所有 ToolAdapter 实现，breaking change 成本高。

**接口扩展**: `SandboxProvider` 需新增 `scope()` 方法用于子 Agent 沙箱隔离：

```java
public interface SandboxProvider {
    SandboxSession create(SandboxSessionConfig config);

    /** 创建隔离子沙箱（子 Agent 使用），共享父沙箱的文件系统但独立进程 */
    default SandboxSession scope(SandboxSession parent, SandboxSessionConfig config) {
        return create(config);  // 默认实现：创建独立沙箱
    }

    String providerId();
}
```

### 8.3 架构

```
ToolAdapter (FileRead / ShellCommand / HttpCall)
  → SandboxSession.exec() / readFile()
    → SandboxProvider.create(config)
      ├── LocalProcessSandbox (existing, dev default)
      └── ContainerSandboxProvider (NEW, prod default)
            → Docker Java API
              --network none
              --read-only
              --security-opt seccomp=restricted
```

### 8.4 ContainerSandboxProvider

```java
@Component("containerSandbox")
@ConditionalOnProperty(name = "sandbox.provider", havingValue = "container")
public class ContainerSandboxProvider implements SandboxProvider {

    private final DockerClient dockerClient;

    @Override
    public SandboxSession create(SandboxSessionConfig config) {
        String sessionId = UUID.randomUUID().toString();
        try {
            CreateContainerCmd cmd = dockerClient.createContainerCmd("sandbox-base:latest")
                    .withName("splx-sbx-" + sessionId)
                    .withNetworkDisabled(true)
                    .withReadonlyRootfs(true)
                    .withHostConfig(HostConfig.newHostConfig()
                            .withNetworkMode("none")
                            .withReadonlyRootfs(true)
                            .withSecurityOpts(List.of("seccomp=restricted"))
                            .withMemory(config.memoryLimitMb() * 1024 * 1024)
                            .withCpuPeriod(100_000)
                            .withCpuQuota(config.cpuLimitMillis() * 100)
                            .withBinds(volumeBind(sessionId)));
            ContainerCreation creation = cmd.exec();
            dockerClient.startContainerCmd(creation.getId()).exec();
            return new ContainerSandboxSession(sessionId, creation.getId(), config, dockerClient);
        } catch (Exception e) {
            // 容器创建失败时清理残留
            cleanupOnFailure(sessionId);
            throw new SandboxCreationException("Failed to create container sandbox: " + e.getMessage(), e);
        }
    }

    private void cleanupOnFailure(String sessionId) {
        try {
            dockerClient.removeContainerCmd("splx-sbx-" + sessionId)
                .withForce(true).exec();
        } catch (Exception ignored) {}
    }
}
```

### 8.5 环境变量清理（P0）

```java
// ContainerSandboxSession 和 LocalProcessSandboxSession 共用
private String[] sanitizeEnv(Map<String, String> env) {
    if (env == null) return new String[0];
    return env.entrySet().stream()
            .filter(e -> !isSensitiveKey(e.getKey()))
            .map(e -> e.getKey() + "=" + e.getValue())
            .toArray(String[]::new);
}

private boolean isSensitiveKey(String key) {
    String upper = key.toUpperCase();
    return upper.contains("PASSWORD") || upper.contains("SECRET")
            || upper.contains("TOKEN") || upper.contains("CREDENTIAL")
            || upper.contains("_KEY") || upper.contains("PRIVATE");
}
```

### 8.6 Provider 选择

```yaml
# application.yml
sandbox:
  provider: local    # "local" | "container"
```

支持租户级覆盖：`SandboxProviderResolver` 从 DB 读取租户配置选择 provider。

### 8.7 关键决策

| 决策 | 选择 | 理由 |
|------|------|------|
| 重构为 SessionEnv？ | 否 | 现有接口足够，重构成本高 |
| 容器运行时 | Docker (docker-java) | 最广泛部署，兼容 Podman |
| 默认隔离 | `--network none` + `--read-only` + seccomp | 安全专家推荐最强默认 |
| 环境变量清理 | 关键词过滤 | PASSWORD/SECRET/TOKEN/KEY/CREDENTIAL |
| 网络隔离 | 硬隔离（容器级）vs 软隔离（LocalProcess） | 生产环境硬隔离 |

### 8.8 迁移路径

```
Phase 1 (now):  LocalProcessSandbox + sanitizeEnv() P0 fix
Phase 2 (L2):   ContainerSandboxProvider as opt-in
Phase 3 (future): ContainerSandboxProvider as default
Phase 4 (future): E2B/Daytona providers via same SandboxProvider interface
```

---

## 9. 数据库迁移与 Feature Flag 策略

### 9.1 数据库迁移

所有 Schema 变更通过 Flyway 迁移脚本管理：

| 迁移脚本 | 内容 | 依赖 |
|---------|------|------|
| `V2026_05_01__add_skill_role_tables.sql` | sf_agent_skill, sf_agent_skill_version, sf_agent_role | 无 |
| `V2026_05_02__add_execution_skill_role_columns.sql` | SfAgentExecution 新增 skill_name, role_name | 上一步 |
| `V2026_05_03__extend_mcp_server.sql` | SfMcpServer 新增 command, args, envVars, serverPublicKey, protocolVersion, toolWhitelist | 无 |

迁移脚本必须：
- 向后兼容（新列可为 null 或有默认值）
- 可回滚（每个 up 迁移配对 down 迁移）
- 不锁表（大表用 `ALTER TABLE ... ADD COLUMN ... DEFAULT ...` 而非 `ALTER TABLE ... ADD COLUMN NOT NULL`）

### 9.2 Feature Flag 策略

各维度通过 Feature Flag 独立开关，支持灰度发布：

```yaml
features:
  skill-role-enabled: false          # Phase 1 开启
  compaction-auto-enabled: false     # Phase 2 开启
  mcp-integration-enabled: false     # Phase 2 开启
  sub-agent-enabled: false           # Phase 3 开启（依赖 P0/P1 安全项）
  container-sandbox-enabled: false   # Phase 4 开启
```

实现方式：`@ConditionalOnProperty` + 配置中心动态刷新。

**子 Agent 特殊要求**: `sub-agent-enabled` 仅在所有 P0/P1 安全前置条件完成后才允许开启。通过 `SubAgentPreconditionChecker` 在启动时校验。

---

## 10. 实施路线图

### Phase 0: 安全基线（Week 9，1 周）

- SkillLoader 沙箱化解析 + 字段长度限制
- ChatMemory AES-256-GCM 加密
- LocalProcessSandbox 环境变量清理
- 测试: SkillLoaderSecurityTest, ChatMemoryEncryptionTest, SandboxEnvCleanupTest

### Phase 1: Skill + Role（Week 10-11，2 周）

- 引入 flexmark-java 解析 Markdown + YAML frontmatter
- 新建 sf_agent_skill、sf_agent_role、sf_agent_skill_version 表
- 实现 SkillRegistry、RoleRegistry（Caffeine 内存缓存 + DB）
- 修改 ThinkingStateHandler.buildPrompt() 注入 Skill/Role
- UI: Monaco Editor + Markdown preview
- 测试: SkillRegistryTest, RoleRegistryTest, ThinkingStateHandlerTest

### Phase 2: Compaction + MCP（Week 12-14，3 周）

**Week 12: Compaction**
- 实现 ToolResultCompactionStrategy（Layer 0）
- 实现 AutoCompactionService（策略链: ToolResult → SlidingWindow → Summarization）
- PTL Retry 机制
- Post-compact 恢复（文件 + Skill 上下文）
- 测试: AutoCompactionServiceTest, CompactionPiiTest

**Week 13-14: MCP**
- 引入 spring-ai-mcp-client-spring-boot-starter
- 实现 McpToolAdapter、McpClientManager、McpToolDiscoveryService
- McpServerRegistry 白名单
- MCP 工具描述 Guardrails 校验
- 测试: McpToolAdapterTest, McpWhitelistTest, McpDiscoveryTest

### Phase 3: Task / Sub-agent（Week 15-16，2 周）

- 实现 TaskToolAdapter（内置 tool）
- SubAgentExecutionService（独立 conversationId + 独立 SandboxSession）
- 全局子 Agent 配额（Redis 计数器，阈值 16）
- Guardrails 传播链
- 子 Agent 独立沙箱隔离
- ancestryChain 跨系统防绕
- 测试: TaskToolAdapterTest, SubAgentQuotaTest, GuardrailsInheritanceTest

### Phase 4: Session + Sandbox（Week 17+，评估后启动）

- Agent 级沙箱状态持久化（文件系统快照）
- ContainerSandboxProvider 作为生产默认
- 租户级 SandboxProviderResolver
- 测试: SessionPersistenceTest, ContainerSandboxTest

---

## 11. 关键架构决策（ADR）

### ADR-1: Sub-agent 采用 Tool 内置化方案

**决策**: `TaskToolAdapter` 注册到 `ToolRegistry`，不新增状态。

**理由**: 复用 ToolCallingStateHandler 的 loop detection、safety guard、retry 路径，最小化架构冲击。

**反对意见**: AI工程认为新增 DELEGATING 状态更干净 → 被否决，架构师的风险评估权重更高。

### ADR-2: 不引入 LangGraph checkpointing / time travel

**决策**: Session 持久化仅补齐 Agent 级沙箱状态（文件系统快照）。

**理由**: 现有 PAUSED/RESUMING 足够；time travel 企业价值存疑；与 Flowable 概念重叠。

### ADR-3: Sandbox 渐进增强，不重构为 SessionEnv

**决策**: 新增 `ContainerSandboxProvider`，不引入 Flue 的 `SessionEnv` 统一接口。

**理由**: 重构接口影响所有 ToolAdapter，breaking change 成本高；Virtual Sandbox 在 Java 生态无成熟库。

### ADR-4: MCP 优先 Spring AI，保留手写 fallback

**决策**: MCP 客户端优先 `spring-ai-mcp-client-spring-boot-starter`，保留手写路径。

**理由**: Spring AI 封装度高但可能不支持 streaming tool result；MCP 协议仍在演进。

### ADR-5: Compaction 四层递进策略链

**决策**: ToolResult 清理 → SlidingWindow → Summarization + Post-compact 恢复 → PTL Retry。

**理由**: 参考 Claude Code 的 7 策略架构，零成本层优先，LLM 摘要作为最后手段。

---

## 12. 风险预警

| 风险 | 来源 | 影响 | 缓解措施 |
|------|------|------|---------|
| Sub-agent 递归攻击 | 安全 | 级联权限扩散 | P0 全局配额 + Guardrails 传播 + 独立沙箱 |
| Skill Markdown 注入 | 安全 | 提示注入覆盖系统行为 | P0 沙箱化解析 + P1 SemanticGuardrail |
| MCP 供应链攻击 | 安全 | 恶意工具、数据外泄 | P1 白名单 + 网络隔离 + SBOM 校验 |
| Spring AI MCP 版本不兼容 | 架构 | 升级阻塞 | 保留手写 fallback 路径 |
| 子 Agent 成本黑洞 | 产品 | 运营成本失控 | TokenBudget 父子成本汇总 + ClickHouse 追踪 |
| Skill/Role 版本漂移 | 架构 | 行为不一致 | sf_agent_skill_version 快照表 + 运行时锁定 |
| 状态机复杂度失控 | 架构 | Layer 2 延期 | Tool 内置化方案，不新增状态 |

---

## 13. 测试覆盖要求

| 维度 | 测试类 | 覆盖率要求 |
|------|--------|-----------|
| Skill/Role | SkillRegistryTest, RoleRegistryTest, RoleOverlayTest, ThinkingStateHandlerTest | ≥ 80% |
| Task/Sub-agent | TaskToolAdapterTest, SubAgentQuotaTest, GuardrailsInheritanceTest, DepthLimitTest | ≥ 80% |
| Compaction | AutoCompactionServiceTest, ToolResultCompactionTest, CompactionPiiTest | ≥ 80% |
| MCP | McpToolAdapterTest, McpWhitelistTest, McpDiscoveryTest, McpDescriptionGuardrailsTest | ≥ 80% |
| Session | SessionPersistenceTest | ≥ 80% |
| Sandbox | ContainerSandboxTest, SandboxEnvCleanupTest | ≥ 80% |

---

## 14. 附录

### 参考文献

- Flue 调研报告: `.claude/outputs/flue-research-report.md`
- Sim Studio AI 调研报告: `.claude/outputs/sim-research-report.md`
- 圆桌辩论终版报告: `.claude/outputs/flue-roundtable-report.md`
- 安全专家报告: `.claude/outputs/roundtable-security-pov.md`
- 架构师报告: `.claude/outputs/roundtable-architecture-pov.md`
- Claude Code 源码: `D:\Downloads\Claude-Code-main-main\`

### 关键术语

| 术语 | 定义 |
|------|------|
| Skill | Markdown 驱动的 Agent 能力模板（instructions） |
| Role | system prompt overlay（人格/风格定义） |
| MCP | Model Context Protocol，外部工具接入协议 |
| Compaction | 上下文压缩，防止 token 超限 |
| PTL | Prompt Too Long，摘要请求本身也超限 |
| Guardrails | 安全防护引擎（输入/输出校验） |
| Sandbox | 沙箱隔离执行环境 |
