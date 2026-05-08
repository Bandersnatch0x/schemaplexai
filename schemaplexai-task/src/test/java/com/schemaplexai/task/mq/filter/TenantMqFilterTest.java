package com.schemaplexai.task.mq.filter;

import com.schemaplexai.common.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class TenantMqFilterTest {

    private final TenantMqFilter filter = new TenantMqFilter();

    @AfterEach
    void cleanup() {
        TenantContextHolder.clear();
    }

    private Message createMessage(String json) {
        MessageProperties props = new MessageProperties();
        props.setReceivedRoutingKey("sf.agent.execute");
        return new Message(json.getBytes(StandardCharsets.UTF_8), props);
    }

    @Test
    void postProcess_validTenantId_setsContext() {
        Message msg = createMessage("{\"agentId\":1,\"tenantId\":\"tenant-abc\",\"prompt\":\"hi\"}");

        filter.postProcessMessage(msg);

        assertEquals("tenant-abc", TenantContextHolder.getTenantId());
    }

    @Test
    void postProcess_missingTenantId_throwsAndRejects() {
        Message msg = createMessage("{\"agentId\":1,\"prompt\":\"hi\"}");

        assertThrows(AmqpRejectAndDontRequeueException.class, () -> filter.postProcessMessage(msg));
        assertNull(TenantContextHolder.getTenantId());
    }

    @Test
    void postProcess_nullTenantId_throwsAndRejects() {
        Message msg = createMessage("{\"agentId\":1,\"tenantId\":null,\"prompt\":\"hi\"}");

        assertThrows(AmqpRejectAndDontRequeueException.class, () -> filter.postProcessMessage(msg));
    }

    @Test
    void postProcess_blankTenantId_throwsAndRejects() {
        Message msg = createMessage("{\"agentId\":1,\"tenantId\":\"   \",\"prompt\":\"hi\"}");

        assertThrows(AmqpRejectAndDontRequeueException.class, () -> filter.postProcessMessage(msg));
    }

    @Test
    void postProcess_invalidJson_throwsAndRejects() {
        Message msg = createMessage("not-json");

        assertThrows(AmqpRejectAndDontRequeueException.class, () -> filter.postProcessMessage(msg));
    }

    @Test
    void postProcess_longTenantId_works() {
        Message msg = createMessage("{\"tenantId\":\"sf-12345678901234567890\"}");

        filter.postProcessMessage(msg);

        assertEquals("sf-12345678901234567890", TenantContextHolder.getTenantId());
    }
}
