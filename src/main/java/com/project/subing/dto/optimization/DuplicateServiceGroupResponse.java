package com.project.subing.dto.optimization;

import com.project.subing.domain.common.ServiceCategory;
import com.project.subing.dto.subscription.SubscriptionResponse;
import com.project.subing.service.SubscriptionOptimizationService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DuplicateServiceGroupResponse {

    private String category;
    private String categoryDescription;
    private List<SubscriptionResponse> subscriptions;
    private Integer totalCost;
    private String message;

    public static DuplicateServiceGroupResponse from(SubscriptionOptimizationService.DuplicateServiceGroup group) {
        ServiceCategory category = group.getCategory();

        return DuplicateServiceGroupResponse.builder()
                .category(category.name())
                .categoryDescription(category.getDescription())
                .subscriptions(group.getSubscriptions().stream()
                        .map(SubscriptionResponse::from)
                        .collect(Collectors.toList()))
                .totalCost(group.getTotalCost())
                .message(String.format("%s 카테고리에서 %d개의 구독을 사용 중입니다. 하나로 통합하는 것을 고려해보세요.",
                        category.getDescription(), group.getSubscriptions().size()))
                .build();
    }
}
