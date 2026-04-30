package com.schemaplexai.ops.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_delivery_record")
public class SfDeliveryRecord extends BaseEntity {

    private Long artifactId;
    private String deliveryType;
    private String recipient;
    private Integer status;
}
