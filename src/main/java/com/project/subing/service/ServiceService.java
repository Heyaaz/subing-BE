package com.project.subing.service;

import com.project.subing.domain.common.ServiceCategory;
import com.project.subing.domain.service.entity.ServiceEntity;
import com.project.subing.domain.service.entity.SubscriptionPlan;
import com.project.subing.dto.service.*;
import com.project.subing.dto.service.ServiceComparisonResponse.ComparisonSummary;
import com.project.subing.exception.business.MissingServicesException;
import com.project.subing.exception.entity.ServiceNotFoundException;
import com.project.subing.repository.ServiceRepository;
import com.project.subing.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServiceService {
    
    private final ServiceRepository serviceRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    
    public List<ServiceResponse> getAllServices() {
        List<ServiceEntity> services = serviceRepository.findAll();
        return convertEntitiesToDtos(services);
    }
    
    public ServiceResponse getServiceById(Long serviceId) {
        ServiceEntity service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ServiceNotFoundException(serviceId));
        return convertEntitiesToDtos(List.of(service)).get(0);
    }
    
    public List<ServiceResponse> getServicesByCategory(String category) {
        ServiceCategory serviceCategory = ServiceCategory.valueOf(category.toUpperCase());
        List<ServiceEntity> services = serviceRepository.findByCategory(serviceCategory);
        return convertEntitiesToDtos(services);
    }
    
    public ServiceComparisonResponse compareServices(ServiceComparisonRequest request) {
        List<ServiceEntity> services = serviceRepository.findAllById(request.getServiceIds());

        if (services.size() != request.getServiceIds().size()) {
            List<Long> foundIds = services.stream().map(ServiceEntity::getId).toList();
            List<Long> missingIds = request.getServiceIds().stream()
                    .filter(id -> !foundIds.contains(id))
                    .toList();
            throw new MissingServicesException(missingIds);
        }

        Map<Long, Integer> requestOrder = new LinkedHashMap<>();
        for (int i = 0; i < request.getServiceIds().size(); i++) {
            requestOrder.put(request.getServiceIds().get(i), i);
        }

        services.sort(Comparator.comparingInt(service -> requestOrder.getOrDefault(service.getId(), Integer.MAX_VALUE)));
        List<ServiceResponse> serviceResponses = convertEntitiesToDtos(services);

        ComparisonSummary summary = calculateComparisonSummary(serviceResponses);

        return ServiceComparisonResponse.builder()
                .services(serviceResponses)
                .summary(summary)
                .build();
    }
    
    private List<ServiceResponse> convertEntitiesToDtos(List<ServiceEntity> serviceEntities) {
        if (serviceEntities.isEmpty()) {
            return List.of();
        }

        List<Long> serviceIds = serviceEntities.stream()
                .map(ServiceEntity::getId)
                .toList();

        Map<Long, List<SubscriptionPlanResponse>> plansByServiceId = subscriptionPlanRepository.findByServiceIdIn(serviceIds)
                .stream()
                .collect(Collectors.groupingBy(
                        plan -> plan.getService().getId(),
                        Collectors.mapping(this::convertPlanEntityToDto, Collectors.toList())
                ));

        List<ServiceResponse> responses = new ArrayList<>();
        for (ServiceEntity serviceEntity : serviceEntities) {
            responses.add(ServiceResponse.builder()
                    .id(serviceEntity.getId())
                    .name(serviceEntity.getServiceName())
                    .description(serviceEntity.getDescription())
                    .category(serviceEntity.getCategory())
                    .website(serviceEntity.getOfficialUrl())
                    .logoUrl(serviceEntity.getIconUrl())
                    .createdAt(serviceEntity.getCreatedAt())
                    .plans(plansByServiceId.getOrDefault(serviceEntity.getId(), List.of()))
                    .build());
        }

        return responses;
    }
    
    private SubscriptionPlanResponse convertPlanEntityToDto(SubscriptionPlan plan) {
        return SubscriptionPlanResponse.builder()
                .id(plan.getId())
                .planName(plan.getPlanName())
                .description(plan.getDescription())
                .monthlyPrice(plan.getMonthlyPrice())
                .features(plan.getFeatures())
                .isPopular(plan.getIsPopular())
                .createdAt(plan.getCreatedAt())
                .build();
    }
    
    private ComparisonSummary calculateComparisonSummary(List<ServiceResponse> services) {
        List<Integer> allPrices = new ArrayList<>();
        
        for (ServiceResponse service : services) {
            for (SubscriptionPlanResponse plan : service.getPlans()) {
                allPrices.add(plan.getMonthlyPrice());
            }
        }
        
        if (allPrices.isEmpty()) {
            return ComparisonSummary.builder()
                    .minPrice(0)
                    .maxPrice(0)
                    .avgPrice(0)
                    .mostPopularService("없음")
                    .bestValueService("없음")
                    .build();
        }
        
        int minPrice = Integer.MAX_VALUE;
        int maxPrice = 0;
        int totalPrice = 0;
        
        for (Integer price : allPrices) {
            if (price < minPrice) minPrice = price;
            if (price > maxPrice) maxPrice = price;
            totalPrice += price;
        }
        
        int avgPrice = totalPrice / allPrices.size();
        
        // 가장 인기 있는 서비스 (인기 플랜이 많은 서비스)
        String mostPopularService = "없음";
        int maxPopularCount = 0;
        
        for (ServiceResponse service : services) {
            int popularCount = 0;
            for (SubscriptionPlanResponse plan : service.getPlans()) {
                if (plan.getIsPopular()) {
                    popularCount++;
                }
            }
            if (popularCount > maxPopularCount) {
                maxPopularCount = popularCount;
                mostPopularService = service.getName();
            }
        }
        
        // 최고 가성비 서비스 (가장 저렴한 인기 플랜을 가진 서비스)
        String bestValueService = "없음";
        int minPopularPrice = Integer.MAX_VALUE;
        
        for (ServiceResponse service : services) {
            for (SubscriptionPlanResponse plan : service.getPlans()) {
                if (plan.getIsPopular() && plan.getMonthlyPrice() < minPopularPrice) {
                    minPopularPrice = plan.getMonthlyPrice();
                    bestValueService = service.getName();
                }
            }
        }
        
        return ComparisonSummary.builder()
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .avgPrice(avgPrice)
                .mostPopularService(mostPopularService)
                .bestValueService(bestValueService)
                .build();
    }

    // ========== 관리자 전용 메서드 ==========

    @Transactional
    public ServiceResponse createService(ServiceCreateRequest request) {
        ServiceEntity service = ServiceEntity.builder()
                .serviceName(request.getServiceName())
                .category(request.getCategory())
                .iconUrl(request.getIconUrl())
                .officialUrl(request.getOfficialUrl())
                .description(request.getDescription())
                .build();

        ServiceEntity savedService = serviceRepository.save(service);
        log.info("새 서비스 생성됨: {}", savedService.getId());

        return convertEntitiesToDtos(List.of(savedService)).get(0);
    }

    @Transactional
    public ServiceResponse updateService(Long serviceId, ServiceUpdateRequest request) {
        ServiceEntity service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ServiceNotFoundException(serviceId));

        service.updateInfo(
            request.getServiceName(),
            request.getCategory(),
            request.getIconUrl(),
            request.getOfficialUrl(),
            request.getDescription(),
            request.getIsActive()
        );

        log.info("서비스 업데이트됨: {}", serviceId);

        return convertEntitiesToDtos(List.of(service)).get(0);
    }

    @Transactional
    public void deleteService(Long serviceId) {
        ServiceEntity service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ServiceNotFoundException(serviceId));

        serviceRepository.delete(service);
        log.info("서비스 삭제됨: {}", serviceId);
    }
}