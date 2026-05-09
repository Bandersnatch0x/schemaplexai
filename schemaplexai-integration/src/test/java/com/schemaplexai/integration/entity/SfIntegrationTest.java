package com.schemaplexai.integration.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SfIntegrationTest {

    @Test
    void gettersAndSetters() {
        SfIntegration entity = new SfIntegration();
        entity.setId(1L);
        entity.setTenantId("t1");
        entity.setName("GitHub");
        entity.setType("github");
        entity.setConfigJson("{\"url\":\"https://github.com\"}");
        entity.setStatus(1);
        entity.setDeleted(0);

        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getTenantId()).isEqualTo("t1");
        assertThat(entity.getName()).isEqualTo("GitHub");
        assertThat(entity.getType()).isEqualTo("github");
        assertThat(entity.getConfigJson()).isEqualTo("{\"url\":\"https://github.com\"}");
        assertThat(entity.getStatus()).isEqualTo(1);
        assertThat(entity.getDeleted()).isEqualTo(0);
    }
}
