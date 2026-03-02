package com.project.subing.service;

import com.project.subing.domain.common.ServiceCategory;
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
        // count 쿼리로 전체 엔티티 로드 방지
        long totalUsers = userRepository.count();
        long freeUsers = userRepository.countByTier(UserTier.FREE);
        long proUsers = userRepository.countByTier(UserTier.PRO);

        long activeSubscriptions = subscriptionRepository.countActiveSubscriptions();

        long totalServices = serviceRepository.count();
        long totalPlans = planRepository.count();

        int totalMonthlyRevenue = (int) (proUsers * PRO_MONTHLY_PRICE);

        Map<String, Long> usersByMonth = calculateUsersByMonth();
        Map<String, Long> subscriptionsByCategory = calculateSubscriptionsByCategory();

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

    private Map<String, Long> calculateUsersByMonth() {
        Map<String, Long> usersByMonth = new HashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

        // 최근 12개월 초기화
        LocalDateTime now = LocalDateTime.now();
        for (int i = 11; i >= 0; i--) {
            String monthKey = now.minusMonths(i).format(formatter);
            usersByMonth.put(monthKey, 0L);
        }

        // DB에서 GROUP BY로 집계 (전체 유저 로드 대신)
        LocalDateTime since = now.minusMonths(12);
        List<Object[]> results = userRepository.countUsersByMonthSince(since);
        for (Object[] row : results) {
            String month = (String) row[0];
            Long count = (Long) row[1];
            if (usersByMonth.containsKey(month)) {
                usersByMonth.put(month, count);
            }
        }

        return usersByMonth;
    }

    private Map<String, Long> calculateSubscriptionsByCategory() {
        Map<String, Long> categoryCount = new HashMap<>();

        for (ServiceCategory category : ServiceCategory.values()) {
            categoryCount.put(category.name(), 0L);
        }

        // DB에서 GROUP BY로 집계 (전체 구독 로드 대신)
        List<Object[]> results = subscriptionRepository.countActiveSubscriptionsByCategory();
        for (Object[] row : results) {
            ServiceCategory category = (ServiceCategory) row[0];
            Long count = (Long) row[1];
            categoryCount.put(category.name(), count);
        }

        return categoryCount;
    }
}
