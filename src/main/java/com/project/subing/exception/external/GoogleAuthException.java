package com.project.subing.exception.external;

import com.project.subing.exception.ErrorCode;

public class GoogleAuthException extends ExternalApiException {

    public GoogleAuthException(String message) {
        super(ErrorCode.GOOGLE_AUTH_ERROR, "Google 인증 실패: " + message);
    }

    public GoogleAuthException(String message, Throwable cause) {
        super(ErrorCode.GOOGLE_AUTH_ERROR, "Google 인증 실패: " + message, cause);
    }

    public GoogleAuthException(Throwable cause) {
        super(ErrorCode.GOOGLE_AUTH_ERROR, "Google 인증 실패: " + cause.getMessage(), cause);
    }
}
