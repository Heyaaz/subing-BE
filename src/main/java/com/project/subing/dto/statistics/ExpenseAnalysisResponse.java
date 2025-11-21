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
public class ExpenseAnalysisResponse {
    private Integer currentMonthTotal;
    private Integer previousMonthTotal;
    private Integer monthlyChange;
    private Double monthlyChangePercentage;
    private Integer yearlyTotal;
    private Integer averageMonthlyExpense;
    private List<String> topExpenseCategories;
    private List<String> recommendations;
    private LocalDateTime generatedAt;
}
