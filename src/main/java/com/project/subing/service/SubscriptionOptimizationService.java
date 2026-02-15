package com.project.subing.service;

import com.project.subing.domain.common.BillingCycle;
import com.project.subing.domain.common.ServiceCategory;
import com.project.subing.domain.service.entity.ServiceEntity;
import com.project.subing.domain.service.entity.SubscriptionPlan;
import com.project.subing.domain.subscription.entity.UserSubscription;
import com.project.subing.dto.optimization.OptimizationEventRequest;
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
    private final OptimizationEngineConfigService optimizationEngineConfigService;

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
        OptimizationEnginePolicy policy = optimizationEngineConfigService.getEffectivePolicy();
        long startedAtNs = System.nanoTime();
        long searchTimeoutNs = toNanoTimeout(policy.getCandidateSearchTimeoutMs());

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

        int topKPlansPerService = Math.max(1, policy.getTopKPlansPerService());
        Map<Long, List<SubscriptionPlan>> candidatePlansByServiceId = plansByServiceId.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .sorted(Comparator.comparingInt(SubscriptionPlan::getMonthlyPrice))
                                .limit(topKPlansPerService)
                                .collect(Collectors.toList())
                ));

        // 5. 서비스ID → ServiceEntity Map
        Map<Long, ServiceEntity> serviceById = allCategoryPlans.stream()
                .map(SubscriptionPlan::getService)
                .collect(Collectors.toMap(ServiceEntity::getId, s -> s, (a, b) -> a));

        // 6. 루프에서 Map 조회만 사용 (추가 쿼리 없음)
        List<CheaperAlternative> alternatives = new ArrayList<>();

        boolean timeoutReached = false;

        outer:
        for (UserSubscription subscription : activeSubscriptions) {
            if (isTimeout(startedAtNs, searchTimeoutNs)) {
                timeoutReached = true;
                break;
            }

            int currentMonthlyCost = normalizeMonthlyCost(subscription.getMonthlyPrice(), subscription.getBillingCycle(), policy);
            Long currentServiceId = subscription.getService().getId();
            ServiceCategory currentCategory = subscription.getService().getCategory();

            // 1단계: 동일 서비스 내 더 저렴한 플랜 (다운그레이드)
            List<SubscriptionPlan> sameServicePlans =
                    candidatePlansByServiceId.getOrDefault(currentServiceId, Collections.emptyList());
            for (SubscriptionPlan plan : sameServicePlans) {
                if (isTimeout(startedAtNs, searchTimeoutNs)) {
                    timeoutReached = true;
                    break outer;
                }
                int alternativeMonthlyCost = normalizeMonthlyCost(plan.getMonthlyPrice(), BillingCycle.MONTHLY, policy);
                if (alternativeMonthlyCost < currentMonthlyCost) {
                    int savings = currentMonthlyCost - alternativeMonthlyCost;
                    int switchCost = calculateSwitchCost(subscription, subscription.getService(), true, policy);
                    int netSavings = savings - switchCost;
                    if (netSavings <= 0) {
                        continue;
                    }
                    int confidenceScore = calculateConfidenceScore(subscription, savings, netSavings, switchCost, true, policy);
                    List<String> reasonCodes = buildReasonCodes(subscription, true, switchCost, confidenceScore);

                    alternatives.add(new CheaperAlternative(
                            subscription, subscription.getService(), plan,
                            currentMonthlyCost, alternativeMonthlyCost, savings, true,
                            switchCost, netSavings, confidenceScore, reasonCodes
                    ));

                    log.info("다운그레이드 대안 발견 - userId: {}, 서비스: {}, 현재: {}원, 대안 플랜: {} ({}원), 절약: {}원, 순절약: {}원",
                            userId, subscription.getService().getServiceName(),
                            currentMonthlyCost, plan.getPlanName(), alternativeMonthlyCost, savings, netSavings);
                }
            }

            // 2단계: 타 서비스 대안 (같은 카테고리, 다른 서비스)
            for (Map.Entry<Long, List<SubscriptionPlan>> entry : candidatePlansByServiceId.entrySet()) {
                if (isTimeout(startedAtNs, searchTimeoutNs)) {
                    timeoutReached = true;
                    break outer;
                }
                Long serviceId = entry.getKey();
                if (serviceId.equals(currentServiceId)) continue;

                ServiceEntity altService = serviceById.get(serviceId);
                if (altService == null || !altService.getCategory().equals(currentCategory)) continue;

                for (SubscriptionPlan plan : entry.getValue()) {
                    if (isTimeout(startedAtNs, searchTimeoutNs)) {
                        timeoutReached = true;
                        break outer;
                    }
                    int alternativeMonthlyCost = normalizeMonthlyCost(plan.getMonthlyPrice(), BillingCycle.MONTHLY, policy);
                    if (alternativeMonthlyCost < currentMonthlyCost) {
                        int savings = currentMonthlyCost - alternativeMonthlyCost;
                        int switchCost = calculateSwitchCost(subscription, altService, false, policy);
                        int netSavings = savings - switchCost;
                        if (netSavings <= 0) {
                            continue;
                        }
                        int confidenceScore = calculateConfidenceScore(subscription, savings, netSavings, switchCost, false, policy);
                        List<String> reasonCodes = buildReasonCodes(subscription, false, switchCost, confidenceScore);

                        alternatives.add(new CheaperAlternative(
                                subscription, altService, plan,
                                currentMonthlyCost, alternativeMonthlyCost, savings, false,
                                switchCost, netSavings, confidenceScore, reasonCodes
                        ));

                        log.info("저렴한 대안 발견 - userId: {}, 현재: {} ({}원), 대안: {} {} ({}원), 절약: {}원, 순절약: {}원",
                                userId, subscription.getService().getServiceName(), currentMonthlyCost,
                                altService.getServiceName(), plan.getPlanName(),
                                alternativeMonthlyCost, savings, netSavings);
                    }
                }
            }
        }

        // 정렬: 동일 서비스 다운그레이드 우선, 그 안에서 절약 금액 내림차순
        alternatives.sort((a, b) -> {
            if (a.isSameService() != b.isSameService()) {
                return a.isSameService() ? -1 : 1;
            }
            return Integer.compare(b.getNetSavings(), a.getNetSavings());
        });

        if (timeoutReached) {
            log.warn("최적화 후보 탐색 타임아웃 - userId: {}, timeoutMs: {}, partialResultCount: {}",
                    userId, policy.getCandidateSearchTimeoutMs(), alternatives.size());
        }

        return alternatives;
    }

    /**
     * 전역 최적화 MVP:
     * - 구독 1건당 최적 대안 1개 선택
     * - 최대 변경 건수 제약 적용
     * - 타임아웃 시 정렬 기반 fallback
     */
    public List<CheaperAlternative> selectPortfolioOptimizedAlternatives(List<CheaperAlternative> alternatives) {
        OptimizationEnginePolicy policy = optimizationEngineConfigService.getEffectivePolicy();
        if (alternatives == null || alternatives.isEmpty()) {
            return Collections.emptyList();
        }

        long startedAtNs = System.nanoTime();
        long optimizeTimeoutNs = toNanoTimeout(policy.getPortfolioOptimizeTimeoutMs());

        Map<Long, CheaperAlternative> bestBySubscription = new HashMap<>();
        boolean timeoutReached = false;

        for (CheaperAlternative alternative : alternatives) {
            if (isTimeout(startedAtNs, optimizeTimeoutNs)) {
                timeoutReached = true;
                break;
            }

            Long subscriptionId = alternative.getCurrentSubscription().getId();
            bestBySubscription.merge(subscriptionId, alternative, this::pickBetterAlternative);
        }

        List<CheaperAlternative> selected = new ArrayList<>(bestBySubscription.values());
        selected.sort((a, b) -> {
            int netSavingsDiff = Integer.compare(b.getNetSavings(), a.getNetSavings());
            if (netSavingsDiff != 0) return netSavingsDiff;
            if (a.isSameService() != b.isSameService()) return a.isSameService() ? -1 : 1;
            return Integer.compare(b.getConfidenceScore(), a.getConfidenceScore());
        });

        int maxChangesPerRun = Math.max(1, policy.getMaxChangesPerRun());
        if (selected.size() > maxChangesPerRun) {
            selected = new ArrayList<>(selected.subList(0, maxChangesPerRun));
        }

        if (timeoutReached) {
            log.warn("포트폴리오 최적화 타임아웃 - timeoutMs: {}, fallbackCount: {}",
                    policy.getPortfolioOptimizeTimeoutMs(), selected.size());
        }
        return selected;
    }

    public void trackOptimizationEvent(Long userId, OptimizationEventRequest request) {
        OptimizationEnginePolicy policy = optimizationEngineConfigService.getEffectivePolicy();
        if (!policy.isTrackingEnabled()) {
            return;
        }

        String normalizedEventType = request.getEventType().trim().toUpperCase(Locale.ROOT);
        Set<String> supportedEvents = Set.of("IMPRESSION", "CLICK_ALTERNATIVE", "CLICK_MANAGE", "DISMISS", "REFRESH");
        if (!supportedEvents.contains(normalizedEventType)) {
            throw new IllegalArgumentException("지원하지 않는 optimization 이벤트 타입입니다: " + request.getEventType());
        }

        log.info("optimization_event userId={} eventType={} source={} suggestionType={} currentSubscriptionId={} alternativeServiceId={} metadata={}",
                userId,
                normalizedEventType,
                request.getSource(),
                request.getSuggestionType(),
                request.getCurrentSubscriptionId(),
                request.getAlternativeServiceId(),
                trimMetadata(request.getMetadata()));
    }

    private String trimMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "{}";
        }
        String serialized = metadata.toString();
        if (serialized.length() > 500) {
            return serialized.substring(0, 500) + "...";
        }
        return serialized;
    }

    private long toNanoTimeout(int timeoutMs) {
        return Math.max(1, timeoutMs) * 1_000_000L;
    }

    private boolean isTimeout(long startedAtNs, long timeoutNs) {
        return System.nanoTime() - startedAtNs > timeoutNs;
    }

    private CheaperAlternative pickBetterAlternative(CheaperAlternative a, CheaperAlternative b) {
        int netSavingsDiff = Integer.compare(a.getNetSavings(), b.getNetSavings());
        if (netSavingsDiff != 0) {
            return netSavingsDiff >= 0 ? a : b;
        }

        if (a.isSameService() != b.isSameService()) {
            return a.isSameService() ? a : b;
        }

        return a.getConfidenceScore() >= b.getConfidenceScore() ? a : b;
    }

    private int normalizeMonthlyCost(int price, BillingCycle billingCycle, OptimizationEnginePolicy policy) {
        if (billingCycle == BillingCycle.YEARLY) {
            int divisor = Math.max(1, policy.getYearlyDivisor());
            return (int) Math.ceil(price / (double) divisor);
        }
        return price;
    }

    private int calculateSwitchCost(UserSubscription subscription, ServiceEntity alternativeService,
                                    boolean sameService, OptimizationEnginePolicy policy) {
        if (sameService) {
            return Math.max(0, policy.getSameServiceSwitchCost());
        }

        int baseCost = Math.max(0, policy.getCrossServiceBaseSwitchCost());
        if (subscription.getBillingCycle() == BillingCycle.YEARLY) {
            baseCost += Math.max(0, policy.getYearlyBillingPenalty());
        }

        if (subscription.getService().getCategory() != alternativeService.getCategory()) {
            baseCost += Math.max(0, policy.getCrossCategoryPenalty());
        }
        return baseCost;
    }

    private int calculateConfidenceScore(UserSubscription subscription, int savings, int netSavings,
                                         int switchCost, boolean sameService, OptimizationEnginePolicy policy) {
        int score = 65;
        score += sameService ? 20 : 5;
        score += switchCost == 0 ? 5 : 0;
        score += netSavings >= 10000 ? 10 : 0;
        score -= netSavings <= 2000 ? 10 : 0;
        score -= (subscription.getBillingCycle() == BillingCycle.YEARLY && !sameService) ? 15 : 0;
        score -= savings <= 1000 ? 5 : 0;
        score += policy.getMaxChangesPerRun() <= 3 ? 2 : 0;
        return Math.max(0, Math.min(100, score));
    }

    private List<String> buildReasonCodes(UserSubscription subscription, boolean sameService, int switchCost, int confidenceScore) {
        List<String> reasonCodes = new ArrayList<>();
        reasonCodes.add(sameService ? "SAME_SERVICE_DOWNGRADE" : "CATEGORY_SWITCH");

        if (subscription.getBillingCycle() == BillingCycle.YEARLY) {
            reasonCodes.add("YEARLY_BILLING_NORMALIZED");
        } else {
            reasonCodes.add("MONTHLY_BILLING_BASE");
        }

        if (switchCost > 0) {
            reasonCodes.add("SWITCH_COST_APPLIED");
        }

        if (confidenceScore >= 80) {
            reasonCodes.add("HIGH_CONFIDENCE");
        } else if (confidenceScore >= 60) {
            reasonCodes.add("MEDIUM_CONFIDENCE");
        } else {
            reasonCodes.add("LOW_CONFIDENCE");
        }
        return reasonCodes;
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
        private final int switchCost;
        private final int netSavings;
        private final int confidenceScore;
        private final List<String> reasonCodes;

        public CheaperAlternative(UserSubscription currentSubscription, ServiceEntity alternativeService,
                                 SubscriptionPlan alternativePlan, int currentPrice, int alternativePrice,
                                 int savings, boolean isSameService, int switchCost, int netSavings,
                                 int confidenceScore, List<String> reasonCodes) {
            this.currentSubscription = currentSubscription;
            this.alternativeService = alternativeService;
            this.alternativePlan = alternativePlan;
            this.currentPrice = currentPrice;
            this.alternativePrice = alternativePrice;
            this.savings = savings;
            this.isSameService = isSameService;
            this.switchCost = switchCost;
            this.netSavings = netSavings;
            this.confidenceScore = confidenceScore;
            this.reasonCodes = List.copyOf(reasonCodes);
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

        public int getSwitchCost() {
            return switchCost;
        }

        public int getNetSavings() {
            return netSavings;
        }

        public int getConfidenceScore() {
            return confidenceScore;
        }

        public List<String> getReasonCodes() {
            return reasonCodes;
        }
    }
}
