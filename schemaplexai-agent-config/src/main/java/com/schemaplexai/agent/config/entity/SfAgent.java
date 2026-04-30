package com.schemaplexai.agent.config.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_agent")
public class SfAgent extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String name;

    private String type;

    private String status;

    private String description;
}
