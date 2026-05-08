package com.schemaplexai.agent.engine.state;

import com.schemaplexai.agent.engine.context.ContextInjector;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.guardrails.GuardrailsEngine;
import com.schemaplexai.agent.engine.loop.AgentLoopDetectionService;
import com.schemaplexai.agent.engine.memory.CompositeChatMemoryStore;
import com.schemaplexai.agent.engine.memory.compaction.AutoCompactionService;
import com.schemaplexai.agent.engine.model.AiModelRouter;
import com.schemaplexai.agent.engine.model.ModelResolver;
import com.schemaplexai.agent.engine.role.RoleOverlay;
import com.schemaplexai.agent.engine.role.RoleRegistry;
import com.schemaplexai.agent.engine.skill.SkillDefinition;
import com.schemaplexai.agent.engine.skill.SkillRegistry;
import com.schemaplexai.agent.engine.tool.ToolRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ThinkingStateHandlerSkillInjectionTest {

    @Mock
    private ContextInjector contextInjector;

    @Mock
    private CompositeChatMemoryStore chatMemoryStore;

    @Mock
    private AiModelRouter modelRouter;

    @Mock
    private AgentLoopDetectionService loopDetection;

    @Mock
    private ModelResolver modelResolver;

    @Mock
    private GuardrailsEngine guardrailsEngine;

    @Mock
    private SkillRegistry skillRegistry;

    @Mock
    private RoleRegistry roleRegistry;

    @Mock
    private AutoCompactionService autoCompactionService;

    @Mock
    private ToolRegistry toolRegistry;

    private ThinkingStateHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ThinkingStateHandler(
                contextInjector,
                chatMemoryStore,
                modelRouter,
                loopDetection,
                modelResolver,
                guardrailsEngine,
                skillRegistry,
                roleRegistry,
                autoCompactionService,
                toolRegistry
        );
    }

    private SfAgentExecution createExecution() {
        SfAgentExecution e = new SfAgentExecution();
        e.setId(1L);
        return e;
    }

    @Test
    void shouldInjectRoleOverlayIntoPrompt() throws Exception {
        when(roleRegistry.resolve("senior-dev", "tenant-1"))
                .thenReturn(new RoleOverlay("senior-dev", "Senior developer", "You are a senior developer."));

        Method buildPromptMethod = ThinkingStateHandler.class.getDeclaredMethod(
                "buildPrompt", List.class, String.class, String.class, String.class, SfAgentExecution.class);
        buildPromptMethod.setAccessible(true);

        String prompt = (String) buildPromptMethod.invoke(
                handler,
                List.of(),
                "tenant-1",
                "senior-dev",
                null,
                createExecution()
        );

        assertThat(prompt).contains("# Role: senior-dev");
        assertThat(prompt).contains("You are a senior developer.");
    }

    @Test
    void shouldInjectSkillInstructionsIntoPrompt() throws Exception {
        when(skillRegistry.resolveByTier("code-reviewer", "tenant-1", 1))
                .thenReturn(new SkillDefinition("code-reviewer", "Code reviewer", "Review code for quality.", 1));
        when(skillRegistry.resolveAvailable("tenant-1", 1)).thenReturn(List.of());

        Method buildPromptMethod = ThinkingStateHandler.class.getDeclaredMethod(
                "buildPrompt", List.class, String.class, String.class, String.class, SfAgentExecution.class);
        buildPromptMethod.setAccessible(true);

        String prompt = (String) buildPromptMethod.invoke(
                handler,
                List.of(),
                "tenant-1",
                null,
                "code-reviewer",
                createExecution()
        );

        assertThat(prompt).contains("# Skill: code-reviewer");
        assertThat(prompt).contains("Review code for quality.");
    }

    @Test
    void shouldInjectBothRoleAndSkill() throws Exception {
        when(roleRegistry.resolve("senior-dev", "tenant-1"))
                .thenReturn(new RoleOverlay("senior-dev", "Senior", "You are a senior developer."));
        when(skillRegistry.resolveByTier("code-reviewer", "tenant-1", 1))
                .thenReturn(new SkillDefinition("code-reviewer", "Reviewer", "Review code.", 1));
        when(skillRegistry.resolveAvailable("tenant-1", 1)).thenReturn(List.of());

        Method buildPromptMethod = ThinkingStateHandler.class.getDeclaredMethod(
                "buildPrompt", List.class, String.class, String.class, String.class, SfAgentExecution.class);
        buildPromptMethod.setAccessible(true);

        String prompt = (String) buildPromptMethod.invoke(
                handler,
                List.of(),
                "tenant-1",
                "senior-dev",
                "code-reviewer",
                createExecution()
        );

        assertThat(prompt).contains("# Role: senior-dev");
        assertThat(prompt).contains("# Skill: code-reviewer");
        assertThat(prompt).contains("You are a senior developer.");
        assertThat(prompt).contains("Review code.");
    }

    @Test
    void shouldReturnEmptyPromptWhenNoRoleOrSkillAndNoMessages() throws Exception {
        Method buildPromptMethod = ThinkingStateHandler.class.getDeclaredMethod(
                "buildPrompt", List.class, String.class, String.class, String.class, SfAgentExecution.class);
        buildPromptMethod.setAccessible(true);

        String prompt = (String) buildPromptMethod.invoke(
                handler,
                List.of(),
                "tenant-1",
                null,
                null,
                createExecution()
        );

        assertThat(prompt).isEmpty();
    }

    @Test
    void shouldReturnOriginalPromptWhenNoRoleOrSkill() throws Exception {
        Method buildPromptMethod = ThinkingStateHandler.class.getDeclaredMethod(
                "buildPrompt", List.class, String.class, String.class, String.class, SfAgentExecution.class);
        buildPromptMethod.setAccessible(true);

        String prompt = (String) buildPromptMethod.invoke(
                handler,
                List.of(new com.schemaplexai.agent.engine.model.LlmMessage("user", "Hello")),
                "tenant-1",
                null,
                null,
                createExecution()
        );

        assertThat(prompt).isEqualTo("user: Hello\n");
    }

    @Test
    void shouldSkipRoleWhenRegistryReturnsNull() throws Exception {
        when(roleRegistry.resolve("unknown-role", "tenant-1")).thenReturn(null);

        Method buildPromptMethod = ThinkingStateHandler.class.getDeclaredMethod(
                "buildPrompt", List.class, String.class, String.class, String.class, SfAgentExecution.class);
        buildPromptMethod.setAccessible(true);

        String prompt = (String) buildPromptMethod.invoke(
                handler,
                List.of(new com.schemaplexai.agent.engine.model.LlmMessage("user", "Hello")),
                "tenant-1",
                "unknown-role",
                null,
                createExecution()
        );

        assertThat(prompt).doesNotContain("# Role:");
        assertThat(prompt).isEqualTo("user: Hello\n");
    }

    @Test
    void shouldSkipSkillWhenRegistryReturnsNull() throws Exception {
        when(skillRegistry.resolveByTier("unknown-skill", "tenant-1", 1)).thenReturn(null);
        when(skillRegistry.resolveAvailable("tenant-1", 1)).thenReturn(List.of());

        Method buildPromptMethod = ThinkingStateHandler.class.getDeclaredMethod(
                "buildPrompt", List.class, String.class, String.class, String.class, SfAgentExecution.class);
        buildPromptMethod.setAccessible(true);

        String prompt = (String) buildPromptMethod.invoke(
                handler,
                List.of(new com.schemaplexai.agent.engine.model.LlmMessage("user", "Hello")),
                "tenant-1",
                null,
                "unknown-skill",
                createExecution()
        );

        assertThat(prompt).doesNotContain("# Skill:");
        assertThat(prompt).isEqualTo("user: Hello\n");
    }
}
