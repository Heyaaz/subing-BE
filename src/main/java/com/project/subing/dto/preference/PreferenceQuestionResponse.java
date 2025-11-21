package com.project.subing.dto.preference;

import com.project.subing.domain.preference.entity.PreferenceQuestion;
import com.project.subing.domain.preference.enums.QuestionCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreferenceQuestionResponse {
    private Long id;
    private QuestionCategory category;
    private String questionText;
    private String emoji;
    private List<PreferenceOptionResponse> options;

    public static PreferenceQuestionResponse from(PreferenceQuestion question) {
        return PreferenceQuestionResponse.builder()
            .id(question.getId())
            .category(question.getCategory())
            .questionText(question.getQuestionText())
            .emoji(question.getEmoji())
            .options(question.getOptions().stream()
                .map(PreferenceOptionResponse::from)
                .collect(Collectors.toList()))
            .build();
    }
}
