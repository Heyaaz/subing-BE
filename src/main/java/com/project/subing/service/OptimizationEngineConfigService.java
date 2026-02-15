package com.project.subing.service;

import com.project.subing.config.OptimizationEngineProperties;
import com.project.subing.domain.optimization.entity.OptimizationEngineConfig;
import com.project.subing.domain.optimization.entity.OptimizationEngineConfigActionType;
import com.project.subing.domain.optimization.entity.OptimizationEngineConfigAudit;
import com.project.subing.repository.OptimizationEngineConfigAuditRepository;
import com.project.subing.repository.OptimizationEngineConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class OptimizationEngineConfigService {

    private static final Set<String> ALLOWED_OVERRIDE_KEYS = Set.of(
            "pricing.yearlyDivisor",
            "pricing.sameServiceSwitchCost",
            "pricing.crossServiceBaseSwitchCost",
            "pricing.yearlyBillingPenalty",
            "pricing.crossCategoryPenalty",
            "portfolio.maxChangesPerRun",
            "performance.topKPlansPerService",
            "performance.candidateSearchTimeoutMs",
            "performance.portfolioOptimizeTimeoutMs",
            "performance.runtimeCacheTtlMs",
            "tracking.enabled"
    );

    private static final int DEFAULT_AUDIT_LIMIT = 30;
    private static final int MIN_AUDIT_LIMIT = 1;
    private static final int MAX_AUDIT_LIMIT = 200;
    private static final Set<OptimizationEngineConfigActionType> ROLLBACK_SOURCE_ACTIONS = EnumSet.of(
            OptimizationEngineConfigActionType.UPSERT,
            OptimizationEngineConfigActionType.DEACTIVATE,
            OptimizationEngineConfigActionType.ROLLBACK_ALL
    );

    private final OptimizationEngineConfigRepository optimizationEngineConfigRepository;
    private final OptimizationEngineConfigAuditRepository optimizationEngineConfigAuditRepository;
    private final OptimizationEngineProperties optimizationEngineProperties;

    private volatile CachedPolicySnapshot cache;

    public OptimizationEnginePolicy getEffectivePolicy() {
        long now = System.currentTimeMillis();
        CachedPolicySnapshot local = cache;
        OptimizationEnginePolicy defaultPolicy = getDefaultPolicy();
        int cacheTtl = Math.max(1000, defaultPolicy.getRuntimeCacheTtlMs());

        if (local != null && now - local.loadedAtEpochMs < cacheTtl) {
            return local.policy;
        }

        synchronized (this) {
            local = cache;
            if (local != null && now - local.loadedAtEpochMs < cacheTtl) {
                return local.policy;
            }

            try {
                return reloadCacheInternal(now).policy;
            } catch (Exception e) {
                log.error("최적화 정책 캐시 로드 실패. 캐시 또는 기본 정책 fallback 적용", e);
                if (local != null) {
                    return local.policy;
                }
                return defaultPolicy;
            }
        }
    }

    public OptimizationEnginePolicy getDefaultPolicy() {
        return fromProperties(optimizationEngineProperties);
    }

    public Map<String, String> getActiveOverrides() {
        return Collections.unmodifiableMap(getCachedSnapshot().activeOverrides);
    }

    public long getCacheLoadedAtEpochMs() {
        return getCachedSnapshot().loadedAtEpochMs;
    }

    public List<OptimizationEngineConfigAudit> getRecentAudits(Integer limit, String configKey) {
        int safeLimit = normalizeLimit(limit);
        if (configKey == null || configKey.isBlank()) {
            return optimizationEngineConfigAuditRepository.findAllByOrderByIdDesc(PageRequest.of(0, safeLimit)).getContent();
        }

        String normalizedKey = normalizeKey(configKey);
        validateOverrideKeys(Set.of(normalizedKey));
        return optimizationEngineConfigAuditRepository
                .findByConfigKeyOrderByIdDesc(normalizedKey, PageRequest.of(0, safeLimit))
                .getContent();
    }

    @Transactional
    public OptimizationEnginePolicy applyOverrides(Map<String, String> overrides, Long changedByUserId) {
        if (overrides == null || overrides.isEmpty()) {
            throw new IllegalArgumentException("변경할 override 항목이 없습니다.");
        }

        Map<String, String> normalizedOverrides = normalizeOverrides(overrides);
        validateOverrideKeys(normalizedOverrides.keySet());

        boolean hasChanged = false;

        for (Map.Entry<String, String> entry : normalizedOverrides.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            OptimizationEngineConfig existingConfig = optimizationEngineConfigRepository.findByConfigKey(key).orElse(null);
            String beforeValue = activeValue(existingConfig);

            if (value == null || value.isBlank()) {
                if (beforeValue == null) {
                    continue;
                }
                existingConfig.deactivate();
                writeAudit(key, beforeValue, null, OptimizationEngineConfigActionType.DEACTIVATE, changedByUserId);
                hasChanged = true;
                continue;
            }

            String trimmedValue = value.trim();
            validateValue(key, trimmedValue);

            if (Objects.equals(beforeValue, trimmedValue)) {
                continue;
            }

            OptimizationEngineConfig target = existingConfig;
            if (target == null) {
                target = OptimizationEngineConfig.builder()
                        .configKey(key)
                        .configValue(trimmedValue)
                        .isActive(true)
                        .build();
            }

            target.updateValue(trimmedValue);
            target.activate();
            optimizationEngineConfigRepository.save(target);

            writeAudit(key, beforeValue, trimmedValue, OptimizationEngineConfigActionType.UPSERT, changedByUserId);
            hasChanged = true;
        }

        if (!hasChanged) {
            return getEffectivePolicy();
        }
        return forceReloadCache().policy;
    }

    @Transactional
    public OptimizationEnginePolicy rollbackAllOverrides(Long changedByUserId) {
        List<OptimizationEngineConfig> activeConfigs = optimizationEngineConfigRepository.findByIsActiveTrue();
        if (activeConfigs.isEmpty()) {
            return getEffectivePolicy();
        }

        for (OptimizationEngineConfig config : activeConfigs) {
            String beforeValue = config.getConfigValue();
            config.deactivate();
            writeAudit(
                    config.getConfigKey(),
                    beforeValue,
                    null,
                    OptimizationEngineConfigActionType.ROLLBACK_ALL,
                    changedByUserId
            );
        }

        return forceReloadCache().policy;
    }

    @Transactional
    public OptimizationEnginePolicy rollbackOverrideByKey(String configKey, Long changedByUserId) {
        String normalizedKey = normalizeKey(configKey);
        validateOverrideKeys(Set.of(normalizedKey));

        OptimizationEngineConfig existingConfig = optimizationEngineConfigRepository.findByConfigKey(normalizedKey).orElse(null);
        String currentValue = activeValue(existingConfig);
        String rollbackTargetValue = resolveRollbackTargetValue(normalizedKey, currentValue);

        if (Objects.equals(currentValue, rollbackTargetValue)) {
            throw new IllegalArgumentException("해당 키는 이전 단계로 롤백할 상태가 없습니다: " + normalizedKey);
        }

        if (rollbackTargetValue == null) {
            if (existingConfig != null) {
                existingConfig.deactivate();
            }
        } else {
            OptimizationEngineConfig target = existingConfig;
            if (target == null) {
                target = OptimizationEngineConfig.builder()
                        .configKey(normalizedKey)
                        .configValue(rollbackTargetValue)
                        .isActive(true)
                        .build();
            }

            target.updateValue(rollbackTargetValue);
            target.activate();
            optimizationEngineConfigRepository.save(target);
        }

        writeAudit(
                normalizedKey,
                currentValue,
                rollbackTargetValue,
                OptimizationEngineConfigActionType.ROLLBACK_KEY,
                changedByUserId
        );

        return forceReloadCache().policy;
    }

    private String resolveRollbackTargetValue(String configKey, String currentValue) {
        List<OptimizationEngineConfigAudit> history = optimizationEngineConfigAuditRepository.findByConfigKeyOrderByIdDesc(configKey);

        for (OptimizationEngineConfigAudit audit : history) {
            if (!ROLLBACK_SOURCE_ACTIONS.contains(audit.getActionType())) {
                continue;
            }
            if (Objects.equals(audit.getAfterValue(), currentValue)) {
                return audit.getBeforeValue();
            }
        }

        if (currentValue != null) {
            // Legacy fallback: override row exists but audit history is missing.
            // In this case, rollback deactivates the override and returns to default policy.
            return null;
        }

        throw new IllegalArgumentException("해당 키의 롤백 이력이 없습니다: " + configKey);
    }

    @Transactional
    public OptimizationEnginePolicy refreshCache() {
        return forceReloadCache().policy;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_AUDIT_LIMIT;
        }
        return Math.max(MIN_AUDIT_LIMIT, Math.min(MAX_AUDIT_LIMIT, limit));
    }

    private Map<String, String> normalizeOverrides(Map<String, String> overrides) {
        Map<String, String> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : overrides.entrySet()) {
            normalized.put(normalizeKey(entry.getKey()), entry.getValue());
        }
        return normalized;
    }

    private String normalizeKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("config key는 비어 있을 수 없습니다.");
        }
        return key.trim();
    }

    private String activeValue(OptimizationEngineConfig config) {
        if (config == null || !Boolean.TRUE.equals(config.getIsActive())) {
            return null;
        }
        return config.getConfigValue();
    }

    private void writeAudit(
            String configKey,
            String beforeValue,
            String afterValue,
            OptimizationEngineConfigActionType actionType,
            Long changedByUserId
    ) {
        optimizationEngineConfigAuditRepository.save(
                OptimizationEngineConfigAudit.builder()
                        .configKey(configKey)
                        .beforeValue(beforeValue)
                        .afterValue(afterValue)
                        .actionType(actionType)
                        .changedByUserId(changedByUserId)
                        .build()
        );
    }

    private CachedPolicySnapshot getCachedSnapshot() {
        CachedPolicySnapshot local = cache;
        if (local != null) {
            return local;
        }
        return forceReloadCache();
    }

    private CachedPolicySnapshot forceReloadCache() {
        synchronized (this) {
            return reloadCacheInternal(System.currentTimeMillis());
        }
    }

    private CachedPolicySnapshot reloadCacheInternal(long loadedAtEpochMs) {
        Map<String, String> activeOverrides = optimizationEngineConfigRepository.findByIsActiveTrue().stream()
                .collect(
                        LinkedHashMap::new,
                        (map, config) -> map.put(config.getConfigKey(), config.getConfigValue()),
                        Map::putAll
                );

        OptimizationEnginePolicy policy = mergeOverrides(getDefaultPolicy(), activeOverrides);
        CachedPolicySnapshot snapshot = new CachedPolicySnapshot(policy, activeOverrides, loadedAtEpochMs);
        cache = snapshot;
        return snapshot;
    }

    private OptimizationEnginePolicy mergeOverrides(OptimizationEnginePolicy defaultPolicy, Map<String, String> overrides) {
        MutablePolicy mutable = new MutablePolicy(defaultPolicy);

        applyIntOverride(overrides, "pricing.yearlyDivisor", value -> mutable.yearlyDivisor = value, 1, Integer.MAX_VALUE);
        applyIntOverride(overrides, "pricing.sameServiceSwitchCost", value -> mutable.sameServiceSwitchCost = value, 0, Integer.MAX_VALUE);
        applyIntOverride(overrides, "pricing.crossServiceBaseSwitchCost", value -> mutable.crossServiceBaseSwitchCost = value, 0, Integer.MAX_VALUE);
        applyIntOverride(overrides, "pricing.yearlyBillingPenalty", value -> mutable.yearlyBillingPenalty = value, 0, Integer.MAX_VALUE);
        applyIntOverride(overrides, "pricing.crossCategoryPenalty", value -> mutable.crossCategoryPenalty = value, 0, Integer.MAX_VALUE);

        applyIntOverride(overrides, "portfolio.maxChangesPerRun", value -> mutable.maxChangesPerRun = value, 1, 100);

        applyIntOverride(overrides, "performance.topKPlansPerService", value -> mutable.topKPlansPerService = value, 1, 1000);
        applyIntOverride(overrides, "performance.candidateSearchTimeoutMs", value -> mutable.candidateSearchTimeoutMs = value, 1, 10_000);
        applyIntOverride(overrides, "performance.portfolioOptimizeTimeoutMs", value -> mutable.portfolioOptimizeTimeoutMs = value, 1, 10_000);
        applyIntOverride(overrides, "performance.runtimeCacheTtlMs", value -> mutable.runtimeCacheTtlMs = value, 1_000, 3_600_000);

        if (overrides.containsKey("tracking.enabled")) {
            mutable.trackingEnabled = parseBoolean(overrides.get("tracking.enabled"), "tracking.enabled");
        }

        return mutable.toPolicy();
    }

    private void applyIntOverride(Map<String, String> overrides, String key, Consumer<Integer> consumer, int min, int max) {
        String value = overrides.get(key);
        if (value == null) return;
        int parsed = parseInt(value, key);
        if (parsed < min || parsed > max) {
            throw new IllegalArgumentException(String.format("%s 값은 %d~%d 범위여야 합니다.", key, min, max));
        }
        consumer.accept(parsed);
    }

    private int parseInt(String value, String key) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " 값은 숫자여야 합니다.");
        }
    }

    private boolean parseBoolean(String value, String key) {
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        throw new IllegalArgumentException(key + " 값은 true/false 여야 합니다.");
    }

    private void validateOverrideKeys(Set<String> keys) {
        for (String key : keys) {
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("config key는 비어 있을 수 없습니다.");
            }
            if (!ALLOWED_OVERRIDE_KEYS.contains(key)) {
                throw new IllegalArgumentException("지원하지 않는 optimization config key입니다: " + key);
            }
        }
    }

    private void validateValue(String key, String value) {
        Map<String, String> probe = Map.of(key, value);
        mergeOverrides(getDefaultPolicy(), probe);
    }

    private OptimizationEnginePolicy fromProperties(OptimizationEngineProperties properties) {
        return new OptimizationEnginePolicy(
                Math.max(1, properties.getPricing().getYearlyDivisor()),
                Math.max(0, properties.getPricing().getSameServiceSwitchCost()),
                Math.max(0, properties.getPricing().getCrossServiceBaseSwitchCost()),
                Math.max(0, properties.getPricing().getYearlyBillingPenalty()),
                Math.max(0, properties.getPricing().getCrossCategoryPenalty()),
                Math.max(1, properties.getPortfolio().getMaxChangesPerRun()),
                Math.max(1, properties.getPerformance().getTopKPlansPerService()),
                Math.max(1, properties.getPerformance().getCandidateSearchTimeoutMs()),
                Math.max(1, properties.getPerformance().getPortfolioOptimizeTimeoutMs()),
                Math.max(1000, properties.getPerformance().getRuntimeCacheTtlMs()),
                properties.getTracking().isEnabled()
        );
    }

    private static class MutablePolicy {
        private int yearlyDivisor;
        private int sameServiceSwitchCost;
        private int crossServiceBaseSwitchCost;
        private int yearlyBillingPenalty;
        private int crossCategoryPenalty;
        private int maxChangesPerRun;
        private int topKPlansPerService;
        private int candidateSearchTimeoutMs;
        private int portfolioOptimizeTimeoutMs;
        private int runtimeCacheTtlMs;
        private boolean trackingEnabled;

        private MutablePolicy(OptimizationEnginePolicy policy) {
            this.yearlyDivisor = policy.getYearlyDivisor();
            this.sameServiceSwitchCost = policy.getSameServiceSwitchCost();
            this.crossServiceBaseSwitchCost = policy.getCrossServiceBaseSwitchCost();
            this.yearlyBillingPenalty = policy.getYearlyBillingPenalty();
            this.crossCategoryPenalty = policy.getCrossCategoryPenalty();
            this.maxChangesPerRun = policy.getMaxChangesPerRun();
            this.topKPlansPerService = policy.getTopKPlansPerService();
            this.candidateSearchTimeoutMs = policy.getCandidateSearchTimeoutMs();
            this.portfolioOptimizeTimeoutMs = policy.getPortfolioOptimizeTimeoutMs();
            this.runtimeCacheTtlMs = policy.getRuntimeCacheTtlMs();
            this.trackingEnabled = policy.isTrackingEnabled();
        }

        private OptimizationEnginePolicy toPolicy() {
            return new OptimizationEnginePolicy(
                    yearlyDivisor,
                    sameServiceSwitchCost,
                    crossServiceBaseSwitchCost,
                    yearlyBillingPenalty,
                    crossCategoryPenalty,
                    maxChangesPerRun,
                    topKPlansPerService,
                    candidateSearchTimeoutMs,
                    portfolioOptimizeTimeoutMs,
                    runtimeCacheTtlMs,
                    trackingEnabled
            );
        }
    }

    private static class CachedPolicySnapshot {
        private final OptimizationEnginePolicy policy;
        private final Map<String, String> activeOverrides;
        private final long loadedAtEpochMs;

        private CachedPolicySnapshot(OptimizationEnginePolicy policy, Map<String, String> activeOverrides, long loadedAtEpochMs) {
            this.policy = policy;
            this.activeOverrides = Collections.unmodifiableMap(new LinkedHashMap<>(activeOverrides));
            this.loadedAtEpochMs = loadedAtEpochMs;
        }
    }
}
