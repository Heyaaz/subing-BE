package com.project.subing.dto.notification;

import com.project.subing.domain.notification.entity.NotificationType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSettingRequest {

    @NotNull(message = "알림 타입은 필수입니다.")
    private NotificationType notificationType;

    @NotNull(message = "활성화 여부는 필수입니다.")
    private Boolean isEnabled;
}