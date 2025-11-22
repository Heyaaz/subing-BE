package com.project.subing.exception.entity;

import com.project.subing.exception.ErrorCode;

/**
 * 구독을 찾을 수 없을 때 발생하는 예외
 *
 * HTTP Status: 404 Not Found
 */
public class SubscriptionNotFoundException extends EntityNotFoundException {

    public SubscriptionNotFoundException(Long subscriptionId) {
        super(ErrorCode.SUBSCRIPTION_NOT_FOUND, "구독을 찾을 수 없습니다: ID " + subscriptionId);
    }
}