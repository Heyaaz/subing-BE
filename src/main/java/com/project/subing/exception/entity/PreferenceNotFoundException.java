package com.project.subing.exception.entity;

import com.project.subing.exception.ErrorCode;

/**
 * 성향 프로필을 찾을 수 없을 때 발생하는 예외
 *
 * HTTP Status: 404 Not Found
 */
public class PreferenceNotFoundException extends EntityNotFoundException {

    public PreferenceNotFoundException(Long userId) {
        super(ErrorCode.PREFERENCE_NOT_FOUND, "성향 프로필을 찾을 수 없습니다. 먼저 테스트를 완료해주세요. (User ID: " + userId + ")");
    }

    public PreferenceNotFoundException(String message) {
        super(ErrorCode.PREFERENCE_NOT_FOUND, message);
    }
}