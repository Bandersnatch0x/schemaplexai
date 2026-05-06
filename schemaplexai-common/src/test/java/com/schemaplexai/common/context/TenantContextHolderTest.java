package com.schemaplexai.common.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TenantContextHolderTest {

    @AfterEach
    void cleanup() {
        TenantContextHolder.clear();
    }

    @Test
    void setAndGet_tenantId() {
        TenantContextHolder.setTenantId("tenant-001");
        assertThat(TenantContextHolder.getTenantId()).isEqualTo("tenant-001");
    }

    @Test
    void get_returnsNull_whenNotSet() {
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    void clear_removesTenantId() {
        TenantContextHolder.setTenantId("tenant-002");
        TenantContextHolder.clear();
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    void set_overwritesPreviousValue() {
        TenantContextHolder.setTenantId("tenant-a");
        TenantContextHolder.setTenantId("tenant-b");
        assertThat(TenantContextHolder.getTenantId()).isEqualTo("tenant-b");
    }

    @Test
    void tenantId_isThreadLocal() throws InterruptedException {
        TenantContextHolder.setTenantId("main-thread");

        AtomicReference<String> otherThreadValue = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Thread otherThread = new Thread(() -> {
            otherThreadValue.set(TenantContextHolder.getTenantId());
            TenantContextHolder.setTenantId("other-thread");
            latch.countDown();
        });
        otherThread.start();
        latch.await();

        assertThat(TenantContextHolder.getTenantId()).isEqualTo("main-thread");
        assertThat(otherThreadValue.get()).isNull();
    }
}
