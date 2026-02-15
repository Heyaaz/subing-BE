package com.project.subing.dto.optimization;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptimizationEngineConfigResponse {

    private OptimizationEnginePolicyResponse defaultPolicy;
    private OptimizationEnginePolicyResponse effectivePolicy;
    private Map<String, String> activeOverrides;
    private Long cacheLoadedAtEpochMs;
    private List<OptimizationEngineConfigAuditResponse> recentAudits;
}
