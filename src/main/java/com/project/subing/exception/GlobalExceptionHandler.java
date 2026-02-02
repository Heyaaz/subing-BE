package com.project.subing.exception;

import com.project.subing.dto.common.ApiResponse;
import com.project.subing.exception.auth.AuthorizationException;
import com.project.subing.exception.business.BusinessRuleViolationException;
import com.project.subing.exception.business.InvalidCredentialsException;
import com.project.subing.exception.entity.EntityNotFoundException;
import com.project.subing.exception.external.ExternalApiException;
import com.project.subing.exception.tier.TierLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 전역 예외 처리 핸들러
 *
 * 모든 컨트롤러에서 발생하는 예외를 일관된 형식으로 처리합니다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * InvalidCredentialsException 처리
     * HTTP Status: 401 Unauthorized
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleInvalidCredentials(
            InvalidCredentialsException e,
            HttpServletRequest request) {

        log.warn("[401] Invalid credentials: {} | Path: {}", e.getMessage(), request.getRequestURI());

        ErrorResponse errorResponse = ErrorResponse.from(e, request.getRequestURI());

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.<ErrorResponse>builder()
                        .success(false)
                        .data(errorResponse)
                        .message(errorResponse.getMessage())
                        .timestamp(errorResponse.getTimestamp())
                        .build());
    }

    /**
     * EntityNotFoundException 처리
     * HTTP Status: 404 Not Found
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleEntityNotFound(
            EntityNotFoundException e,
            HttpServletRequest request) {

        log.warn("[404] Entity not found: {} | Path: {}", e.getMessage(), request.getRequestURI());

        ErrorResponse errorResponse = ErrorResponse.from(e, request.getRequestURI());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<ErrorResponse>builder()
                        .success(false)
                        .data(errorResponse)
                        .message(errorResponse.getMessage())
                        .timestamp(errorResponse.getTimestamp())
                        .build());
    }

    /**
     * BusinessRuleViolationException 처리
     * HTTP Status: 422 Unprocessable Entity
     */
    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleBusinessRuleViolation(
            BusinessRuleViolationException e,
            HttpServletRequest request) {

        log.warn("[422] Business rule violation: {} | Path: {}", e.getMessage(), request.getRequestURI());

        ErrorResponse errorResponse = ErrorResponse.from(e, request.getRequestURI());

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.<ErrorResponse>builder()
                        .success(false)
                        .data(errorResponse)
                        .message(errorResponse.getMessage())
                        .timestamp(errorResponse.getTimestamp())
                        .build());
    }

    /**
     * AuthorizationException 처리
     * HTTP Status: 403 Forbidden
     */
    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleAuthorization(
            AuthorizationException e,
            HttpServletRequest request) {

        log.warn("[403] Authorization failed: {} | Path: {} | User: {}",
                e.getMessage(), request.getRequestURI(), request.getUserPrincipal());

        ErrorResponse errorResponse = ErrorResponse.from(e, request.getRequestURI());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.<ErrorResponse>builder()
                        .success(false)
                        .data(errorResponse)
                        .message(errorResponse.getMessage())
                        .timestamp(errorResponse.getTimestamp())
                        .build());
    }

    /**
     * TierLimitExceededException 처리
     * HTTP Status: 429 Too Many Requests
     */
    @ExceptionHandler(TierLimitExceededException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleTierLimitExceeded(
            TierLimitExceededException e,
            HttpServletRequest request) {

        log.warn("[429] Tier limit exceeded: {} | Path: {} | User: {}",
                e.getMessage(), request.getRequestURI(), request.getUserPrincipal());

        ErrorResponse errorResponse = ErrorResponse.from(e, request.getRequestURI());

        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiResponse.<ErrorResponse>builder()
                        .success(false)
                        .data(errorResponse)
                        .message(errorResponse.getMessage())
                        .timestamp(errorResponse.getTimestamp())
                        .build());
    }

    /**
     * ExternalApiException 처리
     * HTTP Status: 502 Bad Gateway
     */
    @ExceptionHandler(ExternalApiException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleExternalApi(
            ExternalApiException e,
            HttpServletRequest request) {

        log.error("[502] External API error: {} | Path: {}", e.getMessage(), request.getRequestURI(), e);

        ErrorResponse errorResponse = ErrorResponse.from(e, request.getRequestURI());

        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.<ErrorResponse>builder()
                        .success(false)
                        .data(errorResponse)
                        .message(errorResponse.getMessage())
                        .timestamp(errorResponse.getTimestamp())
                        .build());
    }

    /**
     * Validation 에러 처리 (@Valid 실패)
     * HTTP Status: 400 Bad Request
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleValidationError(
            MethodArgumentNotValidException e,
            HttpServletRequest request) {

        log.warn("[400] Validation failed: {} errors | Path: {}",
                e.getBindingResult().getErrorCount(), request.getRequestURI());

        // 필드별 에러 메시지 수집
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("INVALID_INPUT")
                .message("입력 값 검증에 실패했습니다")
                .timestamp(java.time.LocalDateTime.now())
                .path(request.getRequestURI())
                .details(fieldErrors)
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<ErrorResponse>builder()
                        .success(false)
                        .data(errorResponse)
                        .message(errorResponse.getMessage())
                        .timestamp(errorResponse.getTimestamp())
                        .build());
    }

    /**
     * IllegalArgumentException 처리 (기존 코드 호환용)
     * HTTP Status: 400 Bad Request
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleIllegalArgument(
            IllegalArgumentException e,
            HttpServletRequest request) {

        log.warn("[400] Illegal argument: {} | Path: {}", e.getMessage(), request.getRequestURI());

        ErrorResponse errorResponse = ErrorResponse.of(
                "INVALID_INPUT",
                e.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<ErrorResponse>builder()
                        .success(false)
                        .data(errorResponse)
                        .message(errorResponse.getMessage())
                        .timestamp(errorResponse.getTimestamp())
                        .build());
    }

    /**
     * 일반 예외 처리 (예상치 못한 에러)
     * HTTP Status: 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleGenericException(
            Exception e,
            HttpServletRequest request) {

        log.error("[500] Unexpected error: {} | Path: {}", e.getMessage(), request.getRequestURI(), e);

        ErrorResponse errorResponse = ErrorResponse.of(
                "INTERNAL_SERVER_ERROR",
                "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<ErrorResponse>builder()
                        .success(false)
                        .data(errorResponse)
                        .message(errorResponse.getMessage())
                        .timestamp(errorResponse.getTimestamp())
                        .build());
    }
}