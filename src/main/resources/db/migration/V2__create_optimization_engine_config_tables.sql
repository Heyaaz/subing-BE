-- Optimization policy runtime config + audit tables
-- 운영 배포 전 본 SQL이 적용되어야 합니다 (prod는 ddl-auto=validate).

CREATE TABLE IF NOT EXISTS optimization_engine_configs (
    id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(120) NOT NULL,
    config_value VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_optimization_engine_config_key
    ON optimization_engine_configs (config_key);

CREATE TABLE IF NOT EXISTS optimization_engine_config_audits (
    id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(120) NOT NULL,
    before_value VARCHAR(255),
    after_value VARCHAR(255),
    action_type VARCHAR(40) NOT NULL,
    changed_by_user_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_opt_engine_cfg_audit_key_id
    ON optimization_engine_config_audits (config_key, id);

CREATE INDEX IF NOT EXISTS idx_opt_engine_cfg_audit_id
    ON optimization_engine_config_audits (id);
