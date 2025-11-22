package com.project.subing.exception.entity;

import com.project.subing.exception.ErrorCode;

/**
 * 옵션을 찾을 수 없을 때 발생하는 예외
 *
 * HTTP Status: 404 Not Found
 */
public class OptionNotFoundException extends EntityNotFoundException {

    public OptionNotFoundException(Long optionId) {
        super(ErrorCode.OPTION_NOT_FOUND, "옵션을 찾을 수 없습니다: ID " + optionId);
    }
}