package com.project.subing.domain.notification.entity;

public enum NotificationType {
    PAYMENT_DUE_3DAYS("결제일 3일 전"),
    PAYMENT_DUE_1DAY("결제일 1일 전"),
    BUDGET_EXCEEDED("예산 초과"),
    UNUSED_SUBSCRIPTION("미사용 구독"),
    PRICE_CHANGE("가격 변동"),
    SUBSCRIPTION_RENEWAL("구독 갱신");

    private final String description;

    NotificationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}