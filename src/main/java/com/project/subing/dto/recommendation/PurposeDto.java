package com.project.subing.dto.recommendation;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PurposeDto {

    @NotBlank(message = "사용 목적 값은 필수입니다.")
    private String value;

    @NotBlank(message = "사용 목적 라벨은 필수입니다.")
    private String label;
}
