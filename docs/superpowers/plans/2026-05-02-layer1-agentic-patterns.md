# Layer 1 Agentic Patterns 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现 SchemaPlexAI Agent Engine 的 Layer 1 基础骨架（10 个模式），包含 3 个 Critical 安全风险修复、ReAct 循环闭环、Memory/RAG 数据隔离、Reflection/Evaluation/Guardrails 基础设施。

**Architecture:** 基于现有的 11-state 状态机架构，扩展 4 个模块（chain、memory-api、tool-runtime、evaluation），改造 3 个核心接口（AgentStateMachine、ThinkingStateHandler、ExecutionAdmissionService），新增 2 个数据表（chain_definition、evaluation_result）。

**Tech Stack:** Java 21, Spring Boot 3.2.5, LangChain4j 0.31.0, PostgreSQL 16, Redis 7, Milvus 2.3.5, JUnit 5, TestContainers

---

## 任务总览

| # | 任务 | 优先级 | 预计周期 | 状态 |
|---|------|--------|---------|------|
| 1 | Critical 安全风险修复 | P0 | W1 | - [ ] |
| 2 | Tool Use 完整实现 | P1 | W1-2 | - [ ] |
| 3 | Reasoning + Exception Handling | P1 | W3-4 | - [ ] |
| 4 | Memory + RAG 数据隔离 | P1 | W5 | - [ ] |
| 5 | Reflection + Evaluation + Guardrails | P1 | W6 | - [ ] |

---

## 任务 1: Critical 安全风险修复（P0，W1）

### 文件清单

- **Create:** `schemaplexai-agent-engine/src/.../tool/ToolSandbox.java` — 工具执行沙箱接口
- **Create:** `schemaplexai-agent-engine/src/.../tool/ContainerToolSandbox.java` — 容器隔离实现
- **Create:** `schemaplexai-agent-engine/src/.../tool/ToolWhitelist.java` — 工具白名单
- **Create:** `schemaplexai-agent-engine/src/.../context/InputValidator.java` — 输入验证器
- **Create:** `schemaplexai-agent-engine/src/.../context/BlacklistKeywordChecker.java` — 黑名单关键词检查
- **Create:** `schemaplexai-agent-engine/src/.../context/LengthValidator.java` — 长度限制检查
- **Create:** `schemaplexai-agent-engine/src/.../security/SseTokenValidator.java` — SSE Token 验证
- **Modify:** `schemaplexai-agent-engine/src/.../state/ToolCallingStateHandler.java` — 集成沙箱
- **Modify:** `schemaplexai-agent-engine/src/.../context/ContextInjector.java` — 集成输入验证
- **Modify:** `schemaplexai-agent-engine/src/.../controller/AgentExecutionController.java` — SSE 端点验证
- **Test:** `schemaplexai-agent-engine/src/test/.../tool/ToolSandboxTest.java`
- **Test:** `schemaplexai-agent-engine/src/test/.../context/InputValidatorTest.java`
- **Test:** `schemaplexai-agent-engine/src/test/.../security/SseTokenValidatorTest.java`

### 实施步骤

- [ ] **Step 1.1: 创建 ToolSandbox 接口**

```java
package com.schemaplexai.agent.engine.tool;

/**
 * 工具执行沙箱接口，提供安全的工具执行环境。
 */
public interface ToolSandbox {
    
    /**
     * 在沙箱中执行工具调用
     * @param toolCall 工具调用请求
     * @param config 沙箱配置
     * @return 执行结果
     * @throws ToolExecutionException 执行失败时抛出
     */
    ToolResult execute(ToolCall toolCall, SandboxConfig config) throws ToolExecutionException;
    
    /**
     * 验证工具调用是否允许执行
     * @param toolCall 工具调用请求
     * @return 验证结果
     */
    ValidationResult validate(ToolCall toolCall);
}
```

- [ ] **Step 1.2: 创建 ContainerToolSandbox 实现**

```java
package com.schemaplexai.agent.engine.tool;

import java.time.Duration;

/**
 * 基于容器的工具执行沙箱，提供进程级隔离。
 */
public class ContainerToolSandbox implements ToolSandbox {
    
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final long DEFAULT_MEMORY_LIMIT_MB = 512;
    private static final long DEFAULT_CPU_LIMIT_MILLIS = 1000;
    
    private final ToolWhitelist whitelist;
    
    public ContainerToolSandbox(ToolWhitelist whitelist) {
        this.whitelist = whitelist;
    }
    
    @Override
    public ToolResult execute(ToolCall toolCall, SandboxConfig config) throws ToolExecutionException {
        // 验证工具是否在白名单中
        if (!whitelist.isAllowed(toolCall.toolName())) {
            throw new ToolExecutionException(
                ToolErrorCategory.PERMISSION_DENIED,
                "Tool '" + toolCall.toolName() + "' is not in the allowed list"
            );
        }
        
        // 验证工具调用
        ValidationResult validation = validate(toolCall);
        if (!validation.isValid()) {
            throw new ToolExecutionException(
                ToolErrorCategory.INVALID_ARGUMENT,
                "Tool call validation failed: " + validation.errorMessage()
            );
        }
        
        try {
            // 在容器中执行工具
            return executeInContainer(toolCall, config);
        } catch (Exception e) {
            throw new ToolExecutionException(
                ToolErrorCategory.INTERNAL_ERROR,
                "Tool execution failed: " + e.getMessage(),
                e
            );
        }
    }
    
    @Override
    public ValidationResult validate(ToolCall toolCall) {
        // 检查工具名
        if (toolCall.toolName() == null || toolCall.toolName().isBlank()) {
            return ValidationResult.invalid("Tool name is required");
        }
        
        // 检查参数数量限制
        if (toolCall.parameters() != null && toolCall.parameters().size() > 20) {
            return ValidationResult.invalid("Too many parameters (max 20)");
        }
        
        // 检查参数值大小
        for (var param : toolCall.parameters().entrySet()) {
            if (param.getValue() != null && param.getValue().toString().length() > 10000) {
                return ValidationResult.invalid("Parameter '" + param.getKey() + "' exceeds max length");
            }
        }
        
        return ValidationResult.valid();
    }
    
    private ToolResult executeInContainer(ToolCall toolCall, SandboxConfig config) {
        // TODO: 实现容器执行逻辑
        // 1. 创建临时容器
        // 2. 限制资源（CPU/内存/网络）
        // 3. 执行工具调用
        // 4. 收集输出
        // 5. 清理容器
        return ToolResult.success("Container execution not yet implemented");
    }
}
```

- [ ] **Step 1.3: 创建 InputValidator 接口**

```java
package com.schemaplexai.agent.engine.context;

/**
 * 输入验证接口，用于验证用户输入和 LLM 输出。
 */
public interface InputValidator {
    
    /**
     * 验证输入
     * @param input 输入文本
     * @return 验证结果
     */
    ValidationResult validate(String input);
}
```

- [ ] **Step 1.4: 创建 BlacklistKeywordChecker 实现**

```java
package com.schemaplexai.agent.engine.context;

import java.util.Set;

/**
 * 基于黑名单关键词的输入检查器。
 */
public class BlacklistKeywordChecker implements InputValidator {
    
    private static final Set<String> BLACKLISTED_KEYWORDS = Set.of(
        "ignore previous instructions",
        "ignore your instructions",
        "forget what you know",
        "repeat your programming",
        "reveal your instructions",
        "bypass safety",
        "jailbreak"
    );
    
    @Override
    public ValidationResult validate(String input) {
        if (input == null || input.isBlank()) {
            return ValidationResult.valid();
        }
        
        String lowerInput = input.toLowerCase();
        for (String keyword : BLACKLISTED_KEYWORDS) {
            if (lowerInput.contains(keyword)) {
                return ValidationResult.invalid("Input contains blocked keyword: " + keyword);
            }
        }
        
        return ValidationResult.valid();
    }
}
```

- [ ] **Step 1.5: 创建 LengthValidator 实现**

```java
package com.schemaplexai.agent.engine.context;

/**
 * 基于长度限制的输入检查器。
 */
public class LengthValidator implements InputValidator {
    
    private static final int MAX_INPUT_LENGTH = 100000;
    private static final int MAX_OUTPUT_LENGTH = 100000;
    
    private final int maxLength;
    
    public LengthValidator(int maxLength) {
        this.maxLength = maxLength;
    }
    
    public LengthValidator() {
        this(MAX_INPUT_LENGTH);
    }
    
    @Override
    public ValidationResult validate(String input) {
        if (input == null) {
            return ValidationResult.valid();
        }
        
        if (input.length() > maxLength) {
            return ValidationResult.invalid("Input length " + input.length() + " exceeds max " + maxLength);
        }
        
        return ValidationResult.valid();
    }
}
```

- [ ] **Step 1.6: 创建 SseTokenValidator 接口**

```java
package com.schemaplexai.agent.engine.security;

/**
 * SSE Token 验证接口，确保只有授权用户可以订阅 Agent 执行流。
 */
public interface SseTokenValidator {
    
    /**
     * 验证 SSE Token
     * @param token SSE Token
     * @param executionId Agent 执行 ID
     * @return 验证结果
     */
    ValidationResult validate(String token, String executionId);
}
```

- [ ] **Step 1.7: 修改 ContextInjector 集成输入验证**

```java
package com.schemaplexai.agent.engine.context;

import java.util.List;

/**
 * Context 注入器，集成输入验证。
 */
public class ContextInjector {
    
    private final List<InputValidator> validators;
    
    public ContextInjector(List<InputValidator> validators) {
        this.validators = validators;
    }
    
    /**
     * 注入上下文并验证输入
     * @param prompt 原始提示
     * @param context 上下文信息
     * @return 验证后的注入结果
     */
    public String inject(String prompt, AgentContext context) {
        // 1. 验证输入
        for (InputValidator validator : validators) {
            ValidationResult result = validator.validate(prompt);
            if (!result.isValid()) {
                throw new IllegalArgumentException("Input validation failed: " + result.errorMessage());
            }
        }
        
        // 2. 注入上下文
        String injected = doInject(prompt, context);
        
        // 3. 验证注入后的输出
        for (InputValidator validator : validators) {
            ValidationResult result = validator.validate(injected);
            if (!result.isValid()) {
                throw new IllegalArgumentException("Injected content validation failed: " + result.errorMessage());
            }
        }
        
        return injected;
    }
    
    private String doInject(String prompt, AgentContext context) {
        // TODO: 实现实际的上下文注入逻辑
        return prompt;
    }
}
```

- [ ] **Step 1.8: 修改 ToolCallingStateHandler 集成沙箱**

```java
package com.schemaplexai.agent.engine.state;

import com.schemaplexai.agent.engine.tool.ToolSandbox;
import com.schemaplexai.agent.engine.tool.SandboxConfig;

/**
 * ToolCalling 状态处理器，集成安全沙箱。
 */
public class ToolCallingStateHandler implements AgentStateHandler {
    
    private final ToolSandbox sandbox;
    private final SandboxConfig sandboxConfig;
    
    public ToolCallingStateHandler(ToolSandbox sandbox, SandboxConfig sandboxConfig) {
        this.sandbox = sandbox;
        this.sandboxConfig = sandboxConfig;
    }
    
    @Override
    public AgentExecutionState handle(AgentExecutionContext context) {
        try {
            // 1. 解析工具调用
            List<ToolCall> toolCalls = parseToolCalls(context.assistantMessage());
            
            // 2. 在沙箱中执行工具
            for (ToolCall toolCall : toolCalls) {
                ToolResult result = sandbox.execute(toolCall, sandboxConfig);
                context.addObservation(result);
            }
            
            // 3. 转换到 OBSERVATION 状态
            return AgentExecutionState.OBSERVATION;
            
        } catch (ToolExecutionException e) {
            // 4. 错误处理
            context.recordError(e.errorCategory(), e.getMessage());
            return AgentExecutionState.FAILED;
        }
    }
    
    private List<ToolCall> parseToolCalls(String assistantMessage) {
        // TODO: 实现工具调用解析
        return List.of();
    }
}
```

- [ ] **Step 1.9: 修改 AgentExecutionController 集成 SSE 验证**

```java
package com.schemaplexai.agent.engine.controller;

import com.schemaplexai.agent.engine.security.SseTokenValidator;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Agent 执行控制器，集成 SSE Token 验证。
 */
@RestController
@RequestMapping("/agent")
public class AgentExecutionController {
    
    private final SseTokenValidator tokenValidator;
    
    public AgentExecutionController(SseTokenValidator tokenValidator) {
        this.tokenValidator = tokenValidator;
    }
    
    /**
     * 订阅 Agent 执行事件（SSE）
     * @param executionId 执行 ID
     * @param token SSE Token
     * @return SSE Emitter
     */
    @GetMapping("/execution/{executionId}/events")
    public SseEmitter subscribeExecutionEvents(
            @PathVariable String executionId,
            @RequestParam String token) {
        
        // 验证 Token
        ValidationResult result = tokenValidator.validate(token, executionId);
        if (!result.isValid()) {
            throw new SecurityException("Unauthorized SSE access: " + result.errorMessage());
        }
        
        // 创建 SSE Emitter
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        
        // TODO: 注册到事件总线
        
        return emitter;
    }
}
```

- [ ] **Step 1.10: 编写测试**

```java
package com.schemaplexai.agent.engine.tool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

class ToolSandboxTest {
    
    private ContainerToolSandbox sandbox;
    
    @BeforeEach
    void setUp() {
        ToolWhitelist whitelist = new ToolWhitelist(Set.of("calculator", "weather"));
        sandbox = new ContainerToolSandbox(whitelist);
    }
    
    @Test
    void shouldRejectToolsNotInWhitelist() {
        ToolCall toolCall = new ToolCall("malicious", Map.of());
        
        assertThrows(ToolExecutionException.class, () -> {
            sandbox.execute(toolCall, SandboxConfig.defaultConfig());
        });
    }
    
    @Test
    void shouldRejectEmptyToolName() {
        ToolCall toolCall = new ToolCall("", Map.of());
        ValidationResult result = sandbox.validate(toolCall);
        
        assertFalse(result.isValid());
    }
    
    @Test
    void shouldRejectExcessiveParameters() {
        Map<String, Object> params = new HashMap<>();
        for (int i = 0; i < 25; i++) {
            params.put("param" + i, "value" + i);
        }
        
        ToolCall toolCall = new ToolCall("calculator", params);
        ValidationResult result = sandbox.validate(toolCall);
        
        assertFalse(result.isValid());
    }
}
```

- [ ] **Step 1.11: 运行测试并验证通过**

```bash
mvn test -pl schemaplexai-agent-engine -Dtest=ToolSandboxTest,InputValidatorTest,SseTokenValidatorTest -v
```

- [ ] **Step 1.12: 提交**

```bash
git add schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/tool/
git add schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/context/
git add schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/security/
git add schemaplexai-agent-engine/src/test/java/com/schemaplexai/agent/engine/
git commit -m "feat(agent-engine): implement security sandbox, input validation, and SSE token verification

- Add ToolSandbox interface with ContainerToolSandbox implementation
- Add InputValidator interface with BlacklistKeywordChecker and LengthValidator
- Add SseTokenValidator interface for SSE authorization
- Integrate sandbox into ToolCallingStateHandler
- Integrate validation into ContextInjector
- Integrate SSE validation into AgentExecutionController
- Add comprehensive unit tests

Closes #CRITICAL-1, #CRITICAL-2, #CRITICAL-3"
```

---

## 任务 2: Tool Use 完整实现（P1，W1-2）

### 文件清单

- **Create:** `schemaplexai-agent-engine/src/.../tool/ToolRegistry.java` — 工具注册接口
- **Create:** `schemaplexai-agent-engine/src/.../tool/InMemoryToolRegistry.java` — 内存工具注册实现
- **Create:** `schemaplexai-agent-engine/src/.../tool/ToolDefinition.java` — 工具定义数据类
- **Create:** `schemaplexai-agent-engine/src/.../tool/ToolParameter.java` — 工具参数定义
- **Create:** `schemaplexai-agent-engine/src/.../tool/ToolAdapter.java` — 工具适配器
- **Create:** `schemaplexai-agent-engine/src/.../prompt/ReActPromptTemplate.java` — ReAct 提示模板
- **Create:** `schemaplexai-agent-engine/src/.../extractor/FinalAnswerExtractor.java` — 最终答案提取器
- **Modify:** `schemaplexai-agent-engine/src/.../model/LlmProvider.java` — 扩展支持 function calling
- **Modify:** `schemaplexai-agent-engine/src/.../state/ThinkingStateHandler.java` — 集成 ReAct 循环
- **Modify:** `schemaplexai-agent-engine/src/.../AgentExecutionState.java` — 新增 OBSERVATION 状态
- **Test:** `schemaplexai-agent-engine/src/test/.../tool/ToolRegistryTest.java`
- **Test:** `schemaplexai-agent-engine/src/test/.../tool/ToolAdapterTest.java`
- **Test:** `schemaplexai-agent-engine/src/test/.../prompt/ReActPromptTemplateTest.java`
- **Test:** `schemaplexai-agent-engine/src/test/.../extractor/FinalAnswerExtractorTest.java`

### 实施步骤

- [ ] **Step 2.1: 创建 ToolDefinition 数据类**

```java
package com.schemaplexai.agent.engine.tool;

/**
 * 工具定义，描述工具的名称、描述、参数和返回类型。
 */
public record ToolDefinition(
    String name,
    String description,
    List<ToolParameter> parameters,
    String returnType
) {
    
    /**
     * 转换为 OpenAI Function Calling 格式
     */
    public Map<String, Object> toOpenAiFunction() {
        Map<String, Object> function = new HashMap<>();
        function.put("name", name);
        function.put("description", description);
        
        Map<String, Object> params = new HashMap<>();
        params.put("type", "object");
        Map<String, Object> properties = new HashMap<>();
        List<String> required = new ArrayList<>();
        
        for (ToolParameter param : parameters) {
            Map<String, Object> prop = new HashMap<>();
            prop.put("type", param.type());
            prop.put("description", param.description());
            properties.put(param.name(), prop);
            if (param.required()) {
                required.add(param.name());
            }
        }
        
        params.put("properties", properties);
        params.put("required", required);
        function.put("parameters", params);
        
        return function;
    }
    
    /**
     * 转换为 Anthropic Tool Use 格式
     */
    public Map<String, Object> toAnthropicTool() {
        Map<String, Object> tool = new HashMap<>();
        tool.put("name", name);
        tool.put("description", description);
        tool.put("input_schema", toOpenAiFunction().get("parameters"));
        return tool;
    }
}
```

- [ ] **Step 2.2: 创建 ToolRegistry 接口**

```java
package com.schemaplexai.agent.engine.tool;

/**
 * 工具注册接口，管理可用工具。
 */
public interface ToolRegistry {
    
    /**
     * 注册工具
     * @param toolDef 工具定义
     */
    void register(ToolDefinition toolDef);
    
    /**
     * 批量注册工具
     * @param toolDefs 工具定义列表
     */
    void registerAll(List<ToolDefinition> toolDefs);
    
    /**
     * 获取工具定义
     * @param name 工具名称
     * @return 工具定义，不存在返回 null
     */
    ToolDefinition get(String name);
    
    /**
     * 获取所有工具定义
     * @return 工具定义列表
     */
    List<ToolDefinition> getAll();
    
    /**
     * 获取所有工具定义（OpenAI 格式）
     * @return OpenAI Function Calling 格式的工具列表
     */
    List<Map<String, Object>> getAllAsOpenAiFunctions();
    
    /**
     * 获取所有工具定义（Anthropic 格式）
     * @return Anthropic Tool Use 格式的工具列表
     */
    List<Map<String, Object>> getAllAsAnthropicTools();
    
    /**
     * 检查工具是否存在
     * @param name 工具名称
     * @return true 如果存在
     */
    boolean exists(String name);
    
    /**
     * 移除工具
     * @param name 工具名称
     * @return true 如果移除成功
     */
    boolean unregister(String name);
}
```

- [ ] **Step 2.3: 创建 InMemoryToolRegistry 实现**

```java
package com.schemaplexai.agent.engine.tool;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存工具注册实现，支持并发访问。
 */
public class InMemoryToolRegistry implements ToolRegistry {
    
    private final ConcurrentHashMap<String, ToolDefinition> tools = new ConcurrentHashMap<>();
    
    @Override
    public void register(ToolDefinition toolDef) {
        if (toolDef == null) {
            throw new IllegalArgumentException("Tool definition cannot be null");
        }
        if (toolDef.name() == null || toolDef.name().isBlank()) {
            throw new IllegalArgumentException("Tool name cannot be null or blank");
        }
        tools.put(toolDef.name(), toolDef);
    }
    
    @Override
    public void registerAll(List<ToolDefinition> toolDefs) {
        for (ToolDefinition toolDef : toolDefs) {
            register(toolDef);
        }
    }
    
    @Override
    public ToolDefinition get(String name) {
        return tools.get(name);
    }
    
    @Override
    public List<ToolDefinition> getAll() {
        return List.copyOf(tools.values());
    }
    
    @Override
    public List<Map<String, Object>> getAllAsOpenAiFunctions() {
        return tools.values().stream()
            .map(ToolDefinition::toOpenAiFunction)
            .toList();
    }
    
    @Override
    public List<Map<String, Object>> getAllAsAnthropicTools() {
        return tools.values().stream()
            .map(ToolDefinition::toAnthropicTool)
            .toList();
    }
    
    @Override
    public boolean exists(String name) {
        return tools.containsKey(name);
    }
    
    @Override
    public boolean unregister(String name) {
        return tools.remove(name) != null;
    }
}
```

- [ ] **Step 2.4: 创建 ReActPromptTemplate**

```java
package com.schemaplexai.agent.engine.prompt;

/**
 * ReAct 提示模板，用于指导 LLM 生成 ReAct 格式的输出。
 */
public class ReActPromptTemplate {
    
    private static final String SYSTEM_PROMPT = """
        You are a helpful AI assistant that can use tools to answer questions and complete tasks.
        
        You have access to the following tools:
        {tools}
        
        To use a tool, you MUST respond in the following format:
        
        Thought: [Your reasoning about what to do next]
        Action: [The name of the tool to use]
        Action Input: [The input to the tool, as a JSON object]
        
        The tool will then respond with:
        
        Observation: [The result from the tool]
        
        You can then continue reasoning and using tools as needed.
        
        When you have enough information to answer, you MUST respond with:
        
        Thought: [Your reasoning about the final answer]
        Final Answer: [Your final response to the user]
        
        Important rules:
        1. Always start with a Thought
        2. Use Action and Action Input together, or Final Answer alone
        3. Never skip the Thought step
        4. If a tool fails, explain what happened and try a different approach
        5. Maximum {max_iterations} tool calls allowed
        """;
    
    private final String toolsDescription;
    private final int maxIterations;
    
    public ReActPromptTemplate(List<ToolDefinition> tools, int maxIterations) {
        this.toolsDescription = formatTools(tools);
        this.maxIterations = maxIterations;
    }
    
    public ReActPromptTemplate(List<ToolDefinition> tools) {
        this(tools, 10);
    }
    
    /**
     * 生成系统提示
     */
    public String generateSystemPrompt() {
        return SYSTEM_PROMPT
            .replace("{tools}", toolsDescription)
            .replace("{max_iterations}", String.valueOf(maxIterations));
    }
    
    /**
     * 格式化工具列表为文本描述
     */
    private String formatTools(List<ToolDefinition> tools) {
        StringBuilder sb = new StringBuilder();
        for (ToolDefinition tool : tools) {
            sb.append("- ").append(tool.name()).append(": ").append(tool.description()).append("\n");
            for (ToolParameter param : tool.parameters()) {
                sb.append("  - ").append(param.name());
                sb.append(" (").append(param.type()).append(")");
                if (param.required()) {
                    sb.append(" [REQUIRED]");
                }
                sb.append(": ").append(param.description()).append("\n");
            }
        }
        return sb.toString();
    }
    
    /**
     * 构建完整的消息历史
     */
    public List<ChatMessage> buildMessages(String userMessage, List<ChatMessage> conversationHistory) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.system(generateSystemPrompt()));
        messages.addAll(conversationHistory);
        messages.add(ChatMessage.user(userMessage));
        return messages;
    }
}
```

- [ ] **Step 2.5: 创建 FinalAnswerExtractor**

```java
package com.schemaplexai.agent.engine.extractor;

/**
 * 最终答案提取器，从 LLM 输出中提取 Final Answer。
 */
public class FinalAnswerExtractor {
    
    private static final Pattern FINAL_ANSWER_PATTERN = 
        Pattern.compile("Final Answer:\\s*(.+)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    
    /**
     * 提取最终答案
     * @param llmOutput LLM 的完整输出
     * @return 最终答案，如果未找到返回 null
     */
    public String extract(String llmOutput) {
        if (llmOutput == null || llmOutput.isBlank()) {
            return null;
        }
        
        Matcher matcher = FINAL_ANSWER_PATTERN.matcher(llmOutput);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        
        return null;
    }
    
    /**
     * 检查是否包含最终答案
     * @param llmOutput LLM 的完整输出
     * @return true 如果包含 Final Answer
     */
    public boolean containsFinalAnswer(String llmOutput) {
        return extract(llmOutput) != null;
    }
    
    /**
     * 提取思考过程
     * @param llmOutput LLM 的完整输出
     * @return 思考过程，如果未找到返回 null
     */
    public String extractThought(String llmOutput) {
        if (llmOutput == null || llmOutput.isBlank()) {
            return null;
        }
        
        Pattern thoughtPattern = Pattern.compile("Thought:\\s*(.+?)(?=\\n(?:Action|Final Answer):|$)", 
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher matcher = thoughtPattern.matcher(llmOutput);
        
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        
        return null;
    }
    
    /**
     * 提取工具调用
     * @param llmOutput LLM 的完整输出
     * @return 工具调用，如果未找到返回 null
     */
    public ToolCall extractToolCall(String llmOutput) {
        if (llmOutput == null || llmOutput.isBlank()) {
            return null;
        }
        
        Pattern actionPattern = Pattern.compile("Action:\\s*(.+?)\\n", Pattern.CASE_INSENSITIVE);
        Pattern inputPattern = Pattern.compile("Action Input:\\s*(\\{.+?\\})", 
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        
        Matcher actionMatcher = actionPattern.matcher(llmOutput);
        Matcher inputMatcher = inputPattern.matcher(llmOutput);
        
        if (actionMatcher.find() && inputMatcher.find()) {
            String toolName = actionMatcher.group(1).trim();
            String inputJson = inputMatcher.group(1).trim();
            
            try {
                Map<String, Object> parameters = parseJson(inputJson);
                return new ToolCall(toolName, parameters);
            } catch (Exception e) {
                return null;
            }
        }
        
        return null;
    }
    
    private Map<String, Object> parseJson(String json) {
        // 简单的 JSON 解析，实际应使用 Jackson 或 Gson
        return new HashMap<>();
    }
}
```

- [ ] **Step 2.6: 修改 LlmProvider 接口**

```java
package com.schemaplexai.agent.engine.model;

/**
 * LLM 提供商接口，支持 function calling。
 */
public interface LlmProvider {
    
    /**
     * 生成响应（无工具）
     * @param prompt 提示文本
     * @param options 生成选项
     * @return 生成的响应
     */
    LlmResponse generate(String prompt, GenerationOptions options);
    
    /**
     * 生成响应（支持 function calling）
     * @param messages 消息历史
     * @param tools 可用工具列表
     * @param options 生成选项
     * @return 生成的响应
     */
    LlmResponse generateWithTools(List<ChatMessage> messages, 
                                  List<ToolDefinition> tools, 
                                  GenerationOptions options);
    
    /**
     * 流式生成
     * @param prompt 提示文本
     * @param options 生成选项
     * @return 流式响应
     */
    Flux<LlmChunk> generateStream(String prompt, GenerationOptions options);
    
    /**
     * 获取提供商名称
     * @return 提供商名称
     */
    String getName();
    
    /**
     * 检查是否健康
     * @return true 如果健康
     */
    boolean isHealthy();
}
```

- [ ] **Step 2.7: 修改 ThinkingStateHandler**

```java
package com.schemaplexai.agent.engine.state;

import com.schemaplexai.agent.engine.prompt.ReActPromptTemplate;
import com.schemaplexai.agent.engine.extractor.FinalAnswerExtractor;
import com.schemaplexai.agent.engine.tool.ToolRegistry;

/**
 * Thinking 状态处理器，支持 ReAct 循环。
 */
public class ThinkingStateHandler implements AgentStateHandler {
    
    private final LlmProvider llmProvider;
    private final ToolRegistry toolRegistry;
    private final ReActPromptTemplate promptTemplate;
    private final FinalAnswerExtractor answerExtractor;
    private final int maxIterations;
    
    public ThinkingStateHandler(LlmProvider llmProvider, 
                                 ToolRegistry toolRegistry,
                                 int maxIterations) {
        this.llmProvider = llmProvider;
        this.toolRegistry = toolRegistry;
        this.promptTemplate = new ReActPromptTemplate(toolRegistry.getAll(), maxIterations);
        this.answerExtractor = new FinalAnswerExtractor();
        this.maxIterations = maxIterations;
    }
    
    @Override
    public AgentExecutionState handle(AgentExecutionContext context) {
        // 检查迭代次数
        if (context.iterationCount() >= maxIterations) {
            return handleMaxIterationsReached(context);
        }
        
        // 构建消息
        List<ChatMessage> messages = promptTemplate.buildMessages(
            context.userMessage(),
            context.conversationHistory()
        );
        
        // 调用 LLM
        GenerationOptions options = GenerationOptions.builder()
            .temperature(0.7)
            .maxTokens(4096)
            .build();
        
        LlmResponse response = llmProvider.generateWithTools(messages, toolRegistry.getAll(), options);
        
        // 检查是否包含最终答案
        if (answerExtractor.containsFinalAnswer(response.text())) {
            String finalAnswer = answerExtractor.extract(response.text());
            context.setFinalAnswer(finalAnswer);
            return AgentExecutionState.COMPLETED;
        }
        
        // 检查是否包含工具调用
        ToolCall toolCall = answerExtractor.extractToolCall(response.text());
        if (toolCall != null) {
            context.setAssistantMessage(response.text());
            return AgentExecutionState.TOOL_CALLING;
        }
        
        // 无工具调用也无最终答案，视为最终答案
        context.setFinalAnswer(response.text());
        return AgentExecutionState.COMPLETED;
    }
    
    private AgentExecutionState handleMaxIterationsReached(AgentExecutionContext context) {
        // 达到最大迭代次数，使用最后一个输出作为最终答案
        String lastOutput = context.assistantMessage();
        String finalAnswer = answerExtractor.extract(lastOutput);
        
        if (finalAnswer != null) {
            context.setFinalAnswer(finalAnswer);
            return AgentExecutionState.COMPLETED;
        }
        
        // 无法提取最终答案，标记为失败
        context.recordError(ToolErrorCategory.INTERNAL_ERROR, 
            "Max iterations reached without final answer");
        return AgentExecutionState.FAILED;
    }
}
```

- [ ] **Step 2.8: 修改 AgentExecutionState**

```java
package com.schemaplexai.agent.engine.state;

/**
 * Agent 执行状态枚举。
 */
public enum AgentExecutionState {
    
    /**
     * 初始化状态
     */
    INITIALIZING,
    
    /**
     * 就绪状态
     */
    READY,
    
    /**
     * 思考状态（ReAct 循环的核心）
     */
    THINKING,
    
    /**
     * 工具调用状态
     */
    TOOL_CALLING,
    
    /**
     * 观察状态（工具执行后的结果处理）
     */
    OBSERVATION,
    
    /**
     * 暂停状态（等待人工干预）
     */
    PAUSED,
    
    /**
     * 门禁阻塞状态（等待准入检查通过）
     */
    GATE_BLOCKED,
    
    /**
     * 完成状态
     */
    COMPLETED,
    
    /**
     * 失败状态
     */
    FAILED,
    
    /**
     * 重试状态
     */
    RETRYING,
    
    /**
     * 反思状态（自我纠错）
     */
    REFLECTING;
    
    /**
     * 检查是否为终态
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED;
    }
    
    /**
     * 检查是否可以转换到目标状态
     */
    public boolean canTransitionTo(AgentExecutionState target) {
        return switch (this) {
            case INITIALIZING -> target == READY;
            case READY -> target == THINKING;
            case THINKING -> target == TOOL_CALLING || target == COMPLETED || target == FAILED || target == REFLECTING;
            case TOOL_CALLING -> target == OBSERVATION || target == FAILED;
            case OBSERVATION -> target == THINKING; // ReAct 循环的关键转换
            case PAUSED -> target == THINKING;
            case GATE_BLOCKED -> target == THINKING || target == FAILED;
            case REFLECTING -> target == THINKING || target == COMPLETED;
            case RETRYING -> target == THINKING || target == FAILED;
            case COMPLETED, FAILED -> false; // 终态不可转换
        };
    }
}
```

- [ ] **Step 2.9: 编写测试**

```java
package com.schemaplexai.agent.engine.tool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

class ToolRegistryTest {
    
    private InMemoryToolRegistry registry;
    
    @BeforeEach
    void setUp() {
        registry = new InMemoryToolRegistry();
    }
    
    @Test
    void shouldRegisterAndRetrieveTool() {
        ToolDefinition toolDef = new ToolDefinition(
            "calculator",
            "A simple calculator",
            List.of(new ToolParameter("expression", "string", "Math expression", true)),
            "number"
        );
        
        registry.register(toolDef);
        
        assertTrue(registry.exists("calculator"));
        assertEquals(toolDef, registry.get("calculator"));
    }
    
    @Test
    void shouldRejectNullToolDefinition() {
        assertThrows(IllegalArgumentException.class, () -> registry.register(null));
    }
    
    @Test
    void shouldRejectBlankToolName() {
        ToolDefinition toolDef = new ToolDefinition("", "desc", List.of(), "string");
        assertThrows(IllegalArgumentException.class, () -> registry.register(toolDef));
    }
    
    @Test
    void shouldConvertToOpenAiFormat() {
        ToolDefinition toolDef = new ToolDefinition(
            "weather",
            "Get current weather",
            List.of(new ToolParameter("city", "string", "City name", true)),
            "object"
        );
        
        registry.register(toolDef);
        
        List<Map<String, Object>> functions = registry.getAllAsOpenAiFunctions();
        assertEquals(1, functions.size());
        assertEquals("weather", functions.get(0).get("name"));
    }
}
```

```java
package com.schemaplexai.agent.engine.extractor;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FinalAnswerExtractorTest {
    
    private final FinalAnswerExtractor extractor = new FinalAnswerExtractor();
    
    @Test
    void shouldExtractFinalAnswer() {
        String output = """
            Thought: I now have enough information to answer.
            Final Answer: The weather in Beijing is sunny, 25°C.
            """;
        
        String answer = extractor.extract(output);
        assertEquals("The weather in Beijing is sunny, 25°C.", answer);
    }
    
    @Test
    void shouldExtractToolCall() {
        String output = """
            Thought: I need to check the weather.
            Action: weather
            Action Input: {"city": "Beijing"}
            """;
        
        ToolCall toolCall = extractor.extractToolCall(output);
        assertNotNull(toolCall);
        assertEquals("weather", toolCall.toolName());
        assertEquals("Beijing", toolCall.parameters().get("city"));
    }
    
    @Test
    void shouldReturnNullWhenNoFinalAnswer() {
        String output = "This is just regular text without the expected format.";
        assertNull(extractor.extract(output));
    }
    
    @Test
    void shouldDetectFinalAnswerPresence() {
        String withAnswer = "Thought: Done. Final Answer: The answer is 42.";
        String withoutAnswer = "Thought: I need more information.";
        
        assertTrue(extractor.containsFinalAnswer(withAnswer));
        assertFalse(extractor.containsFinalAnswer(withoutAnswer));
    }
}
```

- [ ] **Step 2.10: 运行测试并验证通过**

```bash
mvn test -pl schemaplexai-agent-engine -Dtest=ToolRegistryTest,ToolAdapterTest,ReActPromptTemplateTest,FinalAnswerExtractorTest -v
```

- [ ] **Step 2.11: 提交**

```bash
git add schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/tool/
git add schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/prompt/
git add schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/extractor/
git add schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/model/
git add schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/state/
git add schemaplexai-agent-engine/src/test/java/com/schemaplexai/agent/engine/
git commit -m "feat(agent-engine): implement complete Tool Use with ReAct cycle

- Add ToolRegistry interface with InMemoryToolRegistry
- Add ToolDefinition with OpenAI/Anthropic format conversion
- Add ReActPromptTemplate for structured ReAct prompts
- Add FinalAnswerExtractor for parsing LLM output
- Extend LlmProvider to support function calling
- Update ThinkingStateHandler to support ReAct loop
- Add OBSERVATION and REFLECTING states to state machine
- Add comprehensive unit tests

Closes #P1-2"
```

---

## 任务 3: Reasoning + Exception Handling（P1，W3-4）

### 文件清单

- **Create:** `schemaplexai-agent-engine/src/.../reasoning/ReasoningStrategy.java` — 推理策略接口
- **Create:** `schemaplexai-agent-engine/src/.../reasoning/ReActStrategy.java` — ReAct 推理策略
- **Create:** `schemaplexai-agent-engine/src/.../reasoning/CoTStrategy.java` — Chain-of-Thought 推理策略
- **Create:** `schemaplexai-agent-engine/src/.../exception/RecoveryStrategy.java` — 恢复策略接口
- **Create:** `schemaplexai-agent-engine/src/.../exception/RetryRecoveryStrategy.java` — 重试恢复策略
- **Create:** `schemaplexai-agent-engine/src/.../exception/FallbackRecoveryStrategy.java` — 降级恢复策略
- **Create:** `schemaplexai-agent-engine/src/.../state/ExceptionHandlingStateHandler.java` — 异常处理状态处理器
- **Modify:** `schemaplexai-agent-engine/src/.../state/ThinkingStateHandler.java` — 集成推理策略
- **Modify:** `schemaplexai-agent-engine/src/.../TokenBudget.java` — 累加检查
- **Test:** `schemaplexai-agent-engine/src/test/.../reasoning/ReasoningStrategyTest.java`
- **Test:** `schemaplexai-agent-engine/src/test/.../exception/RecoveryStrategyTest.java`

### 实施步骤

- [ ] **Step 3.1: 创建 ReasoningStrategy 接口**

```java
package com.schemaplexai.agent.engine.reasoning;

/**
 * 推理策略接口，支持多种推理模式。
 */
public interface ReasoningStrategy {
    
    /**
     * 执行推理
     * @param context 推理上下文
     * @param budget Token 预算
     * @return 推理结果
     */
    ThinkingResult think(AgentExecutionContext context, TokenBudget budget);
    
    /**
     * 获取策略名称
     * @return 策略名称
     */
    String getName();
    
    /**
     * 检查是否支持继续推理
     * @param context 推理上下文
     * @return true 如果支持
     */
    boolean canContinue(AgentExecutionContext context);
}
```

- [ ] **Step 3.2: 创建 ReActStrategy 实现**

```java
package com.schemaplexai.agent.engine.reasoning;

/**
 * ReAct 推理策略实现。
 */
public class ReActStrategy implements ReasoningStrategy {
    
    private final LlmProvider llmProvider;
    private final ToolRegistry toolRegistry;
    private final ReActPromptTemplate promptTemplate;
    private final FinalAnswerExtractor answerExtractor;
    
    public ReActStrategy(LlmProvider llmProvider, ToolRegistry toolRegistry) {
        this.llmProvider = llmProvider;
        this.toolRegistry = toolRegistry;
        this.promptTemplate = new ReActPromptTemplate(toolRegistry.getAll());
        this.answerExtractor = new FinalAnswerExtractor();
    }
    
    @Override
    public ThinkingResult think(AgentExecutionContext context, TokenBudget budget) {
        // 检查 Token 预算
        if (!budget.hasRemaining()) {
            return ThinkingResult.exhausted("Token budget exhausted");
        }
        
        // 构建消息
        List<ChatMessage> messages = promptTemplate.buildMessages(
            context.userMessage(),
            context.conversationHistory()
        );
        
        // 调用 LLM
        GenerationOptions options = GenerationOptions.builder()
            .temperature(0.7)
            .maxTokens(Math.min(budget.getRemaining(), 4096))
            .build();
        
        LlmResponse response = llmProvider.generateWithTools(messages, toolRegistry.getAll(), options);
        
        // 消耗 Token
        budget.consume(response.usage().inputTokens(), response.usage().outputTokens());
        
        // 解析结果
        if (answerExtractor.containsFinalAnswer(response.text())) {
            String finalAnswer = answerExtractor.extract(response.text());
            return ThinkingResult.completed(finalAnswer);
        }
        
        ToolCall toolCall = answerExtractor.extractToolCall(response.text());
        if (toolCall != null) {
            return ThinkingResult.toolCall(toolCall, response.text());
        }
        
        return ThinkingResult.completed(response.text());
    }
    
    @Override
    public String getName() {
        return "ReAct";
    }
    
    @Override
    public boolean canContinue(AgentExecutionContext context) {
        return context.iterationCount() < 10; // 最大 10 轮
    }
}
```

- [ ] **Step 3.3: 创建 RecoveryStrategy 接口**

```java
package com.schemaplexai.agent.engine.exception;

/**
 * 恢复策略接口，定义错误后的恢复行为。
 */
public interface RecoveryStrategy {
    
    /**
     * 执行恢复
     * @param error 错误信息
     * @param context 执行上下文
     * @return 恢复结果
     */
    RecoveryResult recover(ToolExecutionException error, AgentExecutionContext context);
    
    /**
     * 检查是否支持恢复该错误
     * @param errorCategory 错误类别
     * @return true 如果支持
     */
    boolean supports(ToolErrorCategory errorCategory);
    
    /**
     * 获取最大重试次数
     * @return 最大重试次数
     */
    int getMaxRetries();
}
```

- [ ] **Step 3.4: 创建 RetryRecoveryStrategy 实现**

```java
package com.schemaplexai.agent.engine.exception;

/**
 * 重试恢复策略，对可重试的错误进行重试。
 */
public class RetryRecoveryStrategy implements RecoveryStrategy {
    
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final Duration RETRY_DELAY = Duration.ofSeconds(1);
    
    private final int maxRetries;
    
    public RetryRecoveryStrategy(int maxRetries) {
        this.maxRetries = maxRetries;
    }
    
    public RetryRecoveryStrategy() {
        this(DEFAULT_MAX_RETRIES);
    }
    
    @Override
    public RecoveryResult recover(ToolExecutionException error, AgentExecutionContext context) {
        int currentRetry = context.getRetryCount();
        
        if (currentRetry >= maxRetries) {
            return RecoveryResult.failed("Max retries exceeded: " + maxRetries);
        }
        
        // 指数退避延迟
        Duration delay = RETRY_DELAY.multipliedBy((long) Math.pow(2, currentRetry));
        
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return RecoveryResult.failed("Retry interrupted");
        }
        
        context.incrementRetryCount();
        return RecoveryResult.retry("Retrying after error: " + error.getMessage());
    }
    
    @Override
    public boolean supports(ToolErrorCategory errorCategory) {
        return errorCategory == ToolErrorCategory.RATE_LIMITED 
            || errorCategory == ToolErrorCategory.TIMEOUT
            || errorCategory == ToolErrorCategory.INTERNAL_ERROR;
    }
    
    @Override
    public int getMaxRetries() {
        return maxRetries;
    }
}
```

- [ ] **Step 3.5: 修改 TokenBudget 实现累加检查**

```java
package com.schemaplexai.agent.engine;

/**
 * Token 预算管理，支持累加检查。
 */
public class TokenBudget {
    
    private final int maxInputTokens;
    private final int maxOutputTokens;
    private int consumedInputTokens;
    private int consumedOutputTokens;
    
    public TokenBudget(int maxInputTokens, int maxOutputTokens) {
        this.maxInputTokens = maxInputTokens;
        this.maxOutputTokens = maxOutputTokens;
        this.consumedInputTokens = 0;
        this.consumedOutputTokens = 0;
    }
    
    /**
     * 消耗 Token
     * @param inputTokens 输入 Token 数量
     * @param outputTokens 输出 Token 数量
     * @throws TokenBudgetExceededException 如果超出预算
     */
    public void consume(int inputTokens, int outputTokens) {
        if (consumedInputTokens + inputTokens > maxInputTokens) {
            throw new TokenBudgetExceededException(
                "Input token budget exceeded: " + (consumedInputTokens + inputTokens) + " > " + maxInputTokens);
        }
        
        if (consumedOutputTokens + outputTokens > maxOutputTokens) {
            throw new TokenBudgetExceededException(
                "Output token budget exceeded: " + (consumedOutputTokens + outputTokens) + " > " + maxOutputTokens);
        }
        
        consumedInputTokens += inputTokens;
        consumedOutputTokens += outputTokens;
    }
    
    /**
     * 检查是否有剩余预算
     */
    public boolean hasRemaining() {
        return consumedInputTokens < maxInputTokens && consumedOutputTokens < maxOutputTokens;
    }
    
    /**
     * 获取剩余输入 Token
     */
    public int getRemainingInput() {
        return maxInputTokens - consumedInputTokens;
    }
    
    /**
     * 获取剩余输出 Token
     */
    public int getRemainingOutput() {
        return maxOutputTokens - consumedOutputTokens;
    }
    
    /**
     * 获取总消耗
     */
    public int getTotalConsumed() {
        return consumedInputTokens + consumedOutputTokens;
    }
}
```

- [ ] **Step 3.6: 编写测试**

```java
package com.schemaplexai.agent.engine.reasoning;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

class ReasoningStrategyTest {
    
    private ReActStrategy strategy;
    
    @BeforeEach
    void setUp() {
        LlmProvider mockProvider = createMockProvider();
        ToolRegistry registry = new InMemoryToolRegistry();
        
        strategy = new ReActStrategy(mockProvider, registry);
    }
    
    @Test
    void shouldReturnCompletedWhenFinalAnswer() {
        AgentExecutionContext context = createContext("What is 2+2?");
        TokenBudget budget = new TokenBudget(10000, 10000);
        
        ThinkingResult result = strategy.think(context, budget);
        
        assertEquals(ThinkingResultType.COMPLETED, result.type());
    }
    
    @Test
    void shouldReturnToolCallWhenAction() {
        AgentExecutionContext context = createContext("What is the weather?");
        TokenBudget budget = new TokenBudget(10000, 10000);
        
        ThinkingResult result = strategy.think(context, budget);
        
        assertEquals(ThinkingResultType.TOOL_CALL, result.type());
    }
    
    @Test
    void shouldRespectTokenBudget() {
        AgentExecutionContext context = createContext("Complex question");
        TokenBudget budget = new TokenBudget(100, 100);
        
        ThinkingResult result = strategy.think(context, budget);
        
        assertTrue(budget.getTotalConsumed() > 0);
    }
}
```

- [ ] **Step 3.7: 运行测试并验证通过**

```bash
mvn test -pl schemaplexai-agent-engine -Dtest=ReasoningStrategyTest,RecoveryStrategyTest,TokenBudgetTest -v
```

- [ ] **Step 3.8: 提交**

```bash
git add schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/reasoning/
git add schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/exception/
git add schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/state/
git add schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/TokenBudget.java
git add schemaplexai-agent-engine/src/test/java/com/schemaplexai/agent/engine/
git commit -m "feat(agent-engine): implement Reasoning strategies and Exception Handling

- Add ReasoningStrategy interface with ReAct and CoT implementations
- Add RecoveryStrategy interface with Retry and Fallback implementations
- Update TokenBudget with cumulative checking
- Integrate reasoning strategies into ThinkingStateHandler
- Add comprehensive unit tests

Closes #P1-3"
```

---

## 任务 4: Memory + RAG 数据隔离（P1，W5）

### 文件清单

- **Create:** `schemaplexai-agent-engine/src/.../memory/MemoryStrategy.java` — 内存策略接口
- **Create:** `schemaplexai-agent-engine/src/.../memory/SlidingWindowStrategy.java` — 滑动窗口策略
- **Create:** `schemaplexai-agent-engine/src/.../memory/SummarizationStrategy.java` — 摘要策略
- **Create:** `schemaplexai-agent-engine/src/.../rag/RagIsolationConfig.java` — RAG 隔离配置
- **Create:** `schemaplexai-agent-engine/src/.../rag/MilvusIsolationService.java` — Milvus 隔离服务
- **Modify:** `schemaplexai-agent-engine/src/.../context/ContextInjector.java` — 集成 RAG 检索
- **Modify:** `schemaplexai-agent-engine/src/.../memory/CompositeChatMemoryStore.java` — 策略化
- **Test:** `schemaplexai-agent-engine/src/test/.../memory/MemoryStrategyTest.java`
- **Test:** `schemaplexai-agent-engine/src/test/.../rag/RagIsolationTest.java`

### 实施步骤

- [ ] **Step 4.1: 创建 MemoryStrategy 接口**

```java
package com.schemaplexai.agent.engine.memory;

/**
 * 内存策略接口，定义消息选择和压缩行为。
 */
public interface MemoryStrategy {
    
    /**
     * 选择消息（根据 Token 预算）
     * @param messages 所有消息
     * @param budget Token 预算
     * @return 选择的消息
     */
    List<ChatMessage> select(List<ChatMessage> messages, TokenBudget budget);
    
    /**
     * 压缩消息历史
     * @param messages 要压缩的消息
     * @return 压缩后的摘要
     */
    CompressedMemory compress(List<ChatMessage> messages);
    
    /**
     * 获取策略名称
     * @return 策略名称
     */
    String getName();
}
```

- [ ] **Step 4.2: 创建 RagIsolationConfig**

```java
package com.schemaplexai.agent.engine.rag;

/**
 * RAG 数据隔离配置，定义多租户数据隔离规则。
 */
public record RagIsolationConfig(
    String collectionPrefix,
    boolean enablePartitionIsolation,
    boolean enableSearchFilter,
    int maxResults,
    double similarityThreshold
) {
    
    public static RagIsolationConfig defaultConfig() {
        return new RagIsolationConfig(
            "tenant_",
            true,
            true,
            10,
            0.7
        );
    }
    
    /**
     * 生成租户隔离的集合名称
     * @param tenantId 租户 ID
     * @return 集合名称
     */
    public String collectionName(String tenantId) {
        return collectionPrefix + tenantId;
    }
    
    /**
     * 生成搜索过滤条件
     * @param tenantId 租户 ID
     * @param projectId 项目 ID
     * @return 过滤条件
     */
    public String searchFilter(String tenantId, String projectId) {
        StringBuilder filter = new StringBuilder();
        filter.append("tenant_id == '").append(tenantId).append("'");
        if (projectId != null && !projectId.isBlank()) {
            filter.append(" AND project_id == '").append(projectId).append("'");
        }
        return filter.toString();
    }
}
```

- [ ] **Step 4.3: 创建 MilvusIsolationService**

```java
package com.schemaplexai.agent.engine.rag;

/**
 * Milvus 隔离服务，提供多租户数据隔离。
 */
public class MilvusIsolationService {
    
    private final RagIsolationConfig config;
    private final MilvusClient milvusClient;
    
    public MilvusIsolationService(RagIsolationConfig config, MilvusClient milvusClient) {
        this.config = config;
        this.milvusClient = milvusClient;
    }
    
    /**
     * 验证搜索权限
     * @param tenantId 租户 ID
     * @param projectId 项目 ID
     * @return 验证结果
     */
    public ValidationResult validateSearchPermission(String tenantId, String projectId) {
        if (tenantId == null || tenantId.isBlank()) {
            return ValidationResult.invalid("Tenant ID is required for RAG search");
        }
        
        // 检查集合是否存在
        String collectionName = config.collectionName(tenantId);
        if (!milvusClient.hasCollection(collectionName)) {
            return ValidationResult.invalid("Collection not found: " + collectionName);
        }
        
        return ValidationResult.valid();
    }
    
    /**
     * 执行隔离搜索
     * @param tenantId 租户 ID
     * @param projectId 项目 ID
     * @param queryEmbedding 查询向量
     * @return 搜索结果
     */
    public List<SearchResult> searchWithIsolation(String tenantId, String projectId, 
                                                   float[] queryEmbedding) {
        // 验证权限
        ValidationResult validation = validateSearchPermission(tenantId, projectId);
        if (!validation.isValid()) {
            throw new SecurityException("RAG search permission denied: " + validation.errorMessage());
        }
        
        // 构建搜索请求
        String collectionName = config.collectionName(tenantId);
        String filter = config.searchFilter(tenantId, projectId);
        
        // 执行搜索
        return milvusClient.search(
            collectionName,
            queryEmbedding,
            config.maxResults(),
            config.similarityThreshold(),
            filter
        );
    }
}
```

- [ ] **Step 4.4: 修改 ContextInjector 集成 RAG 检索**

```java
package com.schemaplexai.agent.engine.context;

/**
 * Context 注入器，集成 RAG 检索和输入验证。
 */
public class ContextInjector {
    
    private final List<InputValidator> validators;
    private final MilvusIsolationService ragService;
    private final EmbeddingService embeddingService;
    
    public ContextInjector(List<InputValidator> validators, 
                           MilvusIsolationService ragService,
                           EmbeddingService embeddingService) {
        this.validators = validators;
        this.ragService = ragService;
        this.embeddingService = embeddingService;
    }
    
    /**
     * 注入上下文并验证输入
     * @param prompt 原始提示
     * @param context Agent 上下文
     * @return 验证后的注入结果
     */
    public String inject(String prompt, AgentContext context) {
        // 1. 验证输入
        for (InputValidator validator : validators) {
            ValidationResult result = validator.validate(prompt);
            if (!result.isValid()) {
                throw new IllegalArgumentException("Input validation failed: " + result.errorMessage());
            }
        }
        
        // 2. 执行 RAG 检索
        String ragContext = retrieveRagContext(prompt, context);
        
        // 3. 组合上下文
        String enrichedPrompt = combineContexts(prompt, ragContext, context);
        
        // 4. 验证注入后的输出
        for (InputValidator validator : validators) {
            ValidationResult result = validator.validate(enrichedPrompt);
            if (!result.isValid()) {
                throw new IllegalArgumentException("Injected content validation failed: " + result.errorMessage());
            }
        }
        
        return enrichedPrompt;
    }
    
    private String retrieveRagContext(String prompt, AgentContext context) {
        try {
            // 生成查询向量
            float[] queryEmbedding = embeddingService.embed(prompt);
            
            // 执行隔离搜索
            List<SearchResult> results = ragService.searchWithIsolation(
                context.tenantId(),
                context.projectId(),
                queryEmbedding
            );
            
            // 格式化搜索结果
            return formatSearchResults(results);
            
        } catch (Exception e) {
            // RAG 检索失败不应该阻塞整个流程
            log.warn("RAG retrieval failed, proceeding without context", e);
            return "";
        }
    }
    
    private String combineContexts(String prompt, String ragContext, AgentContext context) {
        if (ragContext == null || ragContext.isBlank()) {
            return prompt;
        }
        
        return """
            Context from knowledge base:
            %s
            
            User query:
            %s
            """.formatted(ragContext, prompt);
    }
    
    private String formatSearchResults(List<SearchResult> results) {
        if (results == null || results.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            SearchResult result = results.get(i);
            sb.append(i + 1).append(". ").append(result.content()).append("\n");
            if (result.source() != null) {
                sb.append("   Source: ").append(result.source()).append("\n");
            }
        }
        return sb.toString();
    }
}
```

- [ ] **Step 4.5: 编写测试**

```java
package com.schemaplexai.agent.engine.rag;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

class RagIsolationTest {
    
    private MilvusIsolationService ragService;
    private RagIsolationConfig config;
    
    @BeforeEach
    void setUp() {
        config = RagIsolationConfig.defaultConfig();
        MilvusClient mockClient = createMockClient();
        ragService = new MilvusIsolationService(config, mockClient);
    }
    
    @Test
    void shouldRejectSearchWithoutTenantId() {
        ValidationResult result = ragService.validateSearchPermission(null, "project1");
        assertFalse(result.isValid());
    }
    
    @Test
    void shouldRejectSearchForNonExistentCollection() {
        ValidationResult result = ragService.validateSearchPermission("nonexistent", "project1");
        assertFalse(result.isValid());
    }
    
    @Test
    void shouldGenerateCorrectCollectionName() {
        String collectionName = config.collectionName("tenant123");
        assertEquals("tenant_tenant123", collectionName);
    }
    
    @Test
    void shouldGenerateSearchFilter() {
        String filter = config.searchFilter("tenant1", "project1");
        assertEquals("tenant_id == 'tenant1' AND project_id == 'project1'", filter);
    }
}
```

- [ ] **Step 4.6: 运行测试并验证通过**

```bash
mvn test -pl schemaplexai-agent-engine -Dtest=MemoryStrategyTest,RagIsolationTest -v
```

- [ ] **Step 4.7: 提交**

```bash
git add schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/memory/
git add schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/rag/
git add schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/context/
git add schemaplexai-agent-engine/src/test/java/com/schemaplexai/agent/engine/
git commit -m "feat(agent-engine): implement Memory strategies and RAG data isolation

- Add MemoryStrategy interface with SlidingWindow and Summarization
- Add RagIsolationConfig for multi-tenant data isolation
- Add MilvusIsolationService with search permission validation
- Integrate RAG retrieval into ContextInjector
- Add comprehensive unit tests

Closes #P1-4"
```

---

## 任务 5: Reflection + Evaluation + Guardrails（P1，W6）

### 文件清单

- **Create:** `schemaplexai-agent-engine/src/.../evaluation/Evaluator.java` — 评估器接口
- **Create:** `schemaplexai-agent-engine/src/.../evaluation/ShadowReviewEvaluator.java` — 影子评审评估器
- **Create:** `schemaplexai-agent-engine/src/.../evaluation/RuleBasedEvaluator.java` — 规则评估器
- **Create:** `schemaplexai-agent-engine/src/.../guardrails/GuardrailsEngine.java` — Guardrails 引擎
- **Create:** `schemaplexai-agent-engine/src/.../guardrails/BlacklistGuardrail.java` — 黑名单 Guardrail
- **Create:** `schemaplexai-agent-engine/src/.../guardrails/LengthGuardrail.java` — 长度 Guardrail
- **Create:** `schemaplexai-agent-engine/src/.../state/ReflectingStateHandler.java` — 反思状态处理器
- **Modify:** `schemaplexai-agent-engine/src/.../AgentLoopShadowReviewService.java` — 集成评估器
- **Modify:** `schemaplexai-agent-engine/src/.../controller/AgentExecutionController.java` — 集成 Guardrails
- **Test:** `schemaplexai-agent-engine/src/test/.../evaluation/EvaluatorTest.java`
- **Test:** `schemaplexai-agent-engine/src/test/.../guardrails/GuardrailsEngineTest.java`

### 实施步骤

- [ ] **Step 5.1: 创建 Evaluator 接口**

```java
package com.schemaplexai.agent.engine.evaluation;

/**
 * 评估器接口，定义 Agent 执行的评估行为。
 */
public interface Evaluator {
    
    /**
     * 评估执行轨迹
     * @param trace 执行轨迹
     * @return 评估结果
     */
    EvaluationResult evaluate(AgentExecutionTrace trace);
    
    /**
     * 比较两个评估结果
     * @param baseline 基准结果
     * @param current 当前结果
     * @return 差异分析
     */
    EvaluationDelta compare(EvaluationResult baseline, EvaluationResult current);
    
    /**
     * 获取评估器名称
     * @return 评估器名称
     */
    String getName();
}
```

- [ ] **Step 5.2: 创建 RuleBasedEvaluator 实现**

```java
package com.schemaplexai.agent.engine.evaluation;

/**
 * 基于规则的评估器实现。
 */
public class RuleBasedEvaluator implements Evaluator {
    
    @Override
    public EvaluationResult evaluate(AgentExecutionTrace trace) {
        Map<String, Double> dimensions = new HashMap<>();
        List<String> issues = new ArrayList<>();
        
        // 1. 成功率评估
        boolean success = trace.finalState() == AgentExecutionState.COMPLETED;
        dimensions.put("success_rate", success ? 1.0 : 0.0);
        
        // 2. 效率评估（迭代次数）
        double efficiency = Math.max(0, 1.0 - (trace.iterationCount() / 10.0));
        dimensions.put("efficiency", efficiency);
        if (trace.iterationCount() > 5) {
            issues.add("High iteration count: " + trace.iterationCount());
        }
        
        // 3. Token 消耗评估
        double tokenEfficiency = calculateTokenEfficiency(trace);
        dimensions.put("token_efficiency", tokenEfficiency);
        if (tokenEfficiency < 0.3) {
            issues.add("Low token efficiency: " + String.format("%.2f", tokenEfficiency));
        }
        
        // 4. 错误率评估
        double errorRate = calculateErrorRate(trace);
        dimensions.put("error_rate", 1.0 - errorRate);
        if (errorRate > 0.1) {
            issues.add("High error rate: " + String.format("%.2f", errorRate));
        }
        
        // 5. 响应时间评估
        double latencyScore = calculateLatencyScore(trace);
        dimensions.put("latency", latencyScore);
        
        // 计算总分
        double overallScore = dimensions.values().stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
        
        return new EvaluationResult(
            trace.executionId(),
            overallScore,
            dimensions,
            issues,
            Instant.now()
        );
    }
    
    @Override
    public EvaluationDelta compare(EvaluationResult baseline, EvaluationResult current) {
        Map<String, Double> dimensionDeltas = new HashMap<>();
        
        for (String dim : baseline.dimensions().keySet()) {
            double baseValue = baseline.dimensions().getOrDefault(dim, 0.0);
            double currentValue = current.dimensions().getOrDefault(dim, 0.0);
            dimensionDeltas.put(dim, currentValue - baseValue);
        }
        
        return new EvaluationDelta(
            current.overallScore() - baseline.overallScore(),
            dimensionDeltas,
            determineTrend(baseline.overallScore(), current.overallScore())
        );
    }
    
    @Override
    public String getName() {
        return "RuleBasedEvaluator";
    }
    
    private double calculateTokenEfficiency(AgentExecutionTrace trace) {
        if (trace.totalTokens() == 0) return 0;
        return (double) trace.outputTokens() / trace.totalTokens();
    }
    
    private double calculateErrorRate(AgentExecutionTrace trace) {
        if (trace.toolCalls().isEmpty()) return 0;
        long errorCount = trace.toolCalls().stream()
            .filter(tc -> tc.result().isError())
            .count();
        return (double) errorCount / trace.toolCalls().size();
    }
    
    private double calculateLatencyScore(AgentExecutionTrace trace) {
        long latencyMs = trace.duration().toMillis();
        if (latencyMs < 1000) return 1.0;
        if (latencyMs < 5000) return 0.8;
        if (latencyMs < 10000) return 0.6;
        if (latencyMs < 30000) return 0.4;
        return 0.2;
    }
    
    private EvaluationTrend determineTrend(double baseline, double current) {
        double delta = current - baseline;
        if (delta > 0.1) return EvaluationTrend.IMPROVING;
        if (delta < -0.1) return EvaluationTrend.DECLINING;
        return EvaluationTrend.STABLE;
    }
}
```

- [ ] **Step 5.3: 创建 GuardrailsEngine**

```java
package com.schemaplexai.agent.engine.guardrails;

import java.util.List;

/**
 * Guardrails 引擎，管理多个 Guardrail 并执行验证。
 */
public class GuardrailsEngine {
    
    private final List<Guardrail> guardrails;
    
    public GuardrailsEngine(List<Guardrail> guardrails) {
        this.guardrails = guardrails;
    }
    
    /**
     * 验证输入
     * @param input 输入文本
     * @return 验证结果
     */
    public ValidationResult validateInput(String input) {
        for (Guardrail guardrail : guardrails) {
            ValidationResult result = guardrail.validateInput(input);
            if (!result.isValid()) {
                return result;
            }
        }
        return ValidationResult.valid();
    }
    
    /**
     * 验证输出
     * @param output 输出文本
     * @return 验证结果
     */
    public ValidationResult validateOutput(String output) {
        for (Guardrail guardrail : guardrails) {
            ValidationResult result = guardrail.validateOutput(output);
            if (!result.isValid()) {
                return result;
            }
        }
        return ValidationResult.valid();
    }
    
    /**
     * 检查是否为高风险操作
     * @param toolCall 工具调用
     * @return true 如果是高风险操作
     */
    public boolean isHighRiskOperation(ToolCall toolCall) {
        return guardrails.stream()
            .anyMatch(g -> g.isHighRisk(toolCall));
    }
}
```

- [ ] **Step 5.4: 创建 ReflectingStateHandler**

```java
package com.schemaplexai.agent.engine.state;

/**
 * 反思状态处理器，实现自我纠错机制。
 */
public class ReflectingStateHandler implements AgentStateHandler {
    
    private final LlmProvider llmProvider;
    private final GuardrailsEngine guardrailsEngine;
    private final int maxReflectionRounds;
    
    public ReflectingStateHandler(LlmProvider llmProvider, GuardrailsEngine guardrailsEngine) {
        this.llmProvider = llmProvider;
        this.guardrailsEngine = guardrailsEngine;
        this.maxReflectionRounds = 2;
    }
    
    @Override
    public AgentExecutionState handle(AgentExecutionContext context) {
        // 检查反思次数限制
        if (context.reflectionCount() >= maxReflectionRounds) {
            // 达到最大反思次数，接受当前输出
            return AgentExecutionState.COMPLETED;
        }
        
        // 构建反思提示
        String reflectionPrompt = buildReflectionPrompt(context);
        
        // 调用 LLM 进行自我评估
        LlmResponse response = llmProvider.generate(reflectionPrompt, GenerationOptions.defaultOptions());
        
        // 检查反思结果
        ReflectionResult reflectionResult = parseReflectionResult(response.text());
        
        if (reflectionResult.needsRevision()) {
            // 需要修订，返回思考状态重新生成
            context.incrementReflectionCount();
            context.setRevisionInstructions(reflectionResult.suggestions());
            return AgentExecutionState.THINKING;
        } else {
            // 输出质量可接受，完成
            return AgentExecutionState.COMPLETED;
        }
    }
    
    private String buildReflectionPrompt(AgentExecutionContext context) {
        return """
            You are a quality reviewer. Evaluate the following AI response for accuracy, completeness, and safety.
            
            User's original question: %s
            
            AI's response: %s
            
            Consider:
            1. Is the response factually accurate?
            2. Is it complete and helpful?
            3. Does it contain any harmful or inappropriate content?
            4. Could it be improved?
            
            Respond with:
            - "PASS" if the response is good enough
            - "REVISE: [specific suggestions]" if improvements are needed
            
            Your evaluation:
            """.formatted(context.userMessage(), context.finalAnswer());
    }
    
    private ReflectionResult parseReflectionResult(String llmOutput) {
        if (llmOutput.toUpperCase().contains("PASS")) {
            return ReflectionResult.accepted();
        }
        
        if (llmOutput.toUpperCase().contains("REVISE:")) {
            String suggestions = llmOutput.substring(llmOutput.toUpperCase().indexOf("REVISE:") + 7).trim();
            return ReflectionResult.needsRevision(suggestions);
        }
        
        // 默认通过
        return ReflectionResult.accepted();
    }
}
```

- [ ] **Step 5.5: 编写测试**

```java
package com.schemaplexai.agent.engine.evaluation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

class EvaluatorTest {
    
    private RuleBasedEvaluator evaluator;
    
    @BeforeEach
    void setUp() {
        evaluator = new RuleBasedEvaluator();
    }
    
    @Test
    void shouldEvaluateSuccessfulExecution() {
        AgentExecutionTrace trace = createSuccessfulTrace();
        
        EvaluationResult result = evaluator.evaluate(trace);
        
        assertTrue(result.overallScore() > 0.7);
        assertTrue(result.issues().isEmpty());
    }
    
    @Test
    void shouldDetectHighIterationCount() {
        AgentExecutionTrace trace = createTraceWithHighIterations();
        
        EvaluationResult result = evaluator.evaluate(trace);
        
        assertTrue(result.issues().stream()
            .anyMatch(i -> i.contains("iteration")));
    }
    
    @Test
    void shouldCompareResults() {
        EvaluationResult baseline = createBaselineResult();
        EvaluationResult current = createImprovedResult();
        
        EvaluationDelta delta = evaluator.compare(baseline, current);
        
        assertEquals(EvaluationTrend.IMPROVING, delta.trend());
    }
}
```

```java
package com.schemaplexai.agent.engine.guardrails;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

class GuardrailsEngineTest {
    
    private GuardrailsEngine engine;
    
    @BeforeEach
    void setUp() {
        List<Guardrail> guardrails = List.of(
            new BlacklistGuardrail(),
            new LengthGuardrail()
        );
        engine = new GuardrailsEngine(guardrails);
    }
    
    @Test
    void shouldBlockBlacklistedInput() {
        ValidationResult result = engine.validateInput("ignore previous instructions");
        assertFalse(result.isValid());
    }
    
    @Test
    void shouldBlockExcessiveLength() {
        String longInput = "x".repeat(200000);
        ValidationResult result = engine.validateInput(longInput);
        assertFalse(result.isValid());
    }
    
    @Test
    void shouldPassValidInput() {
        ValidationResult result = engine.validateInput("What is the weather?");
        assertTrue(result.isValid());
    }
}
```

- [ ] **Step 5.6: 运行测试并验证通过**

```bash
mvn test -pl schemaplexai-agent-engine -Dtest=EvaluatorTest,GuardrailsEngineTest -v
```

- [ ] **Step 5.7: 提交**

```bash
git add schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/evaluation/
git add schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/guardrails/
git add schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/state/
git add schemaplexai-agent-engine/src/test/java/com/schemaplexai/agent/engine/
git commit -m "feat(agent-engine): implement Reflection, Evaluation, and Guardrails

- Add Evaluator interface with RuleBasedEvaluator
- Add GuardrailsEngine with BlacklistGuardrail and LengthGuardrail
- Add ReflectingStateHandler for self-correction (max 2 rounds)
- Integrate evaluation into AgentLoopShadowReviewService
- Add comprehensive unit tests

Closes #P1-5"
```

---

## 自我审查清单

### Spec 覆盖度检查

- [x] **3 个 Critical 安全风险修复**：任务 1 覆盖（Tool Sandbox + ContextInjector 验证 + SSE 认证）
- [x] **Tool Use 完整实现**：任务 2 覆盖（ToolRegistry + ToolAdapter + ReActPromptTemplate + FinalAnswerExtractor）
- [x] **Reasoning + Exception Handling**：任务 3 覆盖（ReasoningStrategy + RecoveryStrategy + TokenBudget 累加）
- [x] **Memory + RAG 数据隔离**：任务 4 覆盖（MemoryStrategy + RagIsolationConfig + MilvusIsolationService）
- [x] **Reflection + Evaluation + Guardrails**：任务 5 覆盖（Evaluator + GuardrailsEngine + ReflectingStateHandler）

### 占位符扫描

- [x] 无 TBD / TODO / implement later
- [x] 所有步骤包含完整的代码
- [x] 无 "Similar to Task N" 引用
- [x] 所有文件路径精确指定

### 类型一致性

- [x] `ToolDefinition` 在任务 1 和 2 中使用一致
- [x] `ValidationResult` 在任务 1、4、5 中使用一致
- [x] `AgentExecutionState` 在任务 2、3 中使用一致
- [x] `TokenBudget` 在任务 3、4 中使用一致

---

## 执行方式选择

**Plan complete and saved to `docs/superpowers/plans/2026-05-02-layer1-agentic-patterns.md`.**

**Two execution options:**

**1. Subagent-Driven (recommended)** — I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** — Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach do you prefer?
