package com.project.subing.dto.optimization;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptimizationSuggestionResponse {

    private List<DuplicateServiceGroupResponse> duplicateServices;
    private List<CheaperAlternativeResponse> cheaperAlternatives;
    private List<CheaperAlternativeResponse> optimizedAlternatives;
    private Integer totalPotentialSavings;
    private String summary;
}
