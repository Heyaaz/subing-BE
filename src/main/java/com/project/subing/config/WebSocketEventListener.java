package com.project.subing.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * WebSocket 이벤트 리스너
 * WebSocket 연결/종료 이벤트를 감지하고 로깅
 */
@Slf4j
@Component
public class WebSocketEventListener {

    /**
     * WebSocket 연결 이벤트 처리
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        log.info("WebSocket 연결됨 - Session ID: {}", sessionId);
    }

    /**
     * WebSocket 연결 종료 이벤트 처리
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        log.info("WebSocket 연결 종료됨 - Session ID: {}", sessionId);
    }
}