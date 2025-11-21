package com.project.subing.dto.service;

import com.project.subing.domain.common.ServiceCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceUpdateRequest {
    private String serviceName;
    private ServiceCategory category;
    private String iconUrl;
    private String officialUrl;
    private String description;
    private Boolean isActive;
}