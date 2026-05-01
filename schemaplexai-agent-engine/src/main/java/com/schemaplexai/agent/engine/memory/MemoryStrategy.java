package com.schemaplexai.agent.engine.memory;

import com.schemaplexai.agent.engine.admission.TokenBudget;

import java.util.List;

/**
 * Memory strategy interface.
 * Defines how conversation messages are selected and compressed
 * to fit within a given token budget.
 */
public interface MemoryStrategy {

    /**
     * Select messages that fit within the given token budget.
     *
     * @param messages all available messages
     * @param budget   token budget constraint
     * @return selected subset of messages
     */
    List<ChatMessage> select(List<ChatMessage> messages, TokenBudget budget);

    /**
     * Compress a set of messages into a summary.
     *
     * @param messages messages to compress
     * @return compressed memory with summary
     */
    CompressedMemory compress(List<ChatMessage> messages);

    /**
     * Get the strategy name.
     *
     * @return strategy name
     */
    String getName();
}
