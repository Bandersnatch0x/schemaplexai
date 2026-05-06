package com.schemaplexai.task.mq.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * MQ payload for cost data synchronization trigger.
 */
@Data
public class CostSyncMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String syncType;
    private Long tenantId;
    private String dateRange;
    private Boolean forceFullSync;
    private String idempotencyKey;
}
