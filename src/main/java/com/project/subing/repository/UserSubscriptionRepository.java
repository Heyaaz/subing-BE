package com.project.subing.repository;

import com.project.subing.domain.common.ServiceCategory;
import com.project.subing.domain.subscription.entity.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {

    List<UserSubscription> findByUserId(Long userId);

    List<UserSubscription> findByUserIdAndIsActive(Long userId, Boolean isActive);

    List<UserSubscription> findByUserIdAndIsActiveTrue(Long userId);

    Optional<UserSubscription> findByIdAndUserId(Long id, Long userId);

    // 카테고리별 필터링
    @Query("SELECT us FROM UserSubscription us JOIN FETCH us.service s WHERE us.user.id = :userId AND s.category = :category")
    List<UserSubscription> findByUserIdAndServiceCategory(@Param("userId") Long userId, @Param("category") ServiceCategory category);

    // 카테고리 + 활성 상태 필터링
    @Query("SELECT us FROM UserSubscription us JOIN FETCH us.service s WHERE us.user.id = :userId AND s.category = :category AND us.isActive = :isActive")
    List<UserSubscription> findByUserIdAndServiceCategoryAndIsActive(@Param("userId") Long userId, @Param("category") ServiceCategory category, @Param("isActive") Boolean isActive);

    // 특정 서비스를 구독 중인 활성 사용자 찾기
    @Query("SELECT us FROM UserSubscription us JOIN FETCH us.user u WHERE us.service.id = :serviceId AND us.isActive = true")
    List<UserSubscription> findActiveSubscriptionsByServiceId(@Param("serviceId") Long serviceId);

    // 관리자용: 모든 구독을 Service와 함께 조회 (LAZY 로딩 방지)
    @Query("SELECT DISTINCT us FROM UserSubscription us LEFT JOIN FETCH us.service")
    List<UserSubscription> findAllWithService();
}
