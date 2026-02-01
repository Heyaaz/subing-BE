package com.project.subing.exception.auth;

import com.project.subing.exception.ErrorCode;

/**
 * 리소스에 대한 접근 권한이 없을 때 발생하는 예외
 *
 * HTTP Status: 403 Forbidden
 */
public class UnauthorizedAccessException extends AuthorizationException {

    public UnauthorizedAccessException() {
        super(ErrorCode.UNAUTHORIZED_ACCESS);
    }

    public UnauthorizedAccessException(String customMessage) {
        super(ErrorCode.UNAUTHORIZED_ACCESS, customMessage);
    }

    public UnauthorizedAccessException(String resourceType, Long resourceId) {
        super(ErrorCode.UNAUTHORIZED_ACCESS,
              "권한이 없습니다: " + resourceType + " (ID: " + resourceId + ")");
    }
}