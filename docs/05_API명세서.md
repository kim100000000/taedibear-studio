# Taedibear Studio — API 명세서

**버전:** v1.3  
**작성일:** 2026-06-27  
**수정일:** 2026-07-01 — 백엔드 Spring Boot 이전 반영 / 분석 API 추가  
**Base URL:** `https://api.taedibear.com` (개발: `http://localhost:8080`)  
**인증 방식:** JWT Bearer Token

---

## 공통 규칙

### 요청 헤더

```
Content-Type: application/json
Authorization: Bearer <JWT토큰>  ← JWT 필요 API에만
```

### 공통 응답 구조

```json
// 성공
{ "success": true, "data": { ... } }

// 실패
{ "success": false, "error": "에러 메시지" }
```

### HTTP 상태 코드

| 코드 | 의미 |
|------|------|
| 200 | 성공 |
| 201 | 생성 성공 |
| 400 | 잘못된 요청 (유효성 검사 실패) |
| 401 | 인증 실패 (토큰 없음 / 만료) |
| 403 | 권한 없음 |
| 404 | 리소스 없음 |
| 500 | 서버 오류 |

---

## 1. 인증 API (`/api/auth`)

---

### POST /api/auth/register

이메일로 회원가입합니다.

**인증:** 불필요

**Request Body:**
```json
{
  "name": "김성태",
  "email": "taedibear@email.com",
  "password": "password123!"
}
```

**유효성 검사:**
- `name`: 필수, 2자 이상
- `email`: 필수, 이메일 형식
- `password`: 필수, 8자 이상

**Response 201:**
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGci...",
    "user": {
      "id": 1,
      "name": "김성태",
      "email": "taedibear@email.com",
      "plan": "free"
    }
  }
}
```

**Error 400 — 중복 이메일:**
```json
{ "success": false, "error": "이미 사용 중인 이메일이에요." }
```

---

### POST /api/auth/login

이메일과 비밀번호로 로그인합니다.

**인증:** 불필요

**Request Body:**
```json
{
  "email": "taedibear@email.com",
  "password": "password123!"
}
```

**Response 200:**
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGci...",
    "user": {
      "id": 1,
      "name": "김성태",
      "email": "taedibear@email.com",
      "plan": "free"
    }
  }
}
```

**Error 401:**
```json
{ "success": false, "error": "이메일 또는 비밀번호가 올바르지 않아요." }
```

---

### GET /api/auth/google

구글 OAuth 로그인 페이지로 리다이렉트합니다.

**인증:** 불필요  
**Response:** 구글 로그인 페이지로 302 리다이렉트

---

### GET /api/auth/google/callback

구글 OAuth 콜백 처리 후 JWT를 발급합니다.

**인증:** 불필요  
**Response:** 프론트엔드로 리다이렉트 (`/auth?token=<JWT>`)

---

### GET /api/auth/kakao

카카오 OAuth 로그인 페이지로 리다이렉트합니다.

**인증:** 불필요  
**Response:** 카카오 로그인 페이지로 302 리다이렉트

---

### GET /api/auth/kakao/callback

카카오 OAuth 콜백 처리 후 JWT를 발급합니다.

**인증:** 불필요  
**Response:** 프론트엔드로 리다이렉트 (`/auth?token=<JWT>`)

**참고:** 카카오 비즈니스 채널 연동 전에는 email scope가 제공되지 않을 수 있어, 이 경우 `kakao_<id>@kakao.taedibear.local` 형식의 내부용 이메일을 생성해 저장합니다.

---

### GET /api/auth/naver

네이버 OAuth 로그인 페이지로 리다이렉트합니다.

**인증:** 불필요  
**구현 방식:** `NaverOAuthClient.java`에서 Spring WebClient로 직접 OAuth 흐름을 처리합니다.  
**Response:** 네이버 로그인 페이지로 302 리다이렉트

---

### GET /api/auth/naver/callback

네이버 OAuth 콜백 처리 후 JWT를 발급합니다.

**인증:** 불필요  
**Response:** 프론트엔드로 리다이렉트 (`/auth?token=<JWT>`)

**참고:** 이메일 제공 동의를 받지 못한 경우 `naver_<id>@naver.taedibear.local` 형식의 내부용 이메일을 생성해 저장합니다.

---

### POST /api/auth/refresh

JWT 토큰을 갱신합니다.

**인증:** 필요 (만료 토큰도 허용)

**Response 200:**
```json
{
  "success": true,
  "data": { "token": "eyJhbGci..." }
}
```

---

### POST /api/auth/logout

로그아웃합니다. (클라이언트에서 토큰 삭제 유도)

**인증:** 필요

**Response 200:**
```json
{ "success": true }
```

---

## 2. 인스타그램 연동 API (`/api/instagram`)

---

### GET /api/instagram/connect

Meta OAuth 연동 페이지로 리다이렉트합니다.

**인증:** 필요 — 단, 브라우저 전체 페이지 리다이렉트이므로 Authorization 헤더 대신 `?token=<JWT>` 쿼리 파라미터로 JWT를 전달합니다.  
**Response:** Meta 권한 승인 페이지로 302 리다이렉트

---

### GET /api/instagram/callback

Meta OAuth 콜백 처리 후 계정을 저장합니다.

**인증:** 필요  
**Response:** 설정 페이지로 리다이렉트 (`/settings?connected=true`)

---

### GET /api/instagram/accounts

연동된 인스타그램 계정 목록을 조회합니다.

**인증:** 필요

**Response 200:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "instagram_user_id": "12345678",
      "username": "my_cafe_seoul",
      "connected_at": "2025-03-01T10:00:00Z"
    }
  ]
}
```

---

### DELETE /api/instagram/accounts/:id

인스타그램 계정 연동을 해제합니다.

**인증:** 필요

**Response 200:**
```json
{ "success": true }
```

**Error 403:**
```json
{ "success": false, "error": "본인 계정만 해제할 수 있어요." }
```

---

## 3. 게시물 API (`/api/posts`)

---

### POST /api/posts/upload

이미지를 S3에 업로드하고 URL을 반환합니다.

**인증:** 필요  
**Content-Type:** `multipart/form-data`

**Request Body:**
```
image: <File>  (JPG, PNG, WEBP, 최대 10MB)
```

**Response 200:**
```json
{
  "success": true,
  "data": {
    "image_url": "https://s3.amazonaws.com/taedibear/images/abc123.jpg"
  }
}
```

**Error 400:**
```json
{ "success": false, "error": "지원하지 않는 파일 형식이에요. JPG, PNG, WEBP만 가능해요." }
```

---

### POST /api/posts/caption

Gemini API로 캡션과 해시태그를 생성합니다.

**인증:** 필요  
**사용 모델:** `gemini-2.5-flash` (무료 플랜 사용, 2026-06-29 기준 `gemini-2.0-flash` deprecated로 변경)  
**무료 한도:** 분당 15회 요청

**Request Body:**
```json
{
  "image_url": "https://s3.amazonaws.com/taedibear/images/abc123.jpg",
  "business_type": "카페",
  "mood": "감성적인"
}
```

**Response 200:**
```json
{
  "success": true,
  "data": {
    "caption": "오늘도 한 잔의 커피와 함께 여유로운 하루를 시작해요 ☕ 창밖으로 보이는 풍경이 마음을 따뜻하게 해주는 곳, 저희 카페에서 잠시 쉬어가세요.",
    "hashtags": ["#카페", "#커피", "#감성카페", "#카페스타그램", "#홍대카페", "#서울카페", "#커피스타그램", "#카페투어", "#핸드드립", "#일상"]
  }
}
```

**Error 429 — 요청 한도 초과:**
```json
{ "success": false, "error": "잠시 후 다시 시도해주세요." }

---

### POST /api/posts

게시물을 저장합니다. (draft 상태로 저장)

**인증:** 필요

**Request Body:**
```json
{
  "instagram_account_id": 1,
  "image_url": "https://s3.amazonaws.com/taedibear/images/abc123.jpg",
  "caption": "오늘도 한 잔의 커피와 함께...",
  "hashtags": ["#카페", "#커피", "#감성카페"]
}
```

**Response 201:**
```json
{
  "success": true,
  "data": {
    "id": 42,
    "status": "draft",
    "created_at": "2025-03-15T09:00:00Z"
  }
}
```

---

### GET /api/posts

게시물 목록을 조회합니다. (히스토리)

**인증:** 필요

**Query Parameters:**

| 파라미터 | 타입 | 기본값 | 설명 |
|---------|------|--------|------|
| `status` | string | 전체 | draft / scheduled / posted / failed |
| `page` | int | 1 | 페이지 번호 |
| `limit` | int | 20 | 페이지당 개수 |

**Response 200:**
```json
{
  "success": true,
  "data": {
    "posts": [
      {
        "id": 42,
        "image_url": "https://...",
        "caption": "오늘도 한 잔의...",
        "status": "posted",
        "posted_at": "2025-03-15T10:00:00Z",
        "scheduled_at": null
      }
    ],
    "total": 15,
    "page": 1,
    "limit": 20
  }
}
```

---

### GET /api/posts/:id

게시물 상세를 조회합니다.

**인증:** 필요

**Response 200:**
```json
{
  "success": true,
  "data": {
    "id": 42,
    "image_url": "https://...",
    "caption": "오늘도 한 잔의...",
    "hashtags": ["#카페", "#커피"],
    "status": "posted",
    "instagram_post_id": "17854360229135492",
    "posted_at": "2025-03-15T10:00:00Z"
  }
}
```

---

### PUT /api/posts/:id

게시물 내용을 수정합니다. (posted 상태는 수정 불가)

**인증:** 필요

**Request Body:**
```json
{
  "caption": "수정된 캡션 내용",
  "hashtags": ["#카페", "#커피", "#수정된태그"]
}
```

**Response 200:**
```json
{ "success": true, "data": { "id": 42 } }
```

---

### DELETE /api/posts/:id

게시물을 삭제합니다.

**인증:** 필요

**Response 200:**
```json
{ "success": true }
```

---

### POST /api/posts/:id/publish

게시물을 즉시 인스타그램에 업로드합니다.

**인증:** 필요

**Response 200:**
```json
{
  "success": true,
  "data": {
    "instagram_post_id": "17854360229135492",
    "posted_at": "2025-03-15T10:00:00Z"
  }
}
```

**Error 500:**
```json
{ "success": false, "error": "인스타그램 업로드에 실패했어요. 다시 시도해주세요." }
```

---

## 4. 예약 API (`/api/scheduled`)

---

### POST /api/scheduled

업로드를 예약합니다.

**인증:** 필요

**Request Body:**
```json
{
  "post_id": 42,
  "scheduled_at": "2025-03-20T10:00:00Z"
}
```

**유효성 검사:**
- `scheduled_at`: 현재 시간 + 10분 이후여야 함

**Response 201:**
```json
{
  "success": true,
  "data": {
    "id": 7,
    "post_id": 42,
    "scheduled_at": "2025-03-20T10:00:00Z",
    "status": "pending"
  }
}
```

---

### GET /api/scheduled

예약 목록을 조회합니다.

**인증:** 필요

**Response 200:**
```json
{
  "success": true,
  "data": [
    {
      "id": 7,
      "post_id": 42,
      "scheduled_at": "2025-03-20T10:00:00Z",
      "status": "pending",
      "post": {
        "image_url": "https://...",
        "caption": "오늘도 한 잔의..."
      }
    }
  ]
}
```

---

### PUT /api/scheduled/:id

예약 시간을 변경합니다.

**인증:** 필요

**Request Body:**
```json
{ "scheduled_at": "2025-03-21T14:00:00Z" }
```

**Response 200:**
```json
{ "success": true, "data": { "id": 7, "scheduled_at": "2025-03-21T14:00:00Z" } }
```

---

### DELETE /api/scheduled/:id

예약을 취소합니다.

**인증:** 필요

**Response 200:**
```json
{ "success": true }
```

---

## 5. 사용자 API (`/api/users`)

---

### GET /api/users/me

내 프로필을 조회합니다.

**인증:** 필요

**Response 200:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "김성태",
    "email": "taedibear@email.com",
    "plan": "free",
    "created_at": "2025-03-01T10:00:00Z"
  }
}
```

---

### PUT /api/users/me

내 프로필을 수정합니다.

**인증:** 필요

**Request Body:**
```json
{ "name": "Taedibear" }
```

**Response 200:**
```json
{ "success": true, "data": { "id": 1, "name": "Taedibear" } }
```

---

## 6. 분석 API (`/api/analytics`)

---

### GET /api/analytics/summary

**설명:** 로그인 사용자의 업로드 통계를 반환합니다. Meta API 없이 서비스 DB만 사용.

**인증:** 필요 (Bearer Token)

**요청 파라미터:** 없음

**응답 예시:**

```json
{
  "success": true,
  "data": {
    "monthly_uploads": [
      { "month": "2025-08", "count": 3 },
      { "month": "2025-09", "count": 0 },
      { "month": "2025-10", "count": 7 },
      "...(최근 12개월, 빈 달 포함)"
    ],
    "success_rate": 87.5,
    "scheduled_ratio": 60.0,
    "daily_pattern": [
      { "day": "Sun", "count": 2 },
      { "day": "Mon", "count": 5 },
      { "day": "Tue", "count": 3 },
      { "day": "Wed", "count": 8 },
      { "day": "Thu", "count": 4 },
      { "day": "Fri", "count": 6 },
      { "day": "Sat", "count": 1 }
    ],
    "this_month_used": 5
  }
}
```

**필드 설명:**

| 필드 | 타입 | 설명 |
|------|------|------|
| `monthly_uploads` | array | 최근 12개월 월별 업로드 수. 빈 달은 `count: 0` |
| `monthly_uploads[].month` | string | "YYYY-MM" 형식 |
| `monthly_uploads[].count` | number | 해당 월 게시물 수 |
| `success_rate` | number | posted / (posted + failed) × 100, 소수점 1자리 |
| `scheduled_ratio` | number | 예약 경험 post / 전체 post × 100, 소수점 1자리 |
| `daily_pattern` | array | 업로드 완료(posted) 기준 요일별 집계 |
| `daily_pattern[].day` | string | "Sun" ~ "Sat" |
| `daily_pattern[].count` | number | 해당 요일 업로드 완료 수 |
| `this_month_used` | number | 이번 달 총 게시물 수 |

**구현 참고:**
- `monthly_uploads`: `YEAR(p.createdAt)`, `MONTH(p.createdAt)` grouping, 12개월 LinkedHashMap으로 빈 달 보정
- `success_rate`: `PostStatus.posted / (posted + failed)` — 0건이면 0.0 반환
- `scheduled_ratio`: `COUNT(DISTINCT sp.postId) / total_posts` — `ScheduledPostRepository` 활용
- `daily_pattern`: MySQL `FUNCTION('DAYOFWEEK', p.postedAt)` (1=Sun ~ 7=Sat)

---

## 7. 변경 이력

| 버전 | 날짜 | 변경 내용 | 작성자 |
|------|------|----------|--------|
| v1.0 | 2026-06-27 | 최초 작성 | @taedibear |
| v1.1 | 2026-06-27 | AI 캡션 생성 모델 ChatGPT → Gemini 변경 | @taedibear |
| v1.2 | 2026-07-01 | 개발 서버 포트 4000 → 8080 (Spring Boot) / GET /api/auth/naver 구현 방식 Node.js/passport → Spring NaverOAuthClient.java 수정 / GET /api/instagram/connect 인증 방식 명시 (?token= 쿼리 파라미터) | @taedibear |
| v1.3 | 2026-07-01 | GET /api/analytics/summary 신규 추가 (Phase 1-3 분석 대시보드) | @taedibear |
