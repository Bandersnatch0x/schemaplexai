package com.schemaplexai.agent.engine.memory.compaction;

import com.schemaplexai.agent.engine.admission.TokenBudget;
import com.schemaplexai.agent.engine.model.LlmMessage;
import com.schemaplexai.agent.engine.util.TokenEstimator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ToolResultCompactionStrategy implements CompactionStrategy {

    private static final long MAX_INPUT_TOKENS = 120_000;
    private static final int KEEP_RECENT = 3;

    @Override
    public String getName() {
        return "tool_result_cleanup";
    }

    @Override
    public CompactionResult compact(String conversationId, List<LlmMessage> messages, TokenBudget budget) {
        long currentTokens = TokenEstimator.estimate(messages);
        if (currentTokens <= MAX_INPUT_TOKENS) {
            return CompactionResult.noop();
        }
        List<LlmMessage> compacted = clearOldToolResults(messages, KEEP_RECENT);
        return CompactionResult.success(compacted, getName());
    }

    private List<LlmMessage> clearOldToolResults(List<LlmMessage> messages, int keepRecent) {
        List<Integer> toolIndices = new ArrayList<>();
        for (int i = 0; i < messages.size(); i++) {
            if ("tool".equals(messages.get(i).getRole())) {
                toolIndices.add(i);
            }
        }

        List<LlmMessage> result = new ArrayList<>(messages.size());
        for (int i = 0; i < messages.size(); i++) {
            LlmMessage original = messages.get(i);
            if ("tool".equals(original.getRole())) {
                int toolRank = toolIndices.indexOf(i);
                int keepStart = Math.max(0, toolIndices.size() - keepRecent);
                if (toolRank >= keepStart) {
                    result.add(original);
                } else {
                    result.add(new LlmMessage(original.getRole(), "[cleared: tool result]"));
                }
            } else {
                result.add(original);
            }
        }
        return result;
    }
}
