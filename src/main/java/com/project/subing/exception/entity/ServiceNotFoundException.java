package com.project.subing.exception.entity;

import com.project.subing.exception.ErrorCode;

/**
 * 서비스를 찾을 수 없을 때 발생하는 예외
 *
 * HTTP Status: 404 Not Found
 */
public class ServiceNotFoundException extends EntityNotFoundException {

    public ServiceNotFoundException(Long serviceId) {
        super(ErrorCode.SERVICE_NOT_FOUND, "서비스를 찾을 수 없습니다: ID " + serviceId);
    }

    public ServiceNotFoundException(String message) {
        super(ErrorCode.SERVICE_NOT_FOUND, message);
    }
}