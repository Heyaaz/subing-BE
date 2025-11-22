package com.project.subing.exception.external;

import com.project.subing.exception.ErrorCode;

/**
 * GPT 응답 파싱 실패 시 발생하는 예외
 *
 * HTTP Status: 502 Bad Gateway
 */
public class GptParsingException extends ExternalApiException {

    public GptParsingException(String message) {
        super(ErrorCode.GPT_PARSING_ERROR, "GPT 응답 파싱 실패: " + message);
    }

    public GptParsingException(String message, Throwable cause) {
        super(ErrorCode.GPT_PARSING_ERROR, "GPT 응답 파싱 실패: " + message, cause);
    }

    public GptParsingException(Throwable cause) {
        super(ErrorCode.GPT_PARSING_ERROR, "GPT 응답 파싱 실패: " + cause.getMessage(), cause);
    }
}