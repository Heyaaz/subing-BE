package com.project.subing.service;

import com.project.subing.domain.common.ServiceCategory;
import com.project.subing.domain.service.entity.ServiceEntity;
import com.project.subing.domain.service.entity.SubscriptionPlan;
import com.project.subing.domain.subscription.entity.UserSubscription;
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
    private final SubscriptionPlanRepository subscriptionPlanRepository;

    /**
     * 중복 서비스 감지
     * 같은 카테고리의 활성 구독이 2개 이상인 경우 중복으로 판단
     */
    public List<DuplicateServiceGroup> detectDuplicateServices(Long userId) {
        List<UserSubscription> activeSubscriptions =
                userSubscriptionRepository.findByUserIdAndIsActiveTrueWithService(userId);

        // 카테고리별로 그룹화
        Map<ServiceCategory, List<UserSubscription>> categoryMap = activeSubscriptions.stream()
                .collect(Collectors.groupingBy(sub -> sub.getService().getCategory()));

        // 2개 이상인 카테고리만 필터링
        List<DuplicateServiceGroup> duplicates = new ArrayList<>();
        for (Map.Entry<ServiceCategory, List<UserSubscription>> entry : categoryMap.entrySet()) {
            if (entry.getValue().size() >= 2) {
                ServiceCategory category = entry.getKey();
                List<UserSubscription> subscriptions = entry.getValue();

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
     * 저렴한 대안 제안 (N+1 쿼리 최적화 + 동일 서비스 다운그레이드 포함)
     */
    public List<CheaperAlternative> findCheaperAlternatives(Long userId) {
        // 1. 활성 구독 조회 (Service JOIN FETCH) - 1 쿼리
        List<UserSubscription> activeSubscriptions =
                userSubscriptionRepository.findByUserIdAndIsActiveTrueWithService(userId);

        if (activeSubscriptions.isEmpty()) return Collections.emptyList();

        // 2. 구독 중인 카테고리 수집
        Set<ServiceCategory> categories = activeSubscriptions.stream()
                .map(sub -> sub.getService().getCategory())
                .collect(Collectors.toSet());

        // 3. 해당 카테고리의 모든 플랜 한 번에 조회 - 1 쿼리
        List<SubscriptionPlan> allCategoryPlans =
                subscriptionPlanRepository.findByServiceCategoryIn(categories);

        // 4. Map 변환 (서비스ID → 플랜 목록)
        Map<Long, List<SubscriptionPlan>> plansByServiceId = allCategoryPlans.stream()
                .collect(Collectors.groupingBy(plan -> plan.getService().getId()));

        // 5. 서비스ID → ServiceEntity Map
        Map<Long, ServiceEntity> serviceById = allCategoryPlans.stream()
                .map(SubscriptionPlan::getService)
                .collect(Collectors.toMap(ServiceEntity::getId, s -> s, (a, b) -> a));

        // 6. 루프에서 Map 조회만 사용 (추가 쿼리 없음)
        List<CheaperAlternative> alternatives = new ArrayList<>();

        for (UserSubscription subscription : activeSubscriptions) {
            int currentPrice = subscription.getMonthlyPrice();
            Long currentServiceId = subscription.getService().getId();
            ServiceCategory currentCategory = subscription.getService().getCategory();

            // 1단계: 동일 서비스 내 더 저렴한 플랜 (다운그레이드)
            List<SubscriptionPlan> sameServicePlans =
                    plansByServiceId.getOrDefault(currentServiceId, Collections.emptyList());
            for (SubscriptionPlan plan : sameServicePlans) {
                if (plan.getMonthlyPrice() < currentPrice) {
                    int savings = currentPrice - plan.getMonthlyPrice();

                    alternatives.add(new CheaperAlternative(
                            subscription, subscription.getService(), plan,
                            currentPrice, plan.getMonthlyPrice(), savings, true
                    ));

                    log.info("다운그레이드 대안 발견 - userId: {}, 서비스: {}, 현재: {}원, 대안 플랜: {} ({}원), 절약: {}원",
                            userId, subscription.getService().getServiceName(),
                            currentPrice, plan.getPlanName(), plan.getMonthlyPrice(), savings);
                }
            }

            // 2단계: 타 서비스 대안 (같은 카테고리, 다른 서비스)
            for (Map.Entry<Long, List<SubscriptionPlan>> entry : plansByServiceId.entrySet()) {
                Long serviceId = entry.getKey();
                if (serviceId.equals(currentServiceId)) continue;

                ServiceEntity altService = serviceById.get(serviceId);
                if (altService == null || !altService.getCategory().equals(currentCategory)) continue;

                for (SubscriptionPlan plan : entry.getValue()) {
                    if (plan.getMonthlyPrice() < currentPrice) {
                        int savings = currentPrice - plan.getMonthlyPrice();

                        alternatives.add(new CheaperAlternative(
                                subscription, altService, plan,
                                currentPrice, plan.getMonthlyPrice(), savings, false
                        ));

                        log.info("저렴한 대안 발견 - userId: {}, 현재: {} ({}원), 대안: {} {} ({}원), 절약: {}원",
                                userId, subscription.getService().getServiceName(), currentPrice,
                                altService.getServiceName(), plan.getPlanName(),
                                plan.getMonthlyPrice(), savings);
                    }
                }
            }
        }

        // 정렬: 동일 서비스 다운그레이드 우선, 그 안에서 절약 금액 내림차순
        alternatives.sort((a, b) -> {
            if (a.isSameService() != b.isSameService()) {
                return a.isSameService() ? -1 : 1;
            }
            return Integer.compare(b.getSavings(), a.getSavings());
        });

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
        private final boolean isSameService;

        public CheaperAlternative(UserSubscription currentSubscription, ServiceEntity alternativeService,
                                 SubscriptionPlan alternativePlan, int currentPrice, int alternativePrice,
                                 int savings, boolean isSameService) {
            this.currentSubscription = currentSubscription;
            this.alternativeService = alternativeService;
            this.alternativePlan = alternativePlan;
            this.currentPrice = currentPrice;
            this.alternativePrice = alternativePrice;
            this.savings = savings;
            this.isSameService = isSameService;
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

        public boolean isSameService() {
            return isSameService;
        }
    }
}
