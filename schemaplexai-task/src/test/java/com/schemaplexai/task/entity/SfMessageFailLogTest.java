package com.schemaplexai.task.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SfMessageFailLogTest {

    @Test
    void gettersAndSetters_work() {
        SfMessageFailLog entity = new SfMessageFailLog();

        entity.setMessageId("msg-001");
        entity.setExchange("sf.exchange");
        entity.setRoutingKey("sf.agent.execute");
        entity.setPayload("{\"key\":\"value\"}");
        entity.setErrorMsg("Connection timeout");
        entity.setConsumerGroup("AgentExecuteDispatcher");
        entity.setStatus("PENDING");
        entity.setRetryCount(0);

        assertThat(entity.getMessageId()).isEqualTo("msg-001");
        assertThat(entity.getExchange()).isEqualTo("sf.exchange");
        assertThat(entity.getRoutingKey()).isEqualTo("sf.agent.execute");
        assertThat(entity.getPayload()).isEqualTo("{\"key\":\"value\"}");
        assertThat(entity.getErrorMsg()).isEqualTo("Connection timeout");
        assertThat(entity.getConsumerGroup()).isEqualTo("AgentExecuteDispatcher");
        assertThat(entity.getStatus()).isEqualTo("PENDING");
        assertThat(entity.getRetryCount()).isEqualTo(0);
    }

    @Test
    void equals_sameId_returnsTrue() {
        SfMessageFailLog e1 = new SfMessageFailLog();
        e1.setId(1L);
        SfMessageFailLog e2 = new SfMessageFailLog();
        e2.setId(1L);

        assertThat(e1).isEqualTo(e2);
    }

    @Test
    void equals_differentId_returnsFalse() {
        SfMessageFailLog e1 = new SfMessageFailLog();
        e1.setId(1L);
        SfMessageFailLog e2 = new SfMessageFailLog();
        e2.setId(2L);

        assertThat(e1).isNotEqualTo(e2);
    }

    @Test
    void hashCode_sameId_returnsSame() {
        SfMessageFailLog e1 = new SfMessageFailLog();
        e1.setId(5L);
        SfMessageFailLog e2 = new SfMessageFailLog();
        e2.setId(5L);

        assertThat(e1.hashCode()).isEqualTo(e2.hashCode());
    }

    @Test
    void serialVersionUid_isSet() {
        assertThat(SfMessageFailLog.class.getDeclaredFields()).anyMatch(f -> "serialVersionUID".equals(f.getName()));
    }

    @Test
    void inheritsBaseEntityFields() {
        SfMessageFailLog entity = new SfMessageFailLog();
        entity.setId(1L);
        entity.setTenantId("t1");
        entity.setDeleted(0);

        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getTenantId()).isEqualTo("t1");
        assertThat(entity.getDeleted()).isEqualTo(0);
    }
}
