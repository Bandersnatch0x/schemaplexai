package com.schemaplexai.task.mq.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CostSyncMessageTest {

    @Test
    void gettersAndSetters_work() {
        CostSyncMessage msg = new CostSyncMessage();
        msg.setSyncType("api");
        msg.setTenantId(1L);
        msg.setDateRange("2024-01");
        msg.setForceFullSync(true);
        msg.setIdempotencyKey("key1");

        assertThat(msg.getSyncType()).isEqualTo("api");
        assertThat(msg.getTenantId()).isEqualTo(1L);
        assertThat(msg.getDateRange()).isEqualTo("2024-01");
        assertThat(msg.getForceFullSync()).isTrue();
        assertThat(msg.getIdempotencyKey()).isEqualTo("key1");
    }

    @Test
    void defaultValues_areNull() {
        CostSyncMessage msg = new CostSyncMessage();

        assertThat(msg.getSyncType()).isNull();
        assertThat(msg.getTenantId()).isNull();
        assertThat(msg.getDateRange()).isNull();
        assertThat(msg.getForceFullSync()).isNull();
        assertThat(msg.getIdempotencyKey()).isNull();
    }

    @Test
    void serialVersionUid_isSet() {
        assertThat(CostSyncMessage.class.getDeclaredFields()).anyMatch(f -> "serialVersionUID".equals(f.getName()));
    }

    @Test
    void implementsSerializable() {
        assertThat(java.io.Serializable.class.isAssignableFrom(CostSyncMessage.class)).isTrue();
    }
}
