package com.schemaplexai.agent.engine.prompt;

import com.schemaplexai.agent.engine.tool.ToolDefinition;
import com.schemaplexai.agent.engine.tool.ToolRegistry;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ReAct (Reasoning + Acting) 格式的 System Prompt 构建器。
 * 生成 Thought → Action → Action Input → Observation 格式的提示词。
 */
public class ReActPromptTemplate {

    private static final String DEFAULT_PROMPT_HEADER = """
            You are an AI assistant designed to solve problems step by step using the ReAct framework.
            You have access to the following tools. Use them when needed to gather information or take actions.

            """;

    private static final String REACT_FORMAT = """
            Follow this format strictly:

            Thought: <your reasoning about what to do next>
            Action: <the tool name to invoke, must be one of the available tools>
            Action Input: <the input parameters for the tool in JSON format>

            After a tool returns a result, you will see:
            Observation: <the result of the tool execution>

            ... (this Thought/Action/Action Input/Observation cycle can repeat N times)

            When you have the final answer, you MUST respond with:
            Thought: I now know the final answer
            Final Answer: <your final answer to the user's question>

            Begin!
            """;

    private static final int DEFAULT_MAX_ITERATIONS = 10;

    private final ToolRegistry toolRegistry;
    private final int maxIterations;

    public ReActPromptTemplate(ToolRegistry toolRegistry) {
        this(toolRegistry, DEFAULT_MAX_ITERATIONS);
    }

    public ReActPromptTemplate(ToolRegistry toolRegistry, int maxIterations) {
        this.toolRegistry = toolRegistry;
        this.maxIterations = maxIterations > 0 ? maxIterations : DEFAULT_MAX_ITERATIONS;
    }

    /**
     * 构建完整的 ReAct system prompt，包含工具列表和格式说明。
     */
    public String buildSystemPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append(DEFAULT_PROMPT_HEADER);

        // 工具列表
        sb.append(buildToolsSection());
        sb.append("\n");
        sb.append(buildConstraintsSection());
        sb.append("\n");
        sb.append(REACT_FORMAT);

        return sb.toString();
    }

    /**
     * 只构建工具列表部分的 prompt。
     */
    public String buildToolsSection() {
        List<ToolDefinition> tools = toolRegistry.getAll();
        if (tools.isEmpty()) {
            return "No tools are currently available.\n";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Available tools:\n\n");
        for (ToolDefinition tool : tools) {
            sb.append(formatTool(tool));
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * 构建约束条件部分的 prompt，包含最大迭代次数限制。
     */
    public String buildConstraintsSection() {
        return String.format(
                "Constraints:\n" +
                "- You MUST use exactly the tool names listed above. Do not invent tool names.\n" +
                "- You MUST respond with either an Action or a Final Answer in each response.\n" +
                "- You MUST provide Action Input as valid JSON.\n" +
                "- Maximum iterations: %d. If you haven't found an answer after %d iterations, provide your best guess as Final Answer.\n" +
                "- All tool calls and reasoning MUST be in the ReAct format exactly as specified.\n",
                maxIterations, maxIterations
        );
    }

    /**
     * 格式化单个工具定义为人可读的描述。
     */
    private String formatTool(ToolDefinition tool) {
        StringBuilder sb = new StringBuilder();
        sb.append("- ").append(tool.name()).append(": ").append(tool.description()).append("\n");

        if (tool.parameters() != null && !tool.parameters().isEmpty()) {
            sb.append("  Parameters:\n");
            for (var param : tool.parameters()) {
                sb.append(String.format("    - %s (%s)%s: %s\n",
                        param.name(),
                        param.type(),
                        param.required() ? ", required" : "",
                        param.description()));
            }
        }

        if (tool.returnType() != null && !tool.returnType().isBlank()) {
            sb.append(String.format("  Returns: %s\n", tool.returnType()));
        }

        return sb.toString();
    }

    /**
     * 将工具列表格式化为简化的字符串描述（用于注入到已有 prompt 中）。
     */
    public String buildToolListString() {
        List<ToolDefinition> tools = toolRegistry.getAll();
        if (tools.isEmpty()) {
            return "";
        }
        return tools.stream()
                .map(t -> String.format("- %s: %s", t.name(), t.description()))
                .collect(Collectors.joining("\n"));
    }

    public int getMaxIterations() {
        return maxIterations;
    }
}
