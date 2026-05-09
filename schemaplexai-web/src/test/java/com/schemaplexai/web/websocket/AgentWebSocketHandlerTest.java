package com.schemaplexai.web.websocket;

import com.schemaplexai.common.constants.CommonConstants;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AgentWebSocketHandlerTest {

    private final AgentWebSocketHandler handler = new AgentWebSocketHandler();

    private WebSocketSession mockSession(String sessionId, String tenantId, String token) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(sessionId);
        HttpHeaders headers = new HttpHeaders();
        if (tenantId != null) {
            headers.add(CommonConstants.HEADER_TENANT_ID, tenantId);
        }
        if (token != null) {
            headers.add(CommonConstants.HEADER_AUTHORIZATION, CommonConstants.TOKEN_PREFIX + token);
        }
        when(session.getHandshakeHeaders()).thenReturn(headers);
        when(session.isOpen()).thenReturn(true);
        return session;
    }

    @Test
    void afterConnectionEstablished_rejectsMissingToken() throws IOException {
        WebSocketSession session = mockSession("s1", "t1", null);
        handler.afterConnectionEstablished(session);
        verify(session).close(CloseStatus.POLICY_VIOLATION);
    }

    @Test
    void afterConnectionEstablished_acceptsValidToken() throws IOException {
        WebSocketSession session = mockSession("s1", "t1", "valid-token");
        handler.afterConnectionEstablished(session);
        verify(session, never()).close(any());
    }

    @Test
    void handleTextMessage_echoesPayload() throws IOException {
        WebSocketSession session = mockSession("s1", "t1", "token");
        handler.afterConnectionEstablished(session);
        handler.handleTextMessage(session, new TextMessage("hello"));
        verify(session).sendMessage(new TextMessage("Echo: hello"));
    }

    @Test
    void sendToTenant_deliversToOpenSessions() throws IOException {
        WebSocketSession s1 = mockSession("s1", "t1", "token");
        WebSocketSession s2 = mockSession("s2", "t1", "token");
        handler.afterConnectionEstablished(s1);
        handler.afterConnectionEstablished(s2);

        handler.sendToTenant("t1", "broadcast");
        verify(s1).sendMessage(new TextMessage("broadcast"));
        verify(s2).sendMessage(new TextMessage("broadcast"));
    }

    @Test
    void sendToTenant_ignoresClosedSessions() throws IOException {
        WebSocketSession s1 = mockSession("s1", "t1", "token");
        WebSocketSession s2 = mockSession("s2", "t1", "token");
        when(s2.isOpen()).thenReturn(false);
        handler.afterConnectionEstablished(s1);
        handler.afterConnectionEstablished(s2);

        handler.sendToTenant("t1", "msg");
        verify(s1).sendMessage(new TextMessage("msg"));
        verify(s2, never()).sendMessage(any());
    }

    @Test
    void sendToSession_deliversToSpecificSession() throws IOException {
        WebSocketSession s1 = mockSession("s1", "t1", "token");
        handler.afterConnectionEstablished(s1);
        handler.sendToSession("s1", "direct");
        verify(s1).sendMessage(new TextMessage("direct"));
    }

    @Test
    void afterConnectionClosed_removesSession() throws IOException {
        WebSocketSession s1 = mockSession("s1", "t1", "token");
        handler.afterConnectionEstablished(s1);
        handler.afterConnectionClosed(s1, CloseStatus.NORMAL);
        handler.sendToTenant("t1", "msg");
        verify(s1, never()).sendMessage(new TextMessage("msg"));
    }
}
