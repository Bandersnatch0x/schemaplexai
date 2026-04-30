package com.schemaplexai.agent.engine.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_agent_memory")
public class SfAgentMemory extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long agentId;

    private String memoryType;

    private String content;

    private Long sourceExecutionId;
}
