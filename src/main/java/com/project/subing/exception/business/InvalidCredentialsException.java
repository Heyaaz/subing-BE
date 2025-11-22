package com.project.subing.exception.business;

import com.project.subing.exception.ErrorCode;

/**
 * 로그인 인증 실패 시 발생하는 예외
 *
 * HTTP Status: 401 Unauthorized
 */
public class InvalidCredentialsException extends BusinessRuleViolationException {

    public InvalidCredentialsException() {
        super(ErrorCode.INVALID_CREDENTIALS);
    }

    public InvalidCredentialsException(String email) {
        super(ErrorCode.INVALID_CREDENTIALS, "이메일 또는 비밀번호가 올바르지 않습니다: " + email);
    }
}