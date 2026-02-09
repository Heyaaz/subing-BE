package com.project.subing.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthProvider {
    LOCAL("일반"),
    GOOGLE("구글");

    private final String description;
}
