package com.project.subing.controller;

import com.project.subing.dto.review.ReviewCreateRequest;
import com.project.subing.dto.review.ReviewResponse;
import com.project.subing.dto.review.ReviewUpdateRequest;
import com.project.subing.dto.review.ServiceRatingResponse;
import com.project.subing.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            Authentication authentication,
            @RequestBody ReviewCreateRequest request) {
        Long userId = Long.parseLong(authentication.getName());
        ReviewResponse review = reviewService.createReview(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(review);
    }

    @GetMapping("/service/{serviceId}")
    public ResponseEntity<List<ReviewResponse>> getReviewsByService(@PathVariable Long serviceId) {
        List<ReviewResponse> reviews = reviewService.getReviewsByServiceId(serviceId);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/my")
    public ResponseEntity<List<ReviewResponse>> getMyReviews(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        List<ReviewResponse> reviews = reviewService.getMyReviews(userId);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> getReview(@PathVariable Long reviewId) {
        ReviewResponse review = reviewService.getReviewById(reviewId);
        return ResponseEntity.ok(review);
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> updateReview(
            Authentication authentication,
            @PathVariable Long reviewId,
            @RequestBody ReviewUpdateRequest request) {
        Long userId = Long.parseLong(authentication.getName());
        ReviewResponse review = reviewService.updateReview(userId, reviewId, request);
        return ResponseEntity.ok(review);
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            Authentication authentication,
            @PathVariable Long reviewId) {
        Long userId = Long.parseLong(authentication.getName());
        reviewService.deleteReview(userId, reviewId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/service/{serviceId}/rating")
    public ResponseEntity<ServiceRatingResponse> getServiceRating(@PathVariable Long serviceId) {
        ServiceRatingResponse rating = reviewService.getServiceRating(serviceId);
        return ResponseEntity.ok(rating);
    }

    @GetMapping("/service/{serviceId}/check")
    public ResponseEntity<Boolean> checkUserReviewed(
            Authentication authentication,
            @PathVariable Long serviceId) {
        Long userId = Long.parseLong(authentication.getName());
        boolean hasReviewed = reviewService.hasUserReviewed(userId, serviceId);
        return ResponseEntity.ok(hasReviewed);
    }
}