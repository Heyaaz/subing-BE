package com.project.subing.dto.optimization;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptimizationEngineConfigUpdateRequest {

    @NotEmpty(message = "overrides는 최소 1개 이상 필요합니다.")
    private Map<String, String> overrides;
}
