package com.schemaplexai.ops.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_notification")
public class SfNotification extends BaseEntity {

    private Long userId;
    private String type;
    private String title;
    private String content;
    private Integer status;
}
