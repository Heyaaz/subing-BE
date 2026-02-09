package com.project.subing.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class GoogleLoginRequest {

    @NotBlank(message = "인증 코드는 필수입니다.")
    private String code;

    @NotBlank(message = "리다이렉트 URI는 필수입니다.")
    private String redirectUri;
}
