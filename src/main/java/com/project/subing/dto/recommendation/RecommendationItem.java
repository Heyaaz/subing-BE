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
public class RecommendationItem {

    private Long serviceId;
    private String serviceName;
    private Integer score;
    private String mainReason;
    private List<String> pros;
    private List<String> cons;
    private String tip;
}
