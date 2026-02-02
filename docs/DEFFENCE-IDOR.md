컨벤션: 모든 내 데이터 API는 principal만 사용. Controller에서는 `@AuthenticationPrincipal Long userId` 또는 `SecurityUtils.getCurrentUserId()` 사용.

---

핵심 원칙 (IDOR 방어 규칙)
1) 내 데이터 API는 userId를 클라이언트에서 받지 않는다.
   → JWT에서 나온 userId(principal)만 사용 (/me 패턴 추천)

2) 리소스가 subscriptionId, budgetId, notificationId처럼 단일 id로 조작되는 API는
   → “그 리소스의 owner == principal userId” 검증이 반드시 있어야 함.

---

A. “userId를 입력받는” 위험 API 목록 (IDOR 1차 라인)
1) UserController
GET /api/v1/users/{userId}/tier-info
PUT /api/v1/users/{userId}/upgrade-tier?newTier=...
왜 위험? 로그인만 되어 있으면 {userId} 바꿔서 타인 티어 조회/변경 시도 가능.
수정 방향(추천):
내 티어 조회: GET /api/v1/users/me/tier-info
내 티어 변경은 사실 운영상 결제/관리 로직이랑 엮이므로:
일반 유저용 “upgrade”는 막고,
관리자용만 허용: PUT /api/v1/admin/users/{userId}/tier 같은 식으로 분리 추천.

---

2) StatisticsController
GET /api/v1/statistics/monthly/{userId}
GET /api/v1/statistics/analysis/{userId}
왜 위험? userId만 바꾸면 남의 지출 통계/분석을 볼 수 있게 될 수 있음(민감정보).
수정 방향(추천):
GET /api/v1/statistics/monthly (userId 제거, principal로 계산)
GET /api/v1/statistics/analysis (userId 제거)
---

3) RecommendationController (여기 특히 위험도가 큼)
POST /api/v1/recommendations/ai?userId=...
POST /api/v1/recommendations/ai/stream?userId=...
GET /api/v1/recommendations/history/{userId}
POST /api/v1/recommendations/{recommendationId}/feedback?userId=...
POST /api/v1/recommendations/{recommendationId}/click?userId=...
왜 위험?
userId를 바꿔서 남의 추천 히스토리 조회 가능해질 수 있음.
더 심각하게는 GPT 호출에 userId가 들어가면, 남의 티어/사용량 제한을 대신 소모하거나(비용), 추천 결과 저장/피드백을 남의 계정에 남길 수도 있음.
수정 방향(추천):
전부 userId 파라미터 제거하고 principal 사용:
POST /api/v1/recommendations/ai
POST /api/v1/recommendations/ai/stream
GET /api/v1/recommendations/history
feedback/click는 추가로 서비스 단에서:
recommendationId가 principal userId의 recommendation인지 검증(소유권 체크) 필수.

---

4) SubscriptionController
POST /api/v1/subscriptions?userId=...
GET /api/v1/subscriptions?userId=...
왜 위험? userId만 바꾸면 남의 구독 목록 조회/생성 가능해질 수 있음.
수정 방향(추천):
userId 제거:
POST /api/v1/subscriptions (principal userId로 생성)
GET /api/v1/subscriptions (principal userId로 조회 + 필터)

---

5) BudgetController
POST /api/v1/budgets?userId=...
GET /api/v1/budgets?userId=...
GET /api/v1/budgets/current?userId=...
GET /api/v1/budgets/{year}/{month}?userId=...
DELETE /api/v1/budgets/{budgetId}?userId=...
왜 위험?
조회/설정은 userId 바꿔치기로 타인 예산 조회/설정 위험.
delete는 서비스에서 budget.userId == userId 검증은 있는데, 그 userId 자체를 클라이언트가 주는 구조가 문제(원칙적으로 principal을 써야 함).
수정 방향(추천):
전부 userId 제거, principal 사용.
delete는 deleteBudget(budgetId, principalUserId)로 호출.

---

6) PreferenceController
POST /api/v1/preferences/submit?userId=...
GET /api/v1/preferences/profile?userId=...
DELETE /api/v1/preferences/profile?userId=...
왜 위험? userId 바꿔서 남의 성향 데이터 생성/조회/삭제 가능해질 수 있음(민감).
수정 방향(추천):
POST /api/v1/preferences/submit (principal userId)
GET /api/v1/preferences/profile (principal userId)
DELETE /api/v1/preferences/profile (principal userId)

---

7) NotificationController
GET /api/v1/notifications?userId=...
GET /api/v1/notifications/unread?userId=...
GET /api/v1/notifications/unread-count?userId=...
PUT /api/v1/notifications/{notificationId}/read?userId=...
PUT /api/v1/notifications/read-all?userId=...
왜 위험?
조회 계열은 userId 바꿔서 남의 알림 목록/읽지않음 개수 조회 가능.
markAsRead는 서비스에서 소유권 체크를 하긴 하는데, 그래도 userId를 받는 구조 자체가 불필요하고 취약.
수정 방향(추천):
전부 userId 제거하고 principal userId로 처리.
markAsRead(notificationId) 호출 시 내부에서 “notification.userId == principal” 검사.

---

8) NotificationSettingController
GET /api/v1/notification-settings?userId=...
PUT /api/v1/notification-settings?userId=...
왜 위험? userId 바꿔서 남의 알림 설정 조회/변경 가능.
수정 방향(추천):
userId 제거하고 principal로 처리.

---

9) OptimizationController
GET /api/v1/optimization/suggestions?userId=...
GET /api/v1/optimization/duplicates?userId=...
GET /api/v1/optimization/alternatives?userId=...
왜 위험?
userId 바꿔서 남의 구독 기반 “최적화 결과(=생활 패턴/지출 성향)”를 볼 수 있음.
티어 제한/사용량 카운트도 남 계정으로 소모 가능.
수정 방향(추천):
userId 제거, principal 기반 + 소유 데이터만.

---

B. “userId가 없어도” 위험한 곳 (리소스 IDOR 2차 라인)
1) SubscriptionController의 수정/삭제/상태변경
PUT /api/v1/subscriptions/{id}
DELETE /api/v1/subscriptions/{id}
PATCH /api/v1/subscriptions/{id}/status
왜 위험?
현재 SubscriptionService.update/delete/toggle은 subscriptionId만으로 조회 후 바로 변경/삭제하고,
subscription이 누구 것인지(owner) 검증이 없어.
→ 공격자가 subscriptionId를 추측/획득하면 남의 구독을 수정/삭제 가능.
수정 방향(추천):
컨트롤러에서 principal userId를 받아서 서비스로 같이 넘기고,
서비스에서 subscription.getUser().getId().equals(principalUserId) 아니면 403.

---

2) RecommendationController의 feedback/click
POST /recommendations/{recommendationId}/feedback
POST /recommendations/{recommendationId}/click
왜 위험?
recommendationId만 알아내면 남의 추천 결과에 피드백/클릭을 “주입”할 수 있음(데이터 오염, 통계 왜곡).
수정 방향(추천):
RecommendationResult 조회 후 recommendationResult.userId == principalUserId 검증.
---

제일 빠른 “수정 체크리스트”(운영 빨리 올릴 때 우선순위)
1) 모든 @RequestParam Long userId / @PathVariable Long userId 제거하고 principal로 교체
2) Subscription update/delete/toggle에 owner 체크 추가
3) Recommendation feedback/click + history도 principal 기반 + 소유권 체크
4) 알림/예산/성향/통계 전부 /me 개념으로 통일
