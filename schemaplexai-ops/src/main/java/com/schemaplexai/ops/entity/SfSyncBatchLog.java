package com.schemaplexai.ops.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_sync_batch_log")
public class SfSyncBatchLog extends BaseEntity {

    private String syncTable;
    private Integer batchSize;
    private Integer successCount;
    private Integer failCount;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String errorMsg;
}
