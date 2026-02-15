package com.project.subing.dto.optimization;

import com.project.subing.domain.optimization.entity.OptimizationEngineConfigAudit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptimizationEngineConfigAuditResponse {

    private Long id;
    private String configKey;
    private String beforeValue;
    private String afterValue;
    private String actionType;
    private Long changedByUserId;
    private LocalDateTime changedAt;

    public static OptimizationEngineConfigAuditResponse from(OptimizationEngineConfigAudit audit) {
        return OptimizationEngineConfigAuditResponse.builder()
                .id(audit.getId())
                .configKey(audit.getConfigKey())
                .beforeValue(audit.getBeforeValue())
                .afterValue(audit.getAfterValue())
                .actionType(audit.getActionType().name())
                .changedByUserId(audit.getChangedByUserId())
                .changedAt(audit.getCreatedAt())
                .build();
    }
}
