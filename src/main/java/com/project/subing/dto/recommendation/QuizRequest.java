package com.project.subing.dto.recommendation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.List;

@Getter
@EqualsAndHashCode
public class QuizRequest {

    @NotEmpty(message = "관심 분야는 최소 1개 이상 선택해야 합니다.")
    @Size(max = 3, message = "관심 분야는 최대 3개까지 선택 가능합니다.")
    private List<String> interests;

    @NotNull(message = "월 예산은 필수입니다.")
    @Positive(message = "월 예산은 양수여야 합니다.")
    private Integer budget;

    @NotNull(message = "사용 목적은 필수입니다.")
    @Valid
    private PurposeDto purpose;

    @NotEmpty(message = "중요도는 최소 1개 이상 선택해야 합니다.")
    @Size(max = 3, message = "중요도는 최대 3개까지 선택 가능합니다.")
    @Valid
    private List<PriorityDto> priorities;

    /**
     * 사용 목적 라벨 반환 (프롬프트 생성용)
     */
    public String getPurposeLabel() {
        return purpose != null ? purpose.getLabel() : "";
    }

    /**
     * 중요도 라벨 목록 반환 (순위 순서대로 정렬)
     */
    public List<String> getPriorityLabels() {
        if (priorities == null) return List.of();
        return priorities.stream()
                .sorted((a, b) -> a.getRank().compareTo(b.getRank()))
                .map(PriorityDto::getLabel)
                .toList();
    }

    /**
     * 중요도를 "1순위: X, 2순위: Y" 형식으로 반환
     */
    public String getPrioritiesFormatted() {
        if (priorities == null || priorities.isEmpty()) return "";
        return priorities.stream()
                .sorted((a, b) -> a.getRank().compareTo(b.getRank()))
                .map(p -> p.getRank() + "순위: " + p.getLabel())
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }
}
