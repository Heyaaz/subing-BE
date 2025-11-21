package com.project.subing.dto.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRatingResponse {
    private Long serviceId;
    private Double averageRating;
    private Long reviewCount;
}