package com.schemaplexai.model.entity.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Notification")
class NotificationTest {

    @Test
    @DisplayName("should create with no-args constructor")
    void shouldCreateWithNoArgsConstructor() {
        Notification n = new Notification();
        assertThat(n.getUserId()).isNull();
        assertThat(n.getTitle()).isNull();
        assertThat(n.getContent()).isNull();
        assertThat(n.getType()).isNull();
        assertThat(n.getRead()).isNull();
    }

    @Test
    @DisplayName("should support setters and getters")
    void shouldSupportSettersAndGetters() {
        Notification n = new Notification();
        n.setUserId(1L);
        n.setTitle("Test");
        n.setContent("Content");
        n.setType("INFO");
        n.setRead(false);

        assertThat(n.getUserId()).isEqualTo(1L);
        assertThat(n.getTitle()).isEqualTo("Test");
        assertThat(n.getContent()).isEqualTo("Content");
        assertThat(n.getType()).isEqualTo("INFO");
        assertThat(n.getRead()).isFalse();
    }

    @Test
    @DisplayName("should inherit BaseEntity fields")
    void shouldInheritBaseEntityFields() {
        Notification n = new Notification();
        n.setId(10L);
        n.setTenantId("t1");

        assertThat(n.getId()).isEqualTo(10L);
        assertThat(n.getTenantId()).isEqualTo("t1");
    }
}
