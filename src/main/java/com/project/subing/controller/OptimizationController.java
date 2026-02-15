package com.project.subing.controller;

import com.project.subing.dto.common.ApiResponse;
import com.project.subing.dto.optimization.CheaperAlternativeResponse;
import com.project.subing.dto.optimization.DuplicateServiceGroupResponse;
import com.project.subing.dto.optimization.OptimizationEventRequest;
import com.project.subing.dto.optimization.OptimizationSuggestionResponse;
import jakarta.validation.Valid;
import com.project.subing.service.SubscriptionOptimizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/optimization")
@RequiredArgsConstructor
public class OptimizationController {

    private final SubscriptionOptimizationService optimizationService;

    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<OptimizationSuggestionResponse>> getOptimizationSuggestions(
            @AuthenticationPrincipal Long userId) {

        List<SubscriptionOptimizationService.DuplicateServiceGroup> duplicates =
                optimizationService.detectDuplicateServices(userId);
        List<DuplicateServiceGroupResponse> duplicateResponses = duplicates.stream()
                .map(DuplicateServiceGroupResponse::from)
                .collect(Collectors.toList());

        List<SubscriptionOptimizationService.CheaperAlternative> alternatives =
                optimizationService.findCheaperAlternatives(userId);
        List<CheaperAlternativeResponse> alternativeResponses = alternatives.stream()
                .map(CheaperAlternativeResponse::from)
                .collect(Collectors.toList());

        List<SubscriptionOptimizationService.CheaperAlternative> optimizedAlternatives =
                optimizationService.selectPortfolioOptimizedAlternatives(alternatives);
        List<CheaperAlternativeResponse> optimizedAlternativeResponses = optimizedAlternatives.stream()
                .map(CheaperAlternativeResponse::from)
                .collect(Collectors.toList());

        // 전역 최적화 결과 기준 총 잠재 절약 금액
        int totalPotentialSavings = optimizedAlternativeResponses.stream()
                .collect(Collectors.groupingBy(
                        alt -> alt.getCurrentSubscription().getId(),
                        Collectors.maxBy(Comparator.comparingInt(CheaperAlternativeResponse::getNetSavings))
                ))
                .values().stream()
                .filter(Optional::isPresent)
                .mapToInt(opt -> opt.get().getNetSavings())
                .sum();

        String summary = generateSummary(duplicateResponses.size(), optimizedAlternativeResponses.size(), totalPotentialSavings);

        OptimizationSuggestionResponse response = OptimizationSuggestionResponse.builder()
                .duplicateServices(duplicateResponses)
                .cheaperAlternatives(alternativeResponses)
                .optimizedAlternatives(optimizedAlternativeResponses)
                .totalPotentialSavings(totalPotentialSavings)
                .summary(summary)
                .build();

        return ResponseEntity.ok(ApiResponse.success(response, "최적화 제안을 생성했습니다."));
    }

    @GetMapping("/duplicates")
    public ResponseEntity<ApiResponse<List<DuplicateServiceGroupResponse>>> getDuplicateServices(
            @AuthenticationPrincipal Long userId) {

        List<SubscriptionOptimizationService.DuplicateServiceGroup> duplicates =
                optimizationService.detectDuplicateServices(userId);
        List<DuplicateServiceGroupResponse> responses = duplicates.stream()
                .map(DuplicateServiceGroupResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses, "중복 서비스를 조회했습니다."));
    }

    @GetMapping("/alternatives")
    public ResponseEntity<ApiResponse<List<CheaperAlternativeResponse>>> getCheaperAlternatives(
            @AuthenticationPrincipal Long userId) {

        List<SubscriptionOptimizationService.CheaperAlternative> alternatives =
                optimizationService.findCheaperAlternatives(userId);
        List<CheaperAlternativeResponse> responses = alternatives.stream()
                .map(CheaperAlternativeResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses, "저렴한 대안을 조회했습니다."));
    }

    @PostMapping("/events")
    public ResponseEntity<ApiResponse<Void>> trackOptimizationEvent(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody OptimizationEventRequest request) {
        optimizationService.trackOptimizationEvent(userId, request);
        return ResponseEntity.ok(ApiResponse.success(null, "최적화 이벤트가 기록되었습니다."));
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
            summary.append(String.format("%d개의 저렴한 대안이 있으며, 최적 선택 시 월 최대 순절감 %,d원이 예상됩니다.",
                    alternativeCount, totalSavings));
        }

        return summary.toString();
    }
}
