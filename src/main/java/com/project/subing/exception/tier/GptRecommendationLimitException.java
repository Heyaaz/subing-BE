package com.project.subing.exception.tier;

import com.project.subing.exception.ErrorCode;

import java.util.Map;

/**
 * GPT 추천 사용 횟수 제한 초과 시 발생하는 예외
 *
 * HTTP Status: 429 Too Many Requests
 */
public class GptRecommendationLimitException extends TierLimitExceededException {

    public GptRecommendationLimitException() {
        super(ErrorCode.GPT_RECOMMENDATION_LIMIT_EXCEEDED,
              "GPT 추천 사용 횟수를 초과했습니다. PRO 티어로 업그레이드하세요.");
    }

    public GptRecommendationLimitException(int used, int limit) {
        super(ErrorCode.GPT_RECOMMENDATION_LIMIT_EXCEEDED,
              "GPT 추천 사용 횟수를 초과했습니다. PRO 티어로 업그레이드하세요.",
              Map.of("used", used, "limit", limit));
    }
}