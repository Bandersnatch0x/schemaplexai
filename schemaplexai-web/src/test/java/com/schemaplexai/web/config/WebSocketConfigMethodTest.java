package com.schemaplexai.web.config;

import com.schemaplexai.web.websocket.AgentWebSocketHandler;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.config.annotation.ServletWebSocketHandlerRegistration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class WebSocketConfigMethodTest {

    @Test
    void registerWebSocketHandlers_addsAgentHandler() {
        AgentWebSocketHandler handler = mock(AgentWebSocketHandler.class);
        WebSocketConfig config = new WebSocketConfig(handler);

        WebSocketHandlerRegistry registry = mock(WebSocketHandlerRegistry.class);
        ServletWebSocketHandlerRegistration registration = mock(ServletWebSocketHandlerRegistration.class);
        when(registry.addHandler(any(), any(String[].class))).thenReturn(registration);
        when(registration.setAllowedOrigins(any(String[].class))).thenReturn(registration);

        config.registerWebSocketHandlers(registry);

        verify(registry).addHandler(eq(handler), eq("/ws/agent"));
        verify(registration).setAllowedOrigins(eq("*"));
    }
}
