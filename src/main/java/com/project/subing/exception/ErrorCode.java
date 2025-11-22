package com.project.subing.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * API 에러 코드 정의
 *
 * 모든 에러는 고유한 코드와 HTTP 상태 코드, 기본 메시지를 가집니다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ============================================================
    // 400 Bad Request - 잘못된 입력
    // ============================================================
    INVALID_INPUT("400", "잘못된 입력입니다"),

    // ============================================================
    // 401 Unauthorized - 인증 실패
    // ============================================================
    INVALID_CREDENTIALS("401", "이메일 또는 비밀번호가 올바르지 않습니다"),

    // ============================================================
    // 403 Forbidden - 권한 없음
    // ============================================================
    UNAUTHORIZED_ACCESS("403", "권한이 없습니다"),
    REVIEW_OWNERSHIP_VIOLATION("403", "본인이 작성한 리뷰만 수정/삭제할 수 있습니다"),

    // ============================================================
    // 404 Not Found - 리소스를 찾을 수 없음
    // ============================================================
    USER_NOT_FOUND("404", "사용자를 찾을 수 없습니다"),
    SERVICE_NOT_FOUND("404", "서비스를 찾을 수 없습니다"),
    SUBSCRIPTION_NOT_FOUND("404", "구독을 찾을 수 없습니다"),
    REVIEW_NOT_FOUND("404", "리뷰를 찾을 수 없습니다"),
    BUDGET_NOT_FOUND("404", "예산을 찾을 수 없습니다"),
    PLAN_NOT_FOUND("404", "플랜을 찾을 수 없습니다"),
    NOTIFICATION_NOT_FOUND("404", "알림을 찾을 수 없습니다"),
    PREFERENCE_NOT_FOUND("404", "성향 프로필을 찾을 수 없습니다"),
    RECOMMENDATION_NOT_FOUND("404", "추천 결과를 찾을 수 없습니다"),
    OPTION_NOT_FOUND("404", "옵션을 찾을 수 없습니다"),

    // ============================================================
    // 422 Unprocessable Entity - 비즈니스 규칙 위반
    // ============================================================
    DUPLICATE_EMAIL("422", "이미 사용 중인 이메일입니다"),
    INVALID_RATING("422", "평점은 1~5 사이의 값이어야 합니다"),
    DUPLICATE_REVIEW("422", "이미 해당 서비스에 리뷰를 작성하셨습니다"),
    MISSING_SERVICES("422", "일부 서비스를 찾을 수 없습니다"),

    // ============================================================
    // 429 Too Many Requests - 사용량 제한 초과
    // ============================================================
    GPT_RECOMMENDATION_LIMIT_EXCEEDED("429", "GPT 추천 사용 횟수를 초과했습니다"),
    OPTIMIZATION_CHECK_LIMIT_EXCEEDED("429", "최적화 체크 사용 횟수를 초과했습니다"),

    // ============================================================
    // 500 Internal Server Error - 서버 내부 오류
    // ============================================================
    INTERNAL_SERVER_ERROR("500", "서버 내부 오류가 발생했습니다"),
    RECOMMENDATION_SAVE_ERROR("500", "추천 결과 저장에 실패했습니다"),

    // ============================================================
    // 502 Bad Gateway - 외부 API 오류
    // ============================================================
    GPT_API_ERROR("502", "GPT API 호출에 실패했습니다"),
    GPT_PARSING_ERROR("502", "GPT 응답 파싱에 실패했습니다");

    /**
     * HTTP 상태 코드 (문자열)
     */
    private final String code;

    /**
     * 기본 에러 메시지
     */
    private final String message;

    /**
     * HTTP 상태 코드를 정수로 반환
     *
     * @return HTTP 상태 코드
     */
    public int getHttpStatus() {
        return Integer.parseInt(code);
    }
}