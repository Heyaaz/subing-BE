package com.project.subing.exception.entity;

import com.project.subing.exception.ErrorCode;

/**
 * 예산을 찾을 수 없을 때 발생하는 예외
 *
 * HTTP Status: 404 Not Found
 */
public class BudgetNotFoundException extends EntityNotFoundException {

    public BudgetNotFoundException(Long budgetId) {
        super(ErrorCode.BUDGET_NOT_FOUND, "예산을 찾을 수 없습니다: ID " + budgetId);
    }
}