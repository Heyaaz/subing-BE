package com.project.subing.domain.optimization.entity;

public enum OptimizationEventType {
    IMPRESSION,
    CLICK_ALTERNATIVE,
    CLICK_MANAGE,
    DISMISS,
    REFRESH;

    public static OptimizationEventType from(String value) {
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("지원하지 않는 optimization 이벤트 타입입니다: " + value);
        }
    }
}
