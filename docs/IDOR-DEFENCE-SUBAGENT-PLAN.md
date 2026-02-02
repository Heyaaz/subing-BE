# IDOR 방어 적용 - Subagent 오케스트레이션 계획

DEFFENCE-IDOR.md 규칙을 Subagent 단위로 순차 적용하기 위한 계획입니다.

---

## 기준 문서

- **규칙:** `subing-backend/docs/DEFFENCE-IDOR.md`
- **원칙 요약**
  1. 내 데이터 API는 **userId를 클라이언트에서 받지 않음** → JWT principal만 사용 (/me 패턴).
  2. subscriptionId, budgetId, notificationId 등 **리소스 ID로 조작하는 API**는 **해당 리소스의 owner == principal** 검증 필수.

---

## 전체 워크플로우

```
Main Orchestrator (IDOR 방어 총괄)
    ↓
    ├─→ Subagent 1: Security Baseline (Principal 컨벤션)
    │   └─→ 출력: Principal 추출 방식 정리, /me 패턴 가이드
    │
    ├─→ Subagent 2: User & Statistics (userId 제거)
    │   └─→ 출력: UserController /me·admin 분리, StatisticsController principal
    │
    ├─→ Subagent 3: Subscription (principal + owner 검증)
    │   └─→ 출력: SubscriptionController userId 제거 + update/delete/toggle owner 체크
    │
    ├─→ Subagent 4: Budget & Preference (principal only)
    │   └─→ 출력: BudgetController, PreferenceController principal 전환
    │
    ├─→ Subagent 5: Recommendation (principal + 소유권 검증)
    │   └─→ 출력: RecommendationController userId 제거 + feedback/click owner 검증
    │
    ├─→ Subagent 6: Notification & Optimization (principal only)
    │   └─→ 출력: Notification·NotificationSetting·Optimization principal 전환
    │
    └─→ Subagent 7: Verification (체크리스트·회귀 검증)
        └─→ 출력: 수정 체크리스트 완료 여부, 회귀 테스트 결과
```

---

# Subagent 1: Security Baseline

**역할:** Principal 사용 컨벤션 및 /me 패턴 정리

## 담당 작업

1. Controller에서 principal(userId) 추출 방식 통일 (예: `@AuthenticationPrincipal` 또는 SecurityContext).
2. “userId는 클라이언트에서 받지 않는다” 규칙 문서화.
3. 리소스 소유권 검증 패턴(owner == principal) 가이드 한 줄 정리.

## 이전 Subagent로부터 받은 데이터

- 없음 (진입점).

## 실행 과정

1. **Principal 추출:** 기존 컨트롤러/서비스에서 `Long userId` 파라미터 사용처 목록 확인.
2. **공통 방식 결정:** 예) `Long userId = SecurityUtils.getCurrentUserId()` 또는 `@AuthenticationPrincipal UserPrincipal principal` → `principal.getUserId()`.
3. **문서 반영:** DEFFENCE-IDOR.md 상단 또는 별도 한 줄: “모든 내 데이터 API는 principal만 사용, /me 패턴 권장.”

## 다음 Subagent로 전달할 데이터

```json
{
  "principalExtraction": "SecurityUtils.getCurrentUserId() 또는 @AuthenticationPrincipal",
  "conventionDoc": "DEFFENCE-IDOR.md 또는 SECURITY-CONVENTION.md 참조"
}
```

## 사용자 확인 포인트

> 💬 "Principal 컨벤션이 정리되었습니다. 다음 단계(User·Statistics)로 진행할까요?"

---

# Subagent 2: User & Statistics

**역할:** UserController, StatisticsController에서 userId 제거 및 /me·admin 분리

## 담당 작업

1. **UserController**
   - `GET /api/v1/users/{userId}/tier-info` → `GET /api/v1/users/me/tier-info` (principal).
   - `PUT /api/v1/users/{userId}/upgrade-tier` → 일반 유저용은 제거, 관리자용만: `PUT /api/v1/admin/users/{userId}/tier` (AdminUserController 등).
2. **StatisticsController**
   - `GET /api/v1/statistics/monthly/{userId}` → `GET /api/v1/statistics/monthly` (principal).
   - `GET /api/v1/statistics/analysis/{userId}` → `GET /api/v1/statistics/analysis` (principal).

## 이전 Subagent로부터 받은 데이터

```json
{
  "principalExtraction": "SecurityUtils.getCurrentUserId() 또는 @AuthenticationPrincipal"
}
```

## 실행 과정

1. UserController: path/param에서 `userId` 제거, principal로 tier-info·upgrade 처리.
2. 관리자 티어 변경은 admin 전용 API로 분리 (기존 AdminUserController 활용 여부 확인).
3. StatisticsController: path variable `userId` 제거, 서비스에 principal만 전달.

## 다음 Subagent로 전달할 데이터

```json
{
  "controllersDone": ["UserController", "StatisticsController"],
  "meEndpoints": ["/api/v1/users/me/tier-info", "/api/v1/statistics/monthly", "/api/v1/statistics/analysis"]
}
```

## 사용자 확인 포인트

> 💬 "User·Statistics API의 userId가 제거되었습니다. Subscription 단계로 진행할까요?"

---

# Subagent 3: Subscription

**역할:** SubscriptionController userId 제거 + update/delete/toggle 시 owner 검증

## 담당 작업

1. **userId 제거**
   - `POST/GET /api/v1/subscriptions?userId=...` → `POST/GET /api/v1/subscriptions` (principal로 생성/조회).
2. **리소스 IDOR 방어**
   - `PUT /api/v1/subscriptions/{id}`, `DELETE /api/v1/subscriptions/{id}`, `PATCH /api/v1/subscriptions/{id}/status`
   - 서비스에서 `subscription.getUser().getId().equals(principalUserId)` 검증 후 진행, 아니면 403.

## 이전 Subagent로부터 받은 데이터

```json
{
  "principalExtraction": "SecurityUtils.getCurrentUserId() 또는 @AuthenticationPrincipal",
  "controllersDone": ["UserController", "StatisticsController"]
}
```

## 실행 과정

1. Controller: 모든 `userId` param 제거, principal만 서비스에 전달.
2. Service: update/delete/toggle 메서드에 `Long principalUserId` 인자 추가, 조회한 Subscription의 owner와 비교 후 403 처리.

## 다음 Subagent로 전달할 데이터

```json
{
  "controllersDone": ["UserController", "StatisticsController", "SubscriptionController"],
  "ownerCheckAdded": ["SubscriptionService.update", "SubscriptionService.delete", "SubscriptionService.toggle"]
}
```

## 사용자 확인 포인트

> 💬 "Subscription API에 userId 제거 및 owner 검증이 반영되었습니다. Budget·Preference로 진행할까요?"

---

# Subagent 4: Budget & Preference

**역할:** BudgetController, PreferenceController에서 userId 제거 후 principal만 사용

## 담당 작업

1. **BudgetController**
   - `POST/GET /api/v1/budgets?userId=...`, `GET /api/v1/budgets/current?userId=...`, `GET /api/v1/budgets/{year}/{month}?userId=...`, `DELETE /api/v1/budgets/{budgetId}?userId=...`
   - 전부 userId 제거, principal 사용. delete는 `deleteBudget(budgetId, principalUserId)` 형태로 서비스에서 owner 검증 유지.
2. **PreferenceController**
   - `POST /api/v1/preferences/submit?userId=...`, `GET/DELETE /api/v1/preferences/profile?userId=...`
   - userId 제거, principal만 사용.

## 이전 Subagent로부터 받은 데이터

```json
{
  "principalExtraction": "SecurityUtils.getCurrentUserId() 또는 @AuthenticationPrincipal",
  "controllersDone": ["UserController", "StatisticsController", "SubscriptionController"]
}
```

## 실행 과정

1. BudgetController: 모든 `@RequestParam Long userId` 제거, principal로 서비스 호출.
2. BudgetService delete: 시그니처에 principalUserId 포함, 내부에서 `budget.userId == principalUserId` 검증.
3. PreferenceController: 동일하게 userId 제거, principal만 전달.

## 다음 Subagent로 전달할 데이터

```json
{
  "controllersDone": ["UserController", "StatisticsController", "SubscriptionController", "BudgetController", "PreferenceController"]
}
```

## 사용자 확인 포인트

> 💬 "Budget·Preference API가 principal 기반으로 변경되었습니다. Recommendation 단계로 진행할까요?"

---

# Subagent 5: Recommendation

**역할:** RecommendationController userId 제거 + feedback/click 시 recommendationId 소유권 검증

## 담당 작업

1. **userId 제거**
   - `POST /api/v1/recommendations/ai?userId=...` → `POST /api/v1/recommendations/ai`
   - `POST /api/v1/recommendations/ai/stream?userId=...` → `POST /api/v1/recommendations/ai/stream`
   - `GET /api/v1/recommendations/history/{userId}` → `GET /api/v1/recommendations/history`
   - `POST .../feedback?userId=...`, `POST .../click?userId=...` → userId 제거.
2. **리소스 IDOR 방어**
   - feedback/click: RecommendationResult 조회 후 `recommendationResult.userId == principalUserId` 검증, 아니면 403.

## 이전 Subagent로부터 받은 데이터

```json
{
  "principalExtraction": "SecurityUtils.getCurrentUserId() 또는 @AuthenticationPrincipal",
  "controllersDone": ["UserController", "StatisticsController", "SubscriptionController", "BudgetController", "PreferenceController"]
}
```

## 실행 과정

1. Controller: 모든 userId param/path 제거, principal만 서비스에 전달.
2. Service (feedback/click): recommendationId로 엔티티 조회 후 owner == principal 검증 추가.

## 다음 Subagent로 전달할 데이터

```json
{
  "controllersDone": ["...", "RecommendationController"],
  "ownerCheckAdded": ["RecommendationService.feedback", "RecommendationService.click"]
}
```

## 사용자 확인 포인트

> 💬 "Recommendation API에 userId 제거 및 소유권 검증이 반영되었습니다. Notification·Optimization으로 진행할까요?"

---

# Subagent 6: Notification & Optimization

**역할:** NotificationController, NotificationSettingController, OptimizationController에서 userId 제거 후 principal만 사용

## 담당 작업

1. **NotificationController**
   - `GET /api/v1/notifications?userId=...`, `GET .../unread?userId=...`, `GET .../unread-count?userId=...`
   - `PUT .../read?userId=...`, `PUT .../read-all?userId=...`
   - 전부 userId 제거, principal 사용. markAsRead(notificationId) 내부에서 `notification.userId == principal` 검사 유지.
2. **NotificationSettingController**
   - `GET/PUT /api/v1/notification-settings?userId=...` → userId 제거, principal로 처리.
3. **OptimizationController**
   - `GET /api/v1/optimization/suggestions?userId=...`, `.../duplicates?userId=...`, `.../alternatives?userId=...`
   - userId 제거, principal 기반 + 소유 데이터만.

## 이전 Subagent로부터 받은 데이터

```json
{
  "principalExtraction": "SecurityUtils.getCurrentUserId() 또는 @AuthenticationPrincipal",
  "controllersDone": ["UserController", "StatisticsController", "SubscriptionController", "BudgetController", "PreferenceController", "RecommendationController"]
}
```

## 실행 과정

1. 세 Controller 모두 `@RequestParam Long userId` / path variable userId 제거.
2. 서비스 호출 시 principal만 전달.
3. NotificationController markAsRead: 서비스 내부에서 notification 소유자 검증 확인/보강.

## 다음 Subagent로 전달할 데이터

```json
{
  "controllersDone": ["UserController", "StatisticsController", "SubscriptionController", "BudgetController", "PreferenceController", "RecommendationController", "NotificationController", "NotificationSettingController", "OptimizationController"],
  "allUserIdRemoved": true
}
```

## 사용자 확인 포인트

> 💬 "Notification·Optimization까지 principal 전환이 완료되었습니다. 최종 검증 단계로 진행할까요?"

---

# Subagent 7: Verification

**역할:** DEFFENCE-IDOR.md 수정 체크리스트 검증 및 회귀 테스트

## 담당 작업

1. **체크리스트 검증**
   - 모든 `@RequestParam Long userId` / `@PathVariable Long userId` 제거 여부 확인.
   - Subscription update/delete/toggle owner 체크 추가 여부 확인.
   - Recommendation feedback/click + history principal 기반 및 소유권 체크 확인.
   - 알림/예산/성향/통계 /me 개념 통일 여부 확인.
2. **회귀:** 기존 기능 테스트(또는 API 테스트)로 인증 사용자 기준 동작 확인.

## 이전 Subagent로부터 받은 데이터

```json
{
  "controllersDone": ["UserController", "StatisticsController", "SubscriptionController", "BudgetController", "PreferenceController", "RecommendationController", "NotificationController", "NotificationSettingController", "OptimizationController"],
  "allUserIdRemoved": true
}
```

## 실행 과정

1. DEFFENCE-IDOR.md 하단 “수정 체크리스트” 항목별로 코드 검색하여 완료 여부 체크.
2. 필요 시 단위/통합 테스트 실행, 실패 케이스 수정.
3. (선택) IDOR 시나리오 테스트: 타 userId로 요청 시 403/401 기대.

## 다음 Subagent로 전달할 데이터

- 없음 (최종 단계).

## 산출물

- 체크리스트 완료 표 (문서 또는 이슈에 체크).
- 회귀 테스트 결과 요약.

## 최종 확인 포인트

> 💬 "IDOR 방어 체크리스트 검증과 회귀 테스트가 완료되었습니다. 배포 전 한 번 더 검토할까요?"

---

## 검증 체크리스트 (구현 완료 후)

| 항목 | 상태 |
|------|------|
| 모든 내 데이터 API에서 @RequestParam/@PathVariable Long userId 제거, principal 교체 | ✅ |
| Subscription update/delete/toggle에 owner 체크 추가 | ✅ |
| Recommendation feedback/click + history principal 기반 및 소유권 체크 | ✅ |
| 알림/예산/성향/통계 /me 개념 통일 (principal만 사용) | ✅ |
| UserController tier: /me/tier-info, 관리자 티어 변경은 AdminUserController | ✅ |
| 테스트: StatisticsControllerTest, SubscriptionControllerTest X-Test-User-Id 헤더 적용 | ✅ |

---

## 우선순위 요약 (DEFFENCE-IDOR.md 체크리스트와 매핑)

| 순위 | 내용 | 담당 Subagent |
|------|------|----------------|
| 1 | 모든 @RequestParam/@PathVariable Long userId 제거, principal 교체 | 2~6 |
| 2 | Subscription update/delete/toggle에 owner 체크 추가 | 3 |
| 3 | Recommendation feedback/click + history principal + 소유권 체크 | 5 |
| 4 | 알림/예산/성향/통계 /me 개념 통일 | 4, 6 |
| 5 | 체크리스트·회귀 검증 | 7 |

이 계획에 따라 Subagent를 순서대로 실행하면 DEFFENCE-IDOR.md 규칙이 단계별로 적용됩니다.
