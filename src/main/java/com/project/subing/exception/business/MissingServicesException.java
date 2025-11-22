package com.project.subing.exception.business;

import com.project.subing.exception.ErrorCode;

/**
 * 요청한 서비스 목록 중 일부를 찾을 수 없을 때 발생하는 예외
 *
 * HTTP Status: 422 Unprocessable Entity
 */
public class MissingServicesException extends BusinessRuleViolationException {

    public MissingServicesException() {
        super(ErrorCode.MISSING_SERVICES);
    }

    public MissingServicesException(Object missingServiceIds) {
        super(ErrorCode.MISSING_SERVICES, "일부 서비스를 찾을 수 없습니다", missingServiceIds);
    }
}