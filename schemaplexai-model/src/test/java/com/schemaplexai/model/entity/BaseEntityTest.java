package com.schemaplexai.model.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class BaseEntityTest {

    @Test
    void defaultValues_areNull() {
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
    void settersAndGetters_work() {
        BaseEntity entity = new BaseEntity();
        LocalDateTime now = LocalDateTime.now();

        entity.setId(1L);
        entity.setTenantId("tenant-001");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setCreatedBy(100L);
        entity.setUpdatedBy(200L);
        entity.setDeleted(0);

        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getTenantId()).isEqualTo("tenant-001");
        assertThat(entity.getCreatedAt()).isEqualTo(now);
        assertThat(entity.getUpdatedAt()).isEqualTo(now);
        assertThat(entity.getCreatedBy()).isEqualTo(100L);
        assertThat(entity.getUpdatedBy()).isEqualTo(200L);
        assertThat(entity.getDeleted()).isEqualTo(0);
    }

    @Test
    void equals_consistentWithId() {
        BaseEntity e1 = new BaseEntity();
        e1.setId(1L);

        BaseEntity e2 = new BaseEntity();
        e2.setId(1L);

        assertThat(e1).isEqualTo(e2);
    }

    @Test
    void hashCode_consistentWithEquals() {
        BaseEntity e1 = new BaseEntity();
        e1.setId(42L);

        BaseEntity e2 = new BaseEntity();
        e2.setId(42L);

        assertThat(e1.hashCode()).isEqualTo(e2.hashCode());
    }

    @Test
    void implementsSerializable() {
        assertThat(java.io.Serializable.class.isAssignableFrom(BaseEntity.class)).isTrue();
    }
}
