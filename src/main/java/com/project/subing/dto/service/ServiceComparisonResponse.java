package com.project.subing.dto.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceComparisonResponse {
    private List<ServiceResponse> services;
    private ComparisonSummary summary;
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComparisonSummary {
        private Integer minPrice;
        private Integer maxPrice;
        private Integer avgPrice;
        private String mostPopularService;
        private String bestValueService;
    }
}
