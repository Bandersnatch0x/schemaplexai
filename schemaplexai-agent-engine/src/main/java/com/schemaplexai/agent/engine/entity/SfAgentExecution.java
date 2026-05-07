package com.schemaplexai.agent.engine.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    /** Transient metadata map for inter-handler communication (retryContext, blockedReason, etc.) */
    private transient Map<String, Object> metadata = new ConcurrentHashMap<>();

    /** Reference to the latest execution snapshot (for pause/resume) */
    private Long snapshotId;

    private String skillName;

    private String roleName;

    public Object getMetadata(String key) {
        if (metadata == null) metadata = new ConcurrentHashMap<>();
        return metadata.get(key);
    }

    public void setMetadata(String key, Object value) {
        if (metadata == null) metadata = new ConcurrentHashMap<>();
        metadata.put(key, value);
    }
}
