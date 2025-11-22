package com.project.subing.exception.entity;

import com.project.subing.exception.ErrorCode;

/**
 * 알림을 찾을 수 없을 때 발생하는 예외
 *
 * HTTP Status: 404 Not Found
 */
public class NotificationNotFoundException extends EntityNotFoundException {

    public NotificationNotFoundException(Long notificationId) {
        super(ErrorCode.NOTIFICATION_NOT_FOUND, "알림을 찾을 수 없습니다: ID " + notificationId);
    }
}