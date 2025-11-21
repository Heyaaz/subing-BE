package com.project.subing.controller;

import com.project.subing.dto.service.PlanCreateRequest;
import com.project.subing.dto.service.PlanUpdateRequest;
import com.project.subing.dto.service.SubscriptionPlanResponse;
import com.project.subing.service.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/plans")
@RequiredArgsConstructor
public class AdminPlanController {

    private final PlanService planService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SubscriptionPlanResponse>> getAllPlans() {
        List<SubscriptionPlanResponse> plans = planService.getAllPlans();
        return ResponseEntity.ok(plans);
    }

    @GetMapping("/service/{serviceId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SubscriptionPlanResponse>> getPlansByServiceId(@PathVariable Long serviceId) {
        List<SubscriptionPlanResponse> plans = planService.getPlansByServiceId(serviceId);
        return ResponseEntity.ok(plans);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubscriptionPlanResponse> getPlanById(@PathVariable Long id) {
        SubscriptionPlanResponse plan = planService.getPlanById(id);
        return ResponseEntity.ok(plan);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubscriptionPlanResponse> createPlan(@RequestBody PlanCreateRequest request) {
        SubscriptionPlanResponse createdPlan = planService.createPlan(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPlan);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubscriptionPlanResponse> updatePlan(
            @PathVariable Long id,
            @RequestBody PlanUpdateRequest request) {
        SubscriptionPlanResponse updatedPlan = planService.updatePlan(id, request);
        return ResponseEntity.ok(updatedPlan);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePlan(@PathVariable Long id) {
        planService.deletePlan(id);
        return ResponseEntity.noContent().build();
    }
}