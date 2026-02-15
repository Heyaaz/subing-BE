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
        name = "optimization_engine_config_audits",
        indexes = {
                @Index(name = "idx_opt_engine_cfg_audit_key_id", columnList = "config_key,id"),
                @Index(name = "idx_opt_engine_cfg_audit_id", columnList = "id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class OptimizationEngineConfigAudit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_key", nullable = false, length = 120)
    private String configKey;

    @Column(name = "before_value", length = 255)
    private String beforeValue;

    @Column(name = "after_value", length = 255)
    private String afterValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 40)
    private OptimizationEngineConfigActionType actionType;

    @Column(name = "changed_by_user_id")
    private Long changedByUserId;
}
