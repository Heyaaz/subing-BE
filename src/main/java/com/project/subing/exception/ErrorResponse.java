package com.project.subing.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * API 에러 응답 DTO
 *
 * GlobalExceptionHandler에서 생성하여 클라이언트에게 반환하는 에러 응답 객체
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /**
     * 에러 코드 (예: USER_NOT_FOUND, INVALID_INPUT)
     */
    private String errorCode;

    /**
     * 사용자에게 표시할 에러 메시지
     */
    private String message;

    /**
     * 에러 발생 시각
     */
    private LocalDateTime timestamp;

    /**
     * 에러가 발생한 API 경로
     */
    private String path;

    /**
     * 추가 디버깅 정보 (선택적)
     * - Validation 에러의 경우 필드별 에러 메시지
     * - Tier 제한 에러의 경우 현재 사용량, 제한 등
     */
    private Object details;

    /**
     * 기본 ErrorResponse 생성 (details 없이)
     *
     * @param errorCode 에러 코드
     * @param message 에러 메시지
     * @param path API 경로
     * @return ErrorResponse 객체
     */
    public static ErrorResponse of(String errorCode, String message, String path) {
        return ErrorResponse.builder()
                .errorCode(errorCode)
                .message(message)
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();
    }

    /**
     * 상세 정보 포함 ErrorResponse 생성
     *
     * @param errorCode 에러 코드
     * @param message 에러 메시지
     * @param path API 경로
     * @param details 추가 정보
     * @return ErrorResponse 객체
     */
    public static ErrorResponse of(String errorCode, String message, String path, Object details) {
        return ErrorResponse.builder()
                .errorCode(errorCode)
                .message(message)
                .timestamp(LocalDateTime.now())
                .path(path)
                .details(details)
                .build();
    }

    /**
     * ApplicationException으로부터 ErrorResponse 생성
     *
     * @param exception ApplicationException
     * @param path API 경로
     * @return ErrorResponse 객체
     */
    public static ErrorResponse from(ApplicationException exception, String path) {
        return ErrorResponse.builder()
                .errorCode(exception.getErrorCode().name())
                .message(exception.getMessage())
                .timestamp(LocalDateTime.now())
                .path(path)
                .details(exception.getDetails())
                .build();
    }
}