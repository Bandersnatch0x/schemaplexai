package com.schemaplexai.ops.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_sync_cursor")
public class SfSyncCursor extends BaseEntity {

    private String syncTable;
    private Long lastSyncId;
    private LocalDateTime lastSyncTime;
}
