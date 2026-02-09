package com.project.subing.dto.subscription;

import com.project.subing.domain.common.BillingCycle;
import com.project.subing.domain.common.Currency;
import com.project.subing.domain.subscription.entity.UserSubscription;
import com.project.subing.dto.service.ServiceResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse {

    private Long id;
    private Long serviceId;
    private ServiceResponse service;
    private String serviceName;
    private String serviceCategory;
    private String serviceIcon;
    private String planName;
    private Integer monthlyPrice;
    private Currency currency;
    private Integer billingDate;
    private LocalDate nextBillingDate;
    private BillingCycle billingCycle;
    private Boolean isActive;
    private String notes;
    private LocalDate startedAt;
    private LocalDate endedAt;
    private LocalDateTime createdAt;

    public static SubscriptionResponse from(UserSubscription subscription) {
        return SubscriptionResponse.builder()
                .id(subscription.getId())
                .serviceId(subscription.getService() != null ? subscription.getService().getId() : null)
                .service(ServiceResponse.from(subscription.getService()))
                .serviceName(subscription.getService().getServiceName())
                .serviceCategory(subscription.getService().getCategory().getDescription())
                .serviceIcon(subscription.getService().getIconUrl())
                .planName(subscription.getPlanName())
                .monthlyPrice(subscription.getMonthlyPrice())
                .currency(subscription.getCurrency() != null ? subscription.getCurrency() : Currency.KRW)
                .billingDate(subscription.getBillingDate())
                .nextBillingDate(subscription.getNextBillingDate())
                .billingCycle(subscription.getBillingCycle())
                .isActive(subscription.getIsActive())
                .notes(subscription.getNotes())
                .startedAt(subscription.getStartedAt())
                .endedAt(subscription.getEndedAt())
                .createdAt(subscription.getCreatedAt())
                .build();
    }
}
