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
public class MonthlyExpenseResponse {
    private Integer year;
    private Integer month;
    private Integer totalAmount;
    private Integer activeSubscriptions;
    private List<CategoryExpenseResponse> categoryExpenses;
    private LocalDateTime generatedAt;
}
