package com.project.subing.exception.external;

import com.project.subing.exception.ApplicationException;
import com.project.subing.exception.ErrorCode;

/**
 * 외부 API 호출 실패 시 발생하는 예외의 추상 베이스 클래스
 *
 * HTTP Status: 502 Bad Gateway / 503 Service Unavailable
 *
 * OpenAI GPT API 등 외부 API 호출이 실패했을 때 발생합니다.
 */
public abstract class ExternalApiException extends ApplicationException {

    protected ExternalApiException(ErrorCode errorCode) {
        super(errorCode);
    }

    protected ExternalApiException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }

    protected ExternalApiException(ErrorCode errorCode, String customMessage, Throwable cause) {
        super(errorCode, customMessage, cause);
    }

    protected ExternalApiException(ErrorCode errorCode, String customMessage, Object details, Throwable cause) {
        super(errorCode, customMessage, details, cause);
    }
}