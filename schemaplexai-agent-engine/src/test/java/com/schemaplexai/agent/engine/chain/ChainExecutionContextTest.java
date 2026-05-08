package com.schemaplexai.agent.engine.chain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ChainExecutionContextTest {

    @Nested
    class OutputStorage {

        @Test
        void shouldStoreAndRetrieveOutput() {
            ChainExecutionContext ctx = new ChainExecutionContext(Map.of());

            ctx.setOutput("step1", "hello world");

            assertThat(ctx.getOutput("step1")).isEqualTo("hello world");
        }

        @Test
        void shouldReturnNullForMissingOutput() {
            ChainExecutionContext ctx = new ChainExecutionContext(Map.of());

            assertThat(ctx.getOutput("nonexistent")).isNull();
        }

        @Test
        void shouldOverwriteExistingOutput() {
            ChainExecutionContext ctx = new ChainExecutionContext(Map.of());

            ctx.setOutput("step1", "first");
            ctx.setOutput("step1", "second");

            assertThat(ctx.getOutput("step1")).isEqualTo("second");
        }

        @Test
        void shouldReturnImmutableViewOfAllOutputs() {
            ChainExecutionContext ctx = new ChainExecutionContext(Map.of());
            ctx.setOutput("a", "1");
            ctx.setOutput("b", "2");

            Map<String, String> all = ctx.getAllOutputs();

            assertThat(all).hasSize(2);
            assertThat(all).containsEntry("a", "1");
            assertThat(all).containsEntry("b", "2");
        }
    }

    @Nested
    class TemplateResolution {

        @Test
        void shouldResolveSinglePlaceholder() {
            ChainExecutionContext ctx = new ChainExecutionContext(Map.of("name", "Alice"));

            String resolved = ctx.resolveTemplate("Hello {name}, welcome!");

            assertThat(resolved).isEqualTo("Hello Alice, welcome!");
        }

        @Test
        void shouldResolveMultiplePlaceholders() {
            ChainExecutionContext ctx = new ChainExecutionContext(Map.of("greeting", "Hi", "name", "Bob"));

            String resolved = ctx.resolveTemplate("{greeting} {name}!");

            assertThat(resolved).isEqualTo("Hi Bob!");
        }

        @Test
        void shouldPreferStepOutputOverInitialInput() {
            ChainExecutionContext ctx = new ChainExecutionContext(Map.of("result", "initial"));
            ctx.setOutput("result", "from step");

            String resolved = ctx.resolveTemplate("Value: {result}");

            assertThat(resolved).isEqualTo("Value: from step");
        }

        @Test
        void shouldLeaveUnresolvedPlaceholdersIntact() {
            ChainExecutionContext ctx = new ChainExecutionContext(Map.of());

            String resolved = ctx.resolveTemplate("Value: {missing}");

            assertThat(resolved).isEqualTo("Value: {missing}");
        }

        @Test
        void shouldResolveFromStepOutputs() {
            ChainExecutionContext ctx = new ChainExecutionContext(Map.of());
            ctx.setOutput("analysis", "detailed analysis result");

            String resolved = ctx.resolveTemplate("Based on {analysis}, we conclude...");

            assertThat(resolved).isEqualTo("Based on detailed analysis result, we conclude...");
        }

        @Test
        void shouldHandleNullTemplate() {
            ChainExecutionContext ctx = new ChainExecutionContext(Map.of());

            assertThat(ctx.resolveTemplate(null)).isNull();
        }

        @Test
        void shouldHandleEmptyTemplate() {
            ChainExecutionContext ctx = new ChainExecutionContext(Map.of());

            assertThat(ctx.resolveTemplate("")).isEmpty();
        }

        @Test
        void shouldHandleTemplateWithNoPlaceholders() {
            ChainExecutionContext ctx = new ChainExecutionContext(Map.of());

            String resolved = ctx.resolveTemplate("No variables here");

            assertThat(resolved).isEqualTo("No variables here");
        }

        @Test
        void shouldResolveMixedSourcePlaceholders() {
            ChainExecutionContext ctx = new ChainExecutionContext(Map.of("initial", "from input"));
            ctx.setOutput("step1", "from step");

            String resolved = ctx.resolveTemplate("{initial} and {step1}");

            assertThat(resolved).isEqualTo("from input and from step");
        }
    }

    @Nested
    class InitialInputs {

        @Test
        void shouldCopyInitialInputs() {
            Map<String, Object> inputs = Map.of("key", "value");

            ChainExecutionContext ctx = new ChainExecutionContext(inputs);

            assertThat(ctx.resolveTemplate("{key}")).isEqualTo("value");
        }

        @Test
        void shouldHandleNullInitialInputs() {
            ChainExecutionContext ctx = new ChainExecutionContext(null);

            assertThat(ctx.resolveTemplate("{missing}")).isEqualTo("{missing}");
        }

        @Test
        void shouldConvertNonStringInitialInputs() {
            ChainExecutionContext ctx = new ChainExecutionContext(Map.of("count", 42));

            String resolved = ctx.resolveTemplate("Count: {count}");

            assertThat(resolved).isEqualTo("Count: 42");
        }
    }
}
