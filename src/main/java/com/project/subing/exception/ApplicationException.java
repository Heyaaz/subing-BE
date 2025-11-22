package com.project.subing.exception;

import lombok.Getter;

/**
 * 모든 커스텀 예외의 추상 베이스 클래스
 *
 * 모든 비즈니스 예외는 이 클래스를 상속받아 구현합니다.
 * ErrorCode를 통해 에러를 분류하고, 추가 정보를 details에 담을 수 있습니다.
 */
@Getter
public abstract class ApplicationException extends RuntimeException {

    /**
     * 에러 코드 (HTTP 상태 코드 및 메시지 포함)
     */
    private final ErrorCode errorCode;

    /**
     * 추가 디버깅 정보 (선택적)
     */
    private final Object details;

    /**
     * 기본 생성자 - ErrorCode의 기본 메시지 사용
     *
     * @param errorCode 에러 코드
     */
    protected ApplicationException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.details = null;
    }

    /**
     * 커스텀 메시지 생성자
     *
     * @param errorCode 에러 코드
     * @param customMessage 커스텀 에러 메시지
     */
    protected ApplicationException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
        this.details = null;
    }

    /**
     * 상세 정보 포함 생성자
     *
     * @param errorCode 에러 코드
     * @param customMessage 커스텀 에러 메시지
     * @param details 추가 디버깅 정보 (Map, 객체 등)
     */
    protected ApplicationException(ErrorCode errorCode, String customMessage, Object details) {
        super(customMessage);
        this.errorCode = errorCode;
        this.details = details;
    }

    /**
     * 원인 예외 포함 생성자
     *
     * @param errorCode 에러 코드
     * @param customMessage 커스텀 에러 메시지
     * @param cause 원인 예외
     */
    protected ApplicationException(ErrorCode errorCode, String customMessage, Throwable cause) {
        super(customMessage, cause);
        this.errorCode = errorCode;
        this.details = null;
    }

    /**
     * 모든 정보 포함 생성자
     *
     * @param errorCode 에러 코드
     * @param customMessage 커스텀 에러 메시지
     * @param details 추가 디버깅 정보
     * @param cause 원인 예외
     */
    protected ApplicationException(ErrorCode errorCode, String customMessage, Object details, Throwable cause) {
        super(customMessage, cause);
        this.errorCode = errorCode;
        this.details = details;
    }
}