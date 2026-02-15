package com.project.subing.dto.optimization;

import com.project.subing.domain.service.entity.ServiceEntity;
import com.project.subing.dto.subscription.SubscriptionResponse;
import com.project.subing.service.SubscriptionOptimizationService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

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
    private Integer switchCost;
    private Integer netSavings;
    private Integer confidence;
    private List<String> reasonCodes;
    private String message;

    public static CheaperAlternativeResponse from(SubscriptionOptimizationService.CheaperAlternative alternative) {
        ServiceEntity altService = alternative.getAlternativeService();
        boolean sameService = alternative.isSameService();

        String message;
        if (sameService) {
            message = String.format("%s의 플랜을 %s(으)로 다운그레이드하면 월 순절감 %,d원 예상됩니다.",
                    altService.getServiceName(),
                    alternative.getAlternativePlan().getPlanName(),
                    alternative.getNetSavings());
        } else {
            message = String.format("%s을(를) %s(%s)로 변경하면 전환비용 반영 후 월 순절감 %,d원 예상됩니다.",
                    alternative.getCurrentSubscription().getService().getServiceName(),
                    altService.getServiceName(),
                    alternative.getAlternativePlan().getPlanName(),
                    alternative.getNetSavings());
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
                .switchCost(alternative.getSwitchCost())
                .netSavings(alternative.getNetSavings())
                .confidence(alternative.getConfidenceScore())
                .reasonCodes(alternative.getReasonCodes())
                .message(message)
                .build();
    }
}
