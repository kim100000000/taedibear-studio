# Taedibear Studio

소상공인 사장님들이 SNS 걱정 없이 가게 운영에만 집중할 수 있도록, AI가 캡션을 자동으로 만들고 인스타그램에 올려주는 웹서비스입니다.

사진 한 장만 올리면 업종/분위기에 맞는 캡션과 해시태그를 AI가 만들어주고, 바로 올리거나 원하는 시간에 예약 발행까지 한 번에 끝낼 수 있어요.

**기획 → 설계 → 백엔드 → 프론트엔드 → 배포 → 운영 전 과정을 1인으로 개발했습니다.**

## 왜 만들었나

주변 소상공인 사장님들을 보면 다들 장사하시느라 바빠서 SNS는 늘 뒷전이신 분이 많다고 느꼈습니다. 사진은 찍어놓고 캡션은 뭐라고 써야 할지 고민하시다가 결국 올리지 못하는 경우도 많더군요. 그 고민의 과정만이라도 자동화하면 꽤 도움이 될 것 같아 기획부터 백엔드, 프론트엔드까지 직접 개발했습니다. 최종 목표는 소상공인분들이 실제로 유용하게 써먹을 수 있는 서비스를 만드는 것입니다.

## 핵심 기능

- **인증**: 이메일(가입 시 인증 메일) / 구글 / 카카오 / 네이버 로그인, JWT + refresh token
- **AI 캡션 생성**: 사진 업로드 → 업종·분위기·특별메뉴·이벤트·키워드 입력 → Gemini가 캡션+해시태그 생성, 마음에 안 들면 재생성
- **이미지 처리**: 업로드 진행률 표시, 10MB 초과 시 자동 재인코딩, 실패 원인별 에러 메시지, 편집(크롭/필터)
- **발행**: 즉시 발행 / 예약 발행(`@Scheduled`), 캘린더에서 발행 이력 확인, 임시 저장 후 나중에 발행
- **캡션 보관함**: 자주 쓰는 캡션 저장·재사용 (최대 50개)
- **리뷰 관리**: 인스타 댓글을 게시물별로 그룹핑해서 확인, 자동 답글 ON/OFF, 삭제된 댓글 동기화 정리
- **인사이트**: 게시물별 조회수/좋아요/저장, 팔로워 추이
- **크레딧 & 구독**: 가입 크레딧 3개 + 온보딩 보너스 2개, 광고 시청으로 추가 충전, Pro 구독(월 9,900원, 토스페이먼츠) 시 무제한
- **다국어**: 한국어/영어 (i18next)
- **관리자 대시보드**: 유저 현황, 공지사항 작성, 문의 확인
- **공지사항 & 문의**: 유저 대상 공지, 문의는 이메일 연결

## 📌 현재 개발 상태 (2026-07-09 기준)

- ✅ 백엔드/프론트엔드 핵심 기능 및 Phase 6(UX/품질 개선) 완료 — 비밀번호 찾기, 회원가입 이메일 인증, 리뷰 그룹핑, 캡션 보관함, 공지사항+문의 포함
- ✅ Meta(인스타그램) 실연동 및 실제 업로드 확인 완료
- ✅ 배포 완료 — Vercel(프론트) + Railway(백엔드), GitHub 연동으로 push 시 자동 배포
- 🔜 **Meta 앱 심사(App Review) 대기 중** — 현재는 테스트 계정만 인스타그램 연동 가능, 일반 사용자 공개를 위해 Advanced Access 심사 필요 (`docs/11_Meta앱심사준비.md`)
- 🔜 수익화(AdSense, Pro 구독)는 공개 이후 활성화 예정

## 기술 스택

- **프론트엔드**: React (Vite, TypeScript), i18next — Vercel 배포
- **백엔드**: Java 17 + Spring Boot 3.x — Railway 배포
- **DB**: MySQL (Spring Data JPA)
- **인증**: JWT + OAuth2 (이메일 / 구글 / 카카오 / 네이버)
- **AI**: Google Gemini API (gemini-2.5-flash)
- **SNS 연동**: Meta Graph API (Facebook Page에 연결된 Instagram Business 계정)
- **결제**: 토스페이먼츠
- **스토리지**: AWS S3
- **예약 발행**: Spring `@Scheduled`

> 초기에는 Node.js + Express로 구현했고, 이후 Spring Boot로 전면 재구현하며 전환했습니다. Node.js 버전은 git 태그 `backend-node-final`에서 확인할 수 있습니다.

## 🤖 개발 과정에서의 AI 활용

기획 문서(`docs/00~13`)부터 코드까지 Claude Code로 작업했습니다. `CLAUDE.md`에 작업 규칙(기술 스택, 커밋 전 확인사항, 문서 갱신 규칙)을 정의해두고, 세션마다 `memory.md`에 진행 상황·디버깅 기록을 남기며 다음 작업을 이어가는 방식으로 개발했습니다.

AI를 활용해 보안 점검을 진행해 `docs/08_보안품질점검보고서.md`에 Critical 8건(C1~C8)을 찾아냈고, 각 항목이 왜 문제인지 확인하며 결제 금액 서버 검증, CORS 오리진 제한, JWT 관련 노출 제거, SSRF 방어 등을 코드에 반영했습니다. AI를 코드 생성뿐 아니라 기획 문서 작성, 문제 진단까지 함께 쓰는 작업 파트너로 활용했고, 적용된 변경 사항은 직접 확인하며 개발했습니다.

## 🐛 트러블슈팅 하이라이트

전체 기록은 `docs/06_트러블슈팅.md` 참고. 그중 세 가지:

- **JVM 힙 OOM으로 백엔드가 이유 없이 재시작됨**: Railway 메트릭에서 메모리 사용량이 컨테이너 한도 근처에 계속 붙어있는 걸 확인. JVM이 스레드 스택·오프힙 버퍼 등 힙 외 메모리를 감안하지 않고 힙을 크게 잡는 게 원인이라 판단해, `-XX:MaxRAMPercentage=70.0`으로 힙을 컨테이너 한도의 70%로 제한해 해결.
- **캡션 생성 API 500 에러**: 원인 추적 결과, 업로드된 이미지를 재조회할 때 공개 URL로 재요청하는 방식이었는데 S3 버킷이 비공개 정책이라 403으로 실패하고 있었음. 공개 URL 재요청 대신 서버 자격증명으로 S3에서 직접 조회하도록 변경.
- **인스타그램 즉시 발행 400 에러(간헐적)**: Meta 응답 바디를 로그에 남기도록 보강해 원인이 "Media ID is not available"임을 확인. 미디어 컨테이너 생성 후 Meta가 비동기로 이미지를 처리하는데, 그 처리가 끝나기 전에 발행 API를 호출한 게 원인. 컨테이너 상태(`status_code`)가 `FINISHED`가 될 때까지 폴링한 뒤 발행하도록 수정.

## 🔒 보안 & CI/CD

- **보안 점검**: 결제 금액 서버 검증, CORS 오리진 제한, refresh token 무기한 허용 방지, OAuth state 검증, JWT URL 노출 제거, SSRF 차단(자사 S3 도메인만 허용), JWT secret 강제 검증 등을 점검·반영. 상세는 `docs/08_보안품질점검보고서.md`.
- **CI**: `.github/workflows/ci.yml` — push/PR마다 백엔드(`./gradlew build`)와 프론트(`tsc --noEmit`) 자동 검증.
- **CD**: GitHub 레포를 Vercel(프론트)·Railway(백엔드)에 연동해 `develop` 브랜치 push 시 자동 빌드·배포.

## 폴더 구조

```
taedibear-studio/
├── backend-java/             # Spring Boot 백엔드 (운영)
│   ├── src/main/java/...     # auth / user / post / instagram / review / payment / notice / admin / analytics / email / security / config
│   ├── .env.example
│   └── build.gradle
├── frontend/
│   ├── src/
│   │   ├── api/             # axios 클라이언트 + API 함수 (TypeScript)
│   │   ├── components/      # NavBar, Spinner, EmptyState, ConfirmModal, Toast 등
│   │   ├── context/         # AuthContext (로그인 상태)
│   │   ├── pages/           # 랜딩/로그인/회원가입/대시보드/업로드/캡션/예약/캘린더/리뷰/분석/공지/관리자/설정 등
│   │   ├── types/           # 공통 타입 정의 (User, Post, InstagramAccount 등)
│   │   ├── styles/
│   │   ├── App.tsx
│   │   └── main.tsx
│   ├── .env.example
│   └── vercel.json
├── docs/                    # 기획서/명세서/설계서/아키텍처/트러블슈팅/보안점검 문서
├── .github/workflows/ci.yml # GitHub Actions CI
├── nixpacks.toml            # Railway 빌드 설정 (backend-java)
└── .gitignore
```

## 로컬 개발 시작하기

### 사전 준비
- JDK 17
- Node.js 18+ (프론트엔드)
- 로컬 또는 클라우드 MySQL 인스턴스
- Google Gemini API Key
- Google / Kakao / Naver OAuth 앱 키
- Meta 앱 (Facebook Page + Instagram Business 계정 연결)
- AWS S3 버킷 (이미지 저장)
- 토스페이먼츠 API 키 (결제)
- SMTP 계정 (비밀번호 재설정/이메일 인증 발송 — Gmail 사용 시 앱 비밀번호 필요)

### 백엔드

```bash
cd backend-java
cp .env.example .env   # 값 채우기
./gradlew bootRun       # http://localhost:4000
```

### 프론트엔드

```bash
cd frontend
cp .env.example .env
npm install
npm run dev             # http://localhost:5173
```

## API 개요

자세한 요청/응답 형식은 `docs/05_API명세서.md` 참고.

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | /api/auth/register | 회원가입 |
| POST | /api/auth/login | 로그인 (JWT 발급) |
| GET | /api/auth/google, /kakao, /naver | 소셜 로그인 시작/콜백 |
| POST | /api/auth/verify-email, /resend-verification | 이메일 인증 |
| POST | /api/auth/forgot-password, /reset-password | 비밀번호 재설정 |
| GET | /api/users/me | 내 정보 조회 (인증 필요) |
| GET | /api/instagram/connect, /callback | Meta OAuth로 Instagram 계정 연동 |
| GET, DELETE | /api/instagram/accounts | 연동 계정 조회/해제 |
| POST | /api/posts/upload | 이미지 업로드 |
| POST | /api/posts/caption | Gemini로 캡션/해시태그 생성 |
| POST, GET, PUT, DELETE | /api/posts | 게시물 CRUD |
| POST | /api/posts/:id/publish | 즉시 발행 |
| GET | /api/posts/:id/insights | 게시물 인사이트 (조회수/좋아요/저장) |
| POST, GET, PUT, DELETE | /api/scheduled | 예약 업로드 등록/조회/수정/삭제 |
| GET, POST, DELETE | /api/captions | 캡션 보관함 |
| GET | /api/notices | 공지사항 목록 |
| GET, POST, DELETE | /api/admin/notices | 공지사항 관리 (관리자) |
| GET, POST | /api/reviews | 리뷰(댓글) 조회/답글 |
| POST | /api/payments/confirm | 결제 승인 (토스페이먼츠) |

## 배포

- **프론트엔드**: Vercel — `frontend/` 디렉토리를 프로젝트 루트로 지정, `vercel.json` 포함, GitHub 연동 자동 배포
- **백엔드**: Railway — 레포 루트의 `nixpacks.toml`이 `backend-java/`를 빌드·실행, GitHub 연동 자동 배포, 환경변수는 Railway 대시보드에서 설정

## 🚀 Roadmap

1. Meta 앱 심사(App Review) 통과 — 일반 사용자도 인스타그램 연동 가능하도록 Advanced Access 획득
2. 공개 출시 후 수익화 활성화 — Google AdSense + Pro 구독(토스페이먼츠)
3. CI/CD 고도화 — PR 기반 워크플로, 핵심 로직 단위 테스트, Flyway 마이그레이션 도입 (`docs/13_CICD계획서.md`)
4. Phase 5 — 콘텐츠 추천, 팀 계정
