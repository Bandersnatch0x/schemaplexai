package com.schemaplexai.context.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_workspace")
public class SfWorkspace extends BaseEntity {

    private String name;
    private String description;
    private Long parentId;
}
