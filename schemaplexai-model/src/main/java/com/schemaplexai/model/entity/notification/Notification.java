package com.schemaplexai.model.entity.notification;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_notification")
public class Notification extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private String title;

    private String content;

    private String type;

    private Boolean read;
}
