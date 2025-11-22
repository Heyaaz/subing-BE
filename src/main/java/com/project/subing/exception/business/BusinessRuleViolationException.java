package com.project.subing.exception.business;

import com.project.subing.exception.ApplicationException;
import com.project.subing.exception.ErrorCode;

/**
 * 비즈니스 규칙 위반 시 발생하는 예외의 추상 베이스 클래스
 *
 * HTTP Status: 422 Unprocessable Entity
 *
 * 입력 값은 유효하지만 비즈니스 로직상 허용되지 않는 경우 발생합니다.
 * 예: 중복 이메일, 잘못된 평점 범위, 중복 리뷰 등
 */
public abstract class BusinessRuleViolationException extends ApplicationException {

    protected BusinessRuleViolationException(ErrorCode errorCode) {
        super(errorCode);
    }

    protected BusinessRuleViolationException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }

    protected BusinessRuleViolationException(ErrorCode errorCode, String customMessage, Object details) {
        super(errorCode, customMessage, details);
    }
}