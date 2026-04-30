package com.schemaplexai.agent.config.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_prompt_version")
public class SfPromptVersion extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long configId;

    private Long agentId;

    private Integer version;

    private String content;

    private String label;

    private String changeNote;
}
