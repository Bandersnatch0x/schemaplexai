package com.schemaplexai.agent.engine.a2a;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AgentCard")
class AgentCardTest {

    @Test
    @DisplayName("should create AgentCard with all fields via builder")
    void shouldCreateWithBuilder() {
        AgentCard card = AgentCard.builder()
                .name("data-analyzer")
                .version("1.2.0")
                .capabilities(List.of("sql", "chart", "report"))
                .endpointUrl("http://analyzer:8080")
                .authenticationType("bearer")
                .maxConcurrentExecutions(5)
                .supportedMessageFormats(List.of("json", "xml"))
                .build();

        assertThat(card.getName()).isEqualTo("data-analyzer");
        assertThat(card.getVersion()).isEqualTo("1.2.0");
        assertThat(card.getCapabilities()).containsExactly("sql", "chart", "report");
        assertThat(card.getEndpointUrl()).isEqualTo("http://analyzer:8080");
        assertThat(card.getAuthenticationType()).isEqualTo("bearer");
        assertThat(card.getMaxConcurrentExecutions()).isEqualTo(5);
        assertThat(card.getSupportedMessageFormats()).containsExactly("json", "xml");
    }

    @Test
    @DisplayName("should create AgentCard with no-args constructor")
    void shouldCreateWithNoArgsConstructor() {
        AgentCard card = new AgentCard();

        assertThat(card.getName()).isNull();
        assertThat(card.getCapabilities()).isNull();
        assertThat(card.getMaxConcurrentExecutions()).isEqualTo(0);
    }

    @Test
    @DisplayName("should create AgentCard with all-args constructor")
    void shouldCreateWithAllArgsConstructor() {
        AgentCard card = new AgentCard(
                "summarizer",
                "2.0.0",
                List.of("summarize"),
                "http://summarizer:9090",
                "api-key",
                10,
                List.of("json")
        );

        assertThat(card.getName()).isEqualTo("summarizer");
        assertThat(card.getVersion()).isEqualTo("2.0.0");
        assertThat(card.getMaxConcurrentExecutions()).isEqualTo(10);
    }

    @Test
    @DisplayName("should support setters and getters")
    void shouldSupportSettersAndGetters() {
        AgentCard card = new AgentCard();
        card.setName("test-agent");
        card.setVersion("0.0.1");
        card.setCapabilities(List.of("echo"));
        card.setEndpointUrl("http://localhost:8080");
        card.setAuthenticationType("none");
        card.setMaxConcurrentExecutions(1);
        card.setSupportedMessageFormats(List.of("json"));

        assertThat(card.getName()).isEqualTo("test-agent");
        assertThat(card.getEndpointUrl()).isEqualTo("http://localhost:8080");
        assertThat(card.getMaxConcurrentExecutions()).isEqualTo(1);
    }

    @Test
    @DisplayName("should consider two cards equal when fields match")
    void shouldBeEqualWhenFieldsMatch() {
        AgentCard card1 = AgentCard.builder()
                .name("agent-a")
                .version("1.0.0")
                .capabilities(List.of("cap1"))
                .endpointUrl("http://a:8080")
                .authenticationType("none")
                .maxConcurrentExecutions(2)
                .supportedMessageFormats(List.of("json"))
                .build();

        AgentCard card2 = AgentCard.builder()
                .name("agent-a")
                .version("1.0.0")
                .capabilities(List.of("cap1"))
                .endpointUrl("http://a:8080")
                .authenticationType("none")
                .maxConcurrentExecutions(2)
                .supportedMessageFormats(List.of("json"))
                .build();

        assertThat(card1).isEqualTo(card2);
        assertThat(card1.hashCode()).isEqualTo(card2.hashCode());
    }

    @Test
    @DisplayName("should not be equal when fields differ")
    void shouldNotBeEqualWhenFieldsDiffer() {
        AgentCard card1 = AgentCard.builder().name("agent-a").version("1.0.0").build();
        AgentCard card2 = AgentCard.builder().name("agent-b").version("1.0.0").build();

        assertThat(card1).isNotEqualTo(card2);
    }

    @Test
    @DisplayName("should produce meaningful toString")
    void shouldProduceMeaningfulToString() {
        AgentCard card = AgentCard.builder()
                .name("agent-x")
                .endpointUrl("http://x:8080")
                .build();

        assertThat(card.toString()).contains("agent-x");
        assertThat(card.toString()).contains("http://x:8080");
    }
}
