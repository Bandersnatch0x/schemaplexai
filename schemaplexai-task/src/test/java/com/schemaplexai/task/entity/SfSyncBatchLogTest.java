package com.schemaplexai.task.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SfSyncBatchLogTest {

    @Test
    void gettersAndSetters_work() {
        SfSyncBatchLog entity = new SfSyncBatchLog();
        LocalDateTime started = LocalDateTime.now();
        LocalDateTime completed = started.plusMinutes(5);

        entity.setSyncName("cost-sync");
        entity.setBatchId("batch-001");
        entity.setStartId(1L);
        entity.setEndId(1000L);
        entity.setRecordCount(1000);
        entity.setStatus("SUCCESS");
        entity.setErrorMsg(null);
        entity.setStartedAt(started);
        entity.setCompletedAt(completed);

        assertThat(entity.getSyncName()).isEqualTo("cost-sync");
        assertThat(entity.getBatchId()).isEqualTo("batch-001");
        assertThat(entity.getStartId()).isEqualTo(1L);
        assertThat(entity.getEndId()).isEqualTo(1000L);
        assertThat(entity.getRecordCount()).isEqualTo(1000);
        assertThat(entity.getStatus()).isEqualTo("SUCCESS");
        assertThat(entity.getErrorMsg()).isNull();
        assertThat(entity.getStartedAt()).isEqualTo(started);
        assertThat(entity.getCompletedAt()).isEqualTo(completed);
    }

    @Test
    void equals_sameId_returnsTrue() {
        SfSyncBatchLog e1 = new SfSyncBatchLog();
        e1.setId(1L);
        SfSyncBatchLog e2 = new SfSyncBatchLog();
        e2.setId(1L);

        assertThat(e1).isEqualTo(e2);
    }

    @Test
    void equals_differentId_returnsFalse() {
        SfSyncBatchLog e1 = new SfSyncBatchLog();
        e1.setId(1L);
        SfSyncBatchLog e2 = new SfSyncBatchLog();
        e2.setId(2L);

        assertThat(e1).isNotEqualTo(e2);
    }

    @Test
    void hashCode_sameId_returnsSame() {
        SfSyncBatchLog e1 = new SfSyncBatchLog();
        e1.setId(99L);
        SfSyncBatchLog e2 = new SfSyncBatchLog();
        e2.setId(99L);

        assertThat(e1.hashCode()).isEqualTo(e2.hashCode());
    }

    @Test
    void serialVersionUid_isSet() {
        assertThat(SfSyncBatchLog.class.getDeclaredFields()).anyMatch(f -> "serialVersionUID".equals(f.getName()));
    }
}
