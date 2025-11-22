package com.project.subing.service;

import com.project.subing.domain.review.entity.ServiceReview;
import com.project.subing.domain.service.entity.ServiceEntity;
import com.project.subing.domain.user.entity.User;
import com.project.subing.dto.review.ReviewCreateRequest;
import com.project.subing.dto.review.ReviewResponse;
import com.project.subing.dto.review.ReviewUpdateRequest;
import com.project.subing.dto.review.ServiceRatingResponse;
import com.project.subing.exception.auth.ReviewOwnershipException;
import com.project.subing.exception.business.DuplicateReviewException;
import com.project.subing.exception.business.InvalidRatingException;
import com.project.subing.exception.entity.ReviewNotFoundException;
import com.project.subing.exception.entity.ServiceNotFoundException;
import com.project.subing.exception.entity.UserNotFoundException;
import com.project.subing.repository.ServiceRepository;
import com.project.subing.repository.ServiceReviewRepository;
import com.project.subing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ServiceReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ServiceRepository serviceRepository;

    @Transactional
    public ReviewResponse createReview(Long userId, ReviewCreateRequest request) {
        // 이미 리뷰를 작성했는지 확인
        if (reviewRepository.existsByUserIdAndServiceId(userId, request.getServiceId())) {
            throw new DuplicateReviewException(userId, request.getServiceId());
        }

        // 평점 유효성 검사
        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new InvalidRatingException(request.getRating());
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        ServiceEntity service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new ServiceNotFoundException(request.getServiceId()));

        ServiceReview review = ServiceReview.builder()
                .user(user)
                .service(service)
                .rating(request.getRating())
                .content(request.getContent())
                .build();

        ServiceReview savedReview = reviewRepository.save(review);
        log.info("리뷰 작성됨: userId={}, serviceId={}, rating={}", userId, request.getServiceId(), request.getRating());

        return ReviewResponse.from(savedReview);
    }

    public List<ReviewResponse> getReviewsByServiceId(Long serviceId) {
        List<ServiceReview> reviews = reviewRepository.findByServiceIdOrderByCreatedAtDesc(serviceId);
        return reviews.stream()
                .map(ReviewResponse::from)
                .collect(Collectors.toList());
    }

    public List<ReviewResponse> getMyReviews(Long userId) {
        List<ServiceReview> reviews = reviewRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return reviews.stream()
                .map(ReviewResponse::from)
                .collect(Collectors.toList());
    }

    public ReviewResponse getReviewById(Long reviewId) {
        ServiceReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));
        return ReviewResponse.from(review);
    }

    @Transactional
    public ReviewResponse updateReview(Long userId, Long reviewId, ReviewUpdateRequest request) {
        ServiceReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));

        // 본인 리뷰인지 확인
        if (!review.isOwnedBy(userId)) {
            throw new ReviewOwnershipException(reviewId, userId);
        }

        // 평점 유효성 검사
        if (request.getRating() != null && (request.getRating() < 1 || request.getRating() > 5)) {
            throw new InvalidRatingException(request.getRating());
        }

        review.updateReview(request.getRating(), request.getContent());
        log.info("리뷰 수정됨: reviewId={}, userId={}", reviewId, userId);

        return ReviewResponse.from(review);
    }

    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        ServiceReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));

        // 본인 리뷰인지 확인
        if (!review.isOwnedBy(userId)) {
            throw new ReviewOwnershipException(reviewId, userId);
        }

        reviewRepository.delete(review);
        log.info("리뷰 삭제됨: reviewId={}, userId={}", reviewId, userId);
    }

    public ServiceRatingResponse getServiceRating(Long serviceId) {
        Double averageRating = reviewRepository.findAverageRatingByServiceId(serviceId);
        Long reviewCount = reviewRepository.countByServiceId(serviceId);

        return ServiceRatingResponse.builder()
                .serviceId(serviceId)
                .averageRating(averageRating != null ? Math.round(averageRating * 10.0) / 10.0 : 0.0)
                .reviewCount(reviewCount)
                .build();
    }

    public boolean hasUserReviewed(Long userId, Long serviceId) {
        return reviewRepository.existsByUserIdAndServiceId(userId, serviceId);
    }
}