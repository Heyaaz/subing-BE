package com.project.subing.repository;

import com.project.subing.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUser_IdOrderByCreatedAtDesc(Long userId);

    List<Notification> findByUser_IdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    Long countByUser_IdAndIsReadFalse(Long userId);

    boolean existsByUser_IdAndRelatedSubscriptionIdAndType(Long userId, Long subscriptionId, com.project.subing.domain.notification.entity.NotificationType type);
}