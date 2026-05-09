package com.schemaplexai.model.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BaseEntity")
class BaseEntityTest {

    @Test
    @DisplayName("should create with no-args constructor")
    void shouldCreateWithNoArgsConstructor() {
        BaseEntity entity = new BaseEntity();

        assertThat(entity.getId()).isNull();
        assertThat(entity.getTenantId()).isNull();
        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getUpdatedAt()).isNull();
        assertThat(entity.getCreatedBy()).isNull();
        assertThat(entity.getUpdatedBy()).isNull();
        assertThat(entity.getDeleted()).isNull();
    }

    @Test
    @DisplayName("should support setters and getters")
    void shouldSupportSettersAndGetters() {
        BaseEntity entity = new BaseEntity();
        LocalDateTime now = LocalDateTime.now();

        entity.setId(1L);
        entity.setTenantId("t1");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setCreatedBy(100L);
        entity.setUpdatedBy(200L);
        entity.setDeleted(0);

        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getTenantId()).isEqualTo("t1");
        assertThat(entity.getCreatedAt()).isEqualTo(now);
        assertThat(entity.getUpdatedAt()).isEqualTo(now);
        assertThat(entity.getCreatedBy()).isEqualTo(100L);
        assertThat(entity.getUpdatedBy()).isEqualTo(200L);
        assertThat(entity.getDeleted()).isEqualTo(0);
    }

    @Test
    @DisplayName("should be equal when fields match")
    void shouldBeEqualWhenFieldsMatch() {
        BaseEntity e1 = new BaseEntity();
        e1.setId(1L);
        e1.setTenantId("t1");

        BaseEntity e2 = new BaseEntity();
        e2.setId(1L);
        e2.setTenantId("t1");

        assertThat(e1).isEqualTo(e2);
        assertThat(e1.hashCode()).isEqualTo(e2.hashCode());
    }

    @Test
    @DisplayName("should not be equal when fields differ")
    void shouldNotBeEqualWhenFieldsDiffer() {
        BaseEntity e1 = new BaseEntity();
        e1.setId(1L);

        BaseEntity e2 = new BaseEntity();
        e2.setId(2L);

        assertThat(e1).isNotEqualTo(e2);
    }

    @Test
    @DisplayName("should produce meaningful toString")
    void shouldProduceMeaningfulToString() {
        BaseEntity entity = new BaseEntity();
        entity.setId(1L);
        entity.setTenantId("t1");

        assertThat(entity.toString()).contains("BaseEntity");
    }
}
