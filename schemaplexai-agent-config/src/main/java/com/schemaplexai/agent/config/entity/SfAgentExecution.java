package com.schemaplexai.agent.config.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Read-only projection of the {@code sf_agent_execution} table (owned by the
 * agent-engine service). Used exclusively by the Cockpit statistics endpoint
 * for aggregate counts; this module never inserts or updates execution rows.
 *
 * <p>Does not extend {@code BaseEntity} because the table has no
 * {@code created_by}/{@code updated_by} columns.</p>
 */
@Data
@TableName("sf_agent_execution")
public class SfAgentExecution implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String tenantId;

    private Long agentId;

    private String conversationId;

    /** One of the agent-engine execution states (QUEUED ... REJECTED). */
    private String state;

    /** Pause reason when {@code state} is PAUSED (see agent-engine PauseReason). */
    private String pauseReason;

    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
