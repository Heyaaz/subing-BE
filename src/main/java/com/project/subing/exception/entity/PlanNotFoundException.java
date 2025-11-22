package com.project.subing.exception.entity;

import com.project.subing.exception.ErrorCode;

/**
 * 플랜을 찾을 수 없을 때 발생하는 예외
 *
 * HTTP Status: 404 Not Found
 */
public class PlanNotFoundException extends EntityNotFoundException {

    public PlanNotFoundException(Long planId) {
        super(ErrorCode.PLAN_NOT_FOUND, "플랜을 찾을 수 없습니다: ID " + planId);
    }
}