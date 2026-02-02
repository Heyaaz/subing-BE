# Subing API Exception Handling Guide

## 목차
1. [개요](#개요)
2. [예외 계층 구조](#예외-계층-구조)
3. [에러 코드 목록](#에러-코드-목록)
4. [에러 응답 형식](#에러-응답-형식)
5. [예외 타입별 가이드](#예외-타입별-가이드)
6. [개발자 가이드](#개발자-가이드)
7. [클라이언트 에러 핸들링 가이드](#클라이언트-에러-핸들링-가이드)

---

## 개요

Subing API는 일관된 예외 처리를 위해 계층화된 커스텀 예외 시스템을 사용합니다.
모든 예외는 `ApplicationException`을 상속하며, 각 예외는 고유한 `ErrorCode`를 가집니다.

### 주요 특징
- ✅ **일관된 에러 응답 형식**: 모든 API 에러는 동일한 구조의 JSON을 반환
- ✅ **명확한 HTTP 상태 코드**: 예외 타입에 따라 적절한 HTTP 상태 코드 반환
- ✅ **구조화된 에러 코드**: 문자열 메시지 대신 에러 코드로 분류
- ✅ **상세한 에러 정보**: 디버깅을 위한 추가 정보 제공
- ✅ **타입 안전성**: 컴파일 타임에 예외 타입 검증

---

## 예외 계층 구조

```
ApplicationException (추상 베이스)
│
├── EntityNotFoundException         [404 Not Found]
│   ├── UserNotFoundException
│   ├── ServiceNotFoundException
│   ├── SubscriptionNotFoundException
│   ├── ReviewNotFoundException
│   ├── BudgetNotFoundException
│   ├── PlanNotFoundException
│   ├── NotificationNotFoundException
│   ├── PreferenceNotFoundException
│   ├── RecommendationNotFoundException
│   └── OptionNotFoundException
│
├── BusinessRuleViolationException  [422 Unprocessable Entity]
│   ├── DuplicateEmailException
│   ├── InvalidRatingException
│   ├── DuplicateReviewException
│   ├── InvalidCredentialsException
│   └── MissingServicesException
│
├── AuthorizationException          [403 Forbidden]
│   ├── UnauthorizedAccessException
│   └── ReviewOwnershipException
│
├── TierLimitExceededException      [429 Too Many Requests]
│   ├── GptRecommendationLimitException
│   └── OptimizationCheckLimitException
│
└── ExternalApiException            [502 Bad Gateway / 503 Service Unavailable]
    ├── GptApiException
    ├── GptParsingException
    └── RecommendationSaveException
```

### 예외 카테고리별 HTTP 상태 코드

| 예외 카테고리 | HTTP 상태 코드 | 설명 |
|--------------|----------------|------|
| `EntityNotFoundException` | 404 | 요청한 리소스를 찾을 수 없음 |
| `BusinessRuleViolationException` | 422 | 비즈니스 규칙 위반 |
| `AuthorizationException` | 403 | 권한 없음 |
| `TierLimitExceededException` | 429 | 사용량 제한 초과 (Rate Limit) |
| `ExternalApiException` | 502/503 | 외부 API 오류 |
| `Validation Error` | 400 | 입력 값 검증 실패 (@Valid) |
| `Unexpected Error` | 500 | 예상치 못한 서버 오류 |

---

## 에러 코드 목록

### 400 Bad Request
| 에러 코드 | 메시지 | 설명 |
|----------|--------|------|
| `INVALID_INPUT` | 잘못된 입력입니다 | 요청 파라미터 또는 바디가 유효하지 않음 |

### 401 Unauthorized
| 에러 코드 | 메시지 | 설명 |
|----------|--------|------|
| `INVALID_CREDENTIALS` | 이메일 또는 비밀번호가 올바르지 않습니다 | 로그인 인증 실패 |

### 403 Forbidden
| 에러 코드 | 메시지 | 설명 |
|----------|--------|------|
| `UNAUTHORIZED_ACCESS` | 권한이 없습니다 | 해당 리소스에 접근 권한 없음 |
| `REVIEW_OWNERSHIP_VIOLATION` | 본인이 작성한 리뷰만 수정/삭제할 수 있습니다 | 리뷰 소유권 위반 |

### 404 Not Found
| 에러 코드 | 메시지 | 설명 |
|----------|--------|------|
| `USER_NOT_FOUND` | 사용자를 찾을 수 없습니다 | 사용자 ID가 존재하지 않음 |
| `SERVICE_NOT_FOUND` | 서비스를 찾을 수 없습니다 | 구독 서비스 ID가 존재하지 않음 |
| `SUBSCRIPTION_NOT_FOUND` | 구독을 찾을 수 없습니다 | 구독 정보 ID가 존재하지 않음 |
| `REVIEW_NOT_FOUND` | 리뷰를 찾을 수 없습니다 | 리뷰 ID가 존재하지 않음 |
| `BUDGET_NOT_FOUND` | 예산을 찾을 수 없습니다 | 예산 설정 ID가 존재하지 않음 |
| `PLAN_NOT_FOUND` | 플랜을 찾을 수 없습니다 | 서비스 플랜 ID가 존재하지 않음 |
| `NOTIFICATION_NOT_FOUND` | 알림을 찾을 수 없습니다 | 알림 ID가 존재하지 않음 |
| `PREFERENCE_NOT_FOUND` | 성향 프로필을 찾을 수 없습니다 | 성향 테스트 결과가 존재하지 않음 |
| `RECOMMENDATION_NOT_FOUND` | 추천 결과를 찾을 수 없습니다 | GPT 추천 결과 ID가 존재하지 않음 |
| `OPTION_NOT_FOUND` | 옵션을 찾을 수 없습니다 | 성향 테스트 옵션 ID가 존재하지 않음 |

### 422 Unprocessable Entity
| 에러 코드 | 메시지 | 설명 |
|----------|--------|------|
| `DUPLICATE_EMAIL` | 이미 사용 중인 이메일입니다 | 이메일 중복 |
| `INVALID_RATING` | 평점은 1~5 사이의 값이어야 합니다 | 평점 값이 유효 범위를 벗어남 |
| `DUPLICATE_REVIEW` | 이미 해당 서비스에 리뷰를 작성하셨습니다 | 동일 서비스에 중복 리뷰 작성 시도 |
| `MISSING_SERVICES` | 일부 서비스를 찾을 수 없습니다 | 요청한 서비스 목록 중 일부가 존재하지 않음 |

### 429 Too Many Requests
| 에러 코드 | 메시지 | 설명 |
|----------|--------|------|
| `GPT_RECOMMENDATION_LIMIT_EXCEEDED` | GPT 추천 사용 횟수를 초과했습니다 | FREE 티어 GPT 추천 횟수 제한 초과 |
| `OPTIMIZATION_CHECK_LIMIT_EXCEEDED` | 최적화 체크 사용 횟수를 초과했습니다 | FREE 티어 구독 최적화 횟수 제한 초과 |

### 500 Internal Server Error
| 에러 코드 | 메시지 | 설명 |
|----------|--------|------|
| `INTERNAL_SERVER_ERROR` | 서버 내부 오류가 발생했습니다 | 예상치 못한 서버 오류 |
| `RECOMMENDATION_SAVE_ERROR` | 추천 결과 저장에 실패했습니다 | 추천 결과 DB 저장 실패 |

### 502 Bad Gateway
| 에러 코드 | 메시지 | 설명 |
|----------|--------|------|
| `GPT_API_ERROR` | GPT API 호출에 실패했습니다 | OpenAI API 호출 실패 |
| `GPT_PARSING_ERROR` | GPT 응답 파싱에 실패했습니다 | GPT 응답 형식 파싱 오류 |

---

## 에러 응답 형식

### 성공 응답 (참고)
```json
{
  "success": true,
  "data": { ... },
  "message": "요청이 성공적으로 처리되었습니다",
  "timestamp": "2025-01-22T10:30:00.123"
}
```

### 에러 응답 (공통 형식)
```json
{
  "success": false,
  "data": {
    "errorCode": "USER_NOT_FOUND",
    "message": "사용자를 찾을 수 없습니다: ID 123",
    "timestamp": "2025-01-22T10:30:00.123",
    "path": "/api/v1/users/123",
    "details": null
  },
  "message": "에러가 발생했습니다",
  "timestamp": "2025-01-22T10:30:00.123"
}
```

### 에러 응답 필드 설명

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `success` | boolean | ✅ | 항상 `false` |
| `data.errorCode` | string | ✅ | 에러 코드 (위 에러 코드 표 참조) |
| `data.message` | string | ✅ | 사용자에게 표시할 에러 메시지 (한글) |
| `data.timestamp` | string (ISO 8601) | ✅ | 에러 발생 시각 |
| `data.path` | string | ✅ | 에러가 발생한 API 경로 |
| `data.details` | object \| null | ❌ | 추가 디버깅 정보 (선택적) |
| `message` | string | ✅ | 최상위 메시지 (보통 "에러가 발생했습니다") |
| `timestamp` | string (ISO 8601) | ✅ | 응답 생성 시각 |

### Validation 에러 응답 (@Valid 실패)
```json
{
  "success": false,
  "data": {
    "errorCode": "INVALID_INPUT",
    "message": "입력 값 검증에 실패했습니다",
    "timestamp": "2025-01-22T10:30:00.123",
    "path": "/api/v1/subscriptions",
    "details": {
      "email": "이메일 형식이 올바르지 않습니다",
      "password": "비밀번호는 최소 8자 이상이어야 합니다"
    }
  },
  "message": "에러가 발생했습니다",
  "timestamp": "2025-01-22T10:30:00.123"
}
```

---

## 예외 타입별 가이드

### 1. EntityNotFoundException (404)

**언제 발생하나요?**
- 데이터베이스에서 특정 ID의 엔티티를 조회했으나 존재하지 않을 때

**예제:**
```http
GET /api/v1/users/999
```

**응답:**
```json
{
  "success": false,
  "data": {
    "errorCode": "USER_NOT_FOUND",
    "message": "사용자를 찾을 수 없습니다: ID 999",
    "timestamp": "2025-01-22T10:30:00",
    "path": "/api/v1/users/999"
  }
}
```
**HTTP Status:** `404 Not Found`

**클라이언트 처리 방법:**
- 사용자에게 "해당 정보를 찾을 수 없습니다" 안내
- 목록 페이지로 리다이렉트
- 새로고침 후 재시도 권장

---

### 2. BusinessRuleViolationException (422)

**언제 발생하나요?**
- 비즈니스 규칙을 위반한 요청을 했을 때
- 입력 값은 유효하지만 비즈니스 로직상 허용되지 않는 경우

**예제 1: 중복 이메일**
```http
POST /api/v1/auth/signup
{
  "email": "existing@example.com",
  "password": "password123"
}
```

**응답:**
```json
{
  "success": false,
  "data": {
    "errorCode": "DUPLICATE_EMAIL",
    "message": "이미 사용 중인 이메일입니다",
    "timestamp": "2025-01-22T10:30:00",
    "path": "/api/v1/auth/signup"
  }
}
```
**HTTP Status:** `422 Unprocessable Entity`

**예제 2: 잘못된 평점**
```http
POST /api/v1/reviews
{
  "serviceId": 1,
  "rating": 6,
  "content": "Great service"
}
```

**응답:**
```json
{
  "success": false,
  "data": {
    "errorCode": "INVALID_RATING",
    "message": "평점은 1~5 사이의 값이어야 합니다",
    "timestamp": "2025-01-22T10:30:00",
    "path": "/api/v1/reviews"
  }
}
```

**클라이언트 처리 방법:**
- 사용자에게 구체적인 에러 메시지 표시
- 입력 폼의 해당 필드에 에러 표시
- 수정 후 재시도 가능

---

### 3. AuthorizationException (403)

**언제 발생하나요?**
- 인증은 되었으나 해당 리소스에 대한 권한이 없을 때
- 다른 사용자의 리소스를 수정/삭제하려고 할 때

**예제:**
```http
DELETE /api/v1/reviews/123
Authorization: Bearer {token_of_user_A}
# 하지만 Review 123은 User B가 작성
```

**응답:**
```json
{
  "success": false,
  "data": {
    "errorCode": "REVIEW_OWNERSHIP_VIOLATION",
    "message": "본인이 작성한 리뷰만 삭제할 수 있습니다",
    "timestamp": "2025-01-22T10:30:00",
    "path": "/api/v1/reviews/123"
  }
}
```
**HTTP Status:** `403 Forbidden`

**클라이언트 처리 방법:**
- 사용자에게 "권한이 없습니다" 안내
- 수정/삭제 버튼 숨김 처리
- 읽기 전용 모드로 전환

---

### 4. TierLimitExceededException (429)

**언제 발생하나요?**
- FREE 티어 사용자가 제한된 기능의 사용 횟수를 초과했을 때

**예제:**
```http
POST /api/v1/recommendations/gpt-chat
{
  "userId": 1,
  "quiz": { ... }
}
# FREE 티어는 월 3회 제한
```

**응답:**
```json
{
  "success": false,
  "data": {
    "errorCode": "GPT_RECOMMENDATION_LIMIT_EXCEEDED",
    "message": "GPT 추천 사용 횟수를 초과했습니다. PRO 티어로 업그레이드하세요.",
    "timestamp": "2025-01-22T10:30:00",
    "path": "/api/v1/recommendations/gpt-chat",
    "details": {
      "currentTier": "FREE",
      "limit": 3,
      "used": 3,
      "upgradeUrl": "/api/v1/users/upgrade"
    }
  }
}
```
**HTTP Status:** `429 Too Many Requests`

**클라이언트 처리 방법:**
- PRO 티어 업그레이드 안내 모달 표시
- 남은 사용 횟수 UI에 표시
- 업그레이드 페이지로 유도

---

### 5. ExternalApiException (502/503)

**언제 발생하나요?**
- OpenAI GPT API 호출이 실패했을 때
- 외부 API 응답 파싱에 실패했을 때

**예제:**
```http
POST /api/v1/recommendations/gpt-chat
```

**응답:**
```json
{
  "success": false,
  "data": {
    "errorCode": "GPT_API_ERROR",
    "message": "GPT API 호출에 실패했습니다. 잠시 후 다시 시도해주세요.",
    "timestamp": "2025-01-22T10:30:00",
    "path": "/api/v1/recommendations/gpt-chat",
    "details": {
      "retryAfter": 5,
      "cause": "OpenAI API rate limit exceeded"
    }
  }
}
```
**HTTP Status:** `502 Bad Gateway`

**클라이언트 처리 방법:**
- 사용자에게 "일시적인 오류" 안내
- 자동 재시도 (exponential backoff)
- 잠시 후 다시 시도하도록 권장

---

### 6. Validation Error (400)

**언제 발생하나요?**
- `@Valid` 어노테이션에 의한 입력 검증 실패
- 요청 파라미터 타입이 올바르지 않을 때

**예제:**
```http
POST /api/v1/subscriptions
{
  "serviceId": null,
  "monthlyPrice": -1000,
  "billingDate": 35
}
```

**응답:**
```json
{
  "success": false,
  "data": {
    "errorCode": "INVALID_INPUT",
    "message": "입력 값 검증에 실패했습니다",
    "timestamp": "2025-01-22T10:30:00",
    "path": "/api/v1/subscriptions",
    "details": {
      "serviceId": "서비스 ID는 필수입니다",
      "monthlyPrice": "가격은 0 이상이어야 합니다",
      "billingDate": "결제일은 1~31 사이의 값이어야 합니다"
    }
  }
}
```
**HTTP Status:** `400 Bad Request`

**클라이언트 처리 방법:**
- 각 필드에 에러 메시지 표시
- 잘못된 필드 하이라이트
- 수정 후 재제출 가능

---

## 개발자 가이드

### 새로운 예외 추가하기

#### 1. ErrorCode에 추가
```java
// src/main/java/com/project/subing/exception/ErrorCode.java
public enum ErrorCode {
    // ... 기존 코드

    // 새로운 에러 코드
    PAYMENT_FAILED("422", "결제 처리에 실패했습니다"),

    // ...
}
```

#### 2. 커스텀 예외 클래스 생성
```java
// src/main/java/com/project/subing/exception/business/PaymentFailedException.java
package com.project.subing.exception.business;

import com.project.subing.exception.ErrorCode;

public class PaymentFailedException extends BusinessRuleViolationException {

    public PaymentFailedException(String reason) {
        super(ErrorCode.PAYMENT_FAILED, "결제 처리에 실패했습니다: " + reason);
    }

    public PaymentFailedException(String reason, Object details) {
        super(ErrorCode.PAYMENT_FAILED, "결제 처리에 실패했습니다: " + reason, details);
    }
}
```

#### 3. 서비스에서 사용
```java
// src/main/java/com/project/subing/service/PaymentService.java
@Service
public class PaymentService {

    public void processPayment(PaymentRequest request) {
        try {
            // 결제 처리 로직
            paymentGateway.charge(request);
        } catch (PaymentGatewayException e) {
            throw new PaymentFailedException(e.getMessage(), Map.of(
                "transactionId", request.getTransactionId(),
                "amount", request.getAmount()
            ));
        }
    }
}
```

#### 4. GlobalExceptionHandler에 핸들러 추가 (필요시)
```java
// 대부분의 경우 부모 클래스 핸들러가 처리하므로 추가 불필요
// 특별한 로직이 필요한 경우에만 추가

@ExceptionHandler(PaymentFailedException.class)
public ResponseEntity<ApiResponse<Void>> handlePaymentFailed(
    PaymentFailedException e,
    HttpServletRequest request) {

    // 결제 실패 알림 전송
    notificationService.notifyPaymentFailure(e.getDetails());

    // 기본 에러 응답 반환
    return handleBusinessRuleViolation(e, request);
}
```

### 예외 사용 가이드라인

#### ✅ DO
```java
// 1. 구체적인 예외 사용
throw new UserNotFoundException(userId);

// 2. 추가 정보 제공
throw new GptApiException("Rate limit exceeded", Map.of(
    "retryAfter", 60,
    "requestId", requestId
));

// 3. Entity 조회 시 바로 예외 발생
User user = userRepository.findById(userId)
    .orElseThrow(() -> new UserNotFoundException(userId));

// 4. 비즈니스 규칙 검증
if (reviewRepository.existsByUserIdAndServiceId(userId, serviceId)) {
    throw new DuplicateReviewException(userId, serviceId);
}
```

#### ❌ DON'T
```java
// 1. 일반 RuntimeException 사용 금지
throw new RuntimeException("사용자를 찾을 수 없습니다"); // ❌

// 2. 에러 메시지만으로 분류 금지
throw new RuntimeException("GPT API 호출 실패"); // ❌

// 3. null 반환 금지
public User getUser(Long userId) {
    return userRepository.findById(userId).orElse(null); // ❌
}

// 4. 에러 무시 금지
try {
    gptApiCall();
} catch (Exception e) {
    // 아무것도 안 함 ❌
}
```

### 로깅 가이드

```java
@Service
@Slf4j
public class UserService {

    public UserResponse getUser(Long userId) {
        log.debug("Fetching user: {}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> {
                log.warn("User not found: {}", userId);
                return new UserNotFoundException(userId);
            });

        log.info("User retrieved successfully: {}", userId);
        return mapper.toResponse(user);
    }
}
```

**로깅 레벨 가이드:**
- `ERROR`: 예상치 못한 심각한 오류 (500 에러)
- `WARN`: 예상 가능한 에러 (404, 422, 403, 429 등)
- `INFO`: 중요한 비즈니스 로직 실행
- `DEBUG`: 상세한 디버깅 정보

---

## 클라이언트 에러 핸들링 가이드

### JavaScript/TypeScript 예제

#### Axios Interceptor 설정
```typescript
// api/interceptor.ts
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api/v1'
});

// 응답 인터셉터
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      const { status, data } = error.response;
      const errorCode = data.data?.errorCode;
      const message = data.data?.message;

      // 에러 코드별 처리
      switch (errorCode) {
        case 'USER_NOT_FOUND':
        case 'SERVICE_NOT_FOUND':
        case 'SUBSCRIPTION_NOT_FOUND':
          showNotification('해당 정보를 찾을 수 없습니다', 'error');
          router.push('/dashboard');
          break;

        case 'UNAUTHORIZED_ACCESS':
        case 'REVIEW_OWNERSHIP_VIOLATION':
          showNotification('권한이 없습니다', 'error');
          break;

        case 'GPT_RECOMMENDATION_LIMIT_EXCEEDED':
        case 'OPTIMIZATION_CHECK_LIMIT_EXCEEDED':
          showUpgradeModal(message);
          break;

        case 'DUPLICATE_EMAIL':
          showFieldError('email', message);
          break;

        case 'GPT_API_ERROR':
          showNotification('일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요', 'warning');
          break;

        default:
          showNotification(message || '오류가 발생했습니다', 'error');
      }
    }
    return Promise.reject(error);
  }
);

export default api;
```

#### React 컴포넌트 예제
```typescript
// components/SubscriptionForm.tsx
import { useState } from 'react';
import api from '../api/interceptor';

function SubscriptionForm() {
  const [errors, setErrors] = useState<Record<string, string>>({});

  const handleSubmit = async (data: SubscriptionRequest) => {
    try {
      setErrors({});
      const response = await api.post('/subscriptions', data);
      showNotification('구독이 등록되었습니다', 'success');

    } catch (error) {
      if (error.response?.status === 400) {
        // Validation 에러 처리
        const details = error.response.data.data.details;
        setErrors(details || {});
      }
      // 다른 에러는 인터셉터에서 처리
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <input name="serviceId" />
      {errors.serviceId && <span className="error">{errors.serviceId}</span>}

      <input name="monthlyPrice" />
      {errors.monthlyPrice && <span className="error">{errors.monthlyPrice}</span>}

      <button type="submit">등록</button>
    </form>
  );
}
```

### HTTP 상태 코드별 처리 전략

| 상태 코드 | 클라이언트 처리 |
|----------|----------------|
| 400 | 입력 폼의 각 필드에 에러 메시지 표시 |
| 401 | 로그인 페이지로 리다이렉트 |
| 403 | "권한 없음" 메시지 표시, 버튼 비활성화 |
| 404 | "정보 없음" 메시지 표시, 목록으로 이동 |
| 422 | 구체적인 비즈니스 에러 메시지 표시 |
| 429 | PRO 업그레이드 모달 표시 |
| 500 | "서버 오류" 메시지, 고객센터 안내 |
| 502/503 | "일시적 오류" 메시지, 재시도 버튼 |

---

## 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|-----------|
| 1.0.0 | 2025-01-22 | 초안 작성 |

---

## 관련 문서
- [API 문서](./API_REFERENCE.md)
- [개발 가이드](./DEVELOPMENT_GUIDE.md)
- [아키텍처 문서](./ARCHITECTURE.md)