package com.project.subing.dto.recommendation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationResponse {

    private List<RecommendationItem> recommendations;
    private String summary;
    private String alternatives;
}