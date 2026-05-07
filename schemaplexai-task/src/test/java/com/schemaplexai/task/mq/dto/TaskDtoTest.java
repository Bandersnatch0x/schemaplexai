package com.schemaplexai.task.mq.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TaskDtoTest {

    @Test
    void agentExecuteMessage() {
        AgentExecuteMessage msg = new AgentExecuteMessage();
        msg.setAgentId(1L);
        msg.setTenantId("t1");
        msg.setPrompt("hello");
        msg.setConversationId("c1");
        msg.setIdempotencyKey("key1");

        assertThat(msg.getAgentId()).isEqualTo(1L);
        assertThat(msg.getTenantId()).isEqualTo("t1");
        assertThat(msg.getPrompt()).isEqualTo("hello");
        assertThat(msg.getConversationId()).isEqualTo("c1");
        assertThat(msg.getIdempotencyKey()).isEqualTo("key1");
    }

    @Test
    void costSyncMessage() {
        CostSyncMessage msg = new CostSyncMessage();
        msg.setSyncType("api");
        msg.setTenantId(1L);
        msg.setDateRange("2024-01");
        msg.setForceFullSync(true);
        msg.setIdempotencyKey("key1");

        assertThat(msg.getSyncType()).isEqualTo("api");
        assertThat(msg.getTenantId()).isEqualTo(1L);
        assertThat(msg.getDateRange()).isEqualTo("2024-01");
        assertThat(msg.getForceFullSync()).isTrue();
        assertThat(msg.getIdempotencyKey()).isEqualTo("key1");
    }

    @Test
    void notificationMessage() {
        NotificationMessage msg = new NotificationMessage();
        msg.setChannel("email");
        msg.setUserId(1L);
        msg.setTitle("title");
        msg.setContent("content");
        msg.setTemplateCode("T001");
        msg.setIdempotencyKey("key1");

        assertThat(msg.getChannel()).isEqualTo("email");
        assertThat(msg.getUserId()).isEqualTo(1L);
        assertThat(msg.getTitle()).isEqualTo("title");
        assertThat(msg.getContent()).isEqualTo("content");
        assertThat(msg.getTemplateCode()).isEqualTo("T001");
        assertThat(msg.getIdempotencyKey()).isEqualTo("key1");
    }
}
