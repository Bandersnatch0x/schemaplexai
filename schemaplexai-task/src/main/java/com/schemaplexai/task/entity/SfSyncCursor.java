package com.schemaplexai.task.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_sync_cursor")
public class SfSyncCursor extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String syncName;

    private String sourceTable;

    private String targetTable;

    private Long lastSyncId;

    private LocalDateTime lastSyncTime;

    private Integer syncBatchSize;

    private Integer failedCount;

    private String lastError;
}
