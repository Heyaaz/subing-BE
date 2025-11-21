package com.project.subing.service;

import com.project.subing.domain.notification.entity.NotificationType;
import com.project.subing.domain.service.entity.ServiceEntity;
import com.project.subing.domain.service.entity.SubscriptionPlan;
import com.project.subing.domain.subscription.entity.UserSubscription;
import com.project.subing.dto.service.PlanCreateRequest;
import com.project.subing.dto.service.PlanUpdateRequest;
import com.project.subing.dto.service.SubscriptionPlanResponse;
import com.project.subing.repository.ServiceRepository;
import com.project.subing.repository.SubscriptionPlanRepository;
import com.project.subing.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanService {

    private final SubscriptionPlanRepository planRepository;
    private final ServiceRepository serviceRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final NotificationService notificationService;
    private final NotificationWebSocketService notificationWebSocketService;
    private final NotificationSettingService notificationSettingService;

    public List<SubscriptionPlanResponse> getAllPlans() {
        List<SubscriptionPlan> plans = planRepository.findAll();
        List<SubscriptionPlanResponse> responses = new ArrayList<>();

        for (SubscriptionPlan plan : plans) {
            responses.add(convertToDto(plan));
        }

        return responses;
    }

    public List<SubscriptionPlanResponse> getPlansByServiceId(Long serviceId) {
        List<SubscriptionPlan> plans = planRepository.findByServiceId(serviceId);
        List<SubscriptionPlanResponse> responses = new ArrayList<>();

        for (SubscriptionPlan plan : plans) {
            responses.add(convertToDto(plan));
        }

        return responses;
    }

    public SubscriptionPlanResponse getPlanById(Long planId) {
        SubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("플랜을 찾을 수 없습니다: " + planId));
        return convertToDto(plan);
    }

    @Transactional
    public SubscriptionPlanResponse createPlan(PlanCreateRequest request) {
        ServiceEntity service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new RuntimeException("서비스를 찾을 수 없습니다: " + request.getServiceId()));

        SubscriptionPlan plan = SubscriptionPlan.builder()
                .service(service)
                .planName(request.getPlanName())
                .monthlyPrice(request.getMonthlyPrice())
                .description(request.getDescription())
                .features(request.getFeatures())
                .isPopular(request.getIsPopular() != null ? request.getIsPopular() : false)
                .build();

        SubscriptionPlan savedPlan = planRepository.save(plan);
        log.info("새 플랜 생성됨: {}", savedPlan.getId());

        return convertToDto(savedPlan);
    }

    @Transactional
    public SubscriptionPlanResponse updatePlan(Long planId, PlanUpdateRequest request) {
        SubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("플랜을 찾을 수 없습니다: " + planId));

        // 가격 변동 확인
        Integer oldPrice = plan.getMonthlyPrice();
        boolean priceChanged = !oldPrice.equals(request.getMonthlyPrice());

        plan.updateInfo(
            request.getPlanName(),
            request.getMonthlyPrice(),
            request.getDescription(),
            request.getFeatures(),
            request.getIsPopular()
        );

        log.info("플랜 업데이트됨: {}", planId);

        // 가격이 변경된 경우 알림 발송
        if (priceChanged) {
            sendPriceChangeNotifications(plan, oldPrice, request.getMonthlyPrice());
        }

        return convertToDto(plan);
    }

    private void sendPriceChangeNotifications(SubscriptionPlan plan, Integer oldPrice, Integer newPrice) {
        // 해당 서비스를 구독 중인 모든 활성 사용자 찾기
        List<UserSubscription> activeSubscriptions =
            userSubscriptionRepository.findActiveSubscriptionsByServiceId(plan.getService().getId());

        String serviceName = plan.getService().getServiceName();
        String planName = plan.getPlanName();
        int priceDiff = newPrice - oldPrice;
        String changeType = priceDiff > 0 ? "인상" : "인하";

        log.info("플랜 가격 변동 알림 발송 시작 - 서비스: {}, 플랜: {}, 변동: {}원",
                serviceName, planName, priceDiff);

        for (UserSubscription subscription : activeSubscriptions) {
            Long userId = subscription.getUser().getId();

            // 알림 설정 확인
            if (!notificationSettingService.isNotificationEnabled(userId, NotificationType.PRICE_CHANGE)) {
                continue;
            }

            String title = String.format("[%s] 요금제 가격 변동", serviceName);
            String message = String.format(
                "%s 플랜의 가격이 %s되었습니다. (기존: %,d원 → 변경: %,d원)",
                planName, changeType, oldPrice, newPrice
            );

            try {
                // 알림 생성 및 저장 (WebSocket 전송은 NotificationService에서 자동으로 처리)
                notificationService.createNotification(
                    userId,
                    NotificationType.PRICE_CHANGE,
                    title,
                    message,
                    subscription.getId()
                );
                log.info("가격 변동 알림 발송 완료 - userId: {}", userId);
            } catch (Exception e) {
                log.error("가격 변동 알림 발송 실패 - userId: {}", userId, e);
            }
        }

        log.info("플랜 가격 변동 알림 발송 완료 - 총 {}명", activeSubscriptions.size());
    }

    @Transactional
    public void deletePlan(Long planId) {
        SubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("플랜을 찾을 수 없습니다: " + planId));

        planRepository.delete(plan);
        log.info("플랜 삭제됨: {}", planId);
    }

    private SubscriptionPlanResponse convertToDto(SubscriptionPlan plan) {
        return SubscriptionPlanResponse.builder()
                .id(plan.getId())
                .planName(plan.getPlanName())
                .description(plan.getDescription())
                .monthlyPrice(plan.getMonthlyPrice())
                .features(plan.getFeatures())
                .isPopular(plan.getIsPopular())
                .createdAt(plan.getCreatedAt())
                .build();
    }
}