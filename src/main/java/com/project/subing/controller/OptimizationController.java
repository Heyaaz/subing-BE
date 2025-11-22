package com.project.subing.controller;

import com.project.subing.dto.common.ApiResponse;
import com.project.subing.dto.optimization.CheaperAlternativeResponse;
import com.project.subing.dto.optimization.DuplicateServiceGroupResponse;
import com.project.subing.dto.optimization.OptimizationSuggestionResponse;
import com.project.subing.exception.tier.OptimizationCheckLimitException;
import com.project.subing.service.SubscriptionOptimizationService;
import com.project.subing.service.TierLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/optimization")
@RequiredArgsConstructor
public class OptimizationController {

    private final SubscriptionOptimizationService optimizationService;
    private final TierLimitService tierLimitService;

    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<OptimizationSuggestionResponse>> getOptimizationSuggestions(
            @RequestParam Long userId) {

        // 티어 제한 체크
        if (!tierLimitService.canUseOptimizationCheck(userId)) {
            throw new OptimizationCheckLimitException();
        }

        // 중복 서비스 감지
        List<SubscriptionOptimizationService.DuplicateServiceGroup> duplicates =
                optimizationService.detectDuplicateServices(userId);
        List<DuplicateServiceGroupResponse> duplicateResponses = duplicates.stream()
                .map(DuplicateServiceGroupResponse::from)
                .collect(Collectors.toList());

        // 저렴한 대안 찾기
        List<SubscriptionOptimizationService.CheaperAlternative> alternatives =
                optimizationService.findCheaperAlternatives(userId);
        List<CheaperAlternativeResponse> alternativeResponses = alternatives.stream()
                .map(CheaperAlternativeResponse::from)
                .collect(Collectors.toList());

        // 총 절약 가능 금액 계산
        int totalPotentialSavings = alternativeResponses.stream()
                .mapToInt(CheaperAlternativeResponse::getSavings)
                .sum();

        // 요약 메시지 생성
        String summary = generateSummary(duplicateResponses.size(), alternativeResponses.size(), totalPotentialSavings);

        OptimizationSuggestionResponse response = OptimizationSuggestionResponse.builder()
                .duplicateServices(duplicateResponses)
                .cheaperAlternatives(alternativeResponses)
                .totalPotentialSavings(totalPotentialSavings)
                .summary(summary)
                .build();

        // 사용량 증가
        tierLimitService.incrementOptimizationCheck(userId);

        return ResponseEntity.ok(ApiResponse.success(response, "최적화 제안을 생성했습니다."));
    }

    @GetMapping("/duplicates")
    public ResponseEntity<ApiResponse<List<DuplicateServiceGroupResponse>>> getDuplicateServices(
            @RequestParam Long userId) {

        List<SubscriptionOptimizationService.DuplicateServiceGroup> duplicates =
                optimizationService.detectDuplicateServices(userId);
        List<DuplicateServiceGroupResponse> responses = duplicates.stream()
                .map(DuplicateServiceGroupResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses, "중복 서비스를 조회했습니다."));
    }

    @GetMapping("/alternatives")
    public ResponseEntity<ApiResponse<List<CheaperAlternativeResponse>>> getCheaperAlternatives(
            @RequestParam Long userId) {

        List<SubscriptionOptimizationService.CheaperAlternative> alternatives =
                optimizationService.findCheaperAlternatives(userId);
        List<CheaperAlternativeResponse> responses = alternatives.stream()
                .map(CheaperAlternativeResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses, "저렴한 대안을 조회했습니다."));
    }

    private String generateSummary(int duplicateCount, int alternativeCount, int totalSavings) {
        if (duplicateCount == 0 && alternativeCount == 0) {
            return "현재 구독이 최적화되어 있습니다!";
        }

        StringBuilder summary = new StringBuilder();

        if (duplicateCount > 0) {
            summary.append(String.format("%d개의 중복 카테고리가 발견되었습니다. ", duplicateCount));
        }

        if (alternativeCount > 0) {
            summary.append(String.format("%d개의 저렴한 대안이 있으며, 월 최대 %,d원을 절약할 수 있습니다.",
                    alternativeCount, totalSavings));
        }

        return summary.toString();
    }
}
