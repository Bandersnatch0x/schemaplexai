package com.schemaplexai.task.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_message_fail_log")
public class SfMessageFailLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String messageId;

    private String exchange;

    private String routingKey;

    private String payload;

    private String errorMsg;

    private String consumerGroup;

    private String status;

    private Integer retryCount;
}
