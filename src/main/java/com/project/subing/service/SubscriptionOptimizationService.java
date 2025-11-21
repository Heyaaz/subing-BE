package com.project.subing.service;

import com.project.subing.domain.common.ServiceCategory;
import com.project.subing.domain.service.entity.ServiceEntity;
import com.project.subing.domain.service.entity.SubscriptionPlan;
import com.project.subing.domain.subscription.entity.UserSubscription;
import com.project.subing.repository.ServiceRepository;
import com.project.subing.repository.SubscriptionPlanRepository;
import com.project.subing.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SubscriptionOptimizationService {

    private final UserSubscriptionRepository userSubscriptionRepository;
    private final ServiceRepository serviceRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;

    /**
     * 중복 서비스 감지
     * 같은 카테고리의 활성 구독이 2개 이상인 경우 중복으로 판단
     */
    public List<DuplicateServiceGroup> detectDuplicateServices(Long userId) {
        List<UserSubscription> activeSubscriptions = userSubscriptionRepository.findByUserIdAndIsActiveTrue(userId);

        // 카테고리별로 그룹화
        Map<ServiceCategory, List<UserSubscription>> categoryMap = activeSubscriptions.stream()
                .collect(Collectors.groupingBy(sub -> sub.getService().getCategory()));

        // 2개 이상인 카테고리만 필터링
        List<DuplicateServiceGroup> duplicates = new ArrayList<>();
        for (Map.Entry<ServiceCategory, List<UserSubscription>> entry : categoryMap.entrySet()) {
            if (entry.getValue().size() >= 2) {
                ServiceCategory category = entry.getKey();
                List<UserSubscription> subscriptions = entry.getValue();

                // 총 비용 계산
                int totalCost = subscriptions.stream()
                        .mapToInt(UserSubscription::getMonthlyPrice)
                        .sum();

                duplicates.add(new DuplicateServiceGroup(
                        category,
                        subscriptions,
                        totalCost
                ));

                log.info("중복 서비스 감지 - userId: {}, 카테고리: {}, 구독 수: {}, 총 비용: {}",
                        userId, category.getDescription(), subscriptions.size(), totalCost);
            }
        }

        return duplicates;
    }

    /**
     * 저렴한 대안 제안
     * 현재 구독보다 저렴한 동일 카테고리의 다른 서비스 플랜 찾기
     */
    public List<CheaperAlternative> findCheaperAlternatives(Long userId) {
        List<UserSubscription> activeSubscriptions = userSubscriptionRepository.findByUserIdAndIsActiveTrue(userId);
        List<CheaperAlternative> alternatives = new ArrayList<>();

        for (UserSubscription subscription : activeSubscriptions) {
            ServiceCategory category = subscription.getService().getCategory();
            int currentPrice = subscription.getMonthlyPrice();

            // 같은 카테고리의 다른 서비스 찾기
            List<ServiceEntity> categoryServices = serviceRepository.findByCategory(category);

            for (ServiceEntity service : categoryServices) {
                // 현재 구독 중인 서비스는 제외
                if (service.getId().equals(subscription.getService().getId())) {
                    continue;
                }

                // 해당 서비스의 플랜 조회
                List<SubscriptionPlan> plans = subscriptionPlanRepository.findByServiceId(service.getId());

                // 현재 가격보다 저렴한 플랜 찾기
                for (SubscriptionPlan plan : plans) {
                    if (plan.getMonthlyPrice() < currentPrice) {
                        int savings = currentPrice - plan.getMonthlyPrice();

                        alternatives.add(new CheaperAlternative(
                                subscription,
                                service,
                                plan,
                                currentPrice,
                                plan.getMonthlyPrice(),
                                savings
                        ));

                        log.info("저렴한 대안 발견 - userId: {}, 현재: {} ({}원), 대안: {} {} ({}원), 절약: {}원",
                                userId,
                                subscription.getService().getServiceName(),
                                currentPrice,
                                service.getServiceName(),
                                plan.getPlanName(),
                                plan.getMonthlyPrice(),
                                savings);
                    }
                }
            }
        }

        // 절약 금액 기준 내림차순 정렬
        alternatives.sort((a, b) -> Integer.compare(b.getSavings(), a.getSavings()));

        return alternatives;
    }

    // DTO 클래스들
    public static class DuplicateServiceGroup {
        private final ServiceCategory category;
        private final List<UserSubscription> subscriptions;
        private final int totalCost;

        public DuplicateServiceGroup(ServiceCategory category, List<UserSubscription> subscriptions, int totalCost) {
            this.category = category;
            this.subscriptions = subscriptions;
            this.totalCost = totalCost;
        }

        public ServiceCategory getCategory() {
            return category;
        }

        public List<UserSubscription> getSubscriptions() {
            return subscriptions;
        }

        public int getTotalCost() {
            return totalCost;
        }
    }

    public static class CheaperAlternative {
        private final UserSubscription currentSubscription;
        private final ServiceEntity alternativeService;
        private final SubscriptionPlan alternativePlan;
        private final int currentPrice;
        private final int alternativePrice;
        private final int savings;

        public CheaperAlternative(UserSubscription currentSubscription, ServiceEntity alternativeService,
                                 SubscriptionPlan alternativePlan, int currentPrice, int alternativePrice, int savings) {
            this.currentSubscription = currentSubscription;
            this.alternativeService = alternativeService;
            this.alternativePlan = alternativePlan;
            this.currentPrice = currentPrice;
            this.alternativePrice = alternativePrice;
            this.savings = savings;
        }

        public UserSubscription getCurrentSubscription() {
            return currentSubscription;
        }

        public ServiceEntity getAlternativeService() {
            return alternativeService;
        }

        public SubscriptionPlan getAlternativePlan() {
            return alternativePlan;
        }

        public int getCurrentPrice() {
            return currentPrice;
        }

        public int getAlternativePrice() {
            return alternativePrice;
        }

        public int getSavings() {
            return savings;
        }
    }
}
