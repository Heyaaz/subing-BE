package com.project.subing.repository;

import com.project.subing.domain.common.ServiceCategory;
import com.project.subing.domain.service.entity.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRepository extends JpaRepository<ServiceEntity, Long> {
    
    List<ServiceEntity> findByCategory(ServiceCategory category);
    
    List<ServiceEntity> findByServiceNameContainingIgnoreCase(String serviceName);
}
