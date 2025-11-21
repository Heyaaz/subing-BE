package com.project.subing.dto.user;

import com.project.subing.domain.user.entity.User;
import com.project.subing.domain.user.entity.UserTier;
import com.project.subing.domain.user.entity.UserTierUsage;
import com.project.subing.domain.user.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserTierInfoResponse {
    private Long id;
    private String name;
    private String email;
    private UserTier tier;
    private UserRole role;
    private TierLimits tierLimits;
    private UsageInfo currentUsage;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TierLimits {
        private String tierDescription;
        private int monthlyPrice;
        private int maxGptRecommendations; // -1은 무제한
        private int maxOptimizationChecks; // -1은 무제한
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UsageInfo {
        private int year;
        private int month;
        private int gptRecommendationCount;
        private int optimizationCheckCount;
        private int remainingGptRecommendations; // -1은 무제한
        private int remainingOptimizationChecks; // -1은 무제한
    }

    public static UserTierInfoResponse from(User user, UserTierUsage usage, int remainingGpt, int remainingOptimization) {
        return UserTierInfoResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .tier(user.getTier())
                .role(user.getRole())
                .tierLimits(TierLimits.builder()
                        .tierDescription(user.getTier().getDescription())
                        .monthlyPrice(user.getTier().getMonthlyPrice())
                        .maxGptRecommendations(user.getTier().getMaxGptRecommendations())
                        .maxOptimizationChecks(user.getTier().getMaxOptimizationChecks())
                        .build())
                .currentUsage(UsageInfo.builder()
                        .year(usage.getYear())
                        .month(usage.getMonth())
                        .gptRecommendationCount(usage.getGptRecommendationCount())
                        .optimizationCheckCount(usage.getOptimizationCheckCount())
                        .remainingGptRecommendations(remainingGpt)
                        .remainingOptimizationChecks(remainingOptimization)
                        .build())
                .build();
    }
}
