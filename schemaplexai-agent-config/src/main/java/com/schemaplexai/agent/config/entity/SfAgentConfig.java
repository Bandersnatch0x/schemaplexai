package com.schemaplexai.agent.config.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_agent_config")
public class SfAgentConfig extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long agentId;

    private Long maxRounds;

    private Long maxTools;

    private Long maxInputTokens;

    private Long maxOutputTokens;

    private String systemPrompt;

    private String modelId;

    private Double temperature;

    private String executionMode;
}
