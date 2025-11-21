package com.project.subing.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket 설정 클래스
 * STOMP 프로토콜을 사용한 실시간 알림 시스템 구성
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * STOMP 엔드포인트 등록
     * 클라이언트가 WebSocket 연결을 맺을 수 있는 엔드포인트 설정
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")  // CORS 설정 (개발 환경)
                .withSockJS();                   // SockJS 폴백 지원
    }

    /**
     * 메시지 브로커 설정
     * - Simple Broker: 메모리 기반 메시지 브로커 (/topic, /queue)
     * - Application Destination Prefix: 클라이언트가 메시지를 보낼 때 사용하는 prefix
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Simple Broker 활성화 - /topic (1:N), /queue (1:1)
        registry.enableSimpleBroker("/topic", "/queue");

        // 클라이언트에서 메시지 전송 시 prefix
        registry.setApplicationDestinationPrefixes("/app");

        // 특정 사용자에게 메시지 전송 시 prefix
        registry.setUserDestinationPrefix("/user");
    }
}