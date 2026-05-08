package com.schemaplexai.task.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SfIdempotencyKeyTest {

    @Test
    void gettersAndSetters_work() {
        SfIdempotencyKey entity = new SfIdempotencyKey();
        LocalDateTime now = LocalDateTime.now();

        entity.setMessageId("msg-001");
        entity.setConsumerGroup("TestConsumer");
        entity.setStatus("SUCCESS");
        entity.setConsumedAt(now);

        assertThat(entity.getMessageId()).isEqualTo("msg-001");
        assertThat(entity.getConsumerGroup()).isEqualTo("TestConsumer");
        assertThat(entity.getStatus()).isEqualTo("SUCCESS");
        assertThat(entity.getConsumedAt()).isEqualTo(now);
    }

    @Test
    void equals_sameId_returnsTrue() {
        SfIdempotencyKey e1 = new SfIdempotencyKey();
        e1.setId(1L);
        SfIdempotencyKey e2 = new SfIdempotencyKey();
        e2.setId(1L);

        assertThat(e1).isEqualTo(e2);
    }

    @Test
    void equals_differentId_returnsFalse() {
        SfIdempotencyKey e1 = new SfIdempotencyKey();
        e1.setId(1L);
        SfIdempotencyKey e2 = new SfIdempotencyKey();
        e2.setId(2L);

        assertThat(e1).isNotEqualTo(e2);
    }

    @Test
    void hashCode_sameId_returnsSame() {
        SfIdempotencyKey e1 = new SfIdempotencyKey();
        e1.setId(7L);
        SfIdempotencyKey e2 = new SfIdempotencyKey();
        e2.setId(7L);

        assertThat(e1.hashCode()).isEqualTo(e2.hashCode());
    }

    @Test
    void serialVersionUid_isSet() {
        assertThat(SfIdempotencyKey.class.getDeclaredFields()).anyMatch(f -> "serialVersionUID".equals(f.getName()));
    }

    @Test
    void inheritsBaseEntityFields() {
        SfIdempotencyKey entity = new SfIdempotencyKey();
        entity.setId(1L);
        entity.setTenantId("t1");
        entity.setCreatedBy(100L);

        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getTenantId()).isEqualTo("t1");
        assertThat(entity.getCreatedBy()).isEqualTo(100L);
    }
}
