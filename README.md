# Taedibear Studio

소상공인 사장님들이 SNS 걱정 없이 가게 운영에만 집중할 수 있도록, AI가 캡션을 자동으로 만들고 인스타그램에 올려주는 웹서비스입니다.

사진 한 장만 올리면 업종/분위기에 맞는 캡션과 해시태그를 AI가 만들어주고, 바로 올리거나 원하는 시간에 예약 발행까지 한 번에 끝낼 수 있어요.

## 왜 만들었나

주변 소상공인 사장님들 보면 다들 장사하느라 바빠서 SNS는 늘 뒷전이더라고요. 사진은 찍어놓고 캡션 뭐라고 써야 할지 고민하다가 결국 안 올리는 경우도 많고. 그 부분만 자동화해도 꽤 도움이 될 것 같아서 기획부터 백엔드, 프론트엔드까지 혼자 만들어보고 있습니다. 포트폴리오 겸이긴 한데, 욕심은 진짜 써먹을 수 있는 서비스로 만드는 것까지입니다.

## 📌 현재 개발 상태

- ✅ 백엔드 API 완료 — 인증(이메일/구글/카카오/네이버), 이미지 업로드, Gemini AI 캡션 생성, 게시물 CRUD, 예약 업로드(cron)
- ✅ 프론트엔드 핵심 기능 완료 — 로그인/회원가입, 대시보드, 업로드→캡션→예약→완료 플로우, 히스토리, 설정
- 🔨 Meta(인스타그램) 실제 연동 테스트 진행 중 — 코드는 완료, Meta 개발자 계정 인증 대기 중
- 🚀 배포 예정 (Vercel + Railway)

## 기술 스택

- 프론트엔드: React (Vite) — Vercel 배포
- 백엔드: Node.js + Express — Railway 배포
- DB: MySQL (Sequelize ORM)
- 인증: JWT + Passport.js (Local / JWT / Google / Kakao 전략, Naver는 axios 기반 수동 구현)
- AI: Google Gemini API (gemini-2.5-flash)
- SNS 연동: Meta Graph API (Facebook Page에 연결된 Instagram Business 계정)
- 예약 업로드: node-cron

## 폴더 구조

```
taedibear-studio/
├── backend/
│   ├── src/
│   │   ├── config/          # DB(Sequelize), Passport 전략 설정
│   │   ├── controllers/     # auth, user, instagram, posts, scheduled
│   │   ├── middleware/      # JWT 인증 가드, 에러 핸들러
│   │   ├── models/          # User, InstagramAccount, Post, ScheduledPost
│   │   ├── routes/          # Express 라우터
│   │   ├── services/        # gemini, meta, naver, cron
│   │   └── server.js        # 앱 진입점
│   ├── .env.example
│   ├── package.json
│   ├── Procfile
│   └── railway.json
├── frontend/
│   ├── src/
│   │   ├── api/             # axios 클라이언트 + API 함수
│   │   ├── components/      # NavBar, Spinner, EmptyState, ConfirmModal, Toast 등
│   │   ├── context/         # AuthContext (로그인 상태)
│   │   ├── pages/           # 랜딩/로그인/회원가입/대시보드/업로드/캡션/예약/히스토리/설정
│   │   ├── styles/
│   │   ├── App.jsx
│   │   └── main.jsx
│   ├── .env.example
│   ├── index.html
│   ├── package.json
│   └── vercel.json
├── docs/                    # 기획서/명세서/설계서/트러블슈팅 문서
└── .gitignore
```

## 로컬 개발 시작하기

### 사전 준비
- Node.js 18+
- 로컬 또는 클라우드 MySQL 인스턴스
- Google Gemini API Key
- Google / Kakao / Naver OAuth 앱 키
- Meta 앱 (Facebook Page + Instagram Business 계정 연결)

### 백엔드

```bash
cd backend
cp .env.example .env   # 값 채우기
npm install
npm run dev             # http://localhost:4000
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
| POST, GET, PUT, DELETE | /api/scheduled | 예약 업로드 등록/조회/수정/삭제 (cron이 1분마다 처리) |

## 배포

- 프론트엔드: Vercel — `frontend/` 디렉토리를 프로젝트 루트로 지정, `vercel.json` 포함
- 백엔드: Railway — `backend/` 디렉토리, `railway.json` / `Procfile` 포함, 환경변수는 Railway 대시보드에서 설정

## 🚀 Roadmap

지금 가장 걸리는 건 Meta 쪽이에요. 코드는 다 짜놨는데 개발자 계정 인증이 막혀서 실제 연동 테스트를 못 해보고 있어요. 그것만 풀리면 바로 확인하고, 그 다음엔 이런 순서로 갈 생각입니다.

1. Meta 앱 등록 완료 후 실제 연동 테스트
2. Kakao/Naver 앱 키 발급받아서 로그인 테스트
3. 운영 환경용 마이그레이션 정리 (지금은 개발 편의상 `sequelize.sync({ alter: true })`로 때우는 중)
4. 테스트 코드 좀 보강
5. 프론트/백엔드 배포

대략 이렇게 가고 있는데, 막히는 부분 생기면 순서는 바뀔 수도 있어요.
