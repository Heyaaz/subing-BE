package com.project.subing.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserRole {
    USER("일반 사용자"),
    ADMIN("관리자");

    private final String description;

    public boolean isAdmin() {
        return this == ADMIN;
    }

    public boolean isUser() {
        return this == USER;
    }
}