package com.project.subing.dto.recommendation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationItem {

    private Long serviceId;
    private String serviceName;
    private Integer score;
    private String mainReason;
    private List<String> pros;
    private List<String> cons;
    private String tip;

    // 가격 정보
    private Integer minPrice;        // 최저 월 요금 (원 단위, null이면 무료만 있음)
    private Boolean hasFreePlan;     // 무료 플랜 여부
    private String priceRange;       // "무료 ~ ₩22,000/월" 형태의 가격 범위
}
