package com.project.subing.dto.budget;

import com.project.subing.domain.budget.entity.Budget;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetResponse {

    private Long id;
    private Integer year;
    private Integer month;
    private Long monthlyLimit;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BudgetResponse from(Budget budget) {
        return BudgetResponse.builder()
                .id(budget.getId())
                .year(budget.getYear())
                .month(budget.getMonth())
                .monthlyLimit(budget.getMonthlyLimit())
                .createdAt(budget.getCreatedAt())
                .updatedAt(budget.getUpdatedAt())
                .build();
    }
}