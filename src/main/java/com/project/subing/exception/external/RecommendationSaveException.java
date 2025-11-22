package com.project.subing.exception.external;

import com.project.subing.exception.ErrorCode;

/**
 * 추천 결과 저장 실패 시 발생하는 예외
 *
 * HTTP Status: 500 Internal Server Error
 */
public class RecommendationSaveException extends ExternalApiException {

    public RecommendationSaveException(String message) {
        super(ErrorCode.RECOMMENDATION_SAVE_ERROR, "추천 결과 저장 실패: " + message);
    }

    public RecommendationSaveException(String message, Throwable cause) {
        super(ErrorCode.RECOMMENDATION_SAVE_ERROR, "추천 결과 저장 실패: " + message, cause);
    }

    public RecommendationSaveException(Throwable cause) {
        super(ErrorCode.RECOMMENDATION_SAVE_ERROR, "추천 결과 저장 실패: " + cause.getMessage(), cause);
    }
}