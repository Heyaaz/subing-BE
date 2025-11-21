package com.project.subing.dto.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlanResponse {
    private Long id;
    private String planName;
    private String description;
    private Integer monthlyPrice;
    private String features;
    private Boolean isPopular;
    private LocalDateTime createdAt;
}
