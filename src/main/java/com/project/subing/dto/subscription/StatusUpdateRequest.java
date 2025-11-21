package com.project.subing.dto.subscription;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class StatusUpdateRequest {
    
    @NotNull(message = "활성화 상태는 필수입니다.")
    private Boolean isActive;
}
