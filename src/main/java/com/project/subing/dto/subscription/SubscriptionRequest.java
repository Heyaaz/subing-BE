package com.project.subing.dto.subscription;

import com.project.subing.domain.common.BillingCycle;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

@Getter
public class SubscriptionRequest {
    
    @NotNull(message = "서비스 ID는 필수입니다.")
    private Long serviceId;
    
    @NotBlank(message = "플랜명은 필수입니다.")
    private String planName;
    
    @NotNull(message = "월 가격은 필수입니다.")
    @Positive(message = "월 가격은 양수여야 합니다.")
    private Integer monthlyPrice;
    
    @NotNull(message = "결제일은 필수입니다.")
    @Min(value = 1, message = "결제일은 1일 이상이어야 합니다.")
    @Max(value = 31, message = "결제일은 31일 이하여야 합니다.")
    private Integer billingDate;
    
    @NotNull(message = "결제 주기는 필수입니다.")
    private BillingCycle billingCycle;
    
    private String notes;
}
