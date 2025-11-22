package com.project.subing.exception.external;

import com.project.subing.exception.ErrorCode;

/**
 * GPT API 호출 실패 시 발생하는 예외
 *
 * HTTP Status: 502 Bad Gateway
 */
public class GptApiException extends ExternalApiException {

    public GptApiException(String message) {
        super(ErrorCode.GPT_API_ERROR, "GPT API 호출 실패: " + message);
    }

    public GptApiException(String message, Throwable cause) {
        super(ErrorCode.GPT_API_ERROR, "GPT API 호출 실패: " + message, cause);
    }

    public GptApiException(Throwable cause) {
        super(ErrorCode.GPT_API_ERROR, "GPT API 호출 실패: " + cause.getMessage(), cause);
    }
}