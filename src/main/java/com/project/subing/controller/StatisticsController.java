package com.project.subing.controller;

import com.project.subing.dto.common.ApiResponse;
import com.project.subing.dto.statistics.ExpenseAnalysisResponse;
import com.project.subing.dto.statistics.MonthlyExpenseResponse;
import com.project.subing.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/statistics")
@RequiredArgsConstructor
public class StatisticsController {
    
    private final StatisticsService statisticsService;
    
    @GetMapping("/monthly")
    public ResponseEntity<ApiResponse<MonthlyExpenseResponse>> getMonthlyExpense(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "2025") Integer year,
            @RequestParam(defaultValue = "12") Integer month) {

        MonthlyExpenseResponse response = statisticsService.getMonthlyExpense(userId, year, month);
        return ResponseEntity.ok(ApiResponse.success(response, "월별 지출 통계를 조회했습니다."));
    }

    @GetMapping("/analysis")
    public ResponseEntity<ApiResponse<ExpenseAnalysisResponse>> getExpenseAnalysis(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {

        // 기본값 설정
        if (year == null) year = java.time.LocalDateTime.now().getYear();
        if (month == null) month = java.time.LocalDateTime.now().getMonthValue();

        ExpenseAnalysisResponse response = statisticsService.getExpenseAnalysis(userId, year, month);
        return ResponseEntity.ok(ApiResponse.success(response, "지출 분석 결과를 조회했습니다."));
    }
}
