package com.project.subing.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.subing.domain.preference.entity.PreferenceOption;
import com.project.subing.domain.preference.entity.PreferenceQuestion;
import com.project.subing.domain.preference.entity.UserPreference;
import com.project.subing.domain.preference.enums.ProfileType;
import com.project.subing.domain.user.entity.User;
import com.project.subing.repository.PreferenceOptionRepository;
import com.project.subing.repository.PreferenceQuestionRepository;
import com.project.subing.repository.UserPreferenceRepository;
import com.project.subing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PreferenceService {

    private final PreferenceQuestionRepository questionRepository;
    private final PreferenceOptionRepository optionRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * 모든 질문 조회 (순서대로)
     */
    public List<PreferenceQuestion> getAllQuestions() {
        return questionRepository.findAllByOrderByOrderIndexAsc();
    }

    /**
     * 답변 제출 및 분석
     */
    @Transactional
    public UserPreference submitAnswers(Long userId, List<AnswerDto> answers) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 점수 계산
        Map<String, Integer> scores = calculateScores(answers);

        // 카테고리 태그 수집
        Set<String> categoryTags = collectCategoryTags(answers);

        // 프로필 타입 결정
        ProfileType profileType = determineProfileType(scores, categoryTags);

        // 예산 범위 결정
        String budgetRange = determineBudgetRange(scores.get("priceSensitivityScore"));

        // 관심 카테고리 JSON 생성
        String interestedCategories = generateInterestedCategories(categoryTags);

        // 매번 새 레코드 생성 (이력 관리)
        UserPreference userPreference = UserPreference.builder()
            .user(user)
            .profileType(profileType)
            .contentScore(scores.get("contentScore"))
            .priceSensitivityScore(scores.get("priceSensitivityScore"))
            .healthScore(scores.get("healthScore"))
            .selfDevelopmentScore(scores.get("selfDevelopmentScore"))
            .digitalToolScore(scores.get("digitalToolScore"))
            .interestedCategories(interestedCategories)
            .budgetRange(budgetRange)
            .build();

        userPreference = userPreferenceRepository.save(userPreference);

        log.info("사용자 {}의 성향 프로필 저장 완료: {}", userId, profileType);
        return userPreference;
    }

    /**
     * 사용자 프로필 조회 (최신)
     */
    public Optional<UserPreference> getUserPreference(Long userId) {
        return userPreferenceRepository.findFirstByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 사용자 프로필 삭제 - Soft Delete (@SQLDelete)
     */
    @Transactional
    public void deleteUserPreference(Long userId) {
        userPreferenceRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
            .ifPresent(userPreferenceRepository::delete);
        log.info("사용자 {}의 성향 프로필 삭제 완료", userId);
    }

    /**
     * 점수 계산 알고리즘
     */
    private Map<String, Integer> calculateScores(List<AnswerDto> answers) {
        Map<String, Integer> scores = new HashMap<>();
        scores.put("contentScore", 0);
        scores.put("priceSensitivityScore", 0);
        scores.put("healthScore", 0);
        scores.put("selfDevelopmentScore", 0);
        scores.put("digitalToolScore", 0);

        for (AnswerDto answer : answers) {
            PreferenceOption option = optionRepository.findById(answer.getOptionId())
                .orElseThrow(() -> new IllegalArgumentException("옵션을 찾을 수 없습니다: " + answer.getOptionId()));

            // JSON 파싱하여 점수 적용
            try {
                Map<String, Object> scoreImpact = objectMapper.readValue(
                    option.getScoreImpact(),
                    new TypeReference<Map<String, Object>>() {}
                );

                // 각 점수 항목에 대해 처리
                scoreImpact.forEach((key, value) -> {
                    if (scores.containsKey(key) && value instanceof Number) {
                        scores.merge(key, ((Number) value).intValue(), Integer::sum);
                    }
                });
            } catch (JsonProcessingException e) {
                log.error("점수 영향 JSON 파싱 실패: {}", option.getScoreImpact(), e);
            }
        }

        // 점수 정규화 (0-100)
        normalizeScores(scores);

        return scores;
    }

    /**
     * 점수 정규화 (0-100 범위로)
     */
    private void normalizeScores(Map<String, Integer> scores) {
        scores.forEach((key, value) -> {
            // 점수 범위: -100 ~ +200 정도 → 0 ~ 100으로 정규화
            int normalized = Math.max(0, Math.min(100, (value + 50))); // 간단한 정규화
            scores.put(key, normalized);
        });
    }

    /**
     * 카테고리 태그 수집
     */
    private Set<String> collectCategoryTags(List<AnswerDto> answers) {
        Set<String> tags = new HashSet<>();

        for (AnswerDto answer : answers) {
            PreferenceOption option = optionRepository.findById(answer.getOptionId())
                .orElseThrow(() -> new IllegalArgumentException("옵션을 찾을 수 없습니다: " + answer.getOptionId()));

            try {
                List<String> categoryTags = objectMapper.readValue(
                    option.getCategoryTags(),
                    new TypeReference<List<String>>() {}
                );
                tags.addAll(categoryTags);
            } catch (JsonProcessingException e) {
                log.error("카테고리 태그 JSON 파싱 실패: {}", option.getCategoryTags(), e);
            }
        }

        return tags;
    }

    /**
     * 프로필 타입 결정 알고리즘
     */
    private ProfileType determineProfileType(Map<String, Integer> scores, Set<String> categoryTags) {
        int contentScore = scores.get("contentScore");
        int priceScore = scores.get("priceSensitivityScore");
        int healthScore = scores.get("healthScore");
        int selfDevScore = scores.get("selfDevelopmentScore");
        int digitalScore = scores.get("digitalToolScore");

        // 1. 최고 점수 카테고리 찾기
        String topCategory = scores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("contentScore");

        int topScore = scores.get(topCategory);

        // 2. 프로필 타입 매칭
        // 콘텐츠 덕후형 (콘텐츠 점수 높음)
        if (contentScore >= 60 && topCategory.equals("contentScore")) {
            return ProfileType.CONTENT_COLLECTOR;
        }

        // 가성비 마스터형 (가격 민감도 높음)
        if (priceScore >= 60) {
            return ProfileType.SMART_SAVER;
        }

        // 프리미엄 러버형 (가격 민감도 낮음 = 점수가 낮음)
        if (priceScore <= 30) {
            return ProfileType.PREMIUM_ENJOYER;
        }

        // 헬시 라이프형 (건강 점수 높음)
        if (healthScore >= 50 && topCategory.equals("healthScore")) {
            return ProfileType.HEALTHY_LIFESTYLE;
        }

        // 자기계발 중독형 (자기계발 점수 높음)
        if (selfDevScore >= 50 && topCategory.equals("selfDevelopmentScore")) {
            return ProfileType.GROWTH_HACKER;
        }

        // 게이머형 (GAMING 태그 포함)
        if (categoryTags.contains("GAMING")) {
            return ProfileType.HARDCORE_GAMER;
        }

        // 디지털 노마드형 (디지털 도구 점수 높음)
        if (digitalScore >= 50 && topCategory.equals("digitalToolScore")) {
            return ProfileType.CLOUD_WORKER;
        }

        // 기본값: 구독 미니멀리스트형
        return ProfileType.MINIMAL_USER;
    }

    /**
     * 예산 범위 결정
     */
    private String determineBudgetRange(int priceSensitivityScore) {
        if (priceSensitivityScore >= 70) {
            return "월 1만원 이하";
        } else if (priceSensitivityScore >= 40) {
            return "월 1~3만원";
        } else if (priceSensitivityScore >= 20) {
            return "월 3~5만원";
        } else {
            return "월 5만원 이상";
        }
    }

    /**
     * 관심 카테고리 JSON 생성
     */
    private String generateInterestedCategories(Set<String> categoryTags) {
        // 주요 카테고리 추출
        List<String> mainCategories = categoryTags.stream()
            .filter(tag -> tag.matches("STREAMING|MUSIC|READING|GAMING|LEARNING|CLOUD|FITNESS"))
            .map(tag -> {
                switch (tag) {
                    case "STREAMING": return "스트리밍";
                    case "MUSIC": return "음악";
                    case "READING": return "독서";
                    case "GAMING": return "게임";
                    case "LEARNING": return "학습";
                    case "CLOUD": return "클라우드";
                    case "FITNESS": return "운동";
                    default: return tag;
                }
            })
            .distinct()
            .limit(5)
            .collect(Collectors.toList());

        try {
            return objectMapper.writeValueAsString(mainCategories);
        } catch (JsonProcessingException e) {
            log.error("관심 카테고리 JSON 생성 실패", e);
            return "[]";
        }
    }

    /**
     * 답변 DTO
     */
    public static class AnswerDto {
        private Long questionId;
        private Long optionId;

        public AnswerDto() {}

        public AnswerDto(Long questionId, Long optionId) {
            this.questionId = questionId;
            this.optionId = optionId;
        }

        public Long getQuestionId() {
            return questionId;
        }

        public void setQuestionId(Long questionId) {
            this.questionId = questionId;
        }

        public Long getOptionId() {
            return optionId;
        }

        public void setOptionId(Long optionId) {
            this.optionId = optionId;
        }
    }
}
