package com.project.subing.service;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OptimizationEnginePolicy {

    private final int yearlyDivisor;
    private final int sameServiceSwitchCost;
    private final int crossServiceBaseSwitchCost;
    private final int yearlyBillingPenalty;
    private final int crossCategoryPenalty;
    private final int maxChangesPerRun;
    private final int topKPlansPerService;
    private final int candidateSearchTimeoutMs;
    private final int portfolioOptimizeTimeoutMs;
    private final int runtimeCacheTtlMs;
    private final boolean trackingEnabled;
}
