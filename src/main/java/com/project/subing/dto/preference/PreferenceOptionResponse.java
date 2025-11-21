package com.project.subing.dto.preference;

import com.project.subing.domain.preference.entity.PreferenceOption;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreferenceOptionResponse {
    private Long id;
    private String text;
    private String subtext;
    private String emoji;

    public static PreferenceOptionResponse from(PreferenceOption option) {
        return PreferenceOptionResponse.builder()
            .id(option.getId())
            .text(option.getOptionText())
            .subtext(option.getSubtext())
            .emoji(option.getEmoji())
            .build();
    }
}
