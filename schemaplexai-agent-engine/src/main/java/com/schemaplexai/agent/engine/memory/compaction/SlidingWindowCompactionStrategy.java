package com.schemaplexai.agent.engine.memory.compaction;

import com.schemaplexai.agent.engine.admission.TokenBudget;
import com.schemaplexai.agent.engine.model.LlmMessage;
import com.schemaplexai.agent.engine.util.TokenEstimator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SlidingWindowCompactionStrategy implements CompactionStrategy {

    @Override
    public String getName() {
        return "sliding_window";
    }

    @Override
    public CompactionResult compact(String conversationId, List<LlmMessage> messages, TokenBudget budget) {
        if (budget == null) {
            return CompactionResult.noop();
        }
        long currentTokens = TokenEstimator.estimate(messages);
        if (currentTokens <= budget.remainingInput()) {
            return CompactionResult.noop();
        }

        List<LlmMessage> systemMessages = messages.stream()
            .filter(m -> "system".equals(m.getRole()))
            .toList();
        List<LlmMessage> nonSystemMessages = messages.stream()
            .filter(m -> !"system".equals(m.getRole()))
            .toList();

        int low = 0;
        int high = nonSystemMessages.size();
        while (low < high) {
            int mid = (low + high) / 2;
            List<LlmMessage> candidate = new ArrayList<>(systemMessages);
            candidate.addAll(nonSystemMessages.subList(mid, nonSystemMessages.size()));
            if (TokenEstimator.estimate(candidate) <= budget.remainingInput()) {
                high = mid;
            } else {
                low = mid + 1;
            }
        }

        List<LlmMessage> result = new ArrayList<>(systemMessages);
        result.addAll(nonSystemMessages.subList(low, nonSystemMessages.size()));
        return CompactionResult.success(result, getName());
    }
}
