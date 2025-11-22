package com.project.subing.service;

import com.project.subing.domain.notification.entity.Notification;
import com.project.subing.domain.notification.entity.NotificationType;
import com.project.subing.domain.user.entity.User;
import com.project.subing.exception.auth.UnauthorizedAccessException;
import com.project.subing.exception.entity.NotificationNotFoundException;
import com.project.subing.exception.entity.UserNotFoundException;
import com.project.subing.repository.NotificationRepository;
import com.project.subing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationWebSocketService notificationWebSocketService;

    public Notification createNotification(Long userId, NotificationType type, String title,
                                          String message, Long relatedSubscriptionId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 중복 알림 방지 (같은 구독에 대한 같은 타입의 알림이 이미 존재하면 생성하지 않음)
        if (relatedSubscriptionId != null &&
            notificationRepository.existsByUser_IdAndRelatedSubscriptionIdAndType(
                userId, relatedSubscriptionId, type)) {
            return null;
        }

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .relatedSubscriptionId(relatedSubscriptionId)
                .build();

        Notification savedNotification = notificationRepository.save(notification);

        // WebSocket을 통해 실시간 알림 전송
        notificationWebSocketService.sendNotificationToUser(userId, savedNotification);

        // 읽지 않은 알림 개수 업데이트 전송
        Long unreadCount = getUnreadCount(userId);
        notificationWebSocketService.sendUnreadCountToUser(userId, unreadCount);

        return savedNotification;
    }

    @Transactional(readOnly = true)
    public List<Notification> getNotifications(Long userId) {
        return notificationRepository.findByUser_IdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUser_IdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public Long getUnreadCount(Long userId) {
        return notificationRepository.countByUser_IdAndIsReadFalse(userId);
    }

    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));

        if (!notification.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("알림 읽기 권한이 없습니다.", userId);
        }

        notification.markAsRead();

        // 읽지 않은 알림 개수 업데이트 전송
        Long unreadCount = getUnreadCount(userId);
        notificationWebSocketService.sendUnreadCountToUser(userId, unreadCount);
    }

    public void markAllAsRead(Long userId) {
        List<Notification> unreadNotifications = getUnreadNotifications(userId);
        unreadNotifications.forEach(Notification::markAsRead);

        // 읽지 않은 알림 개수 업데이트 전송 (모두 읽음 처리 후 0이 됨)
        notificationWebSocketService.sendUnreadCountToUser(userId, 0L);
    }
}
