package com.project.subing.dto.optimization;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptimizationEventRequest {

    @NotBlank(message = "이벤트 타입은 필수입니다.")
    private String eventType;

    private Long currentSubscriptionId;
    private Long alternativeServiceId;
    private String suggestionType;

    @Builder.Default
    private String source = "OPTIMIZATION_PAGE";

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}
