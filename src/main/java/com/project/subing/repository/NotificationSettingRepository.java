package com.project.subing.repository;

import com.project.subing.domain.notification.entity.NotificationSetting;
import com.project.subing.domain.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {

    List<NotificationSetting> findByUserId(Long userId);

    Optional<NotificationSetting> findByUserIdAndNotificationType(Long userId, NotificationType notificationType);

    boolean existsByUserIdAndNotificationType(Long userId, NotificationType notificationType);
}