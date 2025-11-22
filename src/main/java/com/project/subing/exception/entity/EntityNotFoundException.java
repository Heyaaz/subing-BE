package com.project.subing.exception.entity;

import com.project.subing.exception.ApplicationException;
import com.project.subing.exception.ErrorCode;

/**
 * 엔티티를 찾을 수 없을 때 발생하는 예외의 추상 베이스 클래스
 *
 * HTTP Status: 404 Not Found
 *
 * 모든 엔티티별 NotFoundException은 이 클래스를 상속받아 구현합니다.
 * 예: UserNotFoundException, ServiceNotFoundException 등
 */
public abstract class EntityNotFoundException extends ApplicationException {

    protected EntityNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }

    protected EntityNotFoundException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }

    protected EntityNotFoundException(ErrorCode errorCode, String customMessage, Object details) {
        super(errorCode, customMessage, details);
    }
}