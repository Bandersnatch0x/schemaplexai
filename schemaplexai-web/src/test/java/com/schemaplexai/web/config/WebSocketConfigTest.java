package com.schemaplexai.web.config;

import com.schemaplexai.web.websocket.AgentWebSocketHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {WebSocketConfig.class, AgentWebSocketHandler.class})
class WebSocketConfigTest {

    @Test
    void webSocketConfigBeanExists(ApplicationContext ctx) {
        assertThat(ctx.getBean(WebSocketConfig.class)).isNotNull();
    }

    @Test
    void agentWebSocketHandlerBeanExists(ApplicationContext ctx) {
        assertThat(ctx.getBean(AgentWebSocketHandler.class)).isNotNull();
    }
}
