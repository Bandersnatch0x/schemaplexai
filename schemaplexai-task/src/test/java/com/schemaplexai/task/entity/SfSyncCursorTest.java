package com.schemaplexai.task.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SfSyncCursorTest {

    @Test
    void gettersAndSetters_work() {
        SfSyncCursor entity = new SfSyncCursor();
        LocalDateTime now = LocalDateTime.now();

        entity.setSyncName("cost-sync");
        entity.setSourceTable("sf_cost");
        entity.setTargetTable("clickhouse_cost");
        entity.setLastSyncId(1000L);
        entity.setLastSyncTime(now);
        entity.setSyncBatchSize(500);
        entity.setFailedCount(2);
        entity.setLastError("timeout");

        assertThat(entity.getSyncName()).isEqualTo("cost-sync");
        assertThat(entity.getSourceTable()).isEqualTo("sf_cost");
        assertThat(entity.getTargetTable()).isEqualTo("clickhouse_cost");
        assertThat(entity.getLastSyncId()).isEqualTo(1000L);
        assertThat(entity.getLastSyncTime()).isEqualTo(now);
        assertThat(entity.getSyncBatchSize()).isEqualTo(500);
        assertThat(entity.getFailedCount()).isEqualTo(2);
        assertThat(entity.getLastError()).isEqualTo("timeout");
    }

    @Test
    void equals_sameId_returnsTrue() {
        SfSyncCursor e1 = new SfSyncCursor();
        e1.setId(1L);
        SfSyncCursor e2 = new SfSyncCursor();
        e2.setId(1L);

        assertThat(e1).isEqualTo(e2);
    }

    @Test
    void equals_differentId_returnsFalse() {
        SfSyncCursor e1 = new SfSyncCursor();
        e1.setId(1L);
        SfSyncCursor e2 = new SfSyncCursor();
        e2.setId(2L);

        assertThat(e1).isNotEqualTo(e2);
    }

    @Test
    void hashCode_sameId_returnsSame() {
        SfSyncCursor e1 = new SfSyncCursor();
        e1.setId(42L);
        SfSyncCursor e2 = new SfSyncCursor();
        e2.setId(42L);

        assertThat(e1.hashCode()).isEqualTo(e2.hashCode());
    }

    @Test
    void serialVersionUid_isSet() {
        assertThat(SfSyncCursor.class.getDeclaredFields()).anyMatch(f -> "serialVersionUID".equals(f.getName()));
    }

    @Test
    void inheritsBaseEntityFields() {
        SfSyncCursor entity = new SfSyncCursor();
        entity.setId(1L);
        entity.setTenantId("t1");
        entity.setDeleted(0);

        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getTenantId()).isEqualTo("t1");
        assertThat(entity.getDeleted()).isEqualTo(0);
    }
}
