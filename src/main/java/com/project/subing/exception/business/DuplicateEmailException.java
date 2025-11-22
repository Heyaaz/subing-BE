package com.project.subing.exception.business;

import com.project.subing.exception.ErrorCode;

/**
 * 이메일 중복 시 발생하는 예외
 *
 * HTTP Status: 422 Unprocessable Entity
 */
public class DuplicateEmailException extends BusinessRuleViolationException {

    public DuplicateEmailException(String email) {
        super(ErrorCode.DUPLICATE_EMAIL, "이미 사용 중인 이메일입니다: " + email);
    }

    public DuplicateEmailException() {
        super(ErrorCode.DUPLICATE_EMAIL);
    }
}