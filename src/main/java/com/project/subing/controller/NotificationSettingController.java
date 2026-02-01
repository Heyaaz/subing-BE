package com.project.subing.controller;

import com.project.subing.dto.common.ApiResponse;
import com.project.subing.dto.notification.NotificationSettingRequest;
import com.project.subing.dto.notification.NotificationSettingResponse;
import com.project.subing.service.NotificationSettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notification-settings")
@RequiredArgsConstructor
public class NotificationSettingController {

    private final NotificationSettingService notificationSettingService;

    // 사용자의 모든 알림 설정 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationSettingResponse>>> getNotificationSettings(
            @AuthenticationPrincipal Long userId) {
        List<NotificationSettingResponse> settings = notificationSettingService.getNotificationSettings(userId);
        return ResponseEntity.ok(ApiResponse.success(settings, "알림 설정을 조회했습니다."));
    }

    // 알림 설정 업데이트
    @PutMapping
    public ResponseEntity<ApiResponse<NotificationSettingResponse>> updateNotificationSetting(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody NotificationSettingRequest request) {
        NotificationSettingResponse response = notificationSettingService.updateNotificationSetting(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "알림 설정이 업데이트되었습니다."));
    }
}