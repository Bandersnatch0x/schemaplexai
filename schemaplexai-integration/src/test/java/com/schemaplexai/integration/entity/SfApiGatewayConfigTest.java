package com.schemaplexai.integration.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SfApiGatewayConfigTest {

    @Test
    void gettersAndSetters() {
        SfApiGatewayConfig config = new SfApiGatewayConfig();
        config.setId(1L);
        config.setTenantId("t1");
        config.setName("Gateway");
        config.setBaseUrl("http://localhost:8080");
        config.setAuthType("bearer");
        config.setAuthConfig("{\"token\":\"secret\"}");
        config.setRateLimit(100);
        config.setDeleted(0);

        assertThat(config.getId()).isEqualTo(1L);
        assertThat(config.getTenantId()).isEqualTo("t1");
        assertThat(config.getName()).isEqualTo("Gateway");
        assertThat(config.getBaseUrl()).isEqualTo("http://localhost:8080");
        assertThat(config.getAuthType()).isEqualTo("bearer");
        assertThat(config.getAuthConfig()).isEqualTo("{\"token\":\"secret\"}");
        assertThat(config.getRateLimit()).isEqualTo(100);
        assertThat(config.getDeleted()).isEqualTo(0);
    }

    @Test
    void inheritsBaseEntityFields() {
        SfApiGatewayConfig config = new SfApiGatewayConfig();
        config.setCreatedBy(1L);
        config.setUpdatedBy(1L);

        assertThat(config.getCreatedBy()).isEqualTo(1L);
        assertThat(config.getUpdatedBy()).isEqualTo(1L);
    }
}
