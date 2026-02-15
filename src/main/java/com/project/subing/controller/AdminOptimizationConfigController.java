package com.project.subing.controller;

import com.project.subing.dto.common.ApiResponse;
import com.project.subing.dto.optimization.OptimizationEngineConfigAuditResponse;
import com.project.subing.dto.optimization.OptimizationEngineConfigResponse;
import com.project.subing.dto.optimization.OptimizationEngineConfigUpdateRequest;
import com.project.subing.dto.optimization.OptimizationEnginePolicyResponse;
import com.project.subing.service.OptimizationEngineConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/optimization-config")
@RequiredArgsConstructor
public class AdminOptimizationConfigController {

    private final OptimizationEngineConfigService optimizationEngineConfigService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OptimizationEngineConfigResponse>> getOptimizationConfig() {
        return ResponseEntity.ok(ApiResponse.success(buildResponse(), "최적화 정책을 조회했습니다."));
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OptimizationEngineConfigResponse>> updateOptimizationConfig(
            @AuthenticationPrincipal Long adminUserId,
            @Valid @RequestBody OptimizationEngineConfigUpdateRequest request) {
        optimizationEngineConfigService.applyOverrides(request.getOverrides(), adminUserId);
        return ResponseEntity.ok(ApiResponse.success(buildResponse(), "최적화 정책을 업데이트했습니다."));
    }

    @PostMapping("/rollback")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OptimizationEngineConfigResponse>> rollbackOptimizationConfig(
            @AuthenticationPrincipal Long adminUserId) {
        optimizationEngineConfigService.rollbackAllOverrides(adminUserId);
        return ResponseEntity.ok(ApiResponse.success(buildResponse(), "최적화 정책을 기본값으로 롤백했습니다."));
    }

    @PostMapping("/rollback/{configKey:.+}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OptimizationEngineConfigResponse>> rollbackOptimizationConfigByKey(
            @PathVariable String configKey,
            @AuthenticationPrincipal Long adminUserId) {
        optimizationEngineConfigService.rollbackOverrideByKey(configKey, adminUserId);
        return ResponseEntity.ok(ApiResponse.success(buildResponse(), "최적화 정책 키를 이전 단계로 롤백했습니다."));
    }

    @PostMapping("/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OptimizationEngineConfigResponse>> refreshOptimizationConfigCache() {
        optimizationEngineConfigService.refreshCache();
        return ResponseEntity.ok(ApiResponse.success(buildResponse(), "최적화 정책 캐시를 새로고침했습니다."));
    }

    @GetMapping("/audits")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<OptimizationEngineConfigAuditResponse>>> getOptimizationConfigAudits(
            @RequestParam(required = false) String configKey,
            @RequestParam(defaultValue = "30") Integer limit) {
        List<OptimizationEngineConfigAuditResponse> audits = optimizationEngineConfigService
                .getRecentAudits(limit, configKey)
                .stream()
                .map(OptimizationEngineConfigAuditResponse::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(audits, "최적화 정책 변경 이력을 조회했습니다."));
    }

    private OptimizationEngineConfigResponse buildResponse() {
        return OptimizationEngineConfigResponse.builder()
                .defaultPolicy(OptimizationEnginePolicyResponse.from(optimizationEngineConfigService.getDefaultPolicy()))
                .effectivePolicy(OptimizationEnginePolicyResponse.from(optimizationEngineConfigService.getEffectivePolicy()))
                .activeOverrides(optimizationEngineConfigService.getActiveOverrides())
                .cacheLoadedAtEpochMs(optimizationEngineConfigService.getCacheLoadedAtEpochMs())
                .recentAudits(optimizationEngineConfigService.getRecentAudits(30, null).stream()
                        .map(OptimizationEngineConfigAuditResponse::from)
                        .toList())
                .build();
    }
}
