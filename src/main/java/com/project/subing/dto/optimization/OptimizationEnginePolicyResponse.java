package com.project.subing.dto.optimization;

import com.project.subing.service.OptimizationEnginePolicy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptimizationEnginePolicyResponse {

    private Integer yearlyDivisor;
    private Integer sameServiceSwitchCost;
    private Integer crossServiceBaseSwitchCost;
    private Integer yearlyBillingPenalty;
    private Integer crossCategoryPenalty;
    private Integer maxChangesPerRun;
    private Integer topKPlansPerService;
    private Integer candidateSearchTimeoutMs;
    private Integer portfolioOptimizeTimeoutMs;
    private Integer runtimeCacheTtlMs;
    private Boolean trackingEnabled;

    public static OptimizationEnginePolicyResponse from(OptimizationEnginePolicy policy) {
        return OptimizationEnginePolicyResponse.builder()
                .yearlyDivisor(policy.getYearlyDivisor())
                .sameServiceSwitchCost(policy.getSameServiceSwitchCost())
                .crossServiceBaseSwitchCost(policy.getCrossServiceBaseSwitchCost())
                .yearlyBillingPenalty(policy.getYearlyBillingPenalty())
                .crossCategoryPenalty(policy.getCrossCategoryPenalty())
                .maxChangesPerRun(policy.getMaxChangesPerRun())
                .topKPlansPerService(policy.getTopKPlansPerService())
                .candidateSearchTimeoutMs(policy.getCandidateSearchTimeoutMs())
                .portfolioOptimizeTimeoutMs(policy.getPortfolioOptimizeTimeoutMs())
                .runtimeCacheTtlMs(policy.getRuntimeCacheTtlMs())
                .trackingEnabled(policy.isTrackingEnabled())
                .build();
    }
}
