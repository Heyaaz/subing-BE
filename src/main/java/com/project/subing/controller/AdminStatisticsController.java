package com.project.subing.controller;

import com.project.subing.dto.admin.AdminStatisticsResponse;
import com.project.subing.service.AdminStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/statistics")
@RequiredArgsConstructor
public class AdminStatisticsController {

    private final AdminStatisticsService statisticsService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminStatisticsResponse> getStatistics() {
        AdminStatisticsResponse statistics = statisticsService.getAdminStatistics();
        return ResponseEntity.ok(statistics);
    }
}