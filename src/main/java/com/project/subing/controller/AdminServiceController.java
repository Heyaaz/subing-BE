package com.project.subing.controller;

import com.project.subing.dto.service.ServiceCreateRequest;
import com.project.subing.dto.service.ServiceResponse;
import com.project.subing.dto.service.ServiceUpdateRequest;
import com.project.subing.service.ServiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/services")
@RequiredArgsConstructor
public class AdminServiceController {

    private final ServiceService serviceService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ServiceResponse>> getAllServices() {
        List<ServiceResponse> services = serviceService.getAllServices();
        return ResponseEntity.ok(services);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceResponse> getServiceById(@PathVariable Long id) {
        ServiceResponse service = serviceService.getServiceById(id);
        return ResponseEntity.ok(service);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceResponse> createService(@RequestBody ServiceCreateRequest request) {
        ServiceResponse createdService = serviceService.createService(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdService);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceResponse> updateService(
            @PathVariable Long id,
            @RequestBody ServiceUpdateRequest request) {
        ServiceResponse updatedService = serviceService.updateService(id, request);
        return ResponseEntity.ok(updatedService);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteService(@PathVariable Long id) {
        serviceService.deleteService(id);
        return ResponseEntity.noContent().build();
    }
}