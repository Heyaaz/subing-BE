package com.project.subing.exception.tier;

import com.project.subing.exception.ApplicationException;
import com.project.subing.exception.ErrorCode;

/**
 * 회원 등급 사용량 제한 초과 시 발생하는 예외의 추상 베이스 클래스
 *
 * HTTP Status: 429 Too Many Requests
 *
 * FREE 티어 사용자가 제한된 기능의 사용 횟수를 초과했을 때 발생합니다.
 */
public abstract class TierLimitExceededException extends ApplicationException {

    protected TierLimitExceededException(ErrorCode errorCode) {
        super(errorCode);
    }

    protected TierLimitExceededException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }

    protected TierLimitExceededException(ErrorCode errorCode, String customMessage, Object details) {
        super(errorCode, customMessage, details);
    }
}