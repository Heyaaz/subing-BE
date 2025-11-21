package com.project.subing.service;

import com.project.subing.domain.notification.entity.NotificationSetting;
import com.project.subing.domain.notification.entity.NotificationType;
import com.project.subing.dto.notification.NotificationSettingRequest;
import com.project.subing.dto.notification.NotificationSettingResponse;
import com.project.subing.repository.NotificationSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationSettingService {

    private final NotificationSettingRepository notificationSettingRepository;

    // 사용자의 모든 알림 설정 조회 (없으면 기본값으로 생성)
    public List<NotificationSettingResponse> getNotificationSettings(Long userId) {
        List<NotificationSetting> settings = notificationSettingRepository.findByUserId(userId);

        // 사용자가 처음 조회하는 경우, 모든 타입에 대해 기본 설정 생성
        if (settings.isEmpty()) {
            settings = initializeDefaultSettings(userId);
        } else {
            // 일부 타입만 있는 경우, 없는 타입 추가
            List<NotificationType> existingTypes = settings.stream()
                    .map(NotificationSetting::getNotificationType)
                    .collect(Collectors.toList());

            List<NotificationSetting> missingSettings = new ArrayList<>();
            for (NotificationType type : NotificationType.values()) {
                if (!existingTypes.contains(type)) {
                    NotificationSetting newSetting = NotificationSetting.builder()
                            .userId(userId)
                            .notificationType(type)
                            .isEnabled(true)
                            .build();
                    missingSettings.add(notificationSettingRepository.save(newSetting));
                }
            }
            settings.addAll(missingSettings);
        }

        return settings.stream()
                .map(NotificationSettingResponse::from)
                .sorted((a, b) -> a.getNotificationType().compareTo(b.getNotificationType()))
                .collect(Collectors.toList());
    }

    // 특정 알림 설정 업데이트
    public NotificationSettingResponse updateNotificationSetting(Long userId, NotificationSettingRequest request) {
        NotificationSetting setting = notificationSettingRepository
                .findByUserIdAndNotificationType(userId, request.getNotificationType())
                .orElseGet(() -> NotificationSetting.builder()
                        .userId(userId)
                        .notificationType(request.getNotificationType())
                        .isEnabled(request.getIsEnabled())
                        .build());

        setting.setEnabled(request.getIsEnabled());
        NotificationSetting savedSetting = notificationSettingRepository.save(setting);

        return NotificationSettingResponse.from(savedSetting);
    }

    // 특정 알림 타입 활성화 여부 확인
    public boolean isNotificationEnabled(Long userId, NotificationType type) {
        return notificationSettingRepository
                .findByUserIdAndNotificationType(userId, type)
                .map(NotificationSetting::getIsEnabled)
                .orElse(true); // 설정이 없으면 기본적으로 활성화
    }

    // 기본 설정 초기화
    private List<NotificationSetting> initializeDefaultSettings(Long userId) {
        return Arrays.stream(NotificationType.values())
                .map(type -> NotificationSetting.builder()
                        .userId(userId)
                        .notificationType(type)
                        .isEnabled(true)
                        .build())
                .map(notificationSettingRepository::save)
                .collect(Collectors.toList());
    }
}