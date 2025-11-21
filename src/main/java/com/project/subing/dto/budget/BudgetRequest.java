package com.project.subing.dto.budget;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BudgetRequest {

    private Integer year;
    private Integer month;
    private Long monthlyLimit;
}