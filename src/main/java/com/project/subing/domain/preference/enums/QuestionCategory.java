package com.project.subing.domain.preference.enums;

/**
 * 질문 카테고리 (6개 영역)
 */
public enum QuestionCategory {
    BUDGET("디지털 월세 예산", "구독료 예산 및 가격 민감도"),
    CONTENT("콘텐츠 소비 패턴", "영상/음악/독서 등 콘텐츠 소비 스타일"),
    SUBSCRIPTION("구독 관리 스타일", "구독 개수 및 관리 방식"),
    HEALTH("건강/운동 관심도", "건강/운동/웰빙 관심도"),
    SELF_DEV("자기계발 관심도", "학습/자기계발 관심도"),
    DIGITAL("디지털 도구 활용도", "클라우드/생산성 도구 필요성");

    private final String displayName;
    private final String description;

    QuestionCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
