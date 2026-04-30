package com.schemaplexai.context.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_context_snapshot")
public class SfContextSnapshot extends BaseEntity {

    private Long contextId;
    private String snapshotJson;
    private Integer version;
}
