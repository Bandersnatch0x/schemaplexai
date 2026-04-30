package com.schemaplexai.task.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_idempotency_key")
public class SfIdempotencyKey extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String messageId;

    private String consumerGroup;

    private String status;

    private LocalDateTime consumedAt;
}
