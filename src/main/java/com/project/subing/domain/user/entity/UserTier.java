package com.project.subing.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserTier {
    FREE("무료", 0, -1, 10, 3),
    PRO("프리미엄", 9900, -1, -1, -1);

    private final String description;
    private final int monthlyPrice;
    private final int maxSubscriptions; // -1은 무제한
    private final int maxGptRecommendations; // 월 단위, -1은 무제한
    private final int maxOptimizationChecks; // 월 단위, -1은 무제한

    public boolean isUnlimited(String feature) {
        return switch (feature.toLowerCase()) {
            case "subscriptions" -> maxSubscriptions == -1;
            case "gpt" -> maxGptRecommendations == -1;
            case "optimization" -> maxOptimizationChecks == -1;
            default -> false;
        };
    }

    public int getLimit(String feature) {
        return switch (feature.toLowerCase()) {
            case "subscriptions" -> maxSubscriptions;
            case "gpt" -> maxGptRecommendations;
            case "optimization" -> maxOptimizationChecks;
            default -> 0;
        };
    }
}