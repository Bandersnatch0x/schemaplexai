package com.schemaplexai.agent.engine.memory.compaction;

import com.schemaplexai.agent.engine.admission.TokenBudget;
import com.schemaplexai.agent.engine.memory.ConversationFileTracker;
import com.schemaplexai.agent.engine.model.AiModelRouter;
import com.schemaplexai.agent.engine.model.LlmMessage;
import com.schemaplexai.agent.engine.observability.PiiRedactor;
import com.schemaplexai.agent.engine.skill.SkillRegistry;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class SummarizationCompactionStrategy implements CompactionStrategy {

    private static final int MAX_FILES_TO_RESTORE = 5;
    private static final long FILE_TOKEN_BUDGET = 50_000;

    private final AiModelRouter modelRouter;
    private final ConversationFileTracker fileTracker;
    private final SkillRegistry skillRegistry;

    public SummarizationCompactionStrategy(AiModelRouter modelRouter,
                                            ConversationFileTracker fileTracker,
                                            SkillRegistry skillRegistry) {
        this.modelRouter = modelRouter;
        this.fileTracker = fileTracker;
        this.skillRegistry = skillRegistry;
    }

    @Override
    public String getName() {
        return "summarization_with_restoration";
    }

    @Override
    public CompactionResult compact(String conversationId, List<LlmMessage> messages, TokenBudget budget) {
        // 1. PII redaction
        List<LlmMessage> sanitized = messages != null ? messages.stream()
            .map(m -> {
                LlmMessage copy = new LlmMessage();
                copy.setRole(m.getRole());
                copy.setContent(PiiRedactor.redact(m.getContent()));
                return copy;
            })
            .toList() : List.of();

        // 2. Generate summary via LLM
        String summary = generateSummary(sanitized);

        // 3. Post-compact restore: summary + recent files + active skills
        List<LlmMessage> restored = new ArrayList<>();
        restored.add(new LlmMessage("system", summary));
        restored.addAll(recentFileContext(conversationId));
        restored.addAll(activeSkillContext(conversationId));

        return CompactionResult.success(restored, getName());
    }

    private String generateSummary(List<LlmMessage> messages) {
        String conversationText = messages.stream()
            .map(m -> m.getRole() + ": " + (m.getContent() != null ? m.getContent() : ""))
            .collect(Collectors.joining("\n"));
        String prompt = "Summarize the following conversation concisely, preserving key decisions and context:\n\n" + conversationText;
        return modelRouter.generateWithFallback(prompt, null, 0.3);
    }

    private List<LlmMessage> recentFileContext(String conversationId) {
        List<String> files = fileTracker.getRecentFiles(conversationId, MAX_FILES_TO_RESTORE);
        if (files.isEmpty()) {
            return List.of();
        }
        String fileContext = "Recent files:\n" + files.stream()
            .map(f -> "- " + f)
            .collect(Collectors.joining("\n"));
        return List.of(new LlmMessage("system", fileContext));
    }

    private List<LlmMessage> activeSkillContext(String conversationId) {
        return List.of();
    }
}
