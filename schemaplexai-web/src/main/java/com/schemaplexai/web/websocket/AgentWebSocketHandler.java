package com.schemaplexai.web.websocket;

import com.schemaplexai.common.constants.CommonConstants;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Component
public class AgentWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, CopyOnWriteArraySet<WebSocketSession>> tenantSessions = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> sessionStore = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        String token = resolveToken(session);
        if (!StringUtils.hasText(token)) {
            log.warn("WebSocket connection rejected: missing token, session={}", session.getId());
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        String tenantId = getTenantId(session);
        tenantSessions.computeIfAbsent(tenantId, k -> new CopyOnWriteArraySet<>()).add(session);
        sessionStore.put(session.getId(), session);
        log.info("WebSocket connected: session={}, tenant={}", session.getId(), tenantId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        log.debug("WebSocket received: session={}, payload={}", session.getId(), payload);

        try {
            session.sendMessage(new TextMessage("Echo: " + payload));
        } catch (IOException e) {
            log.error("WebSocket send failed", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String tenantId = getTenantId(session);
        CopyOnWriteArraySet<WebSocketSession> sessions = tenantSessions.get(tenantId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                tenantSessions.remove(tenantId);
            }
        }
        sessionStore.remove(session.getId());
        log.info("WebSocket disconnected: session={}, tenant={}, status={}", session.getId(), tenantId, status);
    }

    public void sendToTenant(String tenantId, String message) {
        CopyOnWriteArraySet<WebSocketSession> sessions = tenantSessions.get(tenantId);
        if (sessions == null) {
            return;
        }
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    log.error("WebSocket send to tenant failed", e);
                }
            }
        }
    }

    public void sendToSession(String sessionId, String message) {
        WebSocketSession session = sessionStore.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                log.error("WebSocket send to session failed", e);
            }
        }
    }

    private String getTenantId(WebSocketSession session) {
        String tenantId = session.getHandshakeHeaders().getFirst(CommonConstants.HEADER_TENANT_ID);
        return tenantId != null ? tenantId : "default";
    }

    private String resolveToken(WebSocketSession session) {
        String bearerToken = session.getHandshakeHeaders().getFirst(CommonConstants.HEADER_AUTHORIZATION);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(CommonConstants.TOKEN_PREFIX)) {
            return bearerToken.substring(CommonConstants.TOKEN_PREFIX.length());
        }
        return null;
    }
}
