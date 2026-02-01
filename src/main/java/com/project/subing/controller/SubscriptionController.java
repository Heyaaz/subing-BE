package com.project.subing.controller;

import com.project.subing.dto.common.ApiResponse;
import com.project.subing.dto.subscription.StatusUpdateRequest;
import com.project.subing.dto.subscription.SubscriptionRequest;
import com.project.subing.dto.subscription.SubscriptionResponse;
import com.project.subing.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {
    
    private final SubscriptionService subscriptionService;
    
    @PostMapping
    public ResponseEntity<ApiResponse<SubscriptionResponse>> createSubscription(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody SubscriptionRequest request) {
        SubscriptionResponse response = subscriptionService.createSubscription(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "구독이 추가되었습니다."));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SubscriptionResponse>>> getUserSubscriptions(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String sort) {

        List<SubscriptionResponse> response;

        if (category != null || isActive != null || sort != null) {
            response = subscriptionService.getUserSubscriptionsWithFilters(userId, category, isActive, sort);
        } else {
            response = subscriptionService.getUserSubscriptions(userId);
        }

        return ResponseEntity.ok(ApiResponse.success(response, "구독 목록을 조회했습니다."));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> updateSubscription(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @Valid @RequestBody SubscriptionRequest request) {
        SubscriptionResponse response = subscriptionService.updateSubscription(id, userId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "구독이 수정되었습니다."));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSubscription(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        subscriptionService.deleteSubscription(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "구독이 삭제되었습니다."));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> toggleSubscriptionStatus(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @RequestBody StatusUpdateRequest request) {
        SubscriptionResponse response = subscriptionService.toggleSubscriptionStatus(id, userId, request.getIsActive());
        return ResponseEntity.ok(ApiResponse.success(response, "구독 상태가 변경되었습니다."));
    }
}
