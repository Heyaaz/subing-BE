package com.project.subing.repository;

import com.project.subing.domain.optimization.entity.OptimizationEngineConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OptimizationEngineConfigRepository extends JpaRepository<OptimizationEngineConfig, Long> {

    Optional<OptimizationEngineConfig> findByConfigKey(String configKey);

    List<OptimizationEngineConfig> findByIsActiveTrue();

    @Modifying
    @Query("UPDATE OptimizationEngineConfig c SET c.isActive = false WHERE c.isActive = true")
    int deactivateAllActive();
}
