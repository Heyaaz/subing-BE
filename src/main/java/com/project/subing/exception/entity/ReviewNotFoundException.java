package com.project.subing.exception.entity;

import com.project.subing.exception.ErrorCode;

/**
 * 리뷰를 찾을 수 없을 때 발생하는 예외
 *
 * HTTP Status: 404 Not Found
 */
public class ReviewNotFoundException extends EntityNotFoundException {

    public ReviewNotFoundException(Long reviewId) {
        super(ErrorCode.REVIEW_NOT_FOUND, "리뷰를 찾을 수 없습니다: ID " + reviewId);
    }
}