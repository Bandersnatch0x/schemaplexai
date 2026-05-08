package com.schemaplexai.task.mq.dto;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationMessageTest {

    @Test
    void gettersAndSetters_work() {
        NotificationMessage msg = new NotificationMessage();
        msg.setChannel("email");
        msg.setUserId(1L);
        msg.setTitle("title");
        msg.setContent("content");
        msg.setTemplateCode("T001");
        msg.setTemplateParams(Map.of("key", "value"));
        msg.setWebhookUrl("http://example.com/hook");
        msg.setWebhookMethod("POST");
        msg.setWebhookHeaders(Map.of("X-Auth", "token"));
        msg.setIdempotencyKey("key1");

        assertThat(msg.getChannel()).isEqualTo("email");
        assertThat(msg.getUserId()).isEqualTo(1L);
        assertThat(msg.getTitle()).isEqualTo("title");
        assertThat(msg.getContent()).isEqualTo("content");
        assertThat(msg.getTemplateCode()).isEqualTo("T001");
        assertThat(msg.getTemplateParams()).containsEntry("key", "value");
        assertThat(msg.getWebhookUrl()).isEqualTo("http://example.com/hook");
        assertThat(msg.getWebhookMethod()).isEqualTo("POST");
        assertThat(msg.getWebhookHeaders()).containsEntry("X-Auth", "token");
        assertThat(msg.getIdempotencyKey()).isEqualTo("key1");
    }

    @Test
    void defaultValues_areNull() {
        NotificationMessage msg = new NotificationMessage();

        assertThat(msg.getChannel()).isNull();
        assertThat(msg.getUserId()).isNull();
        assertThat(msg.getTitle()).isNull();
        assertThat(msg.getContent()).isNull();
        assertThat(msg.getTemplateCode()).isNull();
        assertThat(msg.getTemplateParams()).isNull();
        assertThat(msg.getWebhookUrl()).isNull();
        assertThat(msg.getWebhookMethod()).isNull();
        assertThat(msg.getWebhookHeaders()).isNull();
        assertThat(msg.getIdempotencyKey()).isNull();
    }

    @Test
    void serialVersionUid_isSet() {
        assertThat(NotificationMessage.class.getDeclaredFields()).anyMatch(f -> "serialVersionUID".equals(f.getName()));
    }

    @Test
    void implementsSerializable() {
        assertThat(java.io.Serializable.class.isAssignableFrom(NotificationMessage.class)).isTrue();
    }
}
