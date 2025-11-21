package com.project.subing.domain.preference.enums;

/**
 * 사용자 성향 프로필 타입 (8가지)
 */
public enum ProfileType {
    CONTENT_COLLECTOR(
        "구독 덕후형",
        "구독 많을수록 행복해!",
        "영상/음악/독서 다 좋아하는 콘텐츠 올인형",
        "🎬"
    ),
    SMART_SAVER(
        "알뜰 구독러형",
        "가성비 없으면 안 써!",
        "저렴하면서 실용적인 서비스만 쏙쏙",
        "💰"
    ),
    PREMIUM_ENJOYER(
        "프리미엄 러버형",
        "비싸도 좋으면 OK!",
        "브랜드와 품질 중시, 돈보다 가치",
        "💎"
    ),
    HEALTHY_LIFESTYLE(
        "헬시 라이프형",
        "건강이 최고!",
        "운동/식단/웰빙 관심 많음",
        "💪"
    ),
    GROWTH_HACKER(
        "자기계발 중독형",
        "배움은 계속된다!",
        "학습/강의/생산성 도구에 투자",
        "📚"
    ),
    HARDCORE_GAMER(
        "게이머형",
        "게임이 곧 인생!",
        "게임 구독 서비스 애호가",
        "🎮"
    ),
    CLOUD_WORKER(
        "디지털 노마드형",
        "클라우드가 내 사무실!",
        "업무 효율 극대화형",
        "☁️"
    ),
    MINIMAL_USER(
        "구독 미니멀리스트형",
        "꼭 필요한 것만!",
        "필수 서비스만 최소한으로",
        "🪶"
    );

    private final String displayName;
    private final String quote;
    private final String description;
    private final String emoji;

    ProfileType(String displayName, String quote, String description, String emoji) {
        this.displayName = displayName;
        this.quote = quote;
        this.description = description;
        this.emoji = emoji;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getQuote() {
        return quote;
    }

    public String getDescription() {
        return description;
    }

    public String getEmoji() {
        return emoji;
    }
}
