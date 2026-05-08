package com.schemaplexai.agent.engine.model;

import com.schemaplexai.agent.engine.tool.ToolDefinition;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.Response;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared utility for converting between {@link LlmMessage} and LangChain4j types.
 *
 * <p>Extracted from OpenAiProvider and AnthropicProvider to eliminate duplication.
 * New provider implementations can reuse this converter without coupling to the
 * full LangChain4j model creation API.
 */
public final class LlmMessageConverter {

    private LlmMessageConverter() {
        // utility class
    }

    /**
     * Converts our internal {@link LlmMessage} list to LangChain4j {@link ChatMessage} list.
     *
     * @param messages the internal messages
     * @return LangChain4j chat messages
     * @throws BaseException if a message has a null role
     */
    public static List<ChatMessage> toChatMessages(List<LlmMessage> messages) {
        if (messages == null) {
            return List.of();
        }
        return messages.stream()
                .map(LlmMessageConverter::toChatMessage)
                .toList();
    }

    /**
     * Converts a single {@link LlmMessage} to a LangChain4j {@link ChatMessage}.
     */
    public static ChatMessage toChatMessage(LlmMessage msg) {
        if (msg == null || msg.getRole() == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Message or role cannot be null");
        }
        String role = msg.getRole().toLowerCase();
        String content = msg.getContent() != null ? msg.getContent() : "";
        return switch (role) {
            case "system" -> new SystemMessage(content);
            case "assistant", "ai" -> new AiMessage(content);
            case "user", "human" -> new UserMessage(content);
            default -> new UserMessage(content);
        };
    }

    /**
     * Extracts text from a LangChain4j {@link Response}.
     */
    public static String extractText(Response<AiMessage> response) {
        if (response == null || response.content() == null) {
            return "";
        }
        return response.content().text() != null ? response.content().text() : "";
    }

    /**
     * Enriches the message list by prepending tool descriptions to the first system message,
     * or inserting a new system message if none exists.
     *
     * @param messages the original messages
     * @param tools    tool definitions to describe
     * @return enriched message list with tool descriptions
     */
    public static List<LlmMessage> enrichWithToolDescriptions(List<LlmMessage> messages,
                                                               List<ToolDefinition> tools) {
        if (tools == null || tools.isEmpty()) {
            return messages;
        }

        String toolSection = formatToolSection(tools);
        List<LlmMessage> enriched = new ArrayList<>(messages.size() + 1);

        boolean systemInjected = false;
        for (LlmMessage msg : messages) {
            if (!systemInjected && "system".equalsIgnoreCase(msg.getRole())) {
                enriched.add(new LlmMessage("system",
                        msg.getContent() + "\n\n" + toolSection));
                systemInjected = true;
            } else {
                enriched.add(msg);
            }
        }

        if (!systemInjected) {
            enriched.add(0, new LlmMessage("system", toolSection));
        }

        return enriched;
    }

    /**
     * Formats tool definitions into a prompt section.
     */
    public static String formatToolSection(List<ToolDefinition> tools) {
        StringBuilder sb = new StringBuilder();
        sb.append("Available tools:\n\n");
        for (ToolDefinition tool : tools) {
            sb.append("- ").append(tool.name()).append(": ").append(tool.description()).append("\n");
            if (tool.parameters() != null) {
                tool.parameters().forEach(p ->
                        sb.append(String.format("  - %s (%s)%s: %s%n",
                                p.name(), p.type(), p.required() ? ", required" : "", p.description())));
            }
        }
        sb.append("\nUse Thought/Action/Action Input format to invoke tools. ");
        sb.append("Use Final Answer when you have the answer.");
        return sb.toString();
    }
}
