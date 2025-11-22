package com.project.subing.exception.business;

import com.project.subing.exception.ErrorCode;

/**
 * 평점이 유효 범위를 벗어났을 때 발생하는 예외
 *
 * HTTP Status: 422 Unprocessable Entity
 */
public class InvalidRatingException extends BusinessRuleViolationException {

    public InvalidRatingException(int rating) {
        super(ErrorCode.INVALID_RATING, "평점은 1~5 사이의 값이어야 합니다. 입력된 값: " + rating);
    }

    public InvalidRatingException() {
        super(ErrorCode.INVALID_RATING);
    }
}