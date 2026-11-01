package com.nfu.pettravel.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.SubProtocolCapable;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class AdminWebSocketHandler extends TextWebSocketHandler implements SubProtocolCapable {
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        log.info("Admin WebSocket connected: sessionId={}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        log.info("Admin WebSocket disconnected: sessionId={}", session.getId());
    }

    public void sendToAllAdmins(String message) {
        TextMessage textMessage = new TextMessage(message);
        for (WebSocketSession session : sessions.values()) {
            if (!session.isOpen()) {
                sessions.remove(session.getId(), session);
                continue;
            }
            try {
                synchronized (session) {
                    session.sendMessage(textMessage);
                }
            } catch (IOException failure) {
                sessions.remove(session.getId(), session);
                log.warn("Admin WebSocket send failed: sessionId={}", session.getId(), failure);
            }
        }
    }

    @Override
    public List<String> getSubProtocols() {
        return List.of("bearer");
    }
}
