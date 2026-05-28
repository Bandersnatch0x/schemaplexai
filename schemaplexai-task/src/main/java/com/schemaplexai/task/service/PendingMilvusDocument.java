package com.schemaplexai.task.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingMilvusDocument {

    private Long docId;

    private String tenantId;
}
