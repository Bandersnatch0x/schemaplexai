package com.schemaplexai.task.mq.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentExecuteMessageTest {

    @Test
    void gettersAndSetters_work() {
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
    void defaultValues_areNull() {
        AgentExecuteMessage msg = new AgentExecuteMessage();

        assertThat(msg.getAgentId()).isNull();
        assertThat(msg.getTenantId()).isNull();
        assertThat(msg.getPrompt()).isNull();
        assertThat(msg.getConversationId()).isNull();
        assertThat(msg.getIdempotencyKey()).isNull();
    }

    @Test
    void serialVersionUid_isSet() {
        assertThat(AgentExecuteMessage.class.getDeclaredFields()).anyMatch(f -> "serialVersionUID".equals(f.getName()));
    }

    @Test
    void implementsSerializable() {
        assertThat(java.io.Serializable.class.isAssignableFrom(AgentExecuteMessage.class)).isTrue();
    }
}
