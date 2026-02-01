package com.project.subing.controller;

import com.project.subing.domain.budget.entity.Budget;
import com.project.subing.dto.budget.BudgetRequest;
import com.project.subing.dto.budget.BudgetResponse;
import com.project.subing.dto.common.ApiResponse;
import com.project.subing.service.BudgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @PostMapping
    public ResponseEntity<ApiResponse<BudgetResponse>> setBudget(
            @AuthenticationPrincipal Long userId,
            @RequestBody BudgetRequest request) {
        Budget budget = budgetService.setBudget(
                userId,
                request.getYear(),
                request.getMonth(),
                request.getMonthlyLimit()
        );
        BudgetResponse response = BudgetResponse.from(budget);
        return ResponseEntity.ok(ApiResponse.success(response, "예산이 설정되었습니다."));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BudgetResponse>>> getAllBudgets(
            @AuthenticationPrincipal Long userId) {
        List<Budget> budgets = budgetService.getAllBudgets(userId);
        List<BudgetResponse> responses = budgets.stream()
                .map(BudgetResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses, "예산 목록을 조회했습니다."));
    }

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<BudgetResponse>> getCurrentMonthBudget(
            @AuthenticationPrincipal Long userId) {
        Optional<Budget> budget = budgetService.getCurrentMonthBudget(userId);
        if (budget.isPresent()) {
            BudgetResponse response = BudgetResponse.from(budget.get());
            return ResponseEntity.ok(ApiResponse.success(response, "현재 월 예산을 조회했습니다."));
        } else {
            return ResponseEntity.ok(ApiResponse.success(null, "설정된 예산이 없습니다."));
        }
    }

    @GetMapping("/{year}/{month}")
    public ResponseEntity<ApiResponse<BudgetResponse>> getBudget(
            @AuthenticationPrincipal Long userId,
            @PathVariable Integer year,
            @PathVariable Integer month) {
        Optional<Budget> budget = budgetService.getBudget(userId, year, month);
        if (budget.isPresent()) {
            BudgetResponse response = BudgetResponse.from(budget.get());
            return ResponseEntity.ok(ApiResponse.success(response, "예산을 조회했습니다."));
        } else {
            return ResponseEntity.ok(ApiResponse.success(null, "설정된 예산이 없습니다."));
        }
    }

    @DeleteMapping("/{budgetId}")
    public ResponseEntity<ApiResponse<Void>> deleteBudget(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long budgetId) {
        budgetService.deleteBudget(budgetId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "예산이 삭제되었습니다."));
    }
}