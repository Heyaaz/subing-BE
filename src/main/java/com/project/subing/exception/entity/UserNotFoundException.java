package com.project.subing.exception.entity;

import com.project.subing.exception.ErrorCode;

/**
 * 사용자를 찾을 수 없을 때 발생하는 예외
 *
 * HTTP Status: 404 Not Found
 */
public class UserNotFoundException extends EntityNotFoundException {

    public UserNotFoundException(Long userId) {
        super(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다: ID " + userId);
    }

    public UserNotFoundException(String email) {
        super(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다: Email " + email);
    }

    public UserNotFoundException(String message, Object details) {
        super(ErrorCode.USER_NOT_FOUND, message, details);
    }
}