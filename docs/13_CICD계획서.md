# 13. CI/CD 계획서

작성일: 2026-07-09

---

## 1. 현재 상태

> **브랜치 전략 (2026-07-09 확인)**: 작업·배포 기준 브랜치는 **develop** (origin/develop 최신, main은 구버전에 멈춰 있음). CI는 main/develop 둘 다 감시. 권장 흐름: 평소 develop에 커밋·푸시 → 안정 시점에 develop → main 머지(릴리스 스냅샷). Vercel/Railway가 어느 브랜치를 배포하는지 대시보드에서 확인하고 develop 기준으로 통일할 것.

```
git push (develop)
  ├─→ GitHub Actions CI ······ ⏳ 파일 작성됨(.github/workflows/ci.yml), 첫 push 후 활성화
  ├─→ Vercel ················· ✅ frontend/ 자동 빌드·배포 (레포 연동 시)
  └─→ Railway ················ ✅ nixpacks.toml로 backend-java 빌드·배포 (레포 연동 시)
```

- **CI (검증)**: `.github/workflows/ci.yml` 작성 완료 — push/PR마다 백엔드 `./gradlew build -x test`(JDK 17) + 프론트 `npm ci && tsc --noEmit`. **아직 push 전이라 미동작 — 다음 push부터 자동 실행.**
- **CD (배포)**: Vercel·Railway가 GitHub 레포에 연동돼 있다면 push만으로 자동 배포 = CD는 사실상 이미 동작 중. 각 대시보드에서 "GitHub 연동 + main 브랜치 자동 배포" 설정인지 확인할 것 (CLI 수동 배포로 했다면 연동으로 전환 권장).

### 현재 파이프라인의 공백
1. CI 실패해도 배포는 진행됨 — Actions와 Vercel/Railway가 서로 독립이라, 컴파일이 깨진 코드도 push하면 배포가 시도됨 (Railway 빌드 단계에서야 실패)
2. 테스트 없음 — `-x test`로 컴파일만 검증
3. DB 마이그레이션 자동화 없음 — JPA ddl-auto 의존 (운영에서 위험)
4. 배포 후 정상 확인(스모크 테스트)·롤백 절차 없음

---

## 2. 목표 파이프라인 (단계별 도입)

### Stage 1 — 지금 즉시 (설정만으로 가능)
- [ ] **첫 push로 CI 활성화** → Actions 탭에서 backend/frontend 잡 통과 확인 (Phase 4·5 코드의 첫 컴파일 검증)
- [ ] **브랜치 보호**: GitHub Settings > Branches > main에 "Require status checks (backend, frontend)" — CI 실패 커밋이 main에 못 들어가게
- [ ] **Vercel/Railway가 GitHub 연동 자동 배포인지 확인** — main push = 배포로 통일
- [ ] Railway **Healthcheck 설정**: `/health` 경로 지정 — 새 버전이 뜨지 않으면 이전 버전 유지 (간이 롤백)

### Stage 2 — 출시 전
- [ ] **PR 기반 워크플로**: 기능은 브랜치에서 작업 → PR → CI 통과 후 머지. 1인 개발이라도 "main = 항상 배포 가능" 규칙의 가치가 큼
- [ ] **핵심 단위 테스트 추가** 후 CI에서 `-x test` 제거 — 우선순위: 크레딧 차감/지급(동시성), 예약 발행 상태 전이, JWT 필터(관리자 판별). DB는 H2 인메모리 또는 Testcontainers
- [ ] **Flyway 도입** — 스키마 변경을 마이그레이션 파일로 관리, `ddl-auto=validate`로 전환 (운영 DB 실수 방지)
- [ ] Vercel **프리뷰 배포 활용** — PR마다 자동 생성되는 프리뷰 URL로 UI 확인

### Stage 3 — 유저 확보 후
- [ ] **스테이징 환경** — Railway 별도 서비스 + 테스트 DB, `develop` 브랜치 → 스테이징, `main` → 운영
- [ ] **배포 후 스모크 테스트** — Actions에서 배포 완료 후 `/health` + 로그인 API 호출 확인, 실패 시 알림
- [ ] **에러 모니터링** — Sentry(프론트+백엔드 무료 티어) 연동, 관리자 이메일 리포트와 연계
- [ ] 태그 기반 릴리스(`v1.x`) + 변경 로그 자동화

---

## 3. 운영 규칙 (요약)

| 규칙 | 내용 |
|---|---|
| 배포 단위 | main push = 자동 배포. 금요일 밤·예약 발행 피크 시간(스케줄러 동작 중) 직전 배포 지양 |
| 시크릿 | 코드에 절대 포함 금지 — GitHub Secrets / Vercel·Railway 환경변수만 사용 |
| 환경변수 변경 | `.env.example` 갱신 + memory.md 기록 (예: `ADMIN_EMAIL`) |
| 배포 실패 시 | Railway: 이전 배포로 Rollback 버튼 / Vercel: 이전 Deployment Promote |
| DB 변경 | Stage 2 전까지는 배포 전 수동 백업 → 이후 Flyway 마이그레이션으로만 |

---

## 4. 비용
GitHub Actions 프라이빗 레포 무료 2,000분/월 — 현재 CI는 1회 3~5분 수준이라 월 수백 회 push까지 무료 범위. Vercel/Railway는 기존 플랜 그대로.

---

## 변경 이력
| 버전 | 날짜 | 내용 |
|---|---|---|
| v1.0 | 2026-07-09 | 최초 작성 |
