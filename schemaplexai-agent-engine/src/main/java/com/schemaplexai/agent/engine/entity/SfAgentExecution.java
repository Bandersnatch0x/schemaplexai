package com.schemaplexai.agent.engine.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_agent_execution")
public class SfAgentExecution extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long agentId;

    private String conversationId;

    private String state;

    private String tokenBudgetJson;

    private LocalDateTime completedAt;
}
