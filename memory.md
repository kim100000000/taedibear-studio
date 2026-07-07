# memory.md — 작업 기록 (최신이 위)

> 사용법: 세션이 끝날 때마다 "오늘 한 일 / 결정한 것 / 다음 할 일"을 여기에 추가.
> Claude에게 "memory.md 읽고 이어서 하자"라고 하면 맥락이 이어짐.

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
