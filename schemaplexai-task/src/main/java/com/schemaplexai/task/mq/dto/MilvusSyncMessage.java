package com.schemaplexai.task.mq.dto;

import lombok.Data;

@Data
public class MilvusSyncMessage {

    private String collectionName;

    private String operation;

    private Long docId;

    private String tenantId;

    private String idempotencyKey;
}
