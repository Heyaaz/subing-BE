package com.project.subing.dto.subscription;

import com.project.subing.domain.common.BillingCycle;
import com.project.subing.domain.common.Currency;
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

    /** 요금제명 (선택) */
    private String planName;
    
    @NotNull(message = "월 가격은 필수입니다.")
    @Positive(message = "월 가격은 양수여야 합니다.")
    private Integer monthlyPrice;
    
    /** 원(KRW) 또는 달러(USD). 없으면 KRW */
    private Currency currency;
    
    @NotNull(message = "결제일은 필수입니다.")
    @Min(value = 1, message = "결제일은 1일 이상이어야 합니다.")
    @Max(value = 31, message = "결제일은 31일 이하여야 합니다.")
    private Integer billingDate;
    
    @NotNull(message = "결제 주기는 필수입니다.")
    private BillingCycle billingCycle;

    @NotBlank(message = "시작월은 필수입니다.")
    private String startedAt;
    
    private String notes;
}
