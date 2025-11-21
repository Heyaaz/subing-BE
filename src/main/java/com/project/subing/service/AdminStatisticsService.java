package com.project.subing.service;

import com.project.subing.domain.common.ServiceCategory;
import com.project.subing.domain.subscription.entity.UserSubscription;
import com.project.subing.domain.user.entity.User;
import com.project.subing.domain.user.entity.UserTier;
import com.project.subing.dto.admin.AdminStatisticsResponse;
import com.project.subing.repository.ServiceRepository;
import com.project.subing.repository.SubscriptionPlanRepository;
import com.project.subing.repository.UserRepository;
import com.project.subing.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStatisticsService {

    private final UserRepository userRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final ServiceRepository serviceRepository;
    private final SubscriptionPlanRepository planRepository;

    private static final int PRO_MONTHLY_PRICE = 9900;

    public AdminStatisticsResponse getAdminStatistics() {
        // 1. 사용자 통계
        List<User> allUsers = userRepository.findAll();
        long totalUsers = allUsers.size();
        long freeUsers = allUsers.stream()
                .filter(user -> user.getTier() == UserTier.FREE)
                .count();
        long proUsers = allUsers.stream()
                .filter(user -> user.getTier() == UserTier.PRO)
                .count();

        // 2. 구독 통계 - Service와 함께 fetch join
        List<UserSubscription> allSubscriptions = subscriptionRepository.findAllWithService();
        long activeSubscriptions = allSubscriptions.stream()
                .filter(UserSubscription::getIsActive)
                .count();

        // 3. 서비스 및 플랜 통계
        long totalServices = serviceRepository.count();
        long totalPlans = planRepository.count();

        // 4. 월별 매출 (PRO 사용자 * 9900원)
        int totalMonthlyRevenue = (int) (proUsers * PRO_MONTHLY_PRICE);

        // 5. 월별 가입자 수 (최근 12개월)
        Map<String, Long> usersByMonth = calculateUsersByMonth(allUsers);

        // 6. 카테고리별 구독 수
        Map<String, Long> subscriptionsByCategory = calculateSubscriptionsByCategory(allSubscriptions);

        return AdminStatisticsResponse.builder()
                .totalUsers(totalUsers)
                .freeUsers(freeUsers)
                .proUsers(proUsers)
                .activeSubscriptions(activeSubscriptions)
                .totalServices(totalServices)
                .totalPlans(totalPlans)
                .totalMonthlyRevenue(totalMonthlyRevenue)
                .usersByMonth(usersByMonth)
                .subscriptionsByCategory(subscriptionsByCategory)
                .build();
    }

    private Map<String, Long> calculateUsersByMonth(List<User> users) {
        Map<String, Long> usersByMonth = new HashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

        // 최근 12개월 초기화
        LocalDateTime now = LocalDateTime.now();
        for (int i = 11; i >= 0; i--) {
            String monthKey = now.minusMonths(i).format(formatter);
            usersByMonth.put(monthKey, 0L);
        }

        // 사용자 수 집계
        for (User user : users) {
            String monthKey = user.getCreatedAt().format(formatter);
            if (usersByMonth.containsKey(monthKey)) {
                usersByMonth.put(monthKey, usersByMonth.get(monthKey) + 1);
            }
        }

        return usersByMonth;
    }

    private Map<String, Long> calculateSubscriptionsByCategory(List<UserSubscription> subscriptions) {
        Map<String, Long> categoryCount = new HashMap<>();

        // 모든 카테고리 초기화
        for (ServiceCategory category : ServiceCategory.values()) {
            categoryCount.put(category.name(), 0L);
        }

        // 활성 구독만 집계
        for (UserSubscription subscription : subscriptions) {
            if (subscription.getIsActive() && subscription.getService() != null) {
                ServiceCategory category = subscription.getService().getCategory();
                categoryCount.put(category.name(), categoryCount.get(category.name()) + 1);
            }
        }

        return categoryCount;
    }
}