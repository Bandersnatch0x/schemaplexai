package com.schemaplexai.context.service.impl;

import org.junit.jupiter.api.Test;

class NoOpMilvusSyncServiceImplTest {

    private final NoOpMilvusSyncServiceImpl service = new NoOpMilvusSyncServiceImpl();

    @Test
    void syncToMilvus_doesNotThrow() {
        service.syncToMilvus(1L);
    }
}
