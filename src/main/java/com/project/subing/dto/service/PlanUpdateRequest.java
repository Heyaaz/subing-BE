package com.project.subing.dto.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanUpdateRequest {
    private String planName;
    private Integer monthlyPrice;
    private String description;
    private String features;
    private Boolean isPopular;
}