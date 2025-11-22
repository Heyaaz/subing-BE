package com.project.subing.exception.auth;

import com.project.subing.exception.ApplicationException;
import com.project.subing.exception.ErrorCode;

/**
 * 권한 관련 예외의 추상 베이스 클래스
 *
 * HTTP Status: 403 Forbidden
 *
 * 인증은 되었으나 해당 리소스에 대한 권한이 없을 때 발생합니다.
 */
public abstract class AuthorizationException extends ApplicationException {

    protected AuthorizationException(ErrorCode errorCode) {
        super(errorCode);
    }

    protected AuthorizationException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }

    protected AuthorizationException(ErrorCode errorCode, String customMessage, Object details) {
        super(errorCode, customMessage, details);
    }
}