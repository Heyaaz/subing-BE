package com.project.subing.exception.entity;

import com.project.subing.exception.ErrorCode;

/**
 * 추천 결과를 찾을 수 없을 때 발생하는 예외
 *
 * HTTP Status: 404 Not Found
 */
public class RecommendationNotFoundException extends EntityNotFoundException {

    public RecommendationNotFoundException(Long recommendationId) {
        super(ErrorCode.RECOMMENDATION_NOT_FOUND, "추천 결과를 찾을 수 없습니다: ID " + recommendationId);
    }
}