package com.schemaplexai.model.entity.agent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SfAgentShadowConfig")
class SfAgentShadowConfigTest {

    @Test
    @DisplayName("should create with no-args constructor")
    void shouldCreateWithNoArgsConstructor() {
        SfAgentShadowConfig config = new SfAgentShadowConfig();
        assertThat(config.getAgentId()).isNull();
        assertThat(config.getFeedbackActionsJson()).isNull();
        assertThat(config.getEnabled()).isNull();
    }

    @Test
    @DisplayName("should support setters and getters")
    void shouldSupportSettersAndGetters() {
        SfAgentShadowConfig config = new SfAgentShadowConfig();
        config.setAgentId(1L);
        config.setFeedbackActionsJson("[{\"action\":\"approve\"}]");
        config.setEnabled(true);

        assertThat(config.getAgentId()).isEqualTo(1L);
        assertThat(config.getFeedbackActionsJson()).isEqualTo("[{\"action\":\"approve\"}]");
        assertThat(config.getEnabled()).isTrue();
    }

    @Test
    @DisplayName("should inherit BaseEntity fields")
    void shouldInheritBaseEntityFields() {
        SfAgentShadowConfig config = new SfAgentShadowConfig();
        config.setId(100L);
        config.setTenantId("tenant-1");

        assertThat(config.getId()).isEqualTo(100L);
        assertThat(config.getTenantId()).isEqualTo("tenant-1");
    }

    @Test
    @DisplayName("should be equal when fields match")
    void shouldBeEqualWhenFieldsMatch() {
        SfAgentShadowConfig c1 = new SfAgentShadowConfig();
        c1.setAgentId(1L);
        c1.setEnabled(true);

        SfAgentShadowConfig c2 = new SfAgentShadowConfig();
        c2.setAgentId(1L);
        c2.setEnabled(true);

        assertThat(c1).isEqualTo(c2);
    }

    @Test
    @DisplayName("should not be equal when fields differ")
    void shouldNotBeEqualWhenFieldsDiffer() {
        SfAgentShadowConfig c1 = new SfAgentShadowConfig();
        c1.setAgentId(1L);

        SfAgentShadowConfig c2 = new SfAgentShadowConfig();
        c2.setAgentId(2L);

        assertThat(c1).isNotEqualTo(c2);
    }
}
