-- Soft Delete 구현을 위한 del_yn 컬럼 추가
-- 'N' = 활성 (기본값), 'Y' = 삭제됨

-- 1. users 테이블
ALTER TABLE users ADD COLUMN del_yn VARCHAR(1) NOT NULL DEFAULT 'N';

-- 2. budgets 테이블
ALTER TABLE budgets ADD COLUMN del_yn VARCHAR(1) NOT NULL DEFAULT 'N';

-- 3. user_subscriptions 테이블
ALTER TABLE user_subscriptions ADD COLUMN del_yn VARCHAR(1) NOT NULL DEFAULT 'N';

-- 4. service_reviews 테이블
ALTER TABLE service_reviews ADD COLUMN del_yn VARCHAR(1) NOT NULL DEFAULT 'N';

-- 5. services 테이블
ALTER TABLE services ADD COLUMN del_yn VARCHAR(1) NOT NULL DEFAULT 'N';

-- 6. subscription_plans 테이블
ALTER TABLE subscription_plans ADD COLUMN del_yn VARCHAR(1) NOT NULL DEFAULT 'N';

-- 7. user_preferences 테이블
ALTER TABLE user_preferences ADD COLUMN del_yn VARCHAR(1) NOT NULL DEFAULT 'N';

-- 인덱스 추가 (조회 성능 최적화)
CREATE INDEX idx_users_del_yn ON users(del_yn);
CREATE INDEX idx_budgets_del_yn ON budgets(del_yn);
CREATE INDEX idx_user_subscriptions_del_yn ON user_subscriptions(del_yn);
CREATE INDEX idx_service_reviews_del_yn ON service_reviews(del_yn);
CREATE INDEX idx_services_del_yn ON services(del_yn);
CREATE INDEX idx_subscription_plans_del_yn ON subscription_plans(del_yn);
CREATE INDEX idx_user_preferences_del_yn ON user_preferences(del_yn);
