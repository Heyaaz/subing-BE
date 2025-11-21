package com.project.subing.dto.optimization;

import com.project.subing.domain.service.entity.SubscriptionPlan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlternativePlanInfo {

    private Long planId;
    private String planName;
    private Integer monthlyPrice;
    private String description;
    private String features;

    public static AlternativePlanInfo from(SubscriptionPlan plan) {
        return AlternativePlanInfo.builder()
                .planId(plan.getId())
                .planName(plan.getPlanName())
                .monthlyPrice(plan.getMonthlyPrice())
                .description(plan.getDescription())
                .features(plan.getFeatures())
                .build();
    }
}