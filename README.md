# IT-da

> 프로젝트 의뢰인과 IT 프리랜서를 연결하는 AI 기반 매칭 플랫폼

## 프로젝트 소개

IT-da는 클라이언트의 프로젝트 요구사항을 구조화하고, 적합한 IT 프리랜서를 더 빠르게 찾을 수 있도록 돕는 매칭 플랫폼입니다.
자유 텍스트 기반 프로젝트 입력을 AI 브리프로 정리하고, 스킬/경력 기반 추천 결과를 설명 가능하게 제공하는 것을 목표로 합니다.

현재 저장소는 MVP 백엔드/웹 애플리케이션의 핵심 흐름을 로컬에서 재현할 수 있는 상태입니다.
홈/로그인/회원가입뿐 아니라 클라이언트/프리랜서 대시보드, 이력서 관리, 제안서 작성/수정, AI 브리프와 AI 인터뷰, 추천 실행/조회, 매칭 수락/취소/완료 흐름, 시드 데이터까지 포함합니다.

## 핵심 가치

- 자유 입력 기반 프로젝트 설명을 구조화된 제안서 초안으로 전환
- 스킬, 경력, 선호 조건을 바탕으로 한 설명 가능한 추천
- 제안서 작성부터 매칭, 진행, 완료까지 이어지는 일관된 도메인 흐름

## 목표 서비스 흐름

```text
프리랜서 프로필 등록 -> 클라이언트 프로젝트 입력 -> AI 브리프 생성
-> 제안서 검토 및 모집 시작 -> 추천/지원 -> 매칭 성사
-> 계약 시작 확인/진행 -> 후기 작성 및 완료 확인
```

## 현재 구현 상태

아래 표는 "서비스 전체 완성도"가 아니라 현재 리포지토리에서 확인되는 코드 기준으로 정리한 상태입니다.

| 영역 | 상태 | 설명 |
| --- | --- | --- |
| 인증/회원 | 기초 구현 | Spring Security 폼 로그인, 회원가입 처리, 이메일 중복 검증, BCrypt 비밀번호 암호화 |
| 프리랜서 프로필 | 구현 | `Member`, `Resume`, `ResumeSkill`, `ResumeAttachment`, `ProfileImage`, `StoredFile` 중심 엔티티와 이력서 작성/수정, 스킬/경력 변경 시 임베딩 갱신 |
| 파일 업로드 | 구현 | 로컬 파일 저장 경로와 메타데이터 저장 패턴, `/files/profile/**`, `/files/resume/**`, `/files/proposal/**` 노출 구성 |
| 제안서/AI 입력 | 구현 | `Proposal`, `ProposalPosition`, `ProposalPositionSkill`, AI 브리프 생성, AI 인터뷰 메시지 저장/초안 동기화, `MATCHING` 상태 수정용 edit draft 복제 정책 |
| 추천 | MVP 구현 | 추천 실행 생성/재사용, 추가 추천, 실행 상태 폴링, 실행 이력, 결과 상세, 추천 이유 생성, `RecommendationRun` / `RecommendationResult` / `ResumeEmbedding` 저장 |
| 매칭 | MVP 구현 | 추천 결과 기반 요청, 수락/거절, 계약 시작 확인, 취소 요청/철회/확인, 후기 작성, 완료 확인, 상세/목록 화면 |
| 화면 | 주요 흐름 구현 | 홈(`/`), 로그인(`/login`), 회원가입(`/signup`), 클라이언트 대시보드(`/client/dashboard`), 프리랜서 대시보드(`/freelancers/dashboard`), 제안서(`/proposals/**`), 추천(`/proposals/{id}/recommendations/**`), 매칭(`/matchings/**`) |
| 운영/시드 | 로컬 검증 가능 | `local`/`demo` 프로필에서 50명 프리랜서 이력서, 시드 제안서 2건, 추천 결과/매칭 샘플 자동 적재 |
| 테스트/품질 | 기반 구성 | 도메인/서비스/컨트롤러/리포지토리 테스트와 GitHub Actions CI 설정 포함 |

## 기술 스택

| 구분        | 기술                                                |
|-----------|---------------------------------------------------|
| Language  | Java 17                                           |
| Framework | Spring Boot 3.5.13                                |
| Web       | Spring MVC, Thymeleaf, Validation                 |
| Frontend  | Tailwind CSS, Alpine.js, Lucide                   |
| Security  | Spring Security, BCrypt, Session-based Form Login |
| Data      | Spring Data JPA, QueryDSL, PostgreSQL, H2         |
| Infra     | Docker Compose, GitHub Actions                    |
| Test      | JUnit 5, Spring Boot Test, Testcontainers         |

## 주요 문서

- [유저 플로우 (FigJam)](https://www.figma.com/board/cWaXvSsJ430Zc04jn94YyM/IT-da-%EC%9C%A0%EC%A0%80-%ED%94%8C%EB%A1%9C%EC%9A%B0?node-id=0-1&t=HFlWFnYZp19BCIS1-1)
- [디자인 문서 (Figma)](https://www.figma.com/design/zpmqsi84xBvHAmdLABBwfZ/IT-da-%EB%94%94%EC%9E%90%EC%9D%B8?node-id=0-1&t=xHEfASK7hbFbtYnZ-1)
- [프로젝트 개요](docs/project-overview.md)
- [결정 로그](docs/decision-log.md)
- [도메인 상세 명세](docs/domain-spec.md)
- [구현 백로그](docs/implementation-backlog.md)
- [Figma Make 와이어프레임 프롬프트](docs/figma-make-wireframes.md)

## 로컬 실행 방법

### 사전 요구사항

- Java 17+
- Docker / Docker Compose

### 1. 환경 변수 준비

현재 저장소는 두 가지 설정 소스를 사용합니다.

- `compose.yaml`은 루트의 `.env`를 사용합니다.
- Spring Boot 애플리케이션은 루트의 `.env.properties` 또는 OS 환경 변수를 사용합니다.

프로젝트 루트에 `.env.properties` 파일을 만들고 최소한 아래 키를 채워주세요.

```properties
DRIVER_CLASS_NAME=org.postgresql.Driver
POSTGRES_DB=itda
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
POSTGRES_PORT=5432
POSTGRES_URL=jdbc:postgresql://localhost:5432/itda
FILE_UPLOAD_PATH=./uploads

AI_BRIEF_ENABLED=false
AI_BRIEF_API_KEY=
AI_BRIEF_MODEL=gpt-5-mini

AI_EMBEDDING_ENABLED=false
AI_RECOMMEND_REASON_ENABLED=false
OPENAI_API_KEY=

APP_SEED_ENABLED=true
APP_SEED_PASSWORD=demo1234
```

참고:

- AI 브리프와 AI 인터뷰는 같은 `ai.brief` 설정을 사용합니다.
- 임베딩 생성과 추천 이유 생성은 `OPENAI_API_KEY`를 사용하며 기본값은 비활성화입니다.
- `docker compose`에서 사용하는 `.env`도 같은 DB 값으로 맞춰두는 것을 권장합니다.

### 2. PostgreSQL 실행

```bash
docker compose up -d postgres
```

### 3. 애플리케이션 실행

시드 데이터와 데모 시나리오를 함께 보려면 `local` 또는 `demo` 프로필로 실행해야 합니다.

```bash
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

Docker Desktop을 띄우지 않고 PostgreSQL만 별도로 실행했다면 아래처럼 실행할 수 있습니다.

```bash
SPRING_PROFILES_ACTIVE=local SPRING_DOCKER_COMPOSE_ENABLED=false ./gradlew bootRun
```

시드를 끄고 싶다면 아래처럼 실행하면 됩니다.

```bash
SPRING_PROFILES_ACTIVE=local APP_SEED_ENABLED=false ./gradlew bootRun
```

### 4. 접속 주소

- 홈: `http://localhost:8080`
- 로그인: `http://localhost:8080/login`
- 회원가입: `http://localhost:8080/signup`
- 클라이언트 대시보드: `http://localhost:8080/client/dashboard`
- 프리랜서 대시보드: `http://localhost:8080/freelancers/dashboard`
- 제안서 작성: `http://localhost:8080/proposals/new`

### 5. 시드 데이터

시드 데이터는 `SPRING_PROFILES_ACTIVE=local` 또는 `demo` 이고 `APP_SEED_ENABLED=true`일 때만 적재됩니다.

포함되는 시나리오는 아래와 같습니다.

- 클라이언트 계정 1개와 대표 프리랜서 계정 여러 개
- 추천 테스트용 프리랜서 이력서 50개
- `MATCHING` 상태 제안서 1건: `[SEED] AI 프리랜서 추천 플랫폼 고도화`
- `WRITING` 상태 제안서 1건: `[SEED] 관리자 대시보드 프론트 개편`
- 백엔드 포지션 기준 사전 계산된 추천 결과 Top 3
- `PROPOSED`, `ACCEPTED` 상태 매칭 샘플

- 클라이언트: `seed.client@itda.local`
- 주요 프리랜서: `seed.backend@itda.local`, `seed.ai@itda.local`, `seed.platform@itda.local`, `seed.data@itda.local`, `seed.fullstack@itda.local`
- 기본 비밀번호: `demo1234`

추천/매칭 화면을 빠르게 확인하려면 클라이언트로 로그인한 뒤 대시보드에서 시드 제안서를 열어보는 흐름이 가장 빠릅니다.

### 6. 테스트 실행

```bash
./gradlew test
```

## 프로젝트 구조

```text
.
├── .github/
│   ├── ISSUE_TEMPLATE/          # Feature / Bug 이슈 템플릿
│   ├── workflows/ci.yml         # main 기준 CI
│   └── PULL_REQUEST_TEMPLATE.md
├── compose.yaml                 # PostgreSQL 로컬 실행
├── docs/                        # 기획/도메인/백로그 문서
├── src/
│   ├── main/
│   │   ├── java/com/generic4/itda/
│   │   │   ├── config/          # Security, JPA, QueryDSL, Web 설정
│   │   │   ├── controller/      # 홈, 클라이언트 대시보드, 제안서, AI 브리프, 추천, 이력서 컨트롤러
│   │   │   ├── domain/          # member, resume, file, proposal, recommendation, position 도메인
│   │   │   ├── dto/             # 폼/인증/대시보드/제안서 DTO
│   │   │   ├── exception/       # 예외 정의
│   │   │   ├── repository/      # JPA Repository
│   │   │   ├── service/         # 회원, 이력서, 제안서, AI 브리프, 추천 서비스
│   │   │   └── ItDaApplication.java
│   │   └── resources/
│   │       ├── application.yaml
│   │       └── templates/       # landing, client, freelancer, recommendation 화면
│   └── test/                    # domain / service / controller / repository 테스트
├── build.gradle
├── gradlew
└── README.md
```

## 협업 가이드

현재 저장소에는 아래 협업 자산이 포함되어 있습니다.

- 이슈 템플릿: `.github/ISSUE_TEMPLATE/feature.md`, `.github/ISSUE_TEMPLATE/bug.md`
- PR 템플릿: `.github/PULL_REQUEST_TEMPLATE.md`
- CI: `main` 브랜치 대상 `push`, `pull_request` 시 `./gradlew test` 실행

## 앞으로의 구현 방향

현재 구현 이후의 우선순위는 아래 흐름에 가깝습니다.

- 추천 품질 고도화와 운영 지표/오류 가시성 보강
- 추천 외 경로의 프리랜서 직접 지원 흐름 추가
- 운영자용 직무/스킬/추천 품질 관리 화면 보강
- `proposal_attachments`, 계약서/완료 증빙 같은 파일 기반 확장 도메인 정리
- 추천 결과 비교 UX와 후속 협업 관리 기능 정교화
