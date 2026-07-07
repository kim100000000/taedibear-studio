# Taedibear Studio

소상공인 사장님들이 SNS 걱정 없이 가게 운영에만 집중할 수 있도록, AI가 캡션을 자동으로 만들고 인스타그램에 올려주는 웹서비스입니다.

사진 한 장만 올리면 업종/분위기에 맞는 캡션과 해시태그를 AI가 만들어주고, 바로 올리거나 원하는 시간에 예약 발행까지 한 번에 끝낼 수 있어요.

## 왜 만들었나

주변 소상공인 사장님들을 보면 다들 장사하시느라 바빠서 SNS는 늘 뒷전이신 분이 많다고 느꼈습니다. 사진은 찍어놓고 캡션은 뭐라고 써야 할지 고민하시다가 결국 올리지 못하는 경우도 많더군요. 그 고민의 과정만이라도 자동화하면 꽤 도움이 될 것 같아 기획부터 백엔드, 프론트엔드까지 직접 개발하고 있습니다. 최종 목표는 소상공인분들이 실제로 유용하게 써먹을 수 있는 서비스를 만드는 것입니다.

## 📌 현재 개발 상태

- ✅ 백엔드 API 완료 — 인증(이메일/구글/카카오/네이버), 이미지 업로드, Gemini AI 캡션 생성, 게시물 CRUD, 예약 업로드, 크레딧 시스템
- ✅ 프론트엔드 핵심 기능 완료 — 로그인/회원가입, 대시보드, 업로드→캡션→예약→완료 플로우, 히스토리, 설정
- ✅ Meta(인스타그램) 실연동 및 실제 업로드 확인 완료
- ✅ 배포 완료 — Vercel(프론트) + Railway(백엔드)
- 🔨 Phase 4 (멀티 계정 / 분석 고도화 / 리뷰 자동 답글) 코드 작성 완료, 실사용 테스트 진행 중

## 기술 스택

- 프론트엔드: React (Vite, TypeScript) — Vercel 배포
- 백엔드: Java 17 + Spring Boot 3.x — Railway 배포
- DB: MySQL (Spring Data JPA)
- 인증: JWT + OAuth2 (이메일 / 구글 / 카카오 / 네이버)
- AI: Google Gemini API (gemini-2.5-flash)
- SNS 연동: Meta Graph API (Facebook Page에 연결된 Instagram Business 계정)
- 예약 업로드: Spring `@Scheduled`

> 초기에는 Node.js + Express로 구현했고, 이후 Spring Boot로 전면 재구현하며 전환했습니다. Node.js 버전은 git 태그 `backend-node-final`에서 확인할 수 있습니다.

## 폴더 구조

```
taedibear-studio/
├── backend-java/             # Spring Boot 백엔드 (운영)
│   ├── src/main/java/...     # controller / service / domain / repository / config
│   ├── .env.example
│   └── build.gradle
├── frontend/
│   ├── src/
│   │   ├── api/             # axios 클라이언트 + API 함수 (TypeScript)
│   │   ├── components/      # NavBar, Spinner, EmptyState, ConfirmModal, Toast 등
│   │   ├── context/         # AuthContext (로그인 상태)
│   │   ├── pages/           # 랜딩/로그인/회원가입/대시보드/업로드/캡션/예약/히스토리/설정
│   │   ├── types/           # 공통 타입 정의 (User, Post, InstagramAccount 등)
│   │   ├── styles/
│   │   ├── App.tsx
│   │   └── main.tsx
│   ├── .env.example
│   └── vercel.json
├── docs/                    # 기획서/명세서/설계서/트러블슈팅 문서
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
| GET | /api/users/me | 내 정보 조회 (인증 필요) |
| GET | /api/instagram/connect, /callback | Meta OAuth로 Instagram 계정 연동 |
| GET, DELETE | /api/instagram/accounts | 연동 계정 조회/해제 |
| POST | /api/posts/upload | 이미지 업로드 |
| POST | /api/posts/caption | Gemini로 캡션/해시태그 생성 |
| POST, GET, PUT, DELETE | /api/posts | 게시물 CRUD |
| POST | /api/posts/:id/publish | 즉시 발행 |
| GET | /api/posts/:id/insights | 게시물 인사이트 (조회수/좋아요/저장) |
| POST, GET, PUT, DELETE | /api/scheduled | 예약 업로드 등록/조회/수정/삭제 |

## 배포

- 프론트엔드: Vercel — `frontend/` 디렉토리를 프로젝트 루트로 지정, `vercel.json` 포함
- 백엔드: Railway — 레포 루트의 `nixpacks.toml`이 `backend-java/`를 빌드·실행, 환경변수는 Railway 대시보드에서 설정

## 🚀 Roadmap

1. Phase 4 실사용 테스트 — 계정 재연동 후 팔로워 추이 / 인사이트 / 리뷰 자동 답글 검증
2. Meta 앱 심사(App Review) — 일반 사용자도 인스타그램 연동 가능하도록 Advanced Access 획득
3. 수익화 — Google AdSense + Pro 구독(토스페이먼츠) 연동
4. Phase 5 — 관리자 대시보드, 콘텐츠 추천, 팀 계정
