package com.project.subing.dto.optimization;

import com.project.subing.domain.service.entity.ServiceEntity;
import com.project.subing.dto.subscription.SubscriptionResponse;
import com.project.subing.service.SubscriptionOptimizationService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheaperAlternativeResponse {

    private SubscriptionResponse currentSubscription;
    private Long alternativeServiceId;
    private String alternativeServiceName;
    private String alternativeServiceCategory;
    private String alternativeServiceUrl;
    private AlternativePlanInfo alternativePlan;
    private Integer currentPrice;
    private Integer alternativePrice;
    private Integer savings;
    private Boolean isSameService;
    private String suggestionType;
    private String message;

    public static CheaperAlternativeResponse from(SubscriptionOptimizationService.CheaperAlternative alternative) {
        ServiceEntity altService = alternative.getAlternativeService();
        boolean sameService = alternative.isSameService();

        String message;
        if (sameService) {
            message = String.format("%s의 플랜을 %s(으)로 다운그레이드하면 월 %,d원 절약할 수 있습니다.",
                    altService.getServiceName(),
                    alternative.getAlternativePlan().getPlanName(),
                    alternative.getSavings());
        } else {
            message = String.format("%s을(를) %s(%s)로 변경하면 월 %,d원 절약할 수 있습니다.",
                    alternative.getCurrentSubscription().getService().getServiceName(),
                    altService.getServiceName(),
                    alternative.getAlternativePlan().getPlanName(),
                    alternative.getSavings());
        }

        return CheaperAlternativeResponse.builder()
                .currentSubscription(SubscriptionResponse.from(alternative.getCurrentSubscription()))
                .alternativeServiceId(altService.getId())
                .alternativeServiceName(altService.getServiceName())
                .alternativeServiceCategory(altService.getCategory().getDescription())
                .alternativeServiceUrl(altService.getOfficialUrl())
                .alternativePlan(AlternativePlanInfo.from(alternative.getAlternativePlan()))
                .currentPrice(alternative.getCurrentPrice())
                .alternativePrice(alternative.getAlternativePrice())
                .savings(alternative.getSavings())
                .isSameService(sameService)
                .suggestionType(sameService ? "DOWNGRADE" : "SWITCH")
                .message(message)
                .build();
    }
}
