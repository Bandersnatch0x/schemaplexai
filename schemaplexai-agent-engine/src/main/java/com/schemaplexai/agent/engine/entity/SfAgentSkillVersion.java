package com.schemaplexai.agent.engine.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_agent_skill_version")
public class SfAgentSkillVersion extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long skillId;

    private Integer version;

    private String content;
}
