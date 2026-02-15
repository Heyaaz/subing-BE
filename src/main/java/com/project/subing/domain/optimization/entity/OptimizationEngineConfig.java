package com.project.subing.domain.optimization.entity;

import com.project.subing.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "optimization_engine_configs",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_optimization_engine_config_key", columnNames = {"config_key"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class OptimizationEngineConfig extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_key", nullable = false, length = 120)
    private String configKey;

    @Column(name = "config_value", nullable = false, length = 255)
    private String configValue;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    public void updateValue(String value) {
        this.configValue = value;
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }
}
