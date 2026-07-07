# 11. Meta 앱 심사(App Review) 준비

작성일: 2026-07-06 | 상태: 초안 (제출 전 최신 Meta 정책 확인 필수 — 심사 요건은 자주 바뀜)

목표: Standard Access(개발 모드, 앱 역할 보유자만 연동 가능) → **Advanced Access + 라이브 모드** 전환으로 일반 사용자도 인스타그램 연동 가능하게 만들기.

---

## 1. 전체 순서

1. 테스터 파일럿 (심사 없이 지금 가능)
2. 사업자등록 (홈택스, 개인사업자 가능) — **직접 해야 함**
3. Meta 비즈니스 인증 (Business Manager)
4. 앱 필수 정보 정비 (아래 체크리스트)
5. 스크린캐스트 촬영
6. 권한별 Advanced Access 신청 → 심사 대기 (보통 수일~2주)
7. 통과 후 앱을 라이브 모드로 전환 → 공개 홍보

---

## 2. 제출 전 체크리스트

### 앱 기본 설정 (developers.facebook.com > 앱 설정 > 기본)
- [ ] 앱 이름/아이콘 (1024×1024) — 서비스 브랜딩과 일치
- [ ] 앱 카테고리 설정
- [ ] **개인정보처리방침 URL** — `https://<프론트 도메인>/privacy` (이미 구현됨 ✅ — 배포 URL로 접속되는지, 내용에 Meta 플랫폼 데이터 사용·보관·삭제 항목이 있는지 확인)
- [ ] 서비스 약관 URL — `/terms` (구현됨 ✅)
- [ ] **데이터 삭제 안내** — 콜백 URL 또는 안내 페이지 URL 필수. 회원 탈퇴(DELETE /api/users/me) 기능이 있으므로 "설정 > 회원 탈퇴 시 모든 데이터 즉시 삭제" 안내 페이지를 만들어 URL 등록 (콜백 구현보다 안내 URL이 간단)
- [ ] 앱 도메인, 사이트 URL 등록

### 비즈니스 인증 (business.facebook.com > 보안 센터)
- [ ] 사업자등록증 준비 (국문 + 필요 시 영문 번역)
- [ ] 사업자명/주소/전화가 등록증과 일치하는 웹사이트 or 공적 서류
- [ ] 도메인 소유 확인 (DNS TXT 또는 메타태그)

### 기술 요건
- [ ] OAuth redirect URI가 운영 도메인(HTTPS)으로 등록
- [ ] 심사원이 실제로 로그인해 볼 수 있는 **테스트 계정** 준비: 서비스 로그인용 이메일/비번 + 연동할 인스타 비즈니스 계정(테스트용 Facebook 페이지에 연결된 것)
- [ ] 에러 상태 정리 — 심사원이 흔히 누르는 곳(연동 해제, 재연동, 권한 거부 시)에서 죽지 않는지

### 심사 제출물
- [ ] 권한별 사용 사례 설명문 (3절 초안 활용)
- [ ] 권한별 스크린캐스트 (4절 시나리오 활용)
- [ ] 심사원용 접속 정보 + 테스트 순서 안내문 (영문)

---

## 3. 권한별 설명문 초안

> 코드가 실제 요청하는 7개 scope 기준 (`MetaApiClient.getLoginUrl`). 제출은 영문 권장 — 아래 영문을 그대로 붙여넣고 [ ] 부분만 채울 것.
> **공통 원칙**: "왜 필요한가"를 사용자가 얻는 가치로 설명하고, 스크린캐스트의 장면과 1:1로 대응시켜야 통과율이 높다.

### instagram_basic
- 용도: 연동된 인스타그램 비즈니스 계정의 기본 정보(사용자명, 프로필)를 가져와 "연동된 계정" 목록과 게시 대상 선택 UI에 표시.
- 영문 초안:
  > Taedibear Studio is a web service that helps small business owners create AI-generated captions and publish them to their own Instagram Business accounts. We use instagram_basic to retrieve the connected Instagram Business account's username and profile information, so the user can see which of their accounts is connected and choose which account to publish to. This is shown in Settings > Connected Accounts and in the publish flow.

### instagram_content_publish
- 용도: 사용자가 만든 이미지+캡션을 본인 인스타그램 계정에 즉시/예약 발행. **서비스의 핵심 기능.**
- 영문 초안:
  > This is the core feature of our app. The user uploads a photo, our app generates a caption with AI, and the user publishes it to their own Instagram Business account — either immediately or at a scheduled time they choose. We use instagram_content_publish to create the media container and publish it on the user's behalf. Content is only published with the user's explicit action (pressing "Publish" or setting a schedule).

### pages_show_list
- 용도: 사용자의 Facebook 페이지 목록 조회 — 인스타그램 비즈니스 계정은 페이지를 통해 연결되므로, 연동 과정에서 어떤 페이지에 연결된 인스타 계정인지 찾기 위해 필요.
- 영문 초안:
  > Instagram Business accounts are connected through Facebook Pages. During the one-time account connection flow, we use pages_show_list to list the user's Pages and find the Instagram Business account linked to each Page, so the user can connect it to our service.

### pages_read_engagement
- 용도: 페이지에 연결된 인스타그램 비즈니스 계정 ID 및 관련 메타데이터 조회 (연동 flow에서 pages_show_list와 함께 사용).
- 영문 초안:
  > We use pages_read_engagement together with pages_show_list during the account connection flow to read the Page's linked Instagram Business account information (instagram_business_account field), which is required to identify the account the user wants to connect.

### business_management
- 용도: 비즈니스 자산(페이지-인스타 연결) 조회 보조. **주의: 실제 API 호출에서 안 쓰고 있다면 scope에서 제거하고 심사받지 말 것 — 불필요 권한은 반려 사유.** 제출 전 코드에서 이 권한이 정말 필요한지 확인.
- 영문 초안 (유지하는 경우):
  > We use business_management to access the business assets (Pages and their linked Instagram Business accounts) that the user manages through Meta Business Manager, in order to correctly list connectable Instagram accounts during the connection flow.

### instagram_manage_insights (Phase 4-2)
- 용도: 팔로워 수 스냅샷(일별 추이 그래프) + 게시물별 조회수/좋아요/저장 지표를 분석 대시보드에 표시.
- 영문 초안:
  > Our analytics dashboard helps business owners understand how their posts perform. We use instagram_manage_insights to (1) read the account's follower count once per day to show a follower growth chart, and (2) read per-post metrics (views, likes, saves) that the user can open from their post history. Data is shown only to the account owner.

### instagram_manage_comments (Phase 4-3)
- 용도: 최근 게시물의 댓글을 가져와 AI 답글 초안을 만들고, 사용자가 승인하면 답글 발행 (계정별로 자동 발행 옵션 제공).
- 영문 초안:
  > Small business owners often miss customer comments. We use instagram_manage_comments to fetch recent comments on the user's own posts, generate a suggested reply with AI, and — only after the user approves it (or explicitly enables auto-reply for their account) — publish the reply to the comment. Users can review, edit, skip, or disable this feature at any time in Settings.

---

## 4. 스크린캐스트 가이드

**형식**: 권한마다 해당 기능이 처음부터 끝까지 나오는 영상. 하나의 긴 영상에 다 담아도 되지만, 심사원이 권한↔장면을 못 찾으면 반려되므로 **타임스탬프를 설명문에 명시**할 것. 화면 녹화에 로그인부터 포함 (심사원이 따라할 수 있게).

권장 시나리오 (한 영상, 장면 순서):
1. **[0:00] 로그인** — 테스트 계정으로 서비스 로그인
2. **[0:20] 계정 연동** — 설정 > 인스타그램 연동 클릭 → Meta OAuth 화면 → 페이지/계정 선택 → 연동 완료 목록 표시 (`pages_show_list`, `pages_read_engagement`, `business_management`, `instagram_basic`)
3. **[1:00] 캡션 생성 + 즉시 발행** — 사진 업로드 → AI 캡션 생성 → 발행 → **실제 인스타그램 앱/웹에서 게시물이 올라간 것 확인** (`instagram_content_publish`) ← 이 "실제 반영 확인" 장면이 가장 중요
4. **[2:00] 예약 발행** — 예약 설정 화면
5. **[2:20] 분석** — 분석 대시보드에서 팔로워 추이 + 게시물 인사이트 열기 (`instagram_manage_insights`)
6. **[2:50] 리뷰 답글** — 리뷰 페이지에서 댓글 목록 → AI 초안 확인 → 승인 발행 → 인스타에서 답글 확인 (`instagram_manage_comments`)

**주의사항**:
- UI가 영어 모드로 촬영하면 심사원 이해도가 올라감 (LangToggle 활용)
- 목업/스크린샷 금지 — 실제 동작 화면만
- 개인정보(실제 고객 계정) 노출 금지 — 테스트 계정 사용

---

## 5. 흔한 반려 사유와 대응

| 반려 사유 | 대응 |
|---|---|
| 스크린캐스트에서 권한 사용 장면을 못 찾음 | 권한별 타임스탬프를 설명문에 명시 |
| 요청 권한이 기능 대비 과함 | 안 쓰는 scope 제거 (특히 business_management 검토) |
| 심사원이 로그인 실패 | 테스트 계정 자격증명 재확인, 이메일 인증 등 가입 장벽 우회 경로 제공 |
| 개인정보처리방침에 플랫폼 데이터 항목 없음 | Meta 데이터의 수집·이용·보관·삭제 조항 추가 |
| 데이터 삭제 방법 불명확 | 삭제 안내 페이지 URL 등록 + 방침에 명시 |

반려는 정상 프로세스의 일부다. 반려 코멘트를 읽고 해당 부분만 고쳐 재제출하면 된다.

---

## 6. 심사 전 파일럿 (지금 바로 가능)

1. developers.facebook.com > 해당 앱 > **앱 역할 > 역할 추가 > 테스터**로 지인 사장님의 Facebook 계정 초대
2. 상대가 developers.facebook.com에서 초대 수락
3. 이후 그 계정은 심사 없이 정상 연동 가능 (인스타 비즈니스 계정 + 연결된 페이지 필요)
4. 이 파일럿으로: 심사용 스크린캐스트 소재, 초기 피드백, 온보딩 마찰 지점 확보

---

## 변경 이력
| 버전 | 날짜 | 내용 |
|---|---|---|
| v1.0 | 2026-07-06 | 최초 작성 |
