package com.project.subing.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "optimization.engine")
public class OptimizationEngineProperties {

    private Pricing pricing = new Pricing();
    private Portfolio portfolio = new Portfolio();
    private Performance performance = new Performance();
    private Tracking tracking = new Tracking();

    @Getter
    @Setter
    public static class Pricing {
        private int yearlyDivisor = 12;
        private int sameServiceSwitchCost = 0;
        private int crossServiceBaseSwitchCost = 2000;
        private int yearlyBillingPenalty = 2000;
        private int crossCategoryPenalty = 1000;
    }

    @Getter
    @Setter
    public static class Portfolio {
        private int maxChangesPerRun = 3;
    }

    @Getter
    @Setter
    public static class Performance {
        private int topKPlansPerService = 5;
        private int candidateSearchTimeoutMs = 100;
        private int portfolioOptimizeTimeoutMs = 30;
        private int runtimeCacheTtlMs = 30000;
    }

    @Getter
    @Setter
    public static class Tracking {
        private boolean enabled = true;
    }
}
