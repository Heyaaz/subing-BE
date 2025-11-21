package com.project.subing.dto.service;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.List;

@Getter
public class ServiceComparisonRequest {
    
    @NotEmpty(message = "비교할 서비스 ID 목록이 필요합니다")
    @Size(min = 2, max = 5, message = "2개 이상 5개 이하의 서비스를 비교할 수 있습니다")
    private List<Long> serviceIds;
}
