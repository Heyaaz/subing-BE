package com.project.subing.dto.statistics;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryExpenseResponse {
    private String category;
    private Integer amount;
    private Integer subscriptionCount;
    private Double percentage;
    private List<String> serviceNames;
}
