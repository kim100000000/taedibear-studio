# CLAUDE.md — Taedibear Studio

소상공인용 AI 인스타그램 캡션 생성/예약 발행 웹서비스. 기획~배포까지 1인 개발.

## 기술 스택
- 프론트: React + Vite + TypeScript (`frontend/`) → Vercel
- 백엔드: Java 17 + Spring Boot (`backend-java/`) → Railway (`nixpacks.toml` 확인)
- DB: MySQL (JPA), 인증: JWT + OAuth2 (이메일/구글/카카오/네이버)
- AI: Gemini API (gemini-2.5-flash), SNS: Meta Graph API, 예약: Spring `@Scheduled`

## 현재 상태 (2026-07)
- 백엔드(Java) API·프론트 핵심 기능 완료, Phase 4(멀티 계정/분석 고도화/리뷰 자동 답글) 코드 작성 완료
- Meta(인스타그램) 실연동 및 실제 업로드 확인 완료
- 배포 완료: Vercel(프론트) + Railway(backend-java)

## 작업 규칙
- 문서는 `docs/`에 번호 붙은 한국어 md로 관리 (00~09, CHECKLIST, TODO)
- 진행 상황·결정 사항은 `memory.md`에 기록하고, 작업 시작 전 `memory.md`를 먼저 읽을 것
- `.env`는 절대 커밋/출력하지 말 것. 예시는 `.env.example` 참고
- API 변경 시 `docs/05_API명세서.md`도 함께 갱신

## 자주 쓰는 명령
- 프론트: `cd frontend && npm run dev`
- 백엔드: `cd backend-java && ./gradlew bootRun`

## 이력
- 초기 백엔드는 Node.js + Express → Spring Boot로 전환하며 2026-07-06 `backend-node/` 삭제 (git 태그 `backend-node-final`에서 복구 가능)
