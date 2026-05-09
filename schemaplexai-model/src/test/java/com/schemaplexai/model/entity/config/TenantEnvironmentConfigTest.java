package com.schemaplexai.model.entity.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TenantEnvironmentConfig")
class TenantEnvironmentConfigTest {

    @Test
    @DisplayName("should create with no-args constructor")
    void shouldCreateWithNoArgsConstructor() {
        TenantEnvironmentConfig config = new TenantEnvironmentConfig();
        assertThat(config.getTenantId()).isNull();
        assertThat(config.getEnvironment()).isNull();
        assertThat(config.getSecurityLevel()).isNull();
    }

    @Test
    @DisplayName("should support setters and getters")
    void shouldSupportSettersAndGetters() {
        TenantEnvironmentConfig config = new TenantEnvironmentConfig();
        config.setTenantId("t1");
        config.setEnvironment("prod");
        config.setAllowedTools("[\"http\",\"sql\"]");
        config.setSecurityLevel("HIGH");
        config.setAllowHttpCalls(true);
        config.setAllowFileRead(false);
        config.setAllowIrreversibleOps(false);
        config.setMaxConcurrentToolCalls(5);
        config.setExtraConfig("{\"timeout\":30}");

        assertThat(config.getTenantId()).isEqualTo("t1");
        assertThat(config.getEnvironment()).isEqualTo("prod");
        assertThat(config.getAllowedTools()).isEqualTo("[\"http\",\"sql\"]");
        assertThat(config.getSecurityLevel()).isEqualTo("HIGH");
        assertThat(config.getAllowHttpCalls()).isTrue();
        assertThat(config.getAllowFileRead()).isFalse();
        assertThat(config.getAllowIrreversibleOps()).isFalse();
        assertThat(config.getMaxConcurrentToolCalls()).isEqualTo(5);
        assertThat(config.getExtraConfig()).isEqualTo("{\"timeout\":30}");
    }

    @Test
    @DisplayName("should inherit BaseEntity fields")
    void shouldInheritBaseEntityFields() {
        TenantEnvironmentConfig config = new TenantEnvironmentConfig();
        config.setId(1L);
        config.setCreatedBy(100L);

        assertThat(config.getId()).isEqualTo(1L);
        assertThat(config.getCreatedBy()).isEqualTo(100L);
    }
}
