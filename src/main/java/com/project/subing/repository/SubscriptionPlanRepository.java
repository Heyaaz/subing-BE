package com.project.subing.repository;

import com.project.subing.domain.service.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {
    
    List<SubscriptionPlan> findByServiceId(Long serviceId);
    
    List<SubscriptionPlan> findByServiceIdAndIsPopularTrue(Long serviceId);
}
