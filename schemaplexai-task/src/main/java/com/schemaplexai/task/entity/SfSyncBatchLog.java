package com.schemaplexai.task.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_sync_batch_log")
public class SfSyncBatchLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String syncName;

    private String batchId;

    private Long startId;

    private Long endId;

    private Integer recordCount;

    private String status;

    private String errorMsg;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;
}
