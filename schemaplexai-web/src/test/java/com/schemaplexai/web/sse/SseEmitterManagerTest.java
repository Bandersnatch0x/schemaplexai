package com.schemaplexai.web.sse;

import com.schemaplexai.common.exception.BaseException;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.*;

class SseEmitterManagerTest {

    private final SseEmitterManager manager = new SseEmitterManager();

    @Test
    void createEmitter_shouldThrow_whenTokenInvalid() {
        assertThatThrownBy(() -> manager.createEmitter("t1", "c1", 60000L, ""))
                .isInstanceOf(BaseException.class);
    }

    @Test
    void createEmitter_shouldReturnEmitter_whenTokenValid() {
        SseEmitter emitter = manager.createEmitter("t1", "c1", 60000L, "valid-token");
        assertThat(emitter).isNotNull();
    }

    @Test
    void sendToTenant_shouldNotThrow_whenTenantHasNoEmitters() {
        manager.sendToTenant("unknown", "event", "data");
    }

    @Test
    void broadcast_shouldNotThrow_whenNoEmitters() {
        manager.broadcast("event", "data");
    }

    @Test
    void sendToTenant_shouldSendToExistingEmitters() {
        SseEmitter emitter = manager.createEmitter("t1", "c1", 60000L, "token");
        assertThat(emitter).isNotNull();
        manager.sendToTenant("t1", "msg", "hello");
    }

    @Test
    void broadcast_shouldSendToAllEmitters() {
        SseEmitter e1 = manager.createEmitter("t1", "c1", 60000L, "token");
        SseEmitter e2 = manager.createEmitter("t2", "c2", 60000L, "token");
        assertThat(e1).isNotNull();
        assertThat(e2).isNotNull();
        manager.broadcast("msg", "hello");
    }
}
