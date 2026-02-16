package com.project.subing.dto.recommendation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecommendationResponse {

    private Long recommendationId;  // DB 저장 후 할당되는 ID (피드백 API 연동용)
    private List<RecommendationItem> recommendations;
    private String summary;
    private String alternatives;

    /**
     * recommendationId를 설정하는 setter (DB 저장 후 ID 할당용)
     */
    public void setRecommendationId(Long recommendationId) {
        this.recommendationId = recommendationId;
    }
}