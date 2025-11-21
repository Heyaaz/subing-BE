package com.project.subing.controller;

import com.project.subing.dto.common.ApiResponse;
import com.project.subing.dto.service.ServiceComparisonRequest;
import com.project.subing.dto.service.ServiceComparisonResponse;
import com.project.subing.dto.service.ServiceResponse;
import com.project.subing.service.ServiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/services")
@RequiredArgsConstructor
public class ServiceController {
    
    private final ServiceService serviceService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<ServiceResponse>>> getAllServices() {
        List<ServiceResponse> services = serviceService.getAllServices();
        return ResponseEntity.ok(ApiResponse.success(services, "서비스 목록을 조회했습니다."));
    }
    
    @GetMapping("/{serviceId}")
    public ResponseEntity<ApiResponse<ServiceResponse>> getServiceById(@PathVariable Long serviceId) {
        ServiceResponse service = serviceService.getServiceById(serviceId);
        return ResponseEntity.ok(ApiResponse.success(service, "서비스 정보를 조회했습니다."));
    }
    
    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<List<ServiceResponse>>> getServicesByCategory(@PathVariable String category) {
        List<ServiceResponse> services = serviceService.getServicesByCategory(category);
        return ResponseEntity.ok(ApiResponse.success(services, "카테고리별 서비스 목록을 조회했습니다."));
    }
    
    @PostMapping("/compare")
    public ResponseEntity<ApiResponse<ServiceComparisonResponse>> compareServices(@Valid @RequestBody ServiceComparisonRequest request) {
        ServiceComparisonResponse comparison = serviceService.compareServices(request);
        return ResponseEntity.ok(ApiResponse.success(comparison, "서비스 비교 결과를 조회했습니다."));
    }
}
