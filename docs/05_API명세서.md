# Taedibear Studio — API 명세서

**버전:** v1.4  
**작성일:** 2026-06-27  
**수정일:** 2026-07-05 — C4 refresh token 체계 / C6 JWT URL 노출 제거 반영  
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
**Response:** refresh token을 HttpOnly 쿠키(`refresh_token`)로 발급 후 프론트엔드 `/auth`로 리다이렉트 (C6: JWT를 URL에 노출하지 않음 — 프론트가 `POST /api/auth/refresh`로 access token 교환)

---

### GET /api/auth/kakao

카카오 OAuth 로그인 페이지로 리다이렉트합니다.

**인증:** 불필요  
**Response:** 카카오 로그인 페이지로 302 리다이렉트

---

### GET /api/auth/kakao/callback

카카오 OAuth 콜백 처리 후 JWT를 발급합니다.

**인증:** 불필요  
**Response:** refresh token을 HttpOnly 쿠키(`refresh_token`)로 발급 후 프론트엔드 `/auth`로 리다이렉트 (C6: JWT를 URL에 노출하지 않음 — 프론트가 `POST /api/auth/refresh`로 access token 교환)

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
**Response:** refresh token을 HttpOnly 쿠키(`refresh_token`)로 발급 후 프론트엔드 `/auth`로 리다이렉트 (C6: JWT를 URL에 노출하지 않음 — 프론트가 `POST /api/auth/refresh`로 access token 교환)

**참고:** 이메일 제공 동의를 받지 못한 경우 `naver_<id>@naver.taedibear.local` 형식의 내부용 이메일을 생성해 저장합니다.

---

### POST /api/auth/refresh

refresh token으로 새 access token을 발급합니다. (C4)

**인증:** HttpOnly 쿠키 `refresh_token` (요청 시 `withCredentials` 필요)  
**동작:** DB에 저장된 refresh token을 검증 후 **회전(rotation)** — 사용한 토큰은 즉시 폐기되고 새 refresh 쿠키가 재발급됩니다. 유효하지 않거나 만료된 경우 401.

> 로그인/회원가입/소셜 로그인 시 refresh token(기본 14일)이 HttpOnly 쿠키로 발급됩니다. access token 수명은 30분으로 단축되었습니다.

**Response 200:**
```json
{
  "success": true,
  "data": { "token": "eyJhbGci..." }
}
```

**Error 401:**
```json
{ "success": false, "error": "세션이 만료됐어요. 다시 로그인해주세요." }
```

---

### POST /api/auth/logout

로그아웃합니다. 서버에서 refresh token을 폐기하고 쿠키를 삭제합니다. (C4/M9)

**인증:** HttpOnly 쿠키 `refresh_token` (없어도 200 — 쿠키 삭제만 수행)

**Response 200:**
```json
{ "success": true }
```

---

## 2. 인스타그램 연동 API (`/api/instagram`)

---

### GET /api/instagram/connect-url

Meta OAuth 연동 URL을 반환합니다. (C6: 기존 `GET /connect?token=<JWT>` 방식은 JWT URL 노출 문제로 제거)

**인증:** 필요 (Authorization 헤더)  
**동작:** 일회성 랜덤 nonce(TTL 10분)를 state로 심은 Meta 로그인 URL을 반환하며, 프론트가 이 URL로 이동합니다.

**Response 200:**
```json
{
  "success": true,
  "data": { "url": "https://www.facebook.com/v19.0/dialog/oauth?..." }
}
```

---

### GET /api/instagram/callback

Meta OAuth 콜백 처리 후 계정을 저장합니다.

**인증:** 불필요 (state nonce로 사용자 복원 — 유효하지 않으면 연동 실패)  
**Response:** 설정 페이지로 리다이렉트 (`/settings?connected=true`, 실패 시 `connected=false`)

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
      "connected_at": "2025-03-01T10:00:00Z",
      "auto_reply_enabled": false
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

### PUT /api/instagram/accounts/:id/auto-reply

Phase 4-3: 리뷰(댓글) 자동 답글 사용 여부를 계정별로 켜고 끕니다.

**인증:** 필요

**Request Body:**
```json
{ "enabled": true }
```

**Response 200:**
```json
{ "success": true, "data": { "auto_reply_enabled": true } }
```

---

### GET /api/instagram/accounts/:id/comments

Phase 4-3: 최근 게시물 5개의 댓글을 Meta에서 가져와 로컬(`review_comments`)에 동기화하고, AI 제안 답글과 함께 목록으로 반환합니다.

**인증:** 필요

**Response 200:**
```json
{
  "success": true,
  "data": [
    {
      "id": 10,
      "media_id": "1784...",
      "comment_text": "여기 진짜 맛있어요!",
      "username": "customer_a",
      "suggested_reply": "방문해주셔서 감사해요! 다음에 또 뵙길 바라요 :)",
      "status": "pending",
      "created_at": "2026-07-06T09:00:00Z"
    }
  ]
}
```

---

### POST /api/instagram/comments/:id/reply

승인(또는 수정)한 답글 내용을 실제로 Meta에 발행합니다. `:id`는 위 댓글 목록의 `id`(로컬 review_comments PK)입니다.

**인증:** 필요

**Request Body:**
```json
{ "message": "방문해주셔서 감사해요! 다음에 또 뵙길 바라요 :)" }
```

**Response 200:**
```json
{ "success": true, "data": { "success": true } }
```

---

### POST /api/instagram/comments/:id/skip

이 댓글은 답글을 달지 않고 건너뜁니다.

**인증:** 필요

**Response 200:**
```json
{ "success": true, "data": { "success": true } }
```

---

**Meta 권한 참고 (Phase 4-2/4-3 추가):** 이 기능을 쓰려면 Meta 앱 대시보드의 "Facebook 로그인이 포함된 API 설정 > 권한 및 기능"에 `instagram_manage_insights`(인사이트), `instagram_manage_comments`(댓글 답글) 권한을 추가하고, 기존에 연동된 계정은 **다시 연동(재연결)**해야 새 권한이 반영된 토큰을 받습니다. (`MetaApiClient.getLoginUrl`의 scope 목록에 두 권한을 이미 추가해둠)

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
| `instagram_account_id` | int | 전체 계정 | Phase 4-1: 특정 계정의 게시물만 조회 |
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
        "instagram_account_id": 1,
        "image_url": "https://...",
        "caption": "오늘도 한 잔의...",
        "status": "posted",
        "instagram_post_id": "17854360229135492",
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

### GET /api/posts/:id/insights

Phase 4-2: 발행된(posted) 게시물의 조회수/도달/좋아요/댓글 수를 Meta에서 조회합니다.

**인증:** 필요

**Response 200:**
```json
{
  "success": true,
  "data": {
    "engagement": 12,
    "impressions": 340,
    "reach": 210,
    "like_count": 9,
    "comments_count": 3
  }
}
```

**Error 400:**
```json
{ "success": false, "error": "인스타그램에 업로드된 게시물만 인사이트를 볼 수 있어요." }
```

**Error 500 (권한 미등록):**
```json
{ "success": false, "error": "인사이트를 불러오지 못했어요. 계정에 instagram_manage_insights 권한이 있는지 확인하고, 권한을 새로 추가했다면 설정에서 인스타그램 계정을 다시 연동해주세요." }
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

**Query Parameters:**

| 파라미터 | 타입 | 기본값 | 설명 |
|---------|------|--------|------|
| `instagram_account_id` | int | 전체 계정 합산 | Phase 4-1: 특정 계정만 집계 |

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
    "this_month_used": 5,
    "follower_trend": [
      { "date": "2026-06-07", "followers": 512 },
      { "date": "2026-06-08", "followers": 515 },
      "...(최근 30일 스냅샷)"
    ]
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
| `follower_trend` | array | Phase 4-2: 최근 30일 팔로워 수 스냅샷 (매일 새벽 3시 `FollowerSnapshotScheduler`가 기록) |
| `follower_trend[].date` | string | "YYYY-MM-DD" |
| `follower_trend[].followers` | number | 해당 날짜 팔로워 수 (계정 미지정 시 전체 계정 합산) |

**구현 참고:**
- `monthly_uploads`: `YEAR(p.createdAt)`, `MONTH(p.createdAt)` grouping, 12개월 LinkedHashMap으로 빈 달 보정
- `success_rate`: `PostStatus.posted / (posted + failed)` — 0건이면 0.0 반환
- `scheduled_ratio`: `COUNT(DISTINCT sp.postId) / total_posts` — `ScheduledPostRepository` 활용
- `daily_pattern`: MySQL `FUNCTION('DAYOFWEEK', p.postedAt)` (1=Sun ~ 7=Sat)
- `follower_trend`: Meta Insights API는 팔로워 수 추이를 직접 제공하지 않아, 매일 현재 값을 `follower_snapshots` 테이블에 기록해 누적 (instagram_manage_insights 권한 필요 — 없는 계정은 스냅샷 실패, 로그만 남기고 계속 진행)

---

## 7. 변경 이력

| 버전 | 날짜 | 변경 내용 | 작성자 |
|------|------|----------|--------|
| v1.0 | 2026-06-27 | 최초 작성 | @taedibear |
| v1.1 | 2026-06-27 | AI 캡션 생성 모델 ChatGPT → Gemini 변경 | @taedibear |
| v1.2 | 2026-07-01 | 개발 서버 포트 4000 → 8080 (Spring Boot) / GET /api/auth/naver 구현 방식 Node.js/passport → Spring NaverOAuthClient.java 수정 / GET /api/instagram/connect 인증 방식 명시 (?token= 쿼리 파라미터) | @taedibear |
| v1.3 | 2026-07-01 | GET /api/analytics/summary 신규 추가 (Phase 1-3 분석 대시보드) | @taedibear |
| v1.4 | 2026-07-06 | Phase 4-1: GET /api/posts, GET /api/analytics/summary에 instagram_account_id 필터 추가 / Phase 4-2: GET /api/posts/:id/insights 신규, follower_trend 필드 추가 / Phase 4-3: GET /api/instagram/accounts/:id/comments, POST /api/instagram/comments/:id/reply, POST /api/instagram/comments/:id/skip, PUT /api/instagram/accounts/:id/auto-reply 신규, OAuth scope에 instagram_manage_insights·instagram_manage_comments 추가 | @taedibear |
| v1.4 | 2026-07-05 | C4: POST /auth/refresh를 HttpOnly 쿠키 기반 refresh token 회전 방식으로 변경, logout 서버측 폐기 / C6: 소셜 콜백 `/auth?token=` → refresh 쿠키 + `/auth`, GET /instagram/connect 제거 → GET /instagram/connect-url 신규 | @taedibear |
