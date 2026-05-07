package com.schemaplexai.agent.engine.memory.compaction;

import com.schemaplexai.agent.engine.admission.TokenBudget;
import com.schemaplexai.agent.engine.model.LlmMessage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SlidingWindowCompactionTest {

    private final SlidingWindowCompactionStrategy strategy = new SlidingWindowCompactionStrategy();

    @Test
    void testGetName() {
        assertEquals("sliding_window", strategy.getName());
    }

    @Test
    void testNullBudgetReturnsNoop() {
        List<LlmMessage> messages = List.of(
            new LlmMessage("system", "You are a helpful assistant.")
        );
        CompactionResult result = strategy.compact("conv-1", messages, null);
        assertTrue(result.noOp());
        assertTrue(result.success());
        assertNull(result.messages());
    }

    @Test
    void testWithinBudgetReturnsNoop() {
        TokenBudget budget = new TokenBudget(1000, 100);
        List<LlmMessage> messages = List.of(
            new LlmMessage("system", "You are a helpful assistant.")
        );
        CompactionResult result = strategy.compact("conv-1", messages, budget);
        assertTrue(result.noOp());
        assertTrue(result.success());
    }

    @Test
    void testTrimsOldestNonSystemMessages() {
        // Each message ~5 tokens (20 chars / 4)
        // System: 24 chars ~6 tokens, user: 20 chars ~5 tokens each
        TokenBudget budget = new TokenBudget(20, 100);
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(new LlmMessage("system", "You are a helpful assistant."));
        messages.add(new LlmMessage("user", "Message one is here now."));
        messages.add(new LlmMessage("user", "Message two is here now."));
        messages.add(new LlmMessage("user", "Message three is here."));
        messages.add(new LlmMessage("user", "Message four is here."));
        messages.add(new LlmMessage("assistant", "Message five is here."));

        CompactionResult result = strategy.compact("conv-1", messages, budget);
        assertFalse(result.noOp());
        assertTrue(result.success());
        assertNotNull(result.messages());
        assertEquals("sliding_window", result.strategyName());

        // Result should keep system message + some recent non-system messages
        List<LlmMessage> resultMessages = result.messages();
        assertTrue(resultMessages.stream().anyMatch(m -> "system".equals(m.getRole())));
        assertTrue(resultMessages.size() < messages.size());
    }

    @Test
    void testKeepsAllSystemMessages() {
        TokenBudget budget = new TokenBudget(15, 100);
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(new LlmMessage("system", "You are a helpful assistant."));
        messages.add(new LlmMessage("system", "Always be concise."));
        messages.add(new LlmMessage("user", "Message one is here now."));
        messages.add(new LlmMessage("user", "Message two is here now."));
        messages.add(new LlmMessage("assistant", "Reply to message two."));

        CompactionResult result = strategy.compact("conv-1", messages, budget);
        assertFalse(result.noOp());
        assertTrue(result.success());

        List<LlmMessage> resultMessages = result.messages();
        long systemCount = resultMessages.stream().filter(m -> "system".equals(m.getRole())).count();
        assertEquals(2, systemCount);
    }

    @Test
    void testEmptyMessagesReturnsNoop() {
        TokenBudget budget = new TokenBudget(100, 100);
        List<LlmMessage> messages = List.of();
        CompactionResult result = strategy.compact("conv-1", messages, budget);
        assertTrue(result.noOp());
        assertTrue(result.success());
    }

    @Test
    void testResultFitsWithinBudget() {
        TokenBudget budget = new TokenBudget(15, 100);
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(new LlmMessage("system", "You are a helpful assistant."));
        for (int i = 0; i < 10; i++) {
            messages.add(new LlmMessage("user", "Message number " + i + " here now."));
        }

        CompactionResult result = strategy.compact("conv-1", messages, budget);
        assertFalse(result.noOp());
        assertTrue(result.success());

        // Verify the result messages fit within budget
        long tokenSum = 0;
        for (LlmMessage msg : result.messages()) {
            tokenSum += Math.max(1, msg.getContent().length() / 4L);
        }
        assertTrue(tokenSum <= budget.remainingInput(),
            "Result tokens " + tokenSum + " should fit within budget " + budget.remainingInput());
    }
}
