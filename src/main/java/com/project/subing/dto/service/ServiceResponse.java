package com.project.subing.dto.service;

import com.project.subing.domain.common.ServiceCategory;
import com.project.subing.domain.service.entity.ServiceEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceResponse {
    private Long id;
    private String name;
    private String description;
    private ServiceCategory category;
    private String website;
    private String logoUrl;
    private LocalDateTime createdAt;
    private List<SubscriptionPlanResponse> plans;

    public static ServiceResponse from(ServiceEntity service) {
        return ServiceResponse.builder()
                .id(service.getId())
                .name(service.getServiceName())
                .description(service.getDescription())
                .category(service.getCategory())
                .website(service.getOfficialUrl())
                .logoUrl(service.getIconUrl())
                .createdAt(service.getCreatedAt())
                .build();
    }
}
