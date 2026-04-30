package com.schemaplexai.agent.engine.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_agent_execution_log")
public class SfAgentExecutionLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long executionId;

    private String state;

    private String message;
}
