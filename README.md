# Subing Backend

구독 서비스 관리 플랫폼의 백엔드 API 서버

## 기술 스택

| 구분 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.5.7 |
| ORM | Spring Data JPA + Hibernate |
| Database | PostgreSQL |
| Auth | Spring Security + JWT (HS256) |
| AI | Spring AI 1.0.3 (OpenAI GPT-4o) |
| Real-time | WebSocket (STOMP + SockJS) |
| Cache | Caffeine |
| Build | Gradle 8.14.3 |
| Deploy | Docker + Railway |

## 프로젝트 구조

```
src/main/java/com/project/subing/
├── config/              # 설정 (Security, WebSocket, Cache, SpringAI)
├── controller/          # REST API 컨트롤러 (16개)
├── domain/              # JPA 엔티티 및 Enum (30개)
├── dto/                 # Request/Response DTO (51개)
├── repository/          # JPA Repository (17개)
├── service/             # 비즈니스 로직 (18개)
├── security/            # JWT 토큰, 인증 필터
├── scheduler/           # @Scheduled 알림 작업
└── exception/           # 예외 처리 (@ControllerAdvice)
```

## API 엔드포인트

### 사용자 인증 (`/api/v1/users`)

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/signup` | 회원가입 |
| POST | `/login` | 로그인 |
| POST | `/login/google` | Google OAuth 로그인 |
| GET | `/me/tier-info` | 티어 및 사용량 조회 |

### 구독 관리 (`/api/v1/subscriptions`)

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/` | 구독 추가 |
| GET | `/` | 구독 목록 조회 (필터/정렬) |
| PUT | `/{id}` | 구독 수정 |
| DELETE | `/{id}` | 구독 삭제 |
| PATCH | `/{id}/status` | 활성/비활성 토글 |

### AI 추천 (`/api/v1/recommendations`)

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/ai` | GPT 추천 요청 |
| POST | `/ai/stream` | SSE 스트리밍 추천 |
| GET | `/history` | 추천 기록 조회 |
| POST | `/{id}/feedback` | 피드백 제출 |
| POST | `/{id}/click` | 클릭 추적 (A/B 테스트) |

### 예산 관리 (`/api/v1/budgets`)

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/` | 예산 설정 |
| GET | `/` | 전체 예산 목록 |
| GET | `/current` | 이번 달 예산 |
| GET | `/{year}/{month}` | 특정 월 예산 |
| DELETE | `/{budgetId}` | 예산 삭제 |

### 통계 (`/api/v1/statistics`)

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/monthly` | 월별 지출 |
| GET | `/analysis` | 카테고리별 분석 |

### 최적화 제안 (`/api/v1/optimization`)

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/suggestions` | 최적화 제안 |
| GET | `/duplicates` | 중복 서비스 감지 |
| GET | `/alternatives` | 저렴한 대안 |
| POST | `/events` | 이벤트 추적 |

### 알림 (`/api/v1/notifications`)

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/` | 알림 목록 |
| GET | `/unread` | 읽지 않은 알림 |
| GET | `/unread-count` | 안읽은 수 |
| PUT | `/{id}/read` | 읽음 처리 |
| PUT | `/read-all` | 전체 읽음 |

### 알림 설정 (`/api/v1/notification-settings`)

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/` | 설정 조회 |
| PUT | `/` | 설정 변경 |

### 리뷰 (`/api/v1/reviews`)

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/` | 리뷰 작성 |
| GET | `/service/{serviceId}` | 서비스별 리뷰 |
| GET | `/my` | 내 리뷰 |
| PUT | `/{reviewId}` | 리뷰 수정 |
| DELETE | `/{reviewId}` | 리뷰 삭제 |
| GET | `/service/{serviceId}/rating` | 평균 평점 |
| GET | `/service/{serviceId}/check` | 작성 여부 확인 |

### 성향 테스트 (`/api/v1/preferences`)

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/questions` | 질문 목록 |
| POST | `/submit` | 답변 제출 및 분석 |
| GET | `/profile` | 성향 프로필 조회 |
| DELETE | `/profile` | 프로필 초기화 |

### 서비스 조회 (`/api/v1/services`)

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/` | 전체 서비스 |
| GET | `/{serviceId}` | 서비스 상세 |
| GET | `/category/{category}` | 카테고리별 |
| POST | `/compare` | 서비스 비교 |

### 관리자 API (`/api/v1/admin/*`)

> `@PreAuthorize("hasRole('ADMIN')")` 적용

| 그룹 | 엔드포인트 | 설명 |
|------|----------|------|
| 서비스 | `/admin/services` | 서비스 CRUD |
| 플랜 | `/admin/plans` | 플랜 CRUD |
| 사용자 | `/admin/users` | 사용자 관리, 티어/역할 변경 |
| 통계 | `/admin/statistics` | 대시보드 지표 |
| 최적화 | `/admin/optimization-config` | 정책 설정, 롤백, 변경 이력 |

## 주요 기능

### 인증/인가
- JWT 기반 인증 (HS256, Access Token 1시간 / Refresh Token 7일)
- Google OAuth 2.0 소셜 로그인
- Role 기반 접근 제어 (USER / ADMIN)

### 회원 등급 시스템
- **FREE**: GPT 추천 월 10회, 최적화 월 3회
- **PRO**: 모든 기능 무제한

### AI 추천
- Spring AI + GPT-4o 기반 맞춤형 서비스 추천
- SSE 스트리밍 (실시간 타이핑 효과)
- A/B 테스트 (PromptVersion V1/V2)
- 추천 클릭 추적 및 피드백 수집
- 사용자 성향 데이터 활용

### 실시간 알림
- WebSocket (STOMP + SockJS) 푸시 알림
- 결제일 알림 (3일 전, 1일 전)
- 예산 초과 알림 (매일 자정)
- 미사용 구독 감지 (매주 월요일, 90일 이상)
- 가격 변동 / 구독 갱신 알림

### 구독 최적화
- 중복 서비스 감지 (같은 카테고리 2개 이상)
- 저렴한 대안 제안
- 관리자 정책 오버라이드 + 변경 이력 감사

## 실행 방법

### 사전 요구사항
- Java 17
- PostgreSQL
- OpenAI API Key

### 로컬 실행

```bash
# 환경 변수 설정
cp .env.dev .env

# 실행
./gradlew bootRun
```

### Docker 빌드

```bash
docker build -t subing-backend .
docker run -p 8080:8080 --env-file .env.prod subing-backend
```

### 환경 변수

| 변수 | 설명 |
|------|------|
| `DB_URL` | PostgreSQL JDBC URL |
| `DB_USERNAME` | DB 사용자명 |
| `DB_PASSWORD` | DB 비밀번호 |
| `JWT_SECRET` | JWT 서명 키 (32자 이상) |
| `OPENAI_API_KEY` | OpenAI API 키 |
| `GOOGLE_CLIENT_ID` | Google OAuth Client ID |
| `GOOGLE_CLIENT_SECRET` | Google OAuth Client Secret |

## 배포

- **플랫폼**: Railway (Hobby Plan)
- **런타임**: Eclipse Temurin 17 JRE Alpine
- **메모리**: JVM 최적화 적용 (RSS ~450MB 목표)
- **CI/CD**: GitHub Actions (main/dev 브랜치 push/PR 시 자동 빌드 및 테스트)
