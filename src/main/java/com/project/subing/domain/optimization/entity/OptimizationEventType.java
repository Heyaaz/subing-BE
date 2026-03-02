package com.project.subing.domain.optimization.entity;

import java.util.Arrays;

public enum OptimizationEventType {
    IMPRESSION,
    CLICK_ALTERNATIVE,
    CLICK_MANAGE,
    DISMISS,
    REFRESH;

    public static OptimizationEventType from(String value) {
        String normalized = value.trim().toUpperCase();
        return Arrays.stream(values())
                .filter(type -> type.name().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "지원하지 않는 optimization 이벤트 타입입니다: " + value));
    }
}
