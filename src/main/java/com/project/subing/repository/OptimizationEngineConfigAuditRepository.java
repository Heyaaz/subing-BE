package com.project.subing.repository;

import com.project.subing.domain.optimization.entity.OptimizationEngineConfigAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OptimizationEngineConfigAuditRepository extends JpaRepository<OptimizationEngineConfigAudit, Long> {

    List<OptimizationEngineConfigAudit> findByConfigKeyOrderByIdDesc(String configKey);

    Page<OptimizationEngineConfigAudit> findAllByOrderByIdDesc(Pageable pageable);

    Page<OptimizationEngineConfigAudit> findByConfigKeyOrderByIdDesc(String configKey, Pageable pageable);
}
