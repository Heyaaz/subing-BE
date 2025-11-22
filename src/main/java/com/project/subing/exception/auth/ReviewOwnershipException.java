package com.project.subing.exception.auth;

import com.project.subing.exception.ErrorCode;

/**
 * 리뷰 소유권 위반 시 발생하는 예외
 *
 * HTTP Status: 403 Forbidden
 *
 * 본인이 작성한 리뷰가 아닌데 수정/삭제를 시도할 때 발생합니다.
 */
public class ReviewOwnershipException extends AuthorizationException {

    public ReviewOwnershipException() {
        super(ErrorCode.REVIEW_OWNERSHIP_VIOLATION);
    }

    public ReviewOwnershipException(Long reviewId, Long userId) {
        super(ErrorCode.REVIEW_OWNERSHIP_VIOLATION,
              "본인이 작성한 리뷰만 수정/삭제할 수 있습니다. (Review: " + reviewId + ", User: " + userId + ")");
    }
}