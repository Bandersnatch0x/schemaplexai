package com.schemaplexai.agent.config.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SfPromptVersionTest {

    @Test
    void shouldStorePromptVersionWithLabel() {
        SfPromptVersion pv = new SfPromptVersion();
        pv.setConfigId(1L);
        pv.setAgentId(10L);
        pv.setVersion(3);
        pv.setContent("You are a helpful assistant");
        pv.setLabel("production");
        pv.setChangeNote("Updated tone to be more professional");

        assertThat(pv.getVersion()).isEqualTo(3);
        assertThat(pv.getLabel()).isEqualTo("production");
        assertThat(pv.getContent()).contains("helpful assistant");
    }
}
