package com.project.subing.exception.tier;

import com.project.subing.exception.ErrorCode;

import java.util.Map;

/**
 * 최적화 체크 사용 횟수 제한 초과 시 발생하는 예외
 *
 * HTTP Status: 429 Too Many Requests
 */
public class OptimizationCheckLimitException extends TierLimitExceededException {

    public OptimizationCheckLimitException() {
        super(ErrorCode.OPTIMIZATION_CHECK_LIMIT_EXCEEDED,
              "최적화 체크 사용 횟수를 초과했습니다. PRO 티어로 업그레이드하세요.");
    }

    public OptimizationCheckLimitException(int used, int limit) {
        super(ErrorCode.OPTIMIZATION_CHECK_LIMIT_EXCEEDED,
              "최적화 체크 사용 횟수를 초과했습니다. PRO 티어로 업그레이드하세요.",
              Map.of("used", used, "limit", limit));
    }
}