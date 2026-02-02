package com.project.subing.service;

import com.project.subing.domain.subscription.entity.UserSubscription;
import com.project.subing.domain.service.entity.ServiceEntity;
import com.project.subing.domain.user.entity.User;
import com.project.subing.dto.subscription.SubscriptionRequest;
import com.project.subing.dto.subscription.SubscriptionResponse;
import com.project.subing.exception.auth.UnauthorizedAccessException;
import com.project.subing.exception.entity.ServiceNotFoundException;
import com.project.subing.exception.entity.SubscriptionNotFoundException;
import com.project.subing.exception.entity.UserNotFoundException;
import com.project.subing.repository.ServiceRepository;
import com.project.subing.repository.UserRepository;
import com.project.subing.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SubscriptionService {
    
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final UserRepository userRepository;
    private final ServiceRepository serviceRepository;
    
    public SubscriptionResponse createSubscription(Long userId, SubscriptionRequest request) {
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 서비스 조회
        ServiceEntity service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new ServiceNotFoundException(request.getServiceId()));
        
        // 구독 생성
        com.project.subing.domain.common.Currency currency = request.getCurrency() != null
                ? request.getCurrency() : com.project.subing.domain.common.Currency.KRW;
        LocalDate startedAt = parseStartedAt(request.getStartedAt());
        UserSubscription subscription = UserSubscription.builder()
                .user(user)
                .service(service)
                .planName(request.getPlanName())
                .monthlyPrice(request.getMonthlyPrice())
                .currency(currency)
                .billingDate(request.getBillingDate())
                .billingCycle(request.getBillingCycle())
                .notes(request.getNotes())
                .startedAt(startedAt)
                .build();
        
        UserSubscription savedSubscription = userSubscriptionRepository.save(subscription);
        
        return SubscriptionResponse.builder()
                .id(savedSubscription.getId())
                .serviceId(savedSubscription.getService() != null ? savedSubscription.getService().getId() : null)
                .serviceName(savedSubscription.getService() != null ? savedSubscription.getService().getServiceName() : "서비스 없음")
                .serviceCategory(savedSubscription.getService() != null ? savedSubscription.getService().getCategory().toString() : "카테고리 없음")
                .serviceIcon(savedSubscription.getService() != null ? savedSubscription.getService().getIconUrl() : "")
                .planName(savedSubscription.getPlanName())
                .monthlyPrice(savedSubscription.getMonthlyPrice())
                .currency(savedSubscription.getCurrency())
                .billingDate(savedSubscription.getBillingDate())
                .nextBillingDate(savedSubscription.getNextBillingDate())
                .billingCycle(savedSubscription.getBillingCycle())
                .isActive(savedSubscription.getIsActive())
                .notes(savedSubscription.getNotes())
                .startedAt(savedSubscription.getStartedAt())
                .createdAt(savedSubscription.getCreatedAt())
                .build();
    }
    
    @Transactional(readOnly = true)
    public List<SubscriptionResponse> getUserSubscriptions(Long userId) {
        List<UserSubscription> subscriptions = userSubscriptionRepository.findByUserId(userId);

        return subscriptions.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SubscriptionResponse> getUserSubscriptionsWithFilters(
            Long userId,
            String category,
            Boolean isActive,
            String sort) {

        List<UserSubscription> subscriptions;

        // 필터링 로직
        if (category != null && !category.isEmpty() && isActive != null) {
            // 카테고리 + 활성 상태 필터
            subscriptions = userSubscriptionRepository.findByUserIdAndServiceCategoryAndIsActive(
                    userId,
                    com.project.subing.domain.common.ServiceCategory.valueOf(category.toUpperCase()),
                    isActive
            );
        } else if (category != null && !category.isEmpty()) {
            // 카테고리만 필터
            subscriptions = userSubscriptionRepository.findByUserIdAndServiceCategory(
                    userId,
                    com.project.subing.domain.common.ServiceCategory.valueOf(category.toUpperCase())
            );
        } else if (isActive != null) {
            // 활성 상태만 필터
            subscriptions = userSubscriptionRepository.findByUserIdAndIsActive(userId, isActive);
        } else {
            // 필터 없음
            subscriptions = userSubscriptionRepository.findByUserId(userId);
        }

        // 정렬 로직
        if (sort != null && !sort.isEmpty()) {
            switch (sort.toLowerCase()) {
                case "price_asc":
                    subscriptions.sort((s1, s2) -> s1.getMonthlyPrice().compareTo(s2.getMonthlyPrice()));
                    break;
                case "price_desc":
                    subscriptions.sort((s1, s2) -> s2.getMonthlyPrice().compareTo(s1.getMonthlyPrice()));
                    break;
                case "date_asc":
                    subscriptions.sort((s1, s2) -> s1.getCreatedAt().compareTo(s2.getCreatedAt()));
                    break;
                case "date_desc":
                    subscriptions.sort((s1, s2) -> s2.getCreatedAt().compareTo(s1.getCreatedAt()));
                    break;
                case "name_asc":
                    subscriptions.sort((s1, s2) -> s1.getService().getServiceName().compareTo(s2.getService().getServiceName()));
                    break;
                case "name_desc":
                    subscriptions.sort((s1, s2) -> s2.getService().getServiceName().compareTo(s1.getService().getServiceName()));
                    break;
                default:
                    // 기본 정렬: 생성일 내림차순
                    subscriptions.sort((s1, s2) -> s2.getCreatedAt().compareTo(s1.getCreatedAt()));
            }
        }

        return subscriptions.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    public SubscriptionResponse updateSubscription(Long id, Long principalUserId, SubscriptionRequest request) {
        UserSubscription subscription = userSubscriptionRepository.findById(id)
                .orElseThrow(() -> new SubscriptionNotFoundException(id));

        if (!subscription.getUser().getId().equals(principalUserId)) {
            throw new UnauthorizedAccessException("구독 수정 권한이 없습니다.");
        }

        subscription.updatePrice(request.getMonthlyPrice());
        subscription.setPlanName(request.getPlanName());
        if (request.getCurrency() != null) {
            subscription.setCurrency(request.getCurrency());
        }
        subscription.setBillingDate(request.getBillingDate());
        subscription.setBillingCycle(request.getBillingCycle());
        subscription.setNotes(request.getNotes());
        if (request.getStartedAt() != null) {
            subscription.setStartedAt(parseStartedAt(request.getStartedAt()));
        }

        UserSubscription savedSubscription = userSubscriptionRepository.save(subscription);

        return convertToResponse(savedSubscription);
    }

    public void deleteSubscription(Long id, Long principalUserId) {
        UserSubscription subscription = userSubscriptionRepository.findById(id)
                .orElseThrow(() -> new SubscriptionNotFoundException(id));

        if (!subscription.getUser().getId().equals(principalUserId)) {
            throw new UnauthorizedAccessException("구독 삭제 권한이 없습니다.");
        }

        userSubscriptionRepository.delete(subscription);
    }

    public SubscriptionResponse toggleSubscriptionStatus(Long id, Long principalUserId, Boolean isActive) {
        UserSubscription subscription = userSubscriptionRepository.findById(id)
                .orElseThrow(() -> new SubscriptionNotFoundException(id));

        if (!subscription.getUser().getId().equals(principalUserId)) {
            throw new UnauthorizedAccessException("구독 상태 변경 권한이 없습니다.");
        }

        if (isActive) {
            subscription.reactivate();
        } else {
            subscription.cancel();
        }

        UserSubscription savedSubscription = userSubscriptionRepository.save(subscription);

        return convertToResponse(savedSubscription);
    }
    
    private SubscriptionResponse convertToResponse(UserSubscription subscription) {
        return SubscriptionResponse.builder()
                .id(subscription.getId())
                .serviceId(subscription.getService() != null ? subscription.getService().getId() : null)
                .serviceName(subscription.getService() != null ? subscription.getService().getServiceName() : "서비스 없음")
                .serviceCategory(subscription.getService() != null ? subscription.getService().getCategory().toString() : "카테고리 없음")
                .serviceIcon(subscription.getService() != null ? subscription.getService().getIconUrl() : "")
                .planName(subscription.getPlanName())
                .monthlyPrice(subscription.getMonthlyPrice())
                .currency(subscription.getCurrency() != null ? subscription.getCurrency() : com.project.subing.domain.common.Currency.KRW)
                .billingDate(subscription.getBillingDate())
                .nextBillingDate(subscription.getNextBillingDate())
                .billingCycle(subscription.getBillingCycle())
                .isActive(subscription.getIsActive())
                .notes(subscription.getNotes())
                .startedAt(subscription.getStartedAt())
                .createdAt(subscription.getCreatedAt())
                .build();
    }

    private static LocalDate parseStartedAt(String startedAt) {
        if (startedAt == null || startedAt.isBlank()) {
            return null;
        }
        try {
            return YearMonth.parse(startedAt).atDay(1);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
