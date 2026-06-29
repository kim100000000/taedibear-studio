# Taedibear Studio

소상공인을 위한 SNS 자동화 웹서비스. Google Gemini API(gemini-1.5-flash)로 게시물 캡션을 생성하고, Meta Graph API로 Facebook/Instagram에 자동 게시합니다.

## 기술 스택

- 프론트엔드: React (Vite) — Vercel 배포
- 백엔드: Node.js + Express — Railway 배포
- DB: MySQL (Sequelize ORM)
- 인증: JWT + Passport.js (Local + JWT 전략)
- AI: Google Gemini API (gemini-1.5-flash)
- SNS 연동: Meta Graph API (Facebook Page / Instagram Business)

## 폴더 구조

```
taedibear-studio/
├── backend/
│   ├── src/
│   │   ├── config/          # DB(Sequelize), Passport 전략 설정
│   │   ├── controllers/     # 라우트 핸들러 (auth, sns, ai)
│   │   ├── middleware/      # JWT 인증 가드, 에러 핸들러
│   │   ├── models/          # Sequelize 모델 (User, Post)
│   │   ├── routes/          # Express 라우터
│   │   ├── services/        # Gemini, Meta Graph API 연동 로직
│   │   └── server.js        # 앱 진입점
│   ├── .env.example
│   ├── package.json
│   ├── Procfile
│   └── railway.json
├── frontend/
│   ├── src/
│   │   ├── api/             # axios 클라이언트 + API 함수
│   │   ├── components/      # 공용 컴포넌트 (ProtectedRoute 등)
│   │   ├── context/         # AuthContext (로그인 상태)
│   │   ├── pages/           # 로그인 / 대시보드 / 게시물 작성
│   │   ├── styles/
│   │   ├── App.jsx
│   │   └── main.jsx
│   ├── .env.example
│   ├── index.html
│   ├── package.json
│   └── vercel.json
└── .gitignore
```

## 로컬 개발 시작하기

### 사전 준비
- Node.js 18+
- 로컬 또는 클라우드 MySQL 인스턴스
- Google Gemini API Key
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

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | /api/auth/register | 회원가입 |
| POST | /api/auth/login | 로그인 (JWT 발급) |
| GET | /api/auth/me | 내 정보 조회 (인증 필요) |
| POST | /api/ai/generate-caption | Gemini(gemini-1.5-flash)로 SNS 캡션 생성 |
| POST | /api/sns/posts | 게시물 생성(초안/예약) |
| GET | /api/sns/posts | 내 게시물 목록 |
| POST | /api/sns/posts/:id/publish | Facebook/Instagram에 발행 |

## 배포

- 프론트엔드: Vercel — `frontend/` 디렉토리를 프로젝트 루트로 지정, `vercel.json` 포함
- 백엔드: Railway — `backend/` 디렉토리, `railway.json` / `Procfile` 포함, 환경변수는 Railway 대시보드에서 설정

## 다음 단계 제안

1. MySQL 마이그레이션/시딩 스크립트 추가 (현재는 `sequelize.sync()` 미설정 — 운영에서는 migration 권장)
2. Meta OAuth 연동 라우트 추가 (페이지/IG 계정 연결 플로우)
3. 게시물 예약 발행을 위한 큐/크론 작업 (예: node-cron 또는 Railway Cron)
4. 테스트 코드 (Jest + Supertest) 추가
