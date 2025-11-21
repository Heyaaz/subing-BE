package com.project.subing.dto.notification;

import com.project.subing.domain.notification.entity.NotificationSetting;
import com.project.subing.domain.notification.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSettingResponse {

    private Long id;
    private NotificationType notificationType;
    private String description;
    private Boolean isEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static NotificationSettingResponse from(NotificationSetting setting) {
        return NotificationSettingResponse.builder()
                .id(setting.getId())
                .notificationType(setting.getNotificationType())
                .description(setting.getNotificationType().getDescription())
                .isEnabled(setting.getIsEnabled())
                .createdAt(setting.getCreatedAt())
                .updatedAt(setting.getUpdatedAt())
                .build();
    }
}