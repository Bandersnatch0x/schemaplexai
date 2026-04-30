package com.schemaplexai.context.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_context_item")
public class SfContextItem extends BaseEntity {

    private Long contextId;
    private String key;
    private String value;
    private String itemType;
}
