package com.schemaplexai.agent.engine.memory.compaction;

import com.schemaplexai.agent.engine.admission.TokenBudget;
import com.schemaplexai.agent.engine.memory.CompositeChatMemoryStore;
import com.schemaplexai.agent.engine.model.LlmMessage;
import com.schemaplexai.agent.engine.util.TokenEstimator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AutoCompactionService {

    private static final int MAX_PTL_RETRIES = 3;
    private static final double TRUNCATE_HEAD_RATIO = 0.25;

    private final CompositeChatMemoryStore chatMemoryStore;
    private final ToolResultCompactionStrategy toolResultStrategy;
    private final SlidingWindowCompactionStrategy slidingWindowStrategy;
    private final SummarizationCompactionStrategy summarizationStrategy;

    public AutoCompactionService(CompositeChatMemoryStore chatMemoryStore,
                                  ToolResultCompactionStrategy toolResultStrategy,
                                  SlidingWindowCompactionStrategy slidingWindowStrategy,
                                  SummarizationCompactionStrategy summarizationStrategy) {
        this.chatMemoryStore = chatMemoryStore;
        this.toolResultStrategy = toolResultStrategy;
        this.slidingWindowStrategy = slidingWindowStrategy;
        this.summarizationStrategy = summarizationStrategy;
    }

    public CompactionResult compactIfNeeded(String conversationId, TokenBudget budget) {
        List<LlmMessage> messages = chatMemoryStore.loadMessages(conversationId);
        if (messages == null || messages.isEmpty()) {
            return CompactionResult.noop();
        }

        // Quick path: already within budget
        if (TokenEstimator.estimate(messages) <= budget.remainingInput()) {
            return CompactionResult.noop();
        }

        // Layer 0: Tool result compaction
        CompactionResult layer0Result = toolResultStrategy.compact(conversationId, messages, budget);
        if (layer0Result.success() && !layer0Result.noOp()) {
            List<LlmMessage> compacted = layer0Result.messages();
            if (TokenEstimator.estimate(compacted) <= budget.remainingInput()) {
                chatMemoryStore.replaceMessages(conversationId, compacted);
                return CompactionResult.success(compacted, layer0Result.strategyName());
            }
        }

        // If Layer 0 was noop, use original messages for Layer 1; otherwise use Layer 0 output
        List<LlmMessage> layer1Input = layer0Result.noOp() ? messages : layer0Result.messages();

        // Layer 1: Sliding window compaction
        CompactionResult layer1Result = slidingWindowStrategy.compact(conversationId, layer1Input, budget);
        if (layer1Result.success() && !layer1Result.noOp()) {
            List<LlmMessage> compacted = layer1Result.messages();
            if (TokenEstimator.estimate(compacted) <= budget.remainingInput()) {
                chatMemoryStore.replaceMessages(conversationId, compacted);
                return CompactionResult.success(compacted, layer1Result.strategyName());
            }
        }

        // If Layer 1 was noop, use layer1Input for Layer 2; otherwise use Layer 1 output
        List<LlmMessage> layer2Input = layer1Result.noOp() ? layer1Input : layer1Result.messages();

        // Layer 2: Summarization with PTL retry loop
        List<LlmMessage> currentMessages = new ArrayList<>(layer2Input);
        for (int attempt = 0; attempt < MAX_PTL_RETRIES; attempt++) {
            CompactionResult layer2Result = summarizationStrategy.compact(conversationId, currentMessages, budget);
            if (!layer2Result.success()) {
                // Summarization failed, truncate and retry
                currentMessages = truncateHead(currentMessages, TRUNCATE_HEAD_RATIO);
                continue;
            }

            List<LlmMessage> compacted = layer2Result.messages();
            if (TokenEstimator.estimate(compacted) <= budget.remainingInput()) {
                chatMemoryStore.replaceMessages(conversationId, compacted);
                return CompactionResult.success(compacted, layer2Result.strategyName());
            }

            // Still over budget, truncate and retry
            currentMessages = truncateHead(currentMessages, TRUNCATE_HEAD_RATIO);
        }

        return CompactionResult.failed("All compaction strategies exhausted");
    }

    private List<LlmMessage> truncateHead(List<LlmMessage> messages, double ratio) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        int truncateCount = (int) Math.ceil(messages.size() * ratio);
        truncateCount = Math.min(truncateCount, messages.size());
        return new ArrayList<>(messages.subList(truncateCount, messages.size()));
    }
}
