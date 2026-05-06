package com.schemaplexai.model.entity.agent;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_agent_shadow_config")
public class SfAgentShadowConfig extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long agentId;

    private String feedbackActionsJson;

    private Boolean enabled;
}
