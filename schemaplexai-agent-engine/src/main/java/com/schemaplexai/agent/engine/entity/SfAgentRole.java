package com.schemaplexai.agent.engine.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_agent_role")
public class SfAgentRole extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String name;

    private String description;

    private String overlay;

    /** 0=draft, 1=active, 2=archived */
    private Integer status;
}
