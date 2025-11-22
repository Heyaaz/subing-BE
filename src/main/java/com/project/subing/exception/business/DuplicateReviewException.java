package com.project.subing.exception.business;

import com.project.subing.exception.ErrorCode;

/**
 * 동일 서비스에 중복 리뷰 작성 시 발생하는 예외
 *
 * HTTP Status: 422 Unprocessable Entity
 */
public class DuplicateReviewException extends BusinessRuleViolationException {

    public DuplicateReviewException(Long userId, Long serviceId) {
        super(ErrorCode.DUPLICATE_REVIEW,
              "이미 해당 서비스에 리뷰를 작성하셨습니다. (User: " + userId + ", Service: " + serviceId + ")");
    }

    public DuplicateReviewException() {
        super(ErrorCode.DUPLICATE_REVIEW);
    }
}