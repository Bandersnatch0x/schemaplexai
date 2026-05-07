package com.schemaplexai.agent.engine.memory.compaction;

import com.schemaplexai.agent.engine.admission.TokenBudget;
import com.schemaplexai.agent.engine.model.LlmMessage;

import java.util.List;

public interface CompactionStrategy {

    String getName();

    CompactionResult compact(String conversationId, List<LlmMessage> messages, TokenBudget budget);
}
