package com.project.subing.service;

import com.project.subing.domain.notification.entity.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * WebSocket을 통한 실시간 알림 전송 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 특정 사용자에게 실시간 알림 전송
     *
     * @param userId 알림을 받을 사용자 ID
     * @param notification 전송할 알림 객체
     */
    public void sendNotificationToUser(Long userId, Notification notification) {
        try {
            // /user/{userId}/queue/notifications 경로로 메시지 전송
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/notifications",
                    notification
            );

            log.info("WebSocket 알림 전송 성공 - User ID: {}, Notification ID: {}, Type: {}",
                    userId, notification.getId(), notification.getType());

        } catch (Exception e) {
            log.error("WebSocket 알림 전송 실패 - User ID: {}, Notification ID: {}",
                    userId, notification.getId(), e);
        }
    }

    /**
     * 읽지 않은 알림 개수를 실시간으로 전송
     *
     * @param userId 사용자 ID
     * @param unreadCount 읽지 않은 알림 개수
     */
    public void sendUnreadCountToUser(Long userId, Long unreadCount) {
        try {
            // /user/{userId}/queue/unread-count 경로로 메시지 전송
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/unread-count",
                    unreadCount
            );

            log.info("WebSocket 읽지 않은 알림 개수 전송 성공 - User ID: {}, Count: {}",
                    userId, unreadCount);

        } catch (Exception e) {
            log.error("WebSocket 읽지 않은 알림 개수 전송 실패 - User ID: {}",
                    userId, e);
        }
    }
}