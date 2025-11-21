package com.project.subing.controller;

import com.project.subing.domain.recommendation.entity.RecommendationResult;
import com.project.subing.dto.common.ApiResponse;
import com.project.subing.dto.recommendation.QuizRequest;
import com.project.subing.dto.recommendation.RecommendationResponse;
import com.project.subing.service.GPTRecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final GPTRecommendationService gptRecommendationService;

    @PostMapping("/ai")
    public ResponseEntity<ApiResponse<RecommendationResponse>> getAIRecommendations(
            @RequestParam Long userId,
            @Valid @RequestBody QuizRequest request) {
        RecommendationResponse response = gptRecommendationService.getRecommendations(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "AI 추천이 완료되었습니다."));
    }

    /**
     * AI 추천 스트리밍 (실시간 타이핑 효과)
     */
    @PostMapping("/ai/stream")
    public SseEmitter streamAIRecommendations(
            @RequestParam Long userId,
            @Valid @RequestBody QuizRequest request) {
        return gptRecommendationService.getRecommendationsStream(userId, request);
    }

    @GetMapping("/history/{userId}")
    public ResponseEntity<ApiResponse<List<RecommendationResult>>> getRecommendationHistory(
            @PathVariable Long userId) {
        List<RecommendationResult> history = gptRecommendationService.getRecommendationHistory(userId);
        return ResponseEntity.ok(ApiResponse.success(history, "추천 기록을 조회했습니다."));
    }

    @PostMapping("/{recommendationId}/feedback")
    public ResponseEntity<ApiResponse<Void>> submitFeedback(
            @PathVariable Long recommendationId,
            @RequestParam Long userId,
            @RequestParam Boolean isHelpful,
            @RequestParam(required = false) String comment) {
        gptRecommendationService.saveFeedback(recommendationId, userId, isHelpful, comment);
        return ResponseEntity.ok(ApiResponse.success(null, "피드백이 저장되었습니다."));
    }

    @PostMapping("/{recommendationId}/click")
    public ResponseEntity<ApiResponse<Void>> trackClick(
            @PathVariable Long recommendationId,
            @RequestParam Long userId,
            @RequestParam Long serviceId) {
        gptRecommendationService.trackClick(recommendationId, userId, serviceId);
        return ResponseEntity.ok(ApiResponse.success(null, "클릭이 기록되었습니다."));
    }
}