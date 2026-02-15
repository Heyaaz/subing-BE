package com.project.subing.repository;

import com.project.subing.domain.common.ServiceCategory;
import com.project.subing.domain.service.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {

    List<SubscriptionPlan> findByServiceId(Long serviceId);

    List<SubscriptionPlan> findByServiceIdAndIsPopularTrue(Long serviceId);

    // 최적화 분석용: 해당 카테고리들의 모든 플랜을 Service와 함께 일괄 조회 (N+1 방지)
    @Query("SELECT sp FROM SubscriptionPlan sp " +
           "JOIN FETCH sp.service s " +
           "WHERE s.category IN :categories")
    List<SubscriptionPlan> findByServiceCategoryIn(@Param("categories") Collection<ServiceCategory> categories);
}
