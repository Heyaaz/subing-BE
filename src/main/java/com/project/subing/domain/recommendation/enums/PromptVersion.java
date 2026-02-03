package com.project.subing.domain.recommendation.enums;

/**
 * GPT 프롬프트 버전 (A/B 테스트용)
 */
public enum PromptVersion {
    /**
     * V1: 기본 프롬프트 (현재 사용 중)
     * - 성향 데이터 활용
     * - 장점 3개, 단점 2개
     */
    V1("기본 프롬프트", """
        당신은 구독 서비스 추천 전문가입니다.
        사용자의 선호도와 예산을 꼼꼼히 분석하여 최적의 구독 서비스를 추천해주세요.
        추천 시 다음을 반드시 포함하세요:
        - 추천 이유 (구체적이고 설득력 있게)
        - 장점 3가지
        - 단점 2가지
        - 실용적인 팁

        한글 문장은 반드시 올바른 띄어쓰기를 적용하여 작성하세요.
        출력은 반드시 유효한 JSON 형식이어야 합니다.
        """),

    /**
     * V2: 개선된 프롬프트 (A/B 테스트)
     * - 성향 데이터 강조
     * - 구체적인 사용 시나리오 요청
     */
    V2("성향 중심 프롬프트", """
        당신은 사용자 맞춤형 구독 서비스 추천 전문가입니다.
        사용자의 성향 프로필과 선호도를 기반으로 가장 적합한 서비스를 추천하세요.

        추천 시 다음을 반드시 포함하세요:
        - 추천 이유 (사용자의 성향과 연결하여 구체적으로)
        - 장점 3가지 (사용자가 얻을 수 있는 가치 중심)
        - 단점 2가지 (솔직하게)
        - 실용적인 팁 (구체적인 사용 시나리오 포함)

        한글 문장은 반드시 올바른 띄어쓰기를 적용하여 작성하세요.
        출력은 반드시 유효한 JSON 형식이어야 합니다.
        """);

    private final String description;
    private final String systemPrompt;

    PromptVersion(String description, String systemPrompt) {
        this.description = description;
        this.systemPrompt = systemPrompt;
    }

    public String getDescription() {
        return description;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    /**
     * A/B 테스트를 위한 랜덤 버전 선택
     * V1: 50%, V2: 50%
     */
    public static PromptVersion random() {
        return Math.random() < 0.5 ? V1 : V2;
    }
}