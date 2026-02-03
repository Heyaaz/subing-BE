package com.project.subing.dto.recommendation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PriorityDto {

    @NotBlank(message = "중요도 값은 필수입니다.")
    private String value;

    @NotBlank(message = "중요도 라벨은 필수입니다.")
    private String label;

    @NotNull(message = "순위는 필수입니다.")
    @Min(value = 1, message = "순위는 1 이상이어야 합니다.")
    @Max(value = 3, message = "순위는 3 이하여야 합니다.")
    private Integer rank;
}
