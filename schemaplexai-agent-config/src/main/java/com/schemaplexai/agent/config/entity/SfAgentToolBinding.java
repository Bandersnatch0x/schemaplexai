package com.schemaplexai.agent.config.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_agent_tool_binding")
public class SfAgentToolBinding extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long agentId;

    private String toolName;

    private String toolType;

    private String configJson;
}
