package com.schemaplexai.workflow.service;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NodeVariableSubstitutorTest {

    @Test
    void substitute_inputNamespace_resolvesMergedUpstreamOutput() {
        Map<String, Object> context = Map.of(
                "input", Map.of("generatedText", "hello world"));

        Map<String, Object> resolved = NodeVariableSubstitutor.substitute(
                Map.of("prompt", "Summarize: ${input.generatedText}"), context);

        assertThat(resolved).containsEntry("prompt", "Summarize: hello world");
    }

    @Test
    void substitute_nodeNamespace_resolvesSpecificUpstreamNode() {
        Map<String, Object> context = Map.of(
                "input", Map.of("token", "abc"),
                "n1", Map.of("token", "abc", "score", 7));

        Map<String, Object> resolved = NodeVariableSubstitutor.substitute(
                Map.of("prompt", "token=${n1.token}"), context);

        assertThat(resolved).containsEntry("prompt", "token=abc");
    }

    @Test
    void substitute_wholeValuePlaceholder_preservesType() {
        Map<String, Object> context = Map.of(
                "input", Map.of("score", 42));

        Map<String, Object> resolved = NodeVariableSubstitutor.substitute(
                Map.of("threshold", "${input.score}"), context);

        assertThat(resolved.get("threshold")).isEqualTo(42);
    }

    @Test
    void substitute_nestedMapAndList_resolvedRecursively() {
        Map<String, Object> context = Map.of(
                "input", Map.of("host", "example.com"));

        Map<String, Object> input = Map.of(
                "headers", Map.of("X-Host", "${input.host}"),
                "tags", List.of("${input.host}", "static"));

        Map<String, Object> resolved = NodeVariableSubstitutor.substitute(input, context);

        assertThat((Map<String, Object>) resolved.get("headers"))
                .containsEntry("X-Host", "example.com");
        assertThat((List<Object>) resolved.get("tags"))
                .containsExactly("example.com", "static");
    }

    @Test
    void substitute_unresolvablePlaceholder_leftUnchanged() {
        Map<String, Object> resolved = NodeVariableSubstitutor.substitute(
                Map.of("prompt", "value=${input.missing}"), Map.of("input", Map.of()));

        assertThat(resolved).containsEntry("prompt", "value=${input.missing}");
    }

    @Test
    void substitute_nullInput_returnsEmptyMap() {
        assertThat(NodeVariableSubstitutor.substitute(null, Map.of())).isEmpty();
    }

    @Test
    void substitute_nonStringValues_passThrough() {
        Map<String, Object> resolved = NodeVariableSubstitutor.substitute(
                new HashMap<>(Map.of("count", 5, "flag", true)), Map.of());

        assertThat(resolved)
                .containsEntry("count", 5)
                .containsEntry("flag", true)
                .hasSize(2);
    }

    @Test
    void substitute_multiplePlaceholders_allResolved() {
        Map<String, Object> context = Map.of(
                "input", Map.of("a", "1", "b", "2"));

        Map<String, Object> resolved = NodeVariableSubstitutor.substitute(
                Map.of("prompt", "${input.a}+${input.b}=${input.a}"), context);

        assertThat(resolved).containsEntry("prompt", "1+2=1");
    }

    @Test
    void substitute_laterUpstreamNodeWinsInMergedInput() {
        // The orchestration loop merges upstream outputs in execution order, so a later
        // node's key overrides an earlier node's value in the flat "input" namespace.
        Map<String, Object> merged = new HashMap<>();
        merged.put("result", "first");
        merged.putAll(Map.of("result", "second"));
        Map<String, Object> context = Map.of("input", merged);

        Map<String, Object> resolved = NodeVariableSubstitutor.substitute(
                Map.of("prompt", "${input.result}"), context);

        assertThat(resolved).containsEntry("prompt", "second");
    }
}
