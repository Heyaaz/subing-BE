package com.project.subing.dto.preference;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.subing.domain.preference.entity.UserPreference;
import com.project.subing.domain.preference.enums.ProfileType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreferenceResponse {
    private ProfileType profileType;
    private String profileName;
    private String quote;
    private String description;
    private String emoji;
    private Integer contentScore;
    private Integer priceSensitivityScore;
    private Integer healthScore;
    private Integer selfDevelopmentScore;
    private Integer digitalToolScore;
    private List<String> interestedCategories;
    private String budgetRange;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserPreferenceResponse from(UserPreference preference) {
        ObjectMapper objectMapper = new ObjectMapper();
        List<String> categories = new ArrayList<>();

        try {
            categories = objectMapper.readValue(
                preference.getInterestedCategories(),
                new TypeReference<List<String>>() {}
            );
        } catch (JsonProcessingException e) {
            log.error("관심 카테고리 JSON 파싱 실패", e);
        }

        return UserPreferenceResponse.builder()
            .profileType(preference.getProfileType())
            .profileName(preference.getProfileType().getDisplayName())
            .quote(preference.getProfileType().getQuote())
            .description(preference.getProfileType().getDescription())
            .emoji(preference.getProfileType().getEmoji())
            .contentScore(preference.getContentScore())
            .priceSensitivityScore(preference.getPriceSensitivityScore())
            .healthScore(preference.getHealthScore())
            .selfDevelopmentScore(preference.getSelfDevelopmentScore())
            .digitalToolScore(preference.getDigitalToolScore())
            .interestedCategories(categories)
            .budgetRange(preference.getBudgetRange())
            .createdAt(preference.getCreatedAt())
            .updatedAt(preference.getUpdatedAt())
            .build();
    }
}
