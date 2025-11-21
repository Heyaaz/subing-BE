package com.project.subing.dto.recommendation;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.List;

@Getter
@EqualsAndHashCode
public class QuizRequest {

    @NotEmpty(message = "관심 분야는 최소 1개 이상 선택해야 합니다.")
    private List<String> interests;

    @NotNull(message = "월 예산은 필수입니다.")
    @Positive(message = "월 예산은 양수여야 합니다.")
    private Integer budget;

    @NotNull(message = "사용 목적은 필수입니다.")
    private String purpose;

    @NotEmpty(message = "중요도는 최소 1개 이상 선택해야 합니다.")
    private List<String> priorities;
}