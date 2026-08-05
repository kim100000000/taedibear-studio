# memory.md — 작업 기록 (최신이 위)

> 사용법: 세션이 끝날 때마다 "오늘 한 일 / 결정한 것 / 다음 할 일"을 여기에 추가.
> Claude에게 "memory.md 읽고 이어서 하자"라고 하면 맥락이 이어짐.

## 2026-08-06
- 한 일:
  - S3 과금 폭탄 방지: 하루 총 업로드 100MB 한도 추가 (`domain/DailyUploadQuota`, `repository/DailyUploadQuotaRepository`, `post/UploadQuotaService`) — `S3Service.uploadImage()`가 S3에 닿기 전 `reserve()`로 먼저 체크, 초과 시 429
    - 날짜가 PK라 자정 지나면 자동 리셋, `tryReserve` JPQL로 원자적 증가(동시 요청 레이스 방지)
    - 버그 발견·수정: JPQL bulk UPDATE가 영속성 컨텍스트를 안 거쳐서 같은 트랜잭션 내 재조회 시 stale 값 반환 — `@Modifying(clearAutomatically = true)`로 해결
    - 테스트 추가: `UploadQuotaServiceTest`(Mockito, 한도 통과/초과/동시생성 케이스), `DailyUploadQuotaRepositoryTest`(로컬 MySQL 대상 `@DataJpaTest`, 경계값 포함) — 전체 통과 확인, `./gradlew bootRun`으로 앱 정상 기동도 확인
    - `docs/05_API명세서.md`에 `/api/posts/upload` 429 에러 케이스 추가
  - 브랜치 `feat/daily-upload-quota`에서 작업 (develop이 기본 브랜치라 분리)
- 다음:
  - rate limit(요청 빈도 제한) 자체는 아직 없음 — 이번 건은 하루 총 용량 상한이라는 최후 안전장치일 뿐, 근본적인 rate limit 도입은 별도 작업
  - PR 올리고 develop 머지 검토

## 2026-07-09
- 한 일:
  - 🔴 버그 수정: 리뷰 "@?" → "(알 수 없는 사용자)" 문구 / 인스타에서 삭제된 댓글을 동기화 시 정리(`ReviewService`, 조회 실패 게시물은 오삭제 방지로 건너뜀)
  - 🟠 저비용 고효율 6건 구현:
    - 업로드 진행률 바(`onUploadProgress`) + 편집본 10MB 초과 시 품질 0.75 자동 재인코딩 + 실패 원인별 메시지(네트워크/413/서버) — "바로 업로드 실패" 원인 후보: 서버 multipart 10MB 한도 초과(canvas 재인코딩으로 원본보다 커질 수 있음) 또는 네트워크 오류였는데 메시지가 뭉뚱그려져 있었음
    - 리뷰 자동 답글 ON/OFF 토글 스위치 + 상태 설명 문구 (SettingsPage)
    - 캘린더에 즉시 발행 게시물 표시(posted_at 기준, 예약 발행분 중복 제거)
    - 캡션 화면 "임시 저장" 버튼 → 히스토리 "임시 저장" 탭(신규) + "지금 발행" 버튼
    - 캡션 화면 좌측을 인스타 피드 스타일 실시간 미리보기로 교체(계정명/캡션/해시태그 라이브 반영)
    - `spring.task.scheduling.pool.size=4` (스케줄러 잡 5종 단일 스레드 병목 해소)
  - CHECKLIST에 Phase 6(UX/품질 개선) 신설 — 개선백로그 연동, 출시 전 필수(비밀번호 찾기/이메일 인증) 명시
  - **비밀번호 찾기 구현**: `auth_tokens` 테이블(SHA-256 해시, 일회성) + `AuthTokenService`, POST /forgot-password(존재 여부 비노출)·/reset-password(성공 시 전 세션 폐기), 프론트 /forgot-password·/reset-password 페이지, 로그인 페이지 완료 배너
  - **회원가입 이메일 인증 구현**: `users.email_verified`(columnDefinition default 1 → 기존 유저 잠금 방지, 소셜 가입 자동 true), 가입 시 인증 메일(24h), POST /verify-email·/resend-verification, 미인증 시 캡션 생성 403(`UserService.assertEmailVerified`), 대시보드 미인증 배너+재발송, /verify-email 페이지
  - 리뷰 관리 게시글별 그룹핑 → 🟡 백로그 추가 (media_id 기반, 동의)
  - frontend `tsc` 통과. 백엔드(Java)는 여전히 이 환경에서 컴파일 불가 — push 후 CI 확인 필요
- 주의: 메일 발송은 MAIL_HOST/MAIL_USERNAME/MAIL_PASSWORD/MAIL_FROM 환경변수가 Railway에 설정돼 있어야 실제로 나감 — 배포 후 회원가입/비번찾기 메일 실발송 테스트 필요 (Gmail은 앱 비밀번호 필요)
- 🔴 크레딧 버그 수정: `User.credits` 기본값 5 → 3 정정 (정책: 가입 3 + 온보딩 2 = 5인데 5+2=7이 지급되던 문제). 이미 7개 받은 기존 테스트 계정은 DB에서 수동 정정 필요
- 🟡 중간 규모 3건 구현 (07-09 2차):
  - 리뷰 게시글별 그룹핑 — ReviewsPage에서 media_id로 그룹, 게시물 썸네일/캡션 헤더(posts.instagram_post_id 매칭), 미답변 뱃지, 접기/펼치기
  - 캡션 보관함 — `saved_captions` 테이블(쉼표 해시태그, 최대 50개) + `/api/captions` CRUD + 캡션 화면 "캡션 저장"/"보관함" 모달(사용·삭제). 탈퇴 시 정리 포함
  - 공지사항+문의 — `notices` 테이블 + `GET /api/notices`(전체 유저) + 관리자 작성·삭제(`/api/admin/notices`, AdminPage 섹션) + `/notices` 페이지(아코디언+문의 이메일 링크) + NavBar "공지"
- 쿠팡 파트너스/제휴 수익화 아이디어 논의 — 의견만 전달, 백로그 미반영 (사용자 결정 대기)
- 브랜치 전략 확인: 작업/배포 기준은 **develop** (main은 구버전에 정체). CI를 main+develop 감시로 수정, 푸시는 `git push origin develop --tags`. 안정 시점에 develop→main 머지 권장
  - 사용자 피드백 2차분 백로그 반영 (`docs/TODO/개선백로그.md`) — 비밀번호 찾기·이메일 인증(출시 전 필수), 캡차, 업로드 진행 표시, 다중 이미지/캐러셀, 스케줄러 스레드 풀, DM 일괄 발송(⚠️ Meta 정책 리스크로 대안 제시)
  - 코드 확인 결과 기록: Gemini는 S3 이미지 1장을 base64로 받아 캡션 생성(다중 이미지 미지원), API 동시 처리는 Tomcat 풀 + 크레딧 원자적 차감, `@Scheduled` 잡 5종이 단일 스레드 공유(개선 필요)
  - `docs/12_시스템아키텍처.md` 신규 — mermaid 구조도 + 컴포넌트/요청 흐름/동시성/보안
  - `docs/13_CICD계획서.md` 신규 — 현재 상태(CI 파일 작성됨·미push, CD는 Vercel/Railway 연동 시 동작 중) + 3단계 도입 계획
- 다음:
  - git push → Actions 첫 CI 통과 확인 → main 브랜치 보호 설정
  - Railway healthcheck(`/health`) 설정, Vercel/Railway GitHub 자동 배포 연동 여부 확인

## 2026-07-08 (계속)
- 한 일:
  - 캡션 500 수정 배포 확인 — AI 캡션 생성 정상 작동 확인됨.
  - 새 버그 발견/수정: 즉시 업로드(`POST /api/posts/:id/publish`)가 500 — Railway 로그로 `WebClientResponseException$BadRequest: 400 ... POST .../media_publish` 확인. 미디어 컨테이너 생성 직후 바로 `media_publish`를 호출해서(폴링 없음) Meta가 이미지 다운로드/처리를 끝내기 전에 발행 시도 → "Media ID is not available"류 400.
  - 수정: `MetaApiClient.publishToInstagram()`에 `waitUntilContainerReady()` 추가 — `status_code`가 `FINISHED`될 때까지 최대 15초(1초 간격) 폴링 후 `media_publish` 호출. `InstagramPublishService`는 `WebClientResponseException` 발생 시 Meta가 준 실제 응답 바디를 로그에 남기도록 catch 분기 추가(기존엔 body가 로그에 안 찍혀서 원인 파악이 어려웠음).
  - Railway 리소스 이슈 발견: Metrics에서 메모리가 컨테이너 한도(1GB)에 계속 붙어있다가 한 번 죽었다 재시작하는 패턴 확인 — `nixpacks.toml`에 `-XX:MaxRAMPercentage=70.0` 추가. "Scale and grow - Upgrade to Hobby" 배너로 봐서 현재 플랜이 Hobby 미만(Trial 추정)이고 크레딧도 얼마 안 남음(당시 $2.91/21일) — 지속 모니터링 필요.

- 이전 항목 (진단만, 아래 참고):
  - 버그 진단: `POST /api/posts/caption` 500 에러 — Railway 스택트레이스로 `GeminiService.downloadImage()`에서 터지는 것 확인. `S3Service.uploadImage()`가 객체를 public-read로 안 올리는데, `GeminiService`가 업로드된 이미지를 다시 받아올 때 인증 없는 공개 URL GET을 시도해서(버킷 비공개 시) S3 403 → 처리되지 않은 예외로 500.
  - 버그 수정: `S3Service`에 `downloadImage(imageUrl)` 추가 — 서버가 이미 가진 AWS 자격증명으로 `S3Client.getObject()`를 직접 호출해 읽어오도록 변경(버킷 public-read 불필요, 더 안전). `GeminiService`는 이제 이 메서드를 사용, 자체 `webClient` 공개 GET 로직 제거.
  - 점검: refresh token DB에 여러 행이 쌓이는 문제 — 원인은 `AUTH_COOKIE_SAME_SITE`가 cross-site(Vercel↔Railway)에서 쿠키가 전달되도록 `None`(+`AUTH_COOKIE_SECURE=true`)이어야 하는데, 확인 과정에서 값이 왔다갔다 보고돼 실제 값 재확인 필요 — Railway Variables 탭에서 직접 확인 요망.
  - Gemini 관련: `gemini-2.5-flash`는 아직 서비스 중(공식 종료 예정일 2026-10-16), Google AI Pro(개인 구독) 해지와 API 키 사용은 무관 — 500 원인 아님으로 확인.

- 다음:
  - 인스타그램 즉시 업로드 재배포 후 재테스트 (postId=6, 7 실패 건 — 컨테이너 폴링 수정으로 해결되는지 확인)
  - Railway 플랜/크레딧 확인 — Metrics에 "Upgrade to Hobby" 배너, 잔여 크레딧 얼마 안 남음. 메모리도 한도 근접해서 크래시 이력 있음 → 플랜 업그레이드 검토
  - Railway Variables `AUTH_COOKIE_SAME_SITE`/`AUTH_COOKIE_SECURE` 값 최종 확인 (대화 중 계속 값이 바뀌어 보고돼 혼선 있었음 — None + true로 확정 필요)
  - 리뷰 자동 답글이 특정 commentId(18236133076311924)에 30분마다 계속 400 Bad Request(Facebook Graph API)로 실패 중 — 별도로 원인 확인 필요(댓글 삭제됨/권한 부족 등)
  - 상대방(테스터)을 Meta 앱 대시보드 "앱 역할 > 역할"에서 초대해서 실사용 테스트 진행 예정

## 2026-07-06
- 한 일:
  - CLAUDE.md / memory.md 생성, 프로젝트 지침 체계 정리
  - Meta OAuth "Invalid Scopes" 오류 원인 파악·해결: 앱이 "Instagram API with Instagram Login"으로 설정돼 있어 코드가 요청하는 스코프가 거부되던 문제 → "Facebook 로그인이 포함된 API 설정 > 권한 및 기능"에서 필요 권한 5개 추가해 해결, 인스타그램 계정 실연동 성공
  - 버그 수정: "즉시 업로드" 시 draft로만 저장되고 실제 인스타 발행은 안 되던 문제 → `CaptionPage.tsx`에 누락된 `publishPost` 호출 추가
  - 버그 수정: "새 게시물 만들기" 버튼 UI가 다른 버튼과 다르게 보이던 문제 → `.btn-primary` CSS가 `<Link>`(a태그)에도 정상 적용되도록 수정
  - Phase 4 구현 (코드 작성):
    - 4-1 멀티 계정 관리 — 히스토리/분석 화면에 계정별 필터 추가
    - 4-2 분석 대시보드 고도화 — 팔로워 추이 일별 스냅샷(`FollowerSnapshotScheduler`, 매일 새벽 3시), 게시물별 인사이트(조회수/좋아요/저장) 조회
    - 4-3 리뷰 자동 답글 — 댓글 동기화 + Gemini 답글 초안 생성 + 수동 승인/자동 발행 선택(계정별 토글), 신규 테이블 `follower_snapshots`, `review_comments`, `instagram_accounts.auto_reply_enabled` 컬럼 추가
  - 문서 갱신: CHECKLIST.md, 05_API명세서.md, 06_트러블슈팅.md, 04_DB설계서.md
  - `.env.example`에 실수로 미사용 `META_ACCESS_TOKEN` 줄을 지웠다가 복원 — 실제 `.env`에는 존재하나 코드에서 아직 참조하지 않음, 용도 확인 필요
  - 깃 커밋·푸시 → Vercel+Railway 배포 완료, 인스타그램 실제 업로드 확인 완료
  - backend-node 삭제 — 태그 `backend-node-final` 생성 후 폴더 제거, README 전면 갱신(Java 기준), CLAUDE.md·보안보고서 M11 정리, "동일 명세 유지" 규칙 삭제
  - Phase 5-1 관리자 대시보드 구현:
    - 백엔드: `admin/` 패키지 신규 — `GET /api/admin/summary`(유저/게시물/예약/매출), `/users`(검색+페이지네이션, 유저별 게시물 수), `/payments`(결제 내역). 관리자 = `ADMIN_EMAIL` 이메일 일치 계정 → `UserPrincipal.admin` + ROLE_ADMIN, SecurityConfig `/api/admin/**` 보호, `GET /api/users/me`에 `is_admin` 추가
    - 프론트: `/admin` 라우트 + `AdminPage.tsx`(요약 카드/유저 테이블/결제 테이블), NavBar 관리자 메뉴(is_admin일 때만), ko/en 번역, admin-table CSS. `tsc` 통과
  - `docs/10_수익화전략.md` 신규 작성 — 가격/퍼널/채널별 전략, 워터마크 크레딧 보상 구현 스케치, 단계별 로드맵
  - `docs/11_Meta앱심사준비.md` 신규 작성 — 심사 체크리스트, 7개 scope별 영문 설명문 초안, 스크린캐스트 시나리오, 반려 대응
  - `.github/workflows/ci.yml` 신규 — push/PR마다 백엔드 `gradlew build -x test` + 프론트 `tsc` 자동 검증 (Claude 작업 환경에서 JDK/Maven 접근 불가 → CI가 수동 빌드 확인을 대체)

- 결정:
  - 지침(앱 설정)에는 어조·규칙만, 프로젝트 상세는 CLAUDE.md, 진행 기록은 memory.md
  - 리뷰 자동 답글은 기본값을 "수동 승인" 모드로 — 계정별 토글로 완전 자동 전환 가능, 오탐 답글이 확인 없이 나가는 것 방지

- 다음:
  - Railway 환경변수에 `ADMIN_EMAIL` 설정 (없으면 관리자 대시보드 접근 불가) — 첫 push 후 CI(Actions)에서 백엔드 빌드 통과 확인
  - Meta 앱 심사 준비: 테스터 초대 → 사업자등록 → 비즈니스 인증 → 스크린캐스트 (docs/11 참고). `business_management` scope가 실제로 필요한지 코드 확인 후 불필요하면 제거
  - 워터마크 크레딧 보상 구현 (docs/10_수익화전략.md 7절 스케치대로 — 다음 우선순위)
  - 새 권한(`instagram_manage_insights`, `instagram_manage_comments`) 반영 위해 인스타 계정 재연동
  - Phase 4-2(팔로워 추이·인사이트)/4-3(리뷰 자동 답글) 재연동 후 실사용 테스트
  - `.env`의 `META_ACCESS_TOKEN` 실제 용도 확인 후 코드 연결 여부 결정
  - Meta 앱 심사(App Review) 제출 검토 (현재 Standard Access만 사용 가능 — 관리자/개발자/테스터 계정 한정)

## 진행 중 이슈
- 사용자 피드백 백로그 정리 → `docs/TODO/개선백로그.md` (2026-07-06). 버그 2건(리뷰 @? 표시, 삭제 댓글 미반영) 포함
- Phase 4-2/4-3: 코드 작성 완료, 백엔드는 이 세션 환경에 JDK17/Gradle이 없어 컴파일 미검증 (배포 전 `./gradlew build` 확인 필요) — 배포까지는 완료되었으나 재연동 전이라 실사용 테스트는 아직
- `.env`의 `META_ACCESS_TOKEN` 미사용 상태 — 용도 확인 필요

## 주요 결정 사항 (누적)
- 실제 운영 백엔드는 Java(Spring Boot, `backend-java/`) — Railway 배포 설정(`nixpacks.toml`)이 이걸 빌드·실행함. `backend-node/`는 2026-07-06 삭제 (태그 `backend-node-final`로 복구 가능)
- 관리자 지정 방식: DB 컬럼 없이 `ADMIN_EMAIL` 환경변수 = 이메일 일치 (1인 운영 전제, 2026-07-06)
- AI 모델: gemini-2.5-flash
- 리뷰 자동 답글 기본값: 수동 승인 모드 (2026-07-06)
