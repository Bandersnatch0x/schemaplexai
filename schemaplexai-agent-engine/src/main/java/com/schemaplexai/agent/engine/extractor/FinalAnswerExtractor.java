package com.schemaplexai.agent.engine.extractor;

import com.schemaplexai.agent.engine.tool.ToolCall;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从 LLM 输出中提取 ReAct 格式的结构化信息：
 * - Final Answer（最终答案）
 * - Thought（推理过程）
 * - Action + Action Input（工具调用）
 */
public class FinalAnswerExtractor {

    // 匹配 "Final Answer: <content>" 直到行尾或下一个标签
    private static final Pattern FINAL_ANSWER_PATTERN =
            Pattern.compile("Final\\s+Answer\\s*:\\s*(.+?)(?=\\n(?:Thought|Action|Observation)|$)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    // 匹配 "Thought: <content>" 直到下一个 Action/Observation/Final Answer
    private static final Pattern THOUGHT_PATTERN =
            Pattern.compile("Thought\\s*:\\s*(.+?)(?=\\n(?:Action|Observation|Final\\s+Answer)|$)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    // 匹配 "Action: <tool_name>\nAction Input: <json>"
    private static final Pattern ACTION_PATTERN =
            Pattern.compile("Action\\s*:\\s*(.+?)\\s*\\n+Action\\s+Input\\s*:\\s*(.+?)(?=\\n(?:Thought|Action|Observation|Final\\s+Answer)|$)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    // 备用：匹配单独出现的 Action
    private static final Pattern ACTION_ONLY_PATTERN =
            Pattern.compile("Action\\s*:\\s*(.+?)\\s*$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    // 用于解析 JSON 中 tool 名称的简化模式
    private static final Pattern TOOL_NAME_JSON_PATTERN =
            Pattern.compile("\"tool\"\\s*:\\s*\"(.+?)\"", Pattern.CASE_INSENSITIVE);

    /**
     * 从 LLM 输出中提取最终答案。
     *
     * @param llmOutput LLM 的原始输出文本
     * @return 提取的最终答案，未找到时返回 null
     */
    public String extractFinalAnswer(String llmOutput) {
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
     * 检查 LLM 输出是否包含 Final Answer。
     */
    public boolean hasFinalAnswer(String llmOutput) {
        return extractFinalAnswer(llmOutput) != null;
    }

    /**
     * 从 LLM 输出中提取所有 Thought。
     *
     * @param llmOutput LLM 的原始输出文本
     * @return 所有的 Thought 内容列表
     */
    public List<String> extractThoughts(String llmOutput) {
        if (llmOutput == null || llmOutput.isBlank()) {
            return List.of();
        }
        List<String> thoughts = new ArrayList<>();
        Matcher matcher = THOUGHT_PATTERN.matcher(llmOutput);
        while (matcher.find()) {
            String thought = matcher.group(1).trim();
            if (!thought.isEmpty()) {
                thoughts.add(thought);
            }
        }
        return thoughts;
    }

    /**
     * 提取最后一个 Thought。
     */
    public String extractLastThought(String llmOutput) {
        List<String> thoughts = extractThoughts(llmOutput);
        return thoughts.isEmpty() ? null : thoughts.get(thoughts.size() - 1);
    }

    /**
     * 从 LLM 输出中提取 Action + Action Input，构造为 ToolCall 对象。
     *
     * @param llmOutput LLM 的原始输出文本
     * @return 提取的 ToolCall，未找到时返回 null
     */
    public ToolCall extractToolCall(String llmOutput) {
        if (llmOutput == null || llmOutput.isBlank()) {
            return null;
        }

        // 尝试匹配 "Action: ... \n Action Input: ..." 格式
        Matcher matcher = ACTION_PATTERN.matcher(llmOutput);
        if (matcher.find()) {
            String toolName = matcher.group(1).trim();
            String actionInputRaw = matcher.group(2).trim();
            Map<String, Object> parameters = parseActionInput(actionInputRaw);
            return new ToolCall(toolName, parameters);
        }

        // 备用：尝试解析仅含 Action 的单行格式
        Matcher actionOnlyMatcher = ACTION_ONLY_PATTERN.matcher(llmOutput);
        if (actionOnlyMatcher.find()) {
            String toolName = actionOnlyMatcher.group(1).trim();
            // 尝试从上下文中提取工具名的 JSON
            Matcher jsonMatcher = TOOL_NAME_JSON_PATTERN.matcher(llmOutput);
            if (jsonMatcher.find()) {
                toolName = jsonMatcher.group(1).trim();
            }
            return new ToolCall(toolName, Map.of());
        }

        return null;
    }

    /**
     * 提取所有 ToolCall（按顺序）。
     */
    public List<ToolCall> extractAllToolCalls(String llmOutput) {
        if (llmOutput == null || llmOutput.isBlank()) {
            return List.of();
        }
        List<ToolCall> calls = new ArrayList<>();
        Matcher matcher = ACTION_PATTERN.matcher(llmOutput);
        while (matcher.find()) {
            String toolName = matcher.group(1).trim();
            String actionInputRaw = matcher.group(2).trim();
            Map<String, Object> parameters = parseActionInput(actionInputRaw);
            calls.add(new ToolCall(toolName, parameters));
        }
        return calls;
    }

    /**
     * 将 Action Input 字符串解析为参数 Map。
     * 支持 JSON 格式，解析失败时返回整个字符串作为 raw_input。
     */
    private Map<String, Object> parseActionInput(String actionInputRaw) {
        if (actionInputRaw == null || actionInputRaw.isBlank()) {
            return Map.of();
        }
        String trimmed = actionInputRaw.trim();
        // 尝试解析为 JSON
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
            (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            try {
                return parseJsonToMap(trimmed);
            } catch (Exception e) {
                // JSON 解析失败，返回原始输入
            }
        }
        return Map.of("input", trimmed);
    }

    /**
     * 简易 JSON Object 解析为 Map（不依赖第三方库）。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonToMap(String json) {
        Map<String, Object> result = new LinkedHashMap<>();
        String content = json.trim();
        if (!content.startsWith("{") || !content.endsWith("}")) {
            return result;
        }
        content = content.substring(1, content.length() - 1).trim();
        if (content.isEmpty()) {
            return result;
        }

        // 按逗号分割（简单实现，处理嵌套引号）
        String[] pairs = content.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        for (String pair : pairs) {
            String[] kv = pair.split(":(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", 2);
            if (kv.length == 2) {
                String key = kv[0].trim().replaceAll("^\"|\"$", "");
                String valueStr = kv[1].trim();
                Object value = parseJsonValue(valueStr);
                result.put(key, value);
            }
        }
        return result;
    }

    /**
     * 解析 JSON 值（字符串、数字、布尔、null、嵌套对象）。
     */
    private Object parseJsonValue(String valueStr) {
        if (valueStr == null || valueStr.isEmpty()) {
            return null;
        }
        valueStr = valueStr.trim();
        if (valueStr.startsWith("\"") && valueStr.endsWith("\"")) {
            return valueStr.substring(1, valueStr.length() - 1);
        }
        if (valueStr.startsWith("{") && valueStr.endsWith("}")) {
            return parseJsonToMap(valueStr);
        }
        if ("true".equalsIgnoreCase(valueStr)) {
            return true;
        }
        if ("false".equalsIgnoreCase(valueStr)) {
            return false;
        }
        if ("null".equalsIgnoreCase(valueStr)) {
            return null;
        }
        try {
            if (valueStr.contains(".")) {
                return Double.parseDouble(valueStr);
            }
            return Long.parseLong(valueStr);
        } catch (NumberFormatException e) {
            return valueStr;
        }
    }
}
