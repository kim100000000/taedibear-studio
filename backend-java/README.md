# Taedibear Studio — Java Backend (backend-java)

Node.js 백엔드(`backend/`)를 Java/Spring Boot로 재구현한 버전입니다. API 경로, 요청/응답 형식, 비즈니스 로직(검증 메시지 포함)은 `docs/05_API명세서.md` 기준으로 Node 버전과 동일하게 맞췄습니다.

## 기술 스택
- Java 17, Spring Boot 3.x
- Spring Security + JWT (직접 구현, `spring-boot-starter-oauth2-client` 미사용 — API 경로를 Node 버전과 동일하게 유지하기 위함)
- Spring Data JPA + MySQL (`ddl-auto: update`로 Node의 `sequelize.sync({alter:true})`와 동일하게 자동 동기화)
- Spring WebFlux `WebClient` (Meta Graph API, Gemini API, Google/Kakao/Naver OAuth 호출용 — 리액티브 서버는 아님, MVC + Tomcat 사용)
- `@Scheduled` (1분마다 실행, Node의 `node-cron` 대응)
- AWS SDK v2 (S3 이미지 업로드)
- Lombok, jjwt 0.12.6

## 로컬 빌드 & 실행

이 코드는 샌드박스 환경 제약(Java 17/Gradle 미설치, 외부 네트워크 차단)으로 **컴파일 검증을 하지 못했습니다.** 로컬에서 아래 절차로 직접 빌드/실행해 주세요.

1. **사전 준비**: Java 17 (Temurin 권장), MySQL 실행 중
2. **Gradle Wrapper 생성** (wrapper jar가 포함되어 있지 않음):
   ```bash
   cd backend-java
   gradle wrapper --gradle-version 8.8   # 로컬에 Gradle이 설치되어 있어야 함
   ```
   또는 IntelliJ IDEA에서 폴더를 열면 자동으로 Gradle 프로젝트로 인식되어 별도 wrapper 생성 없이 실행할 수 있습니다.
3. **환경변수 설정**: `.env.example`을 참고해 실제 값을 환경변수로 주입하거나, IDE의 Run Configuration에 등록하세요. **`.env` 파일 자체는 절대 커밋하지 마세요** (`.gitignore`에 이미 포함됨).
4. **빌드**:
   ```bash
   ./gradlew build
   ```
5. **실행**:
   ```bash
   ./gradlew bootRun
   ```
   기본 포트는 `4000`번이며 (`PORT` 환경변수로 변경 가능), Node 버전과 동일한 포트 컨벤션을 따릅니다.

## 폴더 구조
패키지는 기능 단위(`auth`, `user`, `post`, `instagram`, `schedule`)로 나뉘어 있고, 각 패키지 안에 Controller/Service/DTO가 함께 있습니다. 공통 응답 포맷(`ApiResponse`)과 예외 처리(`ApiException` + `GlobalExceptionHandler`)는 `common` 패키지에 있습니다.

## 기존 Node 백엔드(`backend/`)와의 관계
`backend/`는 참고용으로 그대로 남겨두었으며 수정하지 않았습니다. 두 백엔드는 동일한 MySQL 스키마(`docs/04_DB설계서.md`)를 공유하도록 설계되어 있어, 둘 중 하나만 운영 환경에 배포하면 됩니다.
